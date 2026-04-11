/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: wait.
 *
 * @since 1.0
 */
final class ExecutorWaitTest {

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    @Test
    void waitForBackgroundJobsInterrupted() {
        Thread.currentThread().interrupt();
        new Executor().execute(this.parse("sleep 1 &"));
        Assertions.assertTrue(
            Thread.interrupted(),
            "InterruptedException catch should re-set interrupt flag"
        );
    }

    @Test
    void waitNoArgsReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("wait")));
    }

    @Test
    void waitOnKnownBgPidReturnsZeroFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("sleep 0 &")));
    }

    @Test
    void waitOnKnownBgPidReturnsZeroSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("sleep 0 &"));
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("wait %d", executor.lastBackgroundPid())))
        );
    }

    @Test
    void waitOnUnknownPidReturnsError() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("wait 999999")));
    }

    @Test
    void whileLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(
            this.parse("c=true; while $c; do c=false; continue; echo unreached; done")
        );
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).isBlank(),
            String.format("continue should skip body, got: %s", bos)
        );
    }
}
