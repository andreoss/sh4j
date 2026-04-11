/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Additional built-in boundary tests selected from PIT survivors.
 *
 * @since 1.0
 */
final class BuiltinMutationTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void unsetOptionFormsFirst() {
        Assertions.assertNull(Sh4j.INSTANCE.getVar("X", "X=1; unset -v X"));
    }

    @Test
    void unsetOptionFormsSecond() {
        Assertions.assertNull(Sh4j.INSTANCE.getVar("X", "X=1; unset -- X"));
    }

    @Test
    void unsetOptionFormsThird() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("f() { :; }; unset -f f"));
    }

    @Test
    void unsetOptionFormsFourth() {
        Sh4j.INSTANCE.run("f() { :; }; unset -f f");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("X=1; unset -z X"));
    }

    @Test
    void exportBareAndPrintFormsFirst() {
        Assertions.assertEquals("", Sh4j.INSTANCE.getVar("BARE", "export BARE"));
    }

    @Test
    void exportBareAndPrintFormsSecond() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("export BARE; sh -c 'echo ${BARE-unset}'")
        );
    }

    @Test
    void exportBareAndPrintFormsThird() {
        Sh4j.INSTANCE.out("export BARE; sh -c 'echo ${BARE-unset}'");
        Assertions.assertTrue(Sh4j.INSTANCE.out("export -p").contains("export PATH="));
    }

    @Test
    void pwdLogicalAndPhysicalFormsFirst() throws IOException {
        final Path real = new TempDirs(BuiltinMutationTest.root).folder(
            "real"
        ).toPath().toRealPath();
        Assertions.assertEquals(
            real + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -L", real))
        );
    }

    @Test
    void pwdLogicalAndPhysicalFormsSecond() throws IOException {
        final Path real = new TempDirs(BuiltinMutationTest.root).folder(
            "real"
        ).toPath().toRealPath();
        Sh4j.INSTANCE.out(String.format("cd %s; pwd -L", real));
        Assertions.assertEquals(
            real + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -P", real))
        );
    }

    @Test
    void echoNoNewlineAndEmptyFormsFirst() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("echo -n"));
    }

    @Test
    void echoNoNewlineAndEmptyFormsSecond() {
        Sh4j.INSTANCE.out("echo -n");
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo"));
    }

    @Test
    void echoNoNewlineAndEmptyFormsThird() {
        Sh4j.INSTANCE.out("echo -n");
        Sh4j.INSTANCE.out("echo");
        Assertions.assertEquals("a b", Sh4j.INSTANCE.out("echo -n a b"));
    }

    @Test
    void evalEmptyAndNonEmptyFormsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("eval"));
    }

    @Test
    void evalEmptyAndNonEmptyFormsSecond() {
        Sh4j.INSTANCE.run("eval");
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("eval echo hi"));
    }

    @Test
    void evalEmptyAndNonEmptyFormsThird() {
        Sh4j.INSTANCE.run("eval");
        Sh4j.INSTANCE.out("eval echo hi");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("eval ''"));
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
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("shift -1"));
    }

    @Test
    void shiftBoundaryFormsThird() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Sh4j.INSTANCE.run("shift -1");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("shift nope"));
    }

    @Test
    void shiftBoundaryFormsFourth() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Sh4j.INSTANCE.run("shift -1");
        Sh4j.INSTANCE.run("shift nope");
        Assertions.assertEquals(
            String.format("2 rc=1%n"),
            Sh4j.INSTANCE.out("set -- a b; shift 99; echo \"$# rc=$?\"")
        );
    }

    @Test
    void shiftBoundaryFormsFifth() {
        Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1");
        Sh4j.INSTANCE.run("shift -1");
        Sh4j.INSTANCE.run("shift nope");
        Sh4j.INSTANCE.out("set -- a b; shift 99; echo \"$# rc=$?\"");
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 2; echo $1")
        );
    }

    @Test
    void dispatcherUncoveredBranchesFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("f() { return abc; }; f"));
    }

    @Test
    void dispatcherUncoveredBranchesSecond() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done"
            )
        );
    }

    @Test
    void dispatcherUncoveredBranchesThird() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("unset HOME; cd"));
    }

    @Test
    void dispatcherUncoveredBranchesFourth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("umask 999"));
    }

    @Test
    void dispatcherUncoveredBranchesFifth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("wait abc"));
    }

    @Test
    void dispatcherUncoveredBranchesSixth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Assertions.assertEquals(
            String.format("f is a function%n"),
            Sh4j.INSTANCE.out("f() { :; }; type f")
        );
    }

    @Test
    void dispatcherUncoveredBranchesSeventh() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Assertions.assertTrue(Sh4j.INSTANCE.out("command -v ls").startsWith("/"));
    }

    @Test
    void dispatcherUncoveredBranchesEighth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Sh4j.INSTANCE.out("command -v ls");
        Assertions.assertEquals(
            String.format("f%n"),
            Sh4j.INSTANCE.out("f() { :; }; command -v f")
        );
    }

    @Test
    void dispatcherUncoveredBranchesNinth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Sh4j.INSTANCE.out("command -v ls");
        Sh4j.INSTANCE.out("f() { :; }; command -v f");
        Assertions.assertEquals(
            String.format("a=x b=[]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("x%n").getBytes(StandardCharsets.UTF_8)),
                "read a b; echo \"a=$a b=[$b]\""
            ).stdout()
        );
    }

    @Test
    void dispatcherUncoveredBranchesTenth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Sh4j.INSTANCE.out("command -v ls");
        Sh4j.INSTANCE.out("f() { :; }; command -v f");
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format("x%n").getBytes(StandardCharsets.UTF_8)),
            "read a b; echo \"a=$a b=[$b]\""
        );
        Assertions.assertEquals(
            String.format("[][]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("x%n").getBytes(StandardCharsets.UTF_8)),
                "read a b c; echo \"[$b][$c]\""
            ).stdout()
        );
    }

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
            String.format("[abc\\]%n"),
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
    void readInputLineSurvivesIOException() {
        Assertions.assertEquals(
            String.format("rc=1 x=[]%n"),
            Sh4j.INSTANCE.runFull(
                BuiltinMutationTest.failingInput(),
                "read x; echo \"rc=$? x=[$x]\""
            ).stdout()
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
    void umaskSymbolicFallbackAndAliasWithAssignmentsFirst() {
        Assertions.assertEquals(
            String.format("u=,g=,o=%n"),
            Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S")
        );
    }

    @Test
    void umaskSymbolicFallbackAndAliasWithAssignmentsSecond() {
        Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S");
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("set foo; echo $#"));
    }

    @Test
    void umaskSymbolicFallbackAndAliasWithAssignmentsThird() {
        Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S");
        Sh4j.INSTANCE.out("set foo; echo $#");
        Assertions.assertEquals(
            String.format("b='echo'%n"),
            Sh4j.INSTANCE.out("A=1 alias b=echo; alias b")
        );
    }

    @Test
    void literalBracesAndJobStateRefreshFirst() {
        Assertions.assertEquals(String.format("${}%n"), Sh4j.INSTANCE.out("x=1; echo \"${}\""));
    }

    @Test
    void literalBracesAndJobStateRefreshSecond() {
        Sh4j.INSTANCE.out("x=1; echo \"${}\"");
        Assertions.assertEquals(String.format("${ x}%n"), Sh4j.INSTANCE.out("x=1; echo \"${ x}\""));
    }

    @Test
    void literalBracesAndJobStateRefreshThird() {
        Sh4j.INSTANCE.out("x=1; echo \"${}\"");
        Sh4j.INSTANCE.out("x=1; echo \"${ x}\"");
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & sleep 0.3; jobs").contains("Done true"));
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
    void readCustomIfsAndWhitespaceFirst() {
        Assertions.assertEquals(
            String.format("a=x b=y:z%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readCustomIfsAndWhitespaceSecond() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assertions.assertEquals(
            String.format("a=x b=y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("   x   y   %n").getBytes(StandardCharsets.UTF_8)
                ),
                "read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readCustomIfsAndWhitespaceThird() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(
                String.format("   x   y   %n").getBytes(StandardCharsets.UTF_8)
            ),
            "read a b; echo \"a=$a b=$b\""
        );
        Assertions.assertEquals(
            String.format("a=x b=:%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("x::%n").getBytes(StandardCharsets.UTF_8)),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
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
    void jobsWithExplicitAndMissingIdsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait").stdout().contains("sleep")
        );
    }

    @Test
    void jobsWithExplicitAndMissingIdsSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs 999"));
    }

    @Test
    void jobsWithExplicitAndMissingIdsThird() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait");
        Sh4j.INSTANCE.run("jobs 999");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs notanumber"));
    }

    @Test
    void fgExplicitAndMissingIdsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1").stdout().contains("sleep")
        );
    }

    @Test
    void fgExplicitAndMissingIdsSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg 999"));
    }

    @Test
    void fgExplicitAndMissingIdsThird() {
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Sh4j.INSTANCE.run("fg 999");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg notanumber"));
    }

    @Test
    void fcListAndSubstituteFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l").stdout().contains("echo one")
        );
    }

    @Test
    void fcListAndSubstituteSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo hello", "fc -s hello=bye").stdout().contains("bye")
        );
    }

    @Test
    void fcErrorFormsFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -e"));
    }

    @Test
    void fcErrorFormsSecond() {
        Sh4j.INSTANCE.run("fc -e");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -z"));
    }

    @Test
    void fcErrorFormsThird() {
        Sh4j.INSTANCE.run("fc -e");
        Sh4j.INSTANCE.run("fc -z");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -s"));
    }

    @Test
    void fcErrorFormsFourth() {
        Sh4j.INSTANCE.run("fc -e");
        Sh4j.INSTANCE.run("fc -z");
        Sh4j.INSTANCE.run("fc -s");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -l"));
    }

    @Test
    void fcErrorFormsFifth() {
        Sh4j.INSTANCE.run("fc -e");
        Sh4j.INSTANCE.run("fc -z");
        Sh4j.INSTANCE.run("fc -s");
        Sh4j.INSTANCE.run("fc -l");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -s broken"));
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

    @Test
    void aliasFormsFirst() {
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("alias a='echo hi'", "a"));
    }

    @Test
    void aliasFormsSecond() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Assertions.assertEquals(
            String.format("a='echo hi'%n"),
            Sh4j.INSTANCE.out("alias a='echo hi'; alias a")
        );
    }

    @Test
    void aliasFormsThird() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Sh4j.INSTANCE.out("alias a='echo hi'; alias a");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("alias missing"));
    }

    @Test
    void aliasFormsFourth() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Sh4j.INSTANCE.out("alias a='echo hi'; alias a");
        Sh4j.INSTANCE.run("alias missing");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("alias a=b; unalias a"));
    }

    @Test
    void aliasFormsFifth() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Sh4j.INSTANCE.out("alias a='echo hi'; alias a");
        Sh4j.INSTANCE.run("alias missing");
        Sh4j.INSTANCE.run("alias a=b; unalias a");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("alias a=b; unalias -a"));
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
    void dotEmptyAndPositionalFormsFirst() throws IOException {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("."));
    }

    @Test
    void dotEmptyAndPositionalFormsSecond() throws IOException {
        Sh4j.INSTANCE.run(".");
        final Path lib = new TempDirs(BuiltinMutationTest.root).file("dot.sh").toPath();
        Files.write(lib, String.format("echo \"$1-$2\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("a-b%n"),
            Sh4j.INSTANCE.out(String.format(". %s a b", lib))
        );
    }

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
    void commandLocateAndExecuteFormsFirst() {
        Assertions.assertEquals(String.format("echo%n"), Sh4j.INSTANCE.out("command -v echo"));
    }

    @Test
    void commandLocateAndExecuteFormsSecond() {
        Sh4j.INSTANCE.out("command -v echo");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v missing_command"));
    }

    @Test
    void commandLocateAndExecuteFormsThird() {
        Sh4j.INSTANCE.out("command -v echo");
        Sh4j.INSTANCE.run("command -v missing_command");
        Assertions.assertEquals(
            String.format("echo is a shell built-in%n"),
            Sh4j.INSTANCE.out("command -V echo")
        );
    }

    @Test
    void commandLocateAndExecuteFormsFourth() {
        Sh4j.INSTANCE.out("command -v echo");
        Sh4j.INSTANCE.run("command -v missing_command");
        Sh4j.INSTANCE.out("command -V echo");
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("command echo hi"));
    }

    @Test
    void killOptionFormsFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill"));
    }

    @Test
    void killOptionFormsSecond() {
        Sh4j.INSTANCE.run("kill");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill -s"));
    }

    @Test
    void killOptionFormsThird() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill notapid"));
    }

    @Test
    void killOptionFormsFourth() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Sh4j.INSTANCE.run("kill notapid");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -s TERM 999999"));
    }

    @Test
    void killOptionFormsFifth() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Sh4j.INSTANCE.run("kill notapid");
        Sh4j.INSTANCE.run("kill -s TERM 999999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
    }

    @Test
    void killOptionFormsSixth() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Sh4j.INSTANCE.run("kill notapid");
        Sh4j.INSTANCE.run("kill -s TERM 999999");
        Sh4j.INSTANCE.run("kill -9 999999");
        Assertions.assertTrue(Sh4j.INSTANCE.out("kill -l").contains("TERM"));
    }

    @Test
    void killTermExitsZero() throws Exception {
        final Process term = new ProcessBuilder("sleep", "30").start();
        try {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill %d", term.toHandle().pid()))
            );
        } finally {
            term.destroyForcibly();
        }
    }

    @Test
    void killTermStopsProcess() throws Exception {
        final Process term = new ProcessBuilder("sleep", "30").start();
        try {
            Sh4j.INSTANCE.run(String.format("kill %d", term.toHandle().pid()));
            Assertions.assertTrue(term.waitFor(5, TimeUnit.SECONDS));
        } finally {
            term.destroyForcibly();
        }
    }

    @Test
    void killNineExitsZero() throws Exception {
        final Process force = new ProcessBuilder("sleep", "30").start();
        try {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill -9 %d", force.toHandle().pid()))
            );
        } finally {
            force.destroyForcibly();
        }
    }

    @Test
    void killNineStopsProcess() throws Exception {
        final Process force = new ProcessBuilder("sleep", "30").start();
        try {
            Sh4j.INSTANCE.run(String.format("kill -9 %d", force.toHandle().pid()));
            Assertions.assertTrue(force.waitFor(5, TimeUnit.SECONDS));
        } finally {
            force.destroyForcibly();
        }
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
    void umaskAllBitCombinationsFirst() {
        Assertions.assertEquals(
            String.format("u=rwx,g=rwx,o=rwx%n"),
            Sh4j.INSTANCE.out("umask 000; umask -S")
        );
    }

    @Test
    void umaskAllBitCombinationsSecond() {
        Sh4j.INSTANCE.out("umask 000; umask -S");
        Assertions.assertEquals(
            String.format("u=,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 777; umask -S")
        );
    }

    @Test
    void umaskAllBitCombinationsThird() {
        Sh4j.INSTANCE.out("umask 000; umask -S");
        Sh4j.INSTANCE.out("umask 777; umask -S");
        Assertions.assertEquals(
            String.format("u=rwx,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 077; umask -S")
        );
    }

    @Test
    void umaskAllBitCombinationsFourth() {
        Sh4j.INSTANCE.out("umask 000; umask -S");
        Sh4j.INSTANCE.out("umask 777; umask -S");
        Sh4j.INSTANCE.out("umask 077; umask -S");
        Assertions.assertEquals(
            String.format("u=rwx,g=r,o=r%n"),
            Sh4j.INSTANCE.out("umask 033; umask -S")
        );
    }

    @Test
    void readMoreSplittingFormsFirst() {
        Assertions.assertEquals(
            String.format("a= b=x:y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format(":x:y%n").getBytes(StandardCharsets.UTF_8)),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readMoreSplittingFormsSecond() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format(":x:y%n").getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assertions.assertEquals(
            String.format("a=x b=y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x : y%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=' :'; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void fcListModesAndRangesFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -n -l").stdout().contains("\techo one")
        );
    }

    @Test
    void fcListModesAndRangesSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -n -l");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l 1 1").stdout().contains("echo one")
        );
    }

    @Test
    void fcListModesAndRangesThird() {
        Sh4j.INSTANCE.runFull("echo one", "fc -n -l");
        Sh4j.INSTANCE.runFull("echo one", "fc -l 1 1");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -e true").stdout().contains("echo one")
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "errexit", "nounset", "noglob", "xtrace", "noexec", "verbose", "allexport",
        }
    )
    void setOptionNameRoundTrips(final String option) {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(String.format("set -o %s; set +o %s", option, option))
        );
    }

    @Test
    void setUnknownOptionNameSucceeds() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -o unknown_option"));
    }

    @Test
    void setNounsetRoundTripClearsFlags() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -o nounset; set +o nounset; echo $-")
        );
    }

    @Test
    void dotRestoresPositionalParameters() throws IOException {
        final Path lib = new TempDirs(BuiltinMutationTest.root).file("restore.sh").toPath();
        Files.write(lib, String.format("echo \"in=$1\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("in=inner%nouter%n"),
            Sh4j.INSTANCE.out(String.format("set -- outer; . %s inner; echo $1", lib))
        );
    }

    @Test
    void echoReportsExpansionError() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo ${MISSING:?required}"));
    }

    @Test
    void pwdFlagOrder() throws IOException {
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder(
            "pwdorder"
        ).toPath().toRealPath();
        Assertions.assertEquals(
            dir + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -P -L", dir))
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
    void jobsBgFgExplicitIdsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1").stdout().contains("sleep")
        );
    }

    @Test
    void jobsBgFgExplicitIdsSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1").stdout().contains("sleep")
        );
    }

    @Test
    void jobsBgFgExplicitIdsThird() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1");
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("bg %999"));
    }

    @Test
    void jobsBgFgExplicitIdsFourth() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1");
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Sh4j.INSTANCE.run("bg %999");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg %999"));
    }

    @Test
    void aliasQuotedAndEmptyValuesFirst() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("alias e=''", "e"));
    }

    @Test
    void aliasQuotedAndEmptyValuesSecond() {
        Sh4j.INSTANCE.out("alias e=''", "e");
        Assertions.assertEquals(
            String.format("a='x y'%n"),
            Sh4j.INSTANCE.out("alias a=\"x y\"; alias a")
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
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o noexec; echo $-").contains("n"));
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
    void fcEditorAndRangeBoundariesFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -e true 1").stdout().contains("echo one")
        );
    }

    @Test
    void fcEditorAndRangeBoundariesSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -e true 1");
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 3 2").exitCode());
    }

    @Test
    void fcEditorAndRangeBoundariesThird() {
        Sh4j.INSTANCE.runFull("echo one", "fc -e true 1");
        Sh4j.INSTANCE.runFull("echo one", "fc -l 3 2");
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -e true 1 1").exitCode());
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
    void breakZeroUsesOneLevel() {
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 0; done; done")
        );
    }

    @Test
    void jobIdBoundaryFormsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs %0"));
    }

    @Test
    void jobIdBoundaryFormsSecond() {
        Sh4j.INSTANCE.run("jobs %0");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs -1"));
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

    @Test
    void killTermOnStubbornExitsZero() throws Exception {
        final Process stubborn = BuiltinMutationTest.stubborn();
        try {
            Thread.sleep(200L);
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill %d", stubborn.toHandle().pid()))
            );
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void killTermLeavesStubbornAlive() throws Exception {
        final Process stubborn = BuiltinMutationTest.stubborn();
        try {
            Thread.sleep(200L);
            Sh4j.INSTANCE.run(String.format("kill %d", stubborn.toHandle().pid()));
            Thread.sleep(200L);
            Assertions.assertTrue(stubborn.isAlive(), "TERM-ignoring process should survive");
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void killKillOnStubbornExitsZero() throws Exception {
        final Process stubborn = BuiltinMutationTest.stubborn();
        try {
            Thread.sleep(200L);
            final long pid = stubborn.toHandle().pid();
            Sh4j.INSTANCE.run(String.format("kill %d", pid));
            Thread.sleep(200L);
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill -KILL %d", pid))
            );
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void killKillStopsStubborn() throws Exception {
        final Process stubborn = BuiltinMutationTest.stubborn();
        try {
            Thread.sleep(200L);
            final long pid = stubborn.toHandle().pid();
            Sh4j.INSTANCE.run(String.format("kill %d", pid));
            Thread.sleep(200L);
            Sh4j.INSTANCE.run(String.format("kill -KILL %d", pid));
            Assertions.assertTrue(stubborn.waitFor(5L, TimeUnit.SECONDS));
        } finally {
            stubborn.destroyForcibly();
        }
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
    void fcRangeEdgesAndSubstitutionFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 0").exitCode());
    }

    @Test
    void fcRangeEdgesAndSubstitutionSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l 0");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l -1").stdout().contains("echo one")
        );
    }

    @Test
    void fcRangeEdgesAndSubstitutionThird() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l 0");
        Sh4j.INSTANCE.runFull("echo one", "fc -l -1");
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 5").exitCode());
    }

    @Test
    void fcRangeEdgesAndSubstitutionFourth() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l 0");
        Sh4j.INSTANCE.runFull("echo one", "fc -l -1");
        Sh4j.INSTANCE.runFull("echo one", "fc -l 5");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -s one=two").stdout().contains("two")
        );
    }

    @Test
    void jobsWithoutIdsUseMostRecentJobFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("sleep 0.1 & jobs").stdout().contains("sleep"));
    }

    @Test
    void jobsWithoutIdsUseMostRecentJobSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.1 & jobs");
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("sleep 0.1 & bg").stdout().contains("sleep"));
    }

    @Test
    void jobsWithoutIdsUseMostRecentJobThird() {
        Sh4j.INSTANCE.runFull("sleep 0.1 & jobs");
        Sh4j.INSTANCE.runFull("sleep 0.1 & bg");
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("sleep 0.1 & fg").stdout().contains("sleep"));
    }

    @Test
    void dispatcherResidualBranchesFirst() throws IOException {
        Assertions.assertTrue(Sh4j.INSTANCE.out("command -V ls").contains(" is /"));
    }

    @Test
    void dispatcherResidualBranchesSecond() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -V __no_such_cmd__"));
    }

    @Test
    void dispatcherResidualBranchesThird() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Assertions.assertEquals(
            String.format("?%n"),
            Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"")
        );
    }

    @Test
    void dispatcherResidualBranchesFourth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull("echo one; fc -s nope").exitCode());
    }

    @Test
    void dispatcherResidualBranchesFifth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one; fc -s nope").stderr().contains("-s requires pat=rep")
        );
    }

    @Test
    void dispatcherResidualBranchesSixth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Assertions.assertEquals(
            String.format("hi%necho hey%nhey%n"),
            Sh4j.INSTANCE.out("echo hi; fc -s hi=hey")
        );
    }

    @Test
    void dispatcherResidualBranchesSeventh() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Sh4j.INSTANCE.out("echo hi; fc -s hi=hey");
        Assertions.assertEquals(
            String.format("one%n1\techo one%n"),
            Sh4j.INSTANCE.out("echo one; fc -l abc")
        );
    }

    @Test
    void dispatcherResidualBranchesEighth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Sh4j.INSTANCE.out("echo hi; fc -s hi=hey");
        Sh4j.INSTANCE.out("echo one; fc -l abc");
        final Path fdesc = new TempDirs(BuiltinMutationTest.root).file("fdesc.txt").toPath();
        Sh4j.INSTANCE.run(String.format("echo hi; exec 3>%s; fc -s nope 2>&3; exec 3>&-", fdesc));
        Assertions.assertTrue(
            new String(Files.readAllBytes(fdesc), StandardCharsets.UTF_8).contains(
                "fc: -s requires pat=rep"
            )
        );
    }

    @Test
    void pipelineAndDotResidualBranchesFirst() throws IOException {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("__no_such__ | cat"));
    }

    @Test
    void pipelineAndDotResidualBranchesSecond() throws IOException {
        Sh4j.INSTANCE.run("__no_such__ | cat");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("echo x | __no_such__"));
    }

    @Test
    void pipelineAndDotResidualBranchesThird() throws IOException {
        Sh4j.INSTANCE.run("__no_such__ | cat");
        Sh4j.INSTANCE.run("echo x | __no_such__");
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out("(exit 3); echo $?"));
    }

    @Test
    void pipelineAndDotResidualBranchesFourth() throws IOException {
        Sh4j.INSTANCE.run("__no_such__ | cat");
        Sh4j.INSTANCE.run("echo x | __no_such__");
        Sh4j.INSTANCE.out("(exit 3); echo $?");
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder("dotrel").toPath();
        Files.write(dir.resolve("s.sh"), String.format("X=dot%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("dot%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; . ./s.sh; echo \"$X\"", dir))
        );
    }

    @Test
    void arithmeticOperatorResidualBranchesFirst() {
        Assertions.assertEquals(
            String.format("1 6 24 31%n8 1 1 1 1%n2 7 12 3%n1 -1 2%n"),
            Sh4j.INSTANCE.out(
                "echo $((2 ^ 3)) $((1+2+3)) $((2*3*4)) $((0x1f))",
                "echo $((010)) $((3>2)) $((3>=3)) $((3!=2)) $((3==3))",
                "echo $((6&3)) $((6|3)) $((6<<1)) $((6>>1))",
                "echo $((!0)) $((~0)) $((0?1:2))"
            )
        );
    }

    @Test
    void arithmeticOperatorResidualBranchesSecond() {
        Sh4j.INSTANCE.out(
            "echo $((2 ^ 3)) $((1+2+3)) $((2*3*4)) $((0x1f))",
            "echo $((010)) $((3>2)) $((3>=3)) $((3!=2)) $((3==3))",
            "echo $((6&3)) $((6|3)) $((6<<1)) $((6>>1))",
            "echo $((!0)) $((~0)) $((0?1:2))"
        );
        Assertions.assertEquals(
            String.format("7 7 0 0%n"),
            Sh4j.INSTANCE.out("x=5; echo $((x+=2)) $((x)) $((x=0)) $((x))")
        );
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
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs 99"));
    }

    @Test
    void heredocTabStripAndJobLookupBranchesThird() {
        Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"));
        Sh4j.INSTANCE.run("jobs 99");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
    }

    @Test
    void heredocTabStripAndJobLookupBranchesFourth() {
        Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"));
        Sh4j.INSTANCE.run("jobs 99");
        Sh4j.INSTANCE.run("kill -9 999999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -KILL 999999"));
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
    void compoundAssignmentAndGlobPrefixBranchesFirst() throws IOException {
        Assertions.assertEquals(
            String.format("12 6 2 3 11 22 7 1 0%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                    " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
                )
            )
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesSecond() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("echo $((unknownvar+1))"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesThird() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("echo $((1+))"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFourth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Assertions.assertEquals(String.format("0%n"), Sh4j.INSTANCE.out("echo $((x=))"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFifth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("command"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesSixth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesSeventh() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -V"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesEighth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo x >&abc"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesNinth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull("echo ${missing_q?}").exitCode());
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesTenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo ${missing_q?}").stderr().contains("parameter not set")
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesEleventh() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Assertions.assertEquals(
            String.format("sub/a.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir))
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesTwelfth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Assertions.assertEquals(
            dir.resolve("sub").resolve("a.txt") + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir))
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesThirteenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir));
        Assertions.assertEquals(
            String.format("nodir/*.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo nodir/*.txt", dir))
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFourteenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("cd %s; echo nodir/*.txt", dir));
        Assertions.assertEquals(String.format("[]%n"), Sh4j.INSTANCE.out("echo []"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFifteenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("cd %s; echo nodir/*.txt", dir));
        Sh4j.INSTANCE.out("echo []");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("sleep 0.1 & kill -9 $!"));
    }

    private static InputStream failingInput() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public int read(final byte[] buf, final int off, final int len)
                throws IOException {
                throw new IOException("boom");
            }
        };
    }

    private static Process stubborn() throws IOException {
        return new ProcessBuilder(
            "sh", "-c", "trap '' TERM; while true; do sleep 1; done"
        ).start();
    }
}
