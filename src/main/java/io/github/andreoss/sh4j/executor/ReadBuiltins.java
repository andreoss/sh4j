/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Built-ins that read input lines and parse options.
 *
 * @since 1.0
 */
final class ReadBuiltins {

    /** Pattern of a valid variable name. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

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
     * @return The exit code: 0 on success, 1 when EOF came before a newline,
     *  2 for an invalid variable name
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
        final ReadText input = ReadText.read(self, raw);
        final int valid = ReadBuiltins.valid(names);
        int code;
        if (input == null) {
            code = 1;
        } else if (names.isEmpty()) {
            self.environmentMap().put("REPLY", input.text());
            code = input.status();
        } else {
            final List<String> values = new ReadSplitter(
                input, env.getOrDefault("IFS", String.format(" \t%n"))
            ).split(names.size());
            for (int vdx = 0; vdx < valid; vdx = vdx + 1) {
                self.environmentMap().put(names.get(vdx), values.get(vdx));
            }
            code = input.status();
        }
        if (valid < names.size()) {
            self.errStream().println(
                String.format("sh: read: %s: bad variable name", names.get(valid))
            );
            code = 2;
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
     * @return The exit code: 0 on an option, 1 when options end, 2 on a usage error
     */
    static int doGetopts(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final int code;
        if (args.size() < 2) {
            self.errStream().println("sh: getopts: Usage: getopts optstring var [arg...]");
            code = 2;
        } else {
            code = new Getopts(self, args).next();
        }
        return code;
    }

    private static int valid(final List<String> names) {
        int count = 0;
        while (count < names.size() && ReadBuiltins.NAME.matcher(names.get(count)).matches()) {
            count = count + 1;
        }
        return count;
    }
}
