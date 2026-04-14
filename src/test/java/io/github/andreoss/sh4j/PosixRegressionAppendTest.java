/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: append.
 *
 * @since 1.0
 */
final class PosixRegressionAppendTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    /**
     * POSIX 2.7.3 Appending Redirected Output and POSIX 2.7 Redirection.
     */
    @Test
    void appendRedirectAndAssignmentPrefixFirst() throws IOException {
        final File file = new TempDirs(PosixRegressionAppendTest.root).file("append.txt");
        Assertions.assertEquals(
            String.format("a%nb%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "echo a > %s; echo b >> %s; cat %s",
                    file.getAbsolutePath(),
                    file.getAbsolutePath(),
                    file.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void appendRedirectAndAssignmentPrefixSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionAppendTest.root).file("append.txt");
        Sh4j.INSTANCE.out(
            String.format(
                "echo a > %s; echo b >> %s; cat %s",
                file.getAbsolutePath(),
                file.getAbsolutePath(),
                file.getAbsolutePath()
            )
        );
        Assertions.assertEquals(
            String.format("hi%n[]%n"),
            Sh4j.INSTANCE.out("x=1 echo hi; echo [$x]")
        );
    }

    @Test
    void backgroundJobInheritsFunctionsAndWaitJoinsIt() {
        Assertions.assertEquals(
            String.format("F%n"),
            Sh4j.INSTANCE.out("f() { echo F; }; f & wait")
        );
    }

    @Test
    void backgroundWaitReturnsJobStatus() {
        Assertions.assertEquals(3, Sh4j.INSTANCE.run("sh -c 'exit 3' & wait $!"));
    }

    @Test
    void backtickInsideSingleQuotesIsLiteral() {
        Assertions.assertEquals(String.format("`date`%n"), Sh4j.INSTANCE.out("echo '`date`'"));
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsFirst() {
        Assertions.assertEquals(
            String.format("notclass%n"),
            Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac")
        );
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsSecond() {
        Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("klass%n"),
            Sh4j.INSTANCE.out("case abc in [abc]*) echo klass;; *) echo no;; esac")
        );
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsThird() {
        Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac");
        Sh4j.INSTANCE.out("case abc in [abc]*) echo klass;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("if then fi%n"),
            Sh4j.INSTANCE.out("echo if then fi")
        );
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsFourth() {
        Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac");
        Sh4j.INSTANCE.out("case abc in [abc]*) echo klass;; *) echo no;; esac");
        Sh4j.INSTANCE.out("echo if then fi");
        Assertions.assertEquals(
            String.format("multi%n"),
            Sh4j.INSTANCE.out("case aaab in a*b) echo multi;; esac")
        );
    }

    @Test
    void casePatternUndergoesExpansion() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("p='h*'; case hi in $p) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void commandSubstitutionStripsAllTrailingNewlines() {
        Assertions.assertEquals(
            String.format("<a>%n"),
            Sh4j.INSTANCE.out("X=$(printf 'a\\n\\n\\n'); printf '<%s>\\n' \"$X\"")
        );
    }

    @Test
    void commandBypassesFunctionLookupFirst() {
        Assertions.assertEquals(
            127,
            Sh4j.INSTANCE.runFull("f() { echo function; }; command f").exitCode()
        );
    }

    @Test
    void commandBypassesFunctionLookupSecond() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("f() { echo function; }; command f").stdout()
        );
    }

    @Test
    void commandVerboseDescribesFunction() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("f() { :; }; command -V f").contains("f is a function")
        );
    }

    @Test
    void commentStartsAtTokenPositionOnlyFirstCase() {
        Assertions.assertEquals(String.format("a%n"), Sh4j.INSTANCE.out("echo a #c"));
    }

    @Test
    void commentStartsAtTokenPositionOnlySecondCase() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out(String.format("#lead%necho b"))
        );
    }

    @Test
    void commentStartsAtTokenPositionOnlyThirdCase() {
        Assertions.assertEquals(String.format("one#two%n"), Sh4j.INSTANCE.out("echo one#two"));
    }

    @Test
    void descriptorRedirectionsPersistAcrossCommandsFirst() throws IOException {
        final File file = new TempDirs(PosixRegressionAppendTest.root).file("fd.txt");
        Files.write(file.toPath(), String.format("seed%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("v=seed%n"),
            Sh4j.INSTANCE.out(
                String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", file.getAbsolutePath())
            )
        );
    }

    @Test
    void descriptorRedirectionsPersistAcrossCommandsSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionAppendTest.root).file("fd.txt");
        Files.write(file.toPath(), String.format("seed%n").getBytes(StandardCharsets.UTF_8));
        final String path = file.getAbsolutePath();
        Sh4j.INSTANCE.out(String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", path));
        Assertions.assertEquals(
            String.format("w=seed%n"),
            Sh4j.INSTANCE.out(String.format("exec 4<%s; read w <&4; echo \"w=$w\"", path))
        );
    }

    @Test
    void descriptorRedirectionsPersistAcrossCommandsThird() throws IOException {
        final File file = new TempDirs(PosixRegressionAppendTest.root).file("fd.txt");
        Files.write(file.toPath(), String.format("seed%n").getBytes(StandardCharsets.UTF_8));
        final String path = file.getAbsolutePath();
        Sh4j.INSTANCE.out(String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", path));
        Sh4j.INSTANCE.out(String.format("exec 4<%s; read w <&4; echo \"w=$w\"", path));
        Assertions.assertEquals(
            String.format("y=[]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                String.format("exec 5<%s; exec 5<&-; read y <&5; echo \"y=[$y]\"", path)
            ).stdout()
        );
    }

    @Test
    void dollarBangIsNotAnOsPid() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.runFull("sleep 0.1 & kill $!").exitCode());
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileFirst() throws IOException {
        final Path returned = new TempDirs(PosixRegressionAppendTest.root).file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.runFull(String.format(". %s; echo \"rc=$?\"", returned)).exitCode()
        );
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileSecond() throws IOException {
        final Path returned = new TempDirs(PosixRegressionAppendTest.root).file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            String.format("before%nrc=5%n"),
            Sh4j.INSTANCE.runFull(String.format(". %s; echo \"rc=$?\"", returned)).stdout()
        );
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileThird() throws IOException {
        final Path returned = new TempDirs(PosixRegressionAppendTest.root).file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        Sh4j.INSTANCE.runFull(String.format(". %s; echo \"rc=$?\"", returned));
        final Path exited = new TempDirs(PosixRegressionAppendTest.root).file("exit.sh").toPath();
        Files.write(
            exited,
            String.format("echo x; exit 3; echo y%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            3,
            Sh4j.INSTANCE.runFull(String.format(". %s; echo after", exited)).exitCode()
        );
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileFourth() throws IOException {
        final Path returned = new TempDirs(PosixRegressionAppendTest.root).file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        Sh4j.INSTANCE.runFull(String.format(". %s; echo \"rc=$?\"", returned));
        final Path exited = new TempDirs(PosixRegressionAppendTest.root).file("exit.sh").toPath();
        Files.write(
            exited,
            String.format("echo x; exit 3; echo y%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.runFull(String.format(". %s; echo after", exited)).stdout()
        );
    }

    @Test
    void dotMissingFileReturnsOneFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(". /nonexistent/library.sh").exitCode());
    }

    @Test
    void dotMissingFileReturnsOneSecond() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(". /nonexistent/library.sh").stderr().contains("cannot open")
        );
    }

    @Test
    void dotSetsPositionalParameters() throws IOException {
        final File lib = new TempDirs(PosixRegressionAppendTest.root).file("args-lib.sh");
        Files.write(lib.toPath(), String.format("echo \"$1\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("arg%n"),
            Sh4j.INSTANCE.out(String.format(". %s arg", lib.getAbsolutePath()))
        );
    }

    @Test
    void dotWithoutFileReturnsOne() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("."));
    }

    @Test
    void emptyAndMissingCommandsReturnErrorFirst() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("PATH=; ls"));
    }

    @Test
    void emptyAndMissingCommandsReturnErrorSecond() {
        Sh4j.INSTANCE.run("PATH=; ls");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("/nonexistent/command"));
    }

    @Test
    void emptyAndMissingCommandsReturnErrorThird() {
        Sh4j.INSTANCE.run("PATH=; ls");
        Sh4j.INSTANCE.run("/nonexistent/command");
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("PATH=:/bin; true; echo ok")
        );
    }

    @Test
    void execRedirectionFailureTerminatesShellFirst() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "exec 9</nonexistent-dir-xyz/f; echo after"
            ).exitCode()
        );
    }

    @Test
    void execRedirectionFailureTerminatesShellSecond() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "exec 9</nonexistent-dir-xyz/f; echo after"
            ).stdout()
        );
    }

    @Test
    void execRedirectionFailureTerminatesShellThird() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        );
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "exec >/nonexistent-dir-xyz/f; echo after"
            ).exitCode()
        );
    }

    @Test
    void execRedirectionFailureTerminatesShellFourth() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        );
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "exec >/nonexistent-dir-xyz/f; echo after"
            ).stdout()
        );
    }

    @Test
    void execRedirectionPersistsFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.runFull(
                String.format(
                    "exec > %s; echo redirected",
                    new TempDirs(PosixRegressionAppendTest.root).file("exec.out").getAbsolutePath()
                )
            ).exitCode()
        );
    }

    @Test
    void execRedirectionPersistsSecond() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                String.format(
                    "exec > %s; echo redirected",
                    new TempDirs(PosixRegressionAppendTest.root).file("exec.out").getAbsolutePath()
                )
            ).stdout()
        );
    }

    @Test
    void execRedirectionPersistsThird() throws IOException {
        final File file = new TempDirs(PosixRegressionAppendTest.root).file("exec.out");
        Sh4j.INSTANCE.runFull(String.format("exec > %s; echo redirected", file.getAbsolutePath()));
        Assertions.assertEquals(
            String.format("redirected%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void execCommandDoesNotResumeShellFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("exec echo hi; echo after").exitCode());
    }
}
