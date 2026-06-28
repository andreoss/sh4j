/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClosedInput}.
 *
 * @since 1.0
 */
final class ClosedInputTest {

    @Test
    void failsBulkRead() {
        Assertions.assertThrows(
            IOException.class,
            () -> ClosedInput.make().read(new byte[2], 0, 2)
        );
    }
}
