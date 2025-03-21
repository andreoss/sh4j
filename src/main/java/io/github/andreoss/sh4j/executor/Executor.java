/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;
import io.github.andreoss.sh4j.parser.Grouping;
import io.github.andreoss.sh4j.parser.IfNode;
import io.github.andreoss.sh4j.parser.ForNode;
import io.github.andreoss.sh4j.parser.WhileNode;
import io.github.andreoss.sh4j.parser.CaseNode;
import io.github.andreoss.sh4j.parser.FunctionNode;

public class Executor implements BuiltinContext {
    private final Map<String, String> environment;
    private final Map<String, FunctionNode> functions;
    private int lastExitCode = 0;
    private String shellFlags = "";
    private int lastBgPid = 0;
    private final List<String> positionalParams;
    private final List<Thread> backgroundThreads = new ArrayList<>();
    private ByteArrayOutputStream captureStream;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private boolean wordExpansionError = false;
    static final int EXIT_COMMAND_NOT_FOUND = 127;
    private static final int EXIT_COMMAND_NOT_EXECUTABLE = 126;

    public Executor() {
        this.environment = new HashMap<>(System.getenv());
        this.functions = new HashMap<>();
        this.positionalParams = new ArrayList<>(List.of("sh"));
        setDefaults();
    }

    public Executor(Map<String, String> environment) {
        this.environment = new HashMap<>(environment);
        this.functions = new HashMap<>();
        this.positionalParams = new ArrayList<>(List.of("sh"));
        setDefaults();
    }

    public Executor(Map<String, String> environment, List<String> positionalParams) {
        this.environment = new HashMap<>(environment);
        this.functions = new HashMap<>();
        this.positionalParams = new ArrayList<>(positionalParams);
        setDefaults();
    }

    private void setDefaults() {
        environment.putIfAbsent("IFS", " \t\n");
        environment.putIfAbsent("HOME", System.getProperty("user.home", "/root"));
        environment.putIfAbsent("PATH", System.getenv("PATH"));
        if (environment.get("PATH") == null || environment.get("PATH").isEmpty()) {
            environment.put("PATH", "/usr/local/bin:/usr/bin:/bin");
        }
    }

    public String getVariable(String name) {
        return environment.get(name);
    }

    public void setCaptureStream(ByteArrayOutputStream stream) {
        this.captureStream = stream;
        this.out = new PrintStream(stream, true);
    }

    public void setOutputStream(PrintStream out) {
        this.out = out;
    }

    public void setErrorStream(PrintStream err) {
        this.err = err;
    }

    @Override public String executeCommandSubstitution(String cmdText) {
        CharReader reader = new CharReader(cmdText);
        Lexer lexer = new Lexer(reader);
        Parser parser = new Parser(lexer);
        ListNode ast = parser.parse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Executor sub = new Executor(new HashMap<>(environment), new ArrayList<>(positionalParams));
        sub.setCaptureStream(baos);
        sub.execute(ast);
        String out = baos.toString();
        if (out.endsWith("\n")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private final ArithmeticEvaluator arith = new ArithmeticEvaluator();

    @Override public String evalArithmetic(String expr, Map<String, String> env) {
        return arith.evalArithmetic(expr, env);
    }

    @Override public int execute(ListNode list) {
        if (list.getPipelines().isEmpty()) {
            return 0;
        }

        List<Pipeline> pipelines = list.getPipelines();
        List<ListNode.Separator> separators = list.getSeparators();

        for (int i = 0; i < pipelines.size(); i++) {
            Pipeline pipeline = pipelines.get(i);
            ListNode.Separator sep = separators.get(i);

            if (sep == ListNode.Separator.AMP) {
                executeAsync(pipeline);
                lastExitCode = 0;
            } else {
                int exitCode = executePipeline(pipeline);
                lastExitCode = exitCode;
                if (!shouldContinue(sep, exitCode)) {
                    waitForBackgroundJobs();
                    return exitCode;
                }
            }
        }

        waitForBackgroundJobs();
        return lastExitCode;
    }

    private void executeAsync(Pipeline pipeline) {
        Thread bgThread = new Thread(() -> {
            Executor bgExecutor = new Executor(new HashMap<>(environment));
            bgExecutor.out = out;
            bgExecutor.err = err;
            bgExecutor.executePipeline(pipeline);
        });
        bgThread.start();
        backgroundThreads.add(bgThread);
        lastBgPid = (int) bgThread.getId();
    }

    private void waitForBackgroundJobs() {
        for (Thread t : backgroundThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        backgroundThreads.clear();
    }

    private boolean shouldContinue(ListNode.Separator sep, int exitCode) {
        if (sep == ListNode.Separator.SEMI) {
            return true;
        } else if (sep == ListNode.Separator.AND_IF) {
            return exitCode == 0;
        } else if (sep == ListNode.Separator.OR_IF) {
            return exitCode != 0;
        }
        return true;
    }

    private int executePipeline(Pipeline pipeline) {
        List<Node> commands = pipeline.getCommands();
        if (commands.isEmpty() || commands.get(0) == null) {
            return 0;
        }

        int exitCode;
        if (commands.size() == 1) {
            exitCode = executeNode(commands.get(0));
        } else {
            exitCode = executePipelineInternal(pipeline);
        }

        if (pipeline.isBang()) {
            return exitCode == 0 ? 1 : 0;
        }
        return exitCode;
    }

    private int executeNode(Node node) {
        if (node instanceof SimpleCommand cmd) {
            return executeCommand(cmd);
        } else if (node instanceof Subshell sub) {
            Executor subExecutor = new Executor(new HashMap<>(environment));
            return subExecutor.execute(sub.getList());
        } else if (node instanceof Grouping g) {
            return execute(g.getList());
        } else if (node instanceof IfNode n) {
            int condExit = execute(n.getCondition());
            if (condExit == 0) {
                return execute(n.getThenBody());
            } else if (n.getElseBody() != null) {
                return execute(n.getElseBody());
            }
            return condExit;
        } else if (node instanceof ForNode n) {
            int lastExit = 0;
            for (String word : n.getWords()) {
                environment.put(n.getVariable(), word);
                try {
                    lastExit = execute(n.getBody());
                } catch (BreakSignal e) {
                    break;
                } catch (ContinueSignal e) {
                    continue;
                }
            }
            return lastExit;
        } else if (node instanceof WhileNode n) {
            int lastExit = 0;
            while (true) {
                int condExit = execute(n.getCondition());
                boolean runBody = n.isUntil() ? condExit != 0 : condExit == 0;
                if (!runBody) break;
                try {
                    lastExit = execute(n.getBody());
                } catch (BreakSignal e) {
                    break;
                } catch (ContinueSignal e) {
                    continue;
                }
            }
            return lastExit;
        } else if (node instanceof CaseNode n) {
            String word = expandVariables(n.getWord(), environment);
            for (CaseNode.CaseItem item : n.getItems()) {
                for (String pattern : item.getPatterns()) {
                    if (WordExpander.globMatches(pattern, word)) {
                        return execute(item.getBody());
                    }
                }
            }
            return 0;
        } else if (node instanceof FunctionNode n) {
            functions.put(n.getName(), n);
            return 0;
        }
        return 0;
    }

    private int executePipelineInternal(Pipeline pipeline) {
        List<Node> commands = pipeline.getCommands();
        List<Process> processes = new ArrayList<>();
        List<ProcessBuilder> builders = new ArrayList<>();

        for (Node node : commands) {
            if (node instanceof SimpleCommand cmd) {
                String resolved = resolveCommand(cmd.getName());
                if (resolved == null) resolved = cmd.getName();
                ProcessBuilder pb = createProcessBuilder(resolved, cmd, new HashMap<>(environment));
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                builders.add(pb);
            }
        }
        if (builders.isEmpty()) return 0;
        builders.get(0).redirectInput(ProcessBuilder.Redirect.INHERIT);
        ProcessBuilder last = builders.get(builders.size() - 1);
        if (captureStream != null) {
            last.redirectOutput(ProcessBuilder.Redirect.PIPE);
        } else {
            last.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }

        try {
            processes = ProcessBuilder.startPipeline(builders);

            if (captureStream != null) {
                try (var is = processes.get(processes.size() - 1).getInputStream()) {
                    is.transferTo(captureStream);
                }
            }

            int lastExitCode = 0;
            for (Process p : processes) {
                lastExitCode = p.waitFor();
            }

            return lastExitCode;
        } catch (IOException e) {
            for (Process p : processes) {
                p.destroyForcibly();
            }
            return exitCodeForIOException(e);
        } catch (InterruptedException e) {
            for (Process p : processes) {
                p.destroyForcibly();
            }
            return 1;
        }
    }

    private int executeCommand(SimpleCommand cmd) {
        Map<String, String> cmdEnv = new HashMap<>(environment);
        processAssignments(cmd.getAssignments(), cmdEnv);

        String name = cmd.getName();
        if (name == null || name.isEmpty()) {
            environment.putAll(cmdEnv);
            return 0;
        }

        RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
                cmd.getRedirections(), out, err, s -> expandVariables(s, environment));
        out = swap.out();
        err = swap.err();
        Integer builtinResult = dispatcher.dispatch(name, this, cmd.getArguments(), cmdEnv, cmd.getAssignments());
        if (swap.savedOut() != null) {
            out.close();
            out = swap.savedOut();
        }
        if (swap.savedErr() != null) {
            err.close();
            err = swap.savedErr();
        }
        if (builtinResult != null) {
            return builtinResult;
        }

        FunctionNode fn = functions.get(name);
        if (fn != null) {
            return executeNode(fn.getBody());
        }

        String resolved = resolveCommand(name);
        if (resolved == null) {
            return EXIT_COMMAND_NOT_FOUND;
        }

        ProcessBuilder pb = createProcessBuilder(resolved, cmd, cmdEnv);
        if (wordExpansionError) {
            wordExpansionError = false;
            return 1;
        }
        pb.inheritIO();
        RedirectionHandler.setupRedirections(pb, cmd.getRedirections(), s -> expandVariables(s, environment));
        if (captureStream != null) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }

        try {
            Process process = pb.start();
            if (captureStream != null) {
                try (var is = process.getInputStream()) {
                    is.transferTo(captureStream);
                }
            }
            int exitCode = process.waitFor();
            return exitCode;
        } catch (IOException e) {
            return exitCodeForIOException(e);
        } catch (InterruptedException e) {
            return 1;
        }
    }

    private final BuiltinDispatcher dispatcher = new BuiltinDispatcher();

    static boolean isBuiltin(String name) {
        return "exit".equals(name) || "export".equals(name) || "echo".equals(name)
                || "cd".equals(name) || "type".equals(name) || "exec".equals(name)
                || "true".equals(name) || "false".equals(name)
                || "shift".equals(name) || "unset".equals(name)
                || "break".equals(name) || "continue".equals(name)
                || ":".equals(name) || "eval".equals(name);
    }

    private void processAssignments(List<String> assignments, Map<String, String> targetEnv) {
        for (String assignment : assignments) {
            int eqIndex = assignment.indexOf('=');
            if (eqIndex > 0) {
                String varName = assignment.substring(0, eqIndex);
                String varValue = assignment.substring(eqIndex + 1);
                targetEnv.put(varName, varValue);
            }
        }
    }

    @Override public String expandVariables(String arg, Map<String, String> env) {
        return WordExpander.expandVariablesImpl(arg, env, this);
    }

    @Override public String lookupVariable(String name, Map<String, String> env) {
        if (name.length() == 1) {
            String special = getSpecialParam(name.charAt(0));
            if (special != null) return special;
        }
        if (name.chars().allMatch(Character::isDigit)) {
            int idx = Integer.parseInt(name);
            if (idx < positionalParams.size()) return positionalParams.get(idx);
            return "";
        }
        return env.get(name);
    }

    @Override public String getSpecialParam(char c) {
        return switch (c) {
            case '?' -> String.valueOf(lastExitCode);
            case '-' -> shellFlags;
            case '$' -> String.valueOf(ProcessHandle.current().pid());
            case '!' -> String.valueOf(lastBgPid);
            case '#' -> String.valueOf(positionalParams.size() - 1);
            case '@', '*' -> {
                if (positionalParams.size() <= 1) yield "";
                yield String.join(" ", positionalParams.subList(1, positionalParams.size()));
            }
            default -> {
                if (c >= '0' && c <= '9') {
                    int idx = c - '0';
                    if (idx < positionalParams.size()) yield positionalParams.get(idx);
                    yield "";
                }
                yield null;
            }
        };
    }

    @Override public int exitCodeForIOException(IOException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("error=2") || msg.contains("No such file"))) {
            return EXIT_COMMAND_NOT_FOUND;
        }
        return EXIT_COMMAND_NOT_EXECUTABLE;
    }

    @Override public String resolveCommand(String name) {
        if (name.contains("/")) {
            return new File(name).isFile() ? name : null;
        }
        String path = environment.get("PATH");
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] dirs = path.split(":", -1);
        for (String dir : dirs) {
            File f;
            if (dir.isEmpty()) {
                f = new File(name);
            } else {
                f = new File(dir, name);
            }
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    private ProcessBuilder createProcessBuilder(String resolved, SimpleCommand cmd, Map<String, String> env) {
        List<String> command = new ArrayList<>();
        command.add(resolved);

        for (String arg : cmd.getArguments()) {
            String expanded = expandVariables(arg, env);
            if (WordExpander.isQuoted(arg)) {
                command.add(WordExpander.removeQuotes(expanded));
            } else {
                String ifsVal = env.getOrDefault("IFS", " \t\n");
                List<String> fields = WordExpander.pathnameExpand(WordExpander.splitFields(expanded, ifsVal));
                for (int i = 0; i < fields.size(); i++) {
                    fields.set(i, WordExpander.removeQuotes(fields.get(i)));
                }
                command.addAll(fields);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(env);
        pb.directory(new File(System.getProperty("user.dir")));
        return pb;
    }


    int getLastBgPid() {
        return lastBgPid;
    }

    static class BreakSignal extends RuntimeException {}
    static class ContinueSignal extends RuntimeException {}

    @Override public Map<String, String> environmentMap() { return environment; }
    @Override public List<String> positionalParamsList() { return positionalParams; }
    @Override public Map<String, FunctionNode> functionsMap() { return functions; }
    @Override public PrintStream outStream() { return out; }
    @Override public boolean wordExpansionErrorOccurred() { return wordExpansionError; }
    @Override public void clearWordExpansionError() { wordExpansionError = false; }
    @Override public void setWordExpansionError() { wordExpansionError = true; }
    @Override public PrintStream errStream() { return err; }
}
