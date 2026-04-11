/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: expand dollar.
 *
 * @since 1.0
 */
final class ExecutorExpandDollarTest {

    /** HELLO constant. */
    private static final String HELLO = "hello";

    /** A_B_C constant. */
    private static final String A_B_C = "a b c";

    /** HOME_KEY constant. */
    private static final String HOME_KEY = "HOME";

    /** PWD_KEY constant. */
    private static final String PWD_KEY = "PWD";

    /** VAR_DEFAULT_EXPR constant. */
    private static final String VAR_DEFAULT_EXPR = "${var:-default}";

    /** VAR_NAME constant. */
    private static final String VAR_NAME = "var";

    /** VALUE constant. */
    private static final String VALUE = "value";

    @Test
    void expandDollarHash() {
        Assertions.assertEquals(
            "3",
            new Executor(new HashMap<>(), Arrays.asList("sh", "a", "b", "c")).expandVariables(
                "$#",
                new HashMap<>()
            )
        );
    }

    @Test
    void expandDollarAt() {
        Assertions.assertEquals(
            ExecutorExpandDollarTest.A_B_C,
            new Executor(new HashMap<>(), Arrays.asList("sh", "a", "b", "c")).expandVariables(
                "$@",
                new HashMap<>()
            )
        );
    }

    @Test
    void expandDollarStar() {
        Assertions.assertEquals(
            ExecutorExpandDollarTest.A_B_C,
            new Executor(new HashMap<>(), Arrays.asList("sh", "a", "b", "c")).expandVariables(
                "$*",
                new HashMap<>()
            )
        );
    }

    @Test
    void expandDollarZero() {
        Assertions.assertEquals(
            "my_script",
            new Executor(new HashMap<>(), Collections.singletonList("my_script")).expandVariables(
                "$0",
                new HashMap<>()
            )
        );
    }

    @Test
    void expandDollarOne() {
        Assertions.assertEquals(
            "first",
            new Executor(new HashMap<>(), Arrays.asList("sh", "first", "second")).expandVariables(
                "$1",
                new HashMap<>()
            )
        );
    }

    @Test
    void expandDollarNine() {
        Assertions.assertEquals(
            "i",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i")
            ).expandVariables("$9", new HashMap<>())
        );
    }

    @Test
    void expandBracedMultiDigit() {
        Assertions.assertEquals(
            "j",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    void expandDollarHashNone() {
        Assertions.assertEquals("0", new Executor().expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAtEmpty() {
        Assertions.assertEquals("", new Executor().expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarDollarFirst() {
        Assertions.assertNotNull(new Executor().expandVariables("$$", new HashMap<>()));
    }

    @Test
    void expandDollarDollarSecond() {
        Assertions.assertFalse(new Executor().expandVariables("$$", new HashMap<>()).isEmpty());
    }

    @Test
    void expandDollarDollarThird() {
        final String pid = new Executor().expandVariables("$$", new HashMap<>());
        final String jvm = ManagementFactory.getRuntimeMXBean().getName();
        Assertions.assertEquals(
            String.valueOf(Long.parseLong(jvm.substring(0, jvm.indexOf('@')))),
            pid
        );
    }

    @Test
    void expandDollarBang() {
        Assertions.assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    void expandDollarBangBraced() {
        Assertions.assertEquals("0", new Executor().expandVariables("${!}", new HashMap<>()));
    }

    @Test
    void expandTildeToHome() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.HOME_KEY, "/home/testuser");
        Assertions.assertEquals("/home/testuser", WordExpander.expandTilde("~", env));
    }

    @Test
    void expandTildeWithPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.HOME_KEY, "/home/testuser");
        Assertions.assertEquals(
            "/home/testuser/file.txt",
            WordExpander.expandTilde("~/file.txt", env)
        );
    }

    @Test
    void expandTildePlus() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.PWD_KEY, "/current/dir");
        Assertions.assertEquals("/current/dir", WordExpander.expandTilde("~+", env));
    }

    @Test
    void expandTildeNone() {
        Assertions.assertEquals("/foo/bar", WordExpander.expandTilde("/foo/bar", new HashMap<>()));
    }

    @Test
    void expandVarDefault() {
        Assertions.assertEquals(
            "default",
            new Executor().expandVariables(ExecutorExpandDollarTest.VAR_DEFAULT_EXPR, new HashMap<>())
        );
    }

    @Test
    void expandVarDefaultSet() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "actual");
        Assertions.assertEquals(
            "actual",
            new Executor().expandVariables(ExecutorExpandDollarTest.VAR_DEFAULT_EXPR, env)
        );
    }

    @Test
    void expandVarDefaultEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "");
        Assertions.assertEquals(
            "default",
            new Executor().expandVariables(ExecutorExpandDollarTest.VAR_DEFAULT_EXPR, env)
        );
    }

    @Test
    void expandVarDefaultNullOnly() {
        Assertions.assertEquals(
            "default",
            new Executor().expandVariables("${var-default}", new HashMap<>())
        );
    }

    @Test
    void expandVarDefaultNullOnlySetSkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, ExecutorExpandDollarTest.VALUE);
        Assertions.assertEquals(
            ExecutorExpandDollarTest.VALUE,
            new Executor().expandVariables("${var-default}", env)
        );
    }

    @Test
    void expandVarDefaultNullOnlyEmptySkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "");
        Assertions.assertEquals("", new Executor().expandVariables("${var-default}", env));
    }

    @Test
    void expandVarAssignDefaultFirst() {
        Assertions.assertEquals(
            "assigned",
            new Executor().expandVariables("${var:=assigned}", new HashMap<>())
        );
    }

    @Test
    void expandVarAssignDefaultSecond() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("${var:=assigned}", env);
        Assertions.assertEquals("assigned", env.get(ExecutorExpandDollarTest.VAR_NAME));
    }

    @Test
    void expandVarAlternate() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "x");
        Assertions.assertEquals("alt", new Executor().expandVariables("${var:+alt}", env));
    }

    @Test
    void expandVarAlternateUnset() {
        Assertions.assertEquals("", new Executor().expandVariables("${var:+alt}", new HashMap<>()));
    }

    @Test
    void expandVarAssignNullOnlyFirst() {
        Assertions.assertEquals(
            "assigned",
            new Executor().expandVariables("${var=assigned}", new HashMap<>())
        );
    }

    @Test
    void expandVarAssignNullOnlySecond() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("${var=assigned}", env);
        Assertions.assertEquals("assigned", env.get(ExecutorExpandDollarTest.VAR_NAME));
    }

    @Test
    void expandVarAssignNullOnlySetSkipsFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "existing");
        Assertions.assertEquals("existing", new Executor().expandVariables("${var=assigned}", env));
    }

    @Test
    void expandVarAssignNullOnlySetSkipsSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "existing");
        new Executor().expandVariables("${var=assigned}", env);
        Assertions.assertEquals("existing", env.get(ExecutorExpandDollarTest.VAR_NAME));
    }

    @Test
    void expandVarAlternateNullOnly() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "x");
        Assertions.assertEquals("alt", new Executor().expandVariables("${var+alt}", env));
    }

    @Test
    void expandVarAlternateNullOnlyUnset() {
        Assertions.assertEquals("", new Executor().expandVariables("${var+alt}", new HashMap<>()));
    }

    @Test
    void expandVarLength() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, ExecutorExpandDollarTest.HELLO);
        Assertions.assertEquals("5", new Executor().expandVariables("${#var}", env));
    }

    @Test
    void expandVarLengthUnset() {
        Assertions.assertEquals("0", new Executor().expandVariables("${#var}", new HashMap<>()));
    }

    @Test
    void expandVarSuffixShort() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "file.txt");
        Assertions.assertEquals("file", new Executor().expandVariables("${var%.*}", env));
    }

    @Test
    void expandVarSuffixLong() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "a.b.c.txt");
        Assertions.assertEquals("a", new Executor().expandVariables("${var%%.*}", env));
    }

    @Test
    void expandVarPrefixShort() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "foo/bar/baz");
        Assertions.assertEquals("bar/baz", new Executor().expandVariables("${var#*/}", env));
    }

    @Test
    void expandVarPrefixLong() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorExpandDollarTest.VAR_NAME, "foo/bar/baz");
        Assertions.assertEquals("baz", new Executor().expandVariables("${var##*/}", env));
    }
}
