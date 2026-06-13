/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: {@code break} and {@code continue} leave or resume
 * only lexically enclosing loops, the outermost one when the operand is
 * greater than their number (POSIX break, continue).
 *
 * @since 1.0
 */
final class PosixLoopControlTest {

    @Test
    void breakBeyondDepthExitsOutermostLoop() {
        Assertions.assertEquals(
            String.format("1a%nend%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do for j in a b; do echo \"$i$j\"; break 5; done; done; echo end"
            )
        );
    }

    @Test
    void continueBeyondDepthResumesOutermostLoop() {
        Assertions.assertEquals(
            String.format("1%n2%n3%nend%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2 3; do echo \"$i\"; continue 9; echo no; done; echo end"
            )
        );
    }

    @Test
    void continueBeyondDepthResumesOutermostWhileLoop() {
        Assertions.assertEquals(
            String.format("1%n2%nend%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    " ",
                    "n=0; while [ \"$n\" -lt 2 ]; do n=$((n+1)); for j in a b; do",
                    "echo \"$n\"; continue 4; done; echo no; done; echo end"
                )
            )
        );
    }

    @Test
    void breakInFunctionLeavesCallerLoopAlone() {
        Assertions.assertEquals(
            String.format("1%n2%nend%n"),
            Sh4j.INSTANCE.out(
                "f() { break; }; for i in 1 2; do echo \"$i\"; f; done; echo end"
            )
        );
    }

    @Test
    void continueInFunctionLeavesCallerLoopAlone() {
        Assertions.assertEquals(
            String.format("1%nx%n2%nx%nend%n"),
            Sh4j.INSTANCE.out(
                "f() { continue; }; for i in 1 2; do echo \"$i\"; f; echo x; done; echo end"
            )
        );
    }

    @Test
    void breakInFunctionIsSilent() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                "f() { break; }; for i in 1 2; do f; done"
            ).stderr()
        );
    }

    @Test
    void breakBeyondFunctionLoopsStaysInFunction() {
        Assertions.assertEquals(
            String.format("a%ninf%n1%na%ninf%n2%nend%n"),
            Sh4j.INSTANCE.out(
                "f() { for j in a b; do echo \"$j\"; break 3; done; echo inf; };",
                "for i in 1 2; do f; echo \"$i\"; done; echo end"
            )
        );
    }

    @Test
    void breakInSubshellIsSilent() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("for i in 1 2; do (break); echo \"$i\"; done").stderr()
        );
    }

    @Test
    void breakInSubshellLeavesParentLoopRunning() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do (break); echo \"$i\"; done")
        );
    }

    @Test
    void breakInSubshellEndsSubshell() {
        Assertions.assertEquals(
            String.format("1%nout%n2%nout%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do echo \"$i\"; (break 2; echo in); echo out; done"
            )
        );
    }

    @Test
    void breakInSubshellSucceeds() {
        Assertions.assertEquals(
            String.format("1 0%n"),
            Sh4j.INSTANCE.out("for i in 1; do (false; break); echo \"$i $?\"; done")
        );
    }

    @Test
    void breakInSubstitutionEndsSubstitution() {
        Assertions.assertEquals(
            String.format("[]%n"),
            Sh4j.INSTANCE.out("for i in 1; do x=$(break; echo in); echo \"[$x]\"; done")
        );
    }

    @Test
    void breakInPipelineLeavesParentLoopRunning() {
        Assertions.assertEquals(
            String.format("1%n2%nend%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do { break; echo no; } | cat; echo \"$i\"; done; echo end"
            )
        );
    }

    @Test
    void breakZeroIsError() {
        Assertions.assertEquals(
            2,
            Sh4j.INSTANCE.run("for i in 1 2; do break 0; done; echo end")
        );
    }

    @Test
    void breakZeroEndsShell() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do echo \"$i\"; break 0; done; echo end")
        );
    }

    @Test
    void breakZeroIsReported() {
        Assertions.assertEquals(
            String.format("sh: break: Illegal number: 0%n"),
            Sh4j.INSTANCE.runFull("break 0").stderr()
        );
    }

    @Test
    void breakWordIsError() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("break abc; echo end"));
    }

    @Test
    void continueNegativeIsError() {
        Assertions.assertEquals(
            2,
            Sh4j.INSTANCE.run("for i in 1 2; do continue -1; done; echo end")
        );
    }

    @Test
    void continueZeroIsReported() {
        Assertions.assertEquals(
            String.format("sh: continue: Illegal number: 0%n"),
            Sh4j.INSTANCE.runFull("for i in 1; do continue 0; done").stderr()
        );
    }

    @Test
    void returnFromLoopLeavesNoLoopBehind() {
        Assertions.assertEquals(
            String.format("k1%nk2%nafter 0%n"),
            Sh4j.INSTANCE.out(
                "f() { for i in 1; do return 3; done; };",
                "for k in 1 2; do f; echo \"k$k\"; done; f; break; echo \"after $?\""
            )
        );
    }

    @Test
    void returnFromWhileLeavesNoLoopBehind() {
        Assertions.assertEquals(
            "sh: break: not in a loop",
            Sh4j.INSTANCE.runFull(
                "f() { while :; do return 3; done; }; f; break"
            ).stderr().trim()
        );
    }

    @Test
    void topLevelBreakAfterLoopContinues() {
        Assertions.assertEquals(
            String.format("after 0%n"),
            Sh4j.INSTANCE.out("while :; do break; done; break; echo \"after $?\"")
        );
    }

    @Test
    void evalBreaksCallerLoop() {
        Assertions.assertEquals(
            String.format("1%nend%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do echo \"$i\"; eval break; echo after; done; echo end"
            )
        );
    }
}
