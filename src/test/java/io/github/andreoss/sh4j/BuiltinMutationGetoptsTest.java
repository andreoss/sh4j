/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: getopts.
 *
 * @since 1.0
 */
final class BuiltinMutationGetoptsTest {

    @Test
    void getoptsMultiArgumentAndReadEscapeFirst() {
        Assertions.assertEquals(
            String.format("a%nb%ndone%n"),
            Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done")
        );
    }

    @Test
    void getoptsMultiArgumentAndReadEscapeSecond() {
        Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done");
        Assertions.assertEquals(
            String.format("a 2%n? 2%n"),
            Sh4j.INSTANCE.out(
                "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
            )
        );
    }

    @Test
    void getoptsMultiArgumentAndReadEscapeThird() {
        Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done");
        Sh4j.INSTANCE.out(
            "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
        );
        Assertions.assertEquals(
            String.format("[abc]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream("abc\\".getBytes(StandardCharsets.UTF_8)),
                "read x; echo \"[$x]\""
            ).stdout()
        );
    }

    @Test
    void getoptsMultiArgumentAndReadEscapeFourth() {
        Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done");
        Sh4j.INSTANCE.out(
            "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
        );
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream("abc\\".getBytes(StandardCharsets.UTF_8)),
            "read x; echo \"[$x]\""
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -l abc"));
    }

    @Test
    void getoptsMultiArgumentAndReadEscapeFifth() {
        Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done");
        Sh4j.INSTANCE.out(
            "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
        );
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream("abc\\".getBytes(StandardCharsets.UTF_8)),
            "read x; echo \"[$x]\""
        );
        Sh4j.INSTANCE.run("fc -l abc");
        Assertions.assertEquals(
            String.format("? 2%n"),
            Sh4j.INSTANCE.out(
                "set -- -ab; getopts a o; getopts a o; getopts a o; echo \"$o $OPTIND\""
            )
        );
    }

    @Test
    void getoptsMultiArgumentAndReadEscapeSixth() {
        Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done");
        Sh4j.INSTANCE.out(
            "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
        );
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream("abc\\".getBytes(StandardCharsets.UTF_8)),
            "read x; echo \"[$x]\""
        );
        Sh4j.INSTANCE.run("fc -l abc");
        Sh4j.INSTANCE.out("set -- -ab; getopts a o; getopts a o; getopts a o; echo \"$o $OPTIND\"");
        Assertions.assertEquals(
            String.format("? 2%n"),
            Sh4j.INSTANCE.out(
                "set -- -ab x; getopts a o; getopts a o; getopts a o; echo \"$o $OPTIND\""
            )
        );
    }

    @Test
    void getoptsClustersDefaultsAndStopFirst() {
        Assertions.assertEquals(
            String.format("a:%nb:val%n"),
            Sh4j.INSTANCE.out("set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done")
        );
    }

    @Test
    void getoptsClustersDefaultsAndStopSecond() {
        Sh4j.INSTANCE.out("set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done");
        Assertions.assertEquals(
            String.format("?:z%n"),
            Sh4j.INSTANCE.out("set -- -z; getopts a o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsClustersDefaultsAndStopThird() {
        Sh4j.INSTANCE.out("set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done");
        Sh4j.INSTANCE.out("set -- -z; getopts a o; echo \"$o:$OPTARG\"");
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -- --; getopts a o; echo $?")
        );
    }

    @Test
    void getoptsClustersDefaultsAndStopFourth() {
        Sh4j.INSTANCE.out("set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done");
        Sh4j.INSTANCE.out("set -- -z; getopts a o; echo \"$o:$OPTARG\"");
        Sh4j.INSTANCE.out("set -- --; getopts a o; echo $?");
        Assertions.assertEquals(
            String.format("::%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts :a: o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsClustersDefaultsAndStopFifth() {
        Sh4j.INSTANCE.out("set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done");
        Sh4j.INSTANCE.out("set -- -z; getopts a o; echo \"$o:$OPTARG\"");
        Sh4j.INSTANCE.out("set -- --; getopts a o; echo $?");
        Sh4j.INSTANCE.out("set -- -a; getopts :a: o; echo \"$o:$OPTARG\"");
        Assertions.assertEquals(
            String.format("a:val%n"),
            Sh4j.INSTANCE.out("getopts a: o -a val; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsInvalidOptindIsNormalisedFirst() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("OPTIND=bad; set -- -a; getopts a o; echo $?")
        );
    }

    @Test
    void getoptsInvalidOptindIsNormalisedSecond() {
        Sh4j.INSTANCE.out("OPTIND=bad; set -- -a; getopts a o; echo $?");
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("OPTIND=99; set -- -a; getopts a o; echo $?")
        );
    }

    @Test
    void getoptsBoundaryFormsFirst() {
        Assertions.assertEquals(
            String.format("? 1%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o $?\"")
        );
    }

    @Test
    void getoptsBoundaryFormsSecond() {
        Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o $?\"");
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("OPTIND=0; set -- -a; getopts a o; echo $?")
        );
    }

    @Test
    void getoptsBoundaryFormsThird() {
        Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o $?\"");
        Sh4j.INSTANCE.out("OPTIND=0; set -- -a; getopts a o; echo $?");
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; getopts a o; echo $?")
        );
    }

    @Test
    void getoptsMoreBoundariesFirst() {
        Assertions.assertEquals(
            String.format("? 1 2%n"),
            Sh4j.INSTANCE.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"")
        );
    }

    @Test
    void getoptsMoreBoundariesSecond() {
        Sh4j.INSTANCE.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"");
        Assertions.assertEquals(
            String.format("a:val%n"),
            Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsMoreBoundariesThird() {
        Sh4j.INSTANCE.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"");
        Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"");
        Assertions.assertEquals(
            String.format("?:z%n"),
            Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsMoreBoundariesFourth() {
        Sh4j.INSTANCE.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"");
        Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"");
        Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"");
        Assertions.assertEquals(
            String.format("?:%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a: o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsMoreBoundariesFifth() {
        Sh4j.INSTANCE.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"");
        Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"");
        Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"");
        Sh4j.INSTANCE.out("set -- -a; getopts a: o; echo \"$o:$OPTARG\"");
        Assertions.assertEquals(
            String.format("? 1%n"),
            Sh4j.INSTANCE.out("set -- -a file; getopts a o; getopts a o; echo \"$o $?\"")
        );
    }

    @Test
    void getoptsExactBoundaryBranchesFirst() {
        Assertions.assertEquals(
            String.format("?:1:1%n"),
            Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
    }

    @Test
    void getoptsExactBoundaryBranchesSecond() {
        Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"");
        Assertions.assertEquals(
            String.format("?::1%n"),
            Sh4j.INSTANCE.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
    }

    @Test
    void getoptsExactBoundaryBranchesThird() {
        Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"");
        Assertions.assertEquals(
            String.format("?:2:1%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
    }

    @Test
    void getoptsExactBoundaryBranchesFourth() {
        Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o:$OPTIND:$?\"");
        Assertions.assertEquals(
            String.format("?:z%n"),
            Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsExactBoundaryBranchesFifth() {
        Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("set -- -z; getopts a o").stderr().contains("illegal option")
        );
    }

    @Test
    void getoptsExactBoundaryBranchesSixth() {
        Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o:$OPTIND:$?\"");
        Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"");
        Sh4j.INSTANCE.runFull("set -- -z; getopts a o");
        Assertions.assertEquals(
            String.format("a:val%n"),
            Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void hashAndTypeFormsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash echo"));
    }

    @Test
    void hashAndTypeFormsSecond() {
        Sh4j.INSTANCE.run("hash echo");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("hash missing_command"));
    }

    @Test
    void hashAndTypeFormsThird() {
        Sh4j.INSTANCE.run("hash echo");
        Sh4j.INSTANCE.run("hash missing_command");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("type echo"));
    }

    @Test
    void hashAndTypeFormsFourth() {
        Sh4j.INSTANCE.run("hash echo");
        Sh4j.INSTANCE.run("hash missing_command");
        Sh4j.INSTANCE.run("type echo");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("type missing_command"));
    }

    @Test
    void heredocTabStripAndJobLookupBranchesFirst() {
        Assertions.assertEquals(
            String.format("tabbed%n"),
            Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"))
        );
    }

    @Test
    void heredocTabStripAndJobLookupBranchesSecond() {
        Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"));
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("jobs 99"));
    }

    @Test
    void heredocTabStripAndJobLookupBranchesThird() {
        Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"));
        Sh4j.INSTANCE.run("jobs 99");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill -9 999999"));
    }

    @Test
    void heredocTabStripAndJobLookupBranchesFourth() {
        Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"));
        Sh4j.INSTANCE.run("jobs 99");
        Sh4j.INSTANCE.run("kill -9 999999");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill -KILL 999999"));
    }

    @Test
    void heredocTabStripAndJobLookupBranchesFifth() {
        Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"));
        Sh4j.INSTANCE.run("jobs 99");
        Sh4j.INSTANCE.run("kill -9 999999");
        Sh4j.INSTANCE.run("kill -KILL 999999");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v __no_such_cmd__"));
    }

    @Test
    void jobIdBoundaryFormsFirst() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("jobs %0"));
    }

    @Test
    void jobIdBoundaryFormsSecond() {
        Sh4j.INSTANCE.run("jobs %0");
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("jobs -1"));
    }

    @Test
    void jobIdBoundaryFormsThird() {
        Sh4j.INSTANCE.run("jobs %0");
        Sh4j.INSTANCE.run("jobs -1");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("bg %0"));
    }

    @Test
    void jobIdBoundaryFormsFourth() {
        Sh4j.INSTANCE.run("jobs %0");
        Sh4j.INSTANCE.run("jobs -1");
        Sh4j.INSTANCE.run("bg %0");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg %0"));
    }
}
