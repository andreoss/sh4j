/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.CaseItem;
import io.github.andreoss.sh4j.parser.CaseNode;
import io.github.andreoss.sh4j.parser.ForNode;
import io.github.andreoss.sh4j.parser.FunctionNode;
import io.github.andreoss.sh4j.parser.Grouping;
import io.github.andreoss.sh4j.parser.IfNode;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;
import io.github.andreoss.sh4j.parser.WhileNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tree-walking executor for the AST produced by {@link Parser}. Top-level
 * dispatch lives here; subsystems delegate to {@link WordExpander},
 * {@link ArithmeticEvaluator}, {@link RedirectionHandler}, and
 * {@link BuiltinDispatcher}.
 *
 * @since 1.0
 */
public final class Executor implements BuiltinContext {

    /**
     * POSIX 2.14 special built-in utilities - these take priority over user-
     * defined functions in command search (POSIX 2.9.1.1).
     */
    private static final java.util.Set<String> SPECIAL_BUILTINS = Set.of(
        ":", ".", "break", "continue", "eval", "exec", "exit", "export",
        "readonly", "return", "set", "shift", "times", "trap", "unset"
    );

    /** Default IFS value (space, tab, newline). */
    private static final String DEFAULT_IFS = Variables.DEFAULT_IFS;

    /** Exit code for a command that could not be found on PATH. */
    static final int EXIT_COMMAND_NOT_FOUND = 127;

    /** Input streams bound to descriptors by a redirection-only exec. */
    private final Map<Integer, InputStream> descriptors = new HashMap<>();

    /** Output streams bound to descriptors by a redirection-only exec. */
    private final Map<Integer, PrintSink> outdescs = new HashMap<>();

    /** Live shell-variable table. */
    private final Map<String, String> environment;

    /** Names of variables visible to child processes. */
    private final Set<String> exported;

    /** Names of read-only variables. */
    private final Set<String> readonly;

    /** Trap actions keyed by signal name (EXIT is the only executed one). */
    private final Map<String, String> traps;

    /** Per-executor current working directory. */
    private File cwd = new File(System.getProperty(
        "user.dir", Variables.FALLBACK_DIR
    ));

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
    private Map<String, String> snapshot;

    /** Source line the alias snapshot belongs to. */
    private int snapline;

    /** Exit status of the last command substitution (-1 when none ran). */
    private int substat = -1;

    /** True when the current input swap borrowed a descriptor stream. */
    private boolean borrowed;

    /** Nesting depth of active loop bodies. */
    private int loops;

    /**
     * Builds an executor seeded with the current process environment.
     */
    public Executor() {
        this(System.getenv(), Arrays.asList("sh"));
    }

    /**
     * Builds an executor with the given environment view.
     *
     * @param environment Environment variables (copied, not aliased)
     */
    public Executor(final Map<String, String> environment) {
        this(environment, Arrays.asList("sh"));
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
        this.environment = new Variables(environment);
        this.exported = new Exports(environment);
        this.readonly = new HashSet<>();
        this.traps = new HashMap<>();
        this.functions = new HashMap<>();
        this.params = new ArrayList<>(params);
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
        final Map<String, String> result = new HashMap<>();
        for (final String name : this.exported) {
            final String value = this.environment.get(name);
            if (value != null) {
                result.put(name, value);
            }
        }
        return result;
    }

    @Override
    public void markExported(final String name) {
        this.exported.add(name);
    }

    @Override
    public void markReadonly(final String name) {
        this.readonly.add(name);
    }

    @Override
    public boolean isReadonly(final String name) {
        return this.readonly.contains(name);
    }

    @Override
    public boolean unsetVariable(final String name) {
        final boolean removed;
        if (this.readonly.contains(name)) {
            removed = false;
        } else {
            this.environment.remove(name);
            this.exported.remove(name);
            removed = true;
        }
        return removed;
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
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String line = null;
        try {
            int read = this.instream.read();
            while (read != -1 && read != '\n') {
                buffer.write(read);
                read = this.instream.read();
            }
            if (read != -1 || buffer.size() > 0) {
                line = buffer.toString(StandardCharsets.UTF_8);
            }
        } catch (final IOException ex) {
            line = null;
        }
        return line;
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
        final ListNode ast = new Parser(new Lexer(new CharReader(text))).parse();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Executor sub = this.subshell();
        sub.setCaptureStream(baos);
        this.substat = sub.execute(ast);
        String captured = baos.toString(StandardCharsets.UTF_8);
        while (captured.endsWith("\n")) {
            captured = captured.substring(0, captured.length() - 1);
        }
        return captured;
    }

    @Override
    public String evalArithmetic(final String expr, final Map<String, String> env) {
        return this.arith.evalArithmetic(expr, env);
    }

    @Override
    public int execute(final ListNode list) {
        if (this.depth == 0) {
            this.snapshot = null;
            this.snapline = -1;
        }
        this.depth = this.depth + 1;
        int code;
        try {
            code = 0;
            if (!list.getPipelines().isEmpty()) {
                code = this.runPipelineList(list);
            }
        } catch (final ExitSignal ex) {
            if (this.depth > 1) {
                throw ex;
            }
            code = ex.getCode();
        } catch (final ReturnSignal ex) {
            if (this.depth > 1) {
                throw ex;
            }
            this.err.println("sh: return: not in a function");
            code = ex.getCode();
        } catch (final BreakSignal ex) {
            if (this.depth > 1) {
                throw ex;
            }
            code = 0;
        } catch (final ContinueSignal ex) {
            if (this.depth > 1) {
                throw ex;
            }
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
        final SimpleCommand command = new SimpleCommand(name);
        for (final String arg : args) {
            command.addArgument(arg);
        }
        return this.runNamedCommand(command, name, new HashMap<>(env), true);
    }

    @Override
    public String expandVariables(final String arg, final Map<String, String> env) {
        return WordExpander.expandVariablesImpl(arg, env, this);
    }

    @Override
    public List<String> expandArguments(final String arg, final Map<String, String> env) {
        List<String> fields = this.expandFields(arg, env);
        if (arg != null && !WordExpander.isQuoted(arg)
            && this.flags.indexOf('f') < 0) {
            fields = WordExpander.pathnameExpand(fields, this.cwd);
        }
        return fields;
    }

    @Override
    public List<String> expandFields(final String arg, final Map<String, String> env) {
        final List<String> fields = new ArrayList<>();
        if (arg == null) {
            return fields;
        }
        if ("\"$@\"".equals(arg) || "\"${@}\"".equals(arg)) {
            if (this.params.size() > 1) {
                fields.addAll(this.params.subList(1, this.params.size()));
            }
        } else {
            final String expanded = this.expandVariables(arg, env);
            if (WordExpander.isQuoted(arg)) {
                fields.add(WordExpander.removeQuotes(expanded));
            } else {
                final String ifsvalue = env.getOrDefault("IFS", DEFAULT_IFS);
                for (final String field : WordExpander.splitFields(expanded, ifsvalue)) {
                    fields.add(WordExpander.removeQuotes(field));
                }
            }
        }
        return fields;
    }

    @Override
    public String lookupVariable(final String name, final Map<String, String> env) {
        final String result;
        if (name.isEmpty()) {
            result = "";
        } else if (name.length() == 1) {
            result = this.soleParam(name, env);
        } else {
            result = this.lookupNamed(name, env);
        }
        return result;
    }

    @Override
    public String getSpecialParam(final char name) {
        return switch (name) {
            case '?' -> String.valueOf(this.lastcode);
            case '-' -> this.flags;
            case '$' -> String.valueOf(ProcessHandle.current().pid());
            case '!' -> String.valueOf(this.bgpid);
            case '#' -> String.valueOf(this.params.size() - 1);
            case '@', '*' -> this.positionalParamsJoined();
            default -> this.positionalParamForDigit(name);
        };
    }

    @Override
    public String resolveCommand(final String name) {
        return this.resolveCommand(name, this.environment);
    }

    @Override
    public Map<String, String> environmentMap() {
        return this.environment;
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
    }

    @Override
    public void setPositionalParams(final List<String> incoming) {
        final String zeroth;
        if (this.params.isEmpty()) {
            zeroth = "";
        } else {
            zeroth = this.params.get(0);
        }
        this.params.clear();
        this.params.add(zeroth);
        this.params.addAll(incoming);
    }

    /**
     * Runs and removes the EXIT trap using the last exit status.
     */
    public void runExitTrap() {
        this.runExitTrap(this.lastcode);
    }

    /**
     * Runs and removes the EXIT trap with the status the shell exits with.
     *
     * @param status The shell exit status the trap observes as {@code $?}
     */
    public void runExitTrap(final int status) {
        this.lastcode = status;
        final String action = this.traps.remove("EXIT");
        if (action != null && !action.isEmpty() && !"-".equals(action)) {
            try {
                this.execute(
                    new Parser(new Lexer(new CharReader(action))).parse()
                );
            } catch (final RuntimeException ex) {
                this.err.println("sh: trap: " + ex.getMessage());
            }
        }
    }

    /**
     * Returns a variable's current value.
     *
     * @param name A variable name
     * @return The variable's current value, or null when unset
     */
    public String getVariable(final String name) {
        return this.environment.get(name);
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

    private Executor subshell() {
        final Executor child = new Executor(this.exportedEnvironment());
        child.environment.clear();
        child.environment.putAll(this.environment);
        child.exported.clear();
        child.exported.addAll(this.exported);
        child.readonly.addAll(this.readonly);
        child.traps.putAll(this.traps);
        child.functions.putAll(this.functions);
        child.aliases.putAll(this.aliases);
        child.params.clear();
        child.params.addAll(this.params);
        child.flags = this.flags;
        child.cwd = this.cwd;
        child.out = this.out;
        child.err = this.err;
        child.instream = this.instream;
        child.lastcode = this.lastcode;
        return child;
    }

    private int runPipelineList(final ListNode list) {
        final List<Pipeline> pipelines = list.getPipelines();
        final List<ListNode.Separator> separators = list.getSeparators();
        int idx = 0;
        boolean stopped = false;
        while (!stopped && idx < pipelines.size()) {
            final Pipeline pipeline = pipelines.get(idx);
            final ListNode.Separator sep = separators.get(idx);
            final boolean run = idx == 0 || shouldRunNext(
                separators.get(idx - 1), this.lastcode
            );
            if (run) {
                if (sep == ListNode.Separator.AMP) {
                    this.executeAsync(pipeline);
                    this.lastcode = 0;
                } else {
                    final int status;
                    try {
                        status = this.executePipeline(pipeline);
                    } catch (final BreakSignal ex) {
                        if (this.loops > 0) {
                            throw ex;
                        }
                        this.err.println("sh: break: not in a loop");
                        this.lastcode = 0;
                        idx = idx + 1;
                        continue;
                    } catch (final ContinueSignal ex) {
                        if (this.loops > 0) {
                            throw ex;
                        }
                        this.err.println("sh: continue: not in a loop");
                        this.lastcode = 0;
                        idx = idx + 1;
                        continue;
                    }
                    this.lastcode = status;
                    this.history.add(pipelineToString(pipeline));
                    if (this.flags.indexOf('e') >= 0 && status != 0
                        && sep != ListNode.Separator.AND_IF
                        && sep != ListNode.Separator.OR_IF) {
                        stopped = true;
                    }
                }
            }
            idx = idx + 1;
        }
        return this.lastcode;
    }

    private void executeAsync(final Pipeline pipeline) {
        final String cmdtext = pipelineToString(pipeline);
        final Executor parent = this;
        final int jobnum = this.jobs.submit(
            () -> {
                final Executor bgexecutor = parent.subshell();
                bgexecutor.out = parent.out;
                bgexecutor.err = parent.err;
                return bgexecutor.executePipeline(pipeline);
            },
            cmdtext
        );
        this.bgpid = (int) (JobManager.JOB_PID_BASE + jobnum);
    }

    private int executePipeline(final Pipeline pipeline) {
        final List<Node> commands = pipeline.getCommands();
        final int code;
        if (commands.isEmpty() || commands.get(0) == null) {
            code = 0;
        } else {
            final int exit;
            if (commands.size() == 1) {
                exit = this.executeNode(commands.get(0));
            } else {
                exit = this.executePipelineInternal(pipeline);
            }
            if (pipeline.isBang()) {
                if (exit == 0) {
                    code = 1;
                } else {
                    code = 0;
                }
            } else {
                code = exit;
            }
        }
        return code;
    }

    private int executeNode(final Node node) {
        int code;
        if (node instanceof SimpleCommand cmd) {
            this.refreshAliases(cmd);
            this.environment.put("LINENO", String.valueOf(cmd.getLineNumber()));
            if (this.flags.indexOf('x') >= 0) {
                this.err.println(String.format("+ %s", cmd));
            }
            code = this.executeCommand(cmd);
        } else if (node instanceof Subshell sub) {
            final Executor child = this.subshell();
            try {
                code = child.execute(sub.getList());
            } catch (final ExitSignal ex) {
                code = ex.getCode();
            }
        } else if (node instanceof Grouping group) {
            code = this.execute(group.getList());
        } else if (node instanceof IfNode ifNode) {
            code = this.runIf(ifNode);
        } else if (node instanceof ForNode forNode) {
            code = this.runFor(forNode);
        } else if (node instanceof WhileNode whileNode) {
            code = this.runWhile(whileNode);
        } else if (node instanceof CaseNode caseNode) {
            code = this.runCase(caseNode);
        } else if (node instanceof FunctionNode fnNode) {
            this.functions.put(fnNode.getName(), fnNode);
            code = 0;
        } else {
            code = 0;
        }
        return code;
    }

    private int runIf(final IfNode node) {
        final int condexit = this.execute(node.getCondition());
        final int code;
        if (condexit == 0) {
            code = this.execute(node.getThenBody());
        } else if (node.getElseBody() == null) {
            code = 0;
        } else {
            code = this.execute(node.getElseBody());
        }
        return code;
    }

    private int runFor(final ForNode node) {
        this.loops = this.loops + 1;
        int lastcode2 = 0;
        boolean broken = false;
        final List<String> words;
        if (node.getWords() == null) {
            if (this.params.size() <= 1) {
                words = new ArrayList<>();
            } else {
                words = new ArrayList<>(
                    this.params.subList(1, this.params.size())
                );
            }
        } else {
            words = new ArrayList<>();
            for (final String raw : node.getWords()) {
                words.addAll(this.expandFields(raw, this.environment));
            }
        }
        for (final String word : words) {
            if (broken) {
                break;
            }
            this.environment.put(node.getVariable(), word);
            try {
                lastcode2 = this.execute(node.getBody());
            } catch (final BreakSignal ex) {
                if (ex.getLevels() > 1) {
                    throw new BreakSignal(ex.getLevels() - 1);
                }
                broken = true;
            } catch (final ContinueSignal ex) {
                if (ex.getLevels() > 1) {
                    throw new ContinueSignal(ex.getLevels() - 1);
                }
                lastcode2 = 0;
            }
        }
        this.loops = this.loops - 1;
        return lastcode2;
    }

    private int runWhile(final WhileNode node) {
        this.loops = this.loops + 1;
        int lastcode2 = 0;
        boolean running = true;
        while (running) {
            final int condexit = this.execute(node.getCondition());
            final boolean dobody;
            if (node.isUntil()) {
                dobody = condexit != 0;
            } else {
                dobody = condexit == 0;
            }
            if (!dobody) {
                running = false;
            } else {
                try {
                    lastcode2 = this.execute(node.getBody());
                } catch (final BreakSignal ex) {
                    if (ex.getLevels() > 1) {
                        throw new BreakSignal(ex.getLevels() - 1);
                    }
                    running = false;
                } catch (final ContinueSignal ex) {
                    if (ex.getLevels() > 1) {
                        throw new ContinueSignal(ex.getLevels() - 1);
                    }
                    lastcode2 = 0;
                }
            }
        }
        this.loops = this.loops - 1;
        return lastcode2;
    }

    private int runCase(final CaseNode node) {
        final String word = WordExpander.removeQuotes(
            this.expandVariables(node.getWord(), this.environment)
        );
        int code = 0;
        boolean matched = false;
        for (final CaseItem item : node.getItems()) {
            if (matched) {
                break;
            }
            for (final String pattern : item.getPatterns()) {
                final String expanded = this.expandVariables(pattern, this.environment);
                final String cleaned = WordExpander.removeQuotes(expanded);
                final boolean quoted = pattern.indexOf('\'') >= 0 || pattern.indexOf('"') >= 0;
                final boolean hits;
                if (quoted) {
                    hits = cleaned.equals(word);
                } else {
                    hits = WordExpander.globMatches(cleaned, word);
                }
                if (hits) {
                    code = this.execute(item.getBody());
                    matched = true;
                    break;
                }
            }
        }
        return code;
    }

    private int executePipelineInternal(final Pipeline pipeline) {
        final List<Node> commands = pipeline.getCommands();
        final int count = commands.size();
        final int[] codes = new int[count];
        final PipedInputStream[] inputs = new PipedInputStream[count];
        final PipedOutputStream[] outputs = new PipedOutputStream[count];
        try {
            for (int idx = 0; idx < count - 1; idx = idx + 1) {
                outputs[idx] = new PipedOutputStream();
                inputs[idx + 1] = new PipedInputStream(outputs[idx], 65536);
            }
        } catch (final IOException ex) {
            return 1;
        }
        final Thread[] threads = new Thread[count];
        for (int idx = 0; idx < count; idx = idx + 1) {
            final int index = idx;
            final boolean last = index == count - 1;
            final Executor child = this.subshell();
            if (index == 0) {
                child.instream = this.instream;
            } else {
                child.instream = inputs[index];
            }
            if (last) {
                child.out = this.out;
            } else {
                child.out = new PrintSink(outputs[index], StandardCharsets.UTF_8);
            }
            child.err = this.err;
            final Node node = commands.get(index);
            threads[index] = new Thread(
                () -> {
                    try {
                        codes[index] = child.executeNode(node);
                    } catch (final ExitSignal ex) {
                        codes[index] = ex.getCode();
                    } catch (final RuntimeException ex) {
                        codes[index] = 1;
                    } finally {
                        if (!last) {
                            try {
                                outputs[index].close();
                            } catch (final IOException ignored) {
                                codes[index] = 1;
                            }
                        }
                    }
                },
                String.format("sh-pipe-%d", index)
            );
        }
        for (final Thread thread : threads) {
            thread.start();
        }
        for (final Thread thread : threads) {
            try {
                thread.join();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                return 1;
            }
        }
        return codes[count - 1];
    }

    private int executeCommand(final SimpleCommand cmd) {
        final Map<String, String> cenv = new HashMap<>(this.environment);
        this.substat = -1;
        final int assign = this.processAssignments(cmd.getAssignments(), cenv);
        final String name = cmd.getName();
        final int code;
        if (name == null || name.isEmpty()) {
            this.environment.putAll(cenv);
            if (this.substat >= 0) {
                code = this.substat;
            } else {
                code = assign;
            }
        } else {
            code = this.runNamedCommand(cmd, name, cenv);
        }
        return code;
    }

    private int runNamedCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cenv
    ) {
        return this.runNamedCommand(cmd, name, cenv, false);
    }

    private Map<String, String> aliasTable() {
        final Map<String, String> table;
        if (this.snapshot == null) {
            table = this.aliases;
        } else {
            table = this.snapshot;
        }
        return table;
    }

    private void refreshAliases(final SimpleCommand cmd) {
        final int line = cmd.getLineNumber();
        if (this.snapshot == null || line != this.snapline) {
            this.snapshot = new HashMap<>(this.aliases);
            this.snapline = line;
        }
    }

    private int runNamedCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cenv,
        final boolean skipfunc
    ) {
        final String alias;
        if (skipfunc) {
            alias = null;
        } else {
            alias = this.aliasTable().get(name);
        }
        if (alias != null) {
            final StringBuilder line = new StringBuilder(alias);
            for (final String arg : cmd.getArguments()) {
                line.append(' ').append(arg);
            }
            return this.execute(
                new Parser(new Lexer(new CharReader(line.toString()))).parse()
            );
        }
        if ("exec".equals(name)) {
            return this.runExec(cmd, cenv);
        }
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            cmd.getRedirections(),
            new RedirectionHandler.Streams(this.out, this.err),
            new RedirectionHandler.RedirectContext(
                text -> this.expandVariables(text, this.environment),
                this.cwd, this.outdescs
            )
        );
        this.out = swap.out();
        this.err = swap.err();
        final InputStream keptin = this.swapInputForBuiltin(cmd.getRedirections());
        final int code;
        try {
            if (swap.failed()) {
                code = 1;
            } else if (!skipfunc && this.functions.containsKey(name)
                && !isSpecialBuiltin(name)) {
                this.environment.putAll(cenv);
                code = this.invokeFunction(this.functions.get(name), cmd, cenv);
            } else {
                if (isSpecialBuiltin(name)) {
                    this.applyAssignments(cmd.getAssignments(), cenv);
                }
                final Integer bresult = this.dispatcher.dispatch(
                    name, this, cmd.getArguments(), cenv, cmd.getAssignments()
                );
                if (bresult != null) {
                    code = bresult;
                } else {
                    code = this.runExternalCommand(cmd, name, cenv);
                }
            }
        } finally {
            if (keptin != null) {
                if (!this.borrowed) {
                    try {
                        this.instream.close();
                    } catch (final IOException ignored) {
                        // The borrowed stream is closed on a best-effort basis.
                    }
                }
                this.borrowed = false;
                this.instream = keptin;
            }
            if (swap.savedout() != null) {
                this.out = swap.savedout();
            }
            if (swap.savederr() != null) {
                this.err = swap.savederr();
            }
        }
        return code;
    }

    private File redirectionFile(final Redirection redir) {
        final File raw = new File(this.expandVariables(redir.getTarget(), this.environment));
        final File result;
        if (raw.isAbsolute()) {
            result = raw;
        } else {
            result = new File(this.cwd, raw.getPath());
        }
        return result;
    }

    private int runExec(final SimpleCommand cmd, final Map<String, String> cenv) {
        final int code;
        if (cmd.getArguments().isEmpty()) {
            final RedirectionHandler.StreamSwap swap =
                RedirectionHandler.redirectOutputForBuiltin(
                    cmd.getRedirections(),
                    new RedirectionHandler.Streams(this.out, this.err),
                    new RedirectionHandler.RedirectContext(
                        text -> this.expandVariables(text, this.environment),
                        this.cwd, this.outdescs
                    )
                );
            this.out = swap.out();
            this.err = swap.err();
            final InputStream saved = this.swapInputForBuiltin(cmd.getRedirections());
            if (saved != null && saved != System.in) {
                try {
                    saved.close();
                } catch (final IOException ignored) {
                    // The temporary input stream is closed on a best-effort basis.
                }
            }
            boolean bound = !swap.failed();
            for (final Redirection redir : cmd.getRedirections()) {
                final RedirectionType type = redir.getType();
                final int desc = redir.getFd();
                if (desc > 0 && (type == RedirectionType.READWRITE
                    || type == RedirectionType.INPUT)) {
                    final File target = this.redirectionFile(redir);
                    try {
                        if (type == RedirectionType.READWRITE) {
                            final OutputStream appended = Files.newOutputStream(
                                target.toPath(), StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND
                            );
                            final PrintSink writer = new PrintSink(appended, StandardCharsets.UTF_8);
                            this.outdescs.put(Integer.valueOf(desc), writer);
                        }
                        final InputStream source = Files.newInputStream(
                            target.toPath()
                        );
                        this.descriptors.put(Integer.valueOf(desc), source);
                    } catch (final IOException ex) {
                        bound = false;
                    }
                } else if (desc > 0 && (type == RedirectionType.OUTPUT
                    || type == RedirectionType.CLOBBER || type == RedirectionType.APPEND)) {
                    final File target = this.redirectionFile(redir);
                    try {
                        final OutputStream appended = type == RedirectionType.APPEND
                            ? Files.newOutputStream(
                                target.toPath(), StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND
                            )
                            : Files.newOutputStream(target.toPath());
                        this.outdescs.put(
                            Integer.valueOf(desc),
                            new PrintSink(appended, StandardCharsets.UTF_8)
                        );
                    } catch (final IOException ex) {
                        bound = false;
                    }
                } else if (desc > 0 && type == RedirectionType.DUP_INPUT
                    && "-".equals(redir.getTarget())) {
                    final InputStream closed = this.descriptors.remove(desc);
                    if (closed != null) {
                        try {
                            closed.close();
                        } catch (final IOException ex) {
                            bound = false;
                        }
                    }
                } else if (desc > 0 && type == RedirectionType.DUP_OUTPUT
                    && "-".equals(redir.getTarget())) {
                        this.outdescs.remove(Integer.valueOf(desc));
                }
            }
            if (!bound) {
                throw new ExitSignal(1);
            }
            code = 0;
        } else {
            final List<String> args = cmd.getArguments();
            final String name = this.expandVariables(args.get(0), cenv);
            final SimpleCommand external = new SimpleCommand(name);
            for (int idx = 1; idx < args.size(); idx = idx + 1) {
                external.addArgument(args.get(idx));
            }
            for (final Redirection redir : cmd.getRedirections()) {
                external.addRedirection(redir);
            }
            code = this.runExternalCommand(external, name, cenv);
            throw new ExitSignal(code);
        }
        return code;
    }

    private int invokeFunction(
        final FunctionNode func, final SimpleCommand cmd, final Map<String, String> cenv
    ) {
        final List<String> keptargs = new ArrayList<>(this.params);
        final List<String> fargs = new ArrayList<>();
        for (final String arg : cmd.getArguments()) {
            fargs.addAll(this.expandArguments(arg, cenv));
        }
        this.setPositionalParams(fargs);
        try {
            return this.executeNode(func.getBody());
        } catch (final ReturnSignal ex) {
            return ex.getCode();
        } finally {
            this.params.clear();
            this.params.addAll(keptargs);
        }
    }

    private InputStream swapInputForBuiltin(final List<Redirection> redirs) {
        InputStream replacement = null;
        for (final Redirection redir : redirs) {
            final RedirectionType type = redir.getType();
            if (type == RedirectionType.HERE_DOC || type == RedirectionType.HERE_DOC_DASH) {
                final String content = redir.getHereDocContent();
                if (content != null) {
                    final String body;
                    if (redir.isHereDocQuoted()) {
                        body = content;
                    } else {
                        body = this.expandVariables(content, this.environment);
                    }
                    replacement = new ByteArrayInputStream(
                        body.getBytes(StandardCharsets.UTF_8)
                    );
                }
            } else if (type == RedirectionType.DUP_INPUT) {
                final InputStream bound = this.descriptors.get(
                    parseDescriptor(this.expandVariables(redir.getTarget(), this.environment))
                );
                if (bound != null) {
                    replacement = bound;
                    this.borrowed = true;
                }
            } else if (type == RedirectionType.INPUT && redir.getFd() == 0) {
                final File target = this.redirectionFile(redir);
                try {
                    replacement = Files.newInputStream(target.toPath());
                } catch (final IOException ex) {
                    replacement = null;
                }
            }
        }
        if (replacement == null) {
            return null;
        }
        return this.installInput(replacement);
    }

    private InputStream installInput(final InputStream replacement) {
        final InputStream previous = this.instream;
        if (previous != replacement) {
            this.instream = replacement;
        }
        return previous;
    }

    private int runExternalCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cenv
    ) {
        final String resolved = this.resolveCommand(name, cenv);
        int code;
        if (resolved == null) {
            code = EXIT_COMMAND_NOT_FOUND;
        } else {
            final ProcessBuilder builder = this.createProcessBuilder(resolved, cmd, cenv);
            RedirectionHandler.setupRedirections(
                builder, cmd.getRedirections(),
                new RedirectionHandler.RedirectContext(
                    text -> this.expandVariables(text, this.environment),
                    this.cwd, Collections.emptyMap()
                )
            );
            code = this.startAndAwaitOne(builder);
        }
        return code;
    }

    private int startAndAwaitOne(final ProcessBuilder builder) {
        if (Thread.currentThread().isInterrupted()) {
            return 1;
        }
        final ProcessBuilder.Redirect output = builder.redirectOutput();
        final ProcessBuilder.Redirect error = builder.redirectError();
        final ProcessBuilder.Redirect input = builder.redirectInput();
        if (ProcessBuilder.Redirect.PIPE.equals(input) && this.instream == System.in) {
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        }
        int code;
        try {
            final Process process = builder.start();
            Thread writer = null;
            Thread reader = null;
            if (ProcessBuilder.Redirect.PIPE.equals(error)
                && process.getErrorStream() != null) {
                writer = new Thread(
                    () -> {
                        try (InputStream stream = process.getErrorStream()) {
                            stream.transferTo(this.err.bytes());
                        } catch (final IOException ignored) {
                            this.err.flush();
                        }
                    },
                    "sh-stderr"
                );
                writer.start();
            }
            if (ProcessBuilder.Redirect.PIPE.equals(input)
                && this.instream != System.in) {
                reader = new Thread(
                    () -> {
                        try (
                            OutputStream stream = process.getOutputStream()
                        ) {
                            this.instream.transferTo(stream);
                        } catch (final IOException ignored) {
                            this.err.flush();
                        }
                    },
                    "sh-stdin"
                );
                reader.start();
            }
            if (ProcessBuilder.Redirect.PIPE.equals(output)) {
                try (InputStream stream = process.getInputStream()) {
                    stream.transferTo(this.out.bytes());
                }
            }
            code = process.waitFor();
            if (writer != null) {
                writer.join();
            }
            if (reader != null) {
                reader.join();
            }
            this.out.flush();
            this.err.flush();
        } catch (final IOException ex) {
            code = this.exitCodeForIoException(ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            code = 1;
        }
        return code;
    }

    private void applyAssignments(
        final List<String> assignments, final Map<String, String> source
    ) {
        for (final String assignment : assignments) {
            final int equals = assignment.indexOf('=');
            if (equals > 0) {
                final String name = assignment.substring(0, equals);
                final String value = source.get(name);
                if (value != null) {
                    this.environment.put(name, value);
                }
            }
        }
    }

    private int processAssignments(
        final List<String> assignments, final Map<String, String> tenv
    ) {
        for (final String assignment : assignments) {
            final int eqpos = assignment.indexOf('=');
            if (eqpos > 0) {
                final String vname = assignment.substring(0, eqpos);
                if (this.readonly.contains(vname)) {
                    this.err.println(
                        String.format("sh: %s: readonly variable", vname)
                    );
                    throw new ExitSignal(1);
                }
                final String rvalue = assignment.substring(eqpos + 1);
                final String expanded = this.expandVariables(rvalue, tenv);
                tenv.put(vname, WordExpander.removeQuotes(expanded));
            }
        }
        return 0;
    }

    private String positionalParamsJoined() {
        final String joined;
        if (this.params.size() <= 1) {
            joined = "";
        } else {
            String separator = " ";
            final String ifs = this.environment.get("IFS");
            if (ifs != null) {
                if (ifs.isEmpty()) {
                    separator = "";
                } else {
                    separator = ifs.substring(0, 1);
                }
            }
            joined = String.join(
                separator,
                this.params.subList(1, this.params.size())
            );
        }
        return joined;
    }

    private String positionalParamForDigit(final char chr) {
        String result = null;
        if (chr >= '0' && chr <= '9') {
            final int idx = chr - '0';
            if (idx < this.params.size()) {
                result = this.params.get(idx);
            } else {
                result = "";
            }
        }
        return result;
    }

    private int exitCodeForIoException(final IOException exc) {
        final String msg = exc.getMessage();
        final int code;
        if (msg != null && (msg.contains("error=2") || msg.contains("No such file"))) {
            code = EXIT_COMMAND_NOT_FOUND;
        } else {
            code = 126;
        }
        return code;
    }

    private String resolveCommand(final String name, final Map<String, String> env) {
        String result;
        if (name.contains("/")) {
            if (new File(name).isFile()) {
                result = name;
            } else {
                result = null;
            }
        } else {
            final String path = env.get("PATH");
            if (path == null || path.isEmpty()) {
                result = null;
            } else {
                result = this.searchPath(path, name);
            }
        }
        return result;
    }

    private String searchPath(final String path, final String name) {
        String found = null;
        final String[] dirs = path.split(":", -1);
        for (final String dir : dirs) {
            if (found != null) {
                break;
            }
            final File file;
            if (dir.isEmpty()) {
                file = new File(name);
            } else {
                file = new File(dir, name);
            }
            if (file.isFile()) {
                found = file.getAbsolutePath();
            }
        }
        return found;
    }

    private ProcessBuilder createProcessBuilder(
        final String resolved,
        final SimpleCommand cmd,
        final Map<String, String> env
    ) {
        final List<String> command = new ArrayList<>();
        command.add(resolved);
        final boolean globbing = this.flags.indexOf('f') < 0;
        for (final String arg : cmd.getArguments()) {
            List<String> fields = this.expandFields(arg, env);
            if (!WordExpander.isQuoted(arg) && globbing) {
                fields = WordExpander.pathnameExpand(fields, this.cwd);
            }
            command.addAll(fields);
        }
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(this.childEnvironment(cmd, env));
        builder.directory(this.cwd);
        return builder;
    }

    private Map<String, String> childEnvironment(
        final SimpleCommand cmd, final Map<String, String> cenv
    ) {
        final Map<String, String> child = this.exportedEnvironment();
        for (final String assignment : cmd.getAssignments()) {
            final int equals = assignment.indexOf('=');
            if (equals > 0) {
                final String name = assignment.substring(0, equals);
                final String value = cenv.get(name);
                if (value != null) {
                    child.put(name, value);
                }
            }
        }
        return child;
    }

    private static boolean shouldRunNext(
        final ListNode.Separator psep, final int pexit
    ) {
        final boolean run;
        if (psep == ListNode.Separator.AND_IF) {
            run = pexit == 0;
        } else if (psep == ListNode.Separator.OR_IF) {
            run = pexit != 0;
        } else {
            run = true;
        }
        return run;
    }

    private static String pipelineToString(final Pipeline pipeline) {
        final StringBuilder line = new StringBuilder();
        for (final Node node : pipeline.getCommands()) {
            if (node == null) {
                continue;
            }
            if (line.length() > 0) {
                line.append(" | ");
            }
            line.append(node.toString());
        }
        return line.toString();
    }

    private static boolean isSpecialBuiltin(final String name) {
        return SPECIAL_BUILTINS.contains(name);
    }

    private static int parseDescriptor(final String text) {
        int result = -1;
        try {
            result = Integer.parseInt(text.trim());
        } catch (final NumberFormatException ex) {
            result = -1;
        }
        return result;
    }

    private static PrintSink console(
        final OutputStream destination
    ) {
        return new PrintSink(destination, StandardCharsets.UTF_8);
    }

    private static PrintSink printer(
        final OutputStream sink, final boolean flush
    ) {
        return new PrintSink(sink, StandardCharsets.UTF_8);
    }

    private String namedValue(final String name, final Map<String, String> env) {
        final String value = env.get(name);
        if (value == null && this.flags.indexOf('u') >= 0) {
            this.err.println(
                String.format("sh: %s: parameter not set", name)
            );
            throw new ExitSignal(1);
        }
        return value;
    }

    private String positionalAt(final int idx) {
        final String value;
        if (idx < this.params.size()) {
            value = this.params.get(idx);
        } else {
            value = null;
        }
        return value;
    }

    private String lookupNamed(final String name, final Map<String, String> env) {
        final String value;
        if (name.chars().allMatch(Character::isDigit)) {
            value = this.positionalAt(Integer.parseInt(name));
        } else {
            value = this.namedValue(name, env);
        }
        return value;
    }

    private String soleParam(final String name, final Map<String, String> env) {
        final String special = this.getSpecialParam(name.charAt(0));
        final String value;
        if (special == null) {
            value = this.lookupNamed(name, env);
        } else {
            value = special;
        }
        return value;
    }
}
