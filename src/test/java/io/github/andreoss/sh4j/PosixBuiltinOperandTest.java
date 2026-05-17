/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: built-in operands go through POSIX 2.6 word
 * expansion exactly once, quote removal included.
 *
 * @since 1.0
 */
final class PosixBuiltinOperandTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void changesToDoubleQuotedDirectory() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder();
        new TempDirs(dir.toPath()).folder("a b");
        Assertions.assertEquals(
            String.format("%s/a b%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(
                String.format("cd '%s' && cd \"a b\" && pwd", dir.getAbsolutePath())
            )
        );
    }

    @Test
    void changesToSingleQuotedDirectory() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder();
        new TempDirs(dir.toPath()).folder("a b");
        Assertions.assertEquals(
            String.format("%s/a b%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(
                String.format("cd '%s' && cd 'a b' && pwd", dir.getAbsolutePath())
            )
        );
    }

    @Test
    void changesToAbsoluteQuotedDirectory() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder("abs dir");
        Assertions.assertEquals(
            String.format("%s%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(String.format("cd '%s'; pwd", dir.getAbsolutePath()))
        );
    }

    @Test
    void changesToParameterDirectory() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder();
        new TempDirs(dir.toPath()).folder("dir");
        Assertions.assertEquals(
            String.format("%s/dir%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && B=dir && cd \"$B\" && pwd", dir.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void changesToEscapedBlankDirectory() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder();
        new TempDirs(dir.toPath()).folder("a b");
        Assertions.assertEquals(
            String.format("%s/a b%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(
                String.format("cd '%s' && cd a\\ b && pwd", dir.getAbsolutePath())
            )
        );
    }

    @Test
    void cdDoesNotExpandParameterValueTwice() throws IOException {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run(
                String.format(
                    "cd '%s' && v='$HOME' && cd \"$v\"",
                    new TempDirs(PosixBuiltinOperandTest.root).folder().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void cdEntersDirectoryNamedLikeParameter() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder();
        new TempDirs(dir.toPath()).folder("$HOME");
        Assertions.assertEquals(
            String.format("%s/$HOME%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && v='$HOME' && cd \"$v\" && pwd", dir.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void echoDoesNotExpandParameterValueTwice() {
        Assertions.assertEquals(
            String.format("$y%n"),
            Sh4j.INSTANCE.out("x='$y'; y=1; echo \"$x\"")
        );
    }

    @Test
    void exportDoesNotExpandParameterValueTwice() {
        Assertions.assertEquals(
            String.format("$y%n"),
            Sh4j.INSTANCE.out("x='$y'; y=1; export A=\"$x\"; echo \"$A\"")
        );
    }

    @Test
    void exportKeepsUnquotedValueAsOneField() {
        Assertions.assertEquals(
            String.format("a b%n"),
            Sh4j.INSTANCE.out("x='a b'; export A=$x; echo \"$A\"")
        );
    }

    @Test
    void exportTakesQuotedName() {
        Assertions.assertEquals(
            String.format("A=1%n"),
            Sh4j.INSTANCE.out("A=1; n=A; export \"$n\"; sh -c 'echo \"A=$A\"'")
        );
    }

    @Test
    void readonlyDoesNotExpandParameterValueTwice() {
        Assertions.assertEquals(
            String.format("$y%n"),
            Sh4j.INSTANCE.out("x='$y'; y=1; readonly A=\"$x\"; echo \"$A\"")
        );
    }

    @Test
    void readonlyTakesQuotedName() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("A=1; n=A; readonly \"$n\"; (A=2) 2>/dev/null")
        );
    }

    @Test
    void unsetTakesQuotedName() {
        Assertions.assertEquals(
            String.format("gone%n"),
            Sh4j.INSTANCE.out("v=1; n=v; unset \"$n\"; echo \"${v-gone}\"")
        );
    }

    @Test
    void returnTakesQuotedStatus() {
        Assertions.assertEquals(
            3,
            Sh4j.INSTANCE.run("f() { return \"$s\"; }; s=3; f")
        );
    }

    @Test
    void readTakesQuotedName() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("n=v; printf 'x\\n' | { read \"$n\"; echo \"$v\"; }")
        );
    }

    @Test
    void readSplitsUnquotedNames() {
        Assertions.assertEquals(
            String.format("x/y%n"),
            Sh4j.INSTANCE.out("r='a b'; printf 'x y\\n' | { read $r; echo \"$a/$b\"; }")
        );
    }

    @Test
    void getoptsTakesQuotedOptionString() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("o=ab; set -- -b; getopts \"$o\" opt; echo \"$opt\"")
        );
    }

    @Test
    void shiftTakesQuotedCount() {
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out("s=2; set -- a b c; shift \"$s\"; echo \"$1\"")
        );
    }

    @Test
    void umaskTakesQuotedMask() {
        Assertions.assertEquals(
            String.format("0027%n"),
            Sh4j.INSTANCE.out("m=027; umask \"$m\"; umask")
        );
    }

    @Test
    void exitTakesQuotedStatus() {
        Assertions.assertEquals(3, Sh4j.INSTANCE.run("e=3; exit \"$e\""));
    }

    @Test
    void killDoesNotExpandParameterValueTwice() {
        Assertions.assertNotEquals(0, Sh4j.INSTANCE.run("q=1; p='$q'; kill \"$p\""));
    }

    @Test
    void waitTakesQuotedPid() {
        Assertions.assertEquals(
            3,
            Sh4j.INSTANCE.run("sh -c 'exit 3' & p=$!; wait \"$p\"")
        );
    }

    @Test
    void trapKeepsQuotesOfExpandedAction() {
        Assertions.assertEquals(
            String.format("a  b%n"),
            Sh4j.INSTANCE.out("t='echo \"a  b\"'; trap \"$t\" EXIT")
        );
    }

    @Test
    void evalKeepsQuotesOfExpandedOperand() {
        Assertions.assertEquals(
            String.format("a  b%n"),
            Sh4j.INSTANCE.out("v='a  b'; eval \"echo \\\"$v\\\"\"")
        );
    }

    @Test
    void dotDoesNotExpandParameterValueTwice() throws IOException {
        final File dir = new TempDirs(PosixBuiltinOperandTest.root).folder();
        Files.writeString(dir.toPath().resolve("$g"), "echo sourced");
        Assertions.assertEquals(
            String.format("sourced%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && f='$g' && g=none && . \"$f\"",
                    dir.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void commandTakesQuotedName() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out("c=echo; command \"$c\" hi")
        );
    }

    @Test
    void commandDoesNotExpandOperandTwice() {
        Assertions.assertEquals(
            String.format("$y%n"),
            Sh4j.INSTANCE.out("x='$y'; y=1; command echo \"$x\"")
        );
    }

    @Test
    void commandKeepsEmptyOperand() {
        Assertions.assertEquals(
            String.format("[]%n"),
            Sh4j.INSTANCE.out("command printf '[%s]\\n' \"\"")
        );
    }

    @Test
    void execTakesQuotedName() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out("c=echo; (exec \"$c\" hi)")
        );
    }

    @Test
    void execDoesNotExpandOperandTwice() {
        Assertions.assertEquals(
            String.format("$y%n"),
            Sh4j.INSTANCE.out("x='$y'; y=1; (exec echo \"$x\")")
        );
    }

    @Test
    void execWithEmptyExpansionKeepsShell() {
        Assertions.assertEquals(
            String.format("still%n"),
            Sh4j.INSTANCE.out("e=; exec $e; echo still")
        );
    }

    @Test
    void typeTakesQuotedName() {
        Assertions.assertEquals(
            String.format("cd is a shell built-in%n"),
            Sh4j.INSTANCE.out("t=cd; type \"$t\"")
        );
    }

    @Test
    void aliasDoesNotExpandParameterValueTwice() {
        Assertions.assertEquals(
            String.format("foo='echo  x'%n"),
            Sh4j.INSTANCE.out("a='echo  x'; alias foo=\"$a\"; alias foo")
        );
    }

    @Test
    void setDoesNotExpandParameterValueTwice() {
        Assertions.assertEquals(
            String.format("$y%n"),
            Sh4j.INSTANCE.out("x='$y'; y=1; set -- \"$x\"; echo \"$1\"")
        );
    }
}
