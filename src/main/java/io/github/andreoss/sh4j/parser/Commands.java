/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.List;

/**
 * Parses simple commands: assignments, words and redirections.
 *
 * @since 1.0
 */
final class Commands {

    /** Token cursor. */
    private final Cursor cursor;

    /** Redirection parser. */
    private final Redirects redirects;

    /**
     * Ctor.
     *
     * @param cursor Token cursor
     */
    Commands(final Cursor cursor) {
        this.cursor = cursor;
        this.redirects = new Redirects(cursor);
    }

    /**
     * Parses a simple command, empty when none starts here.
     *
     * @return The command
     */
    SimpleCommand parseSimpleCommand() {
        final SimpleCommand parts = new SimpleCommand("");
        int fdnum = -1;
        boolean head = true;
        while (this.startsSimpleCommand() || !head && this.cursor.isBrace()) {
            head = false;
            if (this.cursor.check(TokenType.ASSIGNMENT_WORD)) {
                Commands.assignment(parts, this.cursor.token().text());
                this.cursor.consume();
            } else if (this.cursor.check(TokenType.IO_NUMBER)) {
                fdnum = Integer.parseInt(this.cursor.token().text());
                this.cursor.consume();
            } else if (this.redirects.isRedirectionStart()) {
                this.redirects.consumeRedirection(parts, fdnum);
                fdnum = -1;
            } else {
                this.word(parts);
            }
        }
        return Commands.named(parts);
    }

    private boolean startsSimpleCommand() {
        return this.cursor.check(TokenType.WORD) || this.cursor.check(TokenType.ASSIGNMENT_WORD)
            || this.redirects.isRedirectionStart() || this.cursor.check(TokenType.IO_NUMBER);
    }

    private void word(final SimpleCommand parts) {
        if (parts.getArguments().isEmpty()) {
            parts.setLineNumber(this.cursor.token().line());
        }
        parts.addArgument(this.cursor.token().text());
        this.cursor.consume();
    }

    private static void assignment(final SimpleCommand parts, final String text) {
        if (parts.getArguments().isEmpty()) {
            parts.addAssignment(text);
        } else {
            parts.addArgument(text);
        }
    }

    private static SimpleCommand named(final SimpleCommand parts) {
        final List<String> words = parts.getArguments();
        final SimpleCommand cmd;
        if (words.isEmpty()) {
            cmd = parts;
        } else {
            cmd = new SimpleCommand(words.get(0));
            cmd.setLineNumber(parts.getLineNumber());
            cmd.getArguments().addAll(words.subList(1, words.size()));
            cmd.getAssignments().addAll(parts.getAssignments());
            cmd.getRedirections().addAll(parts.getRedirections());
        }
        return cmd;
    }
}
