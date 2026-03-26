/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Optional;

/**
 * Parsed options of one {@code fc} call.
 *
 * @since 1.0
 */
final class FcOptions {

    /** Arguments after the command name. */
    private final List<String> args;

    /** Inline assignments attached to the call. */
    private final List<String> assigns;

    /** Index of the next unread argument. */
    private int idx;

    /** True when entries are listed. */
    private boolean listmode;

    /** True when listed entries omit their numbers. */
    private boolean nonumbers;

    /** The {@code pat=rep} substitution, when given. */
    private Optional<String> substitute;

    /** The first parse error, when any. */
    private Optional<String> error;

    /**
     * Ctor.
     *
     * @param args Arguments after the command name
     * @param assigns Inline assignments attached to the call
     */
    FcOptions(final List<String> args, final List<String> assigns) {
        this.args = args;
        this.assigns = assigns;
        this.idx = 0;
        this.listmode = true;
        this.nonumbers = false;
        this.substitute = Optional.empty();
        this.error = Optional.empty();
    }

    /**
     * Reads every leading option.
     *
     * @return This, parsed
     */
    FcOptions parse() {
        while (this.error.isEmpty() && this.idx < this.args.size()
            && FcOptions.isOption(this.args.get(this.idx))) {
            this.flags(this.args.get(this.idx));
            this.idx += 1;
        }
        return this;
    }

    /**
     * Returns the first parse error.
     *
     * @return The error message, when parsing failed
     */
    Optional<String> error() {
        return this.error;
    }

    /**
     * Tells whether entries are listed.
     *
     * @return True in list mode
     */
    boolean listing() {
        return this.listmode;
    }

    /**
     * Tells whether listed entries omit their numbers.
     *
     * @return True when {@code -n} was given
     */
    boolean unnumbered() {
        return this.nonumbers;
    }

    /**
     * Returns the {@code pat=rep} substitution.
     *
     * @return The substitution, when {@code -s} was given
     */
    Optional<String> substitution() {
        return this.substitute;
    }

    /**
     * Returns the arguments after the options.
     *
     * @return The {@code first} and {@code last} operands, when given
     */
    List<String> operands() {
        return this.args.subList(Math.min(this.idx, this.args.size()), this.args.size());
    }

    private static boolean isOption(final String arg) {
        return arg.startsWith("-") && !arg.matches("-\\d+");
    }

    private void flags(final String flag) {
        int fidx = 1;
        while (this.error.isEmpty() && fidx < flag.length()) {
            fidx = this.option(flag.charAt(fidx), fidx, flag.length());
        }
    }

    private int option(final char chr, final int fidx, final int length) {
        int next = fidx + 1;
        if (chr == 'l') {
            this.listmode = true;
        } else if (chr == 'n') {
            this.nonumbers = true;
        } else if (chr == 'e') {
            this.editor();
        } else if (chr == 's') {
            this.substitution(this.idx + 1);
            next = length;
        } else {
            this.error = Optional.of(String.format("sh: fc: unknown option -%s", chr));
        }
        return next;
    }

    private void editor() {
        this.idx += 1;
        if (this.idx >= this.args.size()) {
            this.error = Optional.of("sh: fc: -e requires argument");
        }
    }

    private void substitution(final int pos) {
        this.listmode = false;
        if (pos < this.args.size() && this.args.get(pos).contains("=")) {
            this.substitute = Optional.of(this.args.get(pos));
            this.idx = pos;
        } else if (this.assigns.isEmpty()) {
            this.error = Optional.of("sh: fc: -s requires pat=rep");
        } else {
            this.substitute = Optional.of(this.assigns.get(0));
        }
    }
}
