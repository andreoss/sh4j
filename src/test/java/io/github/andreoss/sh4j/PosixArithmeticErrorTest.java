/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.ArithmeticEvaluator;
import io.github.andreoss.sh4j.executor.Executor;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: invalid arithmetic expansion is an expansion
 * error (POSIX 2.6.4, POSIX 2.8.1).
 *
 * @since 1.0
 */
final class PosixArithmeticErrorTest {

    @Test
    void divisionByZeroStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((1/0))\"; echo after")
        );
    }

    @Test
    void divisionByZeroFailsScript() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("echo \"$((1/0))\"; echo after")
        );
    }

    @Test
    void divisionByZeroIsReported() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo \"$((1/0))\"").stderr().contains("division by zero")
        );
    }

    @Test
    void remainderByZeroStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((5%0))\"; echo after")
        );
    }

    @Test
    void variableDivisorZeroStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=0; : \"$((10/x))\"; echo after")
        );
    }

    @Test
    void divideAssignByZeroStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=4; : \"$((x/=0))\"; echo after")
        );
    }

    @Test
    void remainderAssignByZeroStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=4; : \"$((x%=0))\"; echo after")
        );
    }

    @Test
    void subshellErrorExitsOnlySubshell() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("(echo \"$((1/0))\"); echo after")
        );
    }

    @Test
    void subshellErrorFailsSubshell() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("(echo \"$((1/0))\"); echo \"$?\"")
        );
    }

    @Test
    void commandSubstitutionErrorExitsOnlyIt() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("y=$(echo \"$((1/0))\"); echo after")
        );
    }

    @Test
    void incompleteExpressionStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((1+))\"; echo after")
        );
    }

    @Test
    void trailingOperandStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((1 2))\"; echo after")
        );
    }

    @Test
    void invalidCharacterStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((@))\"; echo after")
        );
    }

    @Test
    void conditionalWithoutColonStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((1 ? 2))\"; echo after")
        );
    }

    @Test
    void selfReferenceStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=x; echo \"$((x))\"; echo after")
        );
    }

    @Test
    void invalidVariableValueStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x='1 2'; echo \"$((x))\"; echo after")
        );
    }

    @Test
    void unclosedGroupIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("(1", new HashMap<>())
        );
    }

    @Test
    void emptyExpressionIsZero() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("echo \"$(( ))\"")
        );
    }

    @Test
    void emptyVariableIsZero() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("x=''; echo \"$((x+1))\"")
        );
    }

    @Test
    void skippedAndOperandDividesByZero() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("echo \"$((0 && 1/0))\"")
        );
    }

    @Test
    void skippedOrOperandTakesRemainderByZero() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("echo \"$((1 || 1%0))\"")
        );
    }

    @Test
    void skippedBranchDivideAssignsByZero() {
        Assertions.assertEquals(
            String.format("2 4%n"),
            Sh4j.INSTANCE.out("x=4; echo \"$((1 ? 2 : (x/=0))) $x\"")
        );
    }

    @Test
    void interactiveShellSurvivesDivisionByZero() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "echo \"$((1/0))\"");
        Main.executeLine(executor, "alive=yes");
        Assertions.assertEquals("yes", executor.getVariable("alive"));
    }
}
