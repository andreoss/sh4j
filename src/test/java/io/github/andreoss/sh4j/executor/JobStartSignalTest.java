/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JobControl} and {@link BackgroundTask}: a signal that
 * lands while a job starts its process, and a job that starts after its
 * shell changed.
 *
 * @since 1.0
 */
final class JobStartSignalTest {

    @Test
    void signalDuringStartEndsCommandWithSignalStatus() {
        final JobControl control = new JobControl();
        control.attach();
        control.send(15);
        Thread.interrupted();
        final Executor shell = new Executor();
        shell.setCaptureStream(new ByteArrayOutputStream());
        final int code = shell.execute(JobStartSignalTest.parse("sleep 30"));
        control.detach();
        Assertions.assertEquals(143, code);
    }

    @Test
    void signalDuringStartReportsNoError() {
        final JobControl control = new JobControl();
        control.attach();
        control.send(15);
        Thread.interrupted();
        final Executor shell = new Executor();
        shell.setCaptureStream(new ByteArrayOutputStream());
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        shell.setErrorStream(new PrintSink(err, StandardCharsets.UTF_8));
        shell.execute(JobStartSignalTest.parse("sleep 30"));
        control.detach();
        Assertions.assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void taskRunsInShellOfItsSubmission() {
        final Executor shell = new Executor();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        shell.setCaptureStream(out);
        shell.execute(JobStartSignalTest.parse("x=old"));
        final Supplier<Integer> task = new BackgroundTask(
            shell.subshell(), JobStartSignalTest.parse("echo \"$x\"").getPipelines().get(0)
        );
        shell.execute(JobStartSignalTest.parse("x=new"));
        Assertions.assertEquals(
            String.format("0 old%n"),
            String.format("%d %s", task.get(), out.toString(StandardCharsets.UTF_8))
        );
    }

    private static ListNode parse(final String input) {
        return new Parser(new Lexer(new CharReader(input))).parse();
    }
}
