/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import org.junit.Assert;
import org.junit.Test;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;

/**
 * Tests for RedirectionHandler.
 * @since 1.0
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
public final class RedirectionHandlerTest {

    @Test
    public void redirectStderrToStdout() {
        final PrintStream out = new PrintStream(
            new ByteArrayOutputStream(),
            true,
            StandardCharsets.UTF_8
        );
        final PrintStream err = new PrintStream(
            new ByteArrayOutputStream(),
            true,
            StandardCharsets.UTF_8
        );
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            out,
            err,
            UnaryOperator.identity()
        );
        Assert.assertSame("2>&1 should make err point to out", out, result.err);
        Assert.assertSame(
            "savedErr should be original err for restore",
            err,
            result.savedErr
        );
        Assert.assertNull("stdout untouched", result.savedOut);
    }

    @Test
    public void redirectStdoutToStderr() {
        final PrintStream out = new PrintStream(
            new ByteArrayOutputStream(),
            true,
            StandardCharsets.UTF_8
        );
        final PrintStream err = new PrintStream(
            new ByteArrayOutputStream(),
            true,
            StandardCharsets.UTF_8
        );
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
            out,
            err,
            UnaryOperator.identity()
        );
        Assert.assertSame("1>&2 should make out point to err", err, result.out);
        Assert.assertSame(
            "savedOut should be original out for restore",
            out,
            result.savedOut
        );
    }

    @Test
    public void noRedirsLeavesStreamsUnchanged() {
        final PrintStream out = new PrintStream(
            new ByteArrayOutputStream(),
            true,
            StandardCharsets.UTF_8
        );
        final PrintStream err = new PrintStream(
            new ByteArrayOutputStream(),
            true,
            StandardCharsets.UTF_8
        );
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.<Redirection>asList(),
            out,
            err,
            UnaryOperator.identity()
        );
        Assert.assertSame(out, result.out);
        Assert.assertSame(err, result.err);
        Assert.assertNull(result.savedOut);
        Assert.assertNull(result.savedErr);
    }

    @Test
    public void heredocFileNullContent() {
        Assert.assertNull(RedirectionHandler.heredocFile(null));
    }

    @Test
    public void heredocFileRoundtrip() throws Exception {
        final java.io.File file = RedirectionHandler.heredocFile(String.format("alpha%nbeta%n"));
        Assert.assertNotNull(file);
        Assert.assertTrue(file.isFile());
        Assert.assertEquals(
            String.format("alpha%nbeta%n"),
            new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }
}
