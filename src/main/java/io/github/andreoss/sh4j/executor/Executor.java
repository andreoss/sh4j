/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Default IFS value (space, tab, newline). */
    /**
     * POSIX 2.14 special built-in utilities — these take priority over user-
     * defined functions in command search (POSIX 2.9.1.1).
     */
    private static final java.util.Set<String> SPECIAL_BUILTINS = java.util.Set.of(
        ":", ".", "break", "continue", "eval", "exec", "exit", "export",
        "readonly", "return", "set", "shift", "times", "trap", "unset"
    );

    private static final String DEFAULT_IFS = " \t\n";
    /** Default fallback PATH when none is set. */
    private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

    /** Exit code for a command that could not be found on PATH. */
    static final int EXIT_COMMAND_NOT_FOUND = 127;
    /** Exit code for a command that exists but cannot be executed. */
    private static final int EXIT_COMMAND_NOT_EXECUTABLE = 126;

    /** Live environment (also used as POSIX export view). */
    private final Map<String, String> environment;
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
    /** Flag set by {@link WordExpander} on expansion failure. */
    private boolean wordExpansionError;
    /** Alias table (alias name → replacement text). */
    private Map<String, String> aliases;
    /** Command history for fc built-in. */
    private final CommandHistory history;

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
        this.wordExpansionError = false;
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
        this.environment.putIfAbsent(
            "PPID",
            Long.toString(
                ProcessHandle.current().parent().orElse(ProcessHandle.current()).pid()
            )
        );
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
        final Executor sub = new Executor(
            new HashMap<>(this.environment),
            new ArrayList<>(this.positionalParams)
        );
        sub.functions.putAll(this.functions);
        sub.setCaptureStream(baos);
        sub.execute(ast);
        String captured = baos.toString(StandardCharsets.UTF_8);
        if (captured.endsWith("\n")) {
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
        int code = 0;
        if (!list.getPipelines().isEmpty()) {
            code = this.runPipelineList(list);
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
                    final int exitCode = this.executePipeline(pipeline);
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
        this.waitForBackgroundJobs();
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
        final int jobId = this.jobManager.submit(() -> {
            final Executor bgExecutor = new Executor(new HashMap<>(this.environment));
            bgExecutor.out = this.out;
            bgExecutor.err = this.err;
            bgExecutor.executePipeline(pipeline);
        }, cmdStr);
        this.lastBgPid = jobId;
    }

    @Override
    public int lastBackgroundPid() {
        return this.lastBgPid;
    }

    @Override
    public void waitForBackgroundJobs() {
        this.jobManager.waitForAll();
    }

    private void shutdown() {
        this.jobManager.shutdown();
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
        final int code;
        if (node instanceof SimpleCommand) {
            final SimpleCommand cmd = (SimpleCommand) node;
            this.environment.put("LINENO", String.valueOf(cmd.getLineNumber()));
            code = this.executeCommand(cmd);
        } else if (node instanceof Subshell) {
            final Subshell sub = (Subshell) node;
            final Executor child = new Executor(new HashMap<>(this.environment));
            child.functions.putAll(this.functions);
            child.setOutputStream(this.out);
            child.setErrorStream(this.err);
            code = child.execute(sub.getList());
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
            final String ifsVal = this.environment.getOrDefault("IFS", DEFAULT_IFS);
            for (final String raw : node.getWords()) {
                final String expanded = this.expandVariables(raw, this.environment);
                if (WordExpander.isQuoted(raw)) {
                    words.add(WordExpander.removeQuotes(expanded));
                } else {
                    for (final String field : WordExpander.splitFields(expanded, ifsVal)) {
                        words.add(WordExpander.removeQuotes(field));
                    }
                }
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
        return lastExit;
    }

    private int runWhile(final WhileNode node) {
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
        return lastExit;
    }

    private int runCase(final CaseNode node) {
        final String word = this.expandVariables(node.getWord(), this.environment);
        int code = 0;
        boolean matched = false;
        for (final CaseNode.CaseItem item : node.getItems()) {
            if (matched) {
                break;
            }
            for (final String pattern : item.getPatterns()) {
                if (WordExpander.globMatches(pattern, word)) {
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
        final List<ProcessBuilder> builders = new ArrayList<>();
        for (final Node node : commands) {
            if (node instanceof SimpleCommand) {
                final SimpleCommand cmd = (SimpleCommand) node;
                this.environment.put("LINENO", String.valueOf(cmd.getLineNumber()));
                String resolved = this.resolveCommand(cmd.getName());
                if (resolved == null) {
                    resolved = cmd.getName();
                }
                final ProcessBuilder builder = this.createProcessBuilder(
                    resolved, cmd, new HashMap<>(this.environment)
                );
                builder.redirectError(ProcessBuilder.Redirect.INHERIT);
                builders.add(builder);
            }
        }
        final int code;
        if (builders.isEmpty()) {
            code = 0;
        } else {
            code = this.startAndAwaitPipeline(builders);
        }
        return code;
    }

    private int startAndAwaitPipeline(final List<ProcessBuilder> builders) {
        builders.get(0).redirectInput(ProcessBuilder.Redirect.INHERIT);
        final ProcessBuilder last = builders.get(builders.size() - 1);
        if (this.captureStream == null) {
            last.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        } else {
            last.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }
        List<Process> processes = new ArrayList<>();
        int code;
        try {
            processes = ProcessBuilder.startPipeline(builders);
            if (this.captureStream != null) {
                try (InputStream stream = processes.get(processes.size() - 1).getInputStream()) {
                    stream.transferTo(this.captureStream);
                }
            }
            int lastCode = 0;
            for (final Process proc : processes) {
                lastCode = proc.waitFor();
            }
            code = lastCode;
        } catch (final IOException ex) {
            for (final Process proc : processes) {
                proc.destroyForcibly();
            }
            code = this.exitCodeForIoException(ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            for (final Process proc : processes) {
                proc.destroyForcibly();
            }
            code = 1;
        }
        return code;
    }

    private int executeCommand(final SimpleCommand cmd) {
        final Map<String, String> cmdEnv = new HashMap<>(this.environment);
        final int assignCode = this.processAssignments(cmd.getAssignments(), cmdEnv);
        final String name = cmd.getName();
        final int code;
        if (name == null || name.isEmpty()) {
            this.environment.putAll(cmdEnv);
            code = assignCode;
        } else {
            code = this.runNamedCommand(cmd, name, cmdEnv);
        }
        return code;
    }

    private int runNamedCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cmdEnv
    ) {
        final String alias = this.aliases.get(name);
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
        if (this.functions.containsKey(name) && !isSpecialBuiltin(name)) {
            return this.invokeFunction(this.functions.get(name), cmd, cmdEnv);
        }
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            cmd.getRedirections(), this.out, this.err,
            text -> this.expandVariables(text, this.environment)
        );
        this.out = swap.out;
        this.err = swap.err;
        final InputStream savedIn = this.swapInputForBuiltin(cmd.getRedirections());
        final Integer builtinResult = this.dispatcher.dispatch(
            name, this, cmd.getArguments(), cmdEnv, cmd.getAssignments()
        );
        if (savedIn != null) {
            try {
                this.in.close();
            } catch (final IOException ignored) {
                // best-effort close of the swapped stdin
            }
            this.in = savedIn;
        }
        if (swap.savedOut != null) {
            this.out.close();
            this.out = swap.savedOut;
        }
        if (swap.savedErr != null) {
            this.err.close();
            this.err = swap.savedErr;
        }
        final int code;
        if (builtinResult != null) {
            code = builtinResult;
        } else {
            code = this.runExternalCommand(cmd, name, cmdEnv);
        }
        return code;
    }

    private int invokeFunction(
        final FunctionNode func, final SimpleCommand cmd, final Map<String, String> cmdEnv
    ) {
        final List<String> savedParams = new ArrayList<>(this.positionalParams);
        final List<String> funcArgs = new ArrayList<>();
        final String ifsVal = cmdEnv.getOrDefault("IFS", DEFAULT_IFS);
        for (final String arg : cmd.getArguments()) {
            final String expanded = this.expandVariables(arg, cmdEnv);
            if (WordExpander.isQuoted(arg)) {
                funcArgs.add(WordExpander.removeQuotes(expanded));
            } else {
                for (final String field : WordExpander.splitFields(expanded, ifsVal)) {
                    funcArgs.add(WordExpander.removeQuotes(field));
                }
            }
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
            } else if (type == RedirectionType.INPUT && redir.getFd() == 0) {
                final String target = this.expandVariables(redir.getTarget(), this.environment);
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

    private int runExternalCommand(
        final SimpleCommand cmd, final String name, final Map<String, String> cmdEnv
    ) {
        final String resolved = this.resolveCommand(name);
        int code;
        if (resolved == null) {
            code = EXIT_COMMAND_NOT_FOUND;
        } else {
            final ProcessBuilder builder = this.createProcessBuilder(resolved, cmd, cmdEnv);
            if (this.wordExpansionError) {
                this.wordExpansionError = false;
                code = 1;
            } else {
                builder.inheritIO();
                RedirectionHandler.setupRedirections(
                    builder, cmd.getRedirections(),
                    text -> this.expandVariables(text, this.environment)
                );
                if (this.captureStream != null) {
                    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                }
                code = this.startAndAwaitOne(builder);
            }
        }
        return code;
    }

    private int startAndAwaitOne(final ProcessBuilder builder) {
        int code;
        try {
            final Process process = builder.start();
            if (this.captureStream != null) {
                try (InputStream stream = process.getInputStream()) {
                    stream.transferTo(this.captureStream);
                }
            }
            code = process.waitFor();
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
    static boolean isBuiltin(final String name) {
        return "exit".equals(name) || "export".equals(name) || "echo".equals(name)
            || "cd".equals(name) || "type".equals(name) || "exec".equals(name)
            || "true".equals(name) || "false".equals(name)
            || "shift".equals(name) || "unset".equals(name)
            || "break".equals(name) || "continue".equals(name)
            || ":".equals(name) || "eval".equals(name);
    }

    private int processAssignments(
        final List<String> assignments, final Map<String, String> targetEnv
    ) {
        int code = 0;
        for (final String assignment : assignments) {
            final int eqIndex = assignment.indexOf('=');
            if (eqIndex > 0) {
                final String varName = assignment.substring(0, eqIndex);
                if ("1".equals(this.environment.get(varName + "_READONLY"))) {
                    this.err.println("sh: " + varName + ": readonly variable");
                    code = 1;
                    continue;
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
                } else {
                    result = "";
                }
            } else {
                result = env.get(name);
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
            joined = String.join(
                " ",
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

    @Override
    public int exitCodeForIoException(final IOException exc) {
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
        String result;
        if (name.contains("/")) {
            result = new File(name).isFile() ? name : null;
        } else {
            final String path = this.environment.get("PATH");
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
        for (final String arg : cmd.getArguments()) {
            final String expanded = this.expandVariables(arg, env);
            if (WordExpander.isQuoted(arg)) {
                command.add(WordExpander.removeQuotes(expanded));
            } else {
                final String ifsVal = env.getOrDefault("IFS", DEFAULT_IFS);
                final List<String> fields = WordExpander.pathnameExpand(
                    WordExpander.splitFields(expanded, ifsVal)
                );
                for (int idx = 0; idx < fields.size(); idx = idx + 1) {
                    fields.set(idx, WordExpander.removeQuotes(fields.get(idx)));
                }
                command.addAll(fields);
            }
        }
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(env);
        builder.directory(new File(System.getProperty("user.dir")));
        return builder;
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

        /** Default constructor (single level). */
        BreakSignal() {
            this(1);
        }

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

        /** Default constructor (innermost). */
        ContinueSignal() {
            this(1);
        }

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
    public InputStream stdin() {
        return this.in;
    }

    @Override
    public boolean wordExpansionErrorOccurred() {
        return this.wordExpansionError;
    }

    @Override
    public void clearWordExpansionError() {
        this.wordExpansionError = false;
    }

    @Override
    public void setWordExpansionError() {
        this.wordExpansionError = true;
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
