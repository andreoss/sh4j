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
 * Tests for {@link Sh4j}: relative paths follow the shell working directory
 * (POSIX cd and 2.12 Shell Execution Environment).
 *
 * @since 1.0
 */
final class PosixWorkingDirectoryTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void cdRelativeStepsFromPreviousDirectory() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a/b%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd a; cd b; pwd", dir))
        );
    }

    @Test
    void cdDotDotGoesToParent() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s/a/b; cd ..; pwd", dir))
        );
    }

    @Test
    void cdTwoLevelsUpReturnsToStart() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd a/b; cd ../..; pwd", dir))
        );
    }

    @Test
    void cdDotStaysInPlace() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s/a; cd .; pwd", dir))
        );
    }

    @Test
    void cdMissingRelativeDirectoryFails() throws IOException {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run(PosixWorkingDirectoryTest.script("cd %s; cd nothere"))
        );
    }

    @Test
    void outputRedirectionWritesIntoShellDirectory() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("new%n"),
            Sh4j.INSTANCE.out(
                String.format("cd %s; cd a; echo new > g; cat %s/a/g", dir, dir)
            )
        );
    }

    @Test
    void inputRedirectionReadsFromShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out(PosixWorkingDirectoryTest.script("cd %s; cd a; cat < f"))
        );
    }

    @Test
    void dotSourcesScriptFromShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("dotted%n"),
            Sh4j.INSTANCE.out(PosixWorkingDirectoryTest.script("cd %s; cd a; . ./x"))
        );
    }

    @Test
    void globListsShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("b f run x%n"),
            Sh4j.INSTANCE.out(PosixWorkingDirectoryTest.script("cd %s; cd a; echo *"))
        );
    }

    @Test
    void externalCommandRunsInShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out(PosixWorkingDirectoryTest.script("cd %s; cd a; cat f"))
        );
    }

    @Test
    void relativeCommandPathResolvesInShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("ran%n"),
            Sh4j.INSTANCE.out(PosixWorkingDirectoryTest.script("cd %s; cd a; ./run"))
        );
    }

    @Test
    void commandVFindsRelativePathInShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("./run%n"),
            Sh4j.INSTANCE.out(PosixWorkingDirectoryTest.script("cd %s; cd a; command -v ./run"))
        );
    }

    @Test
    void emptyPathEntrySearchesShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("ran%n"),
            Sh4j.INSTANCE.out(
                PosixWorkingDirectoryTest.script("cd %s; cd a; PATH=:/bin:/usr/bin:$PATH run")
            )
        );
    }

    @Test
    void dashDirectoryOperatorChecksShellDirectory() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(PosixWorkingDirectoryTest.script("cd %s; cd a; test -d b"))
        );
    }

    @Test
    void bracketFileChecksShellDirectory() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(PosixWorkingDirectoryTest.script("cd %s; cd a; [ -f f ]"))
        );
    }

    @Test
    void execOpensDescriptorInShellDirectory() throws IOException {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out(
                PosixWorkingDirectoryTest.script("cd %s; cd a; exec 3<f; read l <&3; echo \"$l\"")
            )
        );
    }

    @Test
    void cdpathEntryLocatesOperandAndPrintsIt() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a/b%n%s/a/b%n", dir, dir),
            Sh4j.INSTANCE.out(String.format("CDPATH=/nowhere:%s/a; cd b; pwd", dir))
        );
    }

    @Test
    void cdpathEmptyEntryUsesShellDirectorySilently() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; CDPATH=:%s/a/b; cd a; pwd", dir, dir))
        );
    }

    @Test
    void cdpathMissFallsBackToShellDirectory() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; CDPATH=/nowhere; cd a; pwd", dir))
        );
    }

    @Test
    void cdpathIgnoredForAbsoluteOperand() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("CDPATH=%s/a; cd %s/a; pwd", dir, dir))
        );
    }

    @Test
    void cdpathIgnoredForDotOperand() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; CDPATH=%s/a; cd ./a; pwd", dir, dir))
        );
    }

    @Test
    void cdpathIgnoredForDotDotOperand() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s/a/b; CDPATH=%s; cd ../; pwd", dir, dir))
        );
    }

    @Test
    void cdDashReturnsAndPrintsPrevious() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s%n%s%n", dir, dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd a; cd -; pwd", dir))
        );
    }

    @Test
    void oldpwdHoldsPreviousDirectory() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd a; echo \"$OLDPWD\"", dir))
        );
    }

    @Test
    void pwdLogicalKeepsSymlink() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/lnk%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd lnk; pwd -L", dir))
        );
    }

    @Test
    void pwdPhysicalResolvesSymlink() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a/b%n", Path.of(dir).toRealPath()),
            Sh4j.INSTANCE.out(String.format("cd %s; cd lnk; pwd -P", dir))
        );
    }

    @Test
    void cdDotDotFromSymlinkIsLogical() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s/lnk; cd ..; pwd", dir))
        );
    }

    @Test
    void cdPhysicalOptionResolvesSymlink() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a/b%n", Path.of(dir).toRealPath()),
            Sh4j.INSTANCE.out(String.format("cd %s; cd -P lnk; pwd", dir))
        );
    }

    @Test
    void cdLogicalOptionKeepsSymlink() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/lnk%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd -P -L lnk; pwd", dir))
        );
    }

    @Test
    void cdPhysicalOptionBeforeDoubleDash() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a/b%n", Path.of(dir).toRealPath()),
            Sh4j.INSTANCE.out(String.format("cd %s; cd -P -- lnk; pwd", dir))
        );
    }

    @Test
    void cdDoubleDashEndsOptions() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s/a%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; cd -- a; pwd", dir))
        );
    }

    @Test
    void subshellCdLeavesParentDirectory() throws IOException {
        final String dir = PosixWorkingDirectoryTest.tree();
        Assertions.assertEquals(
            String.format("%s%n", dir),
            Sh4j.INSTANCE.out(String.format("cd %s; (cd a); pwd", dir))
        );
    }

    private static String script(final String text) throws IOException {
        return String.format(text, PosixWorkingDirectoryTest.tree());
    }

    private static String tree() throws IOException {
        final File top = new TempDirs(PosixWorkingDirectoryTest.root).folder();
        final Path dir = top.toPath();
        Files.createDirectories(dir.resolve("a/b"));
        Files.writeString(dir.resolve("a/f"), String.format("hi%n"), StandardCharsets.UTF_8);
        Files.writeString(
            dir.resolve("a/x"), String.format("echo dotted%n"), StandardCharsets.UTF_8
        );
        final Path run = dir.resolve("a/run");
        Files.writeString(run, String.format("#!/bin/sh%necho ran%n"), StandardCharsets.UTF_8);
        run.toFile().setExecutable(true);
        Files.createSymbolicLink(dir.resolve("lnk"), dir.resolve("a/b"));
        return top.getAbsolutePath();
    }
}
