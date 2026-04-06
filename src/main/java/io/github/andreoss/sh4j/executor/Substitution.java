/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Command substitution: runs a command in a subshell and captures its stdout.
 *
 * @since 1.0
 */
final class Substitution {

    /** The shell that performs the substitution. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that performs the substitution
     */
    Substitution(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Runs the command text and returns its output.
     *
     * @param text The command text
     * @return The captured stdout without trailing newlines
     */
    String capture(final String text) {
        final ListNode ast = new Parser(new Lexer(new CharReader(text))).parse();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Executor sub = this.shell.subshell();
        sub.setCaptureStream(baos);
        this.shell.substitution(sub.execute(ast));
        return Substitution.chomp(baos.toString(StandardCharsets.UTF_8));
    }

    private static String chomp(final String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '\n') {
            end = end - 1;
        }
        return text.substring(0, end);
    }
}
