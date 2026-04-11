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
 * Tests for {@link Executor}: glob matches.
 *
 * @since 1.0
 */
final class ExecutorGlobMatchesTest {

    /** HELLO constant. */
    private static final String HELLO = "hello";

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    @Test
    void globMatchesUnclosedBracketInCaseSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void hashNoArgsReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("hash")));
    }

    @Test
    void hashDashRReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("hash -r")));
    }

    @Test
    void hashWithKnownBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("hash echo")));
    }

    @Test
    void hashWithUnknownNameReturnsError() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("hash sh4j_no_such_cmd_xyz"))
        );
    }

    @Test
    void heredocExpansionFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n")))
        );
    }

    @Test
    void heredocExpansionSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n")));
        Assertions.assertEquals("hello expanded", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsBasicFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n")))
        );
    }

    @Test
    void heredocStripTabsBasicSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n")));
        Assertions.assertEquals(ExecutorGlobMatchesTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMultipleFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n")))
        );
    }

    @Test
    void heredocStripTabsMultipleSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n")));
        Assertions.assertEquals(ExecutorGlobMatchesTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabsFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n")))
        );
    }

    @Test
    void heredocStripTabsDelimiterWithTabsSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n")));
        Assertions.assertEquals(ExecutorGlobMatchesTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMixedFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))
            )
        );
    }

    @Test
    void heredocStripTabsMixedSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(
            this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))
        );
        Assertions.assertEquals(
            String.format("first%nsecond%nthird"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    void isQuotedDoubleQuotes() {
        Assertions.assertTrue(WordExpander.isQuoted("\"hello\""));
    }

    @Test
    void isQuotedSingleQuotes() {
        Assertions.assertTrue(WordExpander.isQuoted("'hello'"));
    }

    @Test
    void isQuotedNoQuotes() {
        Assertions.assertFalse(WordExpander.isQuoted(ExecutorGlobMatchesTest.HELLO));
    }

    @Test
    void isQuotedPartial() {
        Assertions.assertFalse(WordExpander.isQuoted("a\"b\"c"));
    }

    @Test
    void jobsListsBackgroundJobsFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs")));
        stream.flush();
    }

    @Test
    void jobsListsBackgroundJobsSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("[1]"),
            String.format("expected [1] in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void jobsListsBackgroundJobsThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("sleep 0"),
            String.format("expected 'sleep 0' in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void jobsWithPercentIdFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs %1")));
        stream.flush();
    }

    @Test
    void jobsWithPercentIdSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs %1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("[1]"),
            String.format("expected [1] in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void jobsEmptyWhenNoBackgroundJobsFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("jobs")));
        stream.flush();
    }

    @Test
    void jobsEmptyWhenNoBackgroundJobsSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("jobs"));
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void jobsWithNumericIdFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs 1")));
        stream.flush();
    }

    @Test
    void jobsWithNumericIdSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs 1"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("[1]"),
            String.format("output was: %s", bos)
        );
        stream.flush();
    }

    @Test
    void jobsWithNonExistentJobId() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs 99999")));
        stream.flush();
    }

    @Test
    void jobsWithInvalidJobIdReturnsZeroFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs notanumber")));
        stream.flush();
    }

    @Test
    void jobsWithInvalidJobIdReturnsZeroSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs notanumber"));
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void jobsShowsDoneForCompletedJob() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.jobManager().waitForJob(executor.jobManager().submit(() -> { }, "test-cmd"));
        executor.execute(this.parse("jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("Done"),
            String.format("expected Done state in jobs output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void killDashLListsSignalsFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("kill -l")));
        stream.flush();
    }

    @Test
    void killDashLListsSignalsSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("kill -l"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("TERM"));
        stream.flush();
    }

    @Test
    void killNoArgsReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("kill")));
    }

    @Test
    void killUnknownPidReturnsError() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("kill 999999999")));
    }

    @Test
    void killDashSWithNonNumericPidReturnsError() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("kill -s TERM notapid")));
    }

    @Test
    void langVariableCanBeSetFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("LANG=en_US.UTF-8")));
    }

    @Test
    void langVariableCanBeSetSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LANG=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LANG"));
    }

    @Test
    void lcAllVariableOverridesOthersFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("LC_ALL=C")));
    }
}
