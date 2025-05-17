/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class BuiltinDispatcherTest {

    private final BuiltinDispatcher d = new BuiltinDispatcher();

    @Test
    void registryHasTrue() {
        assertTrue(this.d.isBuiltin("true"));
    }

    @Test
    void registryHasFalse() {
        assertTrue(this.d.isBuiltin("false"));
    }

    @Test
    void registryHasColon() {
        assertTrue(this.d.isBuiltin(":"));
    }

    @Test
    void registryRejectsUnknown() {
        assertFalse(this.d.isBuiltin("nope"));
    }

    @Test void dispatchTrueReturns0() {
        assertEquals(0, this.d.dispatch("true", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void dispatchFalseReturns1() {
        assertEquals(1, this.d.dispatch("false", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void dispatchColonReturns0() {
        assertEquals(0, this.d.dispatch(":", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void dispatchUnknownReturnsNull() {
        assertNull(this.d.dispatch("nope", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test
    void registryHasType() {
        assertTrue(this.d.isBuiltin("type"));
    }

    @Test
    void registryHasShift() {
        assertTrue(this.d.isBuiltin("shift"));
    }

    @Test
    void registryHasUnset() {
        assertTrue(this.d.isBuiltin("unset"));
    }

    @Test
    void registryHasBreak() {
        assertTrue(this.d.isBuiltin("break"));
    }

    @Test
    void registryHasContinue() {
        assertTrue(this.d.isBuiltin("continue"));
    }

    @Test void registerCustomBuiltin() {
        this.d.register("mybuiltin", (self, args, env, assigns) -> 42);
        assertTrue(this.d.isBuiltin("mybuiltin"));
        assertEquals(42, this.d.dispatch("mybuiltin", null, List.of(), new HashMap<>(), List.of()));
    }
}
