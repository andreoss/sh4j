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
     * @return The exit code: 0 on success, 1 on EOF
     */
    static int doRead(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        boolean raw = false;
        int idx = 0;
        if (!args.isEmpty() && "-r".equals(args.get(0))) {
            raw = true;
            idx = 1;
        }
        final List<String> names = args.subList(idx, args.size());
        final String line = self.readInputLine();
        final int code;
        if (line == null) {
            code = 1;
        } else {
            final String processed;
            if (raw) {
                processed = line;
            } else {
                processed = processReadEscapes(line);
            }
            final Map<String, String> environment = self.environmentMap();
            if (names.isEmpty()) {
                environment.put("REPLY", processed);
                code = 0;
            } else {
                final String ifs = env.getOrDefault(
                    "IFS", String.format(" \t%n")
                );
                final char firstifs = ifs.charAt(0);
                final List<String> fields = splitReadFields(
                    processed, ifs, firstifs
                );
                for (int vdx = 0; vdx < names.size(); vdx = vdx + 1) {
                    final String name = names.get(vdx);
                    if (vdx == names.size() - 1) {
                        environment.put(
                            name, joinReadFields(fields, vdx, firstifs)
                        );
                    } else if (vdx < fields.size()) {
                        environment.put(name, fields.get(vdx));
                    } else {
                        environment.put(name, "");
                    }
                }
                code = 0;
            }
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
            code = doGetoptsBody(self, args);
        }
        return code;
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

    private static List<String> splitReadFields(
        final String line, final String ifs, final char firstifs
    ) {
        final List<String> fields = new ArrayList<>(0);
        int pos = 0;
        while (pos < line.length() && ifs.indexOf(line.charAt(pos)) >= 0
            && (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) {
            pos = pos + 1;
        }
        final StringBuilder current = new StringBuilder();
        while (pos < line.length()) {
            final char chr = line.charAt(pos);
            if (chr == firstifs && firstifs != ' ' && firstifs != '\t') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (ifs.indexOf(chr) >= 0) {
                fields.add(current.toString());
                current.setLength(0);
                while (pos + 1 < line.length()
                    && ifs.indexOf(line.charAt(pos + 1)) >= 0) {
                    pos = pos + 1;
                }
            } else {
                current.append(chr);
            }
            pos = pos + 1;
        }
        final boolean trailws = current.length() == 0 && pos > 0
            && (line.charAt(pos - 1) == ' ' || line.charAt(pos - 1) == '\t');
        if (!trailws || fields.isEmpty()) {
            fields.add(current.toString());
        }
        return fields;
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

    private static int doGetoptsBody(
        final BuiltinContext self, final List<String> args
    ) {
        final String optstring = args.get(0);
        final String varname = args.get(1);
        final List<String> pargs;
        if (args.size() > 2) {
            pargs = args.subList(2, args.size());
        } else {
            final List<String> pos = self.positionalParamsList();
            pargs = pos.subList(1, pos.size());
        }
        final Map<String, String> environment = self.environmentMap();
        int optind;
        try {
            optind = Integer.parseInt(environment.getOrDefault("OPTIND", "1"));
        } catch (final NumberFormatException ex) {
            optind = 1;
        }
        if (optind < 1) {
            optind = 1;
        }
        final int early;
        if (optind > pargs.size()) {
            environment.put(varname, "?");
            environment.put("OPTIND", String.valueOf(pargs.size() + 1));
            early = 1;
        } else if ("--".equals(pargs.get(optind - 1))) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            environment.put(varname, "?");
            early = 1;
        } else if (pargs.get(optind - 1).length() < 2
            || pargs.get(optind - 1).charAt(0) != '-') {
            environment.put(varname, "?");
            early = 1;
        } else {
            early = 0;
        }
        final int code;
        if (early != 0) {
            code = early;
        } else {
            code = doGetoptsTail(self, environment, optstring, varname, pargs, optind);
        }
        return code;
    }

    private static int doGetoptsTail(
        final BuiltinContext self,
        final Map<String, String> environment,
        final String optstring,
        final String varname,
        final List<String> pargs,
        final int optindin
    ) {
        final String current = pargs.get(optindin - 1);
        int optind = optindin;
        int optpos;
        if (environment.containsKey("OPTIND_POS")) {
            optpos = Integer.parseInt(environment.get("OPTIND_POS"));
        } else {
            optpos = 1;
        }
        boolean exhausted = false;
        if (optpos >= current.length()) {
            optind = optind + 1;
            environment.put("OPTIND", String.valueOf(optind));
            if (optind > pargs.size()) {
                environment.put(varname, "?");
                environment.remove("OPTIND_POS");
                exhausted = true;
            } else if (pargs.get(optind - 1).length() < 2
                || pargs.get(optind - 1).charAt(0) != '-') {
                environment.put(varname, "?");
                environment.remove("OPTIND_POS");
                exhausted = true;
            } else {
                environment.put("OPTIND_POS", String.valueOf(1));
                optpos = 1;
            }
        }
        final int code;
        if (exhausted) {
            code = 1;
        } else {
            final char optchar = current.charAt(optpos);
            environment.put("OPTIND_POS", String.valueOf(optpos + 1));
            final int optidx = optstring.indexOf(optchar);
            if (optidx < 0) {
                if (!optstring.isEmpty() && optstring.charAt(0) == ':') {
                    environment.put(varname, "?");
                    environment.put("OPTARG", String.valueOf(optchar));
                } else {
                    self.errStream().println(
                        String.format("sh: illegal option -- %s", optchar)
                    );
                    environment.put(varname, "?");
                    environment.put("OPTARG", String.valueOf(optchar));
                }
                code = 0;
            } else {
                code = doGetoptsArgument(
                    self, environment, optstring, varname, pargs, optind,
                    optpos, current, optchar, optidx
                );
            }
        }
        return code;
    }

    private static int doGetoptsArgument(
        final BuiltinContext self,
        final Map<String, String> environment,
        final String optstring,
        final String varname,
        final List<String> pargs,
        final int optind,
        final int optpos,
        final String current,
        final char optchar,
        final int optidx
    ) {
        final boolean takesarg = optidx + 1 < optstring.length()
            && optstring.charAt(optidx + 1) == ':';
        boolean stop = false;
        if (takesarg) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            if (optpos + 1 < current.length()) {
                environment.put("OPTARG", current.substring(optpos + 1));
            } else if (optind < pargs.size()) {
                environment.put("OPTARG", pargs.get(optind));
                environment.put("OPTIND", String.valueOf(optind + 2));
            } else {
                if (!optstring.isEmpty() && optstring.charAt(0) == ':') {
                    environment.put(varname, ":");
                } else {
                    self.errStream().println(
                        String.format(
                            "sh: option requires an argument -- %s", optchar
                        )
                    );
                    environment.put(varname, "?");
                }
                environment.put("OPTARG", "");
                environment.remove("OPTIND_POS");
                stop = true;
            }
            if (!stop) {
                environment.remove("OPTIND_POS");
            }
        } else {
            environment.put("OPTARG", "");
        }
        if (!stop) {
            environment.put(varname, String.valueOf(optchar));
            if (!takesarg && optpos + 1 >= current.length()) {
                environment.put("OPTIND", String.valueOf(optind + 1));
                environment.remove("OPTIND_POS");
            }
        }
        return 0;
    }
}
