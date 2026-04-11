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
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: execute if.
 *
 * @since 1.0
 */
final class ExecutorExecuteIfTest {

    /** TRUE_CMD constant. */
    private static final String TRUE_CMD = "true";

    /** FALSE_CMD constant. */
    private static final String FALSE_CMD = "false";

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    @Test
    void executeIfTrueThen() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("if true; then echo hi; fi")));
    }

    @Test
    void executeIfFalseElse() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("if false; then echo a; else echo b; fi"))
        );
    }

    @Test
    void executeIfTrueThenElseNotExecuted() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("if true; then echo hi; else false; fi"))
        );
    }

    @Test
    void executeForDoDone() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("for var in a b c; do echo $var; done"))
        );
    }

    @Test
    void executeWhileFalse() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("while false; do echo hi; done"))
        );
    }

    @Test
    void executeUntilTrue() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("until true; do echo hi; done"))
        );
    }

    @Test
    void executeCaseMatch() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in a) echo match;; esac"))
        );
    }

    @Test
    void executeCaseNoMatch() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in b) echo nomatch;; esac"))
        );
    }

    @Test
    void executeCaseDefaultStar() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in *) echo default;; esac"))
        );
    }

    @Test
    void executeCasePipePattern() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in b|c|a) echo match;; esac"))
        );
    }

    @Test
    void executeCaseFirstMatchOnly() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in a) echo first;; b) echo second;; esac"))
        );
    }

    @Test
    void executeFunctionDefinitionStored() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("foo() { echo hello; }")));
    }

    @Test
    void executeFunctionCall() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("foo() { echo hello; }; foo"))
        );
    }

    @Test
    void executeFunctionSubshellBody() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("foo() (echo hello); foo")));
    }

    @Test
    void executeWhileLoop() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("while false; do echo body; done"))
        );
    }

    @Test
    void executeUntilLoop() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("until true; do echo body; done"))
        );
    }

    @Test
    void executeWhileBodyRunsOnceFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            1,
            executor.execute(this.parse("i=; while [ -z \"$i\" ]; do echo ran; i=1; false; done"))
        );
        stream.flush();
    }

    @Test
    void executeWhileBodyRunsOnceSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("i=; while [ -z \"$i\" ]; do echo ran; i=1; false; done"));
        Assertions.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeUntilBodyRunsOnceFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("i=; until [ -n \"$i\" ]; do echo ran; i=1; true; done"))
        );
        stream.flush();
    }

    @Test
    void executeUntilBodyRunsOnceSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("i=; until [ -n \"$i\" ]; do echo ran; i=1; true; done"));
        Assertions.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        Assertions.assertEquals(
            42,
            new Executor().execute(this.parse("i=; while [ -z \"$i\" ]; do i=1; (exit 42); done"))
        );
    }

    @Test
    void executeIfFalseNoElse() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("if false; then echo yes; fi"))
        );
    }

    @Test
    void executeElifWhenIfFalseFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("if false; then echo no; elif true; then echo yes; fi"))
        );
        stream.flush();
    }

    @Test
    void executeElifWhenIfFalseSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("if false; then echo no; elif true; then echo yes; fi"));
        Assertions.assertEquals(String.format("yes%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeElifChainFalseFalseFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse("if false; then echo a; elif false; then echo b; else echo c; fi")
            )
        );
        stream.flush();
    }

    @Test
    void executeElifChainFalseFalseSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(
            this.parse("if false; then echo a; elif false; then echo b; else echo c; fi")
        );
        Assertions.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeElifChainSecondElifTrueFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse(
                    "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
                )
            )
        );
        stream.flush();
    }

    @Test
    void executeElifChainSecondElifTrueSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(
            this.parse(
                "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
            )
        );
        Assertions.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeRedirectStdoutToStderr() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo test 1>&2")));
    }

    @Test
    void executeHereDoc() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat << EOF%nhello%nEOF%n")))
        );
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        final File file = new File("/tmp/sh4j_nonexec_test.sh");
        file.createNewFile();
        file.setExecutable(false);
        Assertions.assertEquals(126, new Executor().execute(this.parse(file.getAbsolutePath())));
    }

    @Test
    void executorDefaultsPathWhenMissingFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/tmp");
        Assertions.assertNotNull(new Executor(env).getVariable("PATH"));
    }

    @Test
    void executorDefaultsPathWhenMissingSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/tmp");
        Assertions.assertFalse(new Executor(env).getVariable("PATH").isEmpty());
    }

    @Test
    void exitWithValidIntegerAndWordExpansionError() {
        Assertions.assertEquals(
            1,
            new Executor(new HashMap<>()).execute(this.parse("exit 5${UNSET_FOR_EXIT_999:?msg}"))
        );
    }

    @Test
    void exitWithWordExpansionError() {
        Assertions.assertEquals(
            1,
            new Executor(new HashMap<>()).execute(
                this.parse("exit ${UNSET_FOR_EXIT_8x9:?errmsg}")
            )
        );
    }

    @Test
    void expandDollarQuestion() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorExecuteIfTest.FALSE_CMD));
        Assertions.assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionAfterSuccess() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorExecuteIfTest.TRUE_CMD));
        Assertions.assertEquals("0", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarHyphen() {
        Assertions.assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionBraced() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorExecuteIfTest.FALSE_CMD));
        Assertions.assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionInString() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorExecuteIfTest.FALSE_CMD));
        Assertions.assertEquals("exit:1", executor.expandVariables("exit:$?", new HashMap<>()));
    }
}
