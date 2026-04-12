/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: plus empty.
 *
 * @since 1.0
 */
final class Sh4jPosixPlusEmptyTest {

    @Test
    void plusEmptyIfUnset() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo ${UNSET_XYZ+alt}"));
    }

    @Test
    void questionMarkIsLastExitCode() {
        Assertions.assertEquals(String.format("42%n"), Sh4j.INSTANCE.out("(exit 42)", "echo $?"));
    }

    @Test
    void questionMarkUpdatedAfterSuccess() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("false", "true", "echo $?")
        );
    }

    @Test
    void readSingleVar() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("hello%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read VAR",
                "echo $VAR"
            )
        );
    }

    @Test
    void readMultipleVars() {
        Assertions.assertEquals(
            String.format("a b c%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("a b c%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read X Y Z",
                "echo $X $Y $Z"
            )
        );
    }

    @Test
    void readRawMode() {
        Assertions.assertEquals(
            String.format("a\\b%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(String.format("a\\b%n").getBytes(StandardCharsets.UTF_8)),
                "read -r VAR",
                "echo $VAR"
            )
        );
    }

    @Test
    void readWithReplay() {
        Assertions.assertEquals(
            String.format("hello world%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("hello world%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read",
                "echo $REPLY"
            )
        );
    }

    @Test
    void readonlyPreventsReassignment() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull("readonly R=hello", "R=world").exitCode());
    }

    @Test
    void setEExitsOnFailure() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; false; echo 1"));
    }

    @Test
    void setEDoesNotExitOnOrList() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; false || true"));
    }

    @Test
    void setEDoesNotExitInIfCondition() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("set -e; if false; then echo yes; fi; echo after")
        );
    }

    @Test
    void setEStopsAcrossSeparateCalls() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e", "false", "echo 1"));
    }

    @Test
    void setEStopsWithoutEchoAcrossSeparateCalls() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -e", "false", "echo 1"));
    }

    @Test
    void setEWithPipelineLastFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; true | false"));
    }

    @Test
    void setEWithNonLastPipeStageFailsOk() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; false | true"));
    }

    @Test
    void setEWithBangInverts() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; ! false"));
    }

    @Test
    void setEWithBangTrueNoExit() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("set -e; ! false; echo after")
        );
    }

    @Test
    void setEWithCompoundAndOrNoExit() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; false || echo ok"));
    }

    @Test
    void setEWithTrueAndThenOk() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("set -e; true && echo ok")
        );
    }

    @Test
    void shiftRemovesFirstParam() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift; echo $1")
        );
    }

    @Test
    void shiftByN() {
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 2; echo $1")
        );
    }

    @Test
    void simpleCommand() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true"));
    }

    @Test
    void singleQuotesPreserveLiterals() {
        Assertions.assertEquals(
            String.format("hello * ?%n"),
            Sh4j.INSTANCE.out("echo 'hello * ?'")
        );
    }

    @Test
    void subshellDoesNotAffectParent() {
        Assertions.assertEquals(
            String.format("outer%n"),
            Sh4j.INSTANCE.out("X=outer; (X=inner); echo $X")
        );
    }

    @Test
    void subshellCanReadParentVars() {
        Assertions.assertEquals(String.format("outer%n"), Sh4j.INSTANCE.out("X=outer; (echo $X)"));
    }

    @Test
    void unaliasRemovesAlias() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.runFull("alias ll='echo hello'", "unalias ll", "ll").exitCode()
        );
    }

    @Test
    void unaliasAll() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.runFull(
                "alias ll='echo hello'",
                "alias la='echo world'",
                "unalias -a",
                "ll"
            ).exitCode()
        );
    }

    @Test
    void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "value");
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out(env, "unset X", "echo $X"));
    }

    @Test
    void untilLoop() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out("i=1; until [ $i -gt 2 ]; do echo $i; i=$((i+1)); done")
        );
    }

    @Test
    void whileLoop() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out("i=1; while [ $i -le 2 ]; do echo $i; i=$((i+1)); done")
        );
    }
}
