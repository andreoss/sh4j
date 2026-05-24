/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: remove quotes.
 *
 * @since 1.0
 */
final class ExecutorRemoveQuotesTest {

    /** PATH_KEY constant. */
    private static final String PATH_KEY = "PATH";

    /** SCRIPT_KEY constant. */
    private static final String SCRIPT_KEY = "script";

    /** IFS_TAB_NL constant. */
    private static final String IFS_TAB_NL = " \t%n";

    /** HELLO constant. */
    private static final String HELLO = "hello";

    /** A_B_C constant. */
    private static final String A_B_C = "a b c";

    /** HELLO_WORLD constant. */
    private static final String HELLO_WORLD = "hello world";

    @Test
    void removeQuotesPartialDouble() {
        Assertions.assertEquals("foovaluebar", WordExpander.removeQuotes("foo\"value\"bar"));
    }

    @Test
    void removeQuotesPartialSingle() {
        Assertions.assertEquals("helloworld", WordExpander.removeQuotes("hello'world'"));
    }

    @Test
    void removeQuotesApostropheInsideDouble() {
        Assertions.assertEquals("it's", WordExpander.removeQuotes("\"it's\""));
    }

    @Test
    void removeQuotesNestedQuotes() {
        Assertions.assertEquals(
            "hello \"there\" world",
            WordExpander.removeQuotes("\"hello \\\"there\\\" world\"")
        );
    }

    @Test
    void removeQuotesBackslashOutside() {
        Assertions.assertEquals(
            ExecutorRemoveQuotesTest.HELLO_WORLD,
            WordExpander.removeQuotes("hello\\ world")
        );
    }

    @Test
    void removeQuotesBackslashInDouble() {
        Assertions.assertEquals("hello\"world", WordExpander.removeQuotes("\"hello\\\"world\""));
    }

    @Test
    void removeQuotesEmptyQuotesFirst() {
        Assertions.assertEquals("", WordExpander.removeQuotes("\"\""));
    }

    @Test
    void removeQuotesEmptyQuotesSecond() {
        WordExpander.removeQuotes("\"\"");
        Assertions.assertEquals("", WordExpander.removeQuotes("''"));
    }

    @Test
    void resolveCommandWithCustomPathDirInPath() throws Exception {
        final File dir = Files.createTempDirectory("sh4j_custompath_").toFile();
        dir.deleteOnExit();
        final File bin = new File(dir, "myhello");
        try (
            BufferedWriter w = Files.newBufferedWriter(bin.toPath(), StandardCharsets.UTF_8)
        ) {
            w.write(String.format("#!/bin/sh%necho ok%n"));
        }
        bin.setExecutable(true);
        bin.deleteOnExit();
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorRemoveQuotesTest.PATH_KEY, dir.getAbsolutePath());
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("myhello")));
    }

    @Test
    void resolveCommandPathNullReturnsNull() {
        final Map<String, String> empty = new HashMap<>();
        empty.put(ExecutorRemoveQuotesTest.PATH_KEY, "");
        final Executor executor = new Executor(empty);
        executor.expandVariables("", empty);
        Assertions.assertEquals(127, executor.execute(this.parse("nonexistent_xyz_zzz_cmd_abc")));
    }

    @Test
    void resolveCommandWithEmptyDirInPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorRemoveQuotesTest.PATH_KEY, ":/usr/bin");
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("true")));
    }

    @Test
    void shiftLargerThanPositionalCount() {
        Assertions.assertEquals(
            2,
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorRemoveQuotesTest.SCRIPT_KEY, "a", "b")
            ).execute(this.parse("shift 100"))
        );
    }

    @Test
    void shiftRemovesPositionalParamFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("shift; echo $#")));
    }

    @Test
    void shiftRemovesPositionalParamSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        executor.execute(this.parse("shift; echo $#"));
        Assertions.assertEquals("2", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void shiftWithCountFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("shift 2; echo $#")));
    }

    @Test
    void shiftWithCountSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        executor.execute(this.parse("shift 2; echo $#"));
        Assertions.assertEquals("1", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void shiftNonNumericReturnsTwo() {
        Assertions.assertEquals(2, new Executor().execute(this.parse("shift abc")));
    }

    @Test
    void shiftNegativeReturnsTwo() {
        Assertions.assertEquals(2, new Executor().execute(this.parse("shift -1")));
    }

    @Test
    void specialParamShellFlags() {
        Assertions.assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    void specialParamBackgroundPidBeforeAnyJob() {
        Assertions.assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    void specialParamAtStarWithPositionalsFirst() {
        Assertions.assertEquals(
            ExecutorRemoveQuotesTest.A_B_C,
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorRemoveQuotesTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    void specialParamAtStarWithPositionalsSecond() {
        final Executor executor = new Executor(
            new HashMap<>(),
            Arrays.asList(ExecutorRemoveQuotesTest.SCRIPT_KEY, "a", "b", "c")
        );
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$@", env);
        Assertions.assertEquals(
            ExecutorRemoveQuotesTest.A_B_C,
            executor.expandVariables("$*", env)
        );
    }

    @Test
    void specialParamAtStarEmpty() {
        Assertions.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                Collections.singletonList(ExecutorRemoveQuotesTest.SCRIPT_KEY)
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    void splitFieldsDefaultIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields(
                ExecutorRemoveQuotesTest.A_B_C,
                String.format(ExecutorRemoveQuotesTest.IFS_TAB_NL)
            )
        );
    }

    @Test
    void splitFieldsMultipleWs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a   b", String.format(ExecutorRemoveQuotesTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsLeadingWs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("  a b", String.format(ExecutorRemoveQuotesTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsTrailingWs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a b  ", String.format(ExecutorRemoveQuotesTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsColon() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a:b:c", ":")
        );
    }

    @Test
    void splitFieldsEmptyBetweenColons() {
        Assertions.assertEquals(Arrays.asList("a", "", "b"), WordExpander.splitFields("a::b", ":"));
    }

    @Test
    void splitFieldsOnlyColons() {
        Assertions.assertEquals(Collections.singletonList(""), WordExpander.splitFields(":", ":"));
    }

    @Test
    void splitFieldsDoubleColon() {
        Assertions.assertEquals(Arrays.asList("", ""), WordExpander.splitFields("::", ":"));
    }

    @Test
    void splitFieldsNoDelim() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorRemoveQuotesTest.HELLO),
            WordExpander.splitFields(ExecutorRemoveQuotesTest.HELLO, ":")
        );
    }

    @Test
    void splitFieldsEmptyIfs() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorRemoveQuotesTest.HELLO_WORLD),
            WordExpander.splitFields(ExecutorRemoveQuotesTest.HELLO_WORLD, "")
        );
    }

    @Test
    void splitFieldsNullIfs() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorRemoveQuotesTest.HELLO),
            WordExpander.splitFields(ExecutorRemoveQuotesTest.HELLO, null)
        );
    }

    @Test
    void splitFieldsEmptyValue() {
        Assertions.assertEquals(
            Arrays.asList(),
            WordExpander.splitFields("", String.format(ExecutorRemoveQuotesTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsMixedWsNws() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a :b", " :")
        );
    }

    @Test
    void splitFieldsTrailingColon() {
        Assertions.assertEquals(
            Collections.singletonList("a"),
            WordExpander.splitFields("a:", ":")
        );
    }

    @Test
    void splitFieldsCustomNws() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a,b,c", ",")
        );
    }

    @Test
    void splitFieldsTabIfs() {
        Assertions.assertEquals(Arrays.asList("a", "b"), WordExpander.splitFields("a\tb", "\t"));
    }

    @Test
    void splitFieldsMixedIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a:b c", ": ")
        );
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
