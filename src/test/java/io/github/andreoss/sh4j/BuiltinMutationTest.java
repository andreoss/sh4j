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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Additional built-in boundary tests selected from PIT survivors.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class BuiltinMutationTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void unsetOptionForms() {
        Assert.assertNull(Sh4j.getVar("X", "X=1; unset -v X"));
        Assert.assertNull(Sh4j.getVar("X", "X=1; unset -- X"));
        Assert.assertEquals(0, Sh4j.run("f() { :; }; unset -f f"));
        Assert.assertEquals(0, Sh4j.run("X=1; unset -z X"));
    }

    @Test
    public void exportBareAndPrintForms() {
        Assert.assertEquals("", Sh4j.getVar("BARE", "export BARE"));
        Assert.assertEquals(
            "\n",
            Sh4j.out("export BARE; sh -c 'echo ${BARE-unset}'")
        );
        Assert.assertTrue(Sh4j.out("export -p").contains("export PATH="));
    }

    @Test
    public void pwdLogicalAndPhysicalForms() throws IOException {
        final Path real = this.tempdir.newFolder("real").toPath().toRealPath();
        Assert.assertEquals(
            real + "\n",
            Sh4j.out(String.format("cd %s; pwd -L", real))
        );
        Assert.assertEquals(
            real + "\n",
            Sh4j.out(String.format("cd %s; pwd -P", real))
        );
    }

    @Test
    public void echoNoNewlineAndEmptyForms() {
        Assert.assertEquals("", Sh4j.out("echo -n"));
        Assert.assertEquals("\n", Sh4j.out("echo"));
        Assert.assertEquals("a b", Sh4j.out("echo -n a b"));
    }

    @Test
    public void evalEmptyAndNonEmptyForms() {
        Assert.assertEquals(0, Sh4j.run("eval"));
        Assert.assertEquals("hi\n", Sh4j.out("eval echo hi"));
        Assert.assertEquals(0, Sh4j.run("eval ''"));
    }

    @Test
    public void umaskNumericAndSymbolicForms() {
        Assert.assertEquals("0077\n", Sh4j.out("umask 077; umask"));
        Assert.assertEquals("u=rwx,g=,o=\n", Sh4j.out("umask 077; umask -S"));
        Assert.assertEquals(
            "u=rwx,g=rx,o=rx\n",
            Sh4j.out("umask 022; umask -S")
        );
        Assert.assertEquals(1, Sh4j.run("umask nope"));
    }

    @Test
    public void shiftBoundaryForms() {
        Assert.assertEquals("a\n", Sh4j.out("set -- a b; shift 0; echo $1"));
        Assert.assertEquals(1, Sh4j.run("shift -1"));
        Assert.assertEquals(1, Sh4j.run("shift nope"));
        Assert.assertEquals(
            "2 rc=1\n",
            Sh4j.out("set -- a b; shift 99; echo \"$# rc=$?\"")
        );
        Assert.assertEquals("c\n", Sh4j.out("set -- a b c; shift 2; echo $1"));
    }

    @Test
    public void dispatcherUncoveredBranches() {
        Assert.assertEquals(0, Sh4j.run("f() { return abc; }; f"));
        Assert.assertEquals(
            "1a\n2a\n",
            Sh4j.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done")
        );
        Assert.assertEquals(1, Sh4j.run("unset HOME; cd"));
        Assert.assertEquals(1, Sh4j.run("umask 999"));
        Assert.assertEquals(127, Sh4j.run("wait abc"));
        Assert.assertEquals("f is a function\n", Sh4j.out("f() { :; }; type f"));
        Assert.assertTrue(Sh4j.out("command -v ls").startsWith("/"));
        Assert.assertEquals("f\n", Sh4j.out("f() { :; }; command -v f"));
        final Sh4j.Result short1 = Sh4j.runFull(
            new ByteArrayInputStream("x\n".getBytes(StandardCharsets.UTF_8)),
            "read a b; echo \"a=$a b=[$b]\""
        );
        Assert.assertEquals("a=x b=[]\n", short1.stdout());
        final Sh4j.Result short3 = Sh4j.runFull(
            new ByteArrayInputStream("x\n".getBytes(StandardCharsets.UTF_8)),
            "read a b c; echo \"[$b][$c]\""
        );
        Assert.assertEquals("[][]\n", short3.stdout());
    }

    @Test
    public void getoptsMultiArgumentAndReadEscape() {
        Assert.assertEquals(
            "a\nb\ndone\n",
            Sh4j.out("set -- -ab x; while getopts ab o; do echo \"$o\"; done; echo done")
        );
        Assert.assertEquals(
            "a 2\n? 2\n",
            Sh4j.out(
                "set -- -a; getopts a o; echo \"$o $OPTIND\"; getopts a o; echo \"$o $OPTIND\""
            )
        );
        Assert.assertEquals(
            "[abc\\]\n",
            Sh4j.runFull(
                new ByteArrayInputStream("abc\\".getBytes(StandardCharsets.UTF_8)),
                "read x; echo \"[$x]\""
            ).stdout()
        );
        Assert.assertEquals(1, Sh4j.run("fc -l abc"));
        Assert.assertEquals(
            "? 2\n",
            Sh4j.out("set -- -ab; getopts a o; getopts a o; getopts a o; echo \"$o $OPTIND\"")
        );
        Assert.assertEquals(
            "? 2\n",
            Sh4j.out("set -- -ab x; getopts a o; getopts a o; getopts a o; echo \"$o $OPTIND\"")
        );
    }

    @Test
    public void readInputLineSurvivesIOException() {
        final Sh4j.Result result = Sh4j.runFull(
            new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("boom");
                }

                @Override
                public int read(final byte[] buf, final int off, final int len)
                    throws IOException {
                    throw new IOException("boom");
                }
            },
            "read x; echo \"rc=$? x=[$x]\""
        );
        Assert.assertEquals("rc=1 x=[]\n", result.stdout());
    }

    @Test
    public void typeResolutionAndEmptyPathSegment() {
        Assert.assertTrue(Sh4j.out("type ls").startsWith("ls is /"));
        Assert.assertEquals(
            "viapath\n",
            Sh4j.out("PATH=:/bin sh -c 'echo viapath'")
        );
    }

    @Test
    public void umaskSymbolicFallbackAndAliasWithAssignments() {
        Assert.assertEquals("u=,g=,o=\n", Sh4j.out("SH4J_UMASK=zz; umask -S"));
        Assert.assertEquals("1\n", Sh4j.out("set foo; echo $#"));
        Assert.assertEquals("b='echo'\n", Sh4j.out("A=1 alias b=echo; alias b"));
    }

    @Test
    public void literalBracesAndJobStateRefresh() {
        Assert.assertEquals("${}\n", Sh4j.out("x=1; echo \"${}\""));
        Assert.assertEquals("${ x}\n", Sh4j.out("x=1; echo \"${ x}\""));
        Assert.assertTrue(
            Sh4j.out("true & sleep 0.3; jobs").contains("Done true")
        );
    }

    @Test
    public void subshellExitAndWhileSignals() {
        Assert.assertEquals("3\n", Sh4j.out("(exit 3); echo $?"));
        Assert.assertEquals(
            "1\n",
            Sh4j.out("i=0; while true; do i=$((i+1)); break; done; echo $i")
        );
        Assert.assertEquals(
            "1\n",
            Sh4j.out(
                "i=0; while true; do j=0; while true; do i=$((i+1)); break 2; done; done; echo $i"
            )
        );
        Assert.assertEquals(
            "3 3\n",
            Sh4j.out(
                String.format(
                    "i=0; j=0; while [ $i -lt 3 ]; do i=$((i+1)); %s done; echo \"$i $j\"",
                    "while true; do j=$((j+1)); continue 2; done;"
                )
            )
        );
    }

    @Test
    public void readCustomIfsAndWhitespace() {
        final Sh4j.Result colon = Sh4j.runFull(
            new ByteArrayInputStream("x:y:z\n".getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a=x b=y:z\n", colon.stdout());
        final Sh4j.Result spaces = Sh4j.runFull(
            new ByteArrayInputStream("   x   y   \n".getBytes(StandardCharsets.UTF_8)),
            "read a b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a=x b=y\n", spaces.stdout());
        final Sh4j.Result trailing = Sh4j.runFull(
            new ByteArrayInputStream("x::\n".getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a=x b=:\n", trailing.stdout());
    }

    @Test
    public void getoptsClustersDefaultsAndStop() {
        Assert.assertEquals(
            "a:\nb:val\n",
            Sh4j.out(
                "set -- -ab val; while getopts ab: o; do echo \"$o:$OPTARG\"; done"
            )
        );
        Assert.assertEquals(
            "?:z\n",
            Sh4j.out("set -- -z; getopts a o; echo \"$o:$OPTARG\"")
        );
        Assert.assertEquals(
            "1\n",
            Sh4j.out("set -- --; getopts a o; echo $?")
        );
        Assert.assertEquals(
            "::\n",
            Sh4j.out("set -- -a; getopts :a: o; echo \"$o:$OPTARG\"")
        );
        Assert.assertEquals(
            "a:val\n",
            Sh4j.out("getopts a: o -a val; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    public void getoptsInvalidOptindIsNormalised() {
        Assert.assertEquals(
            "0\n",
            Sh4j.out("OPTIND=bad; set -- -a; getopts a o; echo $?")
        );
        Assert.assertEquals(
            "1\n",
            Sh4j.out("OPTIND=99; set -- -a; getopts a o; echo $?")
        );
    }

    @Test
    public void jobsWithExplicitAndMissingIds() {
        final Sh4j.Result listed = Sh4j.runFull(
            "sleep 0.2 & jobs %1; wait"
        );
        Assert.assertTrue(listed.stdout().contains("sleep"));
        Assert.assertEquals(0, Sh4j.run("jobs 999"));
        Assert.assertEquals(0, Sh4j.run("jobs notanumber"));
    }

    @Test
    public void fgExplicitAndMissingIds() {
        final Sh4j.Result listed = Sh4j.runFull("sleep 0.05 & fg %1");
        Assert.assertTrue(listed.stdout().contains("sleep"));
        Assert.assertEquals(1, Sh4j.run("fg 999"));
        Assert.assertEquals(1, Sh4j.run("fg notanumber"));
    }

    @Test
    public void fcListAndSubstitute() {
        final Sh4j.Result listed = Sh4j.runFull("echo one", "fc -l");
        Assert.assertTrue(listed.stdout().contains("echo one"));
        final Sh4j.Result substituted = Sh4j.runFull("echo hello", "fc -s hello=bye");
        Assert.assertTrue(substituted.stdout().contains("bye"));
    }

    @Test
    public void fcErrorForms() {
        Assert.assertEquals(1, Sh4j.run("fc -e"));
        Assert.assertEquals(1, Sh4j.run("fc -z"));
        Assert.assertEquals(1, Sh4j.run("fc -s"));
        Assert.assertEquals(1, Sh4j.run("fc -l"));
        Assert.assertEquals(1, Sh4j.run("fc -s broken"));
    }

    @Test
    public void setOptionRemovalAndPositionalForms() {
        Assert.assertFalse(
            Sh4j.out("set -e; set +e; echo $-").contains("e")
        );
        Assert.assertFalse(
            Sh4j.out("set -o errexit; set +o errexit; echo $-").contains("e")
        );
        Assert.assertEquals("2\n", Sh4j.out("set -- a b; echo $#"));
        Assert.assertEquals("0\n", Sh4j.out("set --; echo $#"));
    }

    @Test
    public void aliasForms() {
        Assert.assertEquals("hi\n", Sh4j.out("alias a='echo hi'", "a"));
        Assert.assertEquals(
            "a='echo hi'\n",
            Sh4j.out("alias a='echo hi'; alias a")
        );
        Assert.assertEquals(1, Sh4j.run("alias missing"));
        Assert.assertEquals(0, Sh4j.run("alias a=b; unalias a"));
        Assert.assertEquals(0, Sh4j.run("alias a=b; unalias -a"));
    }

    @Test
    public void trapListingAndRemovalForms() {
        final Sh4j.Result listed = Sh4j.runFull(
            "trap 'echo t' EXIT INT; trap; trap - EXIT INT"
        );
        Assert.assertTrue(listed.stdout().contains("EXIT"));
        Assert.assertTrue(listed.stdout().contains("INT"));
        Assert.assertEquals("body\n", Sh4j.out("trap '' EXIT; echo body"));
    }

    @Test
    public void dotEmptyAndPositionalForms() throws IOException {
        Assert.assertEquals(1, Sh4j.run("."));
        final Path lib = this.tempdir.newFile("dot.sh").toPath();
        Files.write(lib, "echo \"$1-$2\"\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            "a-b\n",
            Sh4j.out(String.format(". %s a b", lib))
        );
    }

    @Test
    public void readonlyBareAndAssignedForms() {
        Assert.assertEquals(
            "unset\n",
            Sh4j.out("readonly R; echo ${R-unset}")
        );
        Assert.assertEquals(
            "readonly R=1\n",
            Sh4j.out("readonly R=1; readonly -p")
        );
    }

    @Test
    public void hashAndTypeForms() {
        Assert.assertEquals(0, Sh4j.run("hash echo"));
        Assert.assertEquals(127, Sh4j.run("hash missing_command"));
        Assert.assertEquals(0, Sh4j.run("type echo"));
        Assert.assertEquals(1, Sh4j.run("type missing_command"));
    }

    @Test
    public void commandLocateAndExecuteForms() {
        Assert.assertEquals("echo\n", Sh4j.out("command -v echo"));
        Assert.assertEquals(1, Sh4j.run("command -v missing_command"));
        Assert.assertEquals(
            "echo is a shell built-in\n",
            Sh4j.out("command -V echo")
        );
        Assert.assertEquals("hi\n", Sh4j.out("command echo hi"));
    }

    @Test
    public void killOptionForms() {
        Assert.assertEquals(1, Sh4j.run("kill"));
        Assert.assertEquals(1, Sh4j.run("kill -s"));
        Assert.assertEquals(127, Sh4j.run("kill notapid"));
        Assert.assertEquals(127, Sh4j.run("kill -s TERM 999999"));
        Assert.assertEquals(127, Sh4j.run("kill -9 999999"));
        Assert.assertTrue(Sh4j.out("kill -l").contains("TERM"));
    }

    @Test
    public void killRealProcesses() throws Exception {
        final Process term = new ProcessBuilder("sleep", "30").start();
        try {
            Assert.assertEquals(0, Sh4j.run("kill " + term.toHandle().pid()));
            Assert.assertTrue(term.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            term.destroyForcibly();
        }
        final Process force = new ProcessBuilder("sleep", "30").start();
        try {
            Assert.assertEquals(0, Sh4j.run("kill -9 " + force.toHandle().pid()));
            Assert.assertTrue(force.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            force.destroyForcibly();
        }
    }

    @Test
    public void getoptsBoundaryForms() {
        Assert.assertEquals(
            "? 1\n",
            Sh4j.out("set -- -a; getopts a o; getopts a o; echo \"$o $?\"")
        );
        Assert.assertEquals(
            "0\n",
            Sh4j.out("OPTIND=0; set -- -a; getopts a o; echo $?")
        );
        Assert.assertEquals(
            "1\n",
            Sh4j.out("set -- -a; getopts a o; getopts a o; getopts a o; echo $?")
        );
    }

    @Test
    public void umaskAllBitCombinations() {
        Assert.assertEquals("u=rwx,g=rwx,o=rwx\n", Sh4j.out("umask 000; umask -S"));
        Assert.assertEquals("u=,g=,o=\n", Sh4j.out("umask 777; umask -S"));
        Assert.assertEquals("u=rwx,g=,o=\n", Sh4j.out("umask 077; umask -S"));
        Assert.assertEquals("u=rwx,g=r,o=r\n", Sh4j.out("umask 033; umask -S"));
    }

    @Test
    public void readMoreSplittingForms() {
        final Sh4j.Result colonLeading = Sh4j.runFull(
            new ByteArrayInputStream(":x:y\n".getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a= b=x:y\n", colonLeading.stdout());
        final Sh4j.Result mixed = Sh4j.runFull(
            new ByteArrayInputStream("x : y\n".getBytes(StandardCharsets.UTF_8)),
            "IFS=' :'; read a b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a=x b=y\n", mixed.stdout());
    }

    @Test
    public void fcListModesAndRanges() {
        final Sh4j.Result noNumbers = Sh4j.runFull("echo one", "fc -n -l");
        Assert.assertTrue(noNumbers.stdout().contains("\techo one"));
        final Sh4j.Result range = Sh4j.runFull("echo one", "fc -l 1 1");
        Assert.assertTrue(range.stdout().contains("echo one"));
        final Sh4j.Result editor = Sh4j.runFull("echo one", "fc -e true");
        Assert.assertTrue(editor.stdout().contains("echo one"));
    }

    @Test
    public void setAllOptionNames() {
        for (final String option : new String[] {
            "errexit", "nounset", "noglob", "xtrace", "noexec", "verbose", "allexport",
        }) {
            Assert.assertEquals(0, Sh4j.run("set -o " + option + "; set +o " + option));
        }
        Assert.assertEquals(0, Sh4j.run("set -o unknown_option"));
        Assert.assertEquals("\n", Sh4j.out("set -o nounset; set +o nounset; echo $-"));
    }

    @Test
    public void dotRestoresPositionalParameters() throws IOException {
        final Path lib = this.tempdir.newFile("restore.sh").toPath();
        Files.write(lib, "echo \"in=$1\"\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            "in=inner\nouter\n",
            Sh4j.out(String.format("set -- outer; . %s inner; echo $1", lib))
        );
    }

    @Test
    public void echoReportsExpansionError() {
        Assert.assertEquals(1, Sh4j.run("echo ${MISSING:?required}"));
    }

    @Test
    public void pwdFlagOrder() throws IOException {
        final Path dir = this.tempdir.newFolder("pwdorder").toPath().toRealPath();
        Assert.assertEquals(
            dir + "\n",
            Sh4j.out(String.format("cd %s; pwd -P -L", dir))
        );
    }

    @Test
    public void timesPrintsTwoLines() {
        Assert.assertEquals(
            2,
            Sh4j.out("times").split("\n", -1).length - 1
        );
    }

    @Test
    public void jobsBgFgExplicitIds() {
        final Sh4j.Result bg = Sh4j.runFull("sleep 0.2 & bg %1");
        Assert.assertTrue(bg.stdout().contains("sleep"));
        final Sh4j.Result fg = Sh4j.runFull("sleep 0.05 & fg %1");
        Assert.assertTrue(fg.stdout().contains("sleep"));
        Assert.assertEquals(1, Sh4j.run("bg %999"));
        Assert.assertEquals(1, Sh4j.run("fg %999"));
    }

    @Test
    public void aliasQuotedAndEmptyValues() {
        Assert.assertEquals("", Sh4j.out("alias e=''", "e"));
        Assert.assertEquals(
            "a='x y'\n",
            Sh4j.out("alias a=\"x y\"; alias a")
        );
    }

    @Test
    public void getoptsMoreBoundaries() {
        Assert.assertEquals(
            "? 1 2\n",
            Sh4j.out("OPTIND=2; set -- -a; getopts a o; echo \"$o $? $OPTIND\"")
        );
        Assert.assertEquals(
            "a:val\n",
            Sh4j.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"")
        );
        Assert.assertEquals(
            "?:z\n",
            Sh4j.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"")
        );
        Assert.assertEquals(
            "?:\n",
            Sh4j.out("set -- -a; getopts a: o; echo \"$o:$OPTARG\"")
        );
        Assert.assertEquals(
            "? 1\n",
            Sh4j.out("set -- -a file; getopts a o; getopts a o; echo \"$o $?\"")
        );
    }

    @Test
    public void setOptionsAreObservableInFlags() {
        Assert.assertTrue(Sh4j.out("set -o errexit; echo $-").contains("e"));
        Assert.assertTrue(Sh4j.out("set -o nounset; echo $-").contains("u"));
        Assert.assertTrue(Sh4j.out("set -o noglob; echo $-").contains("f"));
        Assert.assertTrue(Sh4j.out("set -o xtrace; echo $-").contains("x"));
        Assert.assertTrue(Sh4j.out("set -o noexec; echo $-").contains("n"));
        Assert.assertTrue(Sh4j.out("set -o verbose; echo $-").contains("v"));
        Assert.assertTrue(Sh4j.out("set -o allexport; echo $-").contains("a"));
        Assert.assertEquals("\n", Sh4j.out("set -eu; set +eu; echo $-"));
    }

    @Test
    public void fcEditorAndRangeBoundaries() {
        final Sh4j.Result editor = Sh4j.runFull("echo one", "fc -e true 1");
        Assert.assertTrue(editor.stdout().contains("echo one"));
        Assert.assertEquals(0, Sh4j.runFull("echo one", "fc -l 3 2").exitCode());
        Assert.assertEquals(0, Sh4j.runFull("echo one", "fc -e true 1 1").exitCode());
    }

    @Test
    public void trapReturnCodesAndDotErrors() {
        Assert.assertEquals(0, Sh4j.run("trap"));
        Assert.assertEquals(0, Sh4j.run("trap - EXIT"));
        final Sh4j.Result dot = Sh4j.runFull(". /nonexistent/dotfile");
        Assert.assertEquals(1, dot.exitCode());
        Assert.assertTrue(dot.stderr().contains("cannot open"));
    }

    @Test
    public void breakZeroUsesOneLevel() {
        Assert.assertEquals(
            "1a\n2a\n",
            Sh4j.out("for i in 1 2; do for j in a b; do echo $i$j; break 0; done; done")
        );
    }

    @Test
    public void jobIdBoundaryForms() {
        Assert.assertEquals(0, Sh4j.run("jobs %0"));
        Assert.assertEquals(0, Sh4j.run("jobs -1"));
        Assert.assertEquals(1, Sh4j.run("bg %0"));
        Assert.assertEquals(1, Sh4j.run("fg %0"));
    }

    @Test
    public void killTermAndKillSemantics() throws Exception {
        final Process stubborn = new ProcessBuilder(
            "sh", "-c", "trap '' TERM; while true; do sleep 1; done"
        ).start();
        try {
            Thread.sleep(200L);
            final long pid = stubborn.toHandle().pid();
            Assert.assertEquals(0, Sh4j.run("kill " + pid));
            Thread.sleep(200L);
            Assert.assertTrue("TERM-ignoring process should survive", stubborn.isAlive());
            Assert.assertEquals(0, Sh4j.run("kill -KILL " + pid));
            Assert.assertTrue(stubborn.waitFor(5L, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    public void getoptsExactBoundaryBranches() {
        Assert.assertEquals(
            "?:1:1\n",
            Sh4j.out("set --; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
        Assert.assertEquals(
            "?::1\n",
            Sh4j.out("set -- -; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
        Assert.assertEquals(
            "?:2:1\n",
            Sh4j.out("set -- -a; getopts a o; getopts a o; echo \"$o:$OPTIND:$?\"")
        );
        Assert.assertEquals(
            "?:z\n",
            Sh4j.out("set -- -z; getopts :a o; echo \"$o:$OPTARG\"")
        );
        final Sh4j.Result illegal = Sh4j.runFull("set -- -z; getopts a o");
        Assert.assertTrue(illegal.stderr().contains("illegal option"));
        Assert.assertEquals(
            "a:val\n",
            Sh4j.out("set -- -aval; getopts a: o; echo \"$o:$OPTARG\"")
        );
    }

    @Test
    public void fcRangeEdgesAndSubstitution() {
        Assert.assertEquals(0, Sh4j.runFull("echo one", "fc -l 0").exitCode());
        Assert.assertTrue(
            Sh4j.runFull("echo one", "fc -l -1").stdout().contains("echo one")
        );
        Assert.assertEquals(0, Sh4j.runFull("echo one", "fc -l 5").exitCode());
        Assert.assertTrue(
            Sh4j.runFull("echo one", "fc -s one=two").stdout().contains("two")
        );
    }

    @Test
    public void jobsWithoutIdsUseMostRecentJob() {
        Assert.assertTrue(
            Sh4j.runFull("sleep 0.1 & jobs").stdout().contains("sleep")
        );
        Assert.assertTrue(
            Sh4j.runFull("sleep 0.1 & bg").stdout().contains("sleep")
        );
        Assert.assertTrue(
            Sh4j.runFull("sleep 0.1 & fg").stdout().contains("sleep")
        );
    }

    @Test
    public void dispatcherResidualBranches() throws IOException {
        Assert.assertTrue(Sh4j.out("command -V ls").contains(" is /"));
        Assert.assertEquals(1, Sh4j.run("command -V __no_such_cmd__"));
        Assert.assertEquals(
            "?\n",
            Sh4j.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"")
        );
        final Sh4j.Result missing = Sh4j.runFull("echo one; fc -s nope");
        Assert.assertEquals(1, missing.exitCode());
        Assert.assertTrue(missing.stderr().contains("-s requires pat=rep"));
        Assert.assertEquals(
            "hi\necho hey\nhey\n",
            Sh4j.out("echo hi; fc -s hi=hey")
        );
        Assert.assertEquals(
            "one\n1\techo one\n",
            Sh4j.out("echo one; fc -l abc")
        );
        final Path fd = this.tempdir.newFile("fd.txt").toPath();
        Sh4j.run("echo hi; exec 3>" + fd + "; fc -s nope 2>&3; exec 3>&-");
        Assert.assertTrue(
            new String(Files.readAllBytes(fd), StandardCharsets.UTF_8)
                .contains("fc: -s requires pat=rep")
        );
    }

    @Test
    public void pipelineAndDotResidualBranches() throws IOException {
        Assert.assertEquals(0, Sh4j.run("__no_such__ | cat"));
        Assert.assertEquals(127, Sh4j.run("echo x | __no_such__"));
        Assert.assertEquals("3\n", Sh4j.out("(exit 3); echo $?"));
        final Path dir = this.tempdir.newFolder("dotrel").toPath();
        Files.write(
            dir.resolve("s.sh"),
            "X=dot\n".getBytes(StandardCharsets.UTF_8)
        );
        Assert.assertEquals(
            "dot\n",
            Sh4j.out("cd " + dir + "; . ./s.sh; echo \"$X\"")
        );
    }

    @Test
    public void arithmeticOperatorResidualBranches() {
        Assert.assertEquals(
            "1 6 24 31\n8 1 1 1 1\n2 7 12 3\n1 -1 2\n",
            Sh4j.out(
                "echo $((2 ^ 3)) $((1+2+3)) $((2*3*4)) $((0x1f))",
                "echo $((010)) $((3>2)) $((3>=3)) $((3!=2)) $((3==3))",
                "echo $((6&3)) $((6|3)) $((6<<1)) $((6>>1))",
                "echo $((!0)) $((~0)) $((0?1:2))"
            )
        );
        Assert.assertEquals(
            "7 7 0 0\n",
            Sh4j.out("x=5; echo $((x+=2)) $((x)) $((x=0)) $((x))")
        );
    }

    @Test
    public void heredocTabStripAndJobLookupBranches() {
        Assert.assertEquals(
            "tabbed\n",
            Sh4j.out("cat <<-EOF\n\ttabbed\n\tEOF")
        );
        Assert.assertEquals(0, Sh4j.run("jobs 99"));
        Assert.assertEquals(127, Sh4j.run("kill -9 999999"));
        Assert.assertEquals(127, Sh4j.run("kill -KILL 999999"));
        Assert.assertEquals(1, Sh4j.run("command -v __no_such_cmd__"));
    }

    @Test
    public void compoundAssignmentAndGlobPrefixBranches() throws IOException {
        Assert.assertEquals(
            "12 6 2 3 11 22 7 1 0\n",
            Sh4j.out(
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))"
                + " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Assert.assertEquals("1\n", Sh4j.out("echo $((unknownvar+1))"));
        Assert.assertEquals("1\n", Sh4j.out("echo $((1+))"));
        Assert.assertEquals("0\n", Sh4j.out("echo $((x=))"));
        Assert.assertEquals(0, Sh4j.run("command"));
        Assert.assertEquals(1, Sh4j.run("command -v"));
        Assert.assertEquals(1, Sh4j.run("command -V"));
        Assert.assertEquals(1, Sh4j.run("echo x >&abc"));
        final Sh4j.Result unset = Sh4j.runFull("echo ${missing_q?}");
        Assert.assertEquals(1, unset.exitCode());
        Assert.assertTrue(unset.stderr().contains("parameter not set"));
        final Path dir = this.tempdir.newFolder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Assert.assertEquals(
            "sub/a.txt\n",
            Sh4j.out("cd " + dir + "; echo sub/*.txt")
        );
        Assert.assertEquals(
            dir.resolve("sub").resolve("a.txt") + "\n",
            Sh4j.out("echo " + dir + "/sub/*.txt")
        );
        Assert.assertEquals(
            "nodir/*.txt\n",
            Sh4j.out("cd " + dir + "; echo nodir/*.txt")
        );
        Assert.assertEquals("[]\n", Sh4j.out("echo []"));
        Assert.assertEquals(127, Sh4j.run("sleep 0.1 & kill -9 $!"));
    }
}
