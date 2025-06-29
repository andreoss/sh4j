/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;

/**
 * Tests for RedirectionHandler.
 * @since 1.0
 */
final class RedirectionHandlerTest {

    /** Identity. */
    private final UnaryOperator<String> identity = s -> s;

    @Test
    void redirectStderrToStdout() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream());
        final PrintStream err = new PrintStream(new ByteArrayOutputStream());
        final RedirectionHandler.StreamSwap result =
                RedirectionHandler.redirectOutputForBuiltin(List.of(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")), out, err, this.identity);
        Assertions.assertSame(out, result.err(), "2>&1 should make err point to out");
        Assertions.assertSame(
            err,
            result.savedErr(),
            "savedErr should be original err for restore"
        );
        Assertions.assertNull(result.savedOut(), "stdout untouched");
    }

    @Test
    void redirectStdoutToStderr() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream());
        final PrintStream err = new PrintStream(new ByteArrayOutputStream());
        final RedirectionHandler.StreamSwap result =
                RedirectionHandler.redirectOutputForBuiltin(List.of(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")), out, err, this.identity);
        Assertions.assertSame(err, result.out(), "1>&2 should make out point to err");
        Assertions.assertSame(
            out,
            result.savedOut(),
            "savedOut should be original out for restore"
        );
    }

    @Test
    void noRedirsLeavesStreamsUnchanged() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream());
        final PrintStream err = new PrintStream(new ByteArrayOutputStream());
        final RedirectionHandler.StreamSwap result =
                RedirectionHandler.redirectOutputForBuiltin(List.of(), out, err, this.identity);
        Assertions.assertSame(out, result.out());
        Assertions.assertSame(err, result.err());
        Assertions.assertNull(result.savedOut());
        Assertions.assertNull(result.savedErr());
    }

    @Test
    void heredocFileNullContent() {
        Assertions.assertNull(RedirectionHandler.heredocFile(null));
    }

    @Test
    void heredocFileRoundtrip() throws Exception {
        final java.io.File file = RedirectionHandler.heredocFile(String.format("alpha%nbeta%n"));
        Assertions.assertNotNull(file);
        Assertions.assertTrue(file.isFile());
        Assertions.assertEquals(
            String.format("alpha%nbeta%n"),
            java.nio.file.Files.readString(file.toPath())
        );
    }
}
