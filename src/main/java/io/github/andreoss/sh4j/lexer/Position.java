/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * A 1-based source position.
 *
 * @param line Source line
 * @param column Source column
 * @since 1.0
 */
public record Position(int line, int column) {
}
