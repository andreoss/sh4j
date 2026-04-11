/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Arrays;
import java.util.HashMap;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for BuiltinDispatcher.
 *
 * @since 1.0
 */
final class BuiltinDispatcherTest {

    @Test
    void registryHasTrue() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("true"), CoreMatchers.is(true));
    }

    @Test
    void registryHasFalse() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("false"), CoreMatchers.is(true));
    }

    @Test
    void registryHasColon() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin(":"), CoreMatchers.is(true));
    }

    @Test
    void registryRejectsUnknown() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("nope"), CoreMatchers.is(false));
    }

    @Test
    void dispatchTrueReturnsZero() {
        MatcherAssert.assertThat(
            new BuiltinDispatcher().dispatch(
                "true",
                null,
                new Invocation(Arrays.asList(), new HashMap<>(), Arrays.asList())
            ),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void dispatchFalseReturnsOne() {
        MatcherAssert.assertThat(
            new BuiltinDispatcher().dispatch(
                "false",
                null,
                new Invocation(Arrays.asList(), new HashMap<>(), Arrays.asList())
            ),
            CoreMatchers.equalTo(1)
        );
    }

    @Test
    void dispatchColonReturnsZero() {
        MatcherAssert.assertThat(
            new BuiltinDispatcher().dispatch(
                ":",
                null,
                new Invocation(Arrays.asList(), new HashMap<>(), Arrays.asList())
            ),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void dispatchUnknownReturnsNull() {
        MatcherAssert.assertThat(
            new BuiltinDispatcher().dispatch(
                "nope",
                null,
                new Invocation(Arrays.asList(), new HashMap<>(), Arrays.asList())
            ),
            CoreMatchers.nullValue()
        );
    }

    @Test
    void registryHasType() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("type"), CoreMatchers.is(true));
    }

    @Test
    void registryHasShift() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("shift"), CoreMatchers.is(true));
    }

    @Test
    void registryHasUnset() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("unset"), CoreMatchers.is(true));
    }

    @Test
    void registryHasBreak() {
        MatcherAssert.assertThat(new BuiltinDispatcher().isBuiltin("break"), CoreMatchers.is(true));
    }

    @Test
    void registryHasContinue() {
        MatcherAssert.assertThat(
            new BuiltinDispatcher().isBuiltin("continue"),
            CoreMatchers.is(true)
        );
    }

    @Test
    void registerCustomBuiltinFirst() {
        final BuiltinDispatcher disp = new BuiltinDispatcher();
        disp.register("mybuiltin", (self, call) -> 42);
        MatcherAssert.assertThat(disp.isBuiltin("mybuiltin"), CoreMatchers.is(true));
    }

    @Test
    void registerCustomBuiltinSecond() {
        final BuiltinDispatcher disp = new BuiltinDispatcher();
        disp.register("mybuiltin", (self, call) -> 42);
        CoreMatchers.is(true);
        MatcherAssert.assertThat(
            disp.dispatch(
                "mybuiltin",
                null,
                new Invocation(Arrays.asList(), new HashMap<>(), Arrays.asList())
            ),
            CoreMatchers.equalTo(42)
        );
    }
}
