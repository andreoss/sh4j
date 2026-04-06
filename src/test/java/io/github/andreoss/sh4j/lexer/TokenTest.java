/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for Token.
 *
 * @since 1.0
 */
final class TokenTest {

    @Test
    void createsTokenWithTypeTextAndPositionCheck1() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void createsTokenWithTypeTextAndPositionCheck2() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("echo"));
    }

    @Test
    void createsTokenWithTypeTextAndPositionCheck3() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokn.text();
        CoreMatchers.equalTo("echo");
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
    }

    @Test
    void createsTokenWithTypeTextAndPositionCheck4() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokn.text();
        CoreMatchers.equalTo("echo");
        tokn.line();
        CoreMatchers.equalTo(1);
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(0));
    }

    @Test
    void createsTokenFromCharReaderCheck1() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void createsTokenFromCharReaderCheck2() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("abc"));
    }

    @Test
    void createsTokenFromCharReaderCheck3() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokn.text();
        CoreMatchers.equalTo("abc");
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
    }

    @Test
    void createsTokenFromCharReaderCheck4() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokn.text();
        CoreMatchers.equalTo("abc");
        tokn.line();
        CoreMatchers.equalTo(1);
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(0));
    }

    @Test
    void tokenEquality() {
        MatcherAssert.assertThat(
            new Token(TokenType.WORD, "x", new Position(1, 0)),
            CoreMatchers.equalTo(new Token(TokenType.WORD, "x", new Position(1, 0)))
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck1() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(CoreMatchers.equalTo((Object) null))
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck2() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        CoreMatchers.not(CoreMatchers.equalTo((Object) null));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(CoreMatchers.equalTo("x"))
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck3() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        CoreMatchers.not(CoreMatchers.equalTo((Object) null));
        CoreMatchers.not(CoreMatchers.equalTo("x"));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0))
                )
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck4() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        CoreMatchers.not(CoreMatchers.equalTo((Object) null));
        CoreMatchers.not(CoreMatchers.equalTo("x"));
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0))
                )
            );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.NEWLINE, "x", new Position(1, 0))
                )
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck5() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        CoreMatchers.not(CoreMatchers.equalTo((Object) null));
        CoreMatchers.not(CoreMatchers.equalTo("x"));
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0))
                )
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.NEWLINE, "x", new Position(1, 0))
                )
            );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(2, 0))
                )
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck6() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        CoreMatchers.not(CoreMatchers.equalTo((Object) null));
        CoreMatchers.not(CoreMatchers.equalTo("x"));
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0))
                )
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.NEWLINE, "x", new Position(1, 0))
                )
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(2, 0))
                )
            );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(1, 1))
                )
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesCheck7() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        CoreMatchers.not(CoreMatchers.equalTo((Object) null));
        CoreMatchers.not(CoreMatchers.equalTo("x"));
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0))
                )
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.NEWLINE, "x", new Position(1, 0))
                )
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(2, 0))
                )
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(1, 1))
                )
            );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.equalTo(base)
        );
    }

    @Test
    void tokenHashCodeAndNullTextCheck1() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(
            first.hashCode(),
            CoreMatchers.equalTo(
                new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode()
            )
        );
    }

    @Test
    void tokenHashCodeAndNullTextCheck2() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        new Token(null, null, new Position(0, 0));
        CoreMatchers.equalTo(
                new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode()
            );
        MatcherAssert.assertThat(
            first.hashCode(),
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0)).hashCode()
                )
            )
        );
    }

    @Test
    void tokenHashCodeAndNullTextCheck3() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        final Token empty = new Token(null, null, new Position(0, 0));
        CoreMatchers.equalTo(
                new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode()
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0)).hashCode()
                )
            );
        MatcherAssert.assertThat(
            empty,
            CoreMatchers.equalTo(
                new Token(null, null, new Position(0, 0))
            )
        );
    }

    @Test
    void tokenHashCodeAndNullTextCheck4() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        final Token empty = new Token(null, null, new Position(0, 0));
        CoreMatchers.equalTo(
                new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode()
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0)).hashCode()
                )
            );
        CoreMatchers.equalTo(
                new Token(null, null, new Position(0, 0))
            );
        MatcherAssert.assertThat(
            empty,
            CoreMatchers.not(CoreMatchers.equalTo(first))
        );
    }

    @Test
    void tokenHashCodeAndNullTextCheck5() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        final Token empty = new Token(null, null, new Position(0, 0));
        CoreMatchers.equalTo(
                new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode()
            );
        CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0)).hashCode()
                )
            );
        CoreMatchers.equalTo(
                new Token(null, null, new Position(0, 0))
            );
        CoreMatchers.not(CoreMatchers.equalTo(first));
        MatcherAssert.assertThat(
            empty.hashCode(),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void tokenToString() {
        MatcherAssert.assertThat(
            new Token(TokenType.NEWLINE, String.format("%n"), new Position(1, 0)).toString(),
            CoreMatchers.containsString("NEWLINE")
        );
    }
}
