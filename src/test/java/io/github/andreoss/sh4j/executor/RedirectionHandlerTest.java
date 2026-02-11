/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import io.github.andreoss.sh4j.PrintSink;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for RedirectionHandler.
 *
 * @since 1.0
 */
final class RedirectionHandlerTest {

    @Test
    void redirectStderrToStdout() {
        final PrintSink out = new PrintSink(
            new ByteArrayOutputStream(),
            StandardCharsets.UTF_8
            );
        final PrintSink err = new PrintSink(
            new ByteArrayOutputStream(),
            StandardCharsets.UTF_8
            );
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        Assertions.assertSame(out, result.err(), "2>&1 should make err point to out");
        Assertions.assertSame(
            err,
            result.savederr(),
            "savedErr should be original err for restore"
        );
        Assertions.assertNull(result.savedout(), "stdout untouched");
    }

    @Test
    void redirectStdoutToStderr() {
        final PrintSink out = new PrintSink(
            new ByteArrayOutputStream(),
            StandardCharsets.UTF_8
            );
        final PrintSink err = new PrintSink(
            new ByteArrayOutputStream(),
            StandardCharsets.UTF_8
            );
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        Assertions.assertSame(err, result.out(), "1>&2 should make out point to err");
        Assertions.assertSame(
            out,
            result.savedout(),
            "savedOut should be original out for restore"
        );
    }

    @Test
    void noRedirsLeavesStreamsUnchanged() {
        final PrintSink out = new PrintSink(
            new ByteArrayOutputStream(),
            StandardCharsets.UTF_8
            );
        final PrintSink err = new PrintSink(
            new ByteArrayOutputStream(),
            StandardCharsets.UTF_8
            );
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.<Redirection>asList(),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        Assertions.assertSame(out, result.out());
        Assertions.assertSame(err, result.err());
        Assertions.assertNull(result.savedout());
        Assertions.assertNull(result.savederr());
    }

    @Test
    void heredocFileNullContent() {
        Assertions.assertNull(RedirectionHandler.heredocFile(null));
    }

    @Test
    void heredocFileRoundtrip() throws Exception {
        final File file = RedirectionHandler.heredocFile(String.format("alpha%nbeta%n"));
        Assertions.assertNotNull(file);
        Assertions.assertTrue(file.isFile());
        Assertions.assertEquals(
            String.format("alpha%nbeta%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void builtinOutputFileAndLastRedirectWins() throws Exception {
        final File first = File.createTempFile("rh-first", ".txt");
        final File second = File.createTempFile("rh-second", ".txt");
        final PrintSink out = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
        );
        final PrintSink err = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
        );
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(
                new Redirection(1, RedirectionType.OUTPUT, first.getAbsolutePath()),
                new Redirection(1, RedirectionType.OUTPUT, second.getAbsolutePath())
            ),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        Assertions.assertSame(out, swap.savedout());
        swap.out().println("hello");
        swap.out().flush();
        Assertions.assertEquals(
            String.format("hello%n"),
            new String(Files.readAllBytes(second.toPath()), StandardCharsets.UTF_8)
        );
        err.flush();
    }

    @Test
    void builtinAppendAndStderrForms() throws Exception {
        final File file = File.createTempFile("rh-append", ".txt");
        Files.write(file.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        final PrintSink out = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
        );
        final PrintSink err = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
        );
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(
                new Redirection(1, RedirectionType.APPEND, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.APPEND, file.getAbsolutePath())
            ),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        swap.out().print("y");
        swap.err().print("z");
        swap.out().flush();
        swap.err().flush();
        Assertions.assertEquals(
            "xyz",
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        err.flush();
    }

    @Test
    void builtinCloseAndDuplicateOutputForms() {
        final PrintSink out = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
        );
        final PrintSink err = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
        );
        final RedirectionHandler.StreamSwap closed = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        Assertions.assertSame(out, closed.savedout());
        Assertions.assertNotSame(out, closed.out());
        final RedirectionHandler.StreamSwap closerr = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        Assertions.assertSame(err, closerr.savederr());
        Assertions.assertNotSame(err, closerr.err());
    }

    @Test
    void setupRedirectionsFileKinds() throws Exception {
        final File file = File.createTempFile("rh-setup", ".txt");
        final ProcessBuilder readin = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readin,
            Collections.singletonList(
                new Redirection(0, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, readin.redirectInput().file());
        final ProcessBuilder readout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readout,
            Collections.singletonList(
                new Redirection(1, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, readout.redirectOutput().file());
        final ProcessBuilder readerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readerr,
            Collections.singletonList(
                new Redirection(2, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, readerr.redirectError().file());
        final ProcessBuilder input = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            input,
            Collections.singletonList(
                new Redirection(0, RedirectionType.INPUT, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, input.redirectInput().file());
    }

    @Test
    void setupRedirectionsOutputAppendAndErrors() throws Exception {
        final File file = File.createTempFile("rh-out", ".txt");
        final ProcessBuilder output = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            output,
            Arrays.asList(
                new Redirection(1, RedirectionType.OUTPUT, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.CLOBBER, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, output.redirectOutput().file());
        Assertions.assertEquals(file, output.redirectError().file());
        final ProcessBuilder append = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            append,
            Arrays.asList(
                new Redirection(1, RedirectionType.APPEND, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.APPEND, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(
            ProcessBuilder.Redirect.Type.APPEND, append.redirectOutput().type()
        );
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.APPEND, append.redirectError().type());
    }

    @Test
    void setupRedirectionsDupAndClose() {
        final ProcessBuilder closeout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(
            ProcessBuilder.Redirect.Type.WRITE, closeout.redirectOutput().type()
        );
        final ProcessBuilder closeerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeerr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(
            ProcessBuilder.Redirect.Type.WRITE, closeerr.redirectError().type()
        );
        final ProcessBuilder closein = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closein,
            Collections.singletonList(new Redirection(0, RedirectionType.DUP_INPUT, "-")),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.READ, closein.redirectInput().type());
        final ProcessBuilder duperr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            duperr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            UnaryOperator.identity()
        );
        Assertions.assertSame(duperr.redirectOutput(), duperr.redirectError());
        final ProcessBuilder dupout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            dupout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
            UnaryOperator.identity()
        );
        Assertions.assertSame(dupout.redirectError(), dupout.redirectOutput());
    }

    @Test
    void setupRedirectionsHeredocQuotedAndUnquoted() {
        final ProcessBuilder unquoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            unquoted,
            Collections.singletonList(
                new Redirection(
                    0, RedirectionType.HERE_DOC,
                    new Redirection.HereDoc("EOF", String.format("body%n"), false)
                )
            ),
            text -> text.toUpperCase(Locale.ROOT)
        );
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.READ, unquoted.redirectInput().type());
        final ProcessBuilder quoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            quoted,
            Collections.singletonList(
                new Redirection(
                    0, RedirectionType.HERE_DOC_DASH,
                    new Redirection.HereDoc("EOF", String.format("body%n"), true)
                )
            ),
            text -> {
                throw new IllegalStateException("quoted heredoc must not expand");
            }
        );
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.READ, quoted.redirectInput().type());
    }
}
