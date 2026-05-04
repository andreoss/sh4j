/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.HereBody;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.Token;
import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.Set;

/**
 * Token cursor over a {@link Lexer} with up to two tokens of lookahead.
 *
 * @since 1.0
 */
final class Cursor {

    /** Tokens that terminate a pipeline. */
    private static final Set<TokenType> SEPARATORS = Set.of(
        TokenType.SEMI, TokenType.AMP, TokenType.AND_IF,
        TokenType.OR_IF, TokenType.NEWLINE
    );

    /** Source token stream. */
    private final Lexer lexer;

    /** Current token (the one the parser is about to consume). */
    private Token current;

    /** Cached one-token lookahead. */
    private Token next;

    /** True when {@link #next} holds a peeked token. */
    private boolean hasnext;

    /** Cached two-token lookahead (only valid when {@link #cached} is true). */
    private Token second;

    /** True when {@link #second} has been filled. */
    private boolean cached;

    /**
     * Ctor.
     *
     * @param lexer The token source
     */
    Cursor(final Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Reads the first token of the stream.
     */
    void init() {
        this.current = this.lexer.nextToken();
        this.next = this.current;
        this.cached = false;
        this.hasnext = false;
    }

    /**
     * Returns the current token without consuming it.
     *
     * @return The current token
     */
    Token token() {
        return this.current;
    }

    /**
     * Consumes the current token.
     *
     * @return The consumed token
     */
    Token consume() {
        final Token cur;
        if (this.hasnext) {
            cur = this.current;
            this.current = this.next;
            if (this.cached) {
                this.next = this.second;
                this.hasnext = true;
            } else {
                this.hasnext = false;
            }
            this.cached = false;
        } else {
            cur = this.current;
            this.current = this.lexer.nextToken();
        }
        return cur;
    }

    /**
     * Peeks at the token after the current one.
     *
     * @return The next token
     */
    Token peekNext() {
        if (!this.hasnext) {
            this.next = this.lexer.nextToken();
            this.hasnext = true;
            this.cached = false;
        }
        return this.next;
    }

    /**
     * Peeks at the second token after the current one.
     *
     * @return The second token ahead
     */
    Token peekSecond() {
        this.peekNext();
        if (!this.cached) {
            this.second = this.lexer.nextToken();
            this.cached = true;
        }
        return this.second;
    }

    /**
     * Consumes the current token, which must have the given type.
     *
     * @param type The expected type
     * @return The consumed token
     */
    Token expect(final TokenType type) {
        if (this.current.type() != type) {
            throw new ParseException(
                String.format(
                    "Expected %s but got %s at line %d column %d",
                    type, this.current.type(),
                    this.current.line(), this.current.column()
                )
            );
        }
        return this.consume();
    }

    /**
     * Tells whether the current token has the given type.
     *
     * @param type The type
     * @return True on a match
     */
    boolean check(final TokenType type) {
        return this.current.type() == type;
    }

    /**
     * Consumes any newline tokens.
     */
    void skipNewlines() {
        while (this.check(TokenType.NEWLINE)) {
            this.consume();
        }
    }

    /**
     * Tells whether the current token has one of the given types.
     *
     * @param terminators The types
     * @return True on a match
     */
    boolean isTerminator(final TokenType... terminators) {
        boolean result = false;
        for (final TokenType type : terminators) {
            if (this.check(type)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Tells whether the current token is one of the given words.
     *
     * @param words The words
     * @return True on a match
     */
    boolean isReservedWord(final String... words) {
        boolean result = false;
        if (this.check(TokenType.WORD)) {
            for (final String word : words) {
                if (this.current.text().equals(word)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Consumes the current token, which must be the given word.
     *
     * @param text The expected word
     * @return The consumed token
     */
    Token expectWord(final String text) {
        if (!this.check(TokenType.WORD) || !this.current.text().equals(text)) {
            throw new ParseException(
                String.format(
                    "Expected '%s' but got %s at line %d column %d",
                    text, this.current.text(),
                    this.current.line(), this.current.column()
                )
            );
        }
        return this.consume();
    }

    /**
     * Tells whether the current token separates pipelines.
     *
     * @return True on a separator
     */
    boolean isPipelineSeparator() {
        return Cursor.SEPARATORS.contains(this.current.type());
    }

    /**
     * Maps the current separator token to a list separator.
     *
     * @return The separator
     */
    ListNode.Separator separator() {
        final ListNode.Separator sep;
        if (this.check(TokenType.SEMI) || this.check(TokenType.NEWLINE)) {
            sep = ListNode.Separator.SEMI;
        } else if (this.check(TokenType.AMP)) {
            sep = ListNode.Separator.AMP;
        } else if (this.check(TokenType.AND_IF)) {
            sep = ListNode.Separator.AND_IF;
        } else {
            sep = ListNode.Separator.OR_IF;
        }
        return sep;
    }

    /**
     * Returns the body of the next here-document from the lexer.
     *
     * @return The body, filled once its line ends
     */
    HereBody hereDoc() {
        return this.lexer.hereDoc();
    }
}
