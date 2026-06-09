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
        BuiltinRegistry.addControl(registry);
        BuiltinRegistry.addVariable(registry);
        BuiltinRegistry.addDirectory(registry);
        BuiltinRegistry.addCommand(registry);
        BuiltinRegistry.addJob(registry);
        BuiltinRegistry.addRead(registry);
        BuiltinRegistry.addHistory(registry);
        return registry;
    }

    private static void addControl(final Map<String, Builtin> registry) {
        registry.put("true", (self, call) -> 0);
        registry.put("false", (self, call) -> 1);
        registry.put(":", (self, call) -> 0);
        registry.put(
            "break",
            (self, call) -> {
                self.scopes().loop(
                    new BreakSignal(BuiltinRegistry.parseLoopLevels(call.arguments())),
                    "break", self
                );
                return 0;
            }
        );
        registry.put(
            "continue",
            (self, call) -> {
                self.scopes().loop(
                    new ContinueSignal(BuiltinRegistry.parseLoopLevels(call.arguments())),
                    "continue", self
                );
                return 0;
            }
        );
        registry.put(
            "return",
            (self, call) -> {
                throw self.scopes().leave(
                    new Operand(self, "return").status(call.arguments()), self
                );
            }
        );
    }

    private static void addVariable(final Map<String, Builtin> registry) {
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
            "shift",
            (self, call) -> VariableBuiltins.doShift(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "alias",
            AliasBuiltins::doAlias
        );
        registry.put(
            "unalias",
            (self, call) -> AliasBuiltins.doUnalias(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "set",
            (self, call) -> new SetCommand(self, call.arguments()).run(call.env())
        );
        registry.put(
            "readonly",
            VariableBuiltins::doReadonly
        );
    }

    private static void addDirectory(final Map<String, Builtin> registry) {
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
            "umask",
            (self, call) -> DirectoryBuiltins.doUmask(
                self,
                call.arguments(),
                call.env()
            )
        );
    }

    private static void addCommand(final Map<String, Builtin> registry) {
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
            (self, call) -> ScriptBuiltins.doEval(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "exit",
            (self, call) -> {
                throw new ExitSignal(
                    new Operand(self, "exit").value(
                        call.arguments(),
                        self.scopes().status(Integer.parseInt(self.getSpecialParam('?')))
                    )
                );
            }
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
            "hash",
            (self, call) -> CommandBuiltins.doHash(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "trap",
            (self, call) -> new TrapBuiltin(self).run(call.arguments())
        );
        registry.put(
            "times",
            (self, call) -> ScriptBuiltins.doTimes(
                self,
                call.env()
            )
        );
        registry.put(
            ".",
            (self, call) -> ScriptBuiltins.doDot(
                self,
                call.arguments(),
                call.env()
            )
        );
    }

    private static void addJob(final Map<String, Builtin> registry) {
        registry.put(
            "wait",
            (self, call) -> SignalBuiltins.doWait(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "kill",
            (self, call) -> SignalBuiltins.doKill(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "jobs",
            (self, call) -> new JobsBuiltin(self).run(call.arguments())
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
    }

    private static void addRead(final Map<String, Builtin> registry) {
        registry.put(
            "read",
            (self, call) -> ReadBuiltins.doRead(
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
    }

    private static void addHistory(final Map<String, Builtin> registry) {
        registry.put(
            "fc",
            HistoryBuiltins::doFc
        );
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
