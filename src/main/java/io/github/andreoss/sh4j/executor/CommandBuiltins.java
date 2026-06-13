/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.List;
import java.util.Map;

/**
 * Built-ins that run, describe or leave commands.
 *
 * @since 1.0
 */
final class CommandBuiltins {

    @GeneratedDefensive
    private CommandBuiltins() {
    }

    /**
     * Implementation of {@code echo [-n] ARG ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doEcho(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        boolean newline = true;
        int start = 0;
        if (!args.isEmpty() && "-n".equals(args.get(0))) {
            newline = false;
            start = 1;
        }
        self.outStream().print(String.join(" ", args.subList(start, args.size())));
        if (newline) {
            self.outStream().println();
        }
        return 0;
    }

    /**
     * Implementation of {@code type NAME ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doType(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        for (final String expanded : args) {
            if (self.functionsMap().containsKey(expanded)) {
                self.outStream().println(String.format("%s is a function", expanded));
            } else if (self.isBuiltinName(expanded)) {
                self.outStream().println(String.format("%s is a shell built-in", expanded));
            } else {
                final String resolved = self.resolveCommand(expanded);
                if (resolved == null) {
                    self.errStream().println(String.format("sh: type: %s: not found", expanded));
                    code = 1;
                } else {
                    self.outStream().println(String.format("%s is %s", expanded, resolved));
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code command [-v|-V|-p] name [args...]}.
     * POSIX 2.14. {@code -v} prints location of NAME; {@code -V}
     * prints a verbose description; without flags, invokes NAME
     * bypassing function lookup. {@code -p} (default-PATH) is
     * accepted but treated as a no-op (the configured PATH is used).
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return Resolved command exit code (1 if NAME not found with -v/-V)
     */
    static int doCommand(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final Options opts = new Options(self, "command", "pvV");
        final int code;
        final boolean valid = opts.read(args);
        final int idx = opts.first();
        final boolean verbose = opts.has('V');
        final boolean locate = opts.has('v');
        if (valid && idx < args.size()) {
            final String name = args.get(idx);
            if (locate || verbose) {
                code = describeCommand(self, name, verbose);
            } else {
                code = self.executeBypassingFunctions(
                    name, args.subList(idx + 1, args.size()), env
                );
            }
        } else if (valid && (locate || verbose)) {
            code = 1;
        } else if (valid) {
            code = 0;
        } else {
            code = 2;
        }
        return code;
    }

    static int doHash(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        for (final String name : args) {
            if ("-r".equals(name)) {
                continue;
            }
            if (!self.isBuiltinName(name) && self.resolveCommand(name) == null) {
                self.errStream().println(String.format("sh: hash: %s: not found", name));
                code = 1;
            }
        }
        return code;
    }

    private static int describeCommand(
        final BuiltinContext self, final String name, final boolean verbose
    ) {
        int code = 0;
        if (self.functionsMap().containsKey(name)) {
            if (verbose) {
                self.outStream().println(String.format("%s is a function", name));
            } else {
                self.outStream().println(name);
            }
        } else if (self.isBuiltinName(name)) {
            if (verbose) {
                self.outStream().println(String.format("%s is a shell built-in", name));
            } else {
                self.outStream().println(name);
            }
        } else {
            final String resolved = self.resolveCommand(name);
            if (resolved == null) {
                CommandBuiltins.missing(self, name, verbose);
                code = 1;
            } else if (verbose) {
                self.outStream().println(String.format("%s is %s", name, resolved));
            } else {
                self.outStream().println(resolved);
            }
        }
        return code;
    }

    private static void missing(
        final BuiltinIo self, final String name, final boolean verbose
    ) {
        if (verbose) {
            self.errStream().println(String.format("sh: command: %s: not found", name));
        }
    }
}
