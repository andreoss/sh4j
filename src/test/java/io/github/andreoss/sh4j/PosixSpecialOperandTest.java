/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for invalid operands of the special built-ins {@code exit},
 * {@code return}, {@code shift} and of the regular built-in {@code read}
 * (POSIX 2.8.1 Consequences of Shell Errors).
 *
 * @since 1.0
 */
final class PosixSpecialOperandTest {

    @Test
    void exitNonNumericExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("exit abc; echo after"));
    }

    @Test
    void exitNonNumericPrintsNothing() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("exit abc 2>/dev/null; echo after"));
    }

    @Test
    void exitNonNumericReportsError() {
        Assertions.assertEquals(
            String.format("sh: exit: Illegal number: abc%n"),
            Sh4j.INSTANCE.runFull("exit abc; echo after").stderr()
        );
    }

    @Test
    void exitNegativeExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("exit -1; echo after"));
    }

    @Test
    void exitOverflowExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("exit 99999999999; echo after"));
    }

    @Test
    void exitExtraOperandUsesFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("exit 1 2; echo after"));
    }

    @Test
    void exitWithoutOperandUsesLastStatus() {
        Assertions.assertEquals(3, Sh4j.INSTANCE.run("(exit 3); exit; echo after"));
    }

    @Test
    void returnNonNumericExitsWithTwo() {
        Assertions.assertEquals(
            2, Sh4j.INSTANCE.run("f() { return abc; }; f; echo \"after $?\"")
        );
    }

    @Test
    void returnNonNumericPrintsNothing() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("f() { return abc; }; f 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void returnNonNumericReportsError() {
        Assertions.assertEquals(
            String.format("sh: return: Illegal number: abc%n"),
            Sh4j.INSTANCE.runFull("f() { return abc; }; f; echo after").stderr()
        );
    }

    @Test
    void returnWithoutOperandUsesLastStatus() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("f() { false; return; }; f; echo \"after $?\"")
        );
    }

    @Test
    void returnExtraOperandUsesFirst() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("f() { return 1 2; }; f; echo \"after $?\"")
        );
    }

    @Test
    void returnOutsideFunctionStopsWithOperand() {
        Assertions.assertEquals(4, Sh4j.INSTANCE.run("return 4; echo after"));
    }

    @Test
    void returnOutsideFunctionStopsWithLastStatus() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("false; return; echo after"));
    }

    @Test
    void shiftNonNumericExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("shift abc; echo after"));
    }

    @Test
    void shiftNonNumericReportsError() {
        Assertions.assertEquals(
            String.format("sh: shift: Illegal number: abc%n"),
            Sh4j.INSTANCE.runFull("shift abc; echo after").stderr()
        );
    }

    @Test
    void shiftNegativeExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("shift -1; echo after"));
    }

    @Test
    void shiftNegativeReportsError() {
        Assertions.assertEquals(
            String.format("sh: shift: Illegal number: -1%n"),
            Sh4j.INSTANCE.runFull("shift -1; echo after").stderr()
        );
    }

    @Test
    void shiftTooManyExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("set -- a; shift 2; echo after"));
    }

    @Test
    void shiftTooManyPrintsNothing() {
        Assertions.assertEquals(
            "", Sh4j.INSTANCE.out("set -- a; shift 2 2>/dev/null; echo after")
        );
    }

    @Test
    void shiftTooManyReportsError() {
        Assertions.assertEquals(
            String.format("sh: shift: can't shift that many%n"),
            Sh4j.INSTANCE.runFull("set -- a; shift 2; echo after").stderr()
        );
    }

    @Test
    void shiftInFunctionErrorExitsShell() {
        Assertions.assertEquals(
            "", Sh4j.INSTANCE.out("f() { shift abc; }; f 2>/dev/null; echo after")
        );
    }

    @Test
    void shiftZeroKeepsParameters() {
        Assertions.assertEquals(
            String.format("2 a%n"),
            Sh4j.INSTANCE.out("set -- a b; shift 0; echo \"$# $1\"")
        );
    }

    @Test
    void shiftAllParametersSucceeds() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("set -- a b; shift 2; echo \"$#\"")
        );
    }

    @Test
    void commandExitNonNumericContinues() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("command exit abc 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void commandExitNonNumericReportsError() {
        Assertions.assertEquals(
            String.format("sh: exit: Illegal number: abc%n"),
            Sh4j.INSTANCE.runFull("command exit abc; echo after").stderr()
        );
    }

    @Test
    void commandShiftNonNumericContinues() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("command shift abc 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void commandShiftTooManyContinues() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("set -- a; command shift 2 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void readInvalidNameFailsWithTwo() {
        Assertions.assertEquals(
            String.format("rc=2%n"),
            Sh4j.INSTANCE.out("read 1bad </dev/null 2>/dev/null; echo \"rc=$?\"")
        );
    }

    @Test
    void readInvalidNameReportsError() {
        Assertions.assertEquals(
            String.format("sh: read: 1bad: bad variable name%n"),
            Sh4j.INSTANCE.runFull("read 1bad </dev/null; echo \"rc=$?\"").stderr()
        );
    }

    @Test
    void readInvalidNameConsumesLine() {
        Assertions.assertEquals(
            String.format("rc=2 b=y%n"),
            Sh4j.INSTANCE.out(
                "printf 'x\\ny\\n' | { read 1bad 2>/dev/null; r=$?; read b; echo \"rc=$r b=$b\"; }"
            )
        );
    }

    @Test
    void readInvalidNameAssignsEarlierNames() {
        Assertions.assertEquals(
            String.format("rc=2 a=hi%n"),
            Sh4j.INSTANCE.out(
                "echo hi | { read a 1bad 2>/dev/null; echo \"rc=$? a=$a\"; }"
            )
        );
    }

    @Test
    void readInvalidNameKeepsShell() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("read 1bad </dev/null 2>/dev/null; echo after")
        );
    }

    @Test
    void readWithoutNameAtEndOfFileFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(new byte[0]), "read; echo \"rc=$?\""
            )
        );
    }
}
