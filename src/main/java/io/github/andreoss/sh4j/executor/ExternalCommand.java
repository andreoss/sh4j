/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Runs a command as a child process found on {@code PATH}.
 *
 * @since 1.0
 */
final class ExternalCommand {

    /** Exit code for a command that could not be found on PATH. */
    static final int NOT_FOUND = 127;

    /** The shell that launches the process. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that launches the process
     */
    ExternalCommand(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Resolves a command name to an executable path.
     *
     * @param name The command name
     * @param env The environment holding {@code PATH}
     * @param cwd The shell working directory for relative names
     * @return The executable path, or null when not found
     */
    static String resolve(
        final String name, final Map<String, String> env, final File cwd
    ) {
        final String result;
        if (name.contains("/")) {
            if (ExternalCommand.within(cwd, name).isFile()) {
                result = name;
            } else {
                result = null;
            }
        } else {
            final String path = env.get("PATH");
            if (path == null || path.isEmpty()) {
                result = null;
            } else {
                result = ExternalCommand.search(path, name, cwd);
            }
        }
        return result;
    }

    /**
     * Runs the command and waits for it.
     *
     * @param cmd The command with its arguments and redirections
     * @param name The command name
     * @param cenv The command environment
     * @return The exit status of the process
     */
    int run(final SimpleCommand cmd, final String name, final Map<String, String> cenv) {
        final String resolved = ExternalCommand.resolve(
            name, cenv, this.shell.currentDirectory()
        );
        final int code;
        if (resolved == null) {
            code = ExternalCommand.NOT_FOUND;
        } else {
            final ProcessBuilder builder = this.builder(resolved, cmd, cenv);
            RedirectionHandler.setupRedirections(
                builder, cmd.getRedirections(),
                new RedirectionHandler.RedirectContext(
                    text -> this.shell.expandVariables(text, this.shell.environmentMap()),
                    this.shell.currentDirectory(), Collections.emptyMap()
                )
            );
            code = this.await(builder);
        }
        return code;
    }

    private ProcessBuilder builder(
        final String resolved,
        final SimpleCommand cmd,
        final Map<String, String> env
    ) {
        final List<String> command = new ArrayList<>(0);
        command.add(resolved);
        for (final String arg : cmd.getArguments()) {
            command.addAll(this.shell.expandArguments(arg, env));
        }
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(this.childEnvironment(cmd, env));
        builder.directory(this.shell.currentDirectory());
        return builder;
    }

    private Map<String, String> childEnvironment(
        final SimpleCommand cmd, final Map<String, String> cenv
    ) {
        final Map<String, String> child = this.shell.exportedEnvironment();
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

    private int await(final ProcessBuilder builder) {
        int code = 1;
        if (!Thread.currentThread().isInterrupted()) {
            code = this.launch(builder);
        }
        return code;
    }

    private int launch(final ProcessBuilder builder) {
        final ProcessBuilder.Redirect output = builder.redirectOutput();
        final ProcessBuilder.Redirect error = builder.redirectError();
        final ProcessBuilder.Redirect input = builder.redirectInput();
        final boolean console = System.in.equals(this.shell.inputStream());
        if (ProcessBuilder.Redirect.PIPE.equals(input) && console) {
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        }
        int code;
        try {
            final Process process = builder.start();
            final List<Thread> pumps = this.pumps(
                process, error, ProcessBuilder.Redirect.PIPE.equals(input) && !console
            );
            if (ProcessBuilder.Redirect.PIPE.equals(output)) {
                this.collect(process);
            }
            code = process.waitFor();
            for (final Thread pump : pumps) {
                pump.join();
            }
            this.shell.outStream().flush();
            this.shell.errStream().flush();
        } catch (final IOException ex) {
            code = this.failure(ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            code = 1;
        }
        return code;
    }

    private List<Thread> pumps(
        final Process process, final ProcessBuilder.Redirect error,
        final boolean feed
    ) {
        final List<Thread> pumps = new ArrayList<>(2);
        if (ProcessBuilder.Redirect.PIPE.equals(error)
            && process.getErrorStream() != null) {
            pumps.add(this.drain(process));
        }
        if (feed) {
            pumps.add(this.feed(process));
        }
        return pumps;
    }

    private Thread drain(final Process process) {
        final Thread thread = new Thread(
            () -> {
                try (InputStream stream = process.getErrorStream()) {
                    stream.transferTo(this.shell.errStream().bytes());
                } catch (final IOException ignored) {
                    this.shell.errStream().flush();
                }
            },
            "sh-stderr"
        );
        thread.start();
        return thread;
    }

    private Thread feed(final Process process) {
        final Thread thread = new Thread(
            () -> {
                try (OutputStream stream = process.getOutputStream()) {
                    this.shell.inputStream().transferTo(stream);
                } catch (final IOException ignored) {
                    this.shell.errStream().flush();
                }
            },
            "sh-stdin"
        );
        thread.start();
        return thread;
    }

    private void collect(final Process process) throws IOException {
        try (InputStream stream = process.getInputStream()) {
            stream.transferTo(this.shell.outStream().bytes());
        }
    }

    private int failure(final IOException exc) {
        final Throwable cause = exc.getCause();
        final int code;
        if (cause instanceof FileNotFoundException) {
            this.shell.errStream().println(String.format("sh: %s", cause.getMessage()));
            code = 1;
        } else {
            code = ExternalCommand.exitCode(exc);
        }
        return code;
    }

    private static int exitCode(final IOException exc) {
        final String msg = exc.getMessage();
        final int code;
        if (msg != null && (msg.contains("error=2") || msg.contains("No such file"))) {
            code = ExternalCommand.NOT_FOUND;
        } else {
            code = 126;
        }
        return code;
    }

    private static String search(
        final String path, final String name, final File cwd
    ) {
        String found = null;
        final String[] dirs = path.split(":", -1);
        for (final String dir : dirs) {
            final File file;
            if (dir.isEmpty()) {
                file = new File(cwd, name);
            } else {
                file = new File(dir, name);
            }
            if (file.isFile()) {
                found = file.getAbsolutePath();
                break;
            }
        }
        return found;
    }

    private static File within(final File cwd, final String name) {
        File file = new File(name);
        if (!file.isAbsolute()) {
            file = new File(cwd, name);
        }
        return file;
    }
}
