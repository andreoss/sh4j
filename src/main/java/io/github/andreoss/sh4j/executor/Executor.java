/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import io.github.andreoss.sh4j.parser.SimpleCommand;

public class Executor {
    private final Map<String, String> environment;

    public Executor() {
        this.environment = System.getenv();
    }

    public Executor(Map<String, String> environment) {
        this.environment = environment;
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

        if (commands.size() == 1) {
            return executeCommand(commands.get(0));
        }

        return executePipelineInternal(pipeline);
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

    private ProcessBuilder createProcessBuilder(SimpleCommand cmd) {
        List<String> command = new ArrayList<>();
        command.add(cmd.getName());
        command.addAll(cmd.getArguments());

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
