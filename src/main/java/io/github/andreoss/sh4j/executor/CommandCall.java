/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.util.Map;

/**
 * One named-command call: the command, its expanded name, the command
 * environment, and whether functions and aliases are bypassed.
 *
 * @param cmd The command with its arguments and redirections
 * @param name The command name
 * @param env The command environment
 * @param bypass True to skip alias and function lookup
 * @since 1.0
 */
record CommandCall(
    SimpleCommand cmd, String name, Map<String, String> env, boolean bypass
) {
}
