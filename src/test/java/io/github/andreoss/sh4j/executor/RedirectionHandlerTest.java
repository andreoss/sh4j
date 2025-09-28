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

    @Test
    public void builtinOutputFileAndLastRedirectWins() throws Exception {
        final java.io.File first = java.io.File.createTempFile("rh-first", ".txt");
        final java.io.File second = java.io.File.createTempFile("rh-second", ".txt");
        final PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(
                new Redirection(1, RedirectionType.OUTPUT, first.getAbsolutePath()),
                new Redirection(1, RedirectionType.OUTPUT, second.getAbsolutePath())
            ),
            out,
            err,
            UnaryOperator.identity()
        );
        Assert.assertSame(out, swap.savedOut);
        swap.out.println("hello");
        swap.out.flush();
        Assert.assertEquals(
            "hello" + System.lineSeparator(),
            new String(java.nio.file.Files.readAllBytes(second.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void builtinAppendAndStderrForms() throws Exception {
        final java.io.File file = java.io.File.createTempFile("rh-append", ".txt");
        java.nio.file.Files.write(file.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        final PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(
                new Redirection(1, RedirectionType.APPEND, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.APPEND, file.getAbsolutePath())
            ),
            out,
            err,
            UnaryOperator.identity()
        );
        swap.out.print("y");
        swap.err.print("z");
        swap.out.flush();
        swap.err.flush();
        Assert.assertEquals(
            "xyz",
            new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void builtinCloseAndDuplicateOutputForms() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap closed = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            out,
            err,
            UnaryOperator.identity()
        );
        Assert.assertSame(out, closed.savedOut);
        Assert.assertNotSame(out, closed.out);
        final RedirectionHandler.StreamSwap closedErr = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            out,
            err,
            UnaryOperator.identity()
        );
        Assert.assertSame(err, closedErr.savedErr);
        Assert.assertNotSame(err, closedErr.err);
    }

    @Test
    public void setupRedirectionsFileKinds() throws Exception {
        final java.io.File file = java.io.File.createTempFile("rh-setup", ".txt");
        final ProcessBuilder readWriteIn = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readWriteIn,
            Arrays.asList(new Redirection(0, RedirectionType.READWRITE, file.getAbsolutePath())),
            UnaryOperator.identity()
        );
        Assert.assertEquals(file, readWriteIn.redirectInput().file());
        final ProcessBuilder readWriteOut = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readWriteOut,
            Arrays.asList(new Redirection(1, RedirectionType.READWRITE, file.getAbsolutePath())),
            UnaryOperator.identity()
        );
        Assert.assertEquals(file, readWriteOut.redirectOutput().file());
        final ProcessBuilder readWriteErr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readWriteErr,
            Arrays.asList(new Redirection(2, RedirectionType.READWRITE, file.getAbsolutePath())),
            UnaryOperator.identity()
        );
        Assert.assertEquals(file, readWriteErr.redirectError().file());
        final ProcessBuilder input = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            input,
            Arrays.asList(new Redirection(0, RedirectionType.INPUT, file.getAbsolutePath())),
            UnaryOperator.identity()
        );
        Assert.assertEquals(file, input.redirectInput().file());
    }

    @Test
    public void setupRedirectionsOutputAppendAndErrors() throws Exception {
        final java.io.File file = java.io.File.createTempFile("rh-out", ".txt");
        final ProcessBuilder output = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            output,
            Arrays.asList(
                new Redirection(1, RedirectionType.OUTPUT, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.CLOBBER, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assert.assertEquals(file, output.redirectOutput().file());
        Assert.assertEquals(file, output.redirectError().file());
        final ProcessBuilder append = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            append,
            Arrays.asList(
                new Redirection(1, RedirectionType.APPEND, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.APPEND, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assert.assertEquals(ProcessBuilder.Redirect.Type.APPEND, append.redirectOutput().type());
        Assert.assertEquals(ProcessBuilder.Redirect.Type.APPEND, append.redirectError().type());
    }

    @Test
    public void setupRedirectionsDupAndClose() {
        final ProcessBuilder closeOut = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeOut,
            Arrays.asList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        Assert.assertEquals(ProcessBuilder.Redirect.Type.WRITE, closeOut.redirectOutput().type());
        final ProcessBuilder closeErr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeErr,
            Arrays.asList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        Assert.assertEquals(ProcessBuilder.Redirect.Type.WRITE, closeErr.redirectError().type());
        final ProcessBuilder closeIn = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeIn,
            Arrays.asList(new Redirection(0, RedirectionType.DUP_INPUT, "-")),
            UnaryOperator.identity()
        );
        Assert.assertEquals(ProcessBuilder.Redirect.Type.READ, closeIn.redirectInput().type());
        final ProcessBuilder dupErr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            dupErr,
            Arrays.asList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            UnaryOperator.identity()
        );
        Assert.assertSame(dupErr.redirectOutput(), dupErr.redirectError());
        final ProcessBuilder dupOut = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            dupOut,
            Arrays.asList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
            UnaryOperator.identity()
        );
        Assert.assertSame(dupOut.redirectError(), dupOut.redirectOutput());
    }

    @Test
    public void setupRedirectionsHeredocQuotedAndUnquoted() {
        final ProcessBuilder unquoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            unquoted,
            Arrays.asList(
                new Redirection(0, RedirectionType.HERE_DOC, "EOF", "body\n", false)
            ),
            text -> text.toUpperCase(java.util.Locale.ROOT)
        );
        Assert.assertEquals(ProcessBuilder.Redirect.Type.READ, unquoted.redirectInput().type());
        final ProcessBuilder quoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            quoted,
            Arrays.asList(
                new Redirection(0, RedirectionType.HERE_DOC_DASH, "EOF", "body\n", true)
            ),
            text -> {
                throw new IllegalStateException("quoted heredoc must not expand");
            }
        );
        Assert.assertEquals(ProcessBuilder.Redirect.Type.READ, quoted.redirectInput().type());
    }
}
