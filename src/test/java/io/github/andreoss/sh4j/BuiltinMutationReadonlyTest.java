/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Sh4j}: readonly.
 *
 * @since 1.0
 */
final class BuiltinMutationReadonlyTest {

    @Test
    void readonlyBareAndAssignedFormsFirst() {
        Assertions.assertEquals(
            String.format("unset%n"),
            Sh4j.INSTANCE.out("readonly R; echo ${R-unset}")
        );
    }

    @Test
    void readonlyBareAndAssignedFormsSecond() {
        Sh4j.INSTANCE.out("readonly R; echo ${R-unset}");
        Assertions.assertEquals(
            String.format("readonly R=1%n"),
            Sh4j.INSTANCE.out("readonly R=1; readonly -p")
        );
    }

    @Test
    void setOptionRemovalAndPositionalFormsFirst() {
        Assertions.assertFalse(Sh4j.INSTANCE.out("set -e; set +e; echo $-").contains("e"));
    }

    @Test
    void setOptionRemovalAndPositionalFormsSecond() {
        Sh4j.INSTANCE.out("set -e; set +e; echo $-");
        Assertions.assertFalse(
            Sh4j.INSTANCE.out("set -o errexit; set +o errexit; echo $-").contains("e")
        );
    }

    @Test
    void setOptionRemovalAndPositionalFormsThird() {
        Sh4j.INSTANCE.out("set -e; set +e; echo $-");
        Sh4j.INSTANCE.out("set -o errexit; set +o errexit; echo $-");
        Assertions.assertEquals(String.format("2%n"), Sh4j.INSTANCE.out("set -- a b; echo $#"));
    }

    @Test
    void setOptionRemovalAndPositionalFormsFourth() {
        Sh4j.INSTANCE.out("set -e; set +e; echo $-");
        Sh4j.INSTANCE.out("set -o errexit; set +o errexit; echo $-");
        Sh4j.INSTANCE.out("set -- a b; echo $#");
        Assertions.assertEquals(String.format("0%n"), Sh4j.INSTANCE.out("set --; echo $#"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "errexit", "nounset", "noglob", "xtrace", "noexec", "verbose", "allexport"
        }
    )
    void setOptionNameRoundTrips(final String option) {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(String.format("set -o %s; set +o %s", option, option))
        );
    }

    @Test
    void setUnknownOptionNameFails() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("set -o unknown_option"));
    }

    @Test
    void setNounsetRoundTripClearsFlags() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -o nounset; set +o nounset; echo $-")
        );
    }

    @Test
    void setOptionsAreObservableInFlagsFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o errexit; echo $-").contains("e"));
    }

    @Test
    void setOptionsAreObservableInFlagsSecond() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o nounset; echo $-").contains("u"));
    }

    @Test
    void setOptionsAreObservableInFlagsThird() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Sh4j.INSTANCE.out("set -o nounset; echo $-");
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o noglob; echo $-").contains("f"));
    }

    @Test
    void setOptionsAreObservableInFlagsFourth() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Sh4j.INSTANCE.out("set -o nounset; echo $-");
        Sh4j.INSTANCE.out("set -o noglob; echo $-");
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o xtrace; echo $-").contains("x"));
    }

    @Test
    void setOptionsAreObservableInFlagsFifth() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Sh4j.INSTANCE.out("set -o nounset; echo $-");
        Sh4j.INSTANCE.out("set -o noglob; echo $-");
        Sh4j.INSTANCE.out("set -o xtrace; echo $-");
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -o noexec; echo $-"));
    }

    @Test
    void setOptionsAreObservableInFlagsSixth() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Sh4j.INSTANCE.out("set -o nounset; echo $-");
        Sh4j.INSTANCE.out("set -o noglob; echo $-");
        Sh4j.INSTANCE.out("set -o xtrace; echo $-");
        Sh4j.INSTANCE.out("set -o noexec; echo $-");
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o verbose; echo $-").contains("v"));
    }

    @Test
    void setOptionsAreObservableInFlagsSeventh() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Sh4j.INSTANCE.out("set -o nounset; echo $-");
        Sh4j.INSTANCE.out("set -o noglob; echo $-");
        Sh4j.INSTANCE.out("set -o xtrace; echo $-");
        Sh4j.INSTANCE.out("set -o noexec; echo $-");
        Sh4j.INSTANCE.out("set -o verbose; echo $-");
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o allexport; echo $-").contains("a"));
    }

    @Test
    void setOptionsAreObservableInFlagsEighth() {
        Sh4j.INSTANCE.out("set -o errexit; echo $-");
        Sh4j.INSTANCE.out("set -o nounset; echo $-");
        Sh4j.INSTANCE.out("set -o noglob; echo $-");
        Sh4j.INSTANCE.out("set -o xtrace; echo $-");
        Sh4j.INSTANCE.out("set -o noexec; echo $-");
        Sh4j.INSTANCE.out("set -o verbose; echo $-");
        Sh4j.INSTANCE.out("set -o allexport; echo $-");
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -eu; set +eu; echo $-")
        );
    }

    @Test
    void shiftBoundaryFormsFirst() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1")
        );
    }

    @Test
    void shiftBoundaryFormsSecond() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("shift -1"));
    }

    @Test
    void shiftBoundaryFormsThird() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Sh4j.INSTANCE.run("shift -1");
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("shift nope"));
    }

    @Test
    void shiftBoundaryFormsFourth() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Sh4j.INSTANCE.run("shift -1");
        Sh4j.INSTANCE.run("shift nope");
        Assertions.assertEquals(
            String.format("2 rc=2%n"),
            Sh4j.INSTANCE.out("set -- a b; command shift 99; echo \"$# rc=$?\"")
        );
    }

    @Test
    void shiftBoundaryFormsFifth() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Sh4j.INSTANCE.run("shift -1");
        Sh4j.INSTANCE.run("shift nope");
        Sh4j.INSTANCE.out("set -- a b; command shift 99; echo \"$# rc=$?\"");
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 2; echo $1")
        );
    }

    @Test
    void subshellExitAndWhileSignalsFirst() {
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out("(exit 3); echo $?"));
    }

    @Test
    void subshellExitAndWhileSignalsSecond() {
        Sh4j.INSTANCE.out("(exit 3); echo $?");
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("i=0; while true; do i=$((i+1)); break; done; echo $i")
        );
    }

    @Test
    void subshellExitAndWhileSignalsThird() {
        Sh4j.INSTANCE.out("(exit 3); echo $?");
        Sh4j.INSTANCE.out("i=0; while true; do i=$((i+1)); break; done; echo $i");
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out(
                "i=0; while true; do j=0; while true; do i=$((i+1)); break 2; done; done; echo $i"
            )
        );
    }

    @Test
    void subshellExitAndWhileSignalsFourth() {
        Sh4j.INSTANCE.out("(exit 3); echo $?");
        Sh4j.INSTANCE.out("i=0; while true; do i=$((i+1)); break; done; echo $i");
        Sh4j.INSTANCE.out(
            "i=0; while true; do j=0; while true; do i=$((i+1)); break 2; done; done; echo $i"
        );
        Assertions.assertEquals(
            String.format("3 3%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "i=0; j=0; while [ $i -lt 3 ]; do i=$((i+1)); %s done; echo \"$i $j\"",
                    "while true; do j=$((j+1)); continue 2; done;"
                )
            )
        );
    }

    @Test
    void timesPrintsTwoLines() {
        Assertions.assertEquals(
            2,
            Sh4j.INSTANCE.out("times").split(String.format("%n"), -1).length - 1
        );
    }

    @Test
    void trapListingAndRemovalFormsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                "trap 'echo t' EXIT INT; trap; trap - EXIT INT"
            ).stdout().contains("EXIT")
        );
    }

    @Test
    void trapListingAndRemovalFormsSecond() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                "trap 'echo t' EXIT INT; trap; trap - EXIT INT"
            ).stdout().contains("INT")
        );
    }

    @Test
    void trapListingAndRemovalFormsThird() {
        Sh4j.INSTANCE.runFull("trap 'echo t' EXIT INT; trap; trap - EXIT INT");
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.out("trap '' EXIT; echo body")
        );
    }

    @Test
    void trapReturnCodesAndDotErrorsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("trap"));
    }

    @Test
    void trapReturnCodesAndDotErrorsSecond() {
        Sh4j.INSTANCE.run("trap");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("trap - EXIT"));
    }

    @Test
    void trapReturnCodesAndDotErrorsThird() {
        Sh4j.INSTANCE.run("trap");
        Sh4j.INSTANCE.run("trap - EXIT");
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(". /nonexistent/dotfile").exitCode());
    }

    @Test
    void trapReturnCodesAndDotErrorsFourth() {
        Sh4j.INSTANCE.run("trap");
        Sh4j.INSTANCE.run("trap - EXIT");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(". /nonexistent/dotfile").stderr().contains("cannot open")
        );
    }

    @Test
    void typeResolutionAndEmptyPathSegmentFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("type ls").startsWith("ls is /"));
    }

    @Test
    void typeResolutionAndEmptyPathSegmentSecond() {
        Sh4j.INSTANCE.out("type ls");
        Assertions.assertEquals(
            String.format("viapath%n"),
            Sh4j.INSTANCE.out("PATH=:/bin sh -c 'echo viapath'")
        );
    }

    @Test
    void umaskNumericAndSymbolicFormsFirst() {
        Assertions.assertEquals(String.format("0077%n"), Sh4j.INSTANCE.out("umask 077; umask"));
    }

    @Test
    void umaskNumericAndSymbolicFormsSecond() {
        Sh4j.INSTANCE.out("umask 077; umask");
        Assertions.assertEquals(
            String.format("u=rwx,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 077; umask -S")
        );
    }

    @Test
    void umaskNumericAndSymbolicFormsThird() {
        Sh4j.INSTANCE.out("umask 077; umask");
        Sh4j.INSTANCE.out("umask 077; umask -S");
        Assertions.assertEquals(
            String.format("u=rwx,g=rx,o=rx%n"),
            Sh4j.INSTANCE.out("umask 022; umask -S")
        );
    }

    @Test
    void umaskNumericAndSymbolicFormsFourth() {
        Sh4j.INSTANCE.out("umask 077; umask");
        Sh4j.INSTANCE.out("umask 077; umask -S");
        Sh4j.INSTANCE.out("umask 022; umask -S");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("umask nope"));
    }
}
