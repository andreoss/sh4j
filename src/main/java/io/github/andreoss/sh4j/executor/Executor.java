/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;
import io.github.andreoss.sh4j.parser.Grouping;
import io.github.andreoss.sh4j.parser.IfNode;
import io.github.andreoss.sh4j.parser.ForNode;
import io.github.andreoss.sh4j.parser.WhileNode;
import io.github.andreoss.sh4j.parser.CaseNode;
import io.github.andreoss.sh4j.parser.FunctionNode;

public class Executor {
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
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{?([a-zA-Z_][a-zA-Z0-9_]*)\\}?");

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

    private String executeCommandSubstitution(String cmdText) {
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

    private String evalArithmetic(String expr, Map<String, String> env) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < expr.length() && (Character.isLetterOrDigit(expr.charAt(j)) || expr.charAt(j) == '_')) j++;
                String name = expr.substring(i, j);
                String val = env.getOrDefault(name, "0");
                sb.append(val);
                i = j;
            } else {
                sb.append(c);
                i++;
            }
        }
        String expanded = sb.toString().trim();
        return String.valueOf(parseArithmetic(expanded, new int[]{0}));
    }

    private int parseArithmetic(String s, int[] pos) {
        int value = parseTerm(s, pos);
        while (pos[0] < s.length()) {
            skipWs(s, pos);
            if (pos[0] >= s.length()) break;
            char c = s.charAt(pos[0]);
            if (c == '+') { pos[0]++; value += parseTerm(s, pos); }
            else if (c == '-') { pos[0]++; value -= parseTerm(s, pos); }
            else break;
        }
        return value;
    }

    private int parseTerm(String s, int[] pos) {
        int value = parseFactor(s, pos);
        while (pos[0] < s.length()) {
            skipWs(s, pos);
            if (pos[0] >= s.length()) break;
            char c = s.charAt(pos[0]);
            if (c == '*') { pos[0]++; value *= parseFactor(s, pos); }
            else if (c == '/') { pos[0]++; int d = parseFactor(s, pos); value = d != 0 ? value / d : 0; }
            else if (c == '%') { pos[0]++; int d = parseFactor(s, pos); value = d != 0 ? value % d : 0; }
            else break;
        }
        return value;
    }

    private int parseFactor(String s, int[] pos) {
        skipWs(s, pos);
        if (pos[0] >= s.length()) return 0;
        char c = s.charAt(pos[0]);
        if (c == '(') {
            pos[0]++;
            int v = parseArithmetic(s, pos);
            skipWs(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ')') pos[0]++;
            return v;
        }
        if (c == '+') { pos[0]++; return parseFactor(s, pos); }
        if (c == '-') { pos[0]++; return -parseFactor(s, pos); }
        int sign = 1;
        int num = 0;
        while (pos[0] < s.length() && Character.isDigit(s.charAt(pos[0]))) {
            num = num * 10 + (s.charAt(pos[0]) - '0');
            pos[0]++;
        }
        return sign * num;
    }

    private void skipWs(String s, int[] pos) {
        while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) pos[0]++;
    }

    public int execute(ListNode list) {
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
        } else if (sep == ListNode.Separator.AMP) {
            return true;
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
                    if (matchGlob(pattern, word)) {
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
                builders.add(pb);
            }
        }

        try {
            for (int i = 0; i < builders.size(); i++) {
                processes.add(builders.get(i).start());
            }

            for (Process p : processes) {
                try {
                    p.getOutputStream().close();
                } catch (IOException e) {
                    // ignore
                }
            }

            int lastExitCode = 0;
            for (int i = 0; i < processes.size(); i++) {
                lastExitCode = processes.get(i).waitFor();
            }

            return lastExitCode;
        } catch (IOException e) {
            for (Process p : processes) {
                p.destroyForcibly();
            }
            return exitCodeForIoException(e);
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

        PrintStream savedOut = redirectOutputForBuiltin(cmd.getRedirections());
        Integer builtinResult = executeBuiltin(name, cmd.getArguments(), cmdEnv, cmd.getAssignments());
        if (savedOut != null) {
            out.close();
            out = savedOut;
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
            return 127;
        }

        ProcessBuilder pb = createProcessBuilder(resolved, cmd, cmdEnv);
        if (wordExpansionError) {
            wordExpansionError = false;
            return 1;
        }
        setupRedirections(pb, cmd.getRedirections());
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
            return exitCodeForIoException(e);
        } catch (InterruptedException e) {
            return 1;
        }
    }

    private PrintStream redirectOutputForBuiltin(List<Redirection> redirs) {
        for (Redirection r : redirs) {
            RedirectionType t = r.getType();
            int fd = r.getFd();
            if (t == RedirectionType.OUTPUT || t == RedirectionType.CLOBBER) {
                if (fd == 1) {
                    try {
                        PrintStream fileOut = new PrintStream(new File(expandVariables(r.getTarget(), environment)));
                        PrintStream saved = out;
                        out = fileOut;
                        return saved;
                    } catch (IOException e) {
                        return null;
                    }
                }
            } else if (t == RedirectionType.APPEND) {
                if (fd == 1) {
                    try {
                        PrintStream fileOut = new PrintStream(new FileOutputStream(expandVariables(r.getTarget(), environment), true));
                        PrintStream saved = out;
                        out = fileOut;
                        return saved;
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private Integer executeBuiltin(String name, List<String> arguments, Map<String, String> env, List<String> assignments) {
        if ("type".equals(name)) {
            int ret = 0;
            for (String arg : arguments) {
                String expanded = expandVariables(arg, env);
                if (isBuiltin(expanded)) {
                    out.println(expanded + " is a shell built-in");
                } else if (functions.containsKey(expanded)) {
                    out.println(expanded + " is a function");
                } else {
                    String resolved = resolveCommand(expanded);
                    if (resolved != null) {
                        out.println(expanded + " is " + resolved);
                    } else {
                        ret = 1;
                    }
                }
            }
            return ret;
        }
        if ("exit".equals(name)) {
            if (arguments.isEmpty()) {
                return 0;
            }
            try {
                int code = Integer.parseInt(expandVariables(arguments.get(0), env));
                if (wordExpansionError) {
                    wordExpansionError = false;
                    return 1;
                }
                return code;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        if ("export".equals(name)) {
            if (!assignments.isEmpty()) {
                environment.putAll(env);
            }
            for (String arg : arguments) {
                int eqIdx = arg.indexOf('=');
                if (eqIdx >= 0) {
                    environment.put(arg.substring(0, eqIdx), arg.substring(eqIdx + 1));
                } else {
                    environment.putIfAbsent(arg, "");
                }
            }
            if (assignments.isEmpty() && arguments.isEmpty()) {
                for (Map.Entry<String, String> e : environment.entrySet()) {
                    out.println("export " + e.getKey() + "=" + e.getValue());
                }
            }
            return 0;
        }
        if ("cd".equals(name)) {
            String target;
            if (arguments.isEmpty()) {
                target = environment.get("HOME");
                if (target == null) return 1;
            } else {
                target = expandVariables(arguments.get(0), env);
                if ("-".equals(target)) {
                    String old = environment.get("OLDPWD");
                    if (old == null) return 1;
                    out.println(old);
                    target = old;
                }
            }
            File dir = new File(target);
            if (!dir.isDirectory()) return 1;
            String absPath;
            try { absPath = dir.getCanonicalPath(); }
            catch (IOException e) { absPath = dir.getAbsolutePath(); }
            String oldPwd = environment.get("PWD");
            if (oldPwd != null) environment.put("OLDPWD", oldPwd);
            environment.put("PWD", absPath);
            System.setProperty("user.dir", absPath);
            return 0;
        }
        if ("echo".equals(name)) {
            boolean newline = true;
            int start = 0;
            if (!arguments.isEmpty() && "-n".equals(arguments.get(0))) {
                newline = false;
                start = 1;
            }
            boolean firstField = true;
            for (int i = start; i < arguments.size(); i++) {
                String arg = arguments.get(i);
                String expanded = expandVariables(arg, env);
                if (isQuoted(arg)) {
                    if (!firstField) out.print(" ");
                    out.print(removeQuotes(expanded));
                    firstField = false;
                } else {
                    String ifsVal = env.getOrDefault("IFS", " \t\n");
                    List<String> fields = splitFields(expanded, ifsVal);
                    fields = pathnameExpand(fields);
                    for (String field : fields) {
                        if (!firstField) out.print(" ");
                        out.print(removeQuotes(field));
                        firstField = false;
                    }
                }
            }
            if (newline) out.println();
            if (wordExpansionError) {
                wordExpansionError = false;
                return 1;
            }
            return 0;
        }
        if ("false".equals(name)) {
            return 1;
        }
        if ("true".equals(name)) {
            return 0;
        }
        if ("shift".equals(name)) {
            int n = 1;
            if (!arguments.isEmpty()) {
                try { n = Integer.parseInt(expandVariables(arguments.get(0), env)); }
                catch (NumberFormatException e) { return 1; }
            }
            if (n < 0) return 1;
            int maxShift = positionalParams.size() - 1;
            if (n == 0) return 0;
            if (n >= maxShift) {
                positionalParams.subList(1, positionalParams.size()).clear();
            } else {
                positionalParams.subList(1, 1 + n).clear();
            }
            return 0;
        }
        if ("unset".equals(name)) {
            for (String arg : arguments) {
                environment.remove(arg);
            }
            return 0;
        }
        if ("break".equals(name)) {
            throw new BreakSignal();
        }
        if ("continue".equals(name)) {
            throw new ContinueSignal();
        }
        if (":".equals(name)) {
            return 0;
        }
        if ("eval".equals(name)) {
            String input = String.join(" ", arguments);
            if (input.isEmpty()) return 0;
            CharReader r = new CharReader(input);
            Lexer l = new Lexer(r);
            Parser p = new Parser(l);
            ListNode ast = p.parse();
            return execute(ast);
        }
        if ("exec".equals(name)) {
            if (arguments.isEmpty()) return 0;
            String cmdName = expandVariables(arguments.get(0), env);
            String resolved = resolveCommand(cmdName);
            if (resolved == null) return 127;
            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.add(resolved);
            for (int i = 1; i < arguments.size(); i++) {
                String expanded = expandVariables(arguments.get(i), env);
                cmdArgs.add(removeQuotes(expanded));
            }
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.environment().putAll(env);
            pb.directory(new File(System.getProperty("user.dir")));
            try {
                Process process = pb.start();
                return process.waitFor();
            } catch (IOException e) {
                return exitCodeForIoException(e);
            } catch (InterruptedException e) {
                return 1;
            }
        }
        return null;
    }

    private static boolean isBuiltin(String name) {
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

    static String expandTilde(String arg, Map<String, String> env) {
        if (arg.isEmpty() || arg.charAt(0) != '~') {
            return arg;
        }
        int slash = arg.indexOf('/');
        String tildePart = slash >= 0 ? arg.substring(0, slash) : arg;
        String rest = slash >= 0 ? arg.substring(slash) : "";

        if ("~".equals(tildePart)) {
            return env.getOrDefault("HOME", "/root") + rest;
        }
        if ("~+".equals(tildePart)) {
            return env.getOrDefault("PWD", System.getProperty("user.dir")) + rest;
        }
        return arg;
    }

    static String normaliseBackticks(String arg) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < arg.length()) {
            char c = arg.charAt(i);
            if (c == '$' && i + 1 < arg.length()) {
                char next = arg.charAt(i + 1);
                if (next == '(') {
                    int depth = 1;
                    int j = i + 2;
                    while (j < arg.length() && depth > 0) {
                        char nc = arg.charAt(j);
                        if (nc == '(') depth++;
                        else if (nc == ')') depth--;
                        j++;
                    }
                    result.append(arg, i, j);
                    i = j;
                    continue;
                } else if (next == '{') {
                    int j = arg.indexOf('}', i + 2);
                    if (j > i + 2) {
                        result.append(arg, i, j + 1);
                        i = j + 1;
                        continue;
                    }
                }
            }
            if (c == '`') {
                int j = i + 1;
                StringBuilder content = new StringBuilder();
                boolean found = false;
                while (j < arg.length()) {
                    char nc = arg.charAt(j);
                    if (nc == '\\' && j + 1 < arg.length()) {
                        char nextc = arg.charAt(j + 1);
                        if (nextc == '`' || nextc == '$' || nextc == '\\') {
                            content.append(nextc);
                            j += 2;
                        } else {
                            content.append(nc);
                            content.append(nextc);
                            j += 2;
                        }
                    } else if (nc == '`') {
                        result.append("$(");
                        result.append(content);
                        result.append(')');
                        j++;
                        found = true;
                        break;
                    } else {
                        content.append(nc);
                        j++;
                    }
                }
                if (!found) {
                    result.append('`');
                    result.append(content);
                }
                i = j;
                continue;
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    String expandVariables(String arg, Map<String, String> env) {
        arg = expandTilde(arg, env);
        arg = normaliseBackticks(arg);
        StringBuilder expanded = new StringBuilder();
        int i = 0;
        while (i < arg.length()) {
            char c = arg.charAt(i);
            if (c == '$' && i + 1 < arg.length()) {
                char next = arg.charAt(i + 1);
                String special = getSpecialParam(next);
                if (special != null) {
                    expanded.append(special);
                    i += 2;
                    continue;
                }
                if (next == '(') {
                    if (i + 2 < arg.length() && arg.charAt(i + 2) == '(') {
                        int depth = 2;
                        int j = i + 3;
                        while (j < arg.length() && depth > 0) {
                            char nc = arg.charAt(j);
                            if (nc == '(') depth++;
                            else if (nc == ')') depth--;
                            j++;
                        }
                        if (depth == 0) {
                            String expr = arg.substring(i + 3, j - 2);
                            expanded.append(evalArithmetic(expr, env));
                            i = j;
                            continue;
                        }
                    } else {
                        int depth = 1;
                        int j = i + 2;
                        while (j < arg.length() && depth > 0) {
                            char nc = arg.charAt(j);
                            if (nc == '(') depth++;
                            else if (nc == ')') depth--;
                            j++;
                        }
                        if (depth == 0) {
                            String cmdText = arg.substring(i + 2, j - 1);
                            expanded.append(executeCommandSubstitution(cmdText));
                            i = j;
                            continue;
                        }
                    }
                }
                if (next == '{' && i + 2 < arg.length()) {
                    int close = arg.indexOf('}', i + 2);
                    if (close > i + 2) {
                        String inner = arg.substring(i + 2, close);
                        String replacement = expandBraced(inner, env);
                        if (replacement != null) {
                            expanded.append(replacement);
                            i = close + 1;
                            continue;
                        }
                    }
                }
            }
            expanded.append(c);
            i++;
        }
        arg = expanded.toString();

        Matcher matcher = VAR_PATTERN.matcher(arg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = env.getOrDefault(varName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String lookupVar(String name, Map<String, String> env) {
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

    private String expandBraced(String inner, Map<String, String> env) {
        if (inner.isEmpty()) return null;
        if (inner.charAt(0) == '#') {
            String val = lookupVar(inner.substring(1), env);
            return val != null ? String.valueOf(val.length()) : "0";
        }
        int nameEnd = 0;
        char fc = inner.charAt(0);
        if (isSpecialParamChar(fc) || fc == '_' || Character.isLetter(fc)) {
            nameEnd = 1;
            if (!isSpecialParamChar(fc)) {
                while (nameEnd < inner.length() && (Character.isLetterOrDigit(inner.charAt(nameEnd)) || inner.charAt(nameEnd) == '_')) {
                    nameEnd++;
                }
            }
        } else if (Character.isDigit(fc)) {
            while (nameEnd < inner.length() && Character.isDigit(inner.charAt(nameEnd))) {
                nameEnd++;
            }
        }
        if (nameEnd == 0) return null;
        String varName = inner.substring(0, nameEnd);
        String rest = inner.substring(nameEnd);
        String varValue = lookupVar(varName, env);

        if (rest.isEmpty()) {
            return varValue != null ? varValue : "";
        }
        if (rest.startsWith(":-")) {
            String word = rest.substring(2);
            if (varValue == null || varValue.isEmpty()) return expandVariables(word, env);
            return varValue;
        }
        if (rest.startsWith("-")) {
            String word = rest.substring(1);
            if (varValue == null) return expandVariables(word, env);
            return varValue;
        }
        if (rest.startsWith(":=")) {
            String word = rest.substring(2);
            if (varValue == null || varValue.isEmpty()) {
                String expanded = expandVariables(word, env);
                if (varName.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_')) {
                    env.put(varName, expanded);
                }
                return expanded;
            }
            return varValue;
        }
        if (rest.startsWith("=")) {
            String word = rest.substring(1);
            if (varValue == null) {
                String expanded = expandVariables(word, env);
                if (varName.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_')) {
                    env.put(varName, expanded);
                }
                return expanded;
            }
            return varValue;
        }
        if (rest.startsWith(":+")) {
            String word = rest.substring(2);
            if (varValue != null && !varValue.isEmpty()) return expandVariables(word, env);
            return "";
        }
        if (rest.startsWith("+")) {
            String word = rest.substring(1);
            if (varValue != null) return expandVariables(word, env);
            return "";
        }
        if (rest.startsWith(":?")) {
            String word = rest.substring(2);
            if (varValue == null || varValue.isEmpty()) {
                err.println("$" + varName + ": " + (!word.isEmpty() ? word : "parameter null or not set"));
                wordExpansionError = true;
                return "";
            }
            return varValue;
        }
        if (rest.startsWith("?")) {
            String word = rest.substring(1);
            if (varValue == null) {
                err.println("$" + varName + ": " + (!word.isEmpty() ? word : "parameter not set"));
                wordExpansionError = true;
                return "";
            }
            return varValue;
        }
        if (varValue == null) return "";
        if (rest.startsWith("##")) {
            String pattern = rest.substring(2);
            int longest = -1;
            for (int i = 0; i <= varValue.length(); i++) {
                String prefix = varValue.substring(0, i);
                if (globMatches(pattern, prefix)) longest = i;
            }
            return longest >= 0 ? varValue.substring(longest) : varValue;
        }
        if (rest.startsWith("#")) {
            String pattern = rest.substring(1);
            for (int i = 0; i <= varValue.length(); i++) {
                String prefix = varValue.substring(0, i);
                if (globMatches(pattern, prefix)) return varValue.substring(i);
            }
            return varValue;
        }
        if (rest.startsWith("%%")) {
            String pattern = rest.substring(2);
            int longest = -1;
            for (int i = 0; i <= varValue.length(); i++) {
                String suffix = varValue.substring(varValue.length() - i);
                if (globMatches(pattern, suffix)) longest = i;
            }
            return longest >= 0 ? varValue.substring(0, varValue.length() - longest) : varValue;
        }
        if (rest.startsWith("%")) {
            String pattern = rest.substring(1);
            for (int i = 0; i <= varValue.length(); i++) {
                String suffix = varValue.substring(varValue.length() - i);
                if (globMatches(pattern, suffix)) return varValue.substring(0, varValue.length() - i);
            }
            return varValue;
        }
        return null;
    }

    private static boolean isSpecialParamChar(char c) {
        return c == '?' || c == '-' || c == '$' || c == '!' || c == '@' || c == '*';
    }

    private String getSpecialParam(char c) {
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

    private int exitCodeForIoException(IOException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("error=2") || msg.contains("No such file"))) {
            return 127;
        }
        return 126;
    }

    private String resolveCommand(String name) {
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
            if (isQuoted(arg)) {
                command.add(removeQuotes(expanded));
            } else {
                String ifsVal = env.getOrDefault("IFS", " \t\n");
                List<String> fields = pathnameExpand(splitFields(expanded, ifsVal));
                for (int i = 0; i < fields.size(); i++) {
                    fields.set(i, removeQuotes(fields.get(i)));
                }
                command.addAll(fields);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(env);
        pb.directory(new File(System.getProperty("user.dir")));
        return pb;
    }

    private void setupRedirections(ProcessBuilder pb, List<Redirection> redirections) {
        for (Redirection redir : redirections) {
            int fd = redir.getFd();
            File file = new File(expandVariables(redir.getTarget(), environment));
            RedirectionType type = redir.getType();
            if (type == RedirectionType.DUP_INPUT) {
                if ("-".equals(redir.getTarget()) && fd == 0) {
                    pb.redirectInput(new File("/dev/null"));
                }
            } else if (type == RedirectionType.DUP_OUTPUT) {
                if ("-".equals(redir.getTarget())) {
                    if (fd == 1) pb.redirectOutput(new File("/dev/null"));
                    else if (fd == 2) pb.redirectError(new File("/dev/null"));
                } else if ("1".equals(redir.getTarget()) && fd == 2) {
                    pb.redirectErrorStream(true);
                } else if ("2".equals(redir.getTarget()) && fd == 1) {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/stderr")));
                }
            } else if (type == RedirectionType.READWRITE) {
                if (fd == 0) pb.redirectInput(file);
                else if (fd == 1) pb.redirectOutput(ProcessBuilder.Redirect.to(file));
                else if (fd == 2) pb.redirectError(ProcessBuilder.Redirect.to(file));
            } else if (type == RedirectionType.INPUT) {
                if (fd == 0) pb.redirectInput(file);
            } else if (type == RedirectionType.HERE_DOC || type == RedirectionType.HERE_DOC_DASH) {
                String content = redir.getHereDocContent();
                if (content != null) {
                    content = expandVariables(content, environment);
                }
                File src = heredocFile(content);
                if (src != null) {
                    if (fd == 0) pb.redirectInput(src);
                }
            } else if (type == RedirectionType.OUTPUT || type == RedirectionType.CLOBBER) {
                if (fd == 1) pb.redirectOutput(ProcessBuilder.Redirect.to(file));
                else if (fd == 2) pb.redirectError(ProcessBuilder.Redirect.to(file));
            } else if (type == RedirectionType.APPEND) {
                if (fd == 1) pb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
                else if (fd == 2) pb.redirectError(ProcessBuilder.Redirect.appendTo(file));
            }
        }
    }

    private boolean matchGlob(String pattern, String word) {
        return globMatches(pattern, word);
    }

    private boolean globMatches(String pattern, String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append('.');
            } else if (c == '.' || c == '(' || c == ')' || c == '+' || c == '^'
                    || c == '$' || c == '{' || c == '}' || c == '|' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '[') {
                int end = pattern.indexOf(']', i);
                if (end > i) {
                    sb.append(pattern, i, end + 1);
                    i = end;
                } else {
                    sb.append("\\[");
                }
            } else {
                sb.append(c);
            }
        }
        return s.matches(sb.toString());
    }

    static List<String> splitFields(String value, String ifs) {
        if (ifs == null || ifs.isEmpty()) {
            return List.of(value);
        }
        String wsChars = "";
        String nwsChars = "";
        for (int i = 0; i < ifs.length(); i++) {
            char c = ifs.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n') wsChars += c;
            else nwsChars += c;
        }
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inField = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (nwsChars.indexOf(c) >= 0) {
                result.add(field.toString());
                field = new StringBuilder();
                inField = false;
            } else if (wsChars.indexOf(c) >= 0) {
                if (inField) {
                    result.add(field.toString());
                    field = new StringBuilder();
                    inField = false;
                }
            } else {
                field.append(c);
                inField = true;
            }
        }
        if (inField) {
            result.add(field.toString());
        } else if (!nwsChars.isEmpty() && value.length() > 0
                && nwsChars.indexOf(value.charAt(value.length() - 1)) >= 0) {
            result.add(field.toString());
        }
        return result;
    }

    static boolean isQuoted(String arg) {
        if (arg == null || arg.length() < 2) return false;
        char first = arg.charAt(0);
        char last = arg.charAt(arg.length() - 1);
        return (first == '"' && last == '"') || (first == '\'' && last == '\'');
    }

    static String stripQuotes(String arg) {
        if (isQuoted(arg)) return arg.substring(1, arg.length() - 1);
        return arg;
    }

    static String removeQuotes(String word) {
        StringBuilder result = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                } else {
                    result.append(c);
                }
            } else if (inDouble) {
                if (c == '"') {
                    inDouble = false;
                } else if (c == '\\' && i + 1 < word.length()
                        && (word.charAt(i + 1) == '$' || word.charAt(i + 1) == '`'
                        || word.charAt(i + 1) == '"' || word.charAt(i + 1) == '\\')) {
                    result.append(word.charAt(i + 1));
                    i++;
                } else {
                    result.append(c);
                }
            } else {
                if (c == '\'') {
                    inSingle = true;
                } else if (c == '"') {
                    inDouble = true;
                } else if (c == '\\' && i + 1 < word.length()) {
                    result.append(word.charAt(i + 1));
                    i++;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    static List<String> pathnameExpand(List<String> fields) {
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (containsGlobChars(field)) {
                List<String> matches = globFiles(field);
                if (matches.isEmpty()) {
                    result.add(field);
                } else {
                    Collections.sort(matches);
                    result.addAll(matches);
                }
            } else {
                result.add(field);
            }
        }
        return result;
    }

    static boolean containsGlobChars(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0 || s.indexOf('[') >= 0;
    }

    private static List<String> globFiles(String pattern) {
        File cwd = new File(System.getProperty("user.dir"));
        String[] files = cwd.list();
        if (files == null) return List.of();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> matches = new ArrayList<>();
        for (String f : files) {
            if (matcher.matches(Paths.get(f))) {
                matches.add(f);
            }
        }
        return matches;
    }

    private static File heredocFile(String content) {
        if (content == null) return null;
        try {
            File temp = File.createTempFile("sh4j_heredoc", ".tmp");
            temp.deleteOnExit();
            try (FileWriter w = new FileWriter(temp)) {
                w.write(content);
            }
            return temp;
        } catch (IOException e) {
            return null;
        }
    }

    int getLastBgPid() {
        return lastBgPid;
    }

    private static class BreakSignal extends RuntimeException {}
    private static class ContinueSignal extends RuntimeException {}
}
