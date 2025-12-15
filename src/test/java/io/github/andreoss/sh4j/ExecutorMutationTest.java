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
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Additional executor state/control-flow tests selected from PIT survivors.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class ExecutorMutationTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void subshellInheritsAliasesFunctionsAndPositionals() {
        assertThat(
            Sh4j.out("alias a='echo A'; f() { echo F; }; set -- x; (a); (f); (echo $1)"),
            equalTo("A\nF\nx\n")
        );
    }

    @Test
    public void subshellInheritsFlagsAndCwd() throws IOException {
        final File dir = this.tempdir.newFolder("subcwd");
        assertThat(
            Sh4j.out(String.format("set -u; cd %s; (echo $-); (pwd)", dir.getAbsolutePath())),
            equalTo("u\n" + dir.getCanonicalPath() + "\n")
        );
    }

    @Test
    public void trapRuntimeErrorIsReported() {
        assertThat(
            Sh4j.runFull("trap 'if' EXIT; echo body").stdout(),
            equalTo("body\n")
        );
        assertThat(
            Sh4j.runFull("trap 'if' EXIT; echo body").stderr(),
            CoreMatchers.containsString("sh: trap:")
        );
    }

    @Test
    public void topLevelLoopAndReturnSignalsAreReported() {
        assertThat(Sh4j.runFull("break").exitCode(), equalTo(0));
        assertThat(Sh4j.runFull("break").stderr(), CoreMatchers.containsString("break: not in a loop"));
        assertThat(Sh4j.runFull("continue").exitCode(), equalTo(0));
        assertThat(Sh4j.runFull("continue").stderr(), CoreMatchers.containsString("continue: not in a loop"));
        assertThat(Sh4j.runFull("return 5").exitCode(), equalTo(5));
        assertThat(Sh4j.runFull("return 5").stderr(), CoreMatchers.containsString("return: not in a function"));
    }

    @Test
    public void ifBranchesPropagateBodyStatus() {
        assertThat(Sh4j.run("if true; then false; fi"), equalTo(1));
        assertThat(Sh4j.run("if false; then :; fi"), equalTo(0));
        assertThat(Sh4j.run("if false; then :; else false; fi"), equalTo(1));
    }

    @Test
    public void forWithoutWordsAndBodyStatus() {
        assertThat(Sh4j.out("for i; do echo $i; done"), equalTo(""));
        assertThat(Sh4j.run("for i in a b; do false; done"), equalTo(1));
        assertThat(Sh4j.run("for i in a b; do break; done"), equalTo(0));
    }

    @Test
    public void caseWithoutMatchReturnsZero() {
        assertThat(Sh4j.run("case x in y) false;; esac"), equalTo(0));
        assertThat(Sh4j.out("case x in y) echo no;; esac"), equalTo(""));
    }

    @Test
    public void aliasAndExecStatuses() {
        assertThat(Sh4j.run("alias a='false'", "a"), equalTo(1));
        assertThat(Sh4j.run("alias a='true'", "a"), equalTo(0));
        assertThat(Sh4j.run("exec sh -c 'exit 7'"), equalTo(7));
    }

    @Test
    public void aliasExpansionUsesTheTableCapturedAtReadTime() {
        assertThat(Sh4j.run("alias q='echo Q'; q"), equalTo(127));
        assertThat(Sh4j.out("alias q='echo Q'", "q"), equalTo("Q\n"));
        assertThat(Sh4j.run("alias e='echo X'; f() { e; }; f"), equalTo(127));
    }

    @Test
    public void functionRestoresPositionalsAndReturnStatus() {
        assertThat(
            Sh4j.out("set -- outer; f() { echo $1; }; f inner; echo $1"),
            equalTo("inner\nouter\n")
        );
        assertThat(Sh4j.run("f() { return 6; }; f"), equalTo(6));
    }

    @Test
    public void builtinHeredocInputForms() {
        assertThat(
            Sh4j.runFull("read x <<EOF\nhi\nEOF\necho \"$x\"").stdout(),
            equalTo("hi\n")
        );
        assertThat(
            Sh4j.runFull("Y=value; read x <<'EOF'\n$Y\nEOF\necho \"$x\"").stdout(),
            equalTo("$Y\n")
        );
    }

    @Test
    public void builtinInputFromMissingFileFallsBackToStdin() {
        assertThat(
            Sh4j.runFull(
                new ByteArrayInputStream("fallback\n".getBytes(StandardCharsets.UTF_8)),
                "read x < /nonexistent/input; echo \"$x\""
            ).stdout(),
            equalTo("fallback\n")
        );
    }

    @Test
    public void externalInputOutputAndStderrPipes() throws IOException {
        assertThat(
            Sh4j.runFull(
                new ByteArrayInputStream("piped\n".getBytes(StandardCharsets.UTF_8)),
                "cat"
            ).stdout(),
            equalTo("piped\n")
        );
        final File file = this.tempdir.newFile("ext.out");
        assertThat(
            Sh4j.runFull(String.format("echo redirected > %s", file.getAbsolutePath())).stdout(),
            equalTo("")
        );
        assertThat(
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8),
            equalTo("redirected\n")
        );
        assertThat(
            Sh4j.runFull("sh -c 'echo stderr >&2'").stderr(),
            CoreMatchers.containsString("stderr")
        );
    }

    @Test
    public void expandFieldsQuotedAtAndEmptyPositionals() {
        assertThat(Sh4j.out("for x in \"$@\"; do echo $x; done"), equalTo(""));
        assertThat(Sh4j.out("echo \"$@\""), equalTo("\n"));
        assertThat(Sh4j.out("IFS=; set -- a b; echo \"$*\""), equalTo("ab\n"));
    }

    @Test
    public void specialParameterBoundaries() {
        assertThat(Sh4j.out("set -- a b; echo ${#}"), equalTo("2\n"));
        assertThat(Sh4j.out("set -- a; echo $9"), equalTo("\n"));
        assertThat(Sh4j.out("echo \"$@\""), equalTo("\n"));
        assertThat(Sh4j.out("set --; echo $1"), equalTo("\n"));
    }

    @Test
    public void nonExecutableFileReturns126() throws IOException {
        final File file = this.tempdir.newFile("not-executable.sh");
        Files.write(file.toPath(), "echo hi\n".getBytes(StandardCharsets.UTF_8));
        assertThat(file.setExecutable(false), is(true));
        assertThat(Sh4j.run(file.getAbsolutePath()), equalTo(126));
    }

    @Test
    public void quotedGlobIsNotExpandedForExternalCommand() throws IOException {
        final File dir = this.tempdir.newFolder("quoted-glob");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        assertThat(
            Sh4j.out(String.format("cd %s; printf '<%%s>\\n' \"*.txt\"", dir.getAbsolutePath())),
            equalTo("<*.txt>\n")
        );
        assertThat(
            Sh4j.out(String.format("cd %s; printf '<%%s>\\n' *.txt", dir.getAbsolutePath())),
            equalTo("<a.txt>\n")
        );
    }

    @Test
    public void functionStderrRedirection() throws IOException {
        final File file = this.tempdir.newFile("fn-err.txt");
        assertThat(
            Sh4j.runFull(
                String.format("f() { echo err >&2; }; f 2> %s; cat %s", file.getAbsolutePath(), file.getAbsolutePath())
            ).stdout(),
            equalTo("err\n")
        );
    }

    @Test
    public void execInputRedirectionPersists() throws IOException {
        final File file = this.tempdir.newFile("exec-in.txt");
        Files.write(file.toPath(), "input\n".getBytes(StandardCharsets.UTF_8));
        assertThat(
            Sh4j.out(String.format("exec < %s; read x; echo $x", file.getAbsolutePath())),
            equalTo("input\n")
        );
    }

    @Test
    public void breakThreeLevels() {
        assertThat(
            Sh4j.out("for i in 1 2; do for j in a b; do for k in x y; do echo $i$j$k; break 3; done; done; done"),
            equalTo("1ax\n")
        );
    }

    @Test
    public void specialBuiltinEmptyAssignmentPersists() {
        assertThat(Sh4j.out("X= export Y=1; echo \"${X-unset}:$Y\""), equalTo(":1\n"));
    }
}
