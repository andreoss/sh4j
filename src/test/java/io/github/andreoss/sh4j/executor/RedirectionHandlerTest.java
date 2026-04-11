/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
    void redirectStderrToStdoutFirst() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        Assertions.assertSame(
            out,
            RedirectionHandler.redirectOutputForBuiltin(
                Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
                new RedirectionHandler.Streams(
                    out,
                    new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
                ),
                UnaryOperator.identity()
            ).err(),
            "2>&1 should make err point to out"
        );
    }

    @Test
    void redirectStderrToStdoutSecond() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        result.err();
        Assertions.assertSame(
            err,
            result.savederr(),
            "savedErr should be original err for restore"
        );
    }

    @Test
    void redirectStderrToStdoutThird() {
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            new RedirectionHandler.Streams(
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8),
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
            ),
            UnaryOperator.identity()
        );
        result.err();
        result.savederr();
        Assertions.assertNull(result.savedout(), "stdout untouched");
    }

    @Test
    void redirectStdoutToStderrFirst() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        Assertions.assertSame(
            err,
            RedirectionHandler.redirectOutputForBuiltin(
                Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
                new RedirectionHandler.Streams(out, err),
                UnaryOperator.identity()
            ).out(),
            "1>&2 should make out point to err"
        );
    }

    @Test
    void redirectStdoutToStderrSecond() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
            new RedirectionHandler.Streams(
                out,
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
            ),
            UnaryOperator.identity()
        );
        result.out();
        Assertions.assertSame(
            out,
            result.savedout(),
            "savedOut should be original out for restore"
        );
    }

    @Test
    void noRedirsLeavesStreamsUnchangedFirst() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        Assertions.assertSame(
            out,
            RedirectionHandler.redirectOutputForBuiltin(
                Arrays.<Redirection>asList(),
                new RedirectionHandler.Streams(
                    out,
                    new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
                ),
                UnaryOperator.identity()
            ).out()
        );
    }

    @Test
    void noRedirsLeavesStreamsUnchangedSecond() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.<Redirection>asList(),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        result.out();
        Assertions.assertSame(err, result.err());
    }

    @Test
    void noRedirsLeavesStreamsUnchangedThird() {
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.<Redirection>asList(),
            new RedirectionHandler.Streams(
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8),
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
            ),
            UnaryOperator.identity()
        );
        result.out();
        result.err();
        Assertions.assertNull(result.savedout());
    }

    @Test
    void noRedirsLeavesStreamsUnchangedFourth() {
        final RedirectionHandler.StreamSwap result = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.<Redirection>asList(),
            new RedirectionHandler.Streams(
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8),
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
            ),
            UnaryOperator.identity()
        );
        result.out();
        result.err();
        result.savedout();
        Assertions.assertNull(result.savederr());
    }

    @Test
    void heredocFileNullContent() {
        Assertions.assertNull(RedirectionHandler.heredocFile(null));
    }

    @Test
    void heredocFileRoundtripFirst() throws Exception {
        Assertions.assertNotNull(RedirectionHandler.heredocFile(String.format("alpha%nbeta%n")));
    }

    @Test
    void heredocFileRoundtripSecond() throws Exception {
        Assertions.assertTrue(
            RedirectionHandler.heredocFile(String.format("alpha%nbeta%n")).isFile()
        );
    }

    @Test
    void heredocFileRoundtripThird() throws Exception {
        Assertions.assertEquals(
            String.format("alpha%nbeta%n"),
            new String(
                Files.readAllBytes(
                    RedirectionHandler.heredocFile(String.format("alpha%nbeta%n")).toPath()
                ),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void builtinOutputFileAndLastRedirectWinsFirst() throws Exception {
        final File first = File.createTempFile("rh-first", ".txt");
        final File second = File.createTempFile("rh-second", ".txt");
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        Assertions.assertSame(
            out,
            RedirectionHandler.redirectOutputForBuiltin(
                Arrays.asList(
                    new Redirection(1, RedirectionType.OUTPUT, first.getAbsolutePath()),
                    new Redirection(1, RedirectionType.OUTPUT, second.getAbsolutePath())
                ),
                new RedirectionHandler.Streams(out, err),
                UnaryOperator.identity()
            ).savedout()
        );
        err.flush();
    }

    @Test
    void builtinOutputFileAndLastRedirectWinsSecond() throws Exception {
        final File first = File.createTempFile("rh-first", ".txt");
        final File second = File.createTempFile("rh-second", ".txt");
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap swap = RedirectionHandler.redirectOutputForBuiltin(
            Arrays.asList(
                new Redirection(1, RedirectionType.OUTPUT, first.getAbsolutePath()),
                new Redirection(1, RedirectionType.OUTPUT, second.getAbsolutePath())
            ),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        swap.savedout();
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
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
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
    void builtinCloseAndDuplicateOutputFormsFirst() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        Assertions.assertSame(
            out,
            RedirectionHandler.redirectOutputForBuiltin(
                Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
                new RedirectionHandler.Streams(
                    out,
                    new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
                ),
                UnaryOperator.identity()
            ).savedout()
        );
    }

    @Test
    void builtinCloseAndDuplicateOutputFormsSecond() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap closed = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            new RedirectionHandler.Streams(
                out,
                new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8)
            ),
            UnaryOperator.identity()
        );
        closed.savedout();
        Assertions.assertNotSame(out, closed.out());
    }

    @Test
    void builtinCloseAndDuplicateOutputFormsThird() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap closed = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        closed.savedout();
        closed.out();
        Assertions.assertSame(
            err,
            RedirectionHandler.redirectOutputForBuiltin(
                Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
                new RedirectionHandler.Streams(out, err),
                UnaryOperator.identity()
            ).savederr()
        );
    }

    @Test
    void builtinCloseAndDuplicateOutputFormsFourth() {
        final PrintSink out = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final PrintSink err = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        final RedirectionHandler.StreamSwap closed = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        closed.savedout();
        closed.out();
        final RedirectionHandler.StreamSwap closerr = RedirectionHandler.redirectOutputForBuiltin(
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            new RedirectionHandler.Streams(out, err),
            UnaryOperator.identity()
        );
        closerr.savederr();
        Assertions.assertNotSame(err, closerr.err());
    }

    @Test
    void setupRedirectionsFileKindsFirst() throws Exception {
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
    }

    @Test
    void setupRedirectionsFileKindsSecond() throws Exception {
        final File file = File.createTempFile("rh-setup", ".txt");
        final ProcessBuilder readin = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readin,
            Collections.singletonList(
                new Redirection(0, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        readin.redirectInput().file();
        final ProcessBuilder readout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readout,
            Collections.singletonList(
                new Redirection(1, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, readout.redirectOutput().file());
    }

    @Test
    void setupRedirectionsFileKindsThird() throws Exception {
        final File file = File.createTempFile("rh-setup", ".txt");
        final ProcessBuilder readin = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readin,
            Collections.singletonList(
                new Redirection(0, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        readin.redirectInput().file();
        final ProcessBuilder readout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readout,
            Collections.singletonList(
                new Redirection(1, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        readout.redirectOutput().file();
        final ProcessBuilder readerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readerr,
            Collections.singletonList(
                new Redirection(2, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(file, readerr.redirectError().file());
    }

    @Test
    void setupRedirectionsFileKindsFourth() throws Exception {
        final File file = File.createTempFile("rh-setup", ".txt");
        final ProcessBuilder readin = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readin,
            Collections.singletonList(
                new Redirection(0, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        readin.redirectInput().file();
        final ProcessBuilder readout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readout,
            Collections.singletonList(
                new Redirection(1, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        readout.redirectOutput().file();
        final ProcessBuilder readerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            readerr,
            Collections.singletonList(
                new Redirection(2, RedirectionType.READWRITE, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        readerr.redirectError().file();
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
    void setupRedirectionsOutputAppendAndErrorsFirst() throws Exception {
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
    }

    @Test
    void setupRedirectionsOutputAppendAndErrorsSecond() throws Exception {
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
        output.redirectOutput().file();
        Assertions.assertEquals(file, output.redirectError().file());
    }

    @Test
    void setupRedirectionsOutputAppendAndErrorsThird() throws Exception {
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
        output.redirectOutput().file();
        output.redirectError().file();
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
            ProcessBuilder.Redirect.Type.APPEND,
            append.redirectOutput().type()
        );
    }

    @Test
    void setupRedirectionsOutputAppendAndErrorsFourth() throws Exception {
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
        output.redirectOutput().file();
        output.redirectError().file();
        final ProcessBuilder append = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            append,
            Arrays.asList(
                new Redirection(1, RedirectionType.APPEND, file.getAbsolutePath()),
                new Redirection(2, RedirectionType.APPEND, file.getAbsolutePath())
            ),
            UnaryOperator.identity()
        );
        append.redirectOutput();
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.APPEND, append.redirectError().type());
    }

    @Test
    void setupRedirectionsDupAndCloseFirst() {
        final ProcessBuilder closeout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(
            ProcessBuilder.Redirect.Type.WRITE,
            closeout.redirectOutput().type()
        );
    }

    @Test
    void setupRedirectionsDupAndCloseSecond() {
        final ProcessBuilder closeout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeout.redirectOutput();
        final ProcessBuilder closeerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeerr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(
            ProcessBuilder.Redirect.Type.WRITE,
            closeerr.redirectError().type()
        );
    }

    @Test
    void setupRedirectionsDupAndCloseThird() {
        final ProcessBuilder closeout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeout.redirectOutput();
        final ProcessBuilder closeerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeerr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeerr.redirectError();
        final ProcessBuilder closein = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closein,
            Collections.singletonList(new Redirection(0, RedirectionType.DUP_INPUT, "-")),
            UnaryOperator.identity()
        );
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.READ, closein.redirectInput().type());
    }

    @Test
    void setupRedirectionsDupAndCloseFourth() {
        final ProcessBuilder closeout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeout.redirectOutput();
        final ProcessBuilder closeerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeerr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeerr.redirectError();
        final ProcessBuilder closein = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closein,
            Collections.singletonList(new Redirection(0, RedirectionType.DUP_INPUT, "-")),
            UnaryOperator.identity()
        );
        closein.redirectInput();
        final ProcessBuilder duperr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            duperr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            UnaryOperator.identity()
        );
        Assertions.assertSame(duperr.redirectOutput(), duperr.redirectError());
    }

    @Test
    void setupRedirectionsDupAndCloseFifth() {
        final ProcessBuilder closeout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeout.redirectOutput();
        final ProcessBuilder closeerr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closeerr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "-")),
            UnaryOperator.identity()
        );
        closeerr.redirectError();
        final ProcessBuilder closein = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            closein,
            Collections.singletonList(new Redirection(0, RedirectionType.DUP_INPUT, "-")),
            UnaryOperator.identity()
        );
        closein.redirectInput();
        final ProcessBuilder duperr = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            duperr,
            Collections.singletonList(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")),
            UnaryOperator.identity()
        );
        duperr.redirectOutput();
        duperr.redirectError();
        final ProcessBuilder dupout = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            dupout,
            Collections.singletonList(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")),
            UnaryOperator.identity()
        );
        Assertions.assertSame(dupout.redirectError(), dupout.redirectOutput());
    }

    @Test
    void setupRedirectionsHeredocQuotedAndUnquotedFirst() {
        final ProcessBuilder unquoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            unquoted,
            Collections.singletonList(
                new Redirection(
                    0,
                    RedirectionType.HERE_DOC,
                    new Redirection.HereDoc("EOF", String.format("body%n"), false)
                )
            ),
            text -> text.toUpperCase(Locale.ROOT)
        );
        Assertions.assertEquals(ProcessBuilder.Redirect.Type.READ, unquoted.redirectInput().type());
    }

    @Test
    void setupRedirectionsHeredocQuotedAndUnquotedSecond() {
        final ProcessBuilder unquoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            unquoted,
            Collections.singletonList(
                new Redirection(
                    0,
                    RedirectionType.HERE_DOC,
                    new Redirection.HereDoc("EOF", String.format("body%n"), false)
                )
            ),
            text -> text.toUpperCase(Locale.ROOT)
        );
        unquoted.redirectInput();
        final ProcessBuilder quoted = new ProcessBuilder("x");
        RedirectionHandler.setupRedirections(
            quoted,
            Collections.singletonList(
                new Redirection(
                    0,
                    RedirectionType.HERE_DOC_DASH,
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
