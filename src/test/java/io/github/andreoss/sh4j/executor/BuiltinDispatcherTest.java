/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BuiltinDispatcher.
 * @since 1.0
 */
final class BuiltinDispatcherTest {

    /** Disp. */
    private final BuiltinDispatcher disp = new BuiltinDispatcher();

    @Test
    void registryHasTrue() {
        assertTrue(this.disp.isBuiltin("true"));
    }

    @Test
    void registryHasFalse() {
        assertTrue(this.disp.isBuiltin("false"));
    }

    @Test
    void registryHasColon() {
        assertTrue(this.disp.isBuiltin(":"));
    }

    @Test
    void registryRejectsUnknown() {
        assertFalse(this.disp.isBuiltin("nope"));
    }

    @Test void dispatchTrueReturnsZero() {
        assertEquals(0, this.disp.dispatch("true", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void dispatchFalseReturnsOne() {
        assertEquals(1, this.disp.dispatch("false", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void dispatchColonReturnsZero() {
        assertEquals(0, this.disp.dispatch(":", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test void dispatchUnknownReturnsNull() {
        assertNull(this.disp.dispatch("nope", null, List.of(), new HashMap<>(), List.of()));
    }

    @Test
    void registryHasType() {
        assertTrue(this.disp.isBuiltin("type"));
    }

    @Test
    void registryHasShift() {
        assertTrue(this.disp.isBuiltin("shift"));
    }

    @Test
    void registryHasUnset() {
        assertTrue(this.disp.isBuiltin("unset"));
    }

    @Test
    void registryHasBreak() {
        assertTrue(this.disp.isBuiltin("break"));
    }

    @Test
    void registryHasContinue() {
        assertTrue(this.disp.isBuiltin("continue"));
    }

    @Test void registerCustomBuiltin() {
        this.disp.register("mybuiltin", (self, args, env, assigns) -> 42);
        assertTrue(this.disp.isBuiltin("mybuiltin"));
        assertEquals(42, this.disp.dispatch("mybuiltin", null, List.of(), new HashMap<>(), List.of()));
    }
}
