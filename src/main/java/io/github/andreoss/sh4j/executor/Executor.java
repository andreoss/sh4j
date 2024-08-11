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

public class Executor {
    private final Map<String, String> environment;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{?([a-zA-Z_][a-zA-Z0-9_]*)\\}?");

    public Executor() {
        this.environment = new HashMap<>(System.getenv());
    }

    public Executor(Map<String, String> environment) {
        this.environment = new HashMap<>(environment);
    }

    public String getVariable(String name) {
        return environment.get(name);
    }

    public int execute(ListNode list) {
        if (list.getPipelines().isEmpty()) {
            return 0;
        }

        int lastExitCode = 0;
        List<Pipeline> pipelines = list.getPipelines();
        List<ListNode.Separator> separators = list.getSeparators();

        for (int i = 0; i < pipelines.size(); i++) {
            Pipeline pipeline = pipelines.get(i);
            int exitCode = executePipeline(pipeline);

            ListNode.Separator sep = separators.get(i);
            boolean shouldContinue = shouldContinue(sep, exitCode);
            if (!shouldContinue) {
                return exitCode;
            }
            lastExitCode = exitCode;
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
        }
        return 0;
    }

    private int executePipelineInternal(Pipeline pipeline) {
        List<Node> commands = pipeline.getCommands();
        List<Process> processes = new ArrayList<>();
        List<ProcessBuilder> builders = new ArrayList<>();

        for (Node node : commands) {
            if (node instanceof SimpleCommand cmd) {
                ProcessBuilder pb = createProcessBuilder(cmd, new HashMap<>(environment));
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
            // For assignment-only commands, apply assignments to the executor's environment
            environment.putAll(cmdEnv);
            return 0;
        }

        Integer builtinResult = executeBuiltin(name, cmd.getArguments(), cmdEnv);
        if (builtinResult != null) {
            return builtinResult;
        }

        ProcessBuilder pb = createProcessBuilder(cmd, cmdEnv);
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

    private Integer executeBuiltin(String name, List<String> arguments, Map<String, String> env) {
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

    private String expandVariables(String arg, Map<String, String> env) {
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

    private int exitCodeForIoException(IOException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("error=2") || msg.contains("No such file"))) {
            return 127;
        }
        return 126;
    }

    private ProcessBuilder createProcessBuilder(SimpleCommand cmd, Map<String, String> env) {
        List<String> command = new ArrayList<>();
        command.add(cmd.getName());

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
