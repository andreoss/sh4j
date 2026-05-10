/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code case} conditional construct (POSIX 2.9.4.3 and the
 * 2.10.2 grammar rule {@code case_list_ns}).
 *
 * @since 1.0
 */
final class PosixCaseConstructTest {

    @Test
    void lastItemWithoutDoubleSemicolon() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(String.format("case x in x) echo m%nesac"))
        );
    }

    @Test
    void lastItemWithoutDoubleSemicolonStatus() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run(String.format("case x in x) echo m%nesac")));
    }

    @Test
    void doubleSemicolonOnOwnLine() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(String.format("case x in%nx) echo m%n;;%nesac"))
        );
    }

    @Test
    void leadingParenthesis() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out("case x in (x) echo m;; esac")
        );
    }

    @Test
    void leadingParenthesisWithoutDoubleSemicolon() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(String.format("case x in%n(x)%necho m%nesac"))
        );
    }

    @Test
    void emptyItem() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out("case x in x) ;; esac; echo e")
        );
    }

    @Test
    void emptyLastItemWithoutDoubleSemicolon() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out(String.format("case x in x)%nesac; echo e"))
        );
    }

    @Test
    void noItems() {
        Assertions.assertEquals(
            String.format("n%n"),
            Sh4j.INSTANCE.out("case x in esac; echo n")
        );
    }

    @Test
    void lastItemWithTwoCommands() {
        Assertions.assertEquals(
            String.format("a%nb%n"),
            Sh4j.INSTANCE.out(String.format("case x in x) echo a; echo b%nesac"))
        );
    }

    @Test
    void lastItemEndingWithSemicolon() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(String.format("case x in x) echo m;%nesac"))
        );
    }

    @Test
    void lastItemFollowedByBlankLine() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(String.format("case x in x) echo m%n%nesac"))
        );
    }

    @Test
    void commandsAfterEsac() {
        Assertions.assertEquals(
            String.format("m%nafter%n"),
            Sh4j.INSTANCE.out(String.format("case x in y) echo y;; x) echo m%nesac; echo after"))
        );
    }

    @Test
    void unmatchedLastItemStatus() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out(String.format("case x in y) echo y%nesac; echo \"$?\""))
        );
    }

    @Test
    void insideFunction() {
        Assertions.assertEquals(
            String.format("A%nO%n"),
            Sh4j.INSTANCE.out(
                String.format("f() { case \"$1\" in a) echo A;; *) echo O%nesac; }%nf a; f b")
            )
        );
    }

    @Test
    void insideCommandSubstitution() {
        Assertions.assertEquals(
            String.format("[s]%n"),
            Sh4j.INSTANCE.out(String.format("v=$(case x in x) echo s%nesac); echo \"[$v]\""))
        );
    }

    @Test
    void esacAsOrdinaryWord() {
        Assertions.assertEquals(
            String.format("esac%n"),
            Sh4j.INSTANCE.out("echo esac")
        );
    }

    @Test
    void esacAsArgumentInsideItem() {
        Assertions.assertEquals(
            String.format("m esac%n"),
            Sh4j.INSTANCE.out(String.format("case x in x) echo m esac%nesac"))
        );
    }
}
