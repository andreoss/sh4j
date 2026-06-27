/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
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
        final Options opts = new Options(self, "unset", "fv");
        final int idx = opts.operands(args);
        final boolean removefunc = opts.has('f');
        final VariableName check = new VariableName(self, "unset");
        for (int aidx = idx; aidx < args.size(); aidx = aidx + 1) {
            final String name = args.get(aidx);
            if (removefunc) {
                self.functionsMap().remove(name);
            } else {
                self.unsetVariable(new WritableName(self).writable(check.valid(name)));
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
        final List<String> names = args.subList(
            new Options(self, "export", "p").operands(args), args.size()
        );
        final VariableName check = new VariableName(self, "export");
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
                final String name = new WritableName(self).writable(
                    check.valid(arg.substring(0, eqidx))
                );
                environment.put(name, arg.substring(eqidx + 1));
                self.markExported(name);
            } else {
                environment.putIfAbsent(check.valid(arg), "");
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
        final List<String> args = call.arguments();
        final List<String> all = args.subList(
            new Options(self, "readonly", "p").operands(args), args.size()
        );
        if (all.isEmpty()) {
            VariableBuiltins.printReadonly(self);
        } else {
            final VariableName check = new VariableName(self, "readonly");
            for (final String arg : all) {
                VariableBuiltins.markReadonly(self, check, arg);
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
        final int shifts = new Operand(self, "shift").value(args, 1);
        final List<String> pos = self.positionalParamsList();
        if (shifts > pos.size() - 1) {
            self.errStream().println("sh: shift: can't shift that many");
            throw new SpecialError(2);
        }
        pos.subList(1, 1 + shifts).clear();
        return 0;
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

    private static void markReadonly(
        final BuiltinContext self, final VariableName check, final String arg
    ) {
        final int eqidx = arg.indexOf('=');
        if (eqidx >= 0) {
            final String name = new WritableName(self).writable(
                check.valid(arg.substring(0, eqidx))
            );
            self.environmentMap().put(name, arg.substring(eqidx + 1));
            self.markReadonly(name);
        } else {
            self.markReadonly(check.valid(arg));
        }
    }
}
