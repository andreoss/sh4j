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
                final String expanded = self.expandVariables(arg.substring(eqidx + 1), env);
                environment.put(name, WordExpander.removeQuotes(expanded));
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
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for variable lookup
     * @param assigns Inline assignments (unused)
     * @return The exit code: 0 on success, 1 on error
     */
    static int doReadonly(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final List<String> all = new ArrayList<>(args);
        all.addAll(assigns);
        final Map<String, String> environment = self.environmentMap();
        if (!all.isEmpty() && "-p".equals(all.get(0))) {
            all.remove(0);
        }
        if (all.isEmpty()) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                if (self.isReadonly(entry.getKey())) {
                    self.outStream().println(
                        String.format(
                            "readonly %s=%s", entry.getKey(), entry.getValue()
                        )
                    );
                }
            }
        } else {
            for (final String arg : all) {
                final int eqidx = arg.indexOf('=');
                if (eqidx >= 0) {
                    final String name = arg.substring(0, eqidx);
                    final String expanded = self.expandVariables(
                        arg.substring(eqidx + 1), env
                    );
                    environment.put(name, WordExpander.removeQuotes(expanded));
                    self.markReadonly(name);
                } else {
                    self.markReadonly(arg);
                }
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
                shifts = Integer.parseInt(self.expandVariables(args.get(0), env));
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

    /**
     * Implementation of {@code set [-efux] [--] [arg...]}.
     * POSIX 2.14: sets shell options and positional parameters.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment (unused)
     * @return The exit code: 0
     */
    static int doSet(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        String flags = self.shellFlags();
        int idx = 0;
        boolean sawdash = false;
        while (idx < args.size()) {
            final String flag = args.get(idx);
            if ("--".equals(flag)) {
                idx = idx + 1;
                sawdash = true;
                break;
            }
            if (("-o".equals(flag) || "+o".equals(flag)) && idx + 1 < args.size()) {
                final String option = args.get(idx + 1);
                final char opt = optionFlag(option);
                if (opt != 0) {
                    if ("-o".equals(flag)) {
                        if (flags.indexOf(opt) < 0) {
                            flags = flags + opt;
                        }
                    } else {
                        flags = flags.replace(String.valueOf(opt), "");
                    }
                }
                idx = idx + 2;
                continue;
            }
            if (flag.length() < 2
                || !("-".equals(flag.substring(0, 1)) || "+".equals(flag.substring(0, 1)))) {
                break;
            }
            final boolean enable = "-".equals(flag.substring(0, 1));
            for (int fidx = 1; fidx < flag.length(); fidx = fidx + 1) {
                final char chr = flag.charAt(fidx);
                if (enable && flags.indexOf(chr) < 0) {
                    flags = flags + chr;
                } else if (!enable) {
                    flags = flags.replace(String.valueOf(chr), "");
                }
            }
            idx = idx + 1;
        }
        self.setShellFlags(flags);
        if (sawdash || idx < args.size()) {
            final List<String> params = new ArrayList<>(0);
            for (int aidx = idx; aidx < args.size(); aidx = aidx + 1) {
                params.addAll(self.expandArguments(args.get(aidx), env));
            }
            self.setPositionalParams(params);
        }
        return 0;
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
        } else {
            flag = 0;
        }
        return flag;
    }

    /**
     * Implementation of {@code alias [name[=value]...]}.
     * POSIX 2.14: without args, prints all aliases.
     * With name=value, sets an alias. With plain name, prints that alias.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @param assigns The assignments
     * @return The exit code: 0 on success, 1 if any named alias is undefined
     */
    static int doAlias(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> aliases = self.aliases();
        int code = 0;
        final boolean hasargs = !args.isEmpty() || !assigns.isEmpty();
        if (!hasargs) {
            for (final Map.Entry<String, String> entry : aliases.entrySet()) {
                self.outStream().println(
                    String.format("%s='%s'", entry.getKey(), entry.getValue())
                );
            }
        } else {
            for (final String arg : args) {
                code = applyAliasArg(self, aliases, arg, code);
            }
            for (final String assign : assigns) {
                code = applyAliasArg(self, aliases, assign, code);
            }
        }
        return code;
    }

    /**
     * Implementation of {@code unalias [-a] name...}.
     * POSIX 2.14: removes named aliases, or all aliases with -a.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0 on success, 1 if any named alias doesn't exist
     */
    static int doUnalias(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final Map<String, String> aliases = self.aliases();
        int code = 0;
        boolean all = false;
        int idx = 0;
        if (!args.isEmpty() && "-a".equals(args.get(0))) {
            all = true;
            idx = 1;
        }
        if (all) {
            aliases.clear();
        } else {
            for (; idx < args.size(); idx = idx + 1) {
                final String name = args.get(idx);
                if (aliases.containsKey(name)) {
                    aliases.remove(name);
                } else {
                    code = 1;
                }
            }
        }
        return code;
    }

    private static int applyAliasArg(
        final BuiltinContext self, final Map<String, String> aliases,
        final String arg, final int code
    ) {
        int result = code;
        final int eqidx = arg.indexOf('=');
        if (eqidx >= 0) {
            String value = arg.substring(eqidx + 1);
            if (VariableBuiltins.isQuoteWrapped(value)) {
                value = value.substring(1, value.length() - 1);
            }
            aliases.put(arg.substring(0, eqidx), value);
        } else if (aliases.containsKey(arg)) {
            self.outStream().println(
                String.format("%s='%s'", arg, aliases.get(arg))
            );
        } else {
            result = 1;
        }
        return result;
    }

    private static boolean isQuoteWrapped(final String text) {
        final int edge = text.length() - 1;
        if (edge < 1) {
            return false;
        }
        final char quote = text.charAt(0);
        return text.charAt(edge) == quote
            && (quote == '\'' || quote == '"');
    }
}
