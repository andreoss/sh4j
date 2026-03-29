/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

/**
 * Resolves special, positional, and named shell parameters.
 *
 * @since 1.0
 */
final class Parameters {

    /** The shell whose parameters are resolved. */
    private final BuiltinContext shell;

    /** Exit status of the most recent foreground command. */
    private final int status;

    /**
     * Ctor.
     *
     * @param shell The shell whose parameters are resolved
     * @param status Exit status of the most recent foreground command
     */
    Parameters(final BuiltinContext shell, final int status) {
        this.shell = shell;
        this.status = status;
    }

    /**
     * Replaces the positional parameters, keeping {@code $0}.
     *
     * @param params The live positional parameters ({@code $0} first)
     * @param incoming The new parameters after {@code $0}
     */
    static void replace(final List<String> params, final List<String> incoming) {
        final String zeroth;
        if (params.isEmpty()) {
            zeroth = "";
        } else {
            zeroth = params.get(0);
        }
        params.clear();
        params.add(zeroth);
        params.addAll(incoming);
    }

    /**
     * Looks up a variable or parameter by name.
     *
     * @param name The variable or parameter name
     * @param env The environment to look named variables up in
     * @return The value, or null when unset
     */
    String lookup(final String name, final Map<String, String> env) {
        final String result;
        if (name.isEmpty()) {
            result = "";
        } else if (name.length() == 1) {
            result = this.sole(name, env);
        } else {
            result = this.named(name, env);
        }
        return result;
    }

    /**
     * Returns the value of a one-character special parameter.
     *
     * @param name The parameter character
     * @return The value, or null when the character is not special
     */
    String special(final char name) {
        final List<String> params = this.shell.positionalParamsList();
        return switch (name) {
            case '?' -> String.valueOf(this.status);
            case '-' -> this.shell.shellFlags();
            case '$' -> String.valueOf(ProcessHandle.current().pid());
            case '!' -> String.valueOf(this.shell.lastBackgroundPid());
            case '#' -> String.valueOf(params.size() - 1);
            case '@', '*' -> this.joined(params);
            default -> Parameters.digit(params, name);
        };
    }

    private String joined(final List<String> params) {
        final String joined;
        if (params.size() <= 1) {
            joined = "";
        } else {
            joined = String.join(
                Parameters.separator(this.shell.environmentMap().get("IFS")),
                params.subList(1, params.size())
            );
        }
        return joined;
    }

    private String named(final String name, final Map<String, String> env) {
        final String value;
        if (name.chars().allMatch(Character::isDigit)) {
            value = this.positional(Integer.parseInt(name));
        } else {
            value = this.variable(name, env);
        }
        return value;
    }

    private String variable(final String name, final Map<String, String> env) {
        final String value = env.get(name);
        if (value == null && this.shell.shellFlags().indexOf('u') >= 0) {
            this.shell.errStream().println(
                String.format("sh: %s: parameter not set", name)
            );
            throw new ExitSignal(1);
        }
        return value;
    }

    private String positional(final int idx) {
        final List<String> params = this.shell.positionalParamsList();
        String value = null;
        if (idx < params.size()) {
            value = params.get(idx);
        }
        return value;
    }

    private String sole(final String name, final Map<String, String> env) {
        final String special = this.special(name.charAt(0));
        final String value;
        if (special == null) {
            value = this.named(name, env);
        } else {
            value = special;
        }
        return value;
    }

    private static String separator(final String ifs) {
        final String separator;
        if (ifs == null) {
            separator = " ";
        } else if (ifs.isEmpty()) {
            separator = "";
        } else {
            separator = ifs.substring(0, 1);
        }
        return separator;
    }

    private static String digit(final List<String> params, final char chr) {
        String result = null;
        if (chr >= '0' && chr <= '9') {
            result = Parameters.indexed(params, chr - '0');
        }
        return result;
    }

    private static String indexed(final List<String> params, final int idx) {
        final String result;
        if (idx < params.size()) {
            result = params.get(idx);
        } else {
            result = "";
        }
        return result;
    }
}
