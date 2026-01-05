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

/**
 * Additional built-in boundary tests selected from PIT survivors.
 *
 * @since 1.0
 */
final class BuiltinMutationTest {

    @TempDir
    private static Path root;

    @Test
    void unsetOptionForms() {
        Assertions.assertNull(Sh4j.INSTANCE.getVar("X", "X=1; unset -v X"));
        Assertions.assertNull(Sh4j.INSTANCE.getVar("X", "X=1; unset -- X"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("f() { :; }; unset -f f"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("X=1; unset -z X"));
    }

    @Test
    void exportBareAndPrintForms() {
        Assertions.assertEquals("", Sh4j.INSTANCE.getVar("BARE", "export BARE"));
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("export BARE; sh -c 'echo ${BARE-unset}'")
        );
        Assertions.assertTrue(Sh4j.INSTANCE.out("export -p").contains("export PATH="));
    }

    @Test
    void pwdLogicalAndPhysicalForms() throws IOException {
        final Path real = TempDirs.folder(BuiltinMutationTest.root, "real").toPath().toRealPath();
        Assertions.assertEquals(
            real + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -L", real))
        );
        Assertions.assertEquals(
            real + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -P", real))
        );
    }

    @Test
    void echoNoNewlineAndEmptyForms() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("echo -n"));
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo"));
        Assertions.assertEquals("a b", Sh4j.INSTANCE.out("echo -n a b"));
    }

    @Test
    void evalEmptyAndNonEmptyForms() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("eval"));
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("eval echo hi"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("eval ''"));
    }

    @Test
    void umaskNumericAndSymbolicForms() {
        Assertions.assertEquals(String.format("0077%n"), Sh4j.INSTANCE.out("umask 077; umask"));
        Assertions.assertEquals(
            String.format("u=rwx,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 077; umask -S")
        );
        Assertions.assertEquals(
            String.format("u=rwx,g=rx,o=rx%n"),
            Sh4j.INSTANCE.out("umask 022; umask -S")
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("umask nope"));
    }

    @Test
    void shiftBoundaryForms() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("set -- a b; shift 0; echo $1")
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("shift -1"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("shift nope"));
        Assertions.assertEquals(
            String.format("2 rc=1%n"),
            Sh4j.INSTANCE.out("set -- a b; shift 99; echo \"$# rc=$?\"")
        );
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 2; echo $1")
        );
    }

    @Test
    void dispatcherUncoveredBranches() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("f() { return abc; }; f"));
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done")
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("unset HOME; cd"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("umask 999"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("wait abc"));
        Assertions.assertEquals(
            String.format("f is a function%n"),
            Sh4j.INSTANCE.out("f() { :; }; type f")
        );
        Assertions.assertTrue(Sh4j.INSTANCE.out("command -v ls").startsWith("/"));
        Assertions.assertEquals(
            String.format("f%n"),
            Sh4j.INSTANCE.out("f() { :; }; command -v f")
        );
        Assertions.assertEquals(
            String.format("a=x b=[]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read a b; echo \"a=$a b=[$b]\""
            ).stdout()
        );
        Assertions.assertEquals(
            String.format("[][]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read a b c; echo \"[$b][$c]\""
            ).stdout()
        );
    }

    @Test
    void getoptsMultiArgumentAndReadEscape() {
        Assertions.assertEquals(
            String.format("a%nb%ndone%n"),
            Sh4j.INSTANCE.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done")
        );
        Assertions.assertEquals(
            String.format("a 2%n? 2%n"),
            Sh4j.INSTANCE.out(
                "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
            )
        );
        Assertions.assertEquals(
            String.format("[abc\\]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream("abc\\".getBytes(StandardCharsets.UTF_8)),
                "read x; echo \"[$x]\""
            ).stdout()
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -l abc"));
        Assertions.assertEquals(
            String.format("? 2%n"),
            Sh4j.INSTANCE.out(
                "set -- -ab; getopts a o; getopts a o; getopts a o; echo \"$o $OPTIND\""
            )
        );
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
    void typeResolutionAndEmptyPathSegment() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("type ls").startsWith("ls is /"));
        Assertions.assertEquals(
            String.format("viapath%n"),
            Sh4j.INSTANCE.out("PATH=:/bin sh -c 'echo viapath'")
        );
    }

    @Test
    void umaskSymbolicFallbackAndAliasWithAssignments() {
        Assertions.assertEquals(
            String.format("u=,g=,o=%n"),
            Sh4j.INSTANCE.out("SH4J_UMASK=zz; umask -S")
        );
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("set foo; echo $#"));
        Assertions.assertEquals(
            String.format("b='echo'%n"),
            Sh4j.INSTANCE.out("A=1 alias b=echo; alias b")
        );
    }

    @Test
    void literalBracesAndJobStateRefresh() {
        Assertions.assertEquals(String.format("${}%n"), Sh4j.INSTANCE.out("x=1; echo \"${}\""));
        Assertions.assertEquals(String.format("${ x}%n"), Sh4j.INSTANCE.out("x=1; echo \"${ x}\""));
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("true & sleep 0.3; jobs").contains("Done true")
        );
    }

    @Test
    void subshellExitAndWhileSignals() {
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out("(exit 3); echo $?"));
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("i=0; while true; do i=$((i+1)); break; done; echo $i")
        );
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out(
                "i=0; while true; do j=0; while true; do i=$((i+1)); break 2; done; done; echo $i"
            )
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
    void readCustomIfsAndWhitespace() {
        Assertions.assertEquals(
            String.format("a=x b=y:z%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
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
        Assertions.assertEquals(
            String.format("a=x b=:%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x::%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void getoptsClustersDefaultsAndStop() {
        Assertions.assertEquals(
            String.format("a:%nb:val%n"),
            Sh4j.INSTANCE.out(
                "set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done"
            )
        );
        Assertions.assertEquals(
            String.format("?:z%n"),
            Sh4j.INSTANCE.out("set -- -z; getopts a o; echo \"$o:$OPTARG\"")
        );
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -- --; getopts a o; echo $?")
        );
        Assertions.assertEquals(
            String.format("::%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts :a: o; echo \"$o:$OPTARG\"")
        );
        Assertions.assertEquals(
            String.format("a:val%n"),
            Sh4j.INSTANCE.out("getopts a: o -a val; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void getoptsInvalidOptindIsNormalised() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("OPTIND=bad; set -- -a; getopts a o; echo $?")
        );
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("OPTIND=99; set -- -a; getopts a o; echo $?")
        );
    }

    @Test
    void jobsWithExplicitAndMissingIds() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait").stdout().contains("sleep")
        );
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs 999"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs notanumber"));
    }

    @Test
    void fgExplicitAndMissingIds() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1").stdout().contains("sleep")
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg 999"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg notanumber"));
    }

    @Test
    void fcListAndSubstitute() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l").stdout().contains("echo one")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo hello", "fc -s hello=bye").stdout().contains("bye")
        );
    }

    @Test
    void fcErrorForms() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -e"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -z"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -s"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -l"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -s broken"));
    }

    @Test
    void setOptionRemovalAndPositionalForms() {
        Assertions.assertFalse(
            Sh4j.INSTANCE.out("set -e; set +e; echo $-").contains("e")
        );
        Assertions.assertFalse(
            Sh4j.INSTANCE.out("set -o errexit; set +o errexit; echo $-").contains("e")
        );
        Assertions.assertEquals(String.format("2%n"), Sh4j.INSTANCE.out("set -- a b; echo $#"));
        Assertions.assertEquals(String.format("0%n"), Sh4j.INSTANCE.out("set --; echo $#"));
    }

    @Test
    void aliasForms() {
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("alias a='echo hi'", "a"));
        Assertions.assertEquals(
            String.format("a='echo hi'%n"),
            Sh4j.INSTANCE.out("alias a='echo hi'; alias a")
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("alias missing"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("alias a=b; unalias a"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("alias a=b; unalias -a"));
    }

    @Test
    void trapListingAndRemovalForms() {
        final Sh4j.Result listed = Sh4j.INSTANCE.runFull(
            "trap 'echo t' EXIT INT; trap; trap - EXIT INT"
        );
        Assertions.assertTrue(listed.stdout().contains("EXIT"));
        Assertions.assertTrue(listed.stdout().contains("INT"));
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.out("trap '' EXIT; echo body")
        );
    }

    @Test
    void dotEmptyAndPositionalForms() throws IOException {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("."));
        final Path lib = TempDirs.file(BuiltinMutationTest.root, "dot.sh").toPath();
        Files.write(lib, String.format("echo \"$1-$2\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("a-b%n"),
            Sh4j.INSTANCE.out(String.format(". %s a b", lib))
        );
    }

    @Test
    void readonlyBareAndAssignedForms() {
        Assertions.assertEquals(
            String.format("unset%n"),
            Sh4j.INSTANCE.out("readonly R; echo ${R-unset}")
        );
        Assertions.assertEquals(
            String.format("readonly R=1%n"),
            Sh4j.INSTANCE.out("readonly R=1; readonly -p")
        );
    }

    @Test
    void hashAndTypeForms() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash echo"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("hash missing_command"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("type echo"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("type missing_command"));
    }

    @Test
    void commandLocateAndExecuteForms() {
        Assertions.assertEquals(String.format("echo%n"), Sh4j.INSTANCE.out("command -v echo"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v missing_command"));
        Assertions.assertEquals(
            String.format("echo is a shell built-in%n"),
            Sh4j.INSTANCE.out("command -V echo")
        );
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("command echo hi"));
    }

    @Test
    void killOptionForms() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill -s"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill notapid"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -s TERM 999999"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("kill -l").contains("TERM"));
    }

    @Test
    void killRealProcesses() throws Exception {
        final Process term = new ProcessBuilder("sleep", "30").start();
        try {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(
                    String.format("kill %d", term.toHandle().pid())
                )
            );
            Assertions.assertTrue(term.waitFor(5, TimeUnit.SECONDS));
        } finally {
            term.destroyForcibly();
        }
        final Process force = new ProcessBuilder("sleep", "30").start();
        try {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(
                    String.format("kill -9 %d", force.toHandle().pid())
                )
            );
            Assertions.assertTrue(force.waitFor(5, TimeUnit.SECONDS));
        } finally {
            force.destroyForcibly();
        }
    }

    @Test
    void getoptsBoundaryForms() {
        Assertions.assertEquals(
            String.format("? 1%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o $?\"")
        );
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("OPTIND=0; set -- -a; getopts a o; echo $?")
        );
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; getopts a o; echo $?")
        );
    }

    @Test
    void umaskAllBitCombinations() {
        Assertions.assertEquals(
            String.format("u=rwx,g=rwx,o=rwx%n"),
            Sh4j.INSTANCE.out("umask 000; umask -S")
        );
        Assertions.assertEquals(
            String.format("u=,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 777; umask -S")
        );
        Assertions.assertEquals(
            String.format("u=rwx,g=,o=%n"),
            Sh4j.INSTANCE.out("umask 077; umask -S")
        );
        Assertions.assertEquals(
            String.format("u=rwx,g=r,o=r%n"),
            Sh4j.INSTANCE.out("umask 033; umask -S")
        );
    }

    @Test
    void readMoreSplittingForms() {
        Assertions.assertEquals(
            String.format("a= b=x:y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format(":x:y%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
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
    void fcListModesAndRanges() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -n -l").stdout().contains("\techo one")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l 1 1").stdout().contains("echo one")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -e true").stdout().contains("echo one")
        );
    }

    @Test
    void setAllOptionNames() {
        for (final String option : new String[] {
            "errexit", "nounset", "noglob", "xtrace", "noexec", "verbose", "allexport",
        }) {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(
                    String.format("set -o %s; set +o %s", option, option)
                )
            );
        }
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -o unknown_option"));
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -o nounset; set +o nounset; echo $-")
        );
    }

    @Test
    void dotRestoresPositionalParameters() throws IOException {
        final Path lib = TempDirs.file(BuiltinMutationTest.root, "restore.sh").toPath();
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
        final Path dir = TempDirs.folder(BuiltinMutationTest.root,
            "pwdorder").toPath().toRealPath();
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
    void jobsBgFgExplicitIds() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1").stdout().contains("sleep")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1").stdout().contains("sleep")
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("bg %999"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg %999"));
    }

    @Test
    void aliasQuotedAndEmptyValues() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("alias e=''", "e"));
        Assertions.assertEquals(
            String.format("a='x y'%n"),
            Sh4j.INSTANCE.out("alias a=\"x y\"; alias a")
        );
    }

    @Test
    void getoptsMoreBoundaries() {
        Assertions.assertEquals(
            String.format("? 1 2%n"),
            Sh4j.INSTANCE.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"")
        );
        Assertions.assertEquals(
            String.format("a:val%n"),
            Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"")
        );
        Assertions.assertEquals(
            String.format("?:z%n"),
            Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"")
        );
        Assertions.assertEquals(
            String.format("?:%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a: o; echo \"$o:$OPTARG\"")
        );
        Assertions.assertEquals(
            String.format("? 1%n"),
            Sh4j.INSTANCE.out("set -- -a file; getopts a o; getopts a o; echo \"$o $?\"")
        );
    }

    @Test
    void setOptionsAreObservableInFlags() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o errexit; echo $-").contains("e"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o nounset; echo $-").contains("u"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o noglob; echo $-").contains("f"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o xtrace; echo $-").contains("x"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o noexec; echo $-").contains("n"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o verbose; echo $-").contains("v"));
        Assertions.assertTrue(Sh4j.INSTANCE.out("set -o allexport; echo $-").contains("a"));
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -eu; set +eu; echo $-")
        );
    }

    @Test
    void fcEditorAndRangeBoundaries() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -e true 1").stdout().contains("echo one")
        );
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 3 2").exitCode());
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -e true 1 1").exitCode());
    }

    @Test
    void trapReturnCodesAndDotErrors() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("trap"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("trap - EXIT"));
        final Sh4j.Result dot = Sh4j.INSTANCE.runFull(". /nonexistent/dotfile");
        Assertions.assertEquals(1, dot.exitCode());
        Assertions.assertTrue(dot.stderr().contains("cannot open"));
    }

    @Test
    void breakZeroUsesOneLevel() {
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 0; done; done")
        );
    }

    @Test
    void jobIdBoundaryForms() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs %0"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs -1"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("bg %0"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg %0"));
    }

    @Test
    void killTermAndKillSemantics() throws Exception {
        final Process stubborn = new ProcessBuilder(
            "sh", "-c", "trap '' TERM; while true; do sleep 1; done"
        ).start();
        try {
            Thread.sleep(200L);
            final long pid = stubborn.toHandle().pid();
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill %d", pid))
            );
            Thread.sleep(200L);
            Assertions.assertTrue(stubborn.isAlive(), "TERM-ignoring process should survive");
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill -KILL %d", pid))
            );
            Assertions.assertTrue(stubborn.waitFor(5L, TimeUnit.SECONDS));
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void getoptsExactBoundaryBranches() {
        Assertions.assertEquals(
            String.format("?:1:1%n"),
            Sh4j.INSTANCE.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
        Assertions.assertEquals(
            String.format("?::1%n"),
            Sh4j.INSTANCE.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
        Assertions.assertEquals(
            String.format("?:2:1%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts a o; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
        Assertions.assertEquals(
            String.format("?:z%n"),
            Sh4j.INSTANCE.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("set -- -z; getopts a o").stderr().contains("illegal option")
        );
        Assertions.assertEquals(
            String.format("a:val%n"),
            Sh4j.INSTANCE.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    void fcRangeEdgesAndSubstitution() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 0").exitCode());
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l -1").stdout().contains("echo one")
        );
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 5").exitCode());
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -s one=two").stdout().contains("two")
        );
    }

    @Test
    void jobsWithoutIdsUseMostRecentJob() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.1 & jobs").stdout().contains("sleep")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.1 & bg").stdout().contains("sleep")
        );
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.1 & fg").stdout().contains("sleep")
        );
    }

    @Test
    void dispatcherResidualBranches() throws IOException {
        Assertions.assertTrue(Sh4j.INSTANCE.out("command -V ls").contains(" is /"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -V __no_such_cmd__"));
        Assertions.assertEquals(
            String.format("?%n"),
            Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"")
        );
        final Sh4j.Result missing = Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Assertions.assertEquals(1, missing.exitCode());
        Assertions.assertTrue(missing.stderr().contains("-s requires pat=rep"));
        Assertions.assertEquals(
            String.format("hi%necho hey%nhey%n"),
            Sh4j.INSTANCE.out("echo hi; fc -s hi=hey")
        );
        Assertions.assertEquals(
            String.format("one%n1\techo one%n"),
            Sh4j.INSTANCE.out("echo one; fc -l abc")
        );
        final Path fdesc = TempDirs.file(BuiltinMutationTest.root, "fdesc.txt").toPath();
        Sh4j.INSTANCE.run(
            String.format(
                "echo hi; exec 3>%s; fc -s nope 2>&3; exec 3>&-",
                fdesc
            )
        );
        Assertions.assertTrue(
            new String(Files.readAllBytes(fdesc), StandardCharsets.UTF_8)
                .contains("fc: -s requires pat=rep")
        );
    }

    @Test
    void pipelineAndDotResidualBranches() throws IOException {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("__no_such__ | cat"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("echo x | __no_such__"));
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out("(exit 3); echo $?"));
        final Path dir = TempDirs.folder(BuiltinMutationTest.root, "dotrel").toPath();
        Files.write(
            dir.resolve("s.sh"),
            String.format("X=dot%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            String.format("dot%n"),
            Sh4j.INSTANCE.out(
                String.format("cd %s; . ./s.sh; echo \"$X\"", dir)
            )
        );
    }

    @Test
    void arithmeticOperatorResidualBranches() {
        Assertions.assertEquals(
            String.format("1 6 24 31%n8 1 1 1 1%n2 7 12 3%n1 -1 2%n"),
            Sh4j.INSTANCE.out(
                "echo $((2 ^ 3)) $((1+2+3)) $((2*3*4)) $((0x1f))",
                "echo $((010)) $((3>2)) $((3>=3)) $((3!=2)) $((3==3))",
                "echo $((6&3)) $((6|3)) $((6<<1)) $((6>>1))",
                "echo $((!0)) $((~0)) $((0?1:2))"
            )
        );
        Assertions.assertEquals(
            String.format("7 7 0 0%n"),
            Sh4j.INSTANCE.out("x=5; echo $((x+=2)) $((x)) $((x=0)) $((x))")
        );
    }

    @Test
    void heredocTabStripAndJobLookupBranches() {
        Assertions.assertEquals(
            String.format("tabbed%n"),
            Sh4j.INSTANCE.out(String.format("cat <<-EOF%n\ttabbed%n\tEOF"))
        );
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs 99"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -KILL 999999"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v __no_such_cmd__"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranches() throws IOException {
        Assertions.assertEquals(
            String.format("12 6 2 3 11 22 7 1 0%n"),
            Sh4j.INSTANCE.out(
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))"
                + " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("echo $((unknownvar+1))"));
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("echo $((1+))"));
        Assertions.assertEquals(String.format("0%n"), Sh4j.INSTANCE.out("echo $((x=))"));
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("command"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -V"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo x >&abc"));
        final Sh4j.Result unset = Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        Assertions.assertEquals(1, unset.exitCode());
        Assertions.assertTrue(unset.stderr().contains("parameter not set"));
        final Path dir = TempDirs.folder(BuiltinMutationTest.root, "glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Assertions.assertEquals(
            String.format("sub/a.txt%n"),
            Sh4j.INSTANCE.out(
                String.format("cd %s; echo sub/*.txt", dir)
            )
        );
        Assertions.assertEquals(
            dir.resolve("sub").resolve("a.txt") + String.format("%n"),
            Sh4j.INSTANCE.out(
                String.format("echo %s/sub/*.txt", dir)
            )
        );
        Assertions.assertEquals(
            String.format("nodir/*.txt%n"),
            Sh4j.INSTANCE.out(
                String.format("cd %s; echo nodir/*.txt", dir)
            )
        );
        Assertions.assertEquals(String.format("[]%n"), Sh4j.INSTANCE.out("echo []"));
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
}
