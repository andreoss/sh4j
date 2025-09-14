/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

/**
 * Holds the executor's table of built-in commands and routes calls to the
 * matching {@link Builtin}. Every built-in registered here keeps its own
 * lambda body; the dispatcher itself stays generic.
 * @since 1.0
 */
final class BuiltinDispatcher {

    /** Default IFS value (space, tab, newline). */
    private static final String DEFAULT_IFS = " \t\n";
    /** {@code -} sentinel used by {@code cd}. */
    private static final String DASH = "-";

    /** Map from built-in name to its implementation. */
    private final Map<String, Builtin> registry;

    /**
     * Builds the dispatcher and registers every shell built-in.
     */
    BuiltinDispatcher() {
        super();
        this.registry = new HashMap<>();
        this.register("true", (self, args, env, assigns) -> 0);
        this.register("false", (self, args, env, assigns) -> 1);
        this.register(":", (self, args, env, assigns) -> 0);
        this.register("break", (self, args, env, assigns) -> {
            throw new Executor.BreakSignal();
        });
        this.register("continue", (self, args, env, assigns) -> {
            throw new Executor.ContinueSignal();
        });
        this.register("unset", BuiltinDispatcher::doUnset);
        this.register("export", BuiltinDispatcher::doExport);
        this.register("cd", BuiltinDispatcher::doCd);
        this.register("pwd", BuiltinDispatcher::doPwd);
        this.register("echo", BuiltinDispatcher::doEcho);
        this.register("eval", BuiltinDispatcher::doEval);
        this.register("exec", BuiltinDispatcher::doExec);
        this.register("exit", BuiltinDispatcher::doExit);
        this.register("type", BuiltinDispatcher::doType);
        this.register("command", BuiltinDispatcher::doCommand);
        this.register("wait", BuiltinDispatcher::doWait);
        this.register("hash", BuiltinDispatcher::doHash);
        this.register("kill", BuiltinDispatcher::doKill);
        this.register("umask", BuiltinDispatcher::doUmask);
        this.register("shift", BuiltinDispatcher::doShift);
        this.register("read", BuiltinDispatcher::doRead);
        this.register("alias", BuiltinDispatcher::doAlias);
        this.register("unalias", BuiltinDispatcher::doUnalias);
        this.register("getopts", BuiltinDispatcher::doGetopts);
        this.register("jobs", BuiltinDispatcher::doJobs);
        this.register("bg", BuiltinDispatcher::doBg);
        this.register("fg", BuiltinDispatcher::doFg);
        this.register("fc", BuiltinDispatcher::doFc);
        this.register("set", BuiltinDispatcher::doSet);
        this.register("readonly", BuiltinDispatcher::doReadonly);
    }

    /**
     * Registers a built-in by name.
     * @param name    The command name (as written in source)
     * @param builtin The implementation
     */
    void register(final String name, final Builtin builtin) {
        this.registry.put(name, builtin);
    }

    /**
     * @param name A command name
     * @return True when a built-in is registered under {@code name}
     */
    boolean isBuiltin(final String name) {
        return this.registry.containsKey(name);
    }

    /**
     * Dispatches to the matching built-in, or returns null when no entry
     * exists (the executor then resolves an external command).
     * @param name        The command name
     * @param self        Narrow façade over the executor
     * @param arguments   Command arguments (excluding the name)
     * @param env         The live environment view
     * @param assignments Inline assignments attached to this call
     * @return The built-in's exit code, or null when not a built-in
     */
    Integer dispatch(
        final String name,
        final BuiltinContext self,
        final List<String> arguments,
        final Map<String, String> env,
        final List<String> assignments
    ) {
        final Builtin builtin = this.registry.get(name);
        final Integer code;
        if (builtin == null) {
            code = null;
        } else {
            code = builtin.invoke(self, arguments, env, assignments);
        }
        return code;
    }

    /**
     * Implementation of {@code unset NAME ...}.
     */
    private static int doUnset(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        for (final String arg : args) {
            self.environmentMap().remove(arg);
        }
        return 0;
    }

    /**
     * Implementation of {@code export NAME[=VALUE] ...}.
     */
    private static int doExport(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> environment = self.environmentMap();
        if (!assigns.isEmpty()) {
            environment.putAll(env);
        }
        for (final String arg : args) {
            final int eqIdx = arg.indexOf('=');
            if (eqIdx >= 0) {
                environment.put(arg.substring(0, eqIdx), arg.substring(eqIdx + 1));
            } else {
                environment.putIfAbsent(arg, "");
            }
        }
        if (assigns.isEmpty() && args.isEmpty()) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                self.outStream().println("export " + entry.getKey() + "=" + entry.getValue());
            }
        }
        return 0;
    }

    /**
     * Implementation of {@code cd [target]}.
     */
    private static int doCd(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> environment = self.environmentMap();
        String target;
        int code = 0;
        if (args.isEmpty()) {
            target = environment.get("HOME");
            if (target == null) {
                code = 1;
            }
        } else {
            target = self.expandVariables(args.get(0), env);
            if (DASH.equals(target)) {
                final String old = environment.get("OLDPWD");
                if (old == null) {
                    code = 1;
                } else {
                    self.outStream().println(old);
                    target = old;
                }
            }
        }
        if (code == 0) {
            final File dir = new File(target);
            if (dir.isDirectory()) {
                code = applyChdir(environment, dir);
            } else {
                code = 1;
            }
        }
        return code;
    }

    /**
     * pwd built-in. POSIX 2.14: writes the current working directory.
     * {@code -L} (default) prefers the {@code PWD} env value; {@code -P}
     * resolves symlinks via {@link java.nio.file.Path#toRealPath}.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on IO error
     */
    private static int doPwd(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
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
                    java.nio.file.Path.of(logical).toRealPath().toString()
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
     * Mutates PWD/OLDPWD and {@code user.dir} for a successful cd.
     * @param environment The live env map
     * @param dir         The directory being entered
     * @return The cd exit code (always 0 here)
     */
    private static int applyChdir(final Map<String, String> environment, final File dir) {
        String abs;
        try {
            abs = dir.getCanonicalPath();
        } catch (final IOException ex) {
            abs = dir.getAbsolutePath();
        }
        final String oldPwd = environment.get("PWD");
        if (oldPwd != null) {
            environment.put("OLDPWD", oldPwd);
        }
        environment.put("PWD", abs);
        System.setProperty("user.dir", abs);
        return 0;
    }

    /**
     * Implementation of {@code echo [-n] ARG ...}.
     */
    private static int doEcho(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean newline = true;
        int start = 0;
        if (!args.isEmpty() && "-n".equals(args.get(0))) {
            newline = false;
            start = 1;
        }
        boolean firstField = true;
        boolean error = false;
        for (int idx = start; idx < args.size(); idx = idx + 1) {
            final String arg = args.get(idx);
            final WordExpander.ExpansionResult res = WordExpander.expandVariables(arg, env, self);
            error = error || res.error;
            final String expanded = res.value;
            if (WordExpander.isQuoted(arg)) {
                if (!firstField) {
                    self.outStream().print(" ");
                }
                self.outStream().print(WordExpander.removeQuotes(expanded));
                firstField = false;
            } else {
                final String ifsVal = env.getOrDefault("IFS", DEFAULT_IFS);
                final List<String> fields = WordExpander.pathnameExpand(
                    WordExpander.splitFields(expanded, ifsVal)
                );
                for (final String field : fields) {
                    if (!firstField) {
                        self.outStream().print(" ");
                    }
                    self.outStream().print(WordExpander.removeQuotes(field));
                    firstField = false;
                }
            }
        }
        if (newline) {
            self.outStream().println();
        }
        return error ? 1 : 0;
    }

    /**
     * Implementation of {@code eval STRING ...}.
     */
    private static int doEval(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final StringBuilder buf = new StringBuilder();
        for (final String arg : args) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(WordExpander.removeQuotes(self.expandVariables(arg, env)));
        }
        final String input = buf.toString();
        final int code;
        if (input.isEmpty()) {
            code = 0;
        } else {
            final ListNode ast = new Parser(new Lexer(new CharReader(input))).parse();
            code = self.execute(ast);
        }
        return code;
    }

    /**
     * Implementation of {@code exec COMMAND ARG ...}.
     */
    private static int doExec(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code;
        if (args.isEmpty()) {
            code = 0;
        } else {
            final String cmdName = self.expandVariables(args.get(0), env);
            final String resolved = self.resolveCommand(cmdName);
            if (resolved == null) {
                code = Executor.EXIT_COMMAND_NOT_FOUND;
            } else {
                code = runExec(self, args, env, resolved);
            }
        }
        return code;
    }

    /**
     * Forks the external command and waits for it.
     * @param self     Façade for env / expansion / IO-exception mapping
     * @param args     The original args (with {@code args[0]} the command name)
     * @param env      Live environment
     * @param resolved Absolute path of the resolved command
     * @return The child's exit code, or a POSIX mapping of an IO failure
     */
    private static int runExec(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final String resolved
    ) {
        final List<String> cmdArgs = new ArrayList<>(args.size());
        cmdArgs.add(resolved);
        for (int idx = 1; idx < args.size(); idx = idx + 1) {
            final String expanded = self.expandVariables(args.get(idx), env);
            cmdArgs.add(WordExpander.removeQuotes(expanded));
        }
        final ProcessBuilder builder = new ProcessBuilder(cmdArgs);
        builder.environment().putAll(env);
        builder.directory(new File(System.getProperty("user.dir")));
        int code;
        try {
            final Process process = builder.start();
            code = process.waitFor();
        } catch (final IOException ex) {
            code = self.exitCodeForIoException(ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            code = 1;
        }
        return code;
    }

    /**
     * Implementation of {@code exit [STATUS]}.
     */
    private static int doExit(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        if (!args.isEmpty()) {
            final WordExpander.ExpansionResult res = WordExpander.expandVariables(
                args.get(0), env, self
            );
            if (res.error) {
                code = 1;
            } else {
                try {
                    code = Integer.parseInt(res.value);
                } catch (final NumberFormatException ex) {
                    code = 1;
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code type NAME ...}.
     */
    private static int doType(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        for (final String arg : args) {
            final String expanded = self.expandVariables(arg, env);
            if (Executor.isBuiltin(expanded)) {
                self.outStream().println(expanded + " is a shell built-in");
            } else if (self.functionsMap().containsKey(expanded)) {
                self.outStream().println(expanded + " is a function");
            } else {
                final String resolved = self.resolveCommand(expanded);
                if (resolved == null) {
                    code = 1;
                } else {
                    self.outStream().println(expanded + " is " + resolved);
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code command [-v|-V|-p] name [args...]}.
     * POSIX 2.14. {@code -v} prints location of NAME; {@code -V}
     * prints a verbose description; without flags, invokes NAME
     * bypassing function lookup. {@code -p} (default-PATH) is
     * accepted but treated as a no-op (the configured PATH is used).
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return Resolved command exit code (1 if NAME not found with -v/-V)
     */
    private static int doCommand(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        int idx = 0;
        boolean verbose = false;
        boolean locate = false;
        while (idx < args.size() && args.get(idx).startsWith("-")) {
            final String flag = args.get(idx);
            if ("-v".equals(flag)) {
                locate = true;
            } else if ("-V".equals(flag)) {
                verbose = true;
            }
            idx += 1;
        }
        if (idx >= args.size()) {
            code = locate || verbose ? 1 : 0;
        } else {
            final String name = self.expandVariables(args.get(idx), env);
            if (locate || verbose) {
                code = describeCommand(self, name, verbose);
            } else {
                final StringBuilder line = new StringBuilder(name);
                for (int i = idx + 1; i < args.size(); i += 1) {
                    line.append(' ').append(args.get(i));
                }
                final ListNode ast = new Parser(
                    new Lexer(new CharReader(line.toString()))
                ).parse();
                code = self.execute(ast);
            }
        }
        return code;
    }

    /**
     * Helper for {@code command -v} / {@code command -V}.
     * @param self    Builtin context
     * @param name    Command name
     * @param verbose true for -V (descriptive), false for -v (location)
     * @return 0 if found, 1 if not
     */
    private static int describeCommand(
        final BuiltinContext self, final String name, final boolean verbose
    ) {
        int code = 0;
        if (Executor.isBuiltin(name)) {
            if (verbose) {
                self.outStream().println(name + " is a shell built-in");
            } else {
                self.outStream().println(name);
            }
        } else if (self.functionsMap().containsKey(name)) {
            if (verbose) {
                self.outStream().println(name + " is a function");
            } else {
                self.outStream().println(name);
            }
        } else {
            final String resolved = self.resolveCommand(name);
            if (resolved == null) {
                code = 1;
            } else if (verbose) {
                self.outStream().println(name + " is " + resolved);
            } else {
                self.outStream().println(resolved);
            }
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
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on parse error
     */
    private static int doUmask(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
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
            final String mode = self.expandVariables(args.get(idx), env);
            try {
                final int value = Integer.parseInt(mode, 8);
                environment.put("SH4J_UMASK", String.format("%04o", value));
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        return code;
    }

    /**
     * Formats an octal umask string as POSIX symbolic mode.
     * @param mask Octal 4-digit umask (e.g. "0022")
     * @return Symbolic representation (e.g. "u=rwx,g=rx,o=rx")
     */
    private static String umaskSymbolic(final String mask) {
        final int value;
        try {
            value = Integer.parseInt(mask, 8);
        } catch (final NumberFormatException ex) {
            return "u=,g=,o=";
        }
        final int allBits = 7;
        final int user = allBits & ~((value >> 6) & allBits);
        final int group = allBits & ~((value >> 3) & allBits);
        final int other = allBits & ~(value & allBits);
        return String.format(
            "u=%s,g=%s,o=%s",
            umaskBitsToString(user), umaskBitsToString(group), umaskBitsToString(other)
        );
    }

    /**
     * Maps a 3-bit permission triple to {@code rwx} chars.
     * @param bits 0..7
     * @return Permission string
     */
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

    /**
     * Implementation of {@code kill [-s SIG | -SIG] PID... | -l}.
     * POSIX 2.14. JDK constraint: ProcessHandle only supports TERM
     * (destroy) and KILL (destroyForcibly); other signals are
     * accepted at the command line but mapped to TERM (which is the
     * POSIX default) and a soft warning is not emitted.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on usage error, 127 if any PID cannot be signalled
     */
    private static int doKill(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        if (args.isEmpty()) {
            code = 1;
        } else if ("-l".equals(args.get(0))) {
            self.outStream().println("HUP INT QUIT KILL TERM");
        } else {
            boolean forcibly = false;
            int idx = 0;
            if ("-s".equals(args.get(0)) && args.size() >= 2) {
                forcibly = "KILL".equals(args.get(1)) || "9".equals(args.get(1));
                idx = 2;
            } else if (args.get(0).startsWith("-") && args.get(0).length() > 1) {
                final String sig = args.get(0).substring(1);
                forcibly = "KILL".equals(sig) || "9".equals(sig);
                idx = 1;
            }
            if (idx >= args.size()) {
                code = 1;
            }
            while (idx < args.size() && code == 0) {
                code = signalOne(
                    self.expandVariables(args.get(idx), env), forcibly
                );
                idx += 1;
            }
        }
        return code;
    }

    /**
     * Helper for {@link #doKill}: send a signal to a single PID.
     * @param pidStr   Decimal PID
     * @param forcibly true for SIGKILL, false for SIGTERM
     * @return 0 on success, 127 if no such process or non-numeric PID
     */
    private static int signalOne(final String pidStr, final boolean forcibly) {
        int code;
        try {
            final long pid = Long.parseLong(pidStr);
            final java.util.Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isPresent()) {
                final boolean sent;
                if (forcibly) {
                    sent = handle.get().destroyForcibly();
                } else {
                    sent = handle.get().destroy();
                }
                code = sent ? 0 : 127;
            } else {
                code = 127;
            }
        } catch (final NumberFormatException ex) {
            code = 127;
        }
        return code;
    }

    /**
     * Implementation of {@code hash [-r] [name...]}.
     * POSIX 2.14. sh4j has no command-lookup cache, so:
     *   - {@code hash} with no args prints nothing and returns 0
     *     (cache is empty by definition)
     *   - {@code hash -r} returns 0 (no-op)
     *   - {@code hash NAME ...} verifies each NAME resolves on PATH;
     *     returns 0 if all do, 127 if any cannot be found
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 127 when a named utility is not found
     */
    private static int doHash(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        for (final String arg : args) {
            if ("-r".equals(arg)) {
                continue;
            }
            final String name = self.expandVariables(arg, env);
            if (!Executor.isBuiltin(name) && self.resolveCommand(name) == null) {
                code = 127;
            }
        }
        return code;
    }

    /**
     * Implementation of {@code wait [PID...]}.
     * POSIX 2.14: without args, waits for all background jobs (exit 0).
     * With PID, waits for that bg job if known (exit 0), else returns 127
     * (POSIX "unknown process" status).
     * @param self    Builtin context
     * @param args    Args after the command name (PIDs)
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 127 when a named PID is not a known bg job
     */
    private static int doWait(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        if (args.isEmpty()) {
            self.waitForBackgroundJobs();
        } else {
            final int known = self.lastBackgroundPid();
            for (final String arg : args) {
                final String expanded = self.expandVariables(arg, env);
                try {
                    final int pid = Integer.parseInt(expanded);
                    if (pid == known && known > 0) {
                        self.waitForBackgroundJobs();
                    } else {
                        code = 127;
                    }
                } catch (final NumberFormatException ex) {
                    code = 127;
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code shift [N]}.
     */
    private static int doShift(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
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
            final int maxShift = pos.size() - 1;
            if (shifts >= maxShift) {
                pos.subList(1, pos.size()).clear();
            } else {
                pos.subList(1, 1 + shifts).clear();
            }
        }
        return code;
    }

    /**
     * Implementation of {@code read [-r] var...}.
     * POSIX 2.14: reads one line from stdin, splits by IFS into variables.
     * The last variable receives all remaining fields.
     * If no variables are given, stores the line in {@code REPLY}.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for variable lookup
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on EOF
     */
    private static int doRead(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean raw = false;
        int idx = 0;
        if (!args.isEmpty() && "-r".equals(args.get(0))) {
            raw = true;
            idx = 1;
        }
        final List<String> names = args.subList(idx, args.size());
        final BufferedReader reader = new BufferedReader(
            new InputStreamReader(self.stdin())
        );
        try {
            final String line = reader.readLine();
            if (line == null) {
                return 1;
            }
            final String processed;
            if (raw) {
                processed = line;
            } else {
                processed = processReadEscapes(line);
            }
            final Map<String, String> environment = self.environmentMap();
            if (names.isEmpty()) {
                environment.put("REPLY", processed);
                return 0;
            }
            final String ifs = env.getOrDefault("IFS", DEFAULT_IFS);
            final char firstIfs = ifs.charAt(0);
            final List<String> fields = splitReadFields(processed, ifs, firstIfs);
            for (int vdx = 0; vdx < names.size(); vdx = vdx + 1) {
                final String name = names.get(vdx);
                if (vdx == names.size() - 1) {
                    environment.put(name, joinReadFields(fields, vdx, ifs, firstIfs));
                } else if (vdx < fields.size()) {
                    environment.put(name, fields.get(vdx));
                } else {
                    environment.put(name, "");
                }
            }
            return 0;
        } catch (final IOException ex) {
            return 1;
        }
    }

    /**
     * Processes backslash escapes in a read line (non-raw mode).
     * Backslash-newline is removed; backslash before any other char
     * preserves that char literally.
     * @param line The raw input line
     * @return The processed line
     */
    private static String processReadEscapes(final String line) {
        final StringBuilder out = new StringBuilder(line.length());
        boolean escaped = false;
        for (int idx = 0; idx < line.length(); idx = idx + 1) {
            final char ch = line.charAt(idx);
            if (escaped) {
                out.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else {
                out.append(ch);
            }
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    /**
     * Splits a read line into fields per POSIX read semantics.
     * Leading IFS whitespace is stripped; remaining fields separated
     * by IFS whitespace; the first non-whitespace IFS char in the
     * original string also acts as a delimiter.
     * @param line     The processed input line
     * @param ifs      The IFS string
     * @param firstIfs The first IFS char (the delimiter char)
     * @return Split fields
     */
    private static List<String> splitReadFields(
        final String line, final String ifs, final char firstIfs
    ) {
        final List<String> fields = new ArrayList<>();
        int pos = 0;
        while (pos < line.length() && ifs.indexOf(line.charAt(pos)) >= 0
            && (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) {
            pos = pos + 1;
        }
        final StringBuilder current = new StringBuilder();
        while (pos < line.length()) {
            final char ch = line.charAt(pos);
            if (ch == firstIfs && firstIfs != ' ' && firstIfs != '\t') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (ifs.indexOf(ch) >= 0) {
                fields.add(current.toString());
                current.setLength(0);
                while (pos + 1 < line.length()
                    && ifs.indexOf(line.charAt(pos + 1)) >= 0) {
                    pos = pos + 1;
                }
            } else {
                current.append(ch);
            }
            pos = pos + 1;
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * Joins remaining fields starting at {@code start} using IFS.
     * @param fields  All split fields
     * @param start   Index of first field to join
     * @param ifs     IFS string for delimiter
     * @param firstIfs The first IFS char
     * @return Joined string
     */
    private static String joinReadFields(
        final List<String> fields, final int start,
        final String ifs, final char firstIfs
    ) {
        final StringBuilder out = new StringBuilder();
        for (int idx = start; idx < fields.size(); idx = idx + 1) {
            if (idx > start) {
                out.append(firstIfs);
            }
            out.append(fields.get(idx));
        }
        return out.toString();
    }

    /**
     * Implementation of {@code alias [name[=value]...]}.
     * POSIX 2.14: without args, prints all aliases.
     * With name=value, sets an alias. With plain name, prints that alias.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment (unused)
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 if any named alias is undefined
     */
    private static int doAlias(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> aliases = self.aliases();
        int code = 0;
        final boolean hasArgs = !args.isEmpty() || !assigns.isEmpty();
        if (!hasArgs) {
            for (final Map.Entry<String, String> entry : aliases.entrySet()) {
                self.outStream().println(
                    entry.getKey() + "='" + entry.getValue() + "'"
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
     * Processes a single alias argument (name or name=value).
     * @param self    Builtin context (for printing)
     * @param aliases The alias table
     * @param arg     The argument text
     * @param code    Current exit code
     * @return Updated exit code
     */
    private static int applyAliasArg(
        final BuiltinContext self, final Map<String, String> aliases,
        final String arg, final int code
    ) {
        int result = code;
        final int eqIdx = arg.indexOf('=');
        if (eqIdx >= 0) {
            String value = arg.substring(eqIdx + 1);
            if (value.length() >= 2
                && ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\"")))) {
                value = value.substring(1, value.length() - 1);
            }
            aliases.put(arg.substring(0, eqIdx), value);
        } else if (aliases.containsKey(arg)) {
            self.outStream().println(
                arg + "='" + aliases.get(arg) + "'"
            );
        } else {
            result = 1;
        }
        return result;
    }

    /**
     * Implementation of {@code unalias [-a] name...}.
     * POSIX 2.14: removes named aliases, or all aliases with -a.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment (unused)
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 if any named alias doesn't exist
     */
    private static int doUnalias(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
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

    /**
     * Implementation of {@code getopts optstring var [args...]}.
     * POSIX 2.14: parses option characters from positional parameters
     * (or optional args). Uses OPTIND/OPTARG/OPTERR env vars for state.
     * @param self    Builtin context
     * @param args    Args: optstring, var, optional positional replacement
     * @param env     Environment for OPTIND/OPTARG access
     * @param assigns Inline assignments (unused)
     * @return 0 if an option was found, 1 if all options processed
     */
    private static int doGetopts(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        if (args.size() < 2) {
            return 1;
        }
        final String optstring = args.get(0);
        final String varName = args.get(1);
        final List<String> pargs;
        final boolean usePositional;
        if (args.size() > 2) {
            pargs = args.subList(2, args.size());
            usePositional = false;
        } else {
            final List<String> pos = self.positionalParamsList();
            pargs = pos.subList(1, pos.size());
            usePositional = true;
        }
        final Map<String, String> environment = self.environmentMap();
        int optind;
        try {
            optind = Integer.parseInt(environment.getOrDefault("OPTIND", "1"));
        } catch (final NumberFormatException ex) {
            optind = 1;
        }
        if (optind < 1) {
            optind = 1;
        }
        if (optind > pargs.size()) {
            environment.put(varName, "?");
            environment.put("OPTIND", String.valueOf(pargs.size() + 1));
            return 1;
        }
        final String current = pargs.get(optind - 1);
        if ("--".equals(current)) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            environment.put(varName, "?");
            return 1;
        }
        if (current.length() < 2 || current.charAt(0) != '-') {
            environment.put(varName, "?");
            return 1;
        }
        int optpos = environment.containsKey("OPTIND_POS")
            ? Integer.parseInt(environment.get("OPTIND_POS")) : 1;
        if (optpos >= current.length()) {
            optind = optind + 1;
            optpos = 1;
            environment.put("OPTIND", String.valueOf(optind));
            if (optind > pargs.size()) {
                environment.put(varName, "?");
                environment.remove("OPTIND_POS");
                return 1;
            }
            final String next = pargs.get(optind - 1);
            if (next.length() < 2 || next.charAt(0) != '-') {
                environment.put(varName, "?");
                environment.remove("OPTIND_POS");
                return 1;
            }
            environment.put("OPTIND_POS", String.valueOf(1));
            optpos = 1;
        }
        final char optchar = current.charAt(optpos);
        environment.put("OPTIND_POS", String.valueOf(optpos + 1));
        final int optIdx = optstring.indexOf(optchar);
        if (optIdx < 0) {
            if (optstring.length() > 0 && optstring.charAt(0) == ':') {
                environment.put(varName, "?");
                environment.put("OPTARG", String.valueOf(optchar));
            } else {
                self.errStream().println(
                    "sh: illegal option -- " + optchar
                );
                environment.put(varName, "?");
                environment.put("OPTARG", String.valueOf(optchar));
            }
            return 0;
        }
        final boolean takesArg = optIdx + 1 < optstring.length()
            && optstring.charAt(optIdx + 1) == ':';
        if (takesArg) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            if (optpos + 1 < current.length()) {
                environment.put(
                    "OPTARG", current.substring(optpos + 1)
                );
            } else if (optind < pargs.size()) {
                environment.put(
                    "OPTARG", pargs.get(optind)
                );
                environment.put("OPTIND", String.valueOf(optind + 2));
            } else {
                if (optstring.length() > 0 && optstring.charAt(0) == ':') {
                    environment.put(varName, ":");
                } else {
                    self.errStream().println(
                        "sh: option requires an argument -- " + optchar
                    );
                    environment.put(varName, "?");
                }
                environment.put("OPTARG", "");
                environment.remove("OPTIND_POS");
                return 0;
            }
            environment.remove("OPTIND_POS");
        } else {
            environment.put("OPTARG", "");
        }
        environment.put(varName, String.valueOf(optchar));
        if (!takesArg && optpos + 1 >= current.length()) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            environment.remove("OPTIND_POS");
        }
        return 0;
    }

    /**
     * Parses a job id that may be specified as {@code %n} or as a bare
     * decimal number.
     * @param arg The argument string
     * @return The integer job id, or -1 when the input is not valid
     */
    private static int parseJobId(final String arg) {
        final String digits;
        if (arg.startsWith("%")) {
            digits = arg.substring(1);
        } else {
            digits = arg;
        }
        try {
            return Integer.parseInt(digits);
        } catch (final NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Implementation of {@code jobs [%n...]}.
     * POSIX 2.14: without args, lists all background jobs with their
     * state and command line.
     * @param self    Builtin context
     * @param args    Args after the command name (optional job ids)
     * @param env     Environment (unused)
     * @param assigns Inline assignments (unused)
     * @return 0
     */
    private static int doJobs(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final JobManager mgr = self.jobManager();
        final List<JobManager.Job> all;
        if (args.isEmpty()) {
            all = mgr.listJobs();
        } else {
            all = new ArrayList<>();
            for (final String arg : args) {
                final int id = parseJobId(arg);
                if (id > 0) {
                    final JobManager.Job job = mgr.getJob(id);
                    if (job != null) {
                        all.add(job);
                    }
                }
            }
        }
        for (final JobManager.Job job : all) {
            final String state;
            switch (job.state) {
                case RUNNING:
                    state = "Running";
                    break;
                case DONE:
                    state = "Done";
                    break;
                default:
                    state = "Stopped";
                    break;
            }
            self.outStream().printf("[%d] %s %s%n", job.id, state, job.command);
        }
        return 0;
    }

    /**
     * Implementation of {@code bg [%n...]}.
     * POSIX 2.14: resumes suspended jobs in the background. Without args,
     * operates on the most recent job. Prints a confirmation line.
     * @param self    Builtin context
     * @param args    Args after the command name (optional job ids)
     * @param env     Environment (unused)
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 if no matching job
     */
    private static int doBg(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final JobManager mgr = self.jobManager();
        final List<Integer> ids = new ArrayList<>();
        if (args.isEmpty()) {
            final List<JobManager.Job> all = mgr.listJobs();
            if (all.isEmpty()) {
                self.errStream().println("sh: bg: no current job");
                return 1;
            }
            ids.add(all.get(all.size() - 1).id);
        } else {
            for (final String arg : args) {
                final int id = parseJobId(arg);
                if (id <= 0 || mgr.getJob(id) == null) {
                    self.errStream().println("sh: bg: " + arg + ": no such job");
                    return 1;
                }
                ids.add(id);
            }
        }
        for (final int id : ids) {
            final JobManager.Job job = mgr.getJob(id);
            if (job != null && job.state == JobManager.State.STOPPED) {
                job.state = JobManager.State.RUNNING;
            }
            self.outStream().printf("[%d] %s%n", id, job != null ? job.command : "");
        }
        return 0;
    }

    /**
     * Implementation of {@code fg [%n]}.
     * POSIX 2.14: brings a background job to the foreground, waiting for
     * it to complete. Without args, operates on the most recent job.
     * @param self    Builtin context
     * @param args    Args after the command name (optional job id)
     * @param env     Environment (unused)
     * @param assigns Inline assignments (unused)
     * @return The job's exit code, or 1 on error
     */
    private static int doFg(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final JobManager mgr = self.jobManager();
        final int id;
        if (args.isEmpty()) {
            final List<JobManager.Job> all = mgr.listJobs();
            if (all.isEmpty()) {
                self.errStream().println("sh: fg: no current job");
                return 1;
            }
            id = all.get(all.size() - 1).id;
        } else {
            id = parseJobId(args.get(0));
            if (id <= 0) {
                self.errStream().println("sh: fg: " + args.get(0) + ": no such job");
                return 1;
            }
        }
        final JobManager.Job job = mgr.getJob(id);
        if (job == null) {
            self.errStream().printf("sh: fg: %d: no such job%n", id);
            return 1;
        }
        self.outStream().println(job.command);
        return mgr.waitForJob(id);
    }

    /**
     * Implementation of {@code fc [-l] [-n] [-s pat=rep] [first [last]]}.
     * POSIX 2.14: lists or re-executes history entries.
     * {@code -l} lists entries (default), {@code -n} suppresses numbers,
     * {@code -s pat=rep} substitutes and re-executes the last command.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for variable lookup
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on error
     */
    private static int doFc(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean listMode = true;
        boolean noNumbers = false;
        String substitute = null;
        int idx = 0;
        while (idx < args.size() && args.get(idx).startsWith("-")
            && !args.get(idx).matches("-\\d+")) {
            final String flag = args.get(idx);
            for (int fidx = 1; fidx < flag.length(); fidx = fidx + 1) {
                final char ch = flag.charAt(fidx);
                if (ch == 'l') {
                    listMode = true;
                } else if (ch == 'n') {
                    noNumbers = true;
                } else if (ch == 'e') {
                    idx = idx + 1;
                    if (idx >= args.size()) {
                        self.errStream().println("sh: fc: -e requires argument");
                        return 1;
                    }
                } else if (ch == 's') {
                    listMode = false;
                    if (idx + 1 < args.size() && args.get(idx + 1).contains("=")) {
                        substitute = args.get(idx + 1);
                        idx = idx + 1;
                    } else if (!assigns.isEmpty()) {
                        substitute = assigns.get(0);
                    } else {
                        self.errStream().println("sh: fc: -s requires pat=rep");
                        return 1;
                    }
                    fidx = flag.length();
                } else {
                    self.errStream().println("sh: fc: unknown option -" + ch);
                    return 1;
                }
            }
            idx = idx + 1;
        }
        final CommandHistory hist = self.history();
        if (hist.size() == 0) {
            self.errStream().println("sh: fc: history is empty");
            return 1;
        }
        int first;
        if (idx < args.size()) {
            first = parseFcNumber(args.get(idx));
            idx = idx + 1;
        } else {
            first = hist.lastNumber() - 15;
            if (first < 1) {
                first = 1;
            }
        }
        final int last;
        if (idx < args.size()) {
            last = parseFcNumber(args.get(idx));
        } else {
            last = hist.lastNumber();
        }
        if (substitute != null) {
            return fcSubstitute(self, hist, substitute, first, env);
        }
        if (listMode) {
            return fcList(self, hist, first, last, noNumbers);
        }
        return 0;
    }

    /**
     * Parses an {@code fc} range argument (number or negative for relative).
     * @param arg The argument string
     * @return The resolved history number, or -1 on parse error
     */
    private static int parseFcNumber(final String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (final NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Lists history entries in the given range.
     * @param self      Builtin context for output
     * @param hist      The command history
     * @param first     Start of range
     * @param last      End of range
     * @param noNumbers True to suppress history numbers
     * @return 0
     */
    private static int fcList(
        final BuiltinContext self, final CommandHistory hist,
        final int first, final int last, final boolean noNumbers
    ) {
        final List<CommandHistory.Entry> entries = hist.list(first, last);
        for (final CommandHistory.Entry entry : entries) {
            if (noNumbers) {
                self.outStream().println("\t" + entry.command());
            } else {
                self.outStream().printf("%d\t%s%n", entry.number(), entry.command());
            }
        }
        return 0;
    }

    /**
     * Substitutes a pattern in a history command and re-executes it.
     * @param self      Builtin context for execution
     * @param hist      The command history
     * @param pattern   The substitution pattern (old=new)
     * @param number    History number to operate on
     * @param env       Environment for variable lookup
     * @return 0 on success, 1 on error
     */
    private static int fcSubstitute(
        final BuiltinContext self, final CommandHistory hist,
        final String pattern, final int number, final Map<String, String> env
    ) {
        final int eqIdx = pattern.indexOf('=');
        if (eqIdx < 0) {
            self.errStream().println("sh: fc: invalid substitution pattern");
            return 1;
        }
        final String oldStr = pattern.substring(0, eqIdx);
        final String newStr = pattern.substring(eqIdx + 1);
        final String cmd = hist.get(number);
        if (cmd == null) {
            self.errStream().printf("sh: fc: %d: no such history entry%n", number);
            return 1;
        }
        final String replaced = cmd.replace(oldStr, newStr);
        self.outStream().println(replaced);
        final ListNode ast = new Parser(
            new Lexer(new CharReader(replaced))
        ).parse();
        return self.execute(ast);
    }

    /**
     * Implementation of {@code set [-efux] [--] [arg...]}.
     * POSIX 2.14: sets shell options and positional parameters.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment (unused)
     * @param assigns Inline assignments (unused)
     * @return 0
     */
    private static int doSet(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        String flags = self.shellFlags();
        int idx = 0;
        while (idx < args.size() && args.get(idx).startsWith("-")
            && !"--".equals(args.get(idx))) {
            final String flag = args.get(idx);
            for (int fidx = 1; fidx < flag.length(); fidx = fidx + 1) {
                final char ch = flag.charAt(fidx);
                if (flags.indexOf(ch) < 0) {
                    flags = flags + ch;
                }
            }
            idx = idx + 1;
        }
        if (idx < args.size() && "--".equals(args.get(idx))) {
            idx = idx + 1;
        }
        self.setShellFlags(flags);
        if (idx < args.size()) {
            final String ifsVal = env.getOrDefault("IFS", DEFAULT_IFS);
            final List<String> params = new ArrayList<>();
            for (int aidx = idx; aidx < args.size(); aidx = aidx + 1) {
                final String arg = args.get(aidx);
                final String expanded = self.expandVariables(arg, env);
                if (WordExpander.isQuoted(arg)) {
                    params.add(WordExpander.removeQuotes(expanded));
                } else {
                    final List<String> fields = WordExpander.splitFields(expanded, ifsVal);
                    for (final String field : fields) {
                        params.add(WordExpander.removeQuotes(field));
                    }
                }
            }
            self.setPositionalParams(params);
        }
        return 0;
    }

    /**
     * Implementation of {@code readonly [-p] [name[=value]...]}.
     * POSIX 2.14: marks variables as read-only.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for variable lookup
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on error
     */
    private static int doReadonly(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final List<String> all = new ArrayList<>(args);
        all.addAll(assigns);
        if (all.isEmpty()) {
            return 0;
        }
        if (!args.isEmpty() && "-p".equals(args.get(0))) {
            return 0;
        }
        final Map<String, String> environment = self.environmentMap();
        for (final String arg : all) {
            final int eqIdx = arg.indexOf('=');
            if (eqIdx >= 0) {
                final String name = arg.substring(0, eqIdx);
                final String value = arg.substring(eqIdx + 1);
                environment.put(name, value);
                environment.put(name + "_READONLY", "1");
            } else {
                environment.put(arg + "_READONLY", "1");
            }
        }
        return 0;
    }
}
