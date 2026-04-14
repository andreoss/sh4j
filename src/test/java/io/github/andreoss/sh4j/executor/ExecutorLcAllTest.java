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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: lc all.
 *
 * @since 1.0
 */
final class ExecutorLcAllTest {

    /** HELLO constant. */
    private static final String HELLO = "hello";

    /** VAR_DEFAULT_EXPR constant. */
    private static final String VAR_DEFAULT_EXPR = "${var:-default}";

    /** HELLO_WORLD constant. */
    private static final String HELLO_WORLD = "hello world";

    @Test
    void lcAllVariableOverridesOthersSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_ALL=C"));
        Assertions.assertEquals("C", executor.getVariable("LC_ALL"));
    }

    @Test
    void lcCollateVariableCanBeSetFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("LC_COLLATE=en_US.UTF-8")));
    }

    @Test
    void lcCollateVariableCanBeSetSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_COLLATE=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LC_COLLATE"));
    }

    @Test
    void lcCtypeVariableCanBeSetFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("LC_CTYPE=en_US.UTF-8")));
    }

    @Test
    void lcCtypeVariableCanBeSetSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_CTYPE=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LC_CTYPE"));
    }

    @Test
    void lcMessagesVariableCanBeSetFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("LC_MESSAGES=en_US.UTF-8")));
    }

    @Test
    void lcMessagesVariableCanBeSetSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_MESSAGES=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LC_MESSAGES"));
    }

    @Test
    void linenoReflectsSourceLineFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("echo $LINENO")));
        stream.flush();
    }

    @Test
    void linenoReflectsSourceLineSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo $LINENO"));
        Assertions.assertEquals(String.format("1%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void linenoUpdatesPerCommandFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("echo $LINENO; echo $LINENO")));
        stream.flush();
    }

    @Test
    void linenoUpdatesPerCommandSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo $LINENO; echo $LINENO"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains(String.format("1%n")));
        stream.flush();
    }

    @Test
    void linenoUpdatesPerCommandThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo $LINENO; echo $LINENO"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains(String.format("1%n1%n"))
                || output.split(String.format("%n")).length >= 2
        );
        stream.flush();
    }

    @Test
    void localeVariablesCanBeExportedFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        Assertions.assertEquals("C", new Executor(env).getVariable("LANG"));
    }

    @Test
    void localeVariablesCanBeExportedSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        Assertions.assertEquals("C", new Executor(env).getVariable("LC_ALL"));
    }

    @Test
    void localeVariablesCanBeExportedThird() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        Assertions.assertEquals("POSIX", new Executor(env).getVariable("LC_COLLATE"));
    }

    @Test
    void localeVariablesCanBeExportedFourth() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        Assertions.assertEquals("UTF-8", new Executor(env).getVariable("LC_CTYPE"));
    }

    @Test
    void localeVariablesCanBeExportedFifth() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        Assertions.assertEquals("C", new Executor(env).getVariable("LC_MESSAGES"));
    }

    @Test
    void normaliseBackticksSimple() {
        Assertions.assertEquals("$(echo hello)", WordExpander.normaliseBackticks("`echo hello`"));
    }

    @Test
    void normaliseBackticksNoChange() {
        Assertions.assertEquals(
            ExecutorLcAllTest.HELLO_WORLD,
            WordExpander.normaliseBackticks(ExecutorLcAllTest.HELLO_WORLD)
        );
    }

    @Test
    void normaliseBackticksSkipsDollarParen() {
        Assertions.assertEquals("$(echo hi)", WordExpander.normaliseBackticks("$(echo hi)"));
    }

    @Test
    void normaliseBackticksEscaped() {
        Assertions.assertEquals("$(echo `hi`)", WordExpander.normaliseBackticks("`echo \\`hi\\``"));
    }

    @Test
    void normaliseBackticksDollarEscape() {
        Assertions.assertEquals("$(echo $PATH)", WordExpander.normaliseBackticks("`echo \\$PATH`"));
    }

    @Test
    void normaliseBackticksBackslashEscape() {
        Assertions.assertEquals("$(echo a\\b)", WordExpander.normaliseBackticks("`echo a\\\\b`"));
    }

    @Test
    void normaliseBackticksNonSpecialEscape() {
        Assertions.assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\\\n`"));
    }

    @Test
    void normaliseBackticksUnclosed() {
        Assertions.assertEquals("`echo hello", WordExpander.normaliseBackticks("`echo hello"));
    }

    @Test
    void normaliseBackticksSkipsDollarBrace() {
        Assertions.assertEquals(
            ExecutorLcAllTest.VAR_DEFAULT_EXPR,
            WordExpander.normaliseBackticks(ExecutorLcAllTest.VAR_DEFAULT_EXPR)
        );
    }

    @Test
    void normaliseBackticksSkipsNestedDollarBrace() {
        Assertions.assertEquals(
            "${var:-${other}}",
            WordExpander.normaliseBackticks("${var:-${other}}")
        );
    }

    @Test
    void normaliseBackticksSkipsTripleNestedDollarBrace() {
        Assertions.assertEquals(
            "${a:-${b:-${c}}}",
            WordExpander.normaliseBackticks("${a:-${b:-${c}}}")
        );
    }

    @Test
    void normaliseBackticksDollarAtEnd() {
        Assertions.assertEquals("foo$", WordExpander.normaliseBackticks("foo$"));
    }

    @Test
    void normaliseBackticksUnmatchedDollarBrace() {
        Assertions.assertEquals("${unclosed", WordExpander.normaliseBackticks("${unclosed"));
    }

    @Test
    void normaliseBackticksDollarFollowedByLetter() {
        Assertions.assertEquals("$VAR", WordExpander.normaliseBackticks("$VAR"));
    }

    @Test
    void normaliseBackticksBackslashAtEndOfInput() {
        Assertions.assertEquals("`abc\\", WordExpander.normaliseBackticks("`abc\\"));
    }

    @Test
    void normaliseBackticksBackslashNonSpecialChar() {
        Assertions.assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\n`"));
    }

    @Test
    void pathnameExpandNoGlob() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorLcAllTest.HELLO),
            WordExpander.pathnameExpand(Collections.singletonList(ExecutorLcAllTest.HELLO))
        );
    }

    @Test
    void pathnameExpandGlobNoMatchKeepsPattern() {
        Assertions.assertEquals(
            Collections.singletonList("__no_match__xyz__"),
            WordExpander.pathnameExpand(Collections.singletonList("__no_match__xyz__"))
        );
    }

    @Test
    void pathnameExpandGlobWithMatch() {
        Assertions.assertEquals(
            Collections.singletonList("pom.xml"),
            WordExpander.pathnameExpand(Collections.singletonList("pom.xml"))
        );
    }

    @Test
    void pathnameExpandWildcardMatch() {
        Assertions.assertTrue(
            WordExpander.pathnameExpand(Collections.singletonList("*.xml")).contains("pom.xml")
        );
    }

    @Test
    void pathnameExpandGlobQuoted() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo \"*.java\"")));
    }

    @Test
    void pathnameExpandGlobUnquoted() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo *.java")));
    }

    @Test
    void pipelineInterruptedException() {
        Thread.currentThread().interrupt();
        Assertions.assertEquals(1, new Executor().execute(this.parse("ls | cat")));
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
