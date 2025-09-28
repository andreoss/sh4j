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
    public void commentStartsAtTokenPositionOnly() {
        Assert.assertEquals("a\n", Sh4j.out("echo a #c"));
        Assert.assertEquals("b\n", Sh4j.out("#lead\necho b"));
        Assert.assertEquals("one#two\n", Sh4j.out("echo one#two"));
    }

    @Test
    public void bracketNegationAndReservedWordsAsArguments() {
        Assert.assertEquals(
            "notclass\n",
            Sh4j.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac")
        );
        Assert.assertEquals(
            "klass\n",
            Sh4j.out("case abc in [abc]*) echo klass;; *) echo no;; esac")
        );
        Assert.assertEquals("if then fi\n", Sh4j.out("echo if then fi"));
        Assert.assertEquals("multi\n", Sh4j.out("case aaab in a*b) echo multi;; esac"));
    }

    /**
     * POSIX 2.7.3 Appending Redirected Output and POSIX 2.7 Redirection.
     */
    @Test
    public void appendRedirectAndAssignmentPrefix() throws IOException {
        final File file = this.tempdir.newFile("append.txt");
        final Sh4j.Result result = Sh4j.runFull(
            String.format("echo a > %s; echo b >> %s; cat %s",
                file.getAbsolutePath(), file.getAbsolutePath(), file.getAbsolutePath())
        );
        Assert.assertEquals("a\nb\n", result.stdout());
        Assert.assertEquals("hi\n[]\n", Sh4j.out("x=1 echo hi; echo [$x]"));
    }

    @Test
    public void shiftBeyondCountAndQuotedCasePatterns() {
        Assert.assertEquals("rc=1\n", Sh4j.out("set -- a; shift 5; echo \"rc=$?\""));
        Assert.assertEquals("count=2\n", Sh4j.out("set -- a b c; shift 1; echo \"count=$#\""));
        Assert.assertEquals(
            "no\n",
            Sh4j.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac")
        );
        Assert.assertEquals(
            "lit\n",
            Sh4j.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac")
        );
        Assert.assertEquals("after\n", Sh4j.out("break; echo after"));
        Assert.assertEquals(
            "1a\n",
            Sh4j.out("for i in 1 2; do for j in a b; do echo $i$j; break 2; done; done")
        );
    }

    @Test
    public void readonlyAssignmentTerminatesShell() {
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(new byte[0]),
            "readonly R=1; R=2; echo after"
        );
        Assert.assertEquals(1, result.exitCode());
        Assert.assertEquals("", result.stdout());
    }

    @Test
    public void globExpandsDirectoryPrefix() throws IOException {
        final File dir = this.tempdir.newFolder("sub");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "b.log").toPath(), new byte[0]);
        Assert.assertEquals(
            String.format("%s/a.txt\n", dir.getAbsolutePath()),
            Sh4j.out(String.format("echo %s/*.txt", dir.getAbsolutePath()))
        );
    }

    /**
     * POSIX 2.4 Reserved Words: reserved words are only reserved in command
     * position, so they stay ordinary arguments elsewhere.
     */
    @Test
    public void posix24ReservedWordsAsArguments() {
        Assert.assertEquals("if then fi\n", Sh4j.out("echo if then fi"));
    }

    /**
     * POSIX 2.10.1 Shell Grammar Lexical Conventions, part of POSIX 2.10 Shell Grammar.: a {@code #} starts a
     * comment only at a token position.
     */
    @Test
    public void posix1010LexicalConventions() {
        Assert.assertEquals("one#two\n", Sh4j.out("echo one#two"));
        Assert.assertEquals("a\n", Sh4j.out("echo a  #trailing"));
    }

    /**
     * POSIX 2.10.2 Shell Grammar Rules: assignments preceding the command
     * name affect only that command's environment.
     */
    @Test
    public void posix1020AssignmentPrefixScope() {
        Assert.assertEquals("hi\n[]\n", Sh4j.out("x=1 echo hi; echo [$x]"));
    }

    /**
     * POSIX 2.11 Signals and Error Handling: {@code set -e} stops the shell
     * on a failing simple command.
     */
    @Test
    public void posix211SetEErrorHandling() {
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(new byte[0]),
            "set -e; false; echo nope"
        );
        Assert.assertEquals(1, result.exitCode());
        Assert.assertEquals("", result.stdout());
    }

    /**
     * POSIX 2.12 Shell Execution Environment: a subshell inherits variables
     * but its directory change does not affect the parent.
     */
    @Test
    public void posix212SubshellEnvironment() {
        final String cwd = Sh4j.out("pwd").trim();
        Assert.assertEquals(
            String.format("1\n/\n%s\n", cwd),
            Sh4j.out("v=1; (echo $v); (cd /; pwd); pwd")
        );
    }

    /**
     * POSIX 2.13.1 Patterns Matching a Single Character, part of POSIX 2.13 Pattern Matching Notation.: {@code ?} and
     * bracket negation with {@code !}.
     */
    @Test
    public void posix2131SingleCharacterPatterns() {
        Assert.assertEquals("q\n", Sh4j.out("case ab in a?) echo q;; *) echo n;; esac"));
        Assert.assertEquals("n\n", Sh4j.out("case z in [!abc]) echo n;; *) echo no;; esac"));
        Assert.assertEquals("k\n", Sh4j.out("case b in [abc]) echo k;; *) echo no;; esac"));
    }

    /**
     * POSIX 2.13.2 Patterns Matching Multiple Characters: {@code *} spans
     * any run of characters.
     */
    @Test
    public void posix2132MultipleCharacterPatterns() {
        Assert.assertEquals("m\n", Sh4j.out("case aaab in a*b) echo m;; *) echo no;; esac"));
    }

    /**
     * POSIX 2.13.3 Patterns Used for Filename Expansion: glob characters
     * expand against the shell's working directory.
     */
    @Test
    public void posix2133FilenameExpansion() throws IOException {
        final File dir = this.tempdir.newFolder("pathexpand");
        Files.write(new File(dir, "one.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "two.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "skip.log").toPath(), new byte[0]);
        Assert.assertEquals(
            "one.txt two.txt\n",
            Sh4j.out(String.format("cd %s; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    public void redirectionsResolveAgainstShellCwd() throws IOException {
        final File dir = this.tempdir.newFolder("cwdtest");
        final String base = dir.getAbsolutePath();
        Assert.assertEquals(
            "seed\n",
            Sh4j.out(
                String.format(
                    "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"", base
                )
            )
        );
        Assert.assertEquals(
            "fd-ok\n",
            Sh4j.out(
                String.format(
                    "cd %s; exec 3>fdout.txt; echo fd-ok >&3; exec 3>&-; cat fdout.txt", base
                )
            )
        );
        Assert.assertEquals(
            "fd-ok\n",
            new String(
                Files.readAllBytes(new File(dir, "fdout.txt").toPath()),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    public void execRedirectionFailureTerminatesShell() {
        final Sh4j.Result input = Sh4j.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        );
        Assert.assertEquals(1, input.exitCode());
        Assert.assertEquals("", input.stdout());
        final Sh4j.Result output = Sh4j.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec >/nonexistent-dir-xyz/f; echo after"
        );
        Assert.assertEquals(1, output.exitCode());
        Assert.assertEquals("", output.stdout());
    }

    @Test
    public void descriptorRedirectionsPersistAcrossCommands() throws IOException {
        final File file = this.tempdir.newFile("fd.txt");
        Files.write(file.toPath(), "seed\n".getBytes(StandardCharsets.UTF_8));
        final String path = file.getAbsolutePath();
        Assert.assertEquals(
            "v=seed\n",
            Sh4j.out(
                String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", path)
            )
        );
        Assert.assertEquals(
            "w=seed\n",
            Sh4j.out(
                String.format("exec 4<%s; read w <&4; echo \"w=$w\"", path)
            )
        );
        Assert.assertEquals(
            "y=[]\n",
            Sh4j.runFull(
                new ByteArrayInputStream(new byte[0]),
                String.format("exec 5<%s; exec 5<&-; read y <&5; echo \"y=[$y]\"", path)
            ).stdout()
        );
    }

    /**
     * POSIX 2.8 Exit Status and POSIX 2.9 Shell Commands: `exit` ends the
     * list and propagates its status.
     */
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

    /**
     * POSIX 2.9.4 Compound Commands: nested loop level unwinding.
     */
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
        Assert.assertEquals(0, Sh4j.run("break"));
        Assert.assertEquals(0, Sh4j.run("continue"));
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
