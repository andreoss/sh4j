/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
/**
 * Tree-walking executor for the AST produced by {@link io.github.andreoss.sh4j.parser.Parser}.
 * Splits responsibility across cooperating classes:
 * {@link io.github.andreoss.sh4j.executor.Executor} (top-level dispatch),
 * {@link io.github.andreoss.sh4j.executor.WordExpander} (POSIX 2.6 word expansion),
 * {@link io.github.andreoss.sh4j.executor.ArithmeticEvaluator} (POSIX 2.6.4 arithmetic),
 * {@link io.github.andreoss.sh4j.executor.RedirectionHandler} (POSIX 2.7 redirections),
 * and {@link io.github.andreoss.sh4j.executor.BuiltinDispatcher} (POSIX 2.14 built-ins).
 * @since 1.0
 */
package io.github.andreoss.sh4j.executor;
