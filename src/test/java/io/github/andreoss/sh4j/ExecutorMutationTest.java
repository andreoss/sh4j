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
 * Additional executor state/control-flow tests selected from PIT survivors.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class ExecutorMutationTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void subshellInheritsAliasesFunctionsAndPositionals() {
        Assert.assertEquals(
            "A\nF\nx\n",
            Sh4j.out(
                "alias a='echo A'; f() { echo F; }; set -- x; (a); (f); (echo $1)"
            )
        );
    }

    @Test
    public void subshellInheritsFlagsAndCwd() throws IOException {
        final File dir = this.tempdir.newFolder("subcwd");
        Assert.assertEquals(
            "u\n" + dir.getCanonicalPath() + "\n",
            Sh4j.out(
                String.format("set -u; cd %s; (echo $-); (pwd)", dir.getAbsolutePath())
            )
        );
    }

    @Test
    public void trapRuntimeErrorIsReported() {
        final Sh4j.Result result = Sh4j.runFull("trap 'if' EXIT; echo body");
        Assert.assertEquals("body\n", result.stdout());
        Assert.assertTrue(result.stderr().contains("sh: trap:"));
    }

    @Test
    public void topLevelLoopAndReturnSignalsAreReported() {
        final Sh4j.Result broken = Sh4j.runFull("break");
        Assert.assertEquals(0, broken.exitCode());
        Assert.assertTrue(broken.stderr().contains("break: not in a loop"));
        final Sh4j.Result continued = Sh4j.runFull("continue");
        Assert.assertEquals(0, continued.exitCode());
        Assert.assertTrue(continued.stderr().contains("continue: not in a loop"));
        final Sh4j.Result returned = Sh4j.runFull("return 5");
        Assert.assertEquals(5, returned.exitCode());
        Assert.assertTrue(returned.stderr().contains("return: not in a function"));
    }

    @Test
    public void ifBranchesPropagateBodyStatus() {
        Assert.assertEquals(1, Sh4j.run("if true; then false; fi"));
        Assert.assertEquals(0, Sh4j.run("if false; then :; fi"));
        Assert.assertEquals(1, Sh4j.run("if false; then :; else false; fi"));
    }

    @Test
    public void forWithoutWordsAndBodyStatus() {
        Assert.assertEquals("", Sh4j.out("for i; do echo $i; done"));
        Assert.assertEquals(1, Sh4j.run("for i in a b; do false; done"));
        Assert.assertEquals(0, Sh4j.run("for i in a b; do break; done"));
    }

    @Test
    public void caseWithoutMatchReturnsZero() {
        Assert.assertEquals(0, Sh4j.run("case x in y) false;; esac"));
        Assert.assertEquals("", Sh4j.out("case x in y) echo no;; esac"));
    }

    @Test
    public void aliasAndExecStatuses() {
        Assert.assertEquals(1, Sh4j.run("alias a='false'", "a"));
        Assert.assertEquals(0, Sh4j.run("alias a='true'", "a"));
        Assert.assertEquals(7, Sh4j.run("exec sh -c 'exit 7'"));
    }

    @Test
    public void aliasExpansionUsesTheTableCapturedAtReadTime() {
        Assert.assertEquals(127, Sh4j.run("alias q='echo Q'; q"));
        Assert.assertEquals("Q\n", Sh4j.out("alias q='echo Q'", "q"));
        Assert.assertEquals(127, Sh4j.run("alias e='echo X'; f() { e; }; f"));
    }

    @Test
    public void functionRestoresPositionalsAndReturnStatus() {
        Assert.assertEquals(
            "inner\nouter\n",
            Sh4j.out("set -- outer; f() { echo $1; }; f inner; echo $1")
        );
        Assert.assertEquals(6, Sh4j.run("f() { return 6; }; f"));
    }

    @Test
    public void builtinHeredocInputForms() {
        final Sh4j.Result plain = Sh4j.runFull(
            "read x <<EOF\nhi\nEOF\necho \"$x\""
        );
        Assert.assertEquals("hi\n", plain.stdout());
        final Sh4j.Result quoted = Sh4j.runFull(
            "Y=value; read x <<'EOF'\n$Y\nEOF\necho \"$x\""
        );
        Assert.assertEquals("$Y\n", quoted.stdout());
    }

    @Test
    public void builtinInputFromMissingFileFallsBackToStdin() {
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream("fallback\n".getBytes(StandardCharsets.UTF_8)),
            "read x < /nonexistent/input; echo \"$x\""
        );
        Assert.assertEquals("fallback\n", result.stdout());
    }

    @Test
    public void externalInputOutputAndStderrPipes() throws IOException {
        final Sh4j.Result input = Sh4j.runFull(
            new ByteArrayInputStream("piped\n".getBytes(StandardCharsets.UTF_8)),
            "cat"
        );
        Assert.assertEquals("piped\n", input.stdout());
        final File file = this.tempdir.newFile("ext.out");
        final Sh4j.Result output = Sh4j.runFull(
            String.format("echo redirected > %s", file.getAbsolutePath())
        );
        Assert.assertEquals("", output.stdout());
        Assert.assertEquals(
            "redirected\n",
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        final Sh4j.Result error = Sh4j.runFull("sh -c 'echo stderr >&2'");
        Assert.assertTrue(error.stderr().contains("stderr"));
    }

    @Test
    public void expandFieldsQuotedAtAndEmptyPositionals() {
        Assert.assertEquals("", Sh4j.out("for x in \"$@\"; do echo $x; done"));
        Assert.assertEquals("\n", Sh4j.out("echo \"$@\""));
        Assert.assertEquals("ab\n", Sh4j.out("IFS=; set -- a b; echo \"$*\""));
    }

    @Test
    public void specialParameterBoundaries() {
        Assert.assertEquals("2\n", Sh4j.out("set -- a b; echo ${#}"));
        Assert.assertEquals("\n", Sh4j.out("set -- a; echo $9"));
        Assert.assertEquals("\n", Sh4j.out("echo \"$@\""));
        Assert.assertEquals("\n", Sh4j.out("set --; echo $1"));
    }

    @Test
    public void nonExecutableFileReturns126() throws IOException {
        final File file = this.tempdir.newFile("not-executable.sh");
        Files.write(file.toPath(), "echo hi\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertTrue(file.setExecutable(false));
        Assert.assertEquals(126, Sh4j.run(file.getAbsolutePath()));
    }

    @Test
    public void quotedGlobIsNotExpandedForExternalCommand() throws IOException {
        final File dir = this.tempdir.newFolder("quoted-glob");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Assert.assertEquals(
            "<*.txt>\n",
            Sh4j.out(
                String.format("cd %s; printf '<%%s>\\n' \"*.txt\"", dir.getAbsolutePath())
            )
        );
        Assert.assertEquals(
            "<a.txt>\n",
            Sh4j.out(
                String.format("cd %s; printf '<%%s>\\n' *.txt", dir.getAbsolutePath())
            )
        );
    }

    @Test
    public void functionStderrRedirection() throws IOException {
        final File file = this.tempdir.newFile("fn-err.txt");
        final Sh4j.Result result = Sh4j.runFull(
            String.format("f() { echo err >&2; }; f 2> %s; cat %s", file.getAbsolutePath(), file.getAbsolutePath())
        );
        Assert.assertEquals("err\n", result.stdout());
    }

    @Test
    public void execInputRedirectionPersists() throws IOException {
        final File file = this.tempdir.newFile("exec-in.txt");
        Files.write(file.toPath(), "input\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            "input\n",
            Sh4j.out(String.format("exec < %s; read x; echo $x", file.getAbsolutePath()))
        );
    }

    @Test
    public void breakThreeLevels() {
        Assert.assertEquals(
            "1ax\n",
            Sh4j.out(
                "for i in 1 2; do for j in a b; do for k in x y; do echo $i$j$k; break 3; done; done; done"
            )
        );
    }

    @Test
    public void specialBuiltinEmptyAssignmentPersists() {
        Assert.assertEquals(
            ":1\n",
            Sh4j.out("X= export Y=1; echo \"${X-unset}:$Y\"")
        );
    }
}
