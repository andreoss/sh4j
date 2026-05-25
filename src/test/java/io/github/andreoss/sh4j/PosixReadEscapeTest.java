/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for backslash escapes, line continuation and field splitting of the
 * {@code read} utility (POSIX read, 2.6.5 Field Splitting).
 *
 * @since 1.0
 */
final class PosixReadEscapeTest {

    @Test
    void keepsEscapedSpaceApartFromBlank() {
        Assertions.assertEquals(
            String.format("[x  a]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'x \\ \\a' | { read v; printf '[%s]\\n' \"$v\"; }"
            )
        );
    }

    @Test
    void escapedSpaceDoesNotSplitFields() {
        Assertions.assertEquals(
            String.format("[a b][c]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a\\ b c' | { read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void joinsContinuedLine() {
        Assertions.assertEquals(
            String.format("[ab]%n"),
            Sh4j.INSTANCE.out("printf 'a\\\\\\nb\\n' | { read v; echo \"[$v]\"; }")
        );
    }

    @Test
    void keepsEscapedLeadingSpace() {
        Assertions.assertEquals(
            String.format("[ lead]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' ' \\ lead' | { read v; printf '[%s]\\n' \"$v\"; }"
            )
        );
    }

    @Test
    void escapedDelimiterDoesNotSplit() {
        Assertions.assertEquals(
            String.format("[a:b][c]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a\\:b:c' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void keepsEscapedTrailingSpace() {
        Assertions.assertEquals(
            String.format("[a ]%n"),
            Sh4j.INSTANCE.out("printf '%s\\n' 'a\\ ' | { read v; printf '[%s]\\n' \"$v\"; }")
        );
    }

    @Test
    void rawModeKeepsBackslashes() {
        Assertions.assertEquals(
            String.format("[a\\ b]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a\\ b' | { read -r v; printf '[%s]\\n' \"$v\"; }"
            )
        );
    }

    @Test
    void rawModeDoesNotContinueLine() {
        Assertions.assertEquals(
            String.format("[a\\]%n"),
            Sh4j.INSTANCE.out("printf 'a\\\\\\nb\\n' | { read -r v; echo \"[$v]\"; }")
        );
    }

    @Test
    void lastVariableKeepsInnerBlanks() {
        Assertions.assertEquals(
            String.format("[p][q   r]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'p  q   r  ' | { read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void lastVariableKeepsEscapedBlankAmongBlanks() {
        Assertions.assertEquals(
            String.format("[p][q   r]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'p  q \\  r  ' | { read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void escapedBackslashIsLiteral() {
        Assertions.assertEquals(
            String.format("[a\\b]%n"),
            Sh4j.INSTANCE.out("printf '%s\\n' 'a\\\\b' | { read v; printf '[%s]\\n' \"$v\"; }")
        );
    }

    @Test
    void dropsBackslashBeforeEndOfFile() {
        Assertions.assertEquals(
            String.format("1[a]%n"),
            Sh4j.INSTANCE.out("printf 'a\\\\' | { read v; echo \"$?[$v]\"; }")
        );
    }

    @Test
    void continuationAtEndOfFileFails() {
        Assertions.assertEquals(
            String.format("1[a]%n"),
            Sh4j.INSTANCE.out("printf 'a\\\\\\n' | { read v; echo \"$?[$v]\"; }")
        );
    }

    @Test
    void continuationJoinsBeforeSplitting() {
        Assertions.assertEquals(
            String.format("[a][bc d]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a b\\' 'c d' | { read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void continuationFillsReply() {
        Assertions.assertEquals(
            String.format("[ab c]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a\\' 'b c' | { read; printf '[%s]\\n' \"$REPLY\"; }"
            )
        );
    }

    @Test
    void escapedDelimitersStayInLastField() {
        Assertions.assertEquals(
            String.format("[a:b][][c:]%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "printf '%s\\n' 'a\\:b::c\\:' | { IFS=: read x y z; ",
                    "printf '[%s][%s][%s]\\n' \"$x\" \"$y\" \"$z\"; }"
                )
            )
        );
    }

    @Test
    void keepsTwoEscapedTrailingSpaces() {
        Assertions.assertEquals(
            String.format("[a  ]%n"),
            Sh4j.INSTANCE.out("printf '%s\\n' 'a\\ \\ ' | { read v; printf '[%s]\\n' \"$v\"; }")
        );
    }

    @Test
    void escapedSpaceAloneFillsLastVariable() {
        Assertions.assertEquals(
            String.format("[a][ ]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a \\ ' | { read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void dropsSingleTrailingDelimiter() {
        Assertions.assertEquals(
            String.format("[a][b]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a:b:' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void keepsDoubleTrailingDelimiter() {
        Assertions.assertEquals(
            String.format("[a][b::]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a:b::' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void keepsDelimitersOfSeveralFields() {
        Assertions.assertEquals(
            String.format("[a][b:c:]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a:b:c:' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void singleVariableKeepsInnerDelimiters() {
        Assertions.assertEquals(
            String.format("[a:b:]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a:b:' | { IFS=: read x; printf '[%s]\\n' \"$x\"; }"
            )
        );
    }

    @Test
    void singleVariableDropsTrailingDelimiter() {
        Assertions.assertEquals(
            String.format("[a]%n"),
            Sh4j.INSTANCE.out("printf '%s\\n' 'a:' | { IFS=: read x; printf '[%s]\\n' \"$x\"; }")
        );
    }

    @Test
    void lastVariableEmptyAfterDelimiter() {
        Assertions.assertEquals(
            String.format("[a][]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a::' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void leadingDelimitersMakeEmptyFields() {
        Assertions.assertEquals(
            String.format("[][][a:b]%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "printf '%s\\n' '::a:b' | { IFS=' :' read x y z; ",
                    "printf '[%s][%s][%s]\\n' \"$x\" \"$y\" \"$z\"; }"
                )
            )
        );
    }

    @Test
    void lastVariableStartsWithDelimiter() {
        Assertions.assertEquals(
            String.format("[a][: b]%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "printf '%s\\n' ' a :: b ' | { IFS=' :' read x y; ",
                    "printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
                )
            )
        );
    }

    @Test
    void blankAroundDelimiterIsOneSeparator() {
        Assertions.assertEquals(
            String.format("[a][b][]%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "printf '%s\\n' 'a : b : ' | { IFS=' :' read x y z; ",
                    "printf '[%s][%s][%s]\\n' \"$x\" \"$y\" \"$z\"; }"
                )
            )
        );
    }

    @Test
    void dropsBlankAndDelimiterAtEnd() {
        Assertions.assertEquals(
            String.format("[a][b]%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "printf '%s\\n' 'a:b :' | { IFS=' :' read x y; ",
                    "printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
                )
            )
        );
    }

    @Test
    void keepsNonBlankAfterTrailingDelimiter() {
        Assertions.assertEquals(
            String.format("[a][b: ]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a:b: ' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void escapedTrailingDelimiterStays() {
        Assertions.assertEquals(
            String.format("[a][b:]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a:b\\::' | { IFS=: read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void emptyIfsDoesNotSplit() {
        Assertions.assertEquals(
            String.format("[a  b  ][]%n"),
            Sh4j.INSTANCE.out(
                "printf '%s\\n' 'a  b  ' | { IFS= read x y; printf '[%s][%s]\\n' \"$x\" \"$y\"; }"
            )
        );
    }

    @Test
    void missingFieldsLeaveVariablesEmpty() {
        Assertions.assertEquals(
            String.format("[a][][]%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "printf '%s\\n' ' a ' | { read x y z; ",
                    "printf '[%s][%s][%s]\\n' \"$x\" \"$y\" \"$z\"; }"
                )
            )
        );
    }
}
