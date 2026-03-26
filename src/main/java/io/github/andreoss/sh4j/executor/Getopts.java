/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

/**
 * One {@code getopts} step over the positional or given arguments, keeping
 * its state in {@code OPTIND}, {@code OPTIND_POS} and {@code OPTARG}.
 *
 * @since 1.0
 */
final class Getopts {

    /** Name of the option-index variable. */
    private static final String OPTIND = "OPTIND";

    /** Name of the in-argument position variable. */
    private static final String OPTPOS = "OPTIND_POS";

    /** Name of the option-argument variable. */
    private static final String OPTARG = "OPTARG";

    /** Name of the variable that receives an unknown option. */
    private static final String UNKNOWN = "?";

    /** Output for diagnostics. */
    private final BuiltinIo self;

    /** The live environment. */
    private final Map<String, String> environment;

    /** The option specification. */
    private final String optstring;

    /** Variable that receives the option character. */
    private final String varname;

    /** Arguments being parsed. */
    private final List<String> pargs;

    /**
     * Ctor.
     *
     * @param self The context
     * @param args The {@code getopts} arguments: optstring, name, operands
     */
    Getopts(final BuiltinContext self, final List<String> args) {
        this.self = self;
        this.environment = self.environmentMap();
        this.optstring = args.get(0);
        this.varname = args.get(1);
        this.pargs = Getopts.operands(self, args);
    }

    /**
     * Reads the next option.
     *
     * @return The exit code: 0 if an option was found, 1 when done
     */
    int next() {
        final int optind = this.optind();
        final int code;
        if (optind > this.pargs.size()) {
            this.environment.put(this.varname, Getopts.UNKNOWN);
            this.environment.put(Getopts.OPTIND, String.valueOf(this.pargs.size() + 1));
            code = 1;
        } else if ("--".equals(this.pargs.get(optind - 1))) {
            this.environment.put(Getopts.OPTIND, String.valueOf(optind + 1));
            this.environment.put(this.varname, Getopts.UNKNOWN);
            code = 1;
        } else if (Getopts.isOption(this.pargs.get(optind - 1))) {
            code = this.tail(optind);
        } else {
            this.environment.put(this.varname, Getopts.UNKNOWN);
            code = 1;
        }
        return code;
    }

    private static List<String> operands(
        final BuiltinContext self, final List<String> args
    ) {
        final List<String> result;
        if (args.size() > 2) {
            result = args.subList(2, args.size());
        } else {
            final List<String> pos = self.positionalParamsList();
            result = pos.subList(1, pos.size());
        }
        return result;
    }

    private static boolean isOption(final String arg) {
        return arg.length() >= 2 && arg.charAt(0) == '-';
    }

    private int optind() {
        int value;
        try {
            value = Integer.parseInt(this.environment.getOrDefault(Getopts.OPTIND, "1"));
        } catch (final NumberFormatException ex) {
            value = 1;
        }
        return Math.max(1, value);
    }

    private int tail(final int start) {
        final String current = this.pargs.get(start - 1);
        final int stored;
        if (this.environment.containsKey(Getopts.OPTPOS)) {
            stored = Integer.parseInt(this.environment.get(Getopts.OPTPOS));
        } else {
            stored = 1;
        }
        final int code;
        if (stored < current.length()) {
            code = this.option(new Getopts.Cursor(current, start, stored));
        } else {
            code = this.advance(current, start + 1);
        }
        return code;
    }

    private int advance(final String current, final int optind) {
        this.environment.put(Getopts.OPTIND, String.valueOf(optind));
        final int code;
        if (optind > this.pargs.size() || !Getopts.isOption(this.pargs.get(optind - 1))) {
            this.environment.put(this.varname, Getopts.UNKNOWN);
            this.environment.remove(Getopts.OPTPOS);
            code = 1;
        } else {
            this.environment.put(Getopts.OPTPOS, String.valueOf(1));
            code = this.option(new Getopts.Cursor(current, optind, 1));
        }
        return code;
    }

    private int option(final Getopts.Cursor at) {
        final char optchar = at.current().charAt(at.optpos());
        this.environment.put(Getopts.OPTPOS, String.valueOf(at.optpos() + 1));
        final int optidx = this.optstring.indexOf(optchar);
        if (optidx < 0) {
            this.illegal(optchar);
        } else if (optidx + 1 < this.optstring.length()
            && this.optstring.charAt(optidx + 1) == ':') {
            this.withArgument(at, optchar);
        } else {
            this.flag(at, optchar);
        }
        return 0;
    }

    private void illegal(final char optchar) {
        if (!this.silent()) {
            this.self.errStream().println(
                String.format("sh: illegal option -- %s", optchar)
            );
        }
        this.environment.put(this.varname, Getopts.UNKNOWN);
        this.environment.put(Getopts.OPTARG, String.valueOf(optchar));
    }

    private void flag(final Getopts.Cursor at, final char optchar) {
        this.environment.put(Getopts.OPTARG, "");
        this.environment.put(this.varname, String.valueOf(optchar));
        if (at.optpos() + 1 >= at.current().length()) {
            this.environment.put(Getopts.OPTIND, String.valueOf(at.optind() + 1));
            this.environment.remove(Getopts.OPTPOS);
        }
    }

    private void withArgument(final Getopts.Cursor at, final char optchar) {
        this.environment.put(Getopts.OPTIND, String.valueOf(at.optind() + 1));
        if (at.optpos() + 1 < at.current().length()) {
            this.environment.put(Getopts.OPTARG, at.current().substring(at.optpos() + 1));
            this.accept(optchar);
        } else if (at.optind() < this.pargs.size()) {
            this.environment.put(Getopts.OPTARG, this.pargs.get(at.optind()));
            this.environment.put(Getopts.OPTIND, String.valueOf(at.optind() + 2));
            this.accept(optchar);
        } else {
            this.missing(optchar);
        }
    }

    private void accept(final char optchar) {
        this.environment.remove(Getopts.OPTPOS);
        this.environment.put(this.varname, String.valueOf(optchar));
    }

    private void missing(final char optchar) {
        if (this.silent()) {
            this.environment.put(this.varname, ":");
        } else {
            this.self.errStream().println(
                String.format("sh: option requires an argument -- %s", optchar)
            );
            this.environment.put(this.varname, Getopts.UNKNOWN);
        }
        this.environment.put(Getopts.OPTARG, "");
        this.environment.remove(Getopts.OPTPOS);
    }

    private boolean silent() {
        return !this.optstring.isEmpty() && this.optstring.charAt(0) == ':';
    }

    /**
     * Position of the option being read.
     *
     * @param current The argument holding the option
     * @param optind One-based index of the next argument
     * @param optpos Index of the option character inside {@code current}
     * @since 1.0
     */
    private record Cursor(String current, int optind, int optpos) {
    }
}
