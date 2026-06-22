/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link Sh4j}: a trap condition given as a number, a name in any
 * case or {@code 0} matches the signal however it is sent, and the
 * {@code trap} listing names it, orders it by signal number and quotes the
 * action for re-input (POSIX trap).
 *
 * @since 1.0
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
final class PosixTrapConditionTest {

    @Test
    void runsNumberedTrapOnDefaultKill() {
        Assertions.assertEquals(
            String.format("t%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo t' 15; kill \"$$\"; echo after")
        );
    }

    @Test
    void runsNamedTrapOnNumberedKill() {
        Assertions.assertEquals(
            String.format("t%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo t' TERM; kill -15 \"$$\"; echo after")
        );
    }

    @Test
    void runsNamedTrapOnNamedKill() {
        Assertions.assertEquals(
            String.format("t%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo t' TERM; kill -s TERM \"$$\"; echo after")
        );
    }

    @Test
    void rejectsSignalPrefixedCondition() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("trap 'echo t' SIGTERM 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void runsNamedUserTrapOnNamedKill() {
        Assertions.assertEquals(
            String.format("u%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo u' USR1; kill -USR1 \"$$\"; echo after")
        );
    }

    @Test
    void runsNumberedUserTrapOnNumberedKill() {
        Assertions.assertEquals(
            String.format("u%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo u' 10; kill -10 \"$$\"; echo after")
        );
    }

    @Test
    void runsNumberedUserTrapOnNamedKill() {
        Assertions.assertEquals(
            String.format("u%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo u' 10; kill -s USR1 \"$$\"; echo after")
        );
    }

    @Test
    void runsLowercaseTrapOnDefaultKill() {
        Assertions.assertEquals(
            String.format("t%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo t' term; kill \"$$\"; echo after")
        );
    }

    @Test
    void listsNumberedConditionByName() {
        Assertions.assertEquals(
            String.format("trap -- 'x' TERM%n"),
            Sh4j.INSTANCE.out("trap 'x' 15; trap")
        );
    }

    @Test
    void listsLowercaseConditionByName() {
        Assertions.assertEquals(
            String.format("trap -- 'x' TERM%n"),
            Sh4j.INSTANCE.out("trap 'x' term; trap")
        );
    }

    @Test
    void listsZeroConditionAsExit() {
        Assertions.assertEquals(
            String.format("trap -- ':' EXIT%n"),
            Sh4j.INSTANCE.out("trap ':' 0; trap")
        );
    }

    @Test
    void listsTrapsBySignalNumber() {
        Assertions.assertEquals(
            String.format(
                "trap -- ':' EXIT%ntrap -- ':' HUP%ntrap -- ':' INT%ntrap -- ':' USR1%n"
            ),
            Sh4j.INSTANCE.out("trap ':' USR1 HUP 0 INT; trap")
        );
    }

    @Test
    void listsActionQuotedForReinput() {
        Assertions.assertEquals(
            String.format("trap -- 'echo '\\''a'\\''' TERM%n"),
            Sh4j.INSTANCE.out("trap \"echo 'a'\" TERM; trap")
        );
    }

    @Test
    void resetsNamedTrapByNumber() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("trap 'echo t' TERM; trap - 15; trap")
        );
    }

    @Test
    void resetsNumberedTrapByName() {
        Assertions.assertEquals(
            143,
            Sh4j.INSTANCE.run("trap 'echo t' 15; trap - TERM; kill \"$$\"; echo after")
        );
    }

    @Test
    void ignoresSignalWithEmptyAction() {
        Assertions.assertEquals(
            String.format("trap -- '' INT%nafter%n"),
            Sh4j.INSTANCE.out("trap '' INT; trap; kill -INT \"$$\"; echo after")
        );
    }
}
