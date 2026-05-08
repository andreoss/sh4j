/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses compound commands: subshells, groupings, {@code if}, {@code for},
 * {@code while}, {@code until}, {@code case} and function definitions.
 *
 * @since 1.0
 */
final class Compounds {

    /** The parser that parses nested lists. */
    private final Parser parser;

    /** Token cursor. */
    private final Cursor cursor;

    /** Redirection parser. */
    private final Redirects redirects;

    /**
     * Ctor.
     *
     * @param parser The parser that parses nested lists
     * @param cursor Token cursor
     */
    Compounds(final Parser parser, final Cursor cursor) {
        this.parser = parser;
        this.cursor = cursor;
        this.redirects = new Redirects(cursor);
    }

    /**
     * Parses a subshell {@code ( list )}.
     *
     * @return The subshell node
     */
    Node parseSubshell() {
        this.cursor.consume();
        final ListNode inner;
        inner = this.parser.parseList(TokenType.RPAREN);
        this.cursor.expect(TokenType.RPAREN);
        return new Subshell(inner);
    }

    /**
     * Parses a brace group {@code { list; }}.
     *
     * @return The grouping node
     */
    Node parseGrouping() {
        this.cursor.consume();
        final ListNode inner;
        inner = this.parser.parseList(TokenType.RBRACE);
        this.cursor.expect(TokenType.RBRACE);
        return new Grouping(inner);
    }

    /**
     * Parses a {@code while} or {@code until} loop.
     *
     * @return The loop node
     */
    WhileNode parseWhileCommand() {
        final boolean until;
        until = "until".equals(this.cursor.token().text());
        this.cursor.consume();
        final ListNode condition;
        condition = this.parser.parseListUntilReserved("do");
        if (this.cursor.check(TokenType.SEMI)) {
            this.cursor.consume();
        }
        this.cursor.expectWord("do");
        final ListNode body;
        body = this.parser.parseListUntilReserved("done");
        this.cursor.expectWord("done");
        return new WhileNode(condition, body, until);
    }

    /**
     * Parses a {@code for} loop.
     *
     * @return The loop node
     */
    ForNode parseForCommand() {
        this.cursor.consume();
        final String name;
        name = this.cursor.expect(TokenType.WORD).text();
        final List<String> words;
        if (this.cursor.check(TokenType.WORD) && "in".equals(this.cursor.token().text())) {
            this.cursor.consume();
            words = new ArrayList<>(0);
            while (this.cursor.check(TokenType.WORD) && !this.cursor.isReservedWord("do")) {
                words.add(this.cursor.token().text());
                this.cursor.consume();
            }
        } else {
            words = null;
        }
        if (this.cursor.check(TokenType.SEMI)) {
            this.cursor.consume();
        }
        this.cursor.expectWord("do");
        final ListNode body;
        body = this.parser.parseListUntilReserved("done");
        this.cursor.expectWord("done");
        return new ForNode(name, words, body);
    }

    /**
     * Parses a function definition {@code name() body}.
     *
     * @return The function node
     */
    FunctionNode parseFunctionDefinition() {
        final String name;
        name = this.cursor.consume().text();
        this.cursor.consume();
        this.cursor.consume();
        final Node body;
        body = this.parseCompoundCommandBody();
        return new FunctionNode(name, body, this.redirects.trailing());
    }

    /**
     * Parses a {@code case} command.
     *
     * @return The case node
     */
    CaseNode parseCaseCommand() {
        this.cursor.consume();
        final String word;
        word = this.cursor.expect(TokenType.WORD).text();
        this.cursor.expectWord("in");
        this.cursor.skipNewlines();
        final List<CaseItem> items = new ArrayList<>(0);
        while (!this.cursor.isReservedWord("esac")) {
            if (this.cursor.check(TokenType.LPAREN)) {
                this.cursor.consume();
            }
            final List<String> patterns = new ArrayList<>(0);
            patterns.add(this.cursor.expect(TokenType.WORD).text());
            while (this.cursor.check(TokenType.PIPE)) {
                this.cursor.consume();
                patterns.add(this.cursor.expect(TokenType.WORD).text());
            }
            this.cursor.expect(TokenType.RPAREN);
            final ListNode body;
            if (this.cursor.check(TokenType.DSEMI)) {
                body = new ListNode();
            } else {
                body = this.parser.parseList(TokenType.DSEMI);
            }
            this.cursor.expect(TokenType.DSEMI);
            this.cursor.skipNewlines();
            items.add(new CaseItem(patterns, body));
        }
        this.cursor.expectWord("esac");
        return new CaseNode(word, items);
    }

    /**
     * Parses an {@code if} command.
     *
     * @return The if node
     */
    IfNode parseIfCommand() {
        this.cursor.consume();
        final ListNode condition;
        condition = this.parser.parseListUntilReserved("then");
        this.cursor.expectWord("then");
        final ListNode consequent;
        consequent = this.parser.parseListUntilReserved("else", "elif", "fi");
        ListNode alternate = null;
        if (this.cursor.isReservedWord("else")) {
            this.cursor.consume();
            alternate = this.parser.parseListUntilReserved("fi");
        } else if (this.cursor.isReservedWord("elif")) {
            alternate = this.parseElifChain();
        }
        this.cursor.expectWord("fi");
        return new IfNode(condition, consequent, alternate);
    }

    private Node parseCompoundCommandBody() {
        final Node body;
        if (this.cursor.check(TokenType.LPAREN)) {
            this.cursor.consume();
            final ListNode inner;
            inner = this.parser.parseList(TokenType.RPAREN);
            this.cursor.expect(TokenType.RPAREN);
            body = new Subshell(inner);
        } else if (this.cursor.check(TokenType.LBRACE)) {
            this.cursor.consume();
            final ListNode inner;
            inner = this.parser.parseList(TokenType.RBRACE);
            this.cursor.expect(TokenType.RBRACE);
            body = new Grouping(inner);
        } else if (this.cursor.isReservedWord("if")) {
            body = this.parseIfCommand();
        } else if (this.cursor.isReservedWord("for")) {
            body = this.parseForCommand();
        } else if (this.cursor.isReservedWord("while", "until")) {
            body = this.parseWhileCommand();
        } else if (this.cursor.isReservedWord("case")) {
            body = this.parseCaseCommand();
        } else {
            throw new ParseException(
                String.format(
                    "Expected compound command after function definition at line %d column %d",
                    this.cursor.token().line(), this.cursor.token().column()
                )
            );
        }
        return body;
    }

    private ListNode parseElifChain() {
        this.cursor.consume();
        final ListNode cond;
        cond = this.parser.parseListUntilReserved("then");
        this.cursor.expectWord("then");
        final ListNode body;
        body = this.parser.parseListUntilReserved("else", "elif", "fi");
        ListNode trailing = null;
        if (this.cursor.isReservedWord("elif")) {
            trailing = this.parseElifChain();
        } else if (this.cursor.isReservedWord("else")) {
            this.cursor.consume();
            trailing = this.parser.parseListUntilReserved("fi");
        }
        final IfNode chain;
        chain = new IfNode(cond, body, trailing);
        final Pipeline pipe = new Pipeline(false);
        pipe.addCommand(chain);
        final ListNode wrapper = new ListNode();
        wrapper.addPipeline(pipe, ListNode.Separator.NONE);
        return wrapper;
    }
}
