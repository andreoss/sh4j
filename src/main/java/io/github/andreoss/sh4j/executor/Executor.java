/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    /**
     * Builds an executor seeded with the current process environment.
     */
    public Executor() {
        this(System.getenv(), List.of("sh"));
    }

    /**
     * Builds an executor with the given environment view.
     * @param environment Environment variables (copied, not aliased)
     */
    public Executor(final Map<String, String> environment) {
        this(environment, List.of("sh"));
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
            if (sep == ListNode.Separator.AMP) {
                this.executeAsync(pipeline);
                this.lastExitCode = 0;
            } else {
                final int exitCode = this.executePipeline(pipeline);
                this.lastExitCode = exitCode;
                if (!shouldContinue(sep, exitCode)) {
                    stopped = true;
                }
            }
            idx = idx + 1;
        }
        this.waitForBackgroundJobs();
        return this.lastExitCode;
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
            if (!line.isEmpty()) {
                line.append(" | ");
            }
            line.append(node.toString());
        }
        return line.toString();
    }

    private static boolean shouldContinue(final ListNode.Separator sep, final int exitCode) {
        final boolean cont;
        if (sep == ListNode.Separator.AND_IF) {
            cont = exitCode == 0;
        } else if (sep == ListNode.Separator.OR_IF) {
            cont = exitCode != 0;
        } else {
            cont = true;
        }
        return cont;
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
        if (node instanceof SimpleCommand cmd) {
            code = this.executeCommand(cmd);
        } else if (node instanceof Subshell sub) {
            code = new Executor(new HashMap<>(this.environment)).execute(sub.getList());
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
        final int condExit = this.execute(node.getCondition());
        final int code;
        if (condExit == 0) {
            code = this.execute(node.getThenBody());
        } else if (node.getElseBody() == null) {
            code = condExit;
        } else {
            code = this.execute(node.getElseBody());
        }
        return code;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private int runFor(final ForNode node) {
        int lastExit = 0;
        boolean broken = false;
        for (final String word : node.getWords()) {
            if (broken) {
                break;
            }
            this.environment.put(node.getVariable(), word);
            try {
                lastExit = this.execute(node.getBody());
            } catch (final BreakSignal ex) {
                broken = true;
            } catch (final ContinueSignal ex) {
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
                    running = false;
                } catch (final ContinueSignal ex) {
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
            if (node instanceof SimpleCommand cmd) {
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
        processAssignments(cmd.getAssignments(), cmdEnv);
        final String name = cmd.getName();
        final int code;
        if (name == null || name.isEmpty()) {
            this.environment.putAll(cmdEnv);
            code = 0;
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
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            cmd.getRedirections(), this.out, this.err,
            text -> this.expandVariables(text, this.environment)
        );
        this.out = swap.out();
        this.err = swap.err();
        final Integer builtinResult = this.dispatcher.dispatch(
            name, this, cmd.getArguments(), cmdEnv, cmd.getAssignments()
        );
        if (swap.savedOut() != null) {
            this.out.close();
            this.out = swap.savedOut();
        }
        if (swap.savedErr() != null) {
            this.err.close();
            this.err = swap.savedErr();
        }
        final int code;
        if (builtinResult != null) {
            code = builtinResult;
        } else {
            final FunctionNode func = this.functions.get(name);
            if (func != null) {
                code = this.executeNode(func.getBody());
            } else {
                code = this.runExternalCommand(cmd, name, cmdEnv);
            }
        }
        return code;
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

    private static void processAssignments(
        final List<String> assignments, final Map<String, String> targetEnv
    ) {
        for (final String assignment : assignments) {
            final int eqIndex = assignment.indexOf('=');
            if (eqIndex > 0) {
                final String varName = assignment.substring(0, eqIndex);
                final String varValue = assignment.substring(eqIndex + 1);
                targetEnv.put(varName, varValue);
            }
        }
    }

    @Override
    public String expandVariables(final String arg, final Map<String, String> env) {
        return WordExpander.expandVariablesImpl(arg, env, this);
    }

    @Override
    public String lookupVariable(final String name, final Map<String, String> env) {
        String result;
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
        return switch (name) {
            case '?' -> String.valueOf(this.lastExitCode);
            case '-' -> this.shellFlags;
            case '$' -> String.valueOf(ProcessHandle.current().pid());
            case '!' -> String.valueOf(this.lastBgPid);
            case '#' -> String.valueOf(this.positionalParams.size() - 1);
            case '@', '*' -> this.positionalParamsJoined();
            default -> this.positionalParamForDigit(name);
        };
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
     * Throwable used to unwind a {@code break} from a loop body. Package
     * private; thrown by the break-builtin and caught in runFor/runWhile.
     * @since 1.0
     */
    static class BreakSignal extends RuntimeException {

        /** Serial version. */
        private static final long serialVersionUID = 1L;

        /** Default constructor. */
        BreakSignal() {
            super();
        }
    }

    /**
     * Throwable used to unwind a {@code continue} from a loop body.
     * @since 1.0
     */
    static class ContinueSignal extends RuntimeException {

        /** Serial version. */
        private static final long serialVersionUID = 1L;

        /** Default constructor. */
        ContinueSignal() {
            super();
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
}
