/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import io.github.andreoss.sh4j.parser.SimpleCommand;

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

            if (i < separators.size()) {
                ListNode.Separator sep = separators.get(i);
                boolean shouldContinue = shouldContinue(sep, exitCode);
                if (!shouldContinue) {
                    return exitCode;
                }
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
        List<SimpleCommand> commands = pipeline.getCommands();
        if (commands.isEmpty() || commands.get(0) == null) {
            return 0;
        }

        int exitCode;
        if (commands.size() == 1) {
            exitCode = executeCommand(commands.get(0));
        } else {
            exitCode = executePipelineInternal(pipeline);
        }

        if (pipeline.isBang()) {
            return exitCode == 0 ? 1 : 0;
        }
        return exitCode;
    }

    private int executePipelineInternal(Pipeline pipeline) {
        List<SimpleCommand> commands = pipeline.getCommands();
        List<Process> processes = new ArrayList<>();
        List<ProcessBuilder> builders = new ArrayList<>();

        for (SimpleCommand cmd : commands) {
            ProcessBuilder pb = createProcessBuilder(cmd);
            builders.add(pb);
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
        } catch (InterruptedException | IOException e) {
            for (Process p : processes) {
                p.destroyForcibly();
            }
            return 1;
        }
    }

    private int executeCommand(SimpleCommand cmd) {
        processAssignments(cmd.getAssignments());

        String name = cmd.getName();
        if (name == null || name.isEmpty()) {
            return 0;
        }

        ProcessBuilder pb = createProcessBuilder(cmd);
        setupRedirections(pb, cmd.getRedirections());

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode;
        } catch (InterruptedException | IOException e) {
            return 1;
        }
    }

    private void processAssignments(List<String> assignments) {
        for (String assignment : assignments) {
            int eqIndex = assignment.indexOf('=');
            if (eqIndex > 0) {
                String varName = assignment.substring(0, eqIndex);
                String varValue = assignment.substring(eqIndex + 1);
                environment.put(varName, varValue);
            }
        }
    }

    private String expandVariables(String arg) {
        Matcher matcher = VAR_PATTERN.matcher(arg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = environment.getOrDefault(varName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private ProcessBuilder createProcessBuilder(SimpleCommand cmd) {
        List<String> command = new ArrayList<>();
        command.add(cmd.getName());

        for (String arg : cmd.getArguments()) {
            command.add(expandVariables(arg));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(environment);
        pb.directory(new File(System.getProperty("user.dir")));
        return pb;
    }

    private void setupRedirections(ProcessBuilder pb, List<Redirection> redirections) {
        for (Redirection redir : redirections) {
            File file = new File(redir.getTarget());
            RedirectionType type = redir.getType();
            if (type == RedirectionType.INPUT) {
                pb.redirectInput(file);
            } else if (type == RedirectionType.OUTPUT) {
                pb.redirectOutput(ProcessBuilder.Redirect.to(file));
            } else if (type == RedirectionType.APPEND) {
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
            } else if (type == RedirectionType.CLOBBER) {
                pb.redirectOutput(ProcessBuilder.Redirect.to(file));
            }
        }
    }
}
