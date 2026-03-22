/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table of every shell built-in by name.
 *
 * @since 1.0
 */
final class BuiltinRegistry {

    @GeneratedDefensive
    private BuiltinRegistry() {
    }

    /**
     * Builds a fresh table of every shell built-in.
     *
     * @return Mutable map from built-in name to its implementation
     */
    static Map<String, Builtin> build() {
        final Map<String, Builtin> registry = new HashMap<>();
        registry.put("true", (self, call) -> 0);
        registry.put("false", (self, call) -> 1);
        registry.put(":", (self, call) -> 0);
        registry.put(
            "break",
            (self, call) -> {
                throw new BreakSignal(BuiltinRegistry.parseLoopLevels(call.arguments()));
            }
        );
        registry.put(
            "continue",
            (self, call) -> {
                throw new ContinueSignal(BuiltinRegistry.parseLoopLevels(call.arguments()));
            }
        );
        registry.put(
            "return",
            (self, call) -> {
                final List<String> args = call.arguments();
                int code = 0;
                if (!args.isEmpty()) {
                    try {
                        code = Integer.parseInt(args.get(0));
                    } catch (final NumberFormatException ignored) {
                        code = 0;
                    }
                }
                throw new ReturnSignal(code);
            }
        );
        registry.put(
            "unset",
            (self, call) -> VariableBuiltins.doUnset(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "export",
            (self, call) -> VariableBuiltins.doExport(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "cd",
            (self, call) -> DirectoryBuiltins.doCd(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "pwd",
            (self, call) -> DirectoryBuiltins.doPwd(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "echo",
            (self, call) -> CommandBuiltins.doEcho(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "eval",
            (self, call) -> CommandBuiltins.doEval(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "exit",
            (self, call) -> CommandBuiltins.doExit(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "type",
            (self, call) -> CommandBuiltins.doType(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "command",
            (self, call) -> CommandBuiltins.doCommand(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "wait",
            (self, call) -> JobBuiltins.doWait(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "hash",
            (self, call) -> CommandBuiltins.doHash(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "kill",
            (self, call) -> JobBuiltins.doKill(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "umask",
            (self, call) -> DirectoryBuiltins.doUmask(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "shift",
            (self, call) -> VariableBuiltins.doShift(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "read",
            (self, call) -> ReadBuiltins.doRead(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "alias",
            (self, call) -> VariableBuiltins.doAlias(
                self,
                call.arguments(),
                call.env(),
                call.assignments()
            )
        );
        registry.put(
            "unalias",
            (self, call) -> VariableBuiltins.doUnalias(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "getopts",
            (self, call) -> ReadBuiltins.doGetopts(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "jobs",
            (self, call) -> JobBuiltins.doJobs(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "bg",
            (self, call) -> JobBuiltins.doBg(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "fg",
            (self, call) -> JobBuiltins.doFg(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "fc",
            (self, call) -> HistoryBuiltins.doFc(
                self,
                call.arguments(),
                call.env(),
                call.assignments()
            )
        );
        registry.put(
            "set",
            (self, call) -> VariableBuiltins.doSet(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "readonly",
            (self, call) -> VariableBuiltins.doReadonly(
                self,
                call.arguments(),
                call.env(),
                call.assignments()
            )
        );
        registry.put(
            "trap",
            (self, call) -> CommandBuiltins.doTrap(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "times",
            (self, call) -> CommandBuiltins.doTimes(
                self,
                call.env()
            )
        );
        registry.put(
            ".",
            (self, call) -> CommandBuiltins.doDot(
                self,
                call.arguments(),
                call.env()
            )
        );
        return registry;
    }

    private static int parseLoopLevels(final List<String> args) {
        int levels = 1;
        if (!args.isEmpty()) {
            try {
                final int parsed = Integer.parseInt(args.get(0));
                if (parsed > 0) {
                    levels = parsed;
                }
            } catch (final NumberFormatException ignored) {
                levels = 1;
            }
        }
        return levels;
    }
}
