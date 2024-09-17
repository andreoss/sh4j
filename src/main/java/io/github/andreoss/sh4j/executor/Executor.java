/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
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

    public int execute(ListNode list) {
        if (list.getPipelines().isEmpty()) {
            return 0;
        }

        List<Pipeline> pipelines = list.getPipelines();
        List<ListNode.Separator> separators = list.getSeparators();

        for (int i = 0; i < pipelines.size(); i++) {
            Pipeline pipeline = pipelines.get(i);
            int exitCode = executePipeline(pipeline);

            ListNode.Separator sep = separators.get(i);
            boolean shouldContinue = shouldContinue(sep, exitCode);
            lastExitCode = exitCode;
            if (!shouldContinue) {
                return exitCode;
            }
        }

        return lastExitCode;
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
                lastExit = execute(n.getBody());
            }
            return lastExit;
        } else if (node instanceof WhileNode n) {
            int lastExit = 0;
            while (true) {
                int condExit = execute(n.getCondition());
                boolean runBody = n.isUntil() ? condExit != 0 : condExit == 0;
                if (!runBody) break;
                lastExit = execute(n.getBody());
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

        Integer builtinResult = executeBuiltin(name, cmd.getArguments(), cmdEnv, cmd.getAssignments());
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
        setupRedirections(pb, cmd.getRedirections());

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode;
        } catch (IOException e) {
            return exitCodeForIoException(e);
        } catch (InterruptedException e) {
            return 1;
        }
    }

    private Integer executeBuiltin(String name, List<String> arguments, Map<String, String> env, List<String> assignments) {
        if ("exit".equals(name)) {
            if (arguments.isEmpty()) {
                return 0;
            }
            try {
                return Integer.parseInt(expandVariables(arguments.get(0), env));
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
                    System.out.println("export " + e.getKey() + "=" + e.getValue());
                }
            }
            return 0;
        }
        return null;
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

    String expandVariables(String arg, Map<String, String> env) {
        arg = expandTilde(arg, env);
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
                throw new RuntimeException("$" + varName + ": " + (!word.isEmpty() ? word : "parameter null or not set"));
            }
            return varValue;
        }
        if (rest.startsWith("?")) {
            String word = rest.substring(1);
            if (varValue == null) {
                throw new RuntimeException("$" + varName + ": " + (!word.isEmpty() ? word : "parameter not set"));
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
            command.add(expandVariables(arg, env));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(env);
        pb.directory(new File(System.getProperty("user.dir")));
        return pb;
    }

    private void setupRedirections(ProcessBuilder pb, List<Redirection> redirections) {
        for (Redirection redir : redirections) {
            int fd = redir.getFd();
            File file = new File(redir.getTarget());
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
}
