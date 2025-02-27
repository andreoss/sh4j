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

    @Test void registryHasTrue() { assertTrue(d.isBuiltin("true")); }
    @Test void registryHasFalse() { assertTrue(d.isBuiltin("false")); }
    @Test void registryHasColon() { assertTrue(d.isBuiltin(":")); }
    @Test void registryRejectsUnknown() { assertFalse(d.isBuiltin("nope")); }

    @Test void dispatchTrueReturns0() {
        assertEquals(0, d.dispatch("true", null, List.of(), new HashMap<>(), List.of()));
    }
    @Test void dispatchFalseReturns1() {
        assertEquals(1, d.dispatch("false", null, List.of(), new HashMap<>(), List.of()));
    }
    @Test void dispatchColonReturns0() {
        assertEquals(0, d.dispatch(":", null, List.of(), new HashMap<>(), List.of()));
    }
    @Test void dispatchUnknownReturnsNull() {
        assertNull(d.dispatch("nope", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void registerCustomBuiltin() {
        d.register("mybuiltin", (self, args, env, assigns) -> 42);
        assertTrue(d.isBuiltin("mybuiltin"));
        assertEquals(42, d.dispatch("mybuiltin", null, List.of(), new HashMap<>(), List.of()));
    }
}
