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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: tilde.
 *
 * @since 1.0
 */
final class ExecutorTildeTest {

    /** VALUE constant. */
    private static final String VALUE = "value";

    @Test
    void tildeUserNameFallthrough() {
        Assertions.assertEquals(
            "~nonexistentuser123",
            new Executor().expandVariables("~nonexistentuser123", new HashMap<>())
        );
    }

    @Test
    void trueBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true")));
    }

    @Test
    void typeForExternalNonBuiltinCommandFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type ls")));
    }

    @Test
    void typeForExternalNonBuiltinCommandSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type ls"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("ls is"));
    }

    @Test
    void typeForExternalCommandFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type true")));
    }

    @Test
    void typeForExternalCommandSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type true"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is"));
    }

    @Test
    void typeBuiltinFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("type echo cd exit export type true false shift unset"))
        );
    }

    @Test
    void typeBuiltinSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("echo is a shell built-in")
        );
    }

    @Test
    void typeBuiltinThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("cd is a shell built-in")
        );
    }

    @Test
    void typeBuiltinFourth() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("exit is a shell built-in")
        );
    }

    @Test
    void typeBuiltinFifth() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("export is a shell built-in")
        );
    }

    @Test
    void typeBuiltinSixth() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("type is a shell built-in")
        );
    }

    @Test
    void typeBuiltinSeventh() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("true is a shell built-in")
        );
    }

    @Test
    void typeBuiltinEighth() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("false is a shell built-in")
        );
    }

    @Test
    void typeExternalFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type true")));
    }

    @Test
    void typeExternalSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type true"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is "));
    }

    @Test
    void typeNotFound() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("type nonexistent_cmd_xyz")));
    }

    @Test
    void typeFunctionFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("myfunc() { true; }; type myfunc")));
    }

    @Test
    void typeFunctionSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("myfunc() { true; }; type myfunc"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("myfunc is a function")
        );
    }

    @Test
    void typeBuiltinsBreakContinueEvalFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("type break continue eval")));
        stream.flush();
    }

    @Test
    void typeBuiltinsBreakContinueEvalSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("type break continue eval"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("break is a shell built-in")
        );
        stream.flush();
    }

    @Test
    void typeBuiltinsBreakContinueEvalThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("type break continue eval"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("continue is a shell built-in")
        );
        stream.flush();
    }

    @Test
    void typeBuiltinsBreakContinueEvalFourth() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("type break continue eval"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("eval is a shell built-in")
        );
        stream.flush();
    }

    @Test
    void umaskNoArgsPrintsDefaultFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("umask")));
        stream.flush();
    }

    @Test
    void umaskNoArgsPrintsDefaultSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask"));
        Assertions.assertEquals("0022", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void umaskSetThenReadEchoesValueFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("umask 077")));
        stream.flush();
    }

    @Test
    void umaskSetThenReadEchoesValueSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask 077"));
        Assertions.assertEquals(0, executor.execute(this.parse("umask")));
        stream.flush();
    }

    @Test
    void umaskSetThenReadEchoesValueThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask 077"));
        executor.execute(this.parse("umask"));
        Assertions.assertEquals("0077", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void umaskDashSPrintsSymbolicFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("umask -S")));
        stream.flush();
    }

    @Test
    void umaskDashSPrintsSymbolicSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask -S"));
        Assertions.assertEquals("u=rwx,g=rx,o=rx", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void umaskNonOctalReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("umask zzz")));
    }

    @Test
    void unaliasRemovesOneFirst() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        Assertions.assertEquals(0, executor.execute(this.parse("unalias ll")));
    }

    @Test
    void unaliasRemovesOneSecond() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        executor.execute(this.parse("unalias ll"));
        Assertions.assertFalse(executor.aliases().containsKey("ll"));
    }

    @Test
    void unaliasRemovesOneThird() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        executor.execute(this.parse("unalias ll"));
        executor.aliases();
        Assertions.assertTrue(executor.aliases().containsKey("gg"));
    }

    @Test
    void unaliasAllRemovesAllFirst() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        Assertions.assertEquals(0, executor.execute(this.parse("unalias -a")));
    }

    @Test
    void unaliasAllRemovesAllSecond() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        executor.execute(this.parse("unalias -a"));
        Assertions.assertTrue(executor.aliases().isEmpty());
    }

    @Test
    void unaliasUnknownReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("unalias nonexistent")));
    }

    @Test
    void unaliasUnknownNameReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("unalias unknown_alias")));
    }

    @Test
    void unsetRemovesVariableFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", ExecutorTildeTest.VALUE);
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("unset MYVAR")));
    }

    @Test
    void unsetRemovesVariableSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", ExecutorTildeTest.VALUE);
        final Executor executor = new Executor(env);
        executor.execute(this.parse("unset MYVAR"));
        Assertions.assertNull(executor.getVariable("MYVAR"));
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
