/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Built-ins that read input lines and parse options.
 *
 * @since 1.0
 */
final class ReadBuiltins {

    @GeneratedDefensive
    private ReadBuiltins() {
    }

    /**
     * Implementation of {@code read [-r] var...}.
     * POSIX 2.14: reads one line from stdin, splits by IFS into variables.
     * The last variable receives all remaining fields.
     * If no variables are given, stores the line in {@code REPLY}.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for variable lookup
     * @return The exit code: 0 on success, 1 when EOF came before a newline
     */
    static int doRead(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final boolean raw = !args.isEmpty() && "-r".equals(args.get(0));
        final List<String> names;
        if (raw) {
            names = args.subList(1, args.size());
        } else {
            names = args;
        }
        final String input = self.readInputLine();
        final int code;
        if (input == null) {
            code = 1;
        } else if (names.isEmpty()) {
            self.environmentMap().put(
                "REPLY", ReadBuiltins.unescape(ReadBuiltins.chomp(input), raw)
            );
            code = ReadBuiltins.status(input);
        } else {
            final String ifs = env.getOrDefault("IFS", String.format(" \t%n"));
            final List<String> values = ReadBuiltins.values(
                new ReadSplitter(
                    ReadBuiltins.unescape(ReadBuiltins.chomp(input), raw), ifs
                ).split(),
                names.size(), ifs.charAt(0)
            );
            for (int vdx = 0; vdx < names.size(); vdx = vdx + 1) {
                self.environmentMap().put(names.get(vdx), values.get(vdx));
            }
            code = ReadBuiltins.status(input);
        }
        return code;
    }

    /**
     * Implementation of {@code getopts optstring var [args...]}.
     * POSIX 2.14: parses option characters from positional parameters
     * (or optional args). Uses OPTIND/OPTARG/OPTERR env vars for state.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0 if an option was found, 1 if all options processed
     */
    static int doGetopts(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final int code;
        if (args.size() < 2) {
            code = 1;
        } else {
            code = new Getopts(self, args).next();
        }
        return code;
    }

    private static String chomp(final String input) {
        String line = input;
        if (ReadBuiltins.ended(input)) {
            line = input.substring(0, input.length() - 1);
        }
        return line;
    }

    private static int status(final String input) {
        int code = 1;
        if (ReadBuiltins.ended(input)) {
            code = 0;
        }
        return code;
    }

    private static boolean ended(final String input) {
        return input.charAt(input.length() - 1) == '\n';
    }

    private static String unescape(final String line, final boolean raw) {
        final String result;
        if (raw) {
            result = line;
        } else {
            result = ReadBuiltins.processReadEscapes(line);
        }
        return result;
    }

    private static List<String> values(
        final List<String> fields, final int count, final char firstifs
    ) {
        final List<String> out = new ArrayList<>(count);
        for (int vdx = 0; vdx < count - 1; vdx = vdx + 1) {
            if (vdx < fields.size()) {
                out.add(fields.get(vdx));
            } else {
                out.add("");
            }
        }
        out.add(ReadBuiltins.joinReadFields(fields, count - 1, firstifs));
        return out;
    }

    private static String processReadEscapes(final String line) {
        final StringBuilder out = new StringBuilder(line.length());
        boolean escaped = false;
        for (int idx = 0; idx < line.length(); idx = idx + 1) {
            final char chr = line.charAt(idx);
            if (escaped) {
                out.append(chr);
                escaped = false;
            } else if (chr == '\\') {
                escaped = true;
            } else {
                out.append(chr);
            }
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private static String joinReadFields(
        final List<String> fields, final int start,
        final char firstifs
    ) {
        final StringBuilder out = new StringBuilder();
        for (int idx = start; idx < fields.size(); idx = idx + 1) {
            if (idx > start) {
                out.append(firstifs);
            }
            out.append(fields.get(idx));
        }
        return out.toString();
    }
}
