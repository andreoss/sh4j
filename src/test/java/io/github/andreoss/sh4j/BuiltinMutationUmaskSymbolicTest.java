/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: umask symbolic.
 *
 * @since 1.0
 */
final class BuiltinMutationUmaskSymbolicTest {

    @Test
    void umaskSymbolicFallbackAndAliasWithAssignmentsFirst() {
        Assertions.assertEquals(
            String.format("u=,g=,o=%n"),
            Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S")
        );
    }

    @Test
    void umaskSymbolicFallbackAndAliasWithAssignmentsSecond() {
        Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S");
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("set foo; echo $#"));
    }

    @Test
    void umaskSymbolicFallbackAndAliasWithAssignmentsThird() {
        Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S");
        Sh4j.INSTANCE.out("set foo; echo $#");
        Assertions.assertEquals(
            String.format("b='echo'%n"),
            Sh4j.INSTANCE.out("A=1 alias b=echo; alias b")
        );
    }

    @Test
    void umaskAllBitCombinationsFirst() {
        Assertions.assertEquals(
            String.format("u=rwx,g=rwx,o=rwx%n"),
            Sh4j.INSTANCE.out("umask 000; umask -S")
        );
    }

    @Test
    void umaskAllBitCombinationsSecond() {
        Sh4j.INSTANCE.out("umask 000; umask -S");
        Assertions.assertEquals(
            String.format("u=,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 777; umask -S")
        );
    }

    @Test
    void umaskAllBitCombinationsThird() {
        Sh4j.INSTANCE.out("umask 000; umask -S");
        Sh4j.INSTANCE.out("umask 777; umask -S");
        Assertions.assertEquals(
            String.format("u=rwx,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 077; umask -S")
        );
    }

    @Test
    void umaskAllBitCombinationsFourth() {
        Sh4j.INSTANCE.out("umask 000; umask -S");
        Sh4j.INSTANCE.out("umask 777; umask -S");
        Sh4j.INSTANCE.out("umask 077; umask -S");
        Assertions.assertEquals(
            String.format("u=rwx,g=r,o=r%n"),
            Sh4j.INSTANCE.out("umask 033; umask -S")
        );
    }

    @Test
    void unsetOptionFormsFirst() {
        Assertions.assertNull(Sh4j.INSTANCE.getVar("X", "X=1; unset -v X"));
    }

    @Test
    void unsetOptionFormsSecond() {
        Assertions.assertNull(Sh4j.INSTANCE.getVar("X", "X=1; unset -- X"));
    }

    @Test
    void unsetOptionFormsThird() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("f() { :; }; unset -f f"));
    }

    @Test
    void unsetOptionFormsFourth() {
        Sh4j.INSTANCE.run("f() { :; }; unset -f f");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("X=1; unset -z X"));
    }
}
