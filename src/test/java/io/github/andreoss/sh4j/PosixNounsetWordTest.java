/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: the {@code -u} option spares the word forms of
 * parameter expansion and the {@code @} and {@code *} parameters
 * (POSIX 2.6.2, set -u).
 *
 * @since 1.0
 */
final class PosixNounsetWordTest {

    @Test
    void defaultFormsExpandUnsetParameter() {
        Assertions.assertEquals(
            String.format("def d2%n"),
            Sh4j.INSTANCE.out("set -u; echo \"${z-def}\" \"${z:-d2}\"")
        );
    }

    @Test
    void alternativeFormsExpandUnsetParameter() {
        Assertions.assertEquals(
            String.format("x y%n"),
            Sh4j.INSTANCE.out("set -u; echo \"${z+set}x\" \"${z:+set}y\"")
        );
    }

    @Test
    void assignFormsSetUnsetParameter() {
        Assertions.assertEquals(
            String.format("v w v w%n"),
            Sh4j.INSTANCE.out("set -u; echo \"${z=v}\" \"${y:=w}\" \"$z\" \"$y\"")
        );
    }

    @Test
    void errorFormReportsItsOwnWord() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("set -u; echo \"${z?gone}\"").stderr().contains("gone")
        );
    }

    @Test
    void emptyPositionalParametersExpandToNothing() {
        Assertions.assertEquals(
            String.format("x y%nok%n"),
            Sh4j.INSTANCE.out("set -u; set --; echo \"$@\"x \"$*\"y; echo ok")
        );
    }

    @Test
    void wordFormsKeepSuccessStatus() {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(
                "set -u; echo \"${z-def}\" \"${z:-d2}\"; echo \"${z+set}x\"; echo \"$@\"x"
            )
        );
    }

    @Test
    void defaultFormsKeepSetParameters() {
        Assertions.assertEquals(
            String.format("a 0 b%n"),
            Sh4j.INSTANCE.out("set -u; set -- a; z=b; echo \"${1-x}\" \"${?-q}\" \"${z:-c}\"")
        );
    }

    @Test
    void plainExpansionStillAborts() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("set -u; echo \"$z\"; echo after").stdout()
        );
    }

    @Test
    void lengthOfUnsetParameterStillAborts() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.run("set -u; echo \"${#z}\"; echo after")
        );
    }

    @Test
    void patternRemovalOfUnsetParameterStillAborts() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.run("set -u; echo \"${z%x}\"; echo after")
        );
    }
}
