/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One {@code set [-efux] [-o name] [--] [arg...]} call.
 * POSIX 2.14: sets shell options and positional parameters.
 *
 * @since 1.0
 */
final class SetCommand {

    /** The context. */
    private final BuiltinContext self;

    /** Arguments after the command name. */
    private final List<String> args;

    /** Shell flags being built. */
    private String flags;

    /** Index of the next unread argument. */
    private int idx;

    /** True once {@code --} was read. */
    private boolean sawdash;

    /** True once the options end. */
    private boolean done;

    /**
     * Ctor.
     *
     * @param self The context
     * @param args Arguments after the command name
     */
    SetCommand(final BuiltinContext self, final List<String> args) {
        this(self, args, self.shellFlags());
    }

    /**
     * Ctor.
     *
     * @param self The context
     * @param args Arguments after the command name
     * @param flags The current shell flags
     */
    private SetCommand(
        final BuiltinContext self, final List<String> args, final String flags
    ) {
        this.self = self;
        this.args = args;
        this.flags = flags;
        this.idx = 0;
        this.sawdash = false;
        this.done = false;
    }

    /**
     * Applies the options and positional parameters.
     *
     * @param env Environment for expanding the parameters
     * @return The exit code: 0
     */
    int run(final Map<String, String> env) {
        while (!this.done && this.idx < this.args.size()) {
            this.step(this.args.get(this.idx));
        }
        this.self.setShellFlags(this.flags);
        if (this.sawdash || this.idx < this.args.size()) {
            final List<String> params = new ArrayList<>(0);
            for (int aidx = this.idx; aidx < this.args.size(); aidx = aidx + 1) {
                params.addAll(this.self.expandArguments(this.args.get(aidx), env));
            }
            this.self.setPositionalParams(params);
        }
        return 0;
    }

    private static boolean isShort(final String flag) {
        return flag.length() >= 2 && (flag.charAt(0) == '-' || flag.charAt(0) == '+');
    }

    private static char optionFlag(final String option) {
        final char flag;
        if ("errexit".equals(option)) {
            flag = 'e';
        } else if ("nounset".equals(option)) {
            flag = 'u';
        } else if ("noglob".equals(option)) {
            flag = 'f';
        } else if ("xtrace".equals(option)) {
            flag = 'x';
        } else if ("noexec".equals(option)) {
            flag = 'n';
        } else if ("verbose".equals(option)) {
            flag = 'v';
        } else if ("allexport".equals(option)) {
            flag = 'a';
        } else if ("noclobber".equals(option)) {
            flag = 'C';
        } else {
            flag = 0;
        }
        return flag;
    }

    private void step(final String flag) {
        if ("--".equals(flag)) {
            this.idx += 1;
            this.sawdash = true;
            this.done = true;
        } else if (("-o".equals(flag) || "+o".equals(flag)) && this.idx + 1 < this.args.size()) {
            this.named("-o".equals(flag), SetCommand.optionFlag(this.args.get(this.idx + 1)));
            this.idx += 2;
        } else if (SetCommand.isShort(flag)) {
            this.letters(flag);
            this.idx += 1;
        } else {
            this.done = true;
        }
    }

    private void named(final boolean enable, final char opt) {
        if (opt != 0) {
            this.toggle(enable, opt);
        }
    }

    private void letters(final String flag) {
        final boolean enable = flag.charAt(0) == '-';
        for (int fidx = 1; fidx < flag.length(); fidx = fidx + 1) {
            this.toggle(enable, flag.charAt(fidx));
        }
    }

    private void toggle(final boolean enable, final char opt) {
        final String letter = String.valueOf(opt);
        if (enable && this.flags.indexOf(opt) < 0) {
            this.flags = this.flags.concat(letter);
        } else if (!enable) {
            this.flags = this.flags.replace(letter, "");
        }
    }
}
