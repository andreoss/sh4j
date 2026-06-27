/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for a readonly variable changed by the special built-ins
 * {@code unset}, {@code export} and {@code readonly}: the error exits a
 * non-interactive shell, but {@code command} runs the utility as a regular
 * built-in and only returns a non-zero status (POSIX 2.8.1, 2.14).
 *
 * @since 1.0
 */
final class PosixReadonlyCommandTest {

    @Test
    void unsetReadonlyExitsShell() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("readonly r=1; unset r; echo after"));
    }

    @Test
    void commandUnsetReadonlyContinues() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("readonly r=1; command unset r; echo \"after $?\"")
        );
    }

    @Test
    void commandUnsetReadonlyReportsError() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("readonly r=1; command unset r; echo \"after $?\"")
                .stderr().contains("r: readonly variable")
        );
    }

    @Test
    void commandUnsetReadonlyKeepsValue() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("readonly r=1; command unset r 2>/dev/null; echo \"$r\"")
        );
    }

    @Test
    void exportReadonlyExitsShell() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("readonly r=1; export r=2; echo after"));
    }

    @Test
    void exportReadonlyPrintsNothingAfter() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("readonly r=1; export r=2 2>/dev/null; echo after")
        );
    }

    @Test
    void commandExportReadonlyContinues() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("readonly r=1; command export r=2; echo \"after $?\"")
        );
    }

    @Test
    void commandExportReadonlyKeepsValue() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("readonly r=1; command export r=2 2>/dev/null; echo \"$r\"")
        );
    }

    @Test
    void commandExportReadonlyReportsError() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("readonly r=1; command export r=2")
                .stderr().contains("r: readonly variable")
        );
    }

    @Test
    void exportReadonlyNameWithoutValueSucceeds() {
        Assertions.assertEquals(
            String.format("0 1%n"),
            Sh4j.INSTANCE.out("readonly r=1; export r; echo \"$? $r\"")
        );
    }

    @Test
    void readonlyReassignExitsShell() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("readonly r=1; readonly r=2; echo after"));
    }

    @Test
    void commandReadonlyReassignContinues() {
        Assertions.assertEquals(
            String.format("after 1 1%n"),
            Sh4j.INSTANCE.out(
                "readonly r=1; command readonly r=2 2>/dev/null; echo \"after $? $r\""
            )
        );
    }

    @Test
    void readonlyAgainWithoutValueSucceeds() {
        Assertions.assertEquals(
            String.format("0 1%n"),
            Sh4j.INSTANCE.out("readonly r=1; readonly r; echo \"$? $r\"")
        );
    }

    @Test
    void assignmentBeforeCommandExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("readonly r=1; r=2 command : 2>/dev/null; echo after")
        );
    }

    @Test
    void plainAssignmentExitsShell() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("readonly r=1; r=2; echo after"));
    }
}
