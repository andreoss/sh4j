/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for BuiltinDispatcher.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class BuiltinDispatcherTest {

    @Test
    public void registryHasTrue() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("true"));
    }

    @Test
    public void registryHasFalse() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("false"));
    }

    @Test
    public void registryHasColon() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin(":"));
    }

    @Test
    public void registryRejectsUnknown() {
        Assert.assertFalse(new BuiltinDispatcher().isBuiltin("nope"));
    }

    @Test public void dispatchTrueReturnsZero() {
        Assert.assertEquals(
            (Integer) 0,
            new BuiltinDispatcher().dispatch("true", null, Arrays.asList(), new HashMap<>(), Arrays.asList())
        );
    }

    @Test public void dispatchFalseReturnsOne() {
        Assert.assertEquals(
            (Integer) 1,
            new BuiltinDispatcher().dispatch("false", null, Arrays.asList(), new HashMap<>(), Arrays.asList())
        );
    }

    @Test public void dispatchColonReturnsZero() {
        Assert.assertEquals(
            (Integer) 0,
            new BuiltinDispatcher().dispatch(":", null, Arrays.asList(), new HashMap<>(), Arrays.asList())
        );
    }

    @Test public void dispatchUnknownReturnsNull() {
        Assert.assertNull(
            new BuiltinDispatcher().dispatch("nope", null, Arrays.asList(), new HashMap<>(), Arrays.asList())
        );
    }

    @Test
    public void registryHasType() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("type"));
    }

    @Test
    public void registryHasShift() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("shift"));
    }

    @Test
    public void registryHasUnset() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("unset"));
    }

    @Test
    public void registryHasBreak() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("break"));
    }

    @Test
    public void registryHasContinue() {
        Assert.assertTrue(new BuiltinDispatcher().isBuiltin("continue"));
    }

    @Test public void registerCustomBuiltin() {
        final BuiltinDispatcher disp = new BuiltinDispatcher();
        disp.register("mybuiltin", (self, args, env, assigns) -> 42);
        Assert.assertTrue(disp.isBuiltin("mybuiltin"));
        Assert.assertEquals(
            (Integer) 42,
            disp.dispatch("mybuiltin", null, Arrays.asList(), new HashMap<>(), Arrays.asList())
        );
    }
}
