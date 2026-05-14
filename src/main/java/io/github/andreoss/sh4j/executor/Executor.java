/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.parser.FunctionNode;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tree-walking executor for the AST produced by {@link Parser}. Top-level
 * dispatch lives here; subsystems delegate to {@link WordExpander},
 * {@link ArithmeticEvaluator}, {@link RedirectionHandler}, and
 * {@link BuiltinDispatcher}.
 *
 * @since 1.0
 */
public final class Executor implements BuiltinContext {

    /** Descriptors bound by a redirection-only exec. */
    private final Descriptors fds;

    /** Live shell variables with their export and read-only markers. */
    private final ShellVariables vars;

    /** Trap actions keyed by signal name (EXIT is the only executed one). */
    private final Map<String, String> traps;

    /** Per-executor current working directory. */
    private File cwd;

    /** Defined shell functions keyed by name. */
    private final Map<String, FunctionNode> functions;

    /** {@code $0}..{@code $N} positional parameters (index 0 = script name). */
    private final List<String> params;

    /** Background-job threads spawned by {@code &}. */
    private final JobManager jobs;

    /** Dispatcher for shell built-ins. */
    private final BuiltinDispatcher dispatcher;

    /** Arithmetic-expansion evaluator. */
    private final ArithmeticEvaluator arith;

    /** Most recent foreground exit code ({@code $?}). */
    private int lastcode;

    /** Current shell-flag string ({@code $-}). */
    private String flags;

    /** Most recent background-job PID ({@code $!}). */
    private int bgpid;

    /** Live stdout used by built-ins. */
    private PrintSink out;

    /** Live stderr used by built-ins. */
    private PrintSink err;

    /** Live stdin used by built-ins (read, etc.). */
    private InputStream instream;

    /** Alias table (alias name -> replacement text). */
    private Map<String, String> aliases;

    /** Command history for fc built-in. */
    private final CommandHistory history;

    /** Nesting depth of {@link #execute(ListNode)} calls. */
    private int depth;

    /** Alias table captured when the current source line started. */
    private final AliasSnapshot snapshot;

    /** Exit status of the last command substitution (-1 when none ran). */
    private int substat;

    /** Runner of compound commands, tracking the loop depth. */
    private final Compound compound;

    /** State of the errexit option. */
    private final Errexit errexit;

    /**
     * Builds an executor seeded with the current process environment.
     */
    public Executor() {
        this(System.getenv(), Collections.singletonList("sh"));
    }

    /**
     * Builds an executor with the given environment view.
     *
     * @param environment Environment variables (copied, not aliased)
     */
    public Executor(final Map<String, String> environment) {
        this(environment, Collections.singletonList("sh"));
    }

    /**
     * Builds an executor with the given environment and positional params.
     *
     * @param environment       Environment variables (copied)
     * @param params  Positional parameters ({@code $0} first)
     */
    public Executor(
        final Map<String, String> environment,
        final List<String> params
    ) {
        this(
            new ShellVariables(Variables.of(environment), Exports.of(environment)),
            params, Executor.initialDirectory()
        );
    }

    private Executor(
        final ShellVariables vars,
        final List<String> incoming,
        final File dir
    ) {
        this.fds = new Descriptors();
        this.vars = vars;
        this.cwd = dir;
        this.substat = -1;
        this.snapshot = new AliasSnapshot();
        this.errexit = new Errexit();
        this.compound = new Compound(this, this.errexit);
        this.traps = new HashMap<>();
        this.functions = new HashMap<>();
        this.params = new ArrayList<>(incoming);
        this.jobs = new JobManager();
        this.dispatcher = new BuiltinDispatcher();
        this.arith = new ArithmeticEvaluator();
        this.lastcode = 0;
        this.flags = "";
        this.bgpid = 0;
        this.out = new PrintSink(System.out, StandardCharsets.UTF_8);
        this.err = new PrintSink(System.err, StandardCharsets.UTF_8);
        this.instream = System.in;
        this.aliases = new HashMap<>();
        this.history = new CommandHistory();
    }

    @Override
    public Map<String, String> exportedEnvironment() {
        return this.vars.exports();
    }

    @Override
    public void markExported(final String name) {
        this.vars.export(name);
    }

    @Override
    public void markReadonly(final String name) {
        this.vars.protect(name);
    }

    @Override
    public boolean isReadonly(final String name) {
        return this.vars.locked(name);
    }

    @Override
    public boolean unsetVariable(final String name) {
        return this.vars.unset(name);
    }

    @Override
    public void setCurrentDirectory(final File dir) {
        this.cwd = dir.getAbsoluteFile();
    }

    @Override
    public File currentDirectory() {
        return this.cwd;
    }

    @Override
    public boolean isBuiltinName(final String name) {
        return this.dispatcher.isBuiltin(name);
    }

    @Override
    public String readInputLine() {
        return InputLine.read(this.instream);
    }

    @Override
    public void setTrap(final String signal, final String action) {
        if (action == null) {
            this.traps.remove(signal);
        } else {
            this.traps.put(signal, action);
        }
    }

    @Override
    public Map<String, String> traps() {
        return this.traps;
    }

    @Override
    public String executeCommandSubstitution(final String text) {
        return new Substitution(this).capture(text);
    }

    @Override
    public String evalArithmetic(final String expr, final Map<String, String> env) {
        String result = "";
        boolean fatal = false;
        try {
            result = this.arith.evaluate(
                expr, new ArithmeticScope(env, this.environmentMap())
            );
        } catch (final ArithmeticException ex) {
            this.err.println(String.format("sh: %s: %s", expr.trim(), ex.getMessage()));
            fatal = true;
        }
        if (fatal) {
            throw new ExitSignal(1);
        }
        return result;
    }

    @Override
    public int execute(final ListNode list) {
        if (this.depth == 0) {
            this.snapshot.reset();
        }
        this.depth = this.depth + 1;
        int code;
        try {
            code = new Pipelines(this).run(list);
        } catch (final ExitSignal ex) {
            this.nested(ex);
            code = ex.getCode();
        } catch (final ReturnSignal ex) {
            this.nested(ex);
            this.err.println("sh: return: not in a function");
            code = ex.getCode();
        } catch (final BreakSignal | ContinueSignal ex) {
            this.nested(ex);
            code = 0;
        } finally {
            this.depth = this.depth - 1;
        }
        return code;
    }

    @Override
    public int lastBackgroundPid() {
        return this.bgpid;
    }

    @Override
    public void waitForBackgroundJobs() {
        this.jobs.waitForAll();
    }

    @Override
    public int executeBypassingFunctions(
        final String name, final List<String> args, final Map<String, String> env
    ) {
        return new NamedCommand(this).bypass(name, args, env);
    }

    @Override
    public String expandVariables(final String arg, final Map<String, String> env) {
        return WordExpander.expandVariablesImpl(arg, env, this);
    }

    @Override
    public List<String> expandArguments(final String arg, final Map<String, String> env) {
        return new Fields(this).arguments(arg, env);
    }

    @Override
    public List<String> expandFields(final String arg, final Map<String, String> env) {
        return new Fields(this).split(arg, env);
    }

    @Override
    public String lookupVariable(final String name, final Map<String, String> env) {
        return new Parameters(this, this.lastcode).lookup(name, env);
    }

    @Override
    public String getSpecialParam(final char name) {
        return new Parameters(this, this.lastcode).special(name);
    }

    @Override
    public String resolveCommand(final String name) {
        return ExternalCommand.resolve(name, this.vars.table());
    }

    @Override
    public Map<String, String> environmentMap() {
        return this.vars.table();
    }

    @Override
    public List<String> positionalParamsList() {
        return this.params;
    }

    @Override
    public Map<String, FunctionNode> functionsMap() {
        return this.functions;
    }

    @Override
    public Map<String, String> aliases() {
        return this.aliases;
    }

    @Override
    public JobManager jobManager() {
        return this.jobs;
    }

    @Override
    public CommandHistory history() {
        return this.history;
    }

    @Override
    public PrintSink outStream() {
        return this.out;
    }

    @Override
    public PrintSink errStream() {
        return this.err;
    }

    @Override
    public String shellFlags() {
        return this.flags;
    }

    @Override
    public void setShellFlags(final String value) {
        this.flags = value;
        this.vars.automatic(value.indexOf('a') >= 0);
    }

    @Override
    public void setPositionalParams(final List<String> incoming) {
        Parameters.replace(this.params, incoming);
    }

    /**
     * Runs and removes the EXIT trap with the status the shell exits with.
     *
     * @param status The shell exit status the trap observes as {@code $?}
     */
    public void runExitTrap(final int status) {
        this.lastcode = status;
        ExitTrap.run(this, this.traps.remove("EXIT"));
    }

    /**
     * Returns a variable's current value.
     *
     * @param name A variable name
     * @return The variable's current value, or null when unset
     */
    public String getVariable(final String name) {
        return this.vars.table().get(name);
    }

    /**
     * Redirects subsequent stdout into the given buffer (used by command
     * substitution).
     *
     * @param stream Destination for captured stdout
     */
    public void setCaptureStream(final ByteArrayOutputStream stream) {
        this.out = new PrintSink(stream, StandardCharsets.UTF_8);
    }

    /**
     * Overrides the executor's stdout.
     *
     * @param outlet The new live stdout
     */
    public void setOutputStream(final PrintSink outlet) {
        this.out = outlet;
    }

    /**
     * Overrides the executor's stderr.
     *
     * @param errlet The new live stderr
     */
    public void setErrorStream(final PrintSink errlet) {
        this.err = errlet;
    }

    /**
     * Overrides the executor's stdin.
     *
     * @param inlet The new live stdin
     */
    public void setInputStream(final InputStream inlet) {
        this.instream = inlet;
    }

    /**
     * Returns the most recent background-job PID.
     *
     * @return The most recent background-job PID ({@code $!})
     */
    int getLastBgPid() {
        return this.bgpid;
    }

    /**
     * Returns the live stdin.
     *
     * @return The live stdin
     */
    InputStream inputStream() {
        return this.instream;
    }

    /**
     * Returns the exit status of the most recent foreground pipeline.
     *
     * @return The value of {@code $?}
     */
    int status() {
        return this.lastcode;
    }

    /**
     * Records the exit status of the most recent foreground pipeline.
     *
     * @param code The new value of {@code $?}
     */
    void status(final int code) {
        this.lastcode = code;
    }

    /**
     * Records the PID of the most recent background job.
     *
     * @param pid The new value of {@code $!}
     */
    void background(final int pid) {
        this.bgpid = pid;
    }

    /**
     * Handles a {@code break} or {@code continue} that reached a list.
     *
     * @param signal The signal, rethrown inside a loop
     * @param name The name of the built-in for the diagnostic
     */
    void requireLoop(final RuntimeException signal, final String name) {
        if (this.compound.inLoop()) {
            throw signal;
        }
        this.err.println(String.format("sh: %s: not in a loop", name));
        this.lastcode = 0;
    }

    /**
     * Creates a subshell environment copied from this shell.
     *
     * @return A new executor sharing nothing mutable with this one
     */
    Executor subshell() {
        final Executor child = new Executor(this.exportedEnvironment());
        child.vars.inherit(this.vars);
        child.traps.putAll(this.traps);
        child.functions.putAll(this.functions);
        child.aliases.putAll(this.aliases);
        child.params.clear();
        child.params.addAll(this.params);
        child.setShellFlags(this.flags);
        child.errexit.inherit(this.errexit);
        child.cwd = this.cwd;
        child.out = this.out;
        child.err = this.err;
        child.instream = this.instream;
        child.lastcode = this.lastcode;
        return child;
    }

    /**
     * Runs one command node.
     *
     * @param node The node
     * @return The exit status
     */
    int executeNode(final Node node) {
        return new NodeRunner(this, this.compound).run(node);
    }

    /**
     * Returns the state of the errexit option.
     *
     * @return The errexit state
     */
    Errexit errexit() {
        return this.errexit;
    }

    /**
     * Returns the descriptors bound by a redirection-only {@code exec}.
     *
     * @return The bound descriptors
     */
    Descriptors descriptors() {
        return this.fds;
    }

    /**
     * Returns the alias table captured for the current source line.
     *
     * @return The alias snapshot
     */
    AliasSnapshot snapshot() {
        return this.snapshot;
    }

    /**
     * Returns the exit status of the last command substitution.
     *
     * @return The exit status, -1 when none ran since the last reset
     */
    int substitution() {
        return this.substat;
    }

    /**
     * Records the exit status of the last command substitution.
     *
     * @param code The exit status, -1 to reset
     */
    void substitution(final int code) {
        this.substat = code;
    }

    /**
     * Runs a built-in.
     *
     * @param name The built-in name
     * @param call The arguments, environment, and assignments of the call
     * @return The exit status, or null when the name is not a built-in
     */
    Integer builtin(final String name, final Invocation call) {
        return this.dispatcher.dispatch(name, this, call);
    }

    private static File initialDirectory() {
        return new File(
            System.getProperty("user.dir", Variables.FALLBACK_DIR)
        );
    }

    private void nested(final RuntimeException signal) {
        if (this.depth > 1) {
            throw signal;
        }
    }
}
