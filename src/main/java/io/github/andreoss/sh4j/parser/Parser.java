/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.Arrays;
import java.util.HashSet;
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

    /** Compound command parser. */
    private final Compounds compounds;

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
        this.compounds = new Compounds(this, cursor);
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

    /**
     * Parses a command list up to one of the given terminator tokens.
     *
     * @param terminators Token types that end the list
     * @return The list
     */
    ListNode parseList(final TokenType... terminators) {
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

    /**
     * Parses a command list up to one of the given reserved words.
     *
     * @param reserved Reserved words that end the list
     * @return The list
     */
    ListNode parseListUntilReserved(final String... reserved) {
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
            result = this.compounds.parseSubshell();
        } else if (this.cursor.check(TokenType.LBRACE)) {
            result = this.compounds.parseGrouping();
        } else if (this.cursor.check(TokenType.WORD)) {
            result = this.parseWordStage(reserved);
        } else {
            result = this.commands.parseSimpleCommand();
        }
        return result;
    }

    private Node parseWordStage(final Set<String> reserved) {
        final Node result;
        if ("if".equals(this.cursor.token().text())) {
            result = this.compounds.parseIfCommand();
        } else if ("for".equals(this.cursor.token().text())) {
            result = this.compounds.parseForCommand();
        } else if (this.isWhileWord()) {
            result = this.compounds.parseWhileCommand();
        } else if ("case".equals(this.cursor.token().text())) {
            result = this.compounds.parseCaseCommand();
        } else if (reserved.contains(this.cursor.token().text())) {
            result = null;
        } else if (this.isFunctionStart()) {
            result = this.compounds.parseFunctionDefinition();
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
