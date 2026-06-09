/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for invalid options and operands of the special built-ins
 * {@code export}, {@code readonly}, {@code unset}, {@code trap} and
 * {@code set}, and of the regular built-ins {@code jobs}, {@code pwd} and
 * {@code command} (POSIX 2.8.1 Consequences of Shell Errors, 12.2 Utility
 * Syntax Guidelines).
 *
 * @since 1.0
 */
final class PosixBuiltinUsageTest {

    @Test
    void exportBadNameExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("export 1abc=x; echo after"));
    }

    @Test
    void exportBadNameReportsError() {
        Assertions.assertEquals(
            String.format("sh: export: 1abc: bad variable name%n"),
            Sh4j.INSTANCE.runFull("export 1abc=x; echo after").stderr()
        );
    }

    @Test
    void exportBareBadNameExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("export 1abc; echo after"));
    }

    @Test
    void exportIllegalOptionExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("export -Z a=1; echo after"));
    }

    @Test
    void exportIllegalOptionReportsError() {
        Assertions.assertEquals(
            String.format("sh: export: Illegal option -Z%n"),
            Sh4j.INSTANCE.runFull("export -Z; echo after").stderr()
        );
    }

    @Test
    void exportPrintListsVariables() {
        Assertions.assertEquals(
            String.format("export ZZQ=1%n"),
            Sh4j.INSTANCE.out("export ZZQ=1; export -p | grep ZZQ")
        );
    }

    @Test
    void exportDoubleDashEndsOptions() {
        Assertions.assertEquals(
            String.format("export ZZQ=1%n"),
            Sh4j.INSTANCE.out("export -- ZZQ=1; export -p | grep ZZQ")
        );
    }

    @Test
    void commandExportBadNameContinues() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("command export 1abc=x 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void readonlyBadNameExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("readonly 1abc=x; echo after"));
    }

    @Test
    void readonlyBadNameReportsError() {
        Assertions.assertEquals(
            String.format("sh: readonly: 1b: bad variable name%n"),
            Sh4j.INSTANCE.runFull("readonly a=1 1b=2; echo after").stderr()
        );
    }

    @Test
    void readonlyIllegalOptionExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("readonly -Z; echo after"));
    }

    @Test
    void readonlyPrintListsVariables() {
        Assertions.assertEquals(
            String.format("readonly ZZQ=1%n"),
            Sh4j.INSTANCE.out("readonly ZZQ=1; readonly -p | grep ZZQ")
        );
    }

    @Test
    void unsetIllegalOptionExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("unset -z; echo after"));
    }

    @Test
    void unsetIllegalOptionReportsError() {
        Assertions.assertEquals(
            String.format("sh: unset: Illegal option -z%n"),
            Sh4j.INSTANCE.runFull("unset -z; echo after").stderr()
        );
    }

    @Test
    void unsetBadNameExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("unset 1abc; echo after"));
    }

    @Test
    void unsetBadNameReportsError() {
        Assertions.assertEquals(
            String.format("sh: unset: 1abc: bad variable name%n"),
            Sh4j.INSTANCE.runFull("unset -v 1abc; echo after").stderr()
        );
    }

    @Test
    void unsetFunctionBadNameSucceeds() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("unset -f 1abc; echo \"$?\"")
        );
    }

    @Test
    void unsetVariableOptionRemovesVariable() {
        Assertions.assertEquals(
            String.format("gone%n"),
            Sh4j.INSTANCE.out("x=1; unset -v -- x; echo \"${x-gone}\"")
        );
    }

    @Test
    void unsetFunctionOptionRemovesFunction() {
        Assertions.assertEquals(
            String.format("127%n"),
            Sh4j.INSTANCE.out("f() { :; }; unset -f f; f 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void trapBadSignalReturnsOne() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("trap x BOGUS 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void trapBadSignalReportsError() {
        Assertions.assertEquals(
            String.format("sh: trap: BOGUS: bad trap%n"),
            Sh4j.INSTANCE.runFull("trap x BOGUS").stderr()
        );
    }

    @Test
    void trapResetBadSignalReturnsOne() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("trap - BOGUS 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void trapBadSignalStopsTheOperands() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("trap x BOGUS INT 2>/dev/null; trap")
        );
    }

    @Test
    void trapUnnamedNumberIsBad() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("trap x 99 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void trapSignalNumberNamesSignal() {
        Assertions.assertEquals(
            String.format("trap -- 'x' INT%n"),
            Sh4j.INSTANCE.out("trap x 2; trap")
        );
    }

    @Test
    void trapLowerCaseNameNamesSignal() {
        Assertions.assertEquals(
            String.format("trap -- 'x' INT%n"),
            Sh4j.INSTANCE.out("trap x int; trap")
        );
    }

    @Test
    void trapZeroIsExit() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out("trap 'echo hi' 0")
        );
    }

    @Test
    void trapDoubleDashEndsOptions() {
        Assertions.assertEquals(
            String.format("t%n"),
            Sh4j.INSTANCE.out("trap -- 'echo t' EXIT")
        );
    }

    @Test
    void trapEmptyConditionIsBad() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("trap x '' 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void trapNumericFirstOperandResets() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("trap 'echo hi' EXIT; trap 0")
        );
    }

    @Test
    void trapIllegalOptionExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("trap -Z; echo after"));
    }

    @Test
    void commandTrapBadSignalContinues() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("command trap x BOGUS 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void setIllegalOptionExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("set -Z; echo after"));
    }

    @Test
    void setIllegalOptionReportsError() {
        Assertions.assertEquals(
            String.format("sh: set: Illegal option -Z%n"),
            Sh4j.INSTANCE.runFull("set -eZ; echo after").stderr()
        );
    }

    @Test
    void setIllegalOptionKeepsParameters() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("set -- a; command set -Z b 2>/dev/null; echo \"$1\"")
        );
    }

    @Test
    void setBadOptionNameExitsWithTwo() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("set +o bogus; echo after"));
    }

    @Test
    void setBadOptionNameReportsError() {
        Assertions.assertEquals(
            String.format("sh: set: Illegal option -o bogus%n"),
            Sh4j.INSTANCE.runFull("set -o bogus; echo after").stderr()
        );
    }

    @Test
    void commandSetIllegalOptionContinues() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("command set -Z 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void setDoubleDashSetsParameters() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out("set -- a b; echo \"$#\"")
        );
    }

    @Test
    void setOptionListingShowsState() {
        Assertions.assertEquals(
            String.format("errexit         on%n"),
            Sh4j.INSTANCE.out("set -e; set -o | grep errexit")
        );
    }

    @Test
    void setOptionListingHasHeader() {
        Assertions.assertEquals(
            String.format("Current option settings%n"),
            Sh4j.INSTANCE.out("set -o | head -n 1")
        );
    }

    @Test
    void setOptionListingKeepsFlags() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -o >/dev/null; echo \"$-\"")
        );
    }

    @Test
    void setPlusOptionListingIsReinput() {
        Assertions.assertEquals(
            String.format("set -o noglob%nset +o nounset%n"),
            Sh4j.INSTANCE.out("set -f; set +o | grep 'noglob\\|nounset'")
        );
    }

    @Test
    void setNamedMonitorOptionIsAccepted() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out("set -o monitor; echo \"$-\"")
        );
    }

    @Test
    void jobsUnknownJobReturnsTwo() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("jobs %9 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void jobsUnknownJobReportsError() {
        Assertions.assertEquals(
            String.format("sh: jobs: %%9: no such job%n"),
            Sh4j.INSTANCE.runFull("jobs %9").stderr()
        );
    }

    @Test
    void jobsIllegalOptionReturnsTwo() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("jobs -Z 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void jobsListOptionsAreAccepted() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("jobs -l; jobs -p; echo \"$?\"")
        );
    }

    @Test
    void jobsKnownJobIsListed() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("sleep 1 & jobs %1 >/dev/null; echo \"$?\"")
        );
    }

    @Test
    void pwdIllegalOptionReturnsTwo() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("pwd -Z 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void pwdIllegalOptionReportsError() {
        Assertions.assertEquals(
            String.format("sh: pwd: Illegal option -Z%n"),
            Sh4j.INSTANCE.runFull("pwd -Z").stderr()
        );
    }

    @Test
    void pwdCombinedOptionsAreAccepted() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("pwd -LP >/dev/null; echo \"$?\"")
        );
    }

    @Test
    void commandIllegalOptionReturnsTwo() {
        Assertions.assertEquals(
            String.format("after 2%n"),
            Sh4j.INSTANCE.out("command -Z true 2>/dev/null; echo \"after $?\"")
        );
    }

    @Test
    void commandIllegalOptionReportsError() {
        Assertions.assertEquals(
            String.format("sh: command: Illegal option -Z%n"),
            Sh4j.INSTANCE.runFull("command -Z").stderr()
        );
    }

    @Test
    void commandDoubleDashRunsUtility() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("command -- echo x")
        );
    }

    @Test
    void commandLocateStillWorks() {
        Assertions.assertEquals(
            String.format("echo%n"),
            Sh4j.INSTANCE.out("command -pv echo")
        );
    }
}
