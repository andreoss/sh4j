/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the executor's table of built-in commands and routes calls to the
 * matching {@link Builtin}. Every built-in registered here keeps its own
 * lambda body; the dispatcher itself stays generic.
 *
 * @since 1.0
 */
final class BuiltinDispatcher {

    /** Map from built-in name to its implementation. */
    private final Map<String, Builtin> registry = buildRegistry();

    /**
     * Builds the dispatcher and registers every shell built-in.
     */
    BuiltinDispatcher() {
    }

    /**
     * Registers a built-in by name.
     *
     * @param name    The command name (as written in source)
     * @param builtin The implementation
     */
    void register(final String name, final Builtin builtin) {
        this.registry.put(name, builtin);
    }

    /**
     * Checks whether a built-in is registered.
     *
     * @param name A command name
     * @return True when a built-in is registered under {@code name}
     */
    boolean isBuiltin(final String name) {
        return this.registry.containsKey(name);
    }

    /**
     * Dispatches to the matching built-in, or returns null when no entry
     * exists (the executor then resolves an external command).
     *
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
            code = builtin.invoke(self, new Invocation(arguments, env, assignments));
        }
        return code;
    }

    private static Map<String, Builtin> buildRegistry() {
        final Map<String, Builtin> registry = new HashMap<>();
        registry.put("true", (self, call) -> 0);
        registry.put("false", (self, call) -> 1);
        registry.put(":", (self, call) -> 0);
        registry.put(
            "break",
            (self, call) -> {
                throw new BreakSignal(parseLoopLevels(call.arguments()));
            }
        );
        registry.put(
            "continue",
            (self, call) -> {
                throw new ContinueSignal(parseLoopLevels(call.arguments()));
            }
        );
        registry.put(
            "return",
            (self, call) -> {
                final List<String> args = call.arguments();
                int code = 0;
                if (!args.isEmpty()) {
                    try {
                        code = Integer.parseInt(args.get(0));
                    } catch (final NumberFormatException ignored) {
                        code = 0;
                    }
                }
                throw new ReturnSignal(code);
            }
        );
        registry.put(
            "unset",
            (self, call) -> BuiltinDispatcher.doUnset(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "export",
            (self, call) -> BuiltinDispatcher.doExport(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "cd",
            (self, call) -> BuiltinDispatcher.doCd(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "pwd",
            (self, call) -> BuiltinDispatcher.doPwd(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "echo",
            (self, call) -> BuiltinDispatcher.doEcho(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "eval",
            (self, call) -> BuiltinDispatcher.doEval(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "exit",
            (self, call) -> BuiltinDispatcher.doExit(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "type",
            (self, call) -> BuiltinDispatcher.doType(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "command",
            (self, call) -> BuiltinDispatcher.doCommand(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "wait",
            (self, call) -> BuiltinDispatcher.doWait(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "hash",
            (self, call) -> BuiltinDispatcher.doHash(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "kill",
            (self, call) -> BuiltinDispatcher.doKill(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "umask",
            (self, call) -> BuiltinDispatcher.doUmask(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "shift",
            (self, call) -> BuiltinDispatcher.doShift(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "read",
            (self, call) -> BuiltinDispatcher.doRead(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "alias",
            (self, call) -> BuiltinDispatcher.doAlias(
                self,
                call.arguments(),
                call.env(),
                call.assignments()
            )
        );
        registry.put(
            "unalias",
            (self, call) -> BuiltinDispatcher.doUnalias(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "getopts",
            (self, call) -> BuiltinDispatcher.doGetopts(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "jobs",
            (self, call) -> BuiltinDispatcher.doJobs(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "bg",
            (self, call) -> BuiltinDispatcher.doBg(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "fg",
            (self, call) -> BuiltinDispatcher.doFg(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "fc",
            (self, call) -> BuiltinDispatcher.doFc(
                self,
                call.arguments(),
                call.env(),
                call.assignments()
            )
        );
        registry.put(
            "set",
            (self, call) -> BuiltinDispatcher.doSet(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "readonly",
            (self, call) -> BuiltinDispatcher.doReadonly(
                self,
                call.arguments(),
                call.env(),
                call.assignments()
            )
        );
        registry.put(
            "trap",
            (self, call) -> BuiltinDispatcher.doTrap(
                self,
                call.arguments(),
                call.env()
            )
        );
        registry.put(
            "times",
            (self, call) -> BuiltinDispatcher.doTimes(
                self,
                call.env()
            )
        );
        registry.put(
            ".",
            (self, call) -> BuiltinDispatcher.doDot(
                self,
                call.arguments(),
                call.env()
            )
        );
        return registry;
    }

    /**
     * Implementation of {@code unset NAME ...}.
     *
     * @param self The context
     * @param args The arguments
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

    private static int parseLoopLevels(final List<String> args) {
        int levels = 1;
        if (!args.isEmpty()) {
            try {
                final int parsed = Integer.parseInt(args.get(0));
                if (parsed > 0) {
                    levels = parsed;
                }
            } catch (final NumberFormatException ignored) {
                levels = 1;
            }
        }
        return levels;
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
     * Implementation of {@code cd [target]}.
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
            if ("-".equals(target)) {
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
                self.setCurrentDirectory(new File(environment.getOrDefault("PWD", ".")));
            } else {
                code = 1;
            }
        }
        return code;
    }

    /**
     * Pwd built-in. POSIX 2.14: writes the current working directory.
     * {@code -L} (default) prefers the {@code PWD} env value; {@code -P}
     * resolves symlinks via {@link Path#toRealPath}.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
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

    private static int applyChdir(final Map<String, String> environment, final File dir) {
        String abs;
        try {
            abs = dir.getCanonicalPath();
        } catch (final IOException ex) {
            abs = dir.getAbsolutePath();
        }
        final String oldpwd = environment.get("PWD");
        if (oldpwd != null) {
            environment.put("OLDPWD", oldpwd);
        }
        environment.put("PWD", abs);
        return 0;
    }

    /**
     * Implementation of {@code echo [-n] ARG ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doEcho(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        boolean newline = true;
        int start = 0;
        if (!args.isEmpty() && "-n".equals(args.get(0))) {
            newline = false;
            start = 1;
        }
        boolean firstfield = true;
        for (int idx = start; idx < args.size(); idx = idx + 1) {
            for (final String field : self.expandArguments(args.get(idx), env)) {
                if (!firstfield) {
                    self.outStream().print(" ");
                }
                self.outStream().print(field);
                firstfield = false;
            }
        }
        if (newline) {
            self.outStream().println();
        }
        return 0;
    }

    /**
     * Implementation of {@code eval STRING ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doEval(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
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
            code = self.execute(
                new Parser(new Lexer(new CharReader(input))).parse()
            );
        }
        return code;
    }

    /**
     * Implementation of {@code exit [STATUS]}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doExit(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        if (!args.isEmpty()) {
            try {
                code = Integer.parseInt(self.expandVariables(args.get(0), env));
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        throw new ExitSignal(code);
    }

    /**
     * Implementation of {@code type NAME ...}.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code (0 on success)
     */
    static int doType(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        for (final String arg : args) {
            final String expanded = self.expandVariables(arg, env);
            if (self.functionsMap().containsKey(expanded)) {
                self.outStream().println(String.format("%s is a function", expanded));
            } else if (self.isBuiltinName(expanded)) {
                self.outStream().println(String.format("%s is a shell built-in", expanded));
            } else {
                final String resolved = self.resolveCommand(expanded);
                if (resolved == null) {
                    code = 1;
                } else {
                    self.outStream().println(String.format("%s is %s", expanded, resolved));
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
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return Resolved command exit code (1 if NAME not found with -v/-V)
     */
    static int doCommand(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code;
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
            if (locate || verbose) {
                code = 1;
            } else {
                code = 0;
            }
        } else {
            final String name = self.expandVariables(args.get(idx), env);
            if (locate || verbose) {
                code = describeCommand(self, name, verbose);
            } else {
                code = self.executeBypassingFunctions(
                    name, args.subList(idx + 1, args.size()), env
                );
            }
        }
        return code;
    }

    private static int describeCommand(
        final BuiltinContext self, final String name, final boolean verbose
    ) {
        int code = 0;
        if (self.functionsMap().containsKey(name)) {
            if (verbose) {
                self.outStream().println(String.format("%s is a function", name));
            } else {
                self.outStream().println(name);
            }
        } else if (self.isBuiltinName(name)) {
            if (verbose) {
                self.outStream().println(String.format("%s is a shell built-in", name));
            } else {
                self.outStream().println(name);
            }
        } else {
            final String resolved = self.resolveCommand(name);
            if (resolved == null) {
                code = 1;
            } else if (verbose) {
                self.outStream().println(String.format("%s is %s", name, resolved));
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
                        Integer.parseInt(self.expandVariables(args.get(idx), env), 8)
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
        boolean ok = true;
        try {
            value = Integer.parseInt(mask, 8);
        } catch (final NumberFormatException ex) {
            ok = false;
        }
        final String symbolic;
        if (!ok) {
            symbolic = "u=,g=,o=";
        } else {
            final int allbits = 7;
            symbolic = String.format(
                "u=%s,g=%s,o=%s",
                umaskBitsToString(allbits & ~((value >> 6) & allbits)),
                umaskBitsToString(allbits & ~((value >> 3) & allbits)),
                umaskBitsToString(allbits & ~(value & allbits))
            );
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

    /**
     * Implementation of {@code kill [-s SIG | -SIG] PID... | -l}.
     * POSIX 2.14. JDK constraint: ProcessHandle only supports TERM
     * (destroy) and KILL (destroyForcibly); other signals are
     * accepted at the command line but mapped to TERM (which is the
     * POSIX default) and a soft warning is not emitted.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return The exit code: 0 on success, 1 on usage error, 127 if any PID cannot be signalled
     */
    static int doKill(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
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

    private static int signalOne(final String pidstr, final boolean forcibly) {
        int code;
        try {
            final long pid = Long.parseLong(pidstr);
            if (JobManager.isJobPid(pid)) {
                code = 127;
            } else {
                final Optional<ProcessHandle> handle = ProcessHandle.of(pid);
                if (handle.isPresent()) {
                    final boolean sent;
                    if (forcibly) {
                        sent = handle.get().destroyForcibly();
                    } else {
                        sent = handle.get().destroy();
                    }
                    if (sent) {
                        code = 0;
                    } else {
                        code = 127;
                    }
                } else {
                    code = 127;
                }
            }
        } catch (final NumberFormatException ex) {
            code = 127;
        }
        return code;
    }

    static int doHash(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        for (final String arg : args) {
            if ("-r".equals(arg)) {
                continue;
            }
            final String name = self.expandVariables(arg, env);
            if (!self.isBuiltinName(name) && self.resolveCommand(name) == null) {
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
     *
     * @param self    Builtin context
     * @param args    Args after the command name (PIDs)
     * @param env     Environment for word expansion
     * @return The exit code: 0 on success, 127 when a named PID is not a known bg job
     */
    static int doWait(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        if (args.isEmpty()) {
            self.waitForBackgroundJobs();
        } else {
            for (final String arg : args) {
                final String expanded = self.expandVariables(arg, env);
                try {
                    code = self.jobManager().waitForPid(Long.parseLong(expanded));
                } catch (final NumberFormatException ex) {
                    code = 127;
                }
            }
        }
        return code;
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
     * Implementation of {@code read [-r] var...}.
     * POSIX 2.14: reads one line from stdin, splits by IFS into variables.
     * The last variable receives all remaining fields.
     * If no variables are given, stores the line in {@code REPLY}.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for variable lookup
     * @return The exit code: 0 on success, 1 on EOF
     */
    static int doRead(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        boolean raw = false;
        int idx = 0;
        if (!args.isEmpty() && "-r".equals(args.get(0))) {
            raw = true;
            idx = 1;
        }
        final List<String> names = args.subList(idx, args.size());
        final String line = self.readInputLine();
        int code;
        if (line == null) {
            code = 1;
        } else {
            final String processed;
            if (raw) {
                processed = line;
            } else {
                processed = processReadEscapes(line);
            }
            final Map<String, String> environment = self.environmentMap();
            if (names.isEmpty()) {
                environment.put("REPLY", processed);
                code = 0;
            } else {
                final String ifs = env.getOrDefault(
                    "IFS", String.format(" \t%n")
                );
                final char firstifs = ifs.charAt(0);
                final List<String> fields = splitReadFields(
                    processed, ifs, firstifs
                );
                for (int vdx = 0; vdx < names.size(); vdx = vdx + 1) {
                    final String name = names.get(vdx);
                    if (vdx == names.size() - 1) {
                        environment.put(
                            name, joinReadFields(fields, vdx, firstifs)
                        );
                    } else if (vdx < fields.size()) {
                        environment.put(name, fields.get(vdx));
                    } else {
                        environment.put(name, "");
                    }
                }
                code = 0;
            }
        }
        return code;
    }

    private static String processReadEscapes(final String line) {
        final StringBuilder out = new StringBuilder(line.length());
        boolean escaped = false;
        for (int idx = 0; idx < line.length(); idx = idx + 1) {
            final char chr = line.charAt(idx);
            if (escaped) {
                out.append(chr);
                escaped = false;
            } else if (chr == '\\') {
                escaped = true;
            } else {
                out.append(chr);
            }
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private static List<String> splitReadFields(
        final String line, final String ifs, final char firstifs
    ) {
        final List<String> fields = new ArrayList<>(0);
        int pos = 0;
        while (pos < line.length() && ifs.indexOf(line.charAt(pos)) >= 0
            && (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) {
            pos = pos + 1;
        }
        final StringBuilder current = new StringBuilder();
        while (pos < line.length()) {
            final char chr = line.charAt(pos);
            if (chr == firstifs && firstifs != ' ' && firstifs != '\t') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (ifs.indexOf(chr) >= 0) {
                fields.add(current.toString());
                current.setLength(0);
                while (pos + 1 < line.length()
                    && ifs.indexOf(line.charAt(pos + 1)) >= 0) {
                    pos = pos + 1;
                }
            } else {
                current.append(chr);
            }
            pos = pos + 1;
        }
        final boolean trailws = current.length() == 0 && pos > 0
            && (line.charAt(pos - 1) == ' ' || line.charAt(pos - 1) == '\t');
        if (!trailws || fields.isEmpty()) {
            fields.add(current.toString());
        }
        return fields;
    }

    private static String joinReadFields(
        final List<String> fields, final int start,
        final char firstifs
    ) {
        final StringBuilder out = new StringBuilder();
        for (int idx = start; idx < fields.size(); idx = idx + 1) {
            if (idx > start) {
                out.append(firstifs);
            }
            out.append(fields.get(idx));
        }
        return out.toString();
    }

    /**
     * Implementation of {@code alias [name[=value]...]}.
     * POSIX 2.14: without args, prints all aliases.
     * With name=value, sets an alias. With plain name, prints that alias.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param assigns Inline assignments (unused)
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

    private static int applyAliasArg(
        final BuiltinContext self, final Map<String, String> aliases,
        final String arg, final int code
    ) {
        int result = code;
        final int eqidx = arg.indexOf('=');
        if (eqidx >= 0) {
            String value = arg.substring(eqidx + 1);
            if (value.length() >= 2
                && (value.startsWith("'") && value.endsWith("'")
                || value.startsWith("\"") && value.endsWith("\""))) {
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

    /**
     * Implementation of {@code unalias [-a] name...}.
     * POSIX 2.14: removes named aliases, or all aliases with -a.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
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

    /**
     * Implementation of {@code getopts optstring var [args...]}.
     * POSIX 2.14: parses option characters from positional parameters
     * (or optional args). Uses OPTIND/OPTARG/OPTERR env vars for state.
     *
     * @param self    Builtin context
     * @param args    Args: optstring, var, optional positional replacement
     * @return The exit code: 0 if an option was found, 1 if all options processed
     */
    static int doGetopts(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code;
        if (args.size() < 2) {
            code = 1;
        } else {
            code = doGetoptsBody(self, args);
        }
        return code;
    }

    private static int doGetoptsBody(
        final BuiltinContext self, final List<String> args
    ) {
        final String optstring = args.get(0);
        final String varname = args.get(1);
        final List<String> pargs;
        if (args.size() > 2) {
            pargs = args.subList(2, args.size());
        } else {
            final List<String> pos = self.positionalParamsList();
            pargs = pos.subList(1, pos.size());
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
        final int early;
        if (optind > pargs.size()) {
            environment.put(varname, "?");
            environment.put("OPTIND", String.valueOf(pargs.size() + 1));
            early = 1;
        } else if ("--".equals(pargs.get(optind - 1))) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            environment.put(varname, "?");
            early = 1;
        } else if (pargs.get(optind - 1).length() < 2
            || pargs.get(optind - 1).charAt(0) != '-') {
            environment.put(varname, "?");
            early = 1;
        } else {
            early = 0;
        }
        int code;
        if (early != 0) {
            code = early;
        } else {
            code = doGetoptsTail(self, environment, optstring, varname, pargs, optind);
        }
        return code;
    }

    private static int doGetoptsTail(
        final BuiltinContext self,
        final Map<String, String> environment,
        final String optstring,
        final String varname,
        final List<String> pargs,
        final int optindin
    ) {
        final String current = pargs.get(optindin - 1);
        int optind = optindin;
        int optpos;
        if (environment.containsKey("OPTIND_POS")) {
            optpos = Integer.parseInt(environment.get("OPTIND_POS"));
        } else {
            optpos = 1;
        }
        boolean exhausted = false;
        if (optpos >= current.length()) {
            optind = optind + 1;
            environment.put("OPTIND", String.valueOf(optind));
            if (optind > pargs.size()) {
                environment.put(varname, "?");
                environment.remove("OPTIND_POS");
                exhausted = true;
            } else if (pargs.get(optind - 1).length() < 2
                || pargs.get(optind - 1).charAt(0) != '-') {
                environment.put(varname, "?");
                environment.remove("OPTIND_POS");
                exhausted = true;
            } else {
                environment.put("OPTIND_POS", String.valueOf(1));
                optpos = 1;
            }
        }
        final int code;
        if (exhausted) {
            code = 1;
        } else {
            final char optchar = current.charAt(optpos);
            environment.put("OPTIND_POS", String.valueOf(optpos + 1));
            final int optidx = optstring.indexOf(optchar);
            if (optidx < 0) {
                if (optstring.length() > 0 && optstring.charAt(0) == ':') {
                    environment.put(varname, "?");
                    environment.put("OPTARG", String.valueOf(optchar));
                } else {
                    self.errStream().println(
                        String.format("sh: illegal option -- %s", optchar)
                    );
                    environment.put(varname, "?");
                    environment.put("OPTARG", String.valueOf(optchar));
                }
                code = 0;
            } else {
                code = doGetoptsArgument(
                    self, environment, optstring, varname, pargs, optind,
                    optpos, current, optchar, optidx
                );
            }
        }
        return code;
    }

    private static int doGetoptsArgument(
        final BuiltinContext self,
        final Map<String, String> environment,
        final String optstring,
        final String varname,
        final List<String> pargs,
        final int optind,
        final int optpos,
        final String current,
        final char optchar,
        final int optidx
    ) {
        final boolean takesarg = optidx + 1 < optstring.length()
            && optstring.charAt(optidx + 1) == ':';
        int code = 0;
        boolean stop = false;
        if (takesarg) {
            environment.put("OPTIND", String.valueOf(optind + 1));
            if (optpos + 1 < current.length()) {
                environment.put("OPTARG", current.substring(optpos + 1));
            } else if (optind < pargs.size()) {
                environment.put("OPTARG", pargs.get(optind));
                environment.put("OPTIND", String.valueOf(optind + 2));
            } else {
                if (optstring.length() > 0 && optstring.charAt(0) == ':') {
                    environment.put(varname, ":");
                } else {
                    self.errStream().println(
                        String.format(
                            "sh: option requires an argument -- %s", optchar
                        )
                    );
                    environment.put(varname, "?");
                }
                environment.put("OPTARG", "");
                environment.remove("OPTIND_POS");
                stop = true;
            }
            if (!stop) {
                environment.remove("OPTIND_POS");
            }
        } else {
            environment.put("OPTARG", "");
        }
        if (!stop) {
            environment.put(varname, String.valueOf(optchar));
            if (!takesarg && optpos + 1 >= current.length()) {
                environment.put("OPTIND", String.valueOf(optind + 1));
                environment.remove("OPTIND_POS");
            }
        }
        return code;
    }

    private static int parseJobId(final String arg) {
        final String digits;
        if (arg.startsWith("%")) {
            digits = arg.substring(1);
        } else {
            digits = arg;
        }
        int parsed = -1;
        try {
            parsed = Integer.parseInt(digits);
        } catch (final NumberFormatException ex) {
            parsed = -1;
        }
        return parsed;
    }

    /**
     * Implementation of {@code jobs [%n...]}.
     * POSIX 2.14: without args, lists all background jobs with their
     * state and command line.
     *
     * @param self    Builtin context
     * @param args    Args after the command name (optional job ids)
     * @return The exit code: 0
     */
    static int doJobs(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final JobManager mgr = self.jobManager();
        final List<Job> all;
        if (args.isEmpty()) {
            all = mgr.listJobs();
        } else {
            all = new ArrayList<>(0);
            for (final String arg : args) {
                final int id = parseJobId(arg);
                if (id > 0) {
                    final Job job = mgr.getJob(id);
                    if (job != null) {
                        all.add(job);
                    }
                }
            }
        }
        for (final Job job : all) {
            final String state = switch (job.getState()) {
                case RUNNING -> "Running";
                case DONE -> "Done";
                default -> "Stopped";
            };
            self.outStream().printf("[%d] %s %s%n", job.getId(), state, job.getCommand());
        }
        return 0;
    }

    /**
     * Implementation of {@code bg [%n...]}.
     * POSIX 2.14: resumes suspended jobs in the background. Without args,
     * operates on the most recent job. Prints a confirmation line.
     *
     * @param self    Builtin context
     * @param args    Args after the command name (optional job ids)
     * @return The exit code: 0 on success, 1 if no matching job
     */
    static int doBg(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final JobManager mgr = self.jobManager();
        final List<Integer> ids = new ArrayList<>(0);
        if (args.isEmpty()) {
            final List<Job> all = mgr.listJobs();
            if (all.isEmpty()) {
                self.errStream().println("sh: bg: no current job");
                return 1;
            }
            ids.add(all.get(all.size() - 1).getId());
        } else {
            for (final String arg : args) {
                final int id = parseJobId(arg);
                if (id <= 0 || mgr.getJob(id) == null) {
                    self.errStream().println(String.format("sh: bg: %s: no such job", arg));
                    return 1;
                }
                ids.add(id);
            }
        }
        for (final int id : ids) {
            final Job job = mgr.getJob(id);
            if (job != null && job.getState() == JobManager.State.STOPPED) {
                job.resume();
            }
            final String command;
            if (job == null) {
                command = "";
            } else {
                command = job.getCommand();
            }
            self.outStream().printf("[%d] %s%n", id, command);
        }
        return 0;
    }

    /**
     * Implementation of {@code fg [%n]}.
     * POSIX 2.14: brings a background job to the foreground, waiting for
     * it to complete. Without args, operates on the most recent job.
     *
     * @param self    Builtin context
     * @param args    Args after the command name (optional job id)
     * @return The job's exit code, or 1 on error
     */
    static int doFg(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final JobManager mgr = self.jobManager();
        final int id;
        if (args.isEmpty()) {
            final List<Job> all = mgr.listJobs();
            if (all.isEmpty()) {
                self.errStream().println("sh: fg: no current job");
                return 1;
            }
            id = all.get(all.size() - 1).getId();
        } else {
            id = parseJobId(args.get(0));
            if (id <= 0) {
                self.errStream().println(String.format("sh: fg: %s: no such job", args.get(0)));
                return 1;
            }
        }
        final Job job = mgr.getJob(id);
        if (job == null) {
            self.errStream().printf("sh: fg: %d: no such job%n", id);
            return 1;
        }
        self.outStream().println(job.getCommand());
        return mgr.waitForJob(id);
    }

    /**
     * Implementation of {@code fc [-l] [-n] [-s pat=rep] [first [last]]}.
     * POSIX 2.14: lists or re-executes history entries.
     * {@code -l} lists entries (default), {@code -n} suppresses numbers,
     * {@code -s pat=rep} substitutes and re-executes the last command.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param assigns Inline assignments (unused)
     * @return The exit code: 0 on success, 1 on error
     */
    static int doFc(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean listmode = true;
        boolean nonumbers = false;
        String substitute = null;
        int idx = 0;
        while (idx < args.size() && args.get(idx).startsWith("-")
            && !args.get(idx).matches("-\\d+")) {
            final String flag = args.get(idx);
            for (int fidx = 1; fidx < flag.length(); fidx = fidx + 1) {
                final char chr = flag.charAt(fidx);
                if (chr == 'l') {
                    listmode = true;
                } else if (chr == 'n') {
                    nonumbers = true;
                } else if (chr == 'e') {
                    idx = idx + 1;
                    if (idx >= args.size()) {
                        self.errStream().println("sh: fc: -e requires argument");
                        return 1;
                    }
                } else if (chr == 's') {
                    listmode = false;
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
                    self.errStream().println(String.format("sh: fc: unknown option -%s", chr));
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
            return fcSubstitute(self, hist, substitute, first);
        }
        if (listmode) {
            return fcList(self, hist, first, last, nonumbers);
        }
        return 0;
    }

    private static int parseFcNumber(final String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (final NumberFormatException ex) {
            return -1;
        }
    }

    private static int fcList(
        final BuiltinContext self, final CommandHistory hist,
        final int first, final int last, final boolean nonumbers
    ) {
        final List<HistoryEntry> entries = hist.list(first, last);
        for (final HistoryEntry entry : entries) {
            if (nonumbers) {
                self.outStream().println(String.format("\t%s", entry.command()));
            } else {
                self.outStream().printf("%d\t%s%n", entry.number(), entry.command());
            }
        }
        return 0;
    }

    private static int fcSubstitute(
        final BuiltinContext self, final CommandHistory hist,
        final String pattern, final int number
    ) {
        final int eqidx = pattern.indexOf('=');
        if (eqidx < 0) {
            self.errStream().println("sh: fc: invalid substitution pattern");
            return 1;
        }
        final String oldstr = pattern.substring(0, eqidx);
        final String newstr = pattern.substring(eqidx + 1);
        final String cmd = hist.get(number);
        if (cmd == null) {
            self.errStream().printf("sh: fc: %d: no such history entry%n", number);
            return 1;
        }
        final String replaced = cmd.replace(oldstr, newstr);
        self.outStream().println(replaced);
        return self.execute(
            new Parser(new Lexer(new CharReader(replaced))).parse()
        );
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
     * Implementation of {@code trap [action signal ...]}.
     * Only the EXIT trap is executed by the executor; other signals are
     * recorded but the JDK does not expose portable signal handlers.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @return The exit code: 0
     */
    static int doTrap(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final int code;
        if (args.isEmpty()) {
            for (final Map.Entry<String, String> entry : self.traps().entrySet()) {
                self.outStream().println(
                    String.format("trap -- '%s' %s", entry.getValue(), entry.getKey())
                );
            }
            code = 0;
        } else if ("-".equals(args.get(0))) {
            for (int idx = 1; idx < args.size(); idx = idx + 1) {
                self.setTrap(args.get(idx).toUpperCase(Locale.ROOT), null);
            }
            code = 0;
        } else {
            final String action = WordExpander.removeQuotes(args.get(0));
            for (int idx = 1; idx < args.size(); idx = idx + 1) {
                self.setTrap(
                    args.get(idx).toUpperCase(Locale.ROOT), action
                );
            }
            code = 0;
        }
        return code;
    }

    /**
     * Implementation of {@code times}.
     *
     * @param self    Builtin context
     * @return The exit code: 0
     */
    static int doTimes(
        final BuiltinContext self,
        final Map<String, String> env
    ) {
        self.outStream().println("0m0.000s 0m0.000s");
        self.outStream().println("0m0.000s 0m0.000s");
        return 0;
    }

    /**
     * Implementation of {@code . file [args...]}.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return The sourced script's exit status, or 1 on IO/parse failure
     */
    static int doDot(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code;
        if (args.isEmpty()) {
            code = 1;
        } else {
            final String path = WordExpander.removeQuotes(
                self.expandVariables(args.get(0), env)
            );
            try {
                final File raw = new File(path);
                final Path rawpath;
                if (raw.isAbsolute()) {
                    rawpath = raw.toPath();
                } else {
                    rawpath = new File(self.currentDirectory(), path).toPath();
                }
                final String source = new String(
                    Files.readAllBytes(rawpath), StandardCharsets.UTF_8
                );
                final List<String> saved = new ArrayList<>(self.positionalParamsList());
                if (args.size() > 1) {
                    self.setPositionalParams(args.subList(1, args.size()));
                }
                try {
                    try {
                        code = self.execute(
                            new Parser(new Lexer(new CharReader(source))).parse()
                        );
                    } catch (final ReturnSignal ex) {
                        code = ex.getCode();
                    }
                } finally {
                    self.positionalParamsList().clear();
                    self.positionalParamsList().addAll(saved);
                }
            } catch (final IOException ex) {
                self.errStream().println(String.format("sh: %s: cannot open", path));
                code = 1;
            }
        }
        return code;
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
                        String.format("readonly %s=%s", entry.getKey(), entry.getValue())
                    );
                }
            }
            return 0;
        }
        for (final String arg : all) {
            final int eqidx = arg.indexOf('=');
            if (eqidx >= 0) {
                final String name = arg.substring(0, eqidx);
                final String expanded = self.expandVariables(arg.substring(eqidx + 1), env);
                environment.put(name, WordExpander.removeQuotes(expanded));
                self.markReadonly(name);
            } else {
                self.markReadonly(arg);
            }
        }
        return 0;
    }

}
