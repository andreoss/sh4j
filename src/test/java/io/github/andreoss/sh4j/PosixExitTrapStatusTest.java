/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: an {@code exit} in the action of the
 * {@code EXIT} trap sets the exit status (POSIX exit, trap).
 *
 * @since 1.0
 */
final class PosixExitTrapStatusTest {

    @Test
    void setsSubshellStatus() {
        Assertions.assertEquals(
            String.format("w%n7%n"),
            Sh4j.INSTANCE.out("(trap 'exit 7' EXIT; echo w); echo \"$?\"")
        );
    }

    @Test
    void setsShellStatus() {
        Assertions.assertEquals(
            7,
            Sh4j.INSTANCE.run("trap 'exit 7' EXIT; echo w")
        );
    }

    @Test
    void overridesShellExitStatus() {
        Assertions.assertEquals(
            7,
            Sh4j.INSTANCE.run("trap 'exit 7' EXIT; exit 3")
        );
    }

    @Test
    void overridesSubshellExitStatus() {
        Assertions.assertEquals(
            String.format("7%n"),
            Sh4j.INSTANCE.out("(trap 'exit 7' EXIT; exit 3); echo \"$?\"")
        );
    }

    @Test
    void setsSubstitutionStatus() {
        Assertions.assertEquals(
            String.format("5 v%n"),
            Sh4j.INSTANCE.out("x=$(trap 'exit 5' EXIT; echo v); echo \"$? $x\"")
        );
    }

    @Test
    void setsPipelineStageStatus() {
        Assertions.assertEquals(
            String.format("7%n"),
            Sh4j.INSTANCE.out(": | { trap 'exit 7' EXIT; :; }; echo \"$?\"")
        );
    }

    @Test
    void setsBackgroundJobStatus() {
        Assertions.assertEquals(
            String.format("7%n"),
            Sh4j.INSTANCE.out("(trap 'exit 7' EXIT; :) & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void keepsStatusWithoutExit() {
        Assertions.assertEquals(
            String.format("t%n4%n"),
            Sh4j.INSTANCE.out("(trap 'echo t' EXIT; exit 4); echo \"$?\"")
        );
    }

    @Test
    void keepsShellStatusWithoutExit() {
        Assertions.assertEquals(
            4,
            Sh4j.INSTANCE.run("trap 'true' EXIT; exit 4")
        );
    }

    @Test
    void bareExitUsesExitingStatus() {
        Assertions.assertEquals(
            String.format("6%n"),
            Sh4j.INSTANCE.out("(trap 'exit' EXIT; exit 6); echo \"$?\"")
        );
    }

    @Test
    void bareExitIgnoresTrapCommands() {
        Assertions.assertEquals(
            String.format("6%n"),
            Sh4j.INSTANCE.out("(trap 'false; exit' EXIT; exit 6); echo \"$?\"")
        );
    }

    @Test
    void bareExitInFunctionUsesExitingStatus() {
        Assertions.assertEquals(
            3,
            Sh4j.INSTANCE.run("trap 'f() { exit; }; false; f' EXIT; exit 3")
        );
    }

    @Test
    void bareExitAfterTrapUsesLastStatus() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("(trap 'exit 2' EXIT; exit 6); (false; exit); echo \"$?\"")
        );
    }

    @Test
    void evalExitSetsStatus() {
        Assertions.assertEquals(
            4,
            Sh4j.INSTANCE.run("trap 'eval \"exit 4\"' EXIT; exit 3")
        );
    }

    @Test
    void errexitInTrapSetsStatus() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("set -e; trap 'false; echo x' EXIT; exit 3")
        );
    }

    @Test
    void runsTrapOnceWhenItExits() {
        Assertions.assertEquals(
            String.format("t%n7%n"),
            Sh4j.INSTANCE.out("(trap 'echo t; exit 7' EXIT; exit 3); echo \"$?\"")
        );
    }

    @Test
    void runsShellTrapOnceWhenItExits() {
        Assertions.assertEquals(
            String.format("t%n"),
            Sh4j.INSTANCE.out("trap 'echo t; exit 7' EXIT; exit 3")
        );
    }

    @Test
    void scriptReturnsTrapStatus() {
        Assertions.assertEquals(
            7,
            Main.runSource("trap 'exit 7' EXIT; exit 3", Collections.singletonList("sh"))
        );
    }
}
