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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: external clobber.
 *
 * @since 1.0
 */
final class ExecutorExternalClobberTest {

    /** HELLO constant. */
    private static final String HELLO = "hello";

    @Test
    void externalClobberRedirectFirst() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)))
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalClobberRedirectSecond() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)));
        Assertions.assertTrue(new File(outpath).exists());
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalClobberRedirectThird() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)));
        new File(outpath).exists();
        Assertions.assertEquals(
            "hello clobber",
            new String(Files.readAllBytes(Paths.get(outpath)), StandardCharsets.UTF_8).trim()
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        final String inpath = "/tmp/sh4j_ext_12_in.txt";
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorExternalClobberTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s 1>&2", inpath)))
        );
        new File(inpath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        final String inpath = "/tmp/sh4j_ext_21_in.txt";
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorExternalClobberTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s 2>&1", inpath)))
        );
        new File(inpath).delete();
    }

    @Test
    void externalCloseStdout() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true 1>&-")));
    }

    @Test
    void externalCloseStderr() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true 2>&-")));
    }

    @Test
    void externalStderrOutputRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_out.txt";
        new File(path).delete();
        new Executor().execute(
            this.parse(String.format("cat /nonexistent_path_xyz_123 2> %s", path))
        );
        Assertions.assertTrue(new File(path).exists());
        new File(path).delete();
    }

    @Test
    void externalStderrAppendRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_append.txt";
        new File(path).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("initial%n"));
        }
        new Executor().execute(
            this.parse(String.format("cat /nonexistent_path_xyz_456 2>> %s", path))
        );
        Assertions.assertTrue(
            new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8).contains(
                "initial"
            )
        );
        new File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorExternalClobberTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <> %s", path)))
        );
        new File(path).delete();
    }

    @Test
    void falseBuiltinReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("false")));
    }

    @Test
    void fcListsHistoryEntriesFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -l 1 3")));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        executor.execute(this.parse("fc -l 1 3"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("1\techo one"));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        executor.execute(this.parse("fc -l 1 3"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("2\techo two"));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesFourth() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        executor.execute(this.parse("fc -l 1 3"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("3\techo three"));
        stream.flush();
    }

    @Test
    void fcListsNoNumbersFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -ln 1 1")));
        stream.flush();
    }

    @Test
    void fcListsNoNumbersSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        executor.execute(this.parse("fc -ln 1 1"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("\techo hello"));
        stream.flush();
    }

    @Test
    void fcListsNoNumbersThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        executor.execute(this.parse("fc -ln 1 1"));
        Assertions.assertFalse(bos.toString(StandardCharsets.UTF_8).contains("1\t"));
        stream.flush();
    }

    @Test
    void fcListsLastEntryFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -l -1 -1")));
        stream.flush();
    }

    @Test
    void fcListsLastEntrySecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        executor.execute(this.parse("fc -l -1 -1"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("echo second"));
        stream.flush();
    }

    @Test
    void fcListsLastEntryThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        executor.execute(this.parse("fc -l -1 -1"));
        Assertions.assertFalse(bos.toString(StandardCharsets.UTF_8).contains("echo first"));
        stream.flush();
    }

    @Test
    void fcEmptyHistoryReturnsError() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("fc -l")));
    }

    @Test
    void fcSubstituteAndReExecuteFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -s hello=world")));
        stream.flush();
    }

    @Test
    void fcSubstituteAndReExecuteSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        executor.execute(this.parse("fc -s hello=world"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("echo world"));
        stream.flush();
    }

    @Test
    void fcSubstituteInvalidPattern() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(1, executor.execute(this.parse("fc -s nosign")));
        stream.flush();
    }

    @Test
    void fcUnknownOptionReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(1, executor.execute(this.parse("fc -z")));
        stream.flush();
    }

    @Test
    void fcSubstituteNonExistentNumber() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(1, executor.execute(this.parse("fc -s hello=world 999")));
        stream.flush();
    }

    @Test
    void fcDefaultRangeListsLastItemFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        for (int idx = 1; idx <= 5; idx = idx + 1) {
            executor.execute(
                this.parse(String.format("echo cmd%d", idx))
            );
        }
        Assertions.assertEquals(0, executor.execute(this.parse("fc -l")));
        stream.flush();
    }

    @Test
    void fcDefaultRangeListsLastItemSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        for (int idx = 1; idx <= 5; idx = idx + 1) {
            executor.execute(
                this.parse(String.format("echo cmd%d", idx))
            );
        }
        executor.execute(this.parse("fc -l"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("echo cmd1"));
        stream.flush();
    }

    @Test
    void fcDefaultRangeListsLastItemThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        for (int idx = 1; idx <= 5; idx = idx + 1) {
            executor.execute(
                this.parse(String.format("echo cmd%d", idx))
            );
        }
        executor.execute(this.parse("fc -l"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("echo cmd5"));
        stream.flush();
    }

    @Test
    void fgBringsJobToForegroundFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & fg")));
        stream.flush();
    }

    @Test
    void fgBringsJobToForegroundSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & fg"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("sleep 0"),
            String.format("expected 'sleep 0' in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void fgWithSpecificJobIdFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & fg %1")));
        stream.flush();
    }

    @Test
    void fgWithSpecificJobIdSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & fg %1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("sleep 0"),
            String.format("expected 'sleep 0' in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void fgNoJobsReturnsError() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("fg")));
    }

    @Test
    void fgUnknownJobReturnsError() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("fg 99999")));
    }

    @Test
    void fgWithInvalidJobSyntaxReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("fg notajob")));
        stream.flush();
    }

    @Test
    void forLoopWithContinueFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done"))
        );
    }

    @Test
    void forLoopWithContinueSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done"));
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void functionDefinitionParsingFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("myfunc() { echo hi; }")));
        stream.flush();
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
