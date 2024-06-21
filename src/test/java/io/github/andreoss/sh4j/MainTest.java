/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MainTest {
    @Test
    void exitCodeForScriptFileReturnsZero() {
        assertEquals(0, Main.exitCode(new String[] {"test.sh"}));
    }
}
