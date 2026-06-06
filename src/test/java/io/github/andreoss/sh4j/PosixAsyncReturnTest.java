/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: return in a function run as an asynchronous list.
 *
 * @since 1.0
 */
final class PosixAsyncReturnTest {

    /**
     * POSIX 2.9.3.1 and return: a function run in the background still
     * returns from its own body.
     */
    @Test
    void returnInBackgroundFunctionIsSilent() {
        Assertions.assertEquals(
            String.format("rc=6%n"),
            Sh4j.INSTANCE.out("f() { return 6; }; f 2>&1 & wait $!; echo \"rc=$?\"")
        );
    }

    @Test
    void returnInBackgroundFunctionOnOwnLineIsSilent() {
        Assertions.assertEquals(
            String.format("rc=6%n"),
            Sh4j.INSTANCE.out(
                String.format("f() { return 6; }%nf 2>&1 &%nwait $!; echo \"rc=$?\"")
            )
        );
    }

    @Test
    void returnInPipelineFunctionIsSilent() {
        Assertions.assertEquals(
            String.format("rc=0%n"),
            Sh4j.INSTANCE.out("f() { return 6; }; f 2>&1 | cat; echo \"rc=$?\"")
        );
    }

    @Test
    void returnInBackgroundFunctionStopsTheBody() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("f() { echo a; return 6; echo b; }; f & wait $!")
        );
    }
}
