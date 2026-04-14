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
 * Tests for {@link Executor}: alias.
 *
 * @since 1.0
 */
final class ExecutorAliasTest {

    @Test
    void aliasExpandsToBuiltinFirst() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hello");
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("ll")));
        stream.flush();
    }

    @Test
    void aliasExpandsToBuiltinSecond() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("ll"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasExpandsWithArgsFirst() {
        final Executor executor = new Executor();
        executor.aliases().put("greet", "echo hello");
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("greet world")));
        stream.flush();
    }

    @Test
    void aliasExpandsWithArgsSecond() {
        final Executor executor = new Executor();
        executor.aliases().put("greet", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("greet world"));
        Assertions.assertEquals(
            String.format("hello world%n"),
            bos.toString(StandardCharsets.UTF_8)
        );
        stream.flush();
    }

    @Test
    void aliasDoesNotAffectOtherCommandsFirst() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hi");
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("echo hello")));
        stream.flush();
    }

    @Test
    void aliasDoesNotAffectOtherCommandsSecond() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hi");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetAndExpandFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "echo hello");
        Assertions.assertEquals(0, executor.execute(this.parse("ll")));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetAndExpandSecond() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "echo hello");
        executor.execute(this.parse("ll"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetViaEqFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("alias ll='echo hello'")));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetViaEqSecond() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias ll='echo hello'"));
        Assertions.assertEquals(0, executor.execute(this.parse("ll")));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetViaEqThird() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias ll='echo hello'"));
        executor.execute(this.parse("ll"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintAllFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        Assertions.assertEquals(0, executor.execute(this.parse("alias")));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintAllSecond() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        executor.execute(this.parse("alias"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("ll='ls -l'"));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintOneFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        Assertions.assertEquals(0, executor.execute(this.parse("alias ll")));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintOneSecond() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        executor.execute(this.parse("alias ll"));
        Assertions.assertEquals(
            String.format("ll='ls -l'%n"),
            bos.toString(StandardCharsets.UTF_8)
        );
        stream.flush();
    }

    @Test
    void aliasBuiltinUndefinedReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("alias nonexistent")));
    }

    @Test
    void aliasWithDoubleQuotedValueFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("alias ll=\"ls -l\"")));
        stream.flush();
    }

    @Test
    void aliasWithDoubleQuotedValueSecond() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias ll=\"ls -l\""));
        Assertions.assertEquals("ls -l", executor.aliases().get("ll"));
        stream.flush();
    }

    @Test
    void aliasShortValueNoUnquoteFirst() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("alias x='a'")));
        stream.flush();
    }

    @Test
    void aliasShortValueNoUnquoteSecond() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias x='a'"));
        Assertions.assertEquals("a", executor.aliases().get("x"));
        stream.flush();
    }

    @Test
    void arithmeticSimple() {
        Assertions.assertEquals("7", new Executor().expandVariables("$((3 + 4))", new HashMap<>()));
    }

    @Test
    void arithmeticMul() {
        Assertions.assertEquals(
            "15",
            new Executor().expandVariables("$((3 * 5))", new HashMap<>())
        );
    }

    @Test
    void arithmeticVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("15", new Executor().expandVariables("$((x + 5))", env));
    }

    @Test
    void arithmeticNested() {
        Assertions.assertEquals(
            "14",
            new Executor().expandVariables("$((2 * (3 + 4)))", new HashMap<>())
        );
    }

    @Test
    void arithmeticShiftLeft() {
        Assertions.assertEquals(
            "32",
            new Executor().expandVariables("$((4 << 3))", new HashMap<>())
        );
    }

    @Test
    void arithmeticShiftRight() {
        Assertions.assertEquals(
            "4",
            new Executor().expandVariables("$((32 >> 3))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLessThanFirst() {
        Assertions.assertEquals("1", new Executor().expandVariables("$((3 < 5))", new HashMap<>()));
    }

    @Test
    void arithmeticLessThanSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((3 < 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((5 < 3))", env));
    }

    @Test
    void arithmeticLessEqualFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((5 <= 5))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLessEqualSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((5 <= 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((6 <= 5))", env));
    }

    @Test
    void arithmeticGreaterThanFirst() {
        Assertions.assertEquals("1", new Executor().expandVariables("$((5 > 3))", new HashMap<>()));
    }

    @Test
    void arithmeticGreaterThanSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((5 > 3))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((3 > 5))", env));
    }

    @Test
    void arithmeticGreaterEqualFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((5 >= 5))", new HashMap<>())
        );
    }

    @Test
    void arithmeticGreaterEqualSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((5 >= 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((4 >= 5))", env));
    }

    @Test
    void arithmeticEqualEqualFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((7 == 7))", new HashMap<>())
        );
    }

    @Test
    void arithmeticEqualEqualSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((7 == 7))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((7 == 8))", env));
    }

    @Test
    void arithmeticNotEqualFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((7 != 8))", new HashMap<>())
        );
    }

    @Test
    void arithmeticNotEqualSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((7 != 8))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((7 != 7))", env));
    }

    @Test
    void arithmeticBitAnd() {
        Assertions.assertEquals(
            "4",
            new Executor().expandVariables("$((12 & 6))", new HashMap<>())
        );
    }

    @Test
    void arithmeticBitXor() {
        Assertions.assertEquals(
            "10",
            new Executor().expandVariables("$((12 ^ 6))", new HashMap<>())
        );
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
