/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.Delimiter;
import io.github.andreoss.sh4j.lexer.HereBody;
import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.List;

/**
 * Parses redirections and here-documents of a simple command.
 *
 * @since 1.0
 */
final class Redirects {

    /** Token cursor. */
    private final Cursor cursor;

    /**
     * Ctor.
     *
     * @param cursor Token cursor
     */
    Redirects(final Cursor cursor) {
        this.cursor = cursor;
    }

    /**
     * Tells whether the current token starts a redirection.
     *
     * @return True on a redirection operator
     */
    boolean isRedirectionStart() {
        return this.isInputRedirect() || this.isOutputRedirect();
    }

    /**
     * Consumes a redirection and adds it to the command.
     *
     * @param cmd The command
     * @param fdnum The explicit descriptor, or -1 for the default
     */
    void consumeRedirection(final SimpleCommand cmd, final int fdnum) {
        if (this.cursor.check(TokenType.DLESS) || this.cursor.check(TokenType.DLESSDASH)) {
            this.cursor.consume();
            final Delimiter raw = new Delimiter(this.cursor.expect(TokenType.WORD).text());
            final boolean quoted = raw.quoted();
            final String delimiter = raw.text();
            final HereBody content = this.cursor.hereDoc();
            final int desc;
            if (fdnum >= 0) {
                desc = fdnum;
            } else {
                desc = 0;
            }
            cmd.addRedirection(
                new Redirection(
                    desc, RedirectionType.HERE_DOC,
                    new Redirection.HereDoc(delimiter, content::text, quoted)
                )
            );
        } else {
            final RedirectionType type = this.redirectionTypeForCurrent();
            final int fallback = Redirects.defaultFdFor(type);
            this.cursor.consume();
            final String target;
            target = this.cursor.expect(TokenType.WORD).text();
            final int desc;
            if (fdnum >= 0) {
                desc = fdnum;
            } else {
                desc = fallback;
            }
            cmd.addRedirection(new Redirection(desc, type, target));
        }
    }

    /**
     * Parses the redirections that follow a compound command.
     *
     * @return The redirections in source order
     */
    List<Redirection> trailing() {
        final SimpleCommand holder = new SimpleCommand("");
        int fdnum = -1;
        while (this.cursor.check(TokenType.IO_NUMBER) || this.isRedirectionStart()) {
            if (this.cursor.check(TokenType.IO_NUMBER)) {
                fdnum = Integer.parseInt(this.cursor.token().text());
                this.cursor.consume();
            } else {
                this.consumeRedirection(holder, fdnum);
                fdnum = -1;
            }
        }
        return holder.getRedirections();
    }

    private boolean isInputRedirect() {
        return this.cursor.check(TokenType.LESS) || this.cursor.check(TokenType.DLESS)
            || this.isInputExtended();
    }

    private boolean isInputExtended() {
        return this.cursor.check(TokenType.DLESSDASH) || this.cursor.check(TokenType.LESSAND)
            || this.cursor.check(TokenType.LESSGREATER);
    }

    private boolean isOutputRedirect() {
        return this.cursor.check(TokenType.GREAT) || this.cursor.check(TokenType.CLOBBER)
            || this.isOutputExtended();
    }

    private boolean isOutputExtended() {
        return this.cursor.check(TokenType.DGREATER) || this.cursor.check(TokenType.GREATAND)
            || this.cursor.check(TokenType.LESSGREATER);
    }

    private RedirectionType redirectionTypeForCurrent() {
        final RedirectionType type;
        if (this.cursor.check(TokenType.LESSAND)) {
            type = RedirectionType.DUP_INPUT;
        } else if (this.cursor.check(TokenType.LESSGREATER)) {
            type = RedirectionType.READWRITE;
        } else if (this.cursor.check(TokenType.LESS)) {
            type = RedirectionType.INPUT;
        } else if (this.cursor.check(TokenType.GREATAND)) {
            type = RedirectionType.DUP_OUTPUT;
        } else if (this.cursor.check(TokenType.GREAT)) {
            type = RedirectionType.OUTPUT;
        } else if (this.cursor.check(TokenType.CLOBBER)) {
            type = RedirectionType.CLOBBER;
        } else {
            type = RedirectionType.APPEND;
        }
        return type;
    }

    private static int defaultFdFor(final RedirectionType type) {
        final int desc;
        if (type == RedirectionType.DUP_INPUT
            || type == RedirectionType.READWRITE
            || type == RedirectionType.INPUT) {
            desc = 0;
        } else {
            desc = 1;
        }
        return desc;
    }
}
