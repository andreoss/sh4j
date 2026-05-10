/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.parser.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: reserved words are recognised only where a
 * command name can appear (POSIX 2.4, 2.9.4.1).
 *
 * @since 1.0
 */
final class PosixReservedWordsTest {

    @Test
    void closingBraceAfterArgumentIsWord() {
        Assertions.assertEquals(
            String.format("a }%n"),
            Sh4j.INSTANCE.out("echo a }")
        );
    }

    @Test
    void closingBraceAsOnlyArgumentIsWord() {
        Assertions.assertEquals(
            String.format("}%n"),
            Sh4j.INSTANCE.out("echo }")
        );
    }

    @Test
    void openingBraceAsArgumentIsWord() {
        Assertions.assertEquals(
            String.format("{%n"),
            Sh4j.INSTANCE.out("echo {")
        );
    }

    @Test
    void bracesInsideWordStayLiteral() {
        Assertions.assertEquals(
            String.format("a{b}c%n"),
            Sh4j.INSTANCE.out("echo a{b}c")
        );
    }

    @Test
    void emptyBracePairIsWord() {
        Assertions.assertEquals(
            String.format("{}%n"),
            Sh4j.INSTANCE.out("echo {}")
        );
    }

    @Test
    void braceListIsNotExpanded() {
        Assertions.assertEquals(
            String.format("{a,b}%n"),
            Sh4j.INSTANCE.out("echo {a,b}")
        );
    }

    @Test
    void bracesAroundBlankStayWords() {
        Assertions.assertEquals(
            String.format("a{ }b%n"),
            Sh4j.INSTANCE.out("echo a{ }b")
        );
    }

    @Test
    void groupStillRuns() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("{ echo x; }")
        );
    }

    @Test
    void groupClosesWithoutBlankAfterSemicolon() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("{ echo a;}")
        );
    }

    @Test
    void closingBraceArgumentInsideGroup() {
        Assertions.assertEquals(
            String.format("}%n"),
            Sh4j.INSTANCE.out("{ echo }; }")
        );
    }

    @Test
    void nestedGroupsRun() {
        Assertions.assertEquals(
            String.format("n%n"),
            Sh4j.INSTANCE.out("{ { echo n; }; }")
        );
    }

    @Test
    void groupClosesAfterNewline() {
        Assertions.assertEquals(
            String.format("nl%n"),
            Sh4j.INSTANCE.out(String.format("{ echo nl%n}"))
        );
    }

    @Test
    void functionBodyPrintsClosingBrace() {
        Assertions.assertEquals(
            String.format("}%n"),
            Sh4j.INSTANCE.out("f() { echo }; }; f")
        );
    }

    @Test
    void functionWithoutBlanksRuns() {
        Assertions.assertEquals(
            String.format("g%n"),
            Sh4j.INSTANCE.out("f(){ echo g;}; f")
        );
    }

    @Test
    void assignmentKeepsClosingBrace() {
        Assertions.assertEquals(
            String.format("}%n"),
            Sh4j.INSTANCE.out("x=}; echo \"$x\"")
        );
    }

    @Test
    void quotedBracesStayWords() {
        Assertions.assertEquals(
            String.format("} }%n"),
            Sh4j.INSTANCE.out("echo \"}\" \\}")
        );
    }

    @Test
    void ifWordsAsArgumentsAreWords() {
        Assertions.assertEquals(
            String.format("if then fi done%n"),
            Sh4j.INSTANCE.out("echo if then fi done")
        );
    }

    @Test
    void loopWordsAsArgumentsAreWords() {
        Assertions.assertEquals(
            String.format("do esac in%n"),
            Sh4j.INSTANCE.out("echo do esac in")
        );
    }

    @Test
    void forListTakesBraces() {
        Assertions.assertEquals(
            String.format("{%n}%n"),
            Sh4j.INSTANCE.out("for i in { }; do echo \"$i\"; done")
        );
    }

    @Test
    void casePatternMatchesClosingBrace() {
        Assertions.assertEquals(
            String.format("y%n"),
            Sh4j.INSTANCE.out("case } in }) echo y;; esac")
        );
    }

    @Test
    void casePatternMatchesOpeningBrace() {
        Assertions.assertEquals(
            String.format("y%n"),
            Sh4j.INSTANCE.out("case { in a|{) echo y;; esac")
        );
    }

    @Test
    void closingBraceAfterAssignmentIsCommandName() {
        Assertions.assertEquals(
            127,
            Sh4j.INSTANCE.run("x=1 }")
        );
    }

    @Test
    void closingBraceAfterRedirectionIsArgument() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("echo x >/dev/null }; echo ok")
        );
    }

    @Test
    void strayClosingBraceIsSyntaxError() {
        Assertions.assertThrowsExactly(
            ParseException.class,
            () -> Sh4j.INSTANCE.run("echo a; }")
        );
    }
}
