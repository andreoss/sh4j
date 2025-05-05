/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
/**
 * Recursive-descent parser for the POSIX shell grammar and the AST node
 * types it produces. The parser consumes the token stream from
 * {@link io.github.andreoss.sh4j.lexer.Lexer} and emits
 * {@link io.github.andreoss.sh4j.parser.Node} subclasses
 * that the executor walks.
 * @since 1.0
 */
package io.github.andreoss.sh4j.parser;
