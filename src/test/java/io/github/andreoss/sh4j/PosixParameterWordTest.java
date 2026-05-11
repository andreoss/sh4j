/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: field splitting and pathname expansion of the word
 * in an unquoted parameter expansion (POSIX 2.6.2, 2.6.5, 2.6.6).
 *
 * @since 1.0
 */
final class PosixParameterWordTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void keepsDoubleQuotedDefaultWordAsOneField() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u:-\"a b\"}; echo")
        );
    }

    @Test
    void splitsUnquotedExpansionInDefaultWord() {
        Assertions.assertEquals(
            String.format("[1][2]%n"),
            Sh4j.INSTANCE.out("v='1 2'; unset u; printf '[%s]' ${u:-$v}; echo")
        );
    }

    @Test
    void keepsQuotedPartBetweenUnquotedParts() {
        Assertions.assertEquals(
            String.format("[xa by]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u:-x\"a b\"y}; echo")
        );
    }

    @Test
    void keepsSingleQuotedDefaultWordAsOneField() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u:-'a b'}; echo")
        );
    }

    @Test
    void keepsBackslashEscapedBlankInDefaultWord() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u:-a\\ b}; echo")
        );
    }

    @Test
    void stillSplitsUnquotedBracedParameter() {
        Assertions.assertEquals(
            String.format("[1][2]%n"),
            Sh4j.INSTANCE.out("v='1 2'; printf '[%s]' ${v}; echo")
        );
    }

    @Test
    void keepsQuotedWordOfUnsetDefault() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u-\"a b\"}; echo")
        );
    }

    @Test
    void keepsQuotedWordOfNonEmptyAlternative() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("w=1; printf '[%s]' ${w:+\"a b\"}; echo")
        );
    }

    @Test
    void keepsQuotedWordOfSetAlternative() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("w=1; printf '[%s]' ${w+\"a b\"}; echo")
        );
    }

    @Test
    void splitsAssignedValueOfAssignDefault() {
        Assertions.assertEquals(
            String.format("[a][b][a b]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u:=\"a b\"} \"$u\"; echo")
        );
    }

    @Test
    void splitsSetValueInsteadOfQuotedWord() {
        Assertions.assertEquals(
            String.format("[1][2]%n"),
            Sh4j.INSTANCE.out("w='1 2'; printf '[%s]' ${w:-\"a b\"}; echo")
        );
    }

    @Test
    void keepsNestedQuotedDefaultWordAsOneField() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u w; printf '[%s]' ${u:-${w:-\"a b\"}}; echo")
        );
    }

    @Test
    void keepsQuotedExpansionInDefaultWord() {
        Assertions.assertEquals(
            String.format("[1 2]%n"),
            Sh4j.INSTANCE.out("v='1 2'; unset u; printf '[%s]' ${u:-\"$v\"}; echo")
        );
    }

    @Test
    void splitsOnlyUnquotedNonWhiteSeparator() {
        Assertions.assertEquals(
            String.format("[a:bc][d]%n"),
            Sh4j.INSTANCE.out("IFS=:; unset u; printf '[%s]' ${u:-\"a:b\"c:d}; echo")
        );
    }

    @Test
    void keepsEmptyQuotedDefaultWordAsField() {
        Assertions.assertEquals(
            String.format("[][z]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' ${u:-\"\"} ${u:-} z; echo")
        );
    }

    @Test
    void keepsDoubleQuotedWholeExpansionAsOneField() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u; printf '[%s]' \"${u:-\"a b\"}\"; echo")
        );
    }

    @Test
    void keepsQuotedDefaultWordInAssignment() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out("unset u; x=${u:-\"a b\"}; printf '[%s]' \"$x\"; echo")
        );
    }

    @Test
    void keepsQuotedDefaultWordInForList() {
        Assertions.assertEquals(
            String.format("[a b]%n"),
            Sh4j.INSTANCE.out(
                "unset u; for i in ${u:-\"a b\"}; do printf '[%s]' \"$i\"; done; echo"
            )
        );
    }

    @Test
    void keepsQuotedDefaultWordWithNoglob() {
        Assertions.assertEquals(
            String.format("[a b][*]%n"),
            Sh4j.INSTANCE.out("unset u; set -f; printf '[%s]' ${u:-\"a b\"} ${u:-*}; echo")
        );
    }

    @Test
    void keepsQuotedDefaultWordInHereDocument() {
        Assertions.assertEquals(
            String.format("a b%n"),
            Sh4j.INSTANCE.out(String.format("unset u; cat <<E%n${u:-\"a b\"}%nE"))
        );
    }

    @Test
    void doesNotGlobQuotedPatternInDefaultWord() throws IOException {
        final File dir = new TempDirs(PosixParameterWordTest.root).folder();
        new TempDirs(dir.toPath()).file("only");
        Assertions.assertEquals(
            String.format("[*]%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; unset u; printf '[%%s]' ${u:-\"*\"}; echo",
                    dir.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void globsUnquotedPatternInDefaultWord() throws IOException {
        final File dir = new TempDirs(PosixParameterWordTest.root).folder();
        new TempDirs(dir.toPath()).file("only");
        Assertions.assertEquals(
            String.format("[only]%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; unset u; printf '[%%s]' ${u:-*}; echo",
                    dir.getAbsolutePath()
                )
            )
        );
    }
}
