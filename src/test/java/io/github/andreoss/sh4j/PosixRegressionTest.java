/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Regression tests for the shell semantics repaired after the Step 37
 * conformance audit.
 * @since 1.0
 */
public final class PosixRegressionTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void exitStopsListAndPropagatesStatus() {
        final Sh4j.Result result = Sh4j.runFull("exit 3; echo after");
        Assert.assertEquals(3, result.exitCode());
        Assert.assertEquals("", result.stdout());
    }

    @Test
    public void execRedirectionPersists() throws IOException {
        final File file = this.tempdir.newFile("exec.out");
        final Sh4j.Result result = Sh4j.runFull(
            String.format("exec > %s; echo redirected", file.getAbsolutePath())
        );
        Assert.assertEquals(0, result.exitCode());
        Assert.assertEquals("", result.stdout());
        Assert.assertEquals(
            "redirected\n",
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void execCommandDoesNotResumeShell() {
        final Sh4j.Result result = Sh4j.runFull("exec echo hi; echo after");
        Assert.assertEquals(0, result.exitCode());
        Assert.assertEquals("hi\n", result.stdout());
    }

    @Test
    public void subshellCdDoesNotChangeParentCwd() {
        final Sh4j.Result result = Sh4j.runFull(
            "cd /tmp; (cd /); pwd"
        );
        Assert.assertEquals("/tmp\n", result.stdout());
    }

    @Test
    public void shellVariablesAreNotExported() {
        final Sh4j.Result result = Sh4j.runFull(
            "X=secret; sh -c 'echo ${X-unset}'"
        );
        Assert.assertEquals("unset\n", result.stdout());
    }

    @Test
    public void exportedValueLosesQuotes() {
        final Sh4j.Result result = Sh4j.runFull(
            "export E='x y'; sh -c 'printf \"%s\\n\" \"$E\"'"
        );
        Assert.assertEquals("x y\n", result.stdout());
    }

    @Test
    public void readonlySurvivesUnset() {
        final Sh4j.Result result = Sh4j.runFull(
            "readonly R='a b'; unset R; echo after"
        );
        Assert.assertEquals(1, result.exitCode());
        Assert.assertEquals("", result.stdout());
        Assert.assertTrue(result.stderr().contains("readonly"));
    }

    @Test
    public void setPlusEClearsErrexit() {
        Assert.assertEquals(
            "alive\n",
            Sh4j.out("set -e; set +e; false; echo alive")
        );
    }

    @Test
    public void nounsetAbortsScript() {
        final Sh4j.Result result = Sh4j.runFull(
            "set -u; echo \"$MISSING\"; echo after"
        );
        Assert.assertEquals(1, result.exitCode());
        Assert.assertEquals("", result.stdout());
        Assert.assertTrue(result.stderr().contains("MISSING"));
    }

    @Test
    public void readConsumesOneLinePerCall() {
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream("x\ny\n".getBytes(StandardCharsets.UTF_8)),
            "read a; read b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a=x b=y\n", result.stdout());
    }

    @Test
    public void quotedAtPreservesArgumentBoundaries() {
        Assert.assertEquals(
            "[a b]\n[c]\n",
            Sh4j.out("set -- \"a b\" c; for x in \"$@\"; do echo \"[$x]\"; done")
        );
    }

    @Test
    public void starJoinsWithFirstIfsCharacter() {
        Assert.assertEquals(
            "a:b\n",
            Sh4j.out("set -- a b; IFS=:; echo \"$*\"")
        );
    }

    @Test
    public void commandSubstitutionStripsAllTrailingNewlines() {
        Assert.assertEquals(
            "<a>\n",
            Sh4j.out("X=$(printf 'a\\n\\n\\n'); printf '<%s>\\n' \"$X\"")
        );
    }

    @Test
    public void backtickInsideSingleQuotesIsLiteral() {
        Assert.assertEquals("`date`\n", Sh4j.out("echo '`date`'"));
    }

    @Test
    public void casePatternUndergoesExpansion() {
        Assert.assertEquals(
            "yes\n",
            Sh4j.out("p='h*'; case hi in $p) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    public void globSkipsDotfilesAndSorts() throws IOException {
        final File dir = this.tempdir.newFolder("globfix");
        Assert.assertTrue(new File(dir, ".hidden").createNewFile());
        Assert.assertTrue(new File(dir, "b.txt").createNewFile());
        Assert.assertTrue(new File(dir, "a.txt").createNewFile());
        Assert.assertEquals(
            "a.txt b.txt\n",
            Sh4j.out(String.format("cd %s; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    public void noglobDisablesPathnameExpansion() throws IOException {
        final File dir = this.tempdir.newFolder("noglob");
        Assert.assertTrue(new File(dir, "a.txt").createNewFile());
        Assert.assertEquals(
            "*.txt\n",
            Sh4j.out(String.format("cd %s; set -f; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    public void typeRecognisesReadBuiltin() {
        Assert.assertEquals("read is a shell built-in\n", Sh4j.out("type read"));
    }

    @Test
    public void commandBypassesFunctionLookup() {
        final Sh4j.Result result = Sh4j.runFull(
            "f() { echo function; }; command f"
        );
        Assert.assertEquals(127, result.exitCode());
        Assert.assertEquals("", result.stdout());
    }

    @Test
    public void pipelineRunsCompoundStages() {
        Assert.assertEquals(
            "hi\n",
            Sh4j.out("if true; then echo hi; fi | cat")
        );
    }

    @Test
    public void pipelineStageRedirectionIsApplied() throws IOException {
        final File file = this.tempdir.newFile("pipe.out");
        final Sh4j.Result result = Sh4j.runFull(
            String.format("echo hi > %s | cat", file.getAbsolutePath())
        );
        Assert.assertEquals("", result.stdout());
        Assert.assertEquals(
            "hi\n",
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void pipelineCanFeedBuiltinRead() {
        Assert.assertEquals(
            "v=x\n",
            Sh4j.out("printf 'x\\n' | { read v; echo \"v=$v\"; }")
        );
    }

    @Test
    public void backgroundJobInheritsFunctionsAndWaitJoinsIt() {
        Assert.assertEquals(
            "F\n",
            Sh4j.out("f() { echo F; }; f & wait")
        );
    }

    @Test
    public void dollarBangIsNotAnOsPid() {
        final Sh4j.Result result = Sh4j.runFull("sleep 0.1 & kill $!");
        Assert.assertEquals(127, result.exitCode());
    }

    @Test
    public void trapDotAndTimesWork() throws IOException {
        final File lib = this.tempdir.newFile("lib.sh");
        Files.write(lib.toPath(), "LIB=ok\n".getBytes(StandardCharsets.UTF_8));
        final Sh4j.Result result = Sh4j.runFull(
            String.format(
                "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                lib.getAbsolutePath()
            )
        );
        Assert.assertTrue(result.stdout().contains("lib=ok"));
        Assert.assertTrue(result.stdout().contains("0m0.000s"));
        Assert.assertTrue(result.stdout().contains("caught"));
    }

    @Test
    public void externalCommandRunsInExecutorCwd() throws IOException {
        final File dir = this.tempdir.newFolder("cwd");
        Assert.assertEquals(
            dir.getCanonicalPath() + "\n",
            Sh4j.out(String.format("cd %s; sh -c pwd", dir.getAbsolutePath()))
        );
    }

    @Test
    public void xtraceWritesCommandToStderr() {
        final Sh4j.Result result = Sh4j.runFull("set -x; echo hi");
        Assert.assertTrue(result.stderr().contains("+ echo hi"));
        Assert.assertEquals("hi\n", result.stdout());
    }

    @Test
    public void execMissingCommandReturns127() {
        Assert.assertEquals(127, Sh4j.runFull("exec nonexistent_exec_xyz").exitCode());
    }

    @Test
    public void trapListingAndRemoval() {
        final Sh4j.Result result = Sh4j.runFull(
            "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
        );
        Assert.assertTrue(result.stdout().contains("trap -- 'echo t' EXIT"));
        Assert.assertTrue(result.stdout().contains("body"));
        Assert.assertFalse(result.stdout().contains("t\n"));
    }

    @Test
    public void dotMissingFileReturnsOne() {
        final Sh4j.Result result = Sh4j.runFull(". /nonexistent/library.sh");
        Assert.assertEquals(1, result.exitCode());
        Assert.assertTrue(result.stderr().contains("cannot open"));
    }

    @Test
    public void dotSetsPositionalParameters() throws IOException {
        final File lib = this.tempdir.newFile("args-lib.sh");
        Files.write(lib.toPath(), "echo \"$1\"\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            "arg\n",
            Sh4j.out(String.format(". %s arg", lib.getAbsolutePath()))
        );
    }

    @Test
    public void killListsAndRejectsJobPids() {
        Assert.assertTrue(Sh4j.out("kill -l").contains("TERM"));
        Assert.assertEquals(127, Sh4j.run("kill notapid"));
        Assert.assertEquals(127, Sh4j.run("sleep 0.1 & kill $!"));
    }

    @Test
    public void waitUnknownPidReturns127() {
        Assert.assertEquals(127, Sh4j.run("wait 999999"));
    }

    @Test
    public void commandVerboseDescribesFunction() {
        Assert.assertTrue(
            Sh4j.out("f() { :; }; command -V f").contains("f is a function")
        );
    }

    @Test
    public void exportAndReadonlyPrintLists() {
        Assert.assertTrue(Sh4j.out("export -p").contains("export PATH="));
        Assert.assertTrue(
            Sh4j.out("readonly R=1; readonly -p").contains("readonly R=1")
        );
    }

    @Test
    public void readReplyAndRawMode() {
        final Sh4j.Result reply = Sh4j.runFull(
            new ByteArrayInputStream("hello\n".getBytes(StandardCharsets.UTF_8)),
            "read; echo \"$REPLY\""
        );
        Assert.assertEquals("hello\n", reply.stdout());
        final Sh4j.Result raw = Sh4j.runFull(
            new ByteArrayInputStream("a\\tb\n".getBytes(StandardCharsets.UTF_8)),
            "read -r x; echo \"$x\""
        );
        Assert.assertEquals("a\\tb\n", raw.stdout());
    }

    @Test
    public void readAtEofReturnsOne() {
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(new byte[0]),
            "read x; echo status=$?"
        );
        Assert.assertEquals("status=1\n", result.stdout());
    }

    @Test
    public void functionRedirectionAndPrefixAssignment() throws IOException {
        final File file = this.tempdir.newFile("fn.out");
        Assert.assertEquals(
            "1\n",
            Sh4j.out("f() { echo $X; }; X=1 f")
        );
        Sh4j.run(String.format("f() { echo hi; }; f > %s", file.getAbsolutePath()));
        Assert.assertEquals(
            "hi\n",
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void pipelineStatusComesFromLastStage() {
        Assert.assertEquals(1, Sh4j.run("true | false"));
        Assert.assertEquals(0, Sh4j.run("false | true"));
    }

    @Test
    public void backgroundWaitReturnsJobStatus() {
        Assert.assertEquals(3, Sh4j.run("sh -c 'exit 3' & wait $!"));
    }

    @Test
    public void setDashOOptions() {
        Assert.assertEquals(
            1,
            Sh4j.run("set -o nounset; echo $MISSING_OPTION_VAR")
        );
    }

    @Test
    public void subshellExitOnlyLeavesSubshell() {
        Assert.assertEquals(
            "after\n",
            Sh4j.out("(exit 3); echo after")
        );
    }

    @Test
    public void nestedBreakAndContinueLevels() {
        Assert.assertEquals(
            "1a\n",
            Sh4j.out("for i in 1 2; do for j in a b; do echo $i$j; break 2; done; done")
        );
        Assert.assertEquals(
            "1a\n2a\n",
            Sh4j.out("for i in 1 2; do for j in a b; do echo $i$j; continue 2; done; done")
        );
    }

    @Test
    public void illegalLoopAndReturnDiagnostics() {
        Assert.assertEquals(1, Sh4j.run("break"));
        Assert.assertEquals(1, Sh4j.run("continue"));
        Assert.assertEquals(5, Sh4j.run("return 5"));
    }

    @Test
    public void returnInsideFunction() {
        Assert.assertEquals(4, Sh4j.run("f() { return 4; }; f"));
    }

    @Test
    public void forWithoutInUsesPositionalParameters() {
        Assert.assertEquals(
            "a\nb\n",
            Sh4j.out("set -- a b; for i; do echo $i; done")
        );
    }

    @Test
    public void pipelineExitAndRuntimeSignalsStayInSubshell() {
        final Sh4j.Result exit = Sh4j.runFull("exit 1 | cat; echo after");
        Assert.assertEquals("after\n", exit.stdout());
        final Sh4j.Result broken = Sh4j.runFull("break | cat; echo after");
        Assert.assertEquals("after\n", broken.stdout());
    }

    @Test
    public void execWithCommandAndRedirection() throws IOException {
        final File file = this.tempdir.newFile("exec-arg.out");
        final Sh4j.Result result = Sh4j.runFull(
            String.format("exec echo hi > %s", file.getAbsolutePath())
        );
        Assert.assertEquals(0, result.exitCode());
        Assert.assertEquals(
            "hi\n",
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void specialBuiltinAssignmentPersists() {
        Assert.assertEquals(
            "12\n",
            Sh4j.out("X=1 export Y=2; echo $X$Y")
        );
    }

    @Test
    public void externalCommandReceivesPrefixAssignment() {
        Assert.assertEquals(
            "1\n",
            Sh4j.out("X=1 sh -c 'echo $X'")
        );
    }

    @Test
    public void emptyAndMissingCommandsReturn127() {
        Assert.assertEquals(127, Sh4j.run("PATH=; ls"));
        Assert.assertEquals(127, Sh4j.run("/nonexistent/command"));
        Assert.assertEquals("ok\n", Sh4j.out("PATH=:/bin; true; echo ok"));
    }

    @Test
    public void trapRuntimeErrorDoesNotCrashExit() {
        final Sh4j.Result result = Sh4j.runFull("trap 'if' EXIT; echo body");
        Assert.assertEquals("body\n", result.stdout());
    }

    @Test
    public void unsetFunctionAndCommandLookup() {
        Assert.assertEquals(
            1, Sh4j.run("f() { :; }; unset -f f; type f")
        );
        Assert.assertEquals(0, Sh4j.run("command"));
        Assert.assertEquals("echo\n", Sh4j.out("command -v echo"));
    }

    @Test
    public void hashWaitAndKillOptionForms() {
        Assert.assertEquals(0, Sh4j.run("hash echo"));
        Assert.assertEquals(0, Sh4j.run("sleep 0.1 & wait"));
        Assert.assertEquals(127, Sh4j.run("kill -s TERM 999999"));
        Assert.assertEquals(127, Sh4j.run("kill -9 999999"));
    }

    @Test
    public void trapEmptyActionIsIgnored() {
        Assert.assertEquals("body\n", Sh4j.out("trap '' EXIT; echo body"));
    }

    @Test
    public void dotWithoutFileReturnsOne() {
        Assert.assertEquals(1, Sh4j.run("."));
    }

    @Test
    public void readLastVariableReceivesRemainder() {
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream("x y z\n".getBytes(StandardCharsets.UTF_8)),
            "read a b; echo \"a=$a b=$b\""
        );
        Assert.assertEquals("a=x b=y z\n", result.stdout());
    }

    @Test
    public void jobsAndFgWithNoJobReportErrors() {
        Assert.assertEquals(0, Sh4j.run("jobs"));
        Assert.assertEquals(1, Sh4j.run("fg"));
        Assert.assertEquals(1, Sh4j.run("bg"));
    }
}
