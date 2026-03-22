/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        boolean firstfield = true;
        for (int idx = start; idx < args.size(); idx = idx + 1) {
            for (final String field : self.expandArguments(args.get(idx), env)) {
                if (!firstfield) {
                    self.outStream().print(" ");
                }
                self.outStream().print(field);
                firstfield = false;
            }
        }
        if (newline) {
            self.outStream().println();
        }
        return 0;
    }

    /**
     * Implementation of {@code eval STRING ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doEval(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final StringBuilder buf = new StringBuilder();
        for (final String arg : args) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(WordExpander.removeQuotes(self.expandVariables(arg, env)));
        }
        final String input = buf.toString();
        final int code;
        if (input.isEmpty()) {
            code = 0;
        } else {
            code = self.execute(
                new Parser(new Lexer(new CharReader(input))).parse()
            );
        }
        return code;
    }

    /**
     * Implementation of {@code exit [STATUS]}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doExit(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        if (!args.isEmpty()) {
            try {
                code = Integer.parseInt(self.expandVariables(args.get(0), env));
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        throw new ExitSignal(code);
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
        for (final String arg : args) {
            final String expanded = self.expandVariables(arg, env);
            if (self.functionsMap().containsKey(expanded)) {
                self.outStream().println(String.format("%s is a function", expanded));
            } else if (self.isBuiltinName(expanded)) {
                self.outStream().println(String.format("%s is a shell built-in", expanded));
            } else {
                final String resolved = self.resolveCommand(expanded);
                if (resolved == null) {
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
        final int code;
        int idx = 0;
        boolean verbose = false;
        boolean locate = false;
        while (idx < args.size() && args.get(idx).startsWith("-")) {
            final String flag = args.get(idx);
            if ("-v".equals(flag)) {
                locate = true;
            } else if ("-V".equals(flag)) {
                verbose = true;
            }
            idx += 1;
        }
        if (idx >= args.size()) {
            if (locate || verbose) {
                code = 1;
            } else {
                code = 0;
            }
        } else {
            final String name = self.expandVariables(args.get(idx), env);
            if (locate || verbose) {
                code = describeCommand(self, name, verbose);
            } else {
                code = self.executeBypassingFunctions(
                    name, args.subList(idx + 1, args.size()), env
                );
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
                code = 1;
            } else if (verbose) {
                self.outStream().println(String.format("%s is %s", name, resolved));
            } else {
                self.outStream().println(resolved);
            }
        }
        return code;
    }

    static int doHash(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        for (final String arg : args) {
            if ("-r".equals(arg)) {
                continue;
            }
            final String name = self.expandVariables(arg, env);
            if (!self.isBuiltinName(name) && self.resolveCommand(name) == null) {
                code = 127;
            }
        }
        return code;
    }

    /**
     * Implementation of {@code trap [action signal ...]}.
     * Only the EXIT trap is executed by the executor; other signals are
     * recorded but the JDK does not expose portable signal handlers.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0
     */
    static int doTrap(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final int code;
        if (args.isEmpty()) {
            for (final Map.Entry<String, String> entry : self.traps().entrySet()) {
                self.outStream().println(
                    String.format("trap -- '%s' %s", entry.getValue(), entry.getKey())
                );
            }
            code = 0;
        } else if ("-".equals(args.get(0))) {
            for (int idx = 1; idx < args.size(); idx = idx + 1) {
                self.setTrap(args.get(idx).toUpperCase(Locale.ROOT), null);
            }
            code = 0;
        } else {
            final String action = WordExpander.removeQuotes(args.get(0));
            for (int idx = 1; idx < args.size(); idx = idx + 1) {
                self.setTrap(
                    args.get(idx).toUpperCase(Locale.ROOT), action
                );
            }
            code = 0;
        }
        return code;
    }

    /**
     * Implementation of {@code times}.
     *
     * @param self The context
     * @param env The environment
     * @return The exit code: 0
     */
    static int doTimes(
        final BuiltinContext self,
        final Map<String, String> env
    ) {
        self.outStream().println("0m0.000s 0m0.000s");
        self.outStream().println("0m0.000s 0m0.000s");
        return 0;
    }

    /**
     * Implementation of {@code . file [args...]}.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return The sourced script's exit status, or 1 on IO/parse failure
     */
    static int doDot(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code;
        if (args.isEmpty()) {
            code = 1;
        } else {
            final String path = WordExpander.removeQuotes(
                self.expandVariables(args.get(0), env)
            );
            try {
                final File raw = new File(path);
                final Path rawpath;
                if (raw.isAbsolute()) {
                    rawpath = raw.toPath();
                } else {
                    rawpath = new File(self.currentDirectory(), path).toPath();
                }
                final String source = new String(
                    Files.readAllBytes(rawpath), StandardCharsets.UTF_8
                );
                final List<String> saved = new ArrayList<>(self.positionalParamsList());
                if (args.size() > 1) {
                    self.setPositionalParams(args.subList(1, args.size()));
                }
                try {
                    try {
                        code = self.execute(
                            new Parser(new Lexer(new CharReader(source))).parse()
                        );
                    } catch (final ReturnSignal ex) {
                        code = ex.getCode();
                    }
                } finally {
                    self.positionalParamsList().clear();
                    self.positionalParamsList().addAll(saved);
                }
            } catch (final IOException ex) {
                self.errStream().println(String.format("sh: %s: cannot open", path));
                code = 1;
            }
        }
        return code;
    }
}
