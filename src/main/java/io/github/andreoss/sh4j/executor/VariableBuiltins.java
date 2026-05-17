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
 * Built-ins that set, export, unset and alias names.
 *
 * @since 1.0
 */
final class VariableBuiltins {

    @GeneratedDefensive
    private VariableBuiltins() {
    }

    /**
     * Implementation of {@code unset NAME ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doUnset(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        boolean removefunc = false;
        int idx = 0;
        while (idx < args.size() && args.get(idx).startsWith("-")) {
            if ("-f".equals(args.get(idx))) {
                removefunc = true;
            } else if (!"-v".equals(args.get(idx)) && !"--".equals(args.get(idx))) {
                break;
            }
            idx = idx + 1;
        }
        for (int aidx = idx; aidx < args.size(); aidx = aidx + 1) {
            final String name = args.get(aidx);
            if (removefunc) {
                self.functionsMap().remove(name);
            } else if (!self.unsetVariable(name)) {
                self.errStream().println(String.format("sh: %s: readonly variable", name));
                throw new ExitSignal(1);
            }
        }
        return 0;
    }

    /**
     * Implementation of {@code export NAME[=VALUE] ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doExport(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final Map<String, String> environment = self.environmentMap();
        final List<String> names = new ArrayList<>(args);
        if (!names.isEmpty() && "-p".equals(names.get(0))) {
            names.clear();
        }
        if (names.isEmpty()) {
            for (final Map.Entry<String, String> entry : self.exportedEnvironment().entrySet()) {
                self.outStream().println(
                    String.format("export %s=%s", entry.getKey(), entry.getValue())
                );
            }
        }
        for (final String arg : names) {
            final int eqidx = arg.indexOf('=');
            if (eqidx >= 0) {
                final String name = arg.substring(0, eqidx);
                environment.put(name, arg.substring(eqidx + 1));
                self.markExported(name);
            } else {
                environment.putIfAbsent(arg, "");
                self.markExported(arg);
            }
        }
        return 0;
    }

    /**
     * Implementation of {@code readonly [-p] [name[=value]...]}.
     * POSIX 2.14: marks variables as read-only.
     *
     * @param self Builtin context
     * @param call Arguments, environment and inline assignments
     * @return The exit code: 0 on success, 1 on error
     */
    static int doReadonly(final BuiltinContext self, final Invocation call) {
        final List<String> all = new ArrayList<>(call.arguments());
        if (!all.isEmpty() && "-p".equals(all.get(0))) {
            all.remove(0);
        }
        if (all.isEmpty()) {
            VariableBuiltins.printReadonly(self);
        } else {
            for (final String arg : all) {
                VariableBuiltins.markReadonly(self, arg);
            }
        }
        return 0;
    }

    /**
     * Implementation of {@code shift [N]}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doShift(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int shifts = 1;
        int code = 0;
        if (!args.isEmpty()) {
            try {
                shifts = Integer.parseInt(args.get(0));
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        if (code == 0 && shifts < 0) {
            code = 1;
        }
        if (code == 0 && shifts > 0) {
            final List<String> pos = self.positionalParamsList();
            if (shifts > pos.size() - 1) {
                code = 1;
            } else {
                pos.subList(1, 1 + shifts).clear();
            }
        }
        return code;
    }

    private static void printReadonly(final BuiltinContext self) {
        for (final Map.Entry<String, String> entry : self.environmentMap().entrySet()) {
            if (self.isReadonly(entry.getKey())) {
                self.outStream().println(
                    String.format("readonly %s=%s", entry.getKey(), entry.getValue())
                );
            }
        }
    }

    private static void markReadonly(final BuiltinContext self, final String arg) {
        final int eqidx = arg.indexOf('=');
        if (eqidx >= 0) {
            final String name = arg.substring(0, eqidx);
            self.environmentMap().put(name, arg.substring(eqidx + 1));
            self.markReadonly(name);
        } else {
            self.markReadonly(arg);
        }
    }
}
