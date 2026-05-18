/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: backslash in here-document bodies (POSIX 2.7.4).
 *
 * @since 1.0
 */
final class PosixHereDocBackslashTest {

    /**
     * POSIX 2.7.4: in an unquoted here-document body a backslash is removed
     * only before dollar, backquote and backslash.
     */
    @Test
    void removesBackslashBeforeSpecialCharsForExternalCommand() {
        Assertions.assertEquals(
            String.format("$x \\ \\a ` 1%n"),
            Sh4j.INSTANCE.out(String.format("x=1%ncat <<E%n\\$x \\\\ \\a \\` $x%nE%n"))
        );
    }

    @Test
    void removesBackslashBeforeSpecialCharsForBuiltin() {
        Assertions.assertEquals(
            String.format("$x \\ \\a ` 1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "x=1%nread -r l <<E%n\\$x \\\\ \\a \\` $x%nE%nprintf '%%s\\n' \"$l\"%n"
                )
            )
        );
    }

    @Test
    void keepsDoubleQuotesAndBackslashBeforeThem() {
        Assertions.assertEquals(
            String.format("a \\\"1\\\" \"1\" \\\"%n"),
            Sh4j.INSTANCE.out(String.format("x=1%ncat <<E%na \\\"$x\\\" \"$x\" \\\\\"%nE%n"))
        );
    }

    @Test
    void keepsDoubleQuotesForBuiltin() {
        Assertions.assertEquals(
            String.format("\"q\" \\\"%n"),
            Sh4j.INSTANCE.out(
                String.format("read -r l <<E%n\"q\" \\\"%nE%nprintf '%%s\\n' \"$l\"%n")
            )
        );
    }

    @Test
    void expandsInsideSingleQuotes() {
        Assertions.assertEquals(
            String.format("'1' \\'%n"),
            Sh4j.INSTANCE.out(String.format("x=1%ncat <<E%n'$x' \\'%nE%n"))
        );
    }

    @Test
    void removesBackslashInDashedHereDocOfFunction() {
        Assertions.assertEquals(
            String.format("$x \\ \\a ` 1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "x=1%nf() { cat <<-E%n\t\\$x \\\\ \\a \\` $x%n\tE%n}%nf%n"
                )
            )
        );
    }

    @Test
    void removesBackslashInHereDocOfCommandSubstitution() {
        Assertions.assertEquals(
            String.format("$x \\ \\a ` 1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "x=1%nv=$(cat <<E%n\\$x \\\\ \\a \\` $x%nE%n)%nprintf '%%s\\n' \"$v\"%n"
                )
            )
        );
    }

    @Test
    void keepsBodyOfSingleQuotedDelimiter() {
        Assertions.assertEquals(
            String.format("\\$x \\\\ \\a \"q\"%n"),
            Sh4j.INSTANCE.out(String.format("x=1%ncat <<'E'%n\\$x \\\\ \\a \"q\"%nE%n"))
        );
    }

    @Test
    void keepsBodyOfDoubleQuotedDelimiter() {
        Assertions.assertEquals(
            String.format("\\$x \\\\ \\a%n"),
            Sh4j.INSTANCE.out(String.format("x=1%ncat <<\"E\"%n\\$x \\\\ \\a%nE%n"))
        );
    }

    @Test
    void keepsBodyOfBackslashedDelimiter() {
        Assertions.assertEquals(
            String.format("\\$x \\\\ \\a%n"),
            Sh4j.INSTANCE.out(String.format("x=1%ncat <<\\E%n\\$x \\\\ \\a%nE%n"))
        );
    }

    @Test
    void keepsQuotesInsideCommandSubstitution() {
        Assertions.assertEquals(
            String.format("a b|1|1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "x=1%ncat <<E%n$(printf '%%s' \"a b\")|${x}|${x:-\"d\"}%nE%n"
                )
            )
        );
    }

    @Test
    void unescapesBackquotedCommand() {
        Assertions.assertEquals(
            String.format("\\b%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%n`printf %%s \\\\\\\\b`%nE%n"))
        );
    }

    @Test
    void keepsTildeLiteral() {
        Assertions.assertEquals(
            String.format("~ \\~%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%n~ \\~%nE%n"))
        );
    }

    @Test
    void keepsLoneDollar() {
        Assertions.assertEquals(
            String.format("$ a$%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%n$ a$%nE%n"))
        );
    }
}
