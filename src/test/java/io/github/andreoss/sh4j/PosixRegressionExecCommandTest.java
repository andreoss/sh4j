/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: exec command.
 *
 * @since 1.0
 */
final class PosixRegressionExecCommandTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void execCommandDoesNotResumeShellSecond() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.runFull("exec echo hi; echo after").stdout()
        );
    }

    @Test
    void execMissingCommandReturnsError() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.runFull("exec nonexistent_exec_xyz").exitCode());
    }

    @Test
    void execWithCommandAndRedirectionFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.runFull(
                String.format(
                    "exec echo hi > %s",
                    new TempDirs(PosixRegressionExecCommandTest.root).file(
                        "exec-arg.out"
                    ).getAbsolutePath()
                )
            ).exitCode()
        );
    }

    @Test
    void execWithCommandAndRedirectionSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionExecCommandTest.root).file("exec-arg.out");
        Sh4j.INSTANCE.runFull(String.format("exec echo hi > %s", file.getAbsolutePath()));
        Assertions.assertEquals(
            String.format("hi%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void execOutputDescriptorWritesFile() throws IOException {
        final File file = new TempDirs(PosixRegressionExecCommandTest.root).file("fd-out.txt");
        Sh4j.INSTANCE.run(String.format("exec 6>%s; echo toFd >&6", file.getAbsolutePath()));
        Assertions.assertEquals(
            String.format("toFd%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void execOutputDescriptorCloseSucceedsFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(
                String.format(
                    "exec 7>%s; exec 7>&-",
                    new TempDirs(PosixRegressionExecCommandTest.root).file(
                        "fd-close.txt"
                    ).getAbsolutePath()
                )
            )
        );
    }

    @Test
    void execOutputDescriptorCloseSucceedsSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionExecCommandTest.root).file("fd-close.txt");
        Sh4j.INSTANCE.run(String.format("exec 7>%s; exec 7>&-", file.getAbsolutePath()));
        Assertions.assertTrue(
            file.isFile() && file.length() == 0L,
            "closing the descriptor must leave an empty file"
        );
    }

    /**
     * POSIX 2.8 Exit Status and POSIX 2.9 Shell Commands: `exit` ends the
     * list and propagates its status.
     */
    @Test
    void exitStopsListAndPropagatesStatusFirst() {
        Assertions.assertEquals(3, Sh4j.INSTANCE.runFull("exit 3; echo after").exitCode());
    }

    @Test
    void exitStopsListAndPropagatesStatusSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull("exit 3; echo after").stdout());
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithFirst() {
        Assertions.assertEquals(
            7,
            Sh4j.INSTANCE.runFull("trap 'echo st=$?' EXIT; exit 7").exitCode()
        );
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithSecond() {
        Assertions.assertEquals(
            String.format("st=7%n"),
            Sh4j.INSTANCE.runFull("trap 'echo st=$?' EXIT; exit 7").stdout()
        );
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithThird() {
        Sh4j.INSTANCE.runFull("trap \"echo st=$?\" EXIT; exit 7");
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull("trap 'echo exit=$?' EXIT; false").exitCode()
        );
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithFourth() {
        Sh4j.INSTANCE.runFull("trap \"echo st=$?\" EXIT; exit 7");
        Assertions.assertEquals(
            String.format("exit=1%n"),
            Sh4j.INSTANCE.runFull("trap 'echo exit=$?' EXIT; false").stdout()
        );
    }

    @Test
    void exportAndReadonlyPrintListsFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("export -p").contains("export PATH="));
    }

    @Test
    void exportAndReadonlyPrintListsSecond() {
        Sh4j.INSTANCE.out("export -p");
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("readonly R=1; readonly -p").contains("readonly R=1")
        );
    }

    @Test
    void exportedValueLosesQuotes() {
        Assertions.assertEquals(
            String.format("x y%n"),
            Sh4j.INSTANCE.out("export E='x y'; sh -c 'printf \"%s\\n\" \"$E\"'")
        );
    }

    @Test
    void externalCommandRunsInExecutorCwd() throws IOException {
        final File dir = new TempDirs(PosixRegressionExecCommandTest.root).folder("cwd");
        Assertions.assertEquals(
            dir.getCanonicalPath() + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; sh -c pwd", dir.getAbsolutePath()))
        );
    }

    @Test
    void externalCommandReceivesPrefixAssignment() {
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("X=1 sh -c 'echo $X'"));
    }

    @Test
    void forWithoutInUsesPositionalParameters() {
        Assertions.assertEquals(
            String.format("a%nb%n"),
            Sh4j.INSTANCE.out("set -- a b; for i; do echo $i; done")
        );
    }

    @Test
    void functionRedirectionAndPrefixAssignmentFirst() throws IOException {
        new TempDirs(PosixRegressionExecCommandTest.root).file("fn.out");
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("f() { echo $X; }; X=1 f"));
    }

    @Test
    void functionRedirectionAndPrefixAssignmentSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionExecCommandTest.root).file("fn.out");
        Sh4j.INSTANCE.out("f() { echo $X; }; X=1 f");
        Sh4j.INSTANCE.run(String.format("f() { echo hi; }; f > %s", file.getAbsolutePath()));
        Assertions.assertEquals(
            String.format("hi%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void globExpandsDirectoryPrefix() throws IOException {
        final File dir = new TempDirs(PosixRegressionExecCommandTest.root).folder("sub");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "b.log").toPath(), new byte[0]);
        Assertions.assertEquals(
            String.format(String.format("%%s/a.txt%n"), dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(String.format("echo %s/*.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void globSkipsDotfilesAndSortsFirst() throws IOException {
        Assertions.assertTrue(
            new File(
                new TempDirs(PosixRegressionExecCommandTest.root).folder("globfix"),
                ".hidden"
            ).createNewFile()
        );
    }

    @Test
    void globSkipsDotfilesAndSortsSecond() throws IOException {
        final File dir = new TempDirs(PosixRegressionExecCommandTest.root).folder("globfix");
        new File(dir, ".hidden").createNewFile();
        Assertions.assertTrue(new File(dir, "b.txt").createNewFile());
    }

    @Test
    void globSkipsDotfilesAndSortsThird() throws IOException {
        final File dir = new TempDirs(PosixRegressionExecCommandTest.root).folder("globfix");
        new File(dir, ".hidden").createNewFile();
        new File(dir, "b.txt").createNewFile();
        Assertions.assertTrue(new File(dir, "a.txt").createNewFile());
    }

    @Test
    void globSkipsDotfilesAndSortsFourth() throws IOException {
        final File dir = new TempDirs(PosixRegressionExecCommandTest.root).folder("globfix");
        new File(dir, ".hidden").createNewFile();
        new File(dir, "b.txt").createNewFile();
        new File(dir, "a.txt").createNewFile();
        Assertions.assertEquals(
            String.format("a.txt b.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void groupingBodyRunsInPlace() {
        Assertions.assertEquals(String.format("g%n"), Sh4j.INSTANCE.out("{ echo g; }"));
    }

    @Test
    void hashWaitAndKillOptionFormsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash echo"));
    }

    @Test
    void hashWaitAndKillOptionFormsSecond() {
        Sh4j.INSTANCE.run("hash echo");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("sleep 0.1 & wait"));
    }

    @Test
    void hashWaitAndKillOptionFormsThird() {
        Sh4j.INSTANCE.run("hash echo");
        Sh4j.INSTANCE.run("sleep 0.1 & wait");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -s TERM 999999"));
    }

    @Test
    void hashWaitAndKillOptionFormsFourth() {
        Sh4j.INSTANCE.run("hash echo");
        Sh4j.INSTANCE.run("sleep 0.1 & wait");
        Sh4j.INSTANCE.run("kill -s TERM 999999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
    }

    @Test
    void illegalLoopAndReturnDiagnosticsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("break"));
    }

    @Test
    void illegalLoopAndReturnDiagnosticsSecond() {
        Sh4j.INSTANCE.run("break");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("continue"));
    }

    @Test
    void illegalLoopAndReturnDiagnosticsThird() {
        Sh4j.INSTANCE.run("break");
        Sh4j.INSTANCE.run("continue");
        Assertions.assertEquals(5, Sh4j.INSTANCE.run("return 5"));
    }

    @Test
    void killListsAndRejectsJobPidsFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("kill -l").contains("TERM"));
    }

    @Test
    void killListsAndRejectsJobPidsSecond() {
        Sh4j.INSTANCE.out("kill -l");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill notapid"));
    }

    @Test
    void killListsAndRejectsJobPidsThird() {
        Sh4j.INSTANCE.out("kill -l");
        Sh4j.INSTANCE.run("kill notapid");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("sleep 0.1 & kill $!"));
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersFirst() {
        Assertions.assertEquals(
            String.format("3 3%n"),
            Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"")
        );
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersSecond() {
        Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"");
        Assertions.assertEquals(
            String.format("0 0%n"),
            Sh4j.INSTANCE.out("set --; echo \"${#*}\" \"${#@}\"")
        );
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersThird() {
        Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"");
        Sh4j.INSTANCE.out("set --; echo \"${#*}\" \"${#@}\"");
        Assertions.assertEquals(String.format("4%n"), Sh4j.INSTANCE.out("v=abcd; echo \"${#v}\""));
    }
}
