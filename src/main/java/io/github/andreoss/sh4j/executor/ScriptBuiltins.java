/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.ParseException;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Built-ins that evaluate, source and trap scripts.
 *
 * @since 1.0
 */
final class ScriptBuiltins {

    @GeneratedDefensive
    private ScriptBuiltins() {
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
        final String input = String.join(" ", args);
        final int code;
        if (input.isEmpty()) {
            code = 0;
        } else {
            code = self.execute(ScriptBuiltins.parse(self, input, "eval"));
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
     * @return The sourced script's exit status, or 1 without a file operand;
     *  an unreadable file raises a special built-in error
     */
    static int doDot(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final int code;
        if (args.isEmpty()) {
            code = 1;
        } else {
            final String path = args.get(0);
            final Optional<String> source = ScriptBuiltins.readSource(self, path);
            if (!source.isPresent()) {
                self.errStream().println(String.format("sh: %s: cannot open", path));
                throw new SpecialError(1);
            }
            code = ScriptBuiltins.runSourced(self, source.get(), args);
        }
        return code;
    }

    private static Optional<String> readSource(
        final BuiltinContext self, final String path
    ) {
        final File raw = new File(path);
        final Path rawpath;
        if (raw.isAbsolute()) {
            rawpath = raw.toPath();
        } else {
            rawpath = new File(self.currentDirectory(), path).toPath();
        }
        Optional<String> result;
        try {
            result = Optional.of(
                new String(Files.readAllBytes(rawpath), StandardCharsets.UTF_8)
            );
        } catch (final IOException ex) {
            result = Optional.empty();
        }
        return result;
    }

    private static int runSourced(
        final BuiltinContext self, final String source, final List<String> args
    ) {
        final List<String> saved = new ArrayList<>(self.positionalParamsList());
        if (args.size() > 1) {
            self.setPositionalParams(args.subList(1, args.size()));
        }
        final int code;
        try {
            code = ScriptBuiltins.executeCatchingReturn(self, source, args.get(0));
        } finally {
            self.positionalParamsList().clear();
            self.positionalParamsList().addAll(saved);
        }
        return code;
    }

    private static int executeCatchingReturn(
        final BuiltinContext self, final String source, final String path
    ) {
        final ListNode list = ScriptBuiltins.parse(self, source, path);
        int code;
        try {
            code = self.scopes().frame(() -> self.execute(list));
        } catch (final ReturnSignal ex) {
            code = ex.getCode();
        }
        return code;
    }

    private static ListNode parse(
        final BuiltinIo self, final String source, final String label
    ) {
        try {
            return new Parser(new Lexer(new CharReader(source))).parse();
        } catch (final ParseException ex) {
            self.errStream().println(
                String.format("sh: %s: syntax error: %s", label, ex.getMessage())
            );
            throw new SpecialError(2, ex);
        }
    }
}
