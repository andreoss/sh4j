/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * Thrown by {@link Parser} when the input token stream does not form a
 * valid POSIX shell command list. Unchecked because the lexer/parser are
 * exercised by tests and the executor surfaces parse errors at the CLI
 * boundary.
 * @since 1.0
 */
public class ParseException extends RuntimeException {

    /**
     * Creates a new parse exception with the given diagnostic message.
     * @param message Human-readable description of the parse failure
     */
    public ParseException(final String message) {
        super(message);
    }
}
