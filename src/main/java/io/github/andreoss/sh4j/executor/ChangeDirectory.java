/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The {@code cd} built-in: resolves the operand against the shell working
 * directory and {@code CDPATH}, logically or physically.
 *
 * @since 1.0
 */
final class ChangeDirectory {

    /**
     * The built-in context.
     */
    private final BuiltinContext shell;

    /**
     * Ctor.
     *
     * @param shell The built-in context
     */
    ChangeDirectory(final BuiltinContext shell) {
        this.shell = shell;
    }

    /**
     * Runs {@code cd [-L|-P] [--] [directory]}.
     *
     * @param args The arguments
     * @return The exit code (0 on success)
     */
    int run(final List<String> args) {
        final int idx = ChangeDirectory.operand(args);
        final Map<String, String> environment = this.shell.environmentMap();
        String target;
        int code = 0;
        if (idx >= args.size()) {
            target = environment.get("HOME");
            if (target == null) {
                this.shell.errStream().println("sh: cd: HOME not set");
                code = 1;
            }
        } else {
            target = args.get(idx);
            if ("-".equals(target)) {
                target = environment.get("OLDPWD");
                code = ChangeDirectory.reportOldPwd(this.shell, target);
            }
        }
        if (code == 0) {
            code = this.change(target, ChangeDirectory.physical(args.subList(0, idx)));
        }
        return code;
    }

    private int change(final String target, final boolean physical) {
        final String found = this.cdpath(target);
        String path = target;
        if (found != null) {
            path = found;
        }
        final File dir = new File(this.absolute(path));
        int code = 1;
        if (dir.isDirectory()) {
            String abs = dir.getPath();
            if (physical) {
                abs = ChangeDirectory.canonical(dir);
            }
            final Map<String, String> environment = this.shell.environmentMap();
            final String oldpwd = environment.get("PWD");
            if (oldpwd != null) {
                environment.put("OLDPWD", oldpwd);
            }
            environment.put("PWD", abs);
            this.shell.setCurrentDirectory(new File(abs));
            if (!path.equals(target)) {
                this.shell.outStream().println(abs);
            }
            code = 0;
        } else {
            this.shell.errStream().println(String.format("sh: cd: can't cd to %s", target));
        }
        return code;
    }

    private String cdpath(final String target) {
        final String list = this.shell.environmentMap().get("CDPATH");
        final String head = target.split("/", -1)[0];
        String found = null;
        if (list != null && !target.startsWith("/")
            && !".".equals(head) && !"..".equals(head)) {
            for (final String entry : list.split(":", -1)) {
                final String candidate = ChangeDirectory.prefixed(entry, target);
                if (new File(this.absolute(candidate)).isDirectory()) {
                    found = candidate;
                    break;
                }
            }
        }
        return found;
    }

    private String absolute(final String path) {
        String full = path;
        if (!path.startsWith("/")) {
            full = String.join("/", this.shell.currentDirectory().getPath(), path);
        }
        return Path.of(full).normalize().toString();
    }

    @GeneratedDefensive
    private static String canonical(final File dir) {
        String abs;
        try {
            abs = dir.getCanonicalPath();
        } catch (final IOException ex) {
            abs = dir.getAbsolutePath();
        }
        return abs;
    }

    private static int reportOldPwd(final BuiltinIo outlet, final String old) {
        final int code;
        if (old == null) {
            outlet.errStream().println("sh: cd: OLDPWD not set");
            code = 1;
        } else {
            outlet.outStream().println(old);
            code = 0;
        }
        return code;
    }

    private static int operand(final List<String> args) {
        int idx = 0;
        while (idx < args.size() && ChangeDirectory.option(args.get(idx))) {
            idx += 1;
        }
        if (idx < args.size() && "--".equals(args.get(idx))) {
            idx += 1;
        }
        return idx;
    }

    private static boolean option(final String arg) {
        return "-P".equals(arg) || "-L".equals(arg);
    }

    private static boolean physical(final List<String> opts) {
        boolean phys = false;
        for (final String opt : opts) {
            if (ChangeDirectory.option(opt)) {
                phys = "-P".equals(opt);
            }
        }
        return phys;
    }

    private static String prefixed(final String entry, final String target) {
        String candidate = target;
        if (!entry.isEmpty()) {
            candidate = String.join("/", entry, target);
        }
        return candidate;
    }
}
