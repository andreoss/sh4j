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

    /** The context. */
    private final BuiltinContext self;

    /** The {@code getopts} arguments: optstring, name, operands. */
    private final List<String> args;

    /**
     * Ctor.
     *
     * @param self The context
     * @param args The {@code getopts} arguments: optstring, name, operands
     */
    Getopts(final BuiltinContext self, final List<String> args) {
        this.self = self;
        this.args = args;
    }

    /**
     * Reads the next option.
     *
     * @return The exit code: 0 if an option was found, 1 when done
     */
    int next() {
        final int optind = this.optind();
        final int code;
        if (optind > this.operands().size()) {
            this.env().put(this.name(), Getopts.UNKNOWN);
            this.env().put(Getopts.OPTIND, String.valueOf(this.operands().size() + 1));
            code = 1;
        } else if ("--".equals(this.operands().get(optind - 1))) {
            this.env().put(Getopts.OPTIND, String.valueOf(optind + 1));
            this.env().put(this.name(), Getopts.UNKNOWN);
            code = 1;
        } else if (Getopts.isOption(this.operands().get(optind - 1))) {
            code = this.tail(optind);
        } else {
            this.env().put(this.name(), Getopts.UNKNOWN);
            code = 1;
        }
        return code;
    }

    private List<String> operands() {
        final List<String> result;
        if (this.args.size() > 2) {
            result = this.args.subList(2, this.args.size());
        } else {
            final List<String> pos = this.self.positionalParamsList();
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
            value = Integer.parseInt(this.env().getOrDefault(Getopts.OPTIND, "1"));
        } catch (final NumberFormatException ex) {
            value = 1;
        }
        return Math.max(1, value);
    }

    private int tail(final int start) {
        final String current = this.operands().get(start - 1);
        final int stored;
        if (this.env().containsKey(Getopts.OPTPOS)) {
            stored = Integer.parseInt(this.env().get(Getopts.OPTPOS));
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
        this.env().put(Getopts.OPTIND, String.valueOf(optind));
        final int code;
        if (optind > this.operands().size() || !Getopts.isOption(this.operands().get(optind - 1))) {
            this.env().put(this.name(), Getopts.UNKNOWN);
            this.env().remove(Getopts.OPTPOS);
            code = 1;
        } else {
            this.env().put(Getopts.OPTPOS, String.valueOf(1));
            code = this.option(new Getopts.Cursor(current, optind, 1));
        }
        return code;
    }

    private int option(final Getopts.Cursor cursor) {
        final char optchar = cursor.current().charAt(cursor.optpos());
        this.env().put(Getopts.OPTPOS, String.valueOf(cursor.optpos() + 1));
        final int optidx = this.spec().indexOf(optchar);
        if (optidx < 0) {
            this.illegal(optchar);
        } else if (optidx + 1 < this.spec().length()
            && this.spec().charAt(optidx + 1) == ':') {
            this.withArgument(cursor, optchar);
        } else {
            this.flag(cursor, optchar);
        }
        return 0;
    }

    private void illegal(final char optchar) {
        if (!this.silent()) {
            this.self.errStream().println(
                String.format("sh: illegal option -- %s", optchar)
            );
        }
        this.env().put(this.name(), Getopts.UNKNOWN);
        this.env().put(Getopts.OPTARG, String.valueOf(optchar));
    }

    private void flag(final Getopts.Cursor cursor, final char optchar) {
        this.env().put(Getopts.OPTARG, "");
        this.env().put(this.name(), String.valueOf(optchar));
        if (cursor.optpos() + 1 >= cursor.current().length()) {
            this.env().put(Getopts.OPTIND, String.valueOf(cursor.optind() + 1));
            this.env().remove(Getopts.OPTPOS);
        }
    }

    private void withArgument(final Getopts.Cursor cursor, final char optchar) {
        this.env().put(Getopts.OPTIND, String.valueOf(cursor.optind() + 1));
        if (cursor.optpos() + 1 < cursor.current().length()) {
            this.env().put(Getopts.OPTARG, cursor.current().substring(cursor.optpos() + 1));
            this.accept(optchar);
        } else if (cursor.optind() < this.operands().size()) {
            this.env().put(Getopts.OPTARG, this.operands().get(cursor.optind()));
            this.env().put(Getopts.OPTIND, String.valueOf(cursor.optind() + 2));
            this.accept(optchar);
        } else {
            this.missing(optchar);
        }
    }

    private void accept(final char optchar) {
        this.env().remove(Getopts.OPTPOS);
        this.env().put(this.name(), String.valueOf(optchar));
    }

    private void missing(final char optchar) {
        if (this.silent()) {
            this.env().put(this.name(), ":");
        } else {
            this.self.errStream().println(
                String.format("sh: option requires an argument -- %s", optchar)
            );
            this.env().put(this.name(), Getopts.UNKNOWN);
        }
        this.env().put(Getopts.OPTARG, "");
        this.env().remove(Getopts.OPTPOS);
    }

    private Map<String, String> env() {
        return this.self.environmentMap();
    }

    private String spec() {
        return this.args.get(0);
    }

    private String name() {
        return this.args.get(1);
    }

    private boolean silent() {
        return !this.spec().isEmpty() && this.spec().charAt(0) == ':';
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
