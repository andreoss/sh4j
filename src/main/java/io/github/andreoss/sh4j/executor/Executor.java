/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
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

/**
 * Tree-walking executor for the AST produced by {@link Parser}. Top-level
 * dispatch lives here; subsystems delegate to {@link WordExpander},
 * {@link ArithmeticEvaluator}, {@link RedirectionHandler}, and
 * {@link BuiltinDispatcher}.
 * @since 1.0
 */
public class Executor implements BuiltinContext {

    /**
     * POSIX 2.14 special built-in utilities — these take priority over user-
     * defined functions in command search (POSIX 2.9.1.1).
     */
    private static final java.util.Set<String> SPECIAL_BUILTINS = java.util.Set.of(
        ":", ".", "break", "continue", "eval", "exec", "exit", "export",
        "readonly", "return", "set", "shift", "times", "trap", "unset"
    );

    /** Default IFS value (space, tab, newline). */
    private static final String DEFAULT_IFS = " \t\n";
    /** Default fallback PATH when none is set. */
    private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

    /** Exit code for a command that could not be found on PATH. */
    static final int EXIT_COMMAND_NOT_FOUND = 127;
    /** Exit code for a command that exists but cannot be executed. */
    private static final int EXIT_COMMAND_NOT_EXECUTABLE = 126;

    /** Live shell-variable table. */
    private final Map<String, String> environment;
    /** Names of variables visible to child processes. */
    private final Set<String> exported;
    /** Names of read-only variables. */
    private final Set<String> readonly;
    /** Trap actions keyed by signal name (EXIT is the only executed one). */
    private final Map<String, String> traps;
    /** Per-executor current working directory. */
    private File cwd;
    /** Defined shell functions keyed by name. */
    private final Map<String, FunctionNode> functions;
    /** {@code $0}..{@code $N} positional parameters (index 0 = script name). */
    private final List<String> positionalParams;
    /** Background-job threads spawned by {@code &}. */
    private final JobManager jobManager;
    /** Dispatcher for shell built-ins. */
    private final BuiltinDispatcher dispatcher;
    /** Arithmetic-expansion evaluator. */
    private final ArithmeticEvaluator arith;

    /** Most recent foreground exit code ({@code $?}). */
    private int lastExitCode;
    /** Current shell-flag string ({@code $-}). */
    private String shellFlags;
    /** Most recent background-job PID ({@code $!}). */
    private int lastBgPid;
    /** When set, stdout of the next external command pipes into this buffer. */
    private ByteArrayOutputStream captureStream;
    /** Live stdout used by built-ins. */
    private PrintStream out;
    /** Live stderr used by built-ins. */
    private PrintStream err;
    /** Live stdin used by built-ins (read, etc.). */
    private InputStream in;
    /** Alias table (alias name → replacement text). */
    private Map<String, String> aliases;
    /** Command history for fc built-in. */
    private final CommandHistory history;
    /** Nesting depth of {@link #execute(ListNode)} calls. */
    private int executeDepth;
    /** Alias table captured when the current source line started. */
    private Map<String, String> aliasSnapshot;
    /** Source line the alias snapshot belongs to. */
    private int aliasSnapshotLine;

    /** Exit status of the last command substitution (-1 when none ran). */
    private int substitutionStatus = -1;

    /** Input streams bound to descriptors by a redirection-only {@code exec}. */
    private final Map<Integer, InputStream> descriptors = new HashMap<>();

    /** Output streams bound to descriptors by a redirection-only exec. */
    private final Map<Integer, PrintStream> outDescriptors = new HashMap<>();


    /** True when the current input swap borrowed a descriptor stream. */
    private boolean inputBorrowed;

    /** Nesting depth of active loop bodies. */
    private int loopDepth;

    /**
     * Builds an executor seeded with the current process environment.
     */
    public Executor() {
        this(System.getenv(), Arrays.asList("sh"));
    }

    /**
     * Builds an executor with the given environment view.
     * @param environment Environment variables (copied, not aliased)
     */
    public Executor(final Map<String, String> environment) {
        this(environment, Arrays.asList("sh"));
    }

    /**
     * Builds an executor with the given environment and positional params.
     * @param environment       Environment variables (copied)
     * @param positionalParams  Positional parameters ({@code $0} first)
     */
    public Executor(
        final Map<String, String> environment,
        final List<String> positionalParams
    ) {
        super();
        this.environment = new HashMap<>(environment);
        this.exported = new HashSet<>(environment.keySet());
        this.readonly = new HashSet<>();
        this.traps = new HashMap<>();
        this.cwd = new File(System.getProperty("user.dir", "/")).getAbsoluteFile();
        this.functions = new HashMap<>();
        this.positionalParams = new ArrayList<>(positionalParams);
        this.jobManager = new JobManager();
        this.dispatcher = new BuiltinDispatcher();
        this.arith = new ArithmeticEvaluator();
        this.lastExitCode = 0;
        this.shellFlags = "";
        this.lastBgPid = 0;
        this.out = System.out;
        this.err = System.err;
        this.in = System.in;
        this.aliases = new HashMap<>();
        this.history = new CommandHistory();
        this.setDefaults();
    }

    private void setDefaults() {
        this.environment.putIfAbsent("IFS", DEFAULT_IFS);
        this.environment.putIfAbsent("HOME", System.getProperty("user.home", "/root"));
        this.environment.putIfAbsent("PATH", System.getenv("PATH"));
        final String path = this.environment.get("PATH");
        if (path == null || path.isEmpty()) {
            this.environment.put("PATH", DEFAULT_PATH);
        }
        this.exported.add("HOME");
        this.exported.add("PATH");
        this.environment.putIfAbsent("PWD", this.cwd.getAbsolutePath());
        this.exported.add("PWD");
        this.environment.putIfAbsent(
            "PPID",
            Long.toString(
                ProcessHandle.current().parent().orElse(ProcessHandle.current()).pid()
            )
        );
    }

    /**
     * Creates a subshell with the same variables, functions, aliases,
     * positional parameters, flags, traps, and working directory.
     * @return A new child executor
     */
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
        child.positionalParams.clear();
        child.positionalParams.addAll(this.positionalParams);
        child.shellFlags = this.shellFlags;
        child.cwd = this.cwd;
        child.out = this.out;
        child.err = this.err;
        child.in = this.in;
        child.lastExitCode = this.lastExitCode;
        return child;
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
            int read = this.in.read();
            while (read != -1 && read != '\n') {
                buffer.write(read);
                read = this.in.read();
            }
            if (read != -1 || buffer.size() > 0) {
                line = buffer.toString(StandardCharsets.UTF_8.name());
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

    /**
     * Runs and removes the EXIT trap using the last exit status.
     */
    public void runExitTrap() {
        this.runExitTrap(this.lastExitCode);
    }

    /**
     * Runs and removes the EXIT trap with the status the shell exits with.
     * @param status The shell exit status the trap observes as {@code $?}
     */
    public void runExitTrap(final int status) {
        this.lastExitCode = status;
        final String action = this.traps.remove("EXIT");
        if (action != null && !action.isEmpty() && !"-".equals(action)) {
            try {
                final ListNode ast = new Parser(
                    new Lexer(new CharReader(action))
                ).parse();
                this.execute(ast);
            } catch (final RuntimeException ex) {
                this.err.println("sh: trap: " + ex.getMessage());
            }
        }
    }

    /**
     * @param name A variable name
     * @return The variable's current value, or null when unset
     */
    public String getVariable(final String name) {
        return this.environment.get(name);
    }

    /**
     * Redirects subsequent stdout into the given buffer (used by command
     * substitution).
     * @param stream Destination for captured stdout
     */
    public void setCaptureStream(final ByteArrayOutputStream stream) {
        this.captureStream = stream;
        this.out = new PrintStream(stream, true, StandardCharsets.UTF_8);
    }

    /**
     * Overrides the executor's stdout.
     * @param outStream The new live stdout
     */
    public void setOutputStream(final PrintStream outStream) {
        this.out = outStream;
    }

    /**
     * Overrides the executor's stderr.
     * @param errStream The new live stderr
     */
    public void setErrorStream(final PrintStream errStream) {
        this.err = errStream;
    }

    /**
     * Overrides the executor's stdin.
     * @param inputStream The new live stdin
     */
    public void setInputStream(final InputStream inputStream) {
        this.in = inputStream;
    }

    @Override
    public String executeCommandSubstitution(final String text) {
        final CharReader reader = new CharReader(text);
        final Lexer lexer = new Lexer(reader);
        final Parser parser = new Parser(lexer);
        final ListNode ast = parser.parse();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Executor sub = this.subshell();
        sub.setCaptureStream(baos);
        this.substitutionStatus = sub.execute(ast);
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
        if (this.executeDepth == 0) {
            this.aliasSnapshot = null;
            this.aliasSnapshotLine = -1;
        }
        this.executeDepth = this.executeDepth + 1;
        int code;
        try {
            code = 0;
            if (!list.getPipelines().isEmpty()) {
                code = this.runPipelineList(list);
            }
        } catch (final ExitSignal ex) {
            if (this.executeDepth > 1) {
                throw ex;
            }
            code = ex.getCode();
        } catch (final ReturnSignal ex) {
            if (this.executeDepth > 1) {
                throw ex;
            }
            this.err.println("sh: return: not in a function");
            code = ex.getCode();
        } catch (final BreakSignal ex) {
            if (this.executeDepth > 1) {
                throw ex;
            }
            code = 0;
        } catch (final ContinueSignal ex) {
            if (this.executeDepth > 1) {
                throw ex;
            }
            code = 0;
        } finally {
            this.executeDepth = this.executeDepth - 1;
        }
        return code;
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
                separators.get(idx - 1), this.lastExitCode
            );
            if (run) {
                if (sep == ListNode.Separator.AMP) {
                    this.executeAsync(pipeline);
                    this.lastExitCode = 0;
                } else {
                    final int exitCode;
                    try {
                        exitCode = this.executePipeline(pipeline);
                    } catch (final BreakSignal ex) {
                        if (this.loopDepth > 0) {
                            throw ex;
                        }
                        this.err.println("sh: break: not in a loop");
                        this.lastExitCode = 0;
                        idx = idx + 1;
                        continue;
                    } catch (final ContinueSignal ex) {
                        if (this.loopDepth > 0) {
                            throw ex;
                        }
                        this.err.println("sh: continue: not in a loop");
                        this.lastExitCode = 0;
                        idx = idx + 1;
                        continue;
                    }
                    this.lastExitCode = exitCode;
                    this.history.add(pipelineToString(pipeline));
                    if (this.shellFlags.indexOf('e') >= 0 && exitCode != 0
                        && sep != ListNode.Separator.AND_IF
                        && sep != ListNode.Separator.OR_IF) {
                        stopped = true;
                    }
                }
            }
            idx = idx + 1;
        }
        return this.lastExitCode;
    }

    private static boolean shouldRunNext(
        final ListNode.Separator prevSep, final int prevExit
    ) {
        final boolean run;
        if (prevSep == ListNode.Separator.AND_IF) {
            run = prevExit == 0;
        } else if (prevSep == ListNode.Separator.OR_IF) {
            run = prevExit != 0;
        } else {
            run = true;
        }
        return run;
    }

    private void executeAsync(final Pipeline pipeline) {
        final String cmdStr = pipelineToString(pipeline);
        final Executor parent = this;
        final int jobId = this.jobManager.submit(() -> {
            final Executor bgExecutor = parent.subshell();
            bgExecutor.out = parent.out;
            bgExecutor.err = parent.err;
            return bgExecutor.executePipeline(pipeline);
        }, cmdStr);
        this.lastBgPid = (int) this.jobManager.pidForId(jobId);
    }

    @Override
    public int lastBackgroundPid() {
        return this.lastBgPid;
    }

    @Override
    public void waitForBackgroundJobs() {
        this.jobManager.waitForAll();
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
                code = exit == 0 ? 1 : 0;
            } else {
                code = exit;
            }
        }
        return code;
    }

    private int executeNode(final Node node) {
        int code;
        if (node instanceof SimpleCommand) {
            final SimpleCommand cmd = (SimpleCommand) node;
            this.refreshAliases(cmd);
            this.environment.put("LINENO", String.valueOf(cmd.getLineNumber()));
            if (this.shellFlags.indexOf('x') >= 0) {
                this.err.println("+ " + cmd);
            }
            code = this.executeCommand(cmd);
        } else if (node instanceof Subshell) {
            final Subshell sub = (Subshell) node;
            final Executor child = this.subshell();
            try {
                code = child.execute(sub.getList());
            } catch (final ExitSignal ex) {
                code = ex.getCode();
            }
        } else if (node instanceof Grouping) {
            final Grouping group = (Grouping) node;
            code = this.execute(group.getList());
        } else if (node instanceof IfNode) {
            final IfNode ifNode = (IfNode) node;
            code = this.runIf(ifNode);
        } else if (node instanceof ForNode) {
            final ForNode forNode = (ForNode) node;
            code = this.runFor(forNode);
        } else if (node instanceof WhileNode) {
            final WhileNode whileNode = (WhileNode) node;
            code = this.runWhile(whileNode);
        } else if (node instanceof CaseNode) {
            final CaseNode caseNode = (CaseNode) node;
            code = this.runCase(caseNode);
        } else if (node instanceof FunctionNode) {
            final FunctionNode fnNode = (FunctionNode) node;
            this.functions.put(fnNode.getName(), fnNode);
            code = 0;
        } else {
            code = 0;
        }
        return code;
    }

    private int runIf(final IfNode node) {
        final int condExit = this.execute(node.getCondition());
        final int code;
        if (condExit == 0) {
            code = this.execute(node.getThenBody());
        } else if (node.getElseBody() == null) {
            code = 0;
        } else {
            code = this.execute(node.getElseBody());
        }
        return code;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private int runFor(final ForNode node) {
        this.loopDepth = this.loopDepth + 1;
        int lastExit = 0;
        boolean broken = false;
        final List<String> words;
        if (node.getWords() == null) {
            words = this.positionalParams.size() <= 1
                ? new ArrayList<>()
                : new ArrayList<>(
                    this.positionalParams.subList(1, this.positionalParams.size())
                );
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
                lastExit = this.execute(node.getBody());
            } catch (final BreakSignal ex) {
                if (ex.getLevels() > 1) {
                    throw new BreakSignal(ex.getLevels() - 1);
                }
                broken = true;
            } catch (final ContinueSignal ex) {
                if (ex.getLevels() > 1) {
                    throw new ContinueSignal(ex.getLevels() - 1);
                }
                lastExit = 0;
            }
        }
        this.loopDepth = this.loopDepth - 1;
        return lastExit;
    }

    private int runWhile(final WhileNode node) {
        this.loopDepth = this.loopDepth + 1;
        int lastExit = 0;
        boolean running = true;
        while (running) {
            final int condExit = this.execute(node.getCondition());
            final boolean runBody = node.isUntil() ? condExit != 0 : condExit == 0;
            if (!runBody) {
                running = false;
            } else {
                try {
                    lastExit = this.execute(node.getBody());
                } catch (final BreakSignal ex) {
                    if (ex.getLevels() > 1) {
                        throw new BreakSignal(ex.getLevels() - 1);
                    }
                    running = false;
                } catch (final ContinueSignal ex) {
                    if (ex.getLevels() > 1) {
                        throw new ContinueSignal(ex.getLevels() - 1);
                    }
                    lastExit = 0;
                }
            }
        }
        this.loopDepth = this.loopDepth - 1;
        return lastExit;
    }

    private int runCase(final CaseNode node) {
        final String word = WordExpander.removeQuotes(
            this.expandVariables(node.getWord(), this.environment)
        );
        int code = 0;
        boolean matched = false;
        for (final CaseNode.CaseItem item : node.getItems()) {
            if (matched) {
                break;
            }
            for (final String pattern : item.getPatterns()) {
                final String expanded = this.expandVariables(pattern, this.environment);
                final String cleaned = WordExpander.removeQuotes(expanded);
                final boolean quoted = pattern.indexOf('\'') >= 0 || pattern.indexOf('"') >= 0;
                final boolean hits = quoted ? cleaned.equals(word)
                    : WordExpander.globMatches(cleaned, word);
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
            child.in = index == 0 ? this.in : inputs[index];
            child.out = last
                ? this.out
                : new PrintStream(outputs[index], true, StandardCharsets.UTF_8);
            child.err = this.err;
            final Node node = commands.get(index);
            threads[index] = new Thread(() -> {
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
            }, "sh-pipe-" + index);
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
        final Map<String, String> cmdEnv = new HashMap<>(this.environment);
        this.substitutionStatus = -1;
        final int assignCode = this.processAssignments(cmd.getAssignments(), cmdEnv);
        final String name = cmd.getName();
        final int code;
        if (name == null || name.isEmpty()) {
            this.environment.putAll(cmdEnv);
            if (this.substitutionStatus >= 0) {
                code = this.substitutionStatus;
            } else {
                code = assignCode;
            }
        } else {
            code = this.runNamedCommand(cmd, name, cmdEnv);
        }
        return code;
    }

    private int runNamedCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cmdEnv
    ) {
        return this.runNamedCommand(cmd, name, cmdEnv, false);
    }

    private Map<String, String> aliasTable() {
        return this.aliasSnapshot == null ? this.aliases : this.aliasSnapshot;
    }

    private void refreshAliases(final SimpleCommand cmd) {
        final int line = cmd.getLineNumber();
        if (this.aliasSnapshot == null || line != this.aliasSnapshotLine) {
            this.aliasSnapshot = new HashMap<>(this.aliases);
            this.aliasSnapshotLine = line;
        }
    }

    private int runNamedCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cmdEnv,
        final boolean bypassFunctions
    ) {
        final String alias = bypassFunctions ? null : this.aliasTable().get(name);
        if (alias != null) {
            final StringBuilder line = new StringBuilder(alias);
            for (final String arg : cmd.getArguments()) {
                line.append(' ').append(arg);
            }
            final ListNode ast = new Parser(
                new Lexer(new CharReader(line.toString()))
            ).parse();
            return this.execute(ast);
        }
        if ("exec".equals(name)) {
            return this.runExec(cmd, cmdEnv);
        }
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            cmd.getRedirections(), this.out, this.err,
            text -> this.expandVariables(text, this.environment), this.cwd, this.outDescriptors
        );
        this.out = swap.out;
        this.err = swap.err;
        final InputStream savedIn = this.swapInputForBuiltin(cmd.getRedirections());
        final int code;
        try {
            if (swap.failed) {
                code = 1;
            } else if (!bypassFunctions && this.functions.containsKey(name)
                && !isSpecialBuiltin(name)) {
                this.environment.putAll(cmdEnv);
                code = this.invokeFunction(this.functions.get(name), cmd, cmdEnv);
            } else {
                if (isSpecialBuiltin(name)) {
                    this.applyAssignments(cmd.getAssignments(), cmdEnv);
                }
                final Integer builtinResult = this.dispatcher.dispatch(
                    name, this, cmd.getArguments(), cmdEnv, cmd.getAssignments()
                );
                if (builtinResult != null) {
                    code = builtinResult;
                } else {
                    code = this.runExternalCommand(cmd, name, cmdEnv);
                }
            }
        } finally {
            if (savedIn != null) {
                if (!this.inputBorrowed) {
                    try {
                        this.in.close();
                    } catch (final IOException ignored) {
                    }
                }
                this.inputBorrowed = false;
                this.in = savedIn;
            }
            if (swap.savedOut != null) {
                if (this.out != this.err) {
                    this.out.close();
                }
                this.out = swap.savedOut;
            }
            if (swap.savedErr != null) {
                if (this.err != this.out) {
                    this.err.close();
                }
                this.err = swap.savedErr;
            }
        }
        return code;
    }

    /**
     * Implements {@code exec}: redirection-only calls persist the stream
     * changes; a command replaces the shell semantically by unwinding with
     * the child's status.
     * @param cmd The exec command node
     * @param cmdEnv Expansion environment
     * @return 0 for redirection-only exec
     */
    /**
     * Expands a redirection target into a file resolved against the shell's
     * working directory.
     * @param redir The redirection whose target is expanded
     * @return The resolved file
     */
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

    private int runExec(final SimpleCommand cmd, final Map<String, String> cmdEnv) {
        final int code;
        if (cmd.getArguments().isEmpty()) {
            final RedirectionHandler.StreamSwap swap =
                RedirectionHandler.redirectOutputForBuiltin(
                    cmd.getRedirections(), this.out, this.err,
                    text -> this.expandVariables(text, this.environment), this.cwd, this.outDescriptors
                );
            this.out = swap.out;
            this.err = swap.err;
            final InputStream saved = this.swapInputForBuiltin(cmd.getRedirections());
            if (saved != null && saved != System.in) {
                try {
                    saved.close();
                } catch (final IOException ignored) {
                }
            }
            boolean bound = !swap.failed;
            for (final Redirection redir : cmd.getRedirections()) {
                final RedirectionType type = redir.getType();
                final int desc = redir.getFd();
                if (desc > 0 && (type == RedirectionType.READWRITE
                    || type == RedirectionType.INPUT)) {
                    final File target = this.redirectionFile(redir);
                    try {
                        if (type == RedirectionType.READWRITE) {
                            this.outDescriptors.put(
                                desc,
                                new PrintStream(
                                    new FileOutputStream(target, true), false,
                                    StandardCharsets.UTF_8
                                )
                            );
                        }
                        this.descriptors.put(desc, new FileInputStream(target));
                    } catch (final IOException ex) {
                        bound = false;
                    }
                } else if (desc > 0 && (type == RedirectionType.OUTPUT
                    || type == RedirectionType.CLOBBER || type == RedirectionType.APPEND)) {
                    final File target = this.redirectionFile(redir);
                    try {
                        this.outDescriptors.put(
                            desc,
                            new PrintStream(
                                new FileOutputStream(target, type == RedirectionType.APPEND),
                                false, StandardCharsets.UTF_8
                            )
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
                    final PrintStream closed = this.outDescriptors.remove(desc);
                    if (closed != null) {
                        closed.close();
                    }
                }
            }
            if (!bound) {
                throw new ExitSignal(1);
            }
            code = 0;
        } else {
            final List<String> args = cmd.getArguments();
            final String name = this.expandVariables(args.get(0), cmdEnv);
            final SimpleCommand external = new SimpleCommand(name);
            for (int idx = 1; idx < args.size(); idx = idx + 1) {
                external.addArgument(args.get(idx));
            }
            for (final Redirection redir : cmd.getRedirections()) {
                external.addRedirection(redir);
            }
            code = this.runExternalCommand(external, name, cmdEnv);
            throw new ExitSignal(code);
        }
        return code;
    }

    private int invokeFunction(
        final FunctionNode func, final SimpleCommand cmd, final Map<String, String> cmdEnv
    ) {
        final List<String> savedParams = new ArrayList<>(this.positionalParams);
        final List<String> funcArgs = new ArrayList<>();
        for (final String arg : cmd.getArguments()) {
            funcArgs.addAll(this.expandArguments(arg, cmdEnv));
        }
        this.setPositionalParams(funcArgs);
        try {
            return this.executeNode(func.getBody());
        } catch (final ReturnSignal ex) {
            return ex.getCode();
        } finally {
            this.positionalParams.clear();
            this.positionalParams.addAll(savedParams);
        }
    }

    private static boolean isSpecialBuiltin(final String name) {
        return SPECIAL_BUILTINS.contains(name);
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
                    this.inputBorrowed = true;
                }
            } else if (type == RedirectionType.INPUT && redir.getFd() == 0) {
                final File target = this.redirectionFile(redir);
                try {
                    replacement = new FileInputStream(target);
                } catch (final java.io.FileNotFoundException ex) {
                    // fall back to no replacement; read will see original stdin
                }
            }
        }
        if (replacement == null) {
            return null;
        }
        final InputStream saved = this.in;
        this.in = replacement;
        return saved;
    }

    /**
     * Parses a redirection descriptor target such as {@code 3}.
     * @param text The expanded redirection target
     * @return The descriptor number, or -1 when it is not numeric
     */
    private static int parseDescriptor(final String text) {
        int result = -1;
        try {
            result = Integer.parseInt(text.trim());
        } catch (final NumberFormatException ex) {
            result = -1;
        }
        return result;
    }

    private int runExternalCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cmdEnv
    ) {
        final String resolved = this.resolveCommand(name, cmdEnv);
        int code;
        if (resolved == null) {
            code = EXIT_COMMAND_NOT_FOUND;
        } else {
            final ProcessBuilder builder = this.createProcessBuilder(resolved, cmd, cmdEnv);
            RedirectionHandler.setupRedirections(
                builder, cmd.getRedirections(),
                text -> this.expandVariables(text, this.environment),
                this.cwd
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
        if (input == ProcessBuilder.Redirect.PIPE && this.in == System.in) {
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        }
        int code;
        try {
            final Process process = builder.start();
            Thread errThread = null;
            Thread inThread = null;
            if (error == ProcessBuilder.Redirect.PIPE
                && process.getErrorStream() != null) {
                errThread = new Thread(() -> {
                    try (InputStream stream = process.getErrorStream()) {
                        stream.transferTo(this.err);
                    } catch (final IOException ignored) {
                        this.err.flush();
                    }
                }, "sh-stderr");
                errThread.start();
            }
            if (input == ProcessBuilder.Redirect.PIPE && this.in != System.in) {
                inThread = new Thread(() -> {
                    try (java.io.OutputStream stream = process.getOutputStream()) {
                        this.in.transferTo(stream);
                    } catch (final IOException ignored) {
                        this.err.flush();
                    }
                }, "sh-stdin");
                inThread.start();
            }
            if (output == ProcessBuilder.Redirect.PIPE) {
                try (InputStream stream = process.getInputStream()) {
                    stream.transferTo(this.out);
                }
            }
            code = process.waitFor();
            if (errThread != null) {
                errThread.join();
            }
            if (inThread != null) {
                inThread.join();
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

    /**
     * @param name A command name
     * @return True when {@code name} is a known shell built-in
     */
    private void applyAssignments(
        final List<String> assignments, final Map<String, String> source
    ) {
        for (final String assignment : assignments) {
            final int eq = assignment.indexOf('=');
            if (eq > 0) {
                final String name = assignment.substring(0, eq);
                final String value = source.get(name);
                if (value != null) {
                    this.environment.put(name, value);
                }
            }
        }
    }

    private int processAssignments(
        final List<String> assignments, final Map<String, String> targetEnv
    ) {
        int code = 0;
        for (final String assignment : assignments) {
            final int eqIndex = assignment.indexOf('=');
            if (eqIndex > 0) {
                final String varName = assignment.substring(0, eqIndex);
                if (this.readonly.contains(varName)) {
                    this.err.println("sh: " + varName + ": readonly variable");
                    throw new ExitSignal(1);
                }
                final String rawValue = assignment.substring(eqIndex + 1);
                final String expanded = this.expandVariables(rawValue, targetEnv);
                targetEnv.put(varName, WordExpander.removeQuotes(expanded));
            }
        }
        return code;
    }

    @Override
    public String expandVariables(final String arg, final Map<String, String> env) {
        return WordExpander.expandVariablesImpl(arg, env, this);
    }

    @Override
    public List<String> expandArguments(final String arg, final Map<String, String> env) {
        List<String> fields = this.expandFields(arg, env);
        if (arg != null && !WordExpander.isQuoted(arg)
            && this.shellFlags.indexOf('f') < 0) {
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
            if (this.positionalParams.size() > 1) {
                fields.addAll(this.positionalParams.subList(1, this.positionalParams.size()));
            }
        } else {
            final String expanded = this.expandVariables(arg, env);
            if (WordExpander.isQuoted(arg)) {
                fields.add(WordExpander.removeQuotes(expanded));
            } else {
                final String ifsVal = env.getOrDefault("IFS", DEFAULT_IFS);
                for (final String field : WordExpander.splitFields(expanded, ifsVal)) {
                    fields.add(WordExpander.removeQuotes(field));
                }
            }
        }
        return fields;
    }

    @Override
    public String lookupVariable(final String name, final Map<String, String> env) {
        String result;
        if (name.isEmpty()) {
            return "";
        }
        if (name.length() == 1) {
            result = this.getSpecialParam(name.charAt(0));
        } else {
            result = null;
        }
        if (result == null) {
            if (name.chars().allMatch(Character::isDigit)) {
                final int idx = Integer.parseInt(name);
                if (idx < this.positionalParams.size()) {
                    result = this.positionalParams.get(idx);
                }
            } else {
                result = env.get(name);
                if (result == null && this.shellFlags.indexOf('u') >= 0) {
                    this.err.println("sh: " + name + ": parameter not set");
                    throw new ExitSignal(1);
                }
            }
        }
        return result;
    }

    @Override
    public String getSpecialParam(final char name) {
        switch (name) {
            case '?':
                return String.valueOf(this.lastExitCode);
            case '-':
                return this.shellFlags;
            case '$':
                return String.valueOf(ProcessHandle.current().pid());
            case '!':
                return String.valueOf(this.lastBgPid);
            case '#':
                return String.valueOf(this.positionalParams.size() - 1);
            case '@':
            case '*':
                return this.positionalParamsJoined();
            default:
                return this.positionalParamForDigit(name);
        }
    }

    private String positionalParamsJoined() {
        final String joined;
        if (this.positionalParams.size() <= 1) {
            joined = "";
        } else {
            String separator = " ";
            final String ifs = this.environment.get("IFS");
            if (ifs != null) {
                separator = ifs.isEmpty() ? "" : ifs.substring(0, 1);
            }
            joined = String.join(
                separator,
                this.positionalParams.subList(1, this.positionalParams.size())
            );
        }
        return joined;
    }

    private String positionalParamForDigit(final char chr) {
        String result = null;
        if (chr >= '0' && chr <= '9') {
            final int idx = chr - '0';
            if (idx < this.positionalParams.size()) {
                result = this.positionalParams.get(idx);
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
            code = EXIT_COMMAND_NOT_EXECUTABLE;
        }
        return code;
    }

    @Override
    public String resolveCommand(final String name) {
        return this.resolveCommand(name, this.environment);
    }

    private String resolveCommand(final String name, final Map<String, String> env) {
        String result;
        if (name.contains("/")) {
            result = new File(name).isFile() ? name : null;
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
        final boolean globbing = this.shellFlags.indexOf('f') < 0;
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
        final SimpleCommand cmd, final Map<String, String> cmdEnv
    ) {
        final Map<String, String> child = this.exportedEnvironment();
        for (final String assignment : cmd.getAssignments()) {
            final int eq = assignment.indexOf('=');
            if (eq > 0) {
                final String name = assignment.substring(0, eq);
                final String value = cmdEnv.get(name);
                if (value != null) {
                    child.put(name, value);
                }
            }
        }
        return child;
    }

    /**
     * @return The most recent background-job PID ({@code $!})
     */
    int getLastBgPid() {
        return this.lastBgPid;
    }

    /**
     * Throwable used to unwind a {@code break N} from N nested loops.
     * @since 1.0
     */
    static class BreakSignal extends RuntimeException {

        /** Serial version. */
        private static final long serialVersionUID = 1L;

        /** Remaining loop levels to break out of. */
        private final int levels;

        /**
         * @param levels Number of loop levels to break out of
         */
        BreakSignal(final int levels) {
            super();
            this.levels = levels;
        }

        /**
         * @return Remaining loop levels to break out of
         */
        int getLevels() {
            return this.levels;
        }
    }

    /**
     * Throwable used to unwind a {@code continue N} from N nested loops.
     * @since 1.0
     */
    static class ContinueSignal extends RuntimeException {

        /** Serial version. */
        private static final long serialVersionUID = 1L;

        /** Remaining loop levels to continue at. */
        private final int levels;

        /**
         * @param levels Number of loop levels to skip over
         */
        ContinueSignal(final int levels) {
            super();
            this.levels = levels;
        }

        /**
         * @return Remaining loop levels to skip over
         */
        int getLevels() {
            return this.levels;
        }
    }

    /**
     * Throwable used to unwind {@code exit} to the shell boundary.
     * @since 1.0
     */
    public static class ExitSignal extends RuntimeException {

        /** Serial version. */
        private static final long serialVersionUID = 1L;

        /** Requested exit status. */
        private final int code;

        /**
         * @param code Requested exit status
         */
        ExitSignal(final int code) {
            super();
            this.code = code;
        }

        /**
         * @return Requested exit status
         */
        int getCode() {
            return this.code;
        }
    }

    /**
     * Throwable used to unwind a {@code return N} from a function body.
     * @since 1.0
     */
    static class ReturnSignal extends RuntimeException {

        /** Serial version. */
        private static final long serialVersionUID = 1L;

        /** The exit status that the function should yield. */
        private final int code;

        /**
         * @param code The exit status to yield
         */
        ReturnSignal(final int code) {
            super();
            this.code = code;
        }

        /**
         * @return The exit status to yield
         */
        int getCode() {
            return this.code;
        }
    }

    @Override
    public Map<String, String> environmentMap() {
        return this.environment;
    }

    @Override
    public List<String> positionalParamsList() {
        return this.positionalParams;
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
        return this.jobManager;
    }

    @Override
    public CommandHistory history() {
        return this.history;
    }

    @Override
    public PrintStream outStream() {
        return this.out;
    }

    @Override
    public PrintStream errStream() {
        return this.err;
    }

    @Override
    public String shellFlags() {
        return this.shellFlags;
    }

    @Override
    public void setShellFlags(final String flags) {
        this.shellFlags = flags;
    }

    @Override
    public void setPositionalParams(final List<String> params) {
        final String script = this.positionalParams.isEmpty()
            ? "" : this.positionalParams.get(0);
        this.positionalParams.clear();
        this.positionalParams.add(script);
        this.positionalParams.addAll(params);
    }
}
