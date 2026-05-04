/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Here-documents whose operators were seen on the current line; their
 * bodies are read in order once the line ends (POSIX 2.7.4).
 *
 * @since 1.0
 */
final class HereDocs {

    /** Here-documents waiting for the end of the line. */
    private final Deque<Pending> pending;

    /** Bodies not yet handed to the parser. */
    private final Deque<HereBody> issued;

    /**
     * Ctor.
     */
    HereDocs() {
        this.pending = new ArrayDeque<>(2);
        this.issued = new ArrayDeque<>(2);
    }

    /**
     * Registers a here-document when the token is the word after its operator.
     *
     * @param previous Type of the token before
     * @param token The token
     */
    void watch(final TokenType previous, final Token token) {
        if (token.type() == TokenType.WORD
            && (previous == TokenType.DLESS || previous == TokenType.DLESSDASH)) {
            final HereBody body = new HereBody();
            this.pending.add(
                new Pending(new Delimiter(token.text()), previous == TokenType.DLESSDASH, body)
            );
            this.issued.add(body);
        }
    }

    /**
     * Tells whether bodies wait for the end of the line.
     *
     * @return True when some here-document is not read yet
     */
    boolean waiting() {
        return !this.pending.isEmpty();
    }

    /**
     * Hands out the next body in the order of the operators.
     *
     * @return The body
     */
    HereBody next() {
        return this.issued.remove();
    }

    /**
     * Reads the waiting bodies, in order, from the start of the next line.
     *
     * @param reader Input cursor just after the newline
     */
    void flush(final CharReader reader) {
        while (!this.pending.isEmpty()) {
            final Pending doc = this.pending.remove();
            doc.body().fill(HereDocs.read(reader, doc));
        }
    }

    private static String read(final CharReader reader, final Pending doc) {
        final StringBuilder content = new StringBuilder();
        boolean done = false;
        while (!done && !reader.isEof()) {
            final String text = HereDocs.line(reader, doc);
            if (text.equals(doc.word().text())) {
                done = true;
            } else {
                content.append(text).append('\n');
            }
        }
        return content.toString();
    }

    private static String line(final CharReader reader, final Pending doc) {
        final StringBuilder line = new StringBuilder();
        boolean more = true;
        while (more) {
            HereDocs.physical(reader, doc.strip(), line);
            more = !doc.word().quoted() && HereDocs.continued(line);
            if (more) {
                line.setLength(line.length() - 1);
            }
        }
        return line.toString();
    }

    private static void physical(
        final CharReader reader, final boolean strip, final StringBuilder line
    ) {
        boolean stripping = strip;
        while (!reader.isEof() && reader.peek() != '\n') {
            final int chr = reader.consume();
            if (!stripping || chr != '\t') {
                stripping = false;
                line.append((char) chr);
            }
        }
        if (!reader.isEof()) {
            reader.consume();
        }
    }

    private static boolean continued(final CharSequence line) {
        int count = 0;
        while (count < line.length() && line.charAt(line.length() - 1 - count) == '\\') {
            count = count + 1;
        }
        return count % 2 == 1;
    }

    /**
     * Here-document waiting for its body.
     *
     * @param word The delimiter word
     * @param strip True for {@code <<-}
     * @param body The body to fill
     * @since 1.0
     */
    private record Pending(Delimiter word, boolean strip, HereBody body) {
    }
}
