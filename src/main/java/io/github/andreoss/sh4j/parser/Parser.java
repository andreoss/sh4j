/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser for the POSIX shell grammar (section 2.10).
 * Pulls tokens from a {@link Lexer} and emits a {@link ListNode} AST that
 * the executor walks.
 *
 * @since 1.0
 */
public final class Parser {

    /** Token cursor. */
    private final Cursor cursor;

    /** Simple command parser. */
    private final Commands commands;

    /**
     * Creates a parser over the given lexer.
     *
     * @param lexer The token source
     */
    public Parser(final Lexer lexer) {
        this(new Cursor(lexer));
    }

    private Parser(final Cursor cursor) {
        this.cursor = cursor;
        this.commands = new Commands(cursor);
    }

    /**
     * Parses the entire token stream as a top-level command list.
     *
     * @return The AST root
     */
    public ListNode parse() {
        this.cursor.init();
        this.cursor.skipNewlines();
        return this.parseList(TokenType.END_OF_FILE);
    }

    private ListNode parseList(final TokenType... terminators) {
        final ListNode list = new ListNode();
        Pipeline pipeline = this.parsePipeline();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        boolean done = false;
        while (!done && this.isListSeparator()) {
            final ListNode.Separator sep = this.cursor.separator();
            this.cursor.consume();
            this.cursor.skipNewlines();
            final int last = list.getPipelines().size() - 1;
            list.getSeparators().set(last, sep);
            if (this.cursor.isTerminator(terminators) || this.cursor.check(TokenType.END_OF_FILE)) {
                done = true;
            } else {
                pipeline = this.parsePipeline();
                list.addPipeline(pipeline, ListNode.Separator.NONE);
            }
        }
        return list;
    }

    private boolean isListSeparator() {
        return this.cursor.isPipelineSeparator() || this.cursor.check(TokenType.NEWLINE);
    }

    private Pipeline parsePipeline() {
        return this.parsePipeline(new HashSet<>());
    }

    private Pipeline parsePipeline(final Set<String> reserved) {
        boolean bang = false;
        if (this.cursor.check(TokenType.WORD) && "!".equals(this.cursor.token().text())) {
            bang = true;
            this.cursor.consume();
        }
        final Pipeline pipeline = new Pipeline(bang);
        final Node first = this.parsePipelineStage(reserved);
        if (first != null) {
            pipeline.addCommand(first);
            final boolean done = false;
            while (!done && this.cursor.check(TokenType.PIPE)) {
                this.cursor.consume();
                final Node stage = this.parsePipelineStage(reserved);
                pipeline.addCommand(stage);
            }
        }
        return pipeline;
    }

    private Node parsePipelineStage(final Set<String> reserved) {
        final Node result;
        if (this.cursor.check(TokenType.LPAREN)) {
            result = this.parseSubshell();
        } else if (this.cursor.check(TokenType.LBRACE)) {
            result = this.parseGrouping();
        } else if (this.cursor.check(TokenType.WORD)) {
            result = this.parseWordStage(reserved);
        } else {
            result = this.commands.parseSimpleCommand();
        }
        return result;
    }

    private Node parseSubshell() {
        this.cursor.consume();
        final ListNode inner;
        inner = this.parseList(TokenType.RPAREN);
        this.cursor.expect(TokenType.RPAREN);
        return new Subshell(inner);
    }

    private Node parseGrouping() {
        this.cursor.consume();
        final ListNode inner;
        inner = this.parseList(TokenType.RBRACE);
        this.cursor.expect(TokenType.RBRACE);
        return new Grouping(inner);
    }

    private Node parseWordStage(final Set<String> reserved) {
        final Node result;
        if ("if".equals(this.cursor.token().text())) {
            result = this.parseIfCommand();
        } else if ("for".equals(this.cursor.token().text())) {
            result = this.parseForCommand();
        } else if (this.isWhileWord()) {
            result = this.parseWhileCommand();
        } else if ("case".equals(this.cursor.token().text())) {
            result = this.parseCaseCommand();
        } else if (reserved.contains(this.cursor.token().text())) {
            result = null;
        } else if (this.isFunctionStart()) {
            result = this.parseFunctionDefinition();
        } else {
            result = this.commands.parseSimpleCommand();
        }
        return result;
    }

    private boolean isWhileWord() {
        return "while".equals(this.cursor.token().text())
            || "until".equals(this.cursor.token().text());
    }

    private boolean isFunctionStart() {
        return this.cursor.peekNext().type() == TokenType.LPAREN
            && this.cursor.peekSecond().type() == TokenType.RPAREN;
    }

    private WhileNode parseWhileCommand() {
        final boolean until;
        until = "until".equals(this.cursor.token().text());
        this.cursor.consume();
        final ListNode condition;
        condition = this.parseListUntilReserved("do");
        if (this.cursor.check(TokenType.SEMI)) {
            this.cursor.consume();
        }
        this.cursor.expectWord("do");
        final ListNode body;
        body = this.parseListUntilReserved("done");
        this.cursor.expectWord("done");
        return new WhileNode(condition, body, until);
    }

    private ForNode parseForCommand() {
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
        body = this.parseListUntilReserved("done");
        this.cursor.expectWord("done");
        return new ForNode(name, words, body);
    }

    private FunctionNode parseFunctionDefinition() {
        final String name;
        name = this.cursor.consume().text();
        this.cursor.consume();
        this.cursor.consume();
        final Node body;
        body = this.parseCompoundCommandBody();
        return new FunctionNode(name, body);
    }

    private Node parseCompoundCommandBody() {
        final Node body;
        if (this.cursor.check(TokenType.LPAREN)) {
            this.cursor.consume();
            final ListNode inner;
            inner = this.parseList(TokenType.RPAREN);
            this.cursor.expect(TokenType.RPAREN);
            body = new Subshell(inner);
        } else if (this.cursor.check(TokenType.LBRACE)) {
            this.cursor.consume();
            final ListNode inner;
            inner = this.parseList(TokenType.RBRACE);
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

    private CaseNode parseCaseCommand() {
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
                body = this.parseList(TokenType.DSEMI);
            }
            this.cursor.expect(TokenType.DSEMI);
            this.cursor.skipNewlines();
            items.add(new CaseItem(patterns, body));
        }
        this.cursor.expectWord("esac");
        return new CaseNode(word, items);
    }

    private IfNode parseIfCommand() {
        this.cursor.consume();
        final ListNode condition;
        condition = this.parseListUntilReserved("then");
        this.cursor.expectWord("then");
        final ListNode consequent;
        consequent = this.parseListUntilReserved("else", "elif", "fi");
        ListNode alternate = null;
        if (this.cursor.isReservedWord("else")) {
            this.cursor.consume();
            alternate = this.parseListUntilReserved("fi");
        } else if (this.cursor.isReservedWord("elif")) {
            alternate = this.parseElifChain();
        }
        this.cursor.expectWord("fi");
        return new IfNode(condition, consequent, alternate);
    }

    private ListNode parseElifChain() {
        this.cursor.consume();
        final ListNode cond;
        cond = this.parseListUntilReserved("then");
        this.cursor.expectWord("then");
        final ListNode body;
        body = this.parseListUntilReserved("else", "elif", "fi");
        ListNode trailing = null;
        if (this.cursor.isReservedWord("elif")) {
            trailing = this.parseElifChain();
        } else if (this.cursor.isReservedWord("else")) {
            this.cursor.consume();
            trailing = this.parseListUntilReserved("fi");
        }
        final IfNode chain;
        chain = new IfNode(cond, body, trailing);
        final Pipeline pipe = new Pipeline(false);
        pipe.addCommand(chain);
        final ListNode wrapper = new ListNode();
        wrapper.addPipeline(pipe, ListNode.Separator.NONE);
        return wrapper;
    }

    private ListNode parseListUntilReserved(final String... reserved) {
        final ListNode list = new ListNode();
        final Set<String> words = new HashSet<>(Arrays.asList(reserved));
        this.cursor.skipNewlines();
        if (!this.cursor.isReservedWord(reserved)) {
            boolean done = false;
            while (!done) {
                done = this.parseReservedEntry(list, words, reserved);
            }
        }
        return list;
    }

    private boolean parseReservedEntry(
        final ListNode list, final Set<String> words, final String... reserved
    ) {
        final Pipeline pipe;
        pipe = this.parsePipeline(words);
        list.addPipeline(pipe, ListNode.Separator.NONE);
        final boolean done;
        if (this.cursor.isPipelineSeparator()) {
            final ListNode.Separator sep;
            sep = this.cursor.separator();
            this.cursor.consume();
            this.cursor.skipNewlines();
            list.getSeparators().set(list.getPipelines().size() - 1, sep);
            done = this.cursor.isReservedWord(reserved);
        } else {
            done = true;
        }
        return done;
    }
}
