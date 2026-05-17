/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Built-ins that read or change the working directory and umask.
 *
 * @since 1.0
 */
final class DirectoryBuiltins {

    @GeneratedDefensive
    private DirectoryBuiltins() {
    }

    /**
     * Implementation of {@code cd [-L|-P] [--] [target]}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doCd(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        return new ChangeDirectory(self).run(args);
    }

    /**
     * Pwd built-in. POSIX 2.14: writes the current working directory.
     * {@code -L} (default) prefers the {@code PWD} env value; {@code -P}
     * resolves symlinks via {@link Path#toRealPath}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0 on success, 1 on IO error
     */
    static int doPwd(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        boolean physical = false;
        for (final String arg : args) {
            if ("-P".equals(arg)) {
                physical = true;
            } else if ("-L".equals(arg)) {
                physical = false;
            }
        }
        final Map<String, String> environment = self.environmentMap();
        final String logical = environment.getOrDefault(
            "PWD", System.getProperty("user.dir", "/")
        );
        int code = 0;
        if (physical) {
            try {
                self.outStream().println(
                    Path.of(logical).toRealPath().toString()
                );
            } catch (final IOException ex) {
                code = 1;
            }
        } else {
            self.outStream().println(logical);
        }
        return code;
    }

    /**
     * Implementation of {@code umask [-S] [mode]}.
     * POSIX 2.14. JDK has no portable umask call, so the value is
     * tracked in the environment under the {@code SH4J_UMASK} key but
     * is NOT actually enforced on file creations (documented stub).
     * Without args, prints the current mask. With a mode, sets it
     * (octal only — symbolic mode is rejected with exit 1). {@code -S}
     * formats the mask symbolically (POSIX 2.14 "Symbolic Output").
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return The exit code: 0 on success, 1 on parse error
     */
    static int doUmask(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final Map<String, String> environment = self.environmentMap();
        final String current = environment.getOrDefault("SH4J_UMASK", "0022");
        int code = 0;
        boolean symbolic = false;
        int idx = 0;
        if (!args.isEmpty() && "-S".equals(args.get(0))) {
            symbolic = true;
            idx = 1;
        }
        if (idx >= args.size()) {
            if (symbolic) {
                self.outStream().println(umaskSymbolic(current));
            } else {
                self.outStream().println(current);
            }
        } else {
            try {
                environment.put(
                    "SH4J_UMASK",
                    String.format(
                        "%04o",
                        Integer.parseInt(args.get(idx), 8)
                    )
                );
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        return code;
    }

    private static String umaskSymbolic(final String mask) {
        int value = 0;
        boolean good = true;
        try {
            value = Integer.parseInt(mask, 8);
        } catch (final NumberFormatException ex) {
            good = false;
        }
        final String symbolic;
        if (good) {
            final int allbits = 7;
            symbolic = String.format(
                "u=%s,g=%s,o=%s",
                umaskBitsToString(allbits & ~((value >> 6) & allbits)),
                umaskBitsToString(allbits & ~((value >> 3) & allbits)),
                umaskBitsToString(allbits & ~(value & allbits))
            );
        } else {
            symbolic = "u=,g=,o=";
        }
        return symbolic;
    }

    private static String umaskBitsToString(final int bits) {
        final StringBuilder out = new StringBuilder(3);
        final int rbit = 4;
        final int wbit = 2;
        final int xbit = 1;
        if ((bits & rbit) != 0) {
            out.append('r');
        }
        if ((bits & wbit) != 0) {
            out.append('w');
        }
        if ((bits & xbit) != 0) {
            out.append('x');
        }
        return out.toString();
    }
}
