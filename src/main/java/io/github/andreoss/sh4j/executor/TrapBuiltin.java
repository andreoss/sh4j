/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

/**
 * One {@code trap [action condition ...]} call (POSIX 2.14 trap): a
 * condition that names no signal is reported as {@code bad trap}, ends the
 * call and gives status 1.
 *
 * @since 1.0
 */
final class TrapBuiltin {

    /** The context. */
    private final BuiltinContext self;

    /**
     * Ctor.
     *
     * @param self The context
     */
    TrapBuiltin(final BuiltinContext self) {
        this.self = self;
    }

    /**
     * Lists, sets or resets traps.
     *
     * @param args Arguments after the command name
     * @return The exit code: 0, or 1 for a bad condition
     */
    int run(final List<String> args) {
        final List<String> ops = args.subList(
            new Options(this.self, "trap", "").operands(args), args.size()
        );
        final int code;
        if (ops.isEmpty()) {
            for (final Map.Entry<String, String> entry : this.self.traps().entrySet()) {
                this.self.outStream().println(
                    String.format("trap -- '%s' %s", entry.getValue(), entry.getKey())
                );
            }
            code = 0;
        } else if (ops.get(0).matches("[0-9]+")) {
            code = this.apply(null, ops);
        } else if ("-".equals(ops.get(0))) {
            code = this.apply(null, ops.subList(1, ops.size()));
        } else {
            code = this.apply(ops.get(0), ops.subList(1, ops.size()));
        }
        return code;
    }

    private static String condition(final String spec) {
        final String name;
        if ("0".equals(spec) || "EXIT".equalsIgnoreCase(spec)) {
            name = "EXIT";
        } else if (spec.isEmpty()) {
            name = "0";
        } else {
            name = KillSignal.name(KillSignal.number(spec));
        }
        return name;
    }

    private int apply(final String action, final List<String> conditions) {
        int code = 0;
        int idx = 0;
        while (code == 0 && idx < conditions.size()) {
            final String spec = conditions.get(idx);
            final String name = TrapBuiltin.condition(spec);
            if (Character.isLetter(name.charAt(0))) {
                this.self.setTrap(name, action);
            } else {
                this.self.errStream().println(
                    String.format("sh: trap: %s: bad trap", spec)
                );
                code = 1;
            }
            idx += 1;
        }
        return code;
    }
}
