/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: POSIX 2.6.4 arithmetic expansion.
 *
 * @since 1.0
 */
final class PosixArithmeticTest {

    @Test
    void compoundAssignmentsPersistAfterColon() {
        Assertions.assertEquals(
            String.format("21 1 1%n"),
            Sh4j.INSTANCE.out(
                "x=5; : \"$((x+=2))\"; : \"$((x*=3))\";",
                "echo \"$x\" \"$((x%4))\" \"$((x>20 && x<30))\""
            )
        );
    }

    @Test
    void plainAssignmentPersists() {
        Assertions.assertEquals(
            String.format("3%n"),
            Sh4j.INSTANCE.out(": \"$((y=3))\"; echo \"$y\"")
        );
    }

    @Test
    void everyAssignmentOperatorAssigns() {
        Assertions.assertEquals(
            String.format("96 24 4 32 8 12 4 5%n"),
            Sh4j.INSTANCE.out(
                "x=100; : \"$((x-=4))\"; a=$x; : \"$((x/=4))\"; b=$x;",
                ": \"$((x%=5))\"; c=$x; : \"$((x<<=3))\"; d=$x;",
                ": \"$((x>>=2))\"; e=$x; : \"$((x|=4))\"; f=$x;",
                ": \"$((x&=6))\"; g=$x; : \"$((x^=1))\";",
                "echo \"$a\" \"$b\" \"$c\" \"$d\" \"$e\" \"$f\" \"$g\" \"$x\""
            )
        );
    }

    @Test
    void assignmentPersistsAfterTrue() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out("x=1; true \"$((x+=1))\"; echo \"$x\"")
        );
    }

    @Test
    void assignmentPersistsAfterFalse() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out("x=1; false \"$((x+=1))\"; echo \"$x\"")
        );
    }

    @Test
    void colonExpandsDefaultAssignment() {
        Assertions.assertEquals(
            String.format("[1]%n"),
            Sh4j.INSTANCE.out(": \"${q:=1}\"; echo \"[$q]\"")
        );
    }

    @Test
    void assignmentPersistsAfterEcho() {
        Assertions.assertEquals(
            String.format("7%n7%n"),
            Sh4j.INSTANCE.out("x=5; echo \"$((x+=2))\"; echo \"$x\"")
        );
    }

    @Test
    void pipelineAssignmentStaysInSubshell() {
        Assertions.assertEquals(
            String.format("3%n[]%n"),
            Sh4j.INSTANCE.out("echo \"$((z=3))\" | cat; echo \"[$z]\"")
        );
    }

    @Test
    void variableNamingVariableIsEvaluated() {
        Assertions.assertEquals(
            String.format("5%n"),
            Sh4j.INSTANCE.out("x=y; y=4; echo \"$((x+1))\"")
        );
    }

    @Test
    void variableHoldingExpressionIsEvaluated() {
        Assertions.assertEquals(
            String.format("7%n"),
            Sh4j.INSTANCE.out("x='2*3'; echo \"$((x+1))\"")
        );
    }

    @Test
    void compoundAssignmentReadsNamedVariable() {
        Assertions.assertEquals(
            String.format("3%n"),
            Sh4j.INSTANCE.out("x=y; y=2; : \"$((x+=1))\"; echo \"$x\"")
        );
    }

    @Test
    void selfReferenceDoesNotHang() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=x; echo \"$((x+1))\"; echo done")
        );
    }

    @Test
    void postIncrementIsErrorWithoutCrash() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=1; echo \"$((x++))\"; echo after")
        );
    }
}
