/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.Parser;

/**
 * Runs the action of the {@code EXIT} trap.
 *
 * @since 1.0
 */
final class ExitTrap {

    @GeneratedDefensive
    private ExitTrap() {
    }

    /**
     * Runs the trap action, reporting a failure on stderr.
     *
     * @param shell The shell that runs the action
     * @param action The trap action, null when no trap is set
     */
    static void run(final BuiltinContext shell, final String action) {
        if (action != null && !action.isEmpty() && !"-".equals(action)) {
            Attempt.failure(
                () -> shell.execute(new Parser(new Lexer(new CharReader(action))).parse())
            ).ifPresent(
                ex -> shell.errStream().println(
                    String.format("sh: trap: %s", ex.getMessage())
                )
            );
        }
    }
}
