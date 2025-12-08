/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for BuiltinDispatcher.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class BuiltinDispatcherTest {

    @Test
    public void registryHasTrue() {
        assertThat(new BuiltinDispatcher().isBuiltin("true"), is(true));
    }

    @Test
    public void registryHasFalse() {
        assertThat(new BuiltinDispatcher().isBuiltin("false"), is(true));
    }

    @Test
    public void registryHasColon() {
        assertThat(new BuiltinDispatcher().isBuiltin(":"), is(true));
    }

    @Test
    public void registryRejectsUnknown() {
        assertThat(new BuiltinDispatcher().isBuiltin("nope"), is(false));
    }

    @Test public void dispatchTrueReturnsZero() {
        assertThat(
            new BuiltinDispatcher().dispatch("true", null, Arrays.asList(), new HashMap<>(), Arrays.asList()),
            equalTo(0)
        );
    }

    @Test public void dispatchFalseReturnsOne() {
        assertThat(
            new BuiltinDispatcher().dispatch("false", null, Arrays.asList(), new HashMap<>(), Arrays.asList()),
            equalTo(1)
        );
    }

    @Test public void dispatchColonReturnsZero() {
        assertThat(
            new BuiltinDispatcher().dispatch(":", null, Arrays.asList(), new HashMap<>(), Arrays.asList()),
            equalTo(0)
        );
    }

    @Test public void dispatchUnknownReturnsNull() {
        assertThat(
            new BuiltinDispatcher().dispatch("nope", null, Arrays.asList(), new HashMap<>(), Arrays.asList()),
            nullValue()
        );
    }

    @Test
    public void registryHasType() {
        assertThat(new BuiltinDispatcher().isBuiltin("type"), is(true));
    }

    @Test
    public void registryHasShift() {
        assertThat(new BuiltinDispatcher().isBuiltin("shift"), is(true));
    }

    @Test
    public void registryHasUnset() {
        assertThat(new BuiltinDispatcher().isBuiltin("unset"), is(true));
    }

    @Test
    public void registryHasBreak() {
        assertThat(new BuiltinDispatcher().isBuiltin("break"), is(true));
    }

    @Test
    public void registryHasContinue() {
        assertThat(new BuiltinDispatcher().isBuiltin("continue"), is(true));
    }

    @Test public void registerCustomBuiltin() {
        final BuiltinDispatcher disp = new BuiltinDispatcher();
        disp.register("mybuiltin", (self, args, env, assigns) -> 42);
        assertThat(disp.isBuiltin("mybuiltin"), is(true));
        assertThat(
            disp.dispatch("mybuiltin", null, Arrays.asList(), new HashMap<>(), Arrays.asList()),
            equalTo(42)
        );
    }
}
