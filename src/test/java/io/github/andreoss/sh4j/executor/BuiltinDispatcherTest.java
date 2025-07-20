/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for BuiltinDispatcher.
 * @since 1.0
 */
final class BuiltinDispatcherTest {

    @Test
    void registryHasTrue() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("true"));
    }

    @Test
    void registryHasFalse() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("false"));
    }

    @Test
    void registryHasColon() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin(":"));
    }

    @Test
    void registryRejectsUnknown() {
        Assertions.assertFalse(new BuiltinDispatcher().isBuiltin("nope"));
    }

    @Test void dispatchTrueReturnsZero() {
        Assertions.assertEquals(
            0,
            new BuiltinDispatcher().dispatch("true", null, List.of(), new HashMap<>(), List.of())
        );
    }

    @Test void dispatchFalseReturnsOne() {
        Assertions.assertEquals(
            1,
            new BuiltinDispatcher().dispatch("false", null, List.of(), new HashMap<>(), List.of())
        );
    }

    @Test void dispatchColonReturnsZero() {
        Assertions.assertEquals(
            0,
            new BuiltinDispatcher().dispatch(":", null, List.of(), new HashMap<>(), List.of())
        );
    }

    @Test void dispatchUnknownReturnsNull() {
        Assertions.assertNull(
            new BuiltinDispatcher().dispatch("nope", null, List.of(), new HashMap<>(), List.of())
        );
    }

    @Test
    void registryHasType() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("type"));
    }

    @Test
    void registryHasShift() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("shift"));
    }

    @Test
    void registryHasUnset() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("unset"));
    }

    @Test
    void registryHasBreak() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("break"));
    }

    @Test
    void registryHasContinue() {
        Assertions.assertTrue(new BuiltinDispatcher().isBuiltin("continue"));
    }

    @Test void registerCustomBuiltin() {
        final BuiltinDispatcher disp = new BuiltinDispatcher();
        disp.register("mybuiltin", (self, args, env, assigns) -> 42);
        Assertions.assertTrue(disp.isBuiltin("mybuiltin"));
        Assertions.assertEquals(
            42,
            disp.dispatch("mybuiltin", null, List.of(), new HashMap<>(), List.of())
        );
    }
}
