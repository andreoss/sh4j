/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.TokenType;

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
        final SimpleCommand cmd;
        if (this.startsSimpleCommand()) {
            cmd = this.startSimpleCommand();
            this.continueSimpleCommand(cmd);
        } else {
            cmd = new SimpleCommand("");
        }
        return cmd;
    }

    private boolean startsSimpleCommand() {
        return this.cursor.check(TokenType.WORD) || this.cursor.check(TokenType.ASSIGNMENT_WORD)
            || this.redirects.isRedirectionStart() || this.cursor.check(TokenType.IO_NUMBER);
    }

    private SimpleCommand startSimpleCommand() {
        final SimpleCommand cmd;
        if (this.cursor.check(TokenType.ASSIGNMENT_WORD)) {
            cmd = this.startWithAssignments();
        } else if (this.cursor.check(TokenType.IO_NUMBER)) {
            this.cursor.consume();
            cmd = new SimpleCommand("");
        } else {
            final String name;
            name = this.cursor.token().text();
            final int line;
            line = this.cursor.token().line();
            this.cursor.consume();
            cmd = new SimpleCommand(name);
            cmd.setLineNumber(line);
        }
        return cmd;
    }

    private SimpleCommand startWithAssignments() {
        SimpleCommand cmd = new SimpleCommand("");
        while (this.cursor.check(TokenType.ASSIGNMENT_WORD)) {
            cmd.addAssignment(this.cursor.token().text());
            this.cursor.consume();
        }
        if (this.cursor.check(TokenType.WORD)) {
            final String name;
            name = this.cursor.token().text();
            final int line;
            line = this.cursor.token().line();
            this.cursor.consume();
            final SimpleCommand named = new SimpleCommand(name);
            named.setLineNumber(line);
            named.getAssignments().addAll(cmd.getAssignments());
            cmd = named;
        }
        return cmd;
    }

    private void continueSimpleCommand(final SimpleCommand cmd) {
        int fdnum = -1;
        if (this.cursor.check(TokenType.IO_NUMBER)) {
            fdnum = Integer.parseInt(this.cursor.token().text());
            this.cursor.consume();
        }
        final boolean named = !cmd.getName().isEmpty();
        while (this.startsSimpleCommand()) {
            if (this.cursor.check(TokenType.ASSIGNMENT_WORD)) {
                if (named) {
                    cmd.addArgument(this.cursor.token().text());
                } else {
                    cmd.addAssignment(this.cursor.token().text());
                }
                this.cursor.consume();
            } else if (this.cursor.check(TokenType.IO_NUMBER)) {
                fdnum = Integer.parseInt(this.cursor.token().text());
                this.cursor.consume();
            } else if (this.redirects.isRedirectionStart()) {
                this.redirects.consumeRedirection(cmd, fdnum);
                fdnum = -1;
            } else if (this.cursor.check(TokenType.WORD)) {
                cmd.addArgument(this.cursor.token().text());
                this.cursor.consume();
            }
        }
    }
}
