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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: function definition.
 *
 * @since 1.0
 */
final class ExecutorFunctionDefinitionTest {

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    @Test
    void functionDefinitionParsingSecond() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("myfunc() { echo hi; }"));
        Assertions.assertTrue(executor.functionsMap().containsKey("myfunc"));
        stream.flush();
    }

    @Test
    void getoptsParsesSimpleFlagFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            0,
            new Executor(env, Arrays.asList("sh", "-a", "file")).execute(
                this.parse("getopts ab: opt")
            )
        );
    }

    @Test
    void getoptsParsesSimpleFlagSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesSimpleFlagThird() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("", executor.getVariable("OPTARG"));
    }

    @Test
    void getoptsParsesFlagWithArgFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            0,
            new Executor(env, Arrays.asList("sh", "-b", "val", "file")).execute(
                this.parse("getopts ab: opt")
            )
        );
    }

    @Test
    void getoptsParsesFlagWithArgSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("b", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesFlagWithArgThird() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("val", executor.getVariable("OPTARG"));
    }

    @Test
    void getoptsReturnsOneWhenDone() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            1,
            new Executor(env, Arrays.asList("sh", "file")).execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsParsesMultipleFlagsFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            0,
            new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file")).execute(
                this.parse("getopts ab: opt")
            )
        );
    }

    @Test
    void getoptsParsesMultipleFlagsSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesMultipleFlagsThird() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals(0, executor.execute(this.parse("getopts ab: opt")));
    }

    @Test
    void getoptsParsesMultipleFlagsFourth() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("b", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesMultipleFlagsFifth() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("val", executor.getVariable("OPTARG"));
    }

    @Test
    void getoptsParsesMultipleFlagsSixth() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals(1, executor.execute(this.parse("getopts ab: opt")));
    }

    @Test
    void getoptsNoArgsReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("getopts")));
    }

    @Test
    void getoptsWithDoubleDashFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            1,
            new Executor(env, Arrays.asList("sh", "--", "file")).execute(
                this.parse("getopts ab opt")
            )
        );
    }

    @Test
    void getoptsWithDoubleDashSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "--", "file"));
        executor.execute(this.parse("getopts ab opt"));
        Assertions.assertEquals("?", executor.getVariable("opt"));
    }

    @Test
    void getoptsIllegalOptionFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-c", "file"));
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("getopts ab opt")));
        stream.flush();
    }

    @Test
    void getoptsIllegalOptionSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-c", "file"));
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        executor.execute(this.parse("getopts ab opt"));
        Assertions.assertEquals("?", executor.getVariable("opt"));
        stream.flush();
    }

    @Test
    void getoptsMissingArgWithColonFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            0,
            new Executor(env, Arrays.asList("sh", "-b")).execute(this.parse("getopts :ab: opt"))
        );
    }

    @Test
    void getoptsMissingArgWithColonSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        executor.execute(this.parse("getopts :ab: opt"));
        Assertions.assertEquals(":", executor.getVariable("opt"));
    }

    @Test
    void getoptsMissingArgWithoutColonFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("getopts ab: opt")));
        stream.flush();
    }

    @Test
    void getoptsMissingArgWithoutColonSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("?", executor.getVariable("opt"));
        stream.flush();
    }

    @Test
    void getoptsWithExplicitArgsFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            0,
            new Executor(env).execute(this.parse("getopts ab opt -a -b val"))
        );
    }

    @Test
    void getoptsWithExplicitArgsSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env);
        executor.execute(this.parse("getopts ab opt -a -b val"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsInvalidOptindFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "notanumber");
        Assertions.assertEquals(
            0,
            new Executor(env, Arrays.asList("sh", "-a")).execute(this.parse("getopts a opt"))
        );
    }

    @Test
    void getoptsInvalidOptindSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "notanumber");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        executor.execute(this.parse("getopts a opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsNegativeOptindFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "-5");
        Assertions.assertEquals(
            0,
            new Executor(env, Arrays.asList("sh", "-a")).execute(this.parse("getopts a opt"))
        );
    }

    @Test
    void getoptsNegativeOptindSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "-5");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        executor.execute(this.parse("getopts a opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void globMatchesQuestionMarkInCaseFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesQuestionMarkInCaseSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesQuestionMarkNoMatchFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesQuestionMarkNoMatchSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("NO".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesCharClassInCaseFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesCharClassInCaseSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesCharClassNoMatchFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesCharClassNoMatchSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("NO".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesCharClassRangeInCaseFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesCharClassRangeInCaseSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesUnclosedBracketInCaseFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac")
            )
        );
    }
}
