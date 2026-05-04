/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: here-documents (POSIX 2.7.4, 2.3).
 *
 * @since 1.0
 */
final class PosixHereDocTest {

    /**
     * POSIX 2.7.4: several here-documents on one line are read in order
     * after the line ends.
     */
    @Test
    void readsTwoHereDocsOnOneLine() {
        Assertions.assertEquals(
            String.format("one%ntwo%n"),
            Sh4j.INSTANCE.out(String.format("cat <<A; cat <<B%none%nA%ntwo%nB%n"))
        );
    }

    @Test
    void keepsStatusOfTwoHereDocsOnOneLine() {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(String.format("cat <<A; cat <<B%none%nA%ntwo%nB%n"))
        );
    }

    @Test
    void stripsTabsOfTwoDashedHereDocs() {
        Assertions.assertEquals(
            String.format("one%n two%nend%n"),
            Sh4j.INSTANCE.out(
                String.format("cat <<-A; cat <<-B%n\tone%n\tA%n\t two%n\t\tB%necho end%n")
            )
        );
    }

    @Test
    void joinsBackslashNewlineInUnquotedHereDoc() {
        Assertions.assertEquals(
            String.format("ab%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%na\\%nb%nE%n"))
        );
    }

    @Test
    void keepsBackslashNewlineInQuotedHereDoc() {
        Assertions.assertEquals(
            String.format("a\\%nb%n"),
            Sh4j.INSTANCE.out(String.format("cat <<'E'%na\\%nb%nE%n"))
        );
    }

    @Test
    void keepsEscapedBackslashBeforeNewline() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E | sed -n '$='%na\\\\%nb%nE%n"))
        );
    }

    @Test
    void joinsLoneBackslashLineWithNext() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%n\\%nx%nE%n"))
        );
    }

    @Test
    void joinsContinuationBeforeDelimiterCheck() {
        Assertions.assertEquals(
            String.format("fooE%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%nfoo\\%nE%nE%n"))
        );
    }

    @Test
    void attachesHereDocInsidePipeline() {
        Assertions.assertEquals(
            String.format("bbb%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E | tr a b%naaa%nE%n"))
        );
    }

    @Test
    void attachesHereDocInsideAndOrList() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out(String.format("true && cat <<E || echo no%nyes%nE%n"))
        );
    }

    @Test
    void attachesHereDocInWhileCondition() {
        Assertions.assertEquals(
            String.format("loop%nloop%n"),
            Sh4j.INSTANCE.out(
                String.format("i=0; while cat <<E%nloop%nE%n[ \"$i\" -lt 1 ]; do i=1; done%n")
            )
        );
    }

    @Test
    void attachesHereDocInFunctionBody() {
        Assertions.assertEquals(
            String.format("fn%n"),
            Sh4j.INSTANCE.out(String.format("f() { cat <<E%nfn%nE%n}%nf%n"))
        );
    }

    @Test
    void readsHereDocEndingScriptWithoutNewline() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%nx%nE"))
        );
    }

    @Test
    void readsHereDocInsideCommandSubstitution() {
        Assertions.assertEquals(
            String.format("[in)side]%n"),
            Sh4j.INSTANCE.out(String.format("x=$(cat <<E%nin)side%nE%n)%necho \"[$x]\"%n"))
        );
    }

    @Test
    void readsDashedHereDocsInsideCommandSubstitution() {
        Assertions.assertEquals(
            String.format("[a)%nb(]%n"),
            Sh4j.INSTANCE.out(
                String.format("x=$(cat <<-'A'; cat <<B%n\ta)%n\tA%nb(%nB%n)%necho \"[$x]\"%n")
            )
        );
    }

    @Test
    void keepsLineNumberAfterHereDocs() {
        Assertions.assertEquals(
            String.format("a%nb%n6%n"),
            Sh4j.INSTANCE.out(String.format("cat <<A; cat <<B%na%nA%nb%nB%necho \"$LINENO\"%n"))
        );
    }
}
