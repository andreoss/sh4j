/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for Integration.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class IntegrationTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void echoHello() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("echo.sh", String.format("echo hello%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void trueCommand() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(this.writeScript("true.sh", String.format("true%n")).getAbsolutePath())
        );
    }

    @Test
    public void falseCommand() throws IOException {
        Assert.assertEquals(
            1,
            Main.runScript(this.writeScript("false.sh", String.format("false%n")).getAbsolutePath())
        );
    }

    @Test
    public void pwdCommand() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(this.writeScript("pwd.sh", String.format("pwd%n")).getAbsolutePath())
        );
    }

    @Test
    public void exitFortyTwo() throws IOException {
        Assert.assertEquals(
            42,
            Main.runScript(
                this.writeScript("exitFortyTwo.sh", String.format("exit 42%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void pipelineEchoCat() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("pipe.sh", String.format("echo hello | cat%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void childJvmCatFileProducesVisibleStdout() throws Exception {
        final File probe = this.tempdir.newFile("probe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("hello-from-cat%n"));
        }
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-cp",
            System.getProperty("java.class.path"),
            "io.github.andreoss.sh4j.Main",
            this.writeScript(
                "catprobe.sh",
                String.format("cat %s%n", probe.getAbsolutePath())
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        final int exitCode = proc.waitFor();
        Assert.assertEquals(
            String.format("sh4j exit code; output: %s", out),
            0,
            exitCode
        );
        Assert.assertTrue(
            String.format("external cat stdout must reach test; got: '%s'", out),
            out.contains("hello-from-cat")
        );
    }

    @Test
    public void nativeBinaryCatProbeSmoke() throws Exception {
        final File nativebin = new File("target/sh4j");
        org.junit.Assume.assumeTrue(
            "target/sh4j native binary not built; skipping native smoke",
            nativebin.isFile() && nativebin.canExecute()
        );
        final File probe = this.tempdir.newFile("nprobe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("hello-native%n"));
        }
        final ProcessBuilder builder = new ProcessBuilder(
            nativebin.getAbsolutePath(),
            this.writeScript(
                "ncatprobe.sh",
                String.format("cat %s%n", probe.getAbsolutePath())
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        final int exitCode = proc.waitFor();
        Assert.assertEquals(
            String.format("native sh4j exit code; output: %s", out),
            0,
            exitCode
        );
        Assert.assertTrue(
            String.format("native sh4j must propagate external cat stdout; got: '%s'", out),
            out.contains("hello-native")
        );
    }

    @Test
    public void childJvmEchoPipeCat() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("h_pipe.sh", String.format("echo child-pipe | cat%n"))
        );
        Assert.assertTrue(
            String.format("pipeline stdout missing; got: '%s'", out),
            out.contains("child-pipe")
        );
    }

    @Test
    public void childJvmCatProbe() throws Exception {
        final File probe = this.tempdir.newFile("h_probe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("child-probe%n"));
        }
        final String out = this.runChildJvm(
            this.writeScript(
                "h_cat.sh",
                String.format("cat %s%n", probe.getAbsolutePath())
            )
        );
        Assert.assertTrue(
            String.format("cat stdout missing; got: '%s'", out),
            out.contains("child-probe")
        );
    }

    @Test
    public void childJvmForLoop() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("h_for.sh", String.format("for i in 1 2 3; do echo $i; done%n"))
        );
        Assert.assertTrue(
            String.format("for-loop body missing; got: '%s'", out),
            out.contains("1") && out.contains("2") && out.contains("3")
        );
    }

    @Test
    public void nativeBinaryPipelineSmoke() throws Exception {
        final File nativebin = new File("target/sh4j");
        org.junit.Assume.assumeTrue(
            "target/sh4j native binary not built; skipping native pipeline smoke",
            nativebin.isFile() && nativebin.canExecute()
        );
        final ProcessBuilder builder = new ProcessBuilder(
            nativebin.getAbsolutePath(),
            this.writeScript(
                "npipe.sh",
                String.format("echo native-pipe | cat%n")
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        final int exitCode = proc.waitFor();
        Assert.assertEquals(
            String.format("native sh4j pipeline exit code; output: %s", out),
            0,
            exitCode
        );
        Assert.assertTrue(
            String.format("native sh4j pipeline must propagate stdout; got: '%s'", out),
            out.contains("native-pipe")
        );
    }

    @Test
    public void pipelineMultiple() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "pipe_multi.sh",
                    String.format("echo hello | cat | wc -c%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    public void andIfBothSucceed() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("and.sh", String.format("true && echo ok%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void andIfFirstFails() throws IOException {
        Assert.assertEquals(
            1,
            Main.runScript(
                this.writeScript(
                    "and_fail.sh",
                    String.format("false && echo not_reached%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    public void orIfFirstFails() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("or.sh", String.format("false || echo ok%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void orIfFirstSucceeds() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "or_succ.sh",
                    String.format("true || echo not_reached%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    public void bangInvertsExit() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("bang.sh", String.format("! false%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void bangInvertsTrueToFalse() throws IOException {
        Assert.assertEquals(
            1,
            Main.runScript(
                this.writeScript("bang_true.sh", String.format("! true%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void sequentialSemicolon() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("semi.sh", String.format("echo a; echo b%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void backgroundAmpersand() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("bg.sh", String.format("sleep 0 &%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void assignmentThenCommand() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "assign.sh",
                    String.format("FOO=hello echo $FOO%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    public void assignmentOnlyPersists() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("assign_only.sh", String.format("FOO=bar%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void multipleAssignments() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("multi_assign.sh", String.format("A=1 B=2%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void outputRedirection() throws IOException {
        final File outfile = this.tempdir.newFile("out.txt");
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "redir_out.sh",
                    String.format("echo test > %s", outfile.getAbsolutePath()) + String.format("%n")
                ).getAbsolutePath()
            )
        );
        Assert.assertTrue(outfile.exists());
        Assert.assertEquals(
            "test",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void inputRedirection() throws IOException {
        final File infile = this.tempdir.newFile("in.txt");
        Files.writeString(infile.toPath(), String.format("input data%n"));
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "redir_in.sh",
                    String.format("cat < %s", infile.getAbsolutePath()) + String.format("%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    public void appendRedirection() throws IOException {
        final File outfile = this.tempdir.newFile("append.txt");
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "append.sh",
                    String.format(
                        "echo line2 >> %s",
                        outfile.getAbsolutePath()
                    ) + String.format("%n")
                ).getAbsolutePath()
            )
        );
        Assert.assertTrue("append.txt should exist", outfile.exists());
        Assert.assertEquals(
            "line2",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void clobberRedirection() throws IOException {
        final File outfile = this.tempdir.newFile("clobber.txt");
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "clobber.sh",
                    String.format(
                        "echo test >| %s",
                        outfile.getAbsolutePath()
                    ) + String.format("%n")
                ).getAbsolutePath()
            )
        );
        Assert.assertTrue(outfile.exists());
    }

    @Test
    public void stderrRedirect() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "stderr_redir.sh",
                    String.format(
                        "echo test 2>%s%n",
                        this.tempdir.newFile("stderr.txt").getAbsolutePath()
                    )
                ).getAbsolutePath()
            )
        );
    }

    @Test
    public void variableExpansionWithRedirect() throws IOException {
        final File outfile = this.tempdir.newFile("out2.txt");
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "varexp_redir.sh",
                    String.format(
                        "echo $HOME > %s",
                        outfile.getAbsolutePath()
                    ) + String.format("%n")
                ).getAbsolutePath()
            )
        );
        Assert.assertTrue("out2.txt should exist", outfile.exists());
        Assert.assertFalse(
            "content should not be empty",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim().isEmpty()
        );
    }

    @Test
    public void complexScript() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("complex.sh", String.format("MSG=hello%necho $MSG%n"))
                    .getAbsolutePath()
            )
        );
    }

    @Test
    public void variableExpansion() throws IOException {
        Assert.assertEquals(
            0,
            Main.runScript(
                this.writeScript("varexp.sh", String.format("echo $SHELL%n")).getAbsolutePath()
            )
        );
    }

    @Test
    public void commandNotFound() throws IOException {
        Assert.assertEquals(
            127,
            Main.runScript(
                this.writeScript(
                    "notfound.sh",
                    String.format("nonexistent_cmd_xyz%n")
                ).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.2.2 Single-Quotes - all characters between single-quotes are
     * preserved literally; no expansion occurs.
     */
    @Test
    public void singleQuotesPreserveLiterally() throws Exception {
        final File script = this.writeScript(
            "sq.sh", String.format("X=val%necho '$X'%n")
        );
        Assert.assertEquals("$X\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.2.3 Double-Quotes - variables expand, single quotes are literal.
     */
    @Test
    public void doubleQuotesAllowVariableExpansion() throws Exception {
        final File script = this.writeScript(
            "dq.sh", String.format("X=val%necho \"X=$X\"%n")
        );
        Assert.assertEquals("X=val\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:-word} default if unset.
     */
    @Test
    public void parameterDefaultExpansion() throws Exception {
        final File script = this.writeScript(
            "pe.sh", String.format("echo ${UNSET:-default}%n")
        );
        Assert.assertEquals("default\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.3 Command Substitution - $(...) replaced by its stdout.
     */
    @Test
    public void commandSubstitutionDollarParen() throws Exception {
        final File script = this.writeScript(
            "cs.sh", String.format("echo $(echo nested)%n")
        );
        Assert.assertEquals("nested\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - $((expression)).
     */
    @Test
    public void arithmeticExpansion() throws Exception {
        final File script = this.writeScript(
            "ae.sh", String.format("echo $((2 * (3 + 4)))%n")
        );
        Assert.assertEquals("14\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.4 Here-Document - delimited input becomes stdin for the
     * command.
     */
    @Test
    public void hereDocument() throws Exception {
        final File script = this.writeScript(
            "hd.sh", String.format("cat <<EOF%nfirst line%nsecond line%nEOF%n")
        );
        Assert.assertEquals("first line\nsecond line\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.1 If Conditional Construct - if/elif/else/fi.
     */
    @Test
    public void ifThenElseConstruct() throws Exception {
        final File script = this.writeScript(
            "ite.sh",
            String.format("if false; then echo no; else echo yes; fi%n")
        );
        Assert.assertEquals("yes\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.4 Case Conditional Construct - case/in/esac.
     */
    @Test
    public void caseConditionalConstruct() throws Exception {
        final File script = this.writeScript(
            "case.sh",
            String.format("x=banana%ncase $x in%n  apple) echo a;;%n  banana) echo b;;%nesac%n")
        );
        Assert.assertEquals("b\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.5 Function Definition Command - functions persist and bind
     * positional parameters.
     */
    @Test
    public void functionDefinitionAndCall() throws Exception {
        final File script = this.writeScript(
            "fn.sh",
            String.format("greet() { echo hi $1; }%ngreet world%n")
        );
        Assert.assertEquals("hi world\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.2 Special Parameters - $# yields positional-parameter count.
     */
    @Test
    public void specialParameterPositionalCount() throws Exception {
        final File script = this.writeScript(
            "dollar-hash.sh",
            String.format("set -- a b c%necho count=$#%n")
        );
        Assert.assertEquals("count=3\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.2.1 Escape Character - backslash quotes the next character so it
     * loses any special meaning.
     */
    @Test
    public void backslashEscapesDollar() throws Exception {
        final File script = this.writeScript(
            "bs.sh", String.format("X=val%necho \\$X%n")
        );
        Assert.assertEquals("$X\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.5 Field Splitting - unquoted expansion is split on IFS.
     */
    @Test
    public void fieldSplittingOnIfs() throws Exception {
        final File script = this.writeScript(
            "fs.sh", String.format("IFS=:%nX=a:b:c%nfor i in $X; do echo $i; done%n")
        );
        Assert.assertEquals("a\nb\nc\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.6 Pathname Expansion - glob characters match filenames.
     */
    @Test
    public void pathnameExpansionGlob() throws Exception {
        final File dir = this.tempdir.newFolder("globdir");
        Assert.assertTrue(new File(dir, "alpha.txt").createNewFile());
        Assert.assertTrue(new File(dir, "beta.txt").createNewFile());
        Assert.assertTrue(new File(dir, "other.log").createNewFile());
        final File script = this.writeScript(
            "glob.sh",
            String.format("cd %s%necho *.txt | tr ' ' '\\n' | sort%n", dir.getAbsolutePath())
        );
        Assert.assertEquals(
            "alpha.txt\nbeta.txt\n", this.runChildJvm(script)
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${#var} yields length.
     */
    @Test
    public void parameterLengthExpansion() throws Exception {
        final File script = this.writeScript(
            "plen.sh", String.format("X=hello%necho ${#X}%n")
        );
        Assert.assertEquals("5\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var%suffix} removes shortest suffix.
     */
    @Test
    public void parameterSuffixRemoval() throws Exception {
        final File script = this.writeScript(
            "psuf.sh",
            String.format("X=foo.tar.gz%necho ${X%%.gz}%n")
        );
        Assert.assertEquals("foo.tar\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.8.2 Exit Status - `!` inverts the exit status of the pipeline.
     */
    @Test
    public void bangInvertsPipelineExit() throws Exception {
        final File script = this.writeScript(
            "bang.sh", String.format("! true%necho $?%n")
        );
        Assert.assertEquals("1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.3 While - body runs while the condition exits zero.
     */
    @Test
    public void whileLoopCounts() throws Exception {
        final File script = this.writeScript(
            "while.sh",
            String.format("i=1%nwhile [ $i -le 3 ]; do echo $i; i=$((i+1)); done%n")
        );
        Assert.assertEquals("1\n2\n3\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.5 Until - body runs while the condition exits non-zero.
     */
    @Test
    public void untilLoopCounts() throws Exception {
        final File script = this.writeScript(
            "until.sh",
            String.format("i=1%nuntil [ $i -gt 3 ]; do echo $i; i=$((i+1)); done%n")
        );
        Assert.assertEquals("1\n2\n3\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.3 Lists - AND-list short-circuits on first failure.
     */
    @Test
    public void andOrListShortCircuit() throws Exception {
        final File script = this.writeScript(
            "andor.sh",
            String.format("false && echo skip || echo ran%n")
        );
        Assert.assertEquals("ran\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.2 For - iterates over the given word list.
     */
    @Test
    public void forIteratesWords() throws Exception {
        final File script = this.writeScript(
            "for.sh",
            String.format("for n in one two three; do echo $n; done%n")
        );
        Assert.assertEquals("one\ntwo\nthree\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.1 Positional Parameters - $1, $2 access individual params.
     */
    @Test
    public void positionalParametersDollarOneAndTwo() throws Exception {
        final File script = this.writeScript(
            "pos.sh", String.format("set -- alpha beta gamma%necho $1-$2%n")
        );
        Assert.assertEquals("alpha-beta\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.1 Tilde Expansion - leading ~ expands to $HOME.
     */
    @Test
    public void tildeExpansionToHome() throws Exception {
        final File script = this.writeScript(
            "tilde.sh", String.format("HOME=/tmp/home%necho ~/x%n")
        );
        Assert.assertEquals("/tmp/home/x\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.7 Quote Removal - escaped quote inside double-quotes is
     * preserved literally.
     */
    @Test
    public void quoteRemovalEscapedDoubleQuote() throws Exception {
        final File script = this.writeScript(
            "qr.sh", String.format("echo \"a\\\"b\"%n")
        );
        Assert.assertEquals("a\"b\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.6 Duplicating Output FD - 2>&1 merges stderr into stdout.
     */
    @Test
    public void duplicateStderrToStdout() throws Exception {
        final File script = this.writeScript(
            "dupfd.sh",
            String.format("echo to-out%necho to-err 1>&2 2>&1%n")
        );
        Assert.assertEquals("to-out\nto-err\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.6 Brace Group - { list; } groups commands without subshell.
     */
    @Test
    public void braceGroupSharesEnvironment() throws Exception {
        final File script = this.writeScript(
            "brace.sh",
            String.format("{ X=inside; }%necho $X%n")
        );
        Assert.assertEquals("inside\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.1 Subshell - ( list ) runs in a subshell; outer env
     * unchanged.
     */
    @Test
    public void subshellEnvironmentIsolated() throws Exception {
        final File script = this.writeScript(
            "sub.sh",
            String.format("X=outer%n(X=inner; echo $X)%necho $X%n")
        );
        Assert.assertEquals("inner\nouter\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.3 PATH - command search finds executables on PATH.
     */
    @Test
    public void pathSearchFindsExternalCommand() throws Exception {
        final File script = this.writeScript(
            "path.sh", String.format("echo $(printf hello)%n")
        );
        Assert.assertEquals("hello\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - variable reference inside $((..)).
     */
    @Test
    public void arithmeticReferencesVariable() throws Exception {
        final File script = this.writeScript(
            "arith2.sh", String.format("a=6; b=7%necho $((a * b))%n")
        );
        Assert.assertEquals("42\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.8.2 Exit Status - $? carries the last command's exit.
     */
    @Test
    public void exitStatusPropagatesToDollarQuestion() throws Exception {
        final File script = this.writeScript(
            "dollarq.sh",
            String.format("(exit 7)%necho $?%n")
        );
        Assert.assertEquals("7\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var#prefix} removes shortest
     * matching prefix.
     */
    @Test
    public void parameterPrefixRemoval() throws Exception {
        final File script = this.writeScript(
            "ppref.sh",
            String.format("X=foo.bar.baz%necho ${X#foo.}%n")
        );
        Assert.assertEquals("bar.baz\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.3.1 Alias Substitution - alias name expands to its body before
     * parsing the rest of the command.
     */
    @Test
    public void aliasSubstitution() throws Exception {
        final File script = this.writeScript(
            "alias.sh",
            String.format("alias greet='echo hi'%ngreet there%n")
        );
        Assert.assertEquals("hi there\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.7 Open File Descriptor for Reading and Writing - {@code <>}
     * opens a file for both read and write.
     */
    @Test
    public void readWriteRedirection() throws Exception {
        final File data = this.tempdir.newFile("rw.dat");
        Files.writeString(data.toPath(), "stored\n");
        final File script = this.writeScript(
            "rw.sh",
            String.format("cat <>%s%n", data.getAbsolutePath())
        );
        Assert.assertEquals("stored\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.8.1 Consequences of Shell Errors - {@code set -e} aborts on
     * first command failure.
     */
    @Test
    public void setEAbortsOnFirstFailure() throws Exception {
        final File script = this.writeScript(
            "sete.sh",
            String.format("set -e%nfalse%necho should-not-print%n")
        );
        final String out = this.runChildJvmAllowingFailure(script);
        Assert.assertFalse(
            "set -e should abort before echo runs: " + out,
            out.contains("should-not-print")
        );
    }

    /**
     * POSIX 2.14 export builtin - marks variable for child environment.
     */
    @Test
    public void exportMakesVariableVisibleToSubshell() throws Exception {
        final File script = this.writeScript(
            "export.sh",
            String.format("X=outer%nexport X%n(echo $X)%n")
        );
        Assert.assertEquals("outer\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 unset builtin - removes a variable.
     */
    @Test
    public void unsetRemovesVariable() throws Exception {
        final File script = this.writeScript(
            "unset.sh",
            String.format("X=hello%nunset X%necho [${X-empty}]%n")
        );
        Assert.assertEquals("[empty]\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 shift builtin - drops positional parameter $1, renumbers
     * the rest.
     */
    @Test
    public void shiftDropsFirstPositional() throws Exception {
        final File script = this.writeScript(
            "shift.sh",
            String.format("set -- a b c%nshift%necho $1-$2%n")
        );
        Assert.assertEquals("b-c\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 readonly builtin - prevents reassignment.
     */
    @Test
    public void readonlyPreventsReassignment() throws Exception {
        final File script = this.writeScript(
            "ro.sh",
            String.format("readonly R=fixed%nR=changed%necho $R%n")
        );
        final String out = this.runChildJvm(script);
        Assert.assertTrue("expected 'fixed' in output, got: " + out, out.contains("fixed"));
        Assert.assertFalse(
            "readonly should block reassignment, got: " + out,
            out.contains("changed")
        );
    }

    /**
     * POSIX 2.14 true / false builtins - exit 0 / 1 respectively.
     */
    @Test
    public void trueAndFalseExitCodes() throws Exception {
        final File script = this.writeScript(
            "tf.sh",
            String.format("true; echo $?%nfalse; echo $?%n")
        );
        Assert.assertEquals("0\n1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 pwd builtin - prints the working directory.
     */
    @Test
    public void pwdPrintsWorkingDirectory() throws Exception {
        final File dir = this.tempdir.newFolder("wd");
        final File script = this.writeScript(
            "pwd.sh",
            String.format("cd %s%npwd%n", dir.getAbsolutePath())
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue(
            "expected pwd to end with /wd, got " + out, out.endsWith("/wd")
        );
    }

    /**
     * POSIX 2.14 colon builtin - {@code :} is a no-op returning 0.
     */
    @Test
    public void colonBuiltinIsNoop() throws Exception {
        final File script = this.writeScript(
            "colon.sh",
            String.format(":%necho $?%n")
        );
        Assert.assertEquals("0\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 cd builtin - changes the working directory.
     */
    @Test
    public void cdChangesWorkingDirectory() throws Exception {
        final File target = this.tempdir.newFolder("cd-target");
        final File script = this.writeScript(
            "cd.sh",
            String.format("cd %s%npwd%n", target.getAbsolutePath())
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue("expected to land in /cd-target, got " + out, out.endsWith("/cd-target"));
    }

    /**
     * POSIX 2.14 eval builtin - parses and runs its arguments as shell input.
     */
    @Test
    public void evalRunsConstructedCommand() throws Exception {
        final File script = this.writeScript(
            "eval.sh",
            String.format("CMD='echo dynamic'%neval $CMD%n")
        );
        Assert.assertEquals("dynamic\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 read builtin - reads one line from stdin into VAR.
     */
    @Test
    public void readBuiltinFromStdinViaHeredoc() throws Exception {
        final File script = this.writeScript(
            "read.sh",
            String.format("read VAR <<EOF%nhello world%nEOF%necho got=$VAR%n")
        );
        Assert.assertEquals("got=hello world\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 getopts builtin - parses option flags.
     */
    @Test
    public void getoptsParsesSingleFlag() throws Exception {
        final File script = this.writeScript(
            "getopts.sh",
            String.format("set -- -a foo%nwhile getopts a: opt; do echo $opt:$OPTARG; done%n")
        );
        Assert.assertEquals("a:foo\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 umask builtin - default mask is 0022.
     */
    @Test
    public void umaskDefaultIsPrintable() throws Exception {
        final File script = this.writeScript(
            "umask.sh", String.format("umask%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue("expected 4-digit octal, got " + out, out.matches("\\d{4}"));
    }

    /**
     * POSIX 2.5.2 Special Parameters - $$ is the shell's PID (positive int).
     */
    @Test
    public void specialParameterDollarDollarIsPid() throws Exception {
        final File script = this.writeScript(
            "pid.sh", String.format("echo $$%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue("expected positive int, got " + out, out.matches("\\d+"));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:?word} writes error to stderr
     * when var is unset.
     */
    @Test
    public void parameterErrorIfUnset() throws Exception {
        final File script = this.writeScript(
            "perr.sh",
            String.format("echo ${UNSET_REQ:?required}%n")
        );
        final String out = this.runChildJvmAllowingFailure(script);
        Assert.assertTrue(
            "expected error mentioning UNSET_REQ/required, got: " + out,
            out.contains("UNSET_REQ") || out.contains("required")
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:+word} substitutes word when
     * var is set non-empty.
     */
    @Test
    public void parameterAlternativeIfSet() throws Exception {
        final File script = this.writeScript(
            "palt.sh",
            String.format("X=v%necho ${X:+present}%necho ${UNSET:+present}%n")
        );
        Assert.assertEquals("present\n\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.2 Pipelines - data flows through every stage of a 3-stage
     * pipeline.
     */
    @Test
    public void multiStagePipeline() throws Exception {
        final File script = this.writeScript(
            "pipe.sh",
            String.format("echo hello | tr 'a-z' 'A-Z' | tr -d 'L'%n")
        );
        Assert.assertEquals("HEO\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.1 Redirecting Input - input redirect supplies the content of
     * a file to the command's stdin.
     */
    @Test
    public void inputRedirectionFromFile() throws Exception {
        final File data = this.tempdir.newFile("in.dat");
        Files.writeString(data.toPath(), "feed\n");
        final File script = this.writeScript(
            "redir.sh",
            String.format("cat <%s%n", data.getAbsolutePath())
        );
        Assert.assertEquals("feed\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 cd builtin - {@code cd -} jumps back to the previous
     * directory recorded in OLDPWD.
     */
    @Test
    public void cdDashRevisitsPreviousDirectory() throws Exception {
        final File first = this.tempdir.newFolder("first");
        final File second = this.tempdir.newFolder("second");
        final File script = this.writeScript(
            "cdback.sh",
            String.format(
                "cd %s%ncd %s%ncd - >/dev/null%npwd%n",
                first.getAbsolutePath(), second.getAbsolutePath()
            )
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue("expected to return to /first, got " + out, out.endsWith("/first"));
    }

    /**
     * POSIX 2.14 type builtin - reports how the shell would interpret a
     * name (builtin / function / file / not found).
     */
    @Test
    public void typeBuiltinReportsBuiltin() throws Exception {
        final File script = this.writeScript(
            "type.sh", String.format("type echo%n")
        );
        final String out = this.runChildJvm(script);
        Assert.assertTrue(
            "expected output to mention echo as a builtin, got: " + out,
            out.contains("echo")
        );
    }

    /**
     * POSIX 2.14 command -v - prints the command location / kind.
     */
    @Test
    public void commandDashVReportsBuiltin() throws Exception {
        final File script = this.writeScript(
            "cmdv.sh", String.format("command -v cd%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertEquals("cd", out);
    }

    /**
     * POSIX 2.14 hash builtin - {@code hash -r} clears the cache and
     * returns 0.
     */
    @Test
    public void hashDashRReturnsZero() throws Exception {
        final File script = this.writeScript(
            "hashr.sh", String.format("hash -r%necho $?%n")
        );
        Assert.assertEquals("0\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.2 Quoting - mixing single and double quotes in one word
     * preserves each region's semantics.
     */
    @Test
    public void mixedQuotingPreservesEachRegion() throws Exception {
        final File script = this.writeScript(
            "mq.sh",
            String.format("X=val%necho '$X'\"$X\"%n")
        );
        Assert.assertEquals("$Xval\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.5 Function Definition - recursive function (factorial of 5).
     */
    @Test
    public void recursiveFunctionFactorial() throws Exception {
        final File script = this.writeScript(
            "fact.sh",
            String.format(
                "fact() {%n  if [ $1 -le 1 ]; then echo 1; "
                    + "else echo $(( $1 * $(fact $(( $1 - 1 ))) )); fi%n"
                    + "}%nfact 5%n"
            )
        );
        Assert.assertEquals("120\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 wait builtin - {@code wait} without args returns 0 when no
     * background jobs are running.
     */
    @Test
    public void waitWithoutBackgroundJobsReturnsZero() throws Exception {
        final File script = this.writeScript(
            "wait.sh", String.format("wait%necho $?%n")
        );
        Assert.assertEquals("0\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.4 Here-Document with quoted delimiter - body is NOT
     * variable-expanded.
     */
    @Test
    public void heredocQuotedDelimiterPreventsExpansion() throws Exception {
        final File script = this.writeScript(
            "hdq.sh",
            String.format(
                "X=val%ncat <<'EOF'%n$X literal%nEOF%n"
            )
        );
        Assert.assertEquals("$X literal\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.2 Special Parameters - $0 expands to the script name passed
     * to the shell.
     */
    @Test
    public void specialParameterDollarZeroIsScriptName() throws Exception {
        final File script = this.writeScript(
            "dollar0.sh", String.format("echo $0%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue(
            "expected $0 to end with /dollar0.sh, got: " + out,
            out.endsWith("/dollar0.sh")
        );
    }

    /**
     * POSIX 2.6.1 Tilde Expansion - tilde NOT at the start of a word must
     * NOT expand.
     */
    @Test
    public void tildeMidWordNotExpanded() throws Exception {
        final File script = this.writeScript(
            "midtilde.sh",
            String.format("HOME=/h%necho a~b%n")
        );
        Assert.assertEquals("a~b\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - unary minus produces negative
     * integer.
     */
    @Test
    public void arithmeticNegativeNumbers() throws Exception {
        final File script = this.writeScript(
            "arithneg.sh", String.format("echo $((-3 - 4))%n")
        );
        Assert.assertEquals("-7\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - octal literal (leading 0).
     */
    @Test
    public void arithmeticOctalLiteral() throws Exception {
        final File script = this.writeScript(
            "octal.sh", String.format("echo $((010 + 1))%n")
        );
        Assert.assertEquals("9\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.3 nested while loops - inner break breaks only the inner
     * loop.
     */
    @Test
    public void nestedWhileInnerBreak() throws Exception {
        final File script = this.writeScript(
            "nested.sh",
            String.format(
                "i=0%nwhile [ $i -lt 2 ]; do "
                    + "j=0; while [ $j -lt 3 ]; do echo $i.$j; "
                    + "if [ $j -eq 1 ]; then break; fi; j=$((j+1)); done; "
                    + "i=$((i+1)); done%n"
            )
        );
        Assert.assertEquals("0.0\n0.1\n1.0\n1.1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.5 Function Definition - a function can shadow a builtin
     * within its own scope.
     */
    @Test
    public void functionShadowsBuiltin() throws Exception {
        final File script = this.writeScript(
            "shadow.sh",
            String.format("echo() { /usr/bin/env echo wrapped: $*; }%necho hi%n")
        );
        Assert.assertEquals("wrapped: hi\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 unset -f - removes a function definition.
     */
    @Test
    public void unsetDashFRemovesFunction() throws Exception {
        final File script = this.writeScript(
            "unsetf.sh",
            String.format(
                "f() { echo from-fn; }%nf%nunset -f f%nf 2>/dev/null || echo gone%n"
            )
        );
        Assert.assertEquals("from-fn\ngone\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.3 Token Recognition - {@code #} starts a comment that runs
     * to end-of-line; the next command still executes.
     */
    @Test
    public void hashStartsComment() throws Exception {
        final File script = this.writeScript(
            "comment.sh",
            String.format("# this is a comment%necho after%n")
        );
        Assert.assertEquals("after\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.2.1 Escape Character - backslash-newline outside quotes is
     * a line continuation, joining the two halves into one word.
     */
    @Test
    public void backslashNewlineIsLineContinuation() throws Exception {
        final File script = this.writeScript(
            "cont.sh",
            String.format("echo one\\%ntwo%n")
        );
        Assert.assertEquals("onetwo\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.3 IFS - reset to default (space-tab-newline) when unset
     * during field splitting.
     */
    @Test
    public void ifsUnsetUsesDefault() throws Exception {
        final File script = this.writeScript(
            "ifs.sh",
            String.format(
                "unset IFS%nX='a b c'%nfor i in $X; do echo $i; done%n"
            )
        );
        Assert.assertEquals("a\nb\nc\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.4 Case Patterns - `|` separates alternative patterns.
     */
    @Test
    public void caseAlternativePatterns() throws Exception {
        final File script = this.writeScript(
            "casealt.sh",
            String.format(
                "for w in a b c d; do case $w in a|b) echo letter;; *) echo other;; esac; done%n"
            )
        );
        Assert.assertEquals("letter\nletter\nother\nother\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.4 Case Patterns - `?` matches any single character.
     */
    @Test
    public void caseQuestionMarkMatchesSingleChar() throws Exception {
        final File script = this.writeScript(
            "caseq.sh",
            String.format(
                "for w in ab abc a; do case $w in ??) echo two;; *) echo not-two;; esac; done%n"
            )
        );
        Assert.assertEquals("two\nnot-two\nnot-two\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var##pattern}` removes longest
     * matching prefix.
     */
    @Test
    public void parameterLongestPrefixRemoval() throws Exception {
        final File script = this.writeScript(
            "lpref.sh",
            String.format("X=/path/to/file.txt%necho ${X##*/}%n")
        );
        Assert.assertEquals("file.txt\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var%%pattern}` removes longest
     * matching suffix.
     */
    @Test
    public void parameterLongestSuffixRemoval() throws Exception {
        final File script = this.writeScript(
            "lsuf.sh",
            String.format("X=foo.tar.gz%necho ${X%%%%.*}%n")
        );
        Assert.assertEquals("foo\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - bit-shift operators.
     */
    @Test
    public void arithmeticBitShift() throws Exception {
        final File script = this.writeScript(
            "shift.sh",
            String.format("echo $((1 << 5)) $((1024 >> 3))%n")
        );
        Assert.assertEquals("32 128\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - comparison operators yield 0/1.
     */
    @Test
    public void arithmeticComparisonResult() throws Exception {
        final File script = this.writeScript(
            "cmp.sh",
            String.format("echo $((5 > 3)) $((3 > 5)) $((5 == 5))%n")
        );
        Assert.assertEquals("1 0 1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.5 Function Definition - {@code return N} sets the function's
     * exit status.
     */
    @Test
    public void functionReturnValue() throws Exception {
        final File script = this.writeScript(
            "ret.sh",
            String.format("f() { return 42; }%nf%necho $?%n")
        );
        Assert.assertEquals("42\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 break N - breaks out of N levels of loops.
     */
    @Test
    public void breakNExitsMultipleLoops() throws Exception {
        final File script = this.writeScript(
            "brk2.sh",
            String.format(
                "for i in 1 2; do for j in a b c; do "
                    + "if [ $j = b ]; then break 2; fi; echo $i.$j; done; done%necho after%n"
            )
        );
        Assert.assertEquals("1.a\nafter\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.5 Duplicating an Input FD - {@code n<&-} closes the named fd.
     */
    @Test
    public void closeInputFileDescriptor() throws Exception {
        final File script = this.writeScript(
            "close.sh",
            String.format("cat <<EOF%nin%nEOF%necho post%n")
        );
        Assert.assertEquals("in\npost\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.2 Special Parameters - $- contains current shell flags.
     */
    @Test
    public void specialParameterDashIsShellFlags() throws Exception {
        final File script = this.writeScript(
            "dash.sh",
            String.format("set -e%necho $-%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue("expected $- to contain e, got: " + out, out.contains("e"));
    }

    /**
     * POSIX 2.5.3 LINENO - shell-variable holding the current line number
     * of the executing script.
     */
    @Test
    public void linenoTracksScriptLineNumber() throws Exception {
        final File script = this.writeScript(
            "ln.sh",
            String.format("echo first%necho second-$LINENO%n")
        );
        final String out = this.runChildJvm(script);
        Assert.assertTrue(
            "expected LINENO to fire on line 2, got: " + out,
            out.contains("second-2")
        );
    }

    /**
     * POSIX 2.5.3 OLDPWD - previous working directory after cd.
     */
    @Test
    public void oldpwdAfterCd() throws Exception {
        final File target = this.tempdir.newFolder("after");
        final File script = this.writeScript(
            "oldpwd.sh",
            String.format(
                "ORIG=$PWD%ncd %s%necho $OLDPWD%n", target.getAbsolutePath()
            )
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertFalse("OLDPWD should not be empty", out.isEmpty());
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - ternary operator.
     */
    @Test
    public void arithmeticTernaryOperator() throws Exception {
        final File script = this.writeScript(
            "tern.sh",
            String.format("echo $((1 > 0 ? 42 : 13)) $((1 > 5 ? 42 : 13))%n")
        );
        Assert.assertEquals("42 13\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - logical AND / OR short-circuit.
     */
    @Test
    public void arithmeticLogicalOps() throws Exception {
        final File script = this.writeScript(
            "logop.sh",
            String.format("echo $((1 && 0)) $((0 || 2)) $((1 && 3))%n")
        );
        Assert.assertEquals("0 1 1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 readonly -p - lists all readonly variables.
     */
    @Test
    public void readonlyDashPListsBindings() throws Exception {
        final File script = this.writeScript(
            "rop.sh",
            String.format("readonly X=hello%nreadonly -p%n")
        );
        final String out = this.runChildJvm(script);
        Assert.assertTrue("expected to list X, got: " + out, out.contains("X"));
    }

    /**
     * POSIX 2.14 type - resolves multiple names in one invocation.
     */
    @Test
    public void typeWithMultipleNames() throws Exception {
        final File script = this.writeScript(
            "tmulti.sh",
            String.format("type echo cd%n")
        );
        final String out = this.runChildJvm(script);
        Assert.assertTrue("expected echo in output, got: " + out, out.contains("echo"));
        Assert.assertTrue("expected cd in output, got: " + out, out.contains("cd"));
    }

    /**
     * POSIX 2.5.2 Special Parameters - $! is the PID of the most recently
     * started background command.
     */
    @Test
    public void specialParameterBangIsLastBgPid() throws Exception {
        final File script = this.writeScript(
            "bgpid.sh",
            String.format("true &%necho ${!}%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue(
            "expected positive int for $!, got: " + out, out.matches("\\d+")
        );
    }

    /**
     * POSIX 2.14 set -- with no further args clears positional parameters.
     */
    @Test
    public void setDashDashClearsPositionalParams() throws Exception {
        final File script = this.writeScript(
            "setclr.sh",
            String.format("set -- a b c%nset --%necho ${#}%n")
        );
        Assert.assertEquals("0\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.4 here-document with leading-tab strip via {@code <<-}.
     */
    @Test
    public void heredocDashStripsLeadingTabs() throws Exception {
        final File script = this.writeScript(
            "hdtab.sh",
            String.format("cat <<-EOF%n\tindented%n\tagain%n\tEOF%n")
        );
        Assert.assertEquals("indented\nagain\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.2 Special Parameters - $$ stays stable across the script
     * (every read returns the same pid).
     */
    @Test
    public void specialParameterDollarDollarStable() throws Exception {
        final File script = this.writeScript(
            "ddstable.sh",
            String.format("A=$$%nB=$$%n[ \"$A\" = \"$B\" ] && echo same%n")
        );
        Assert.assertEquals("same\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - parentheses change grouping.
     */
    @Test
    public void arithmeticParensGroup() throws Exception {
        final File script = this.writeScript(
            "arithparen.sh",
            String.format("echo $(( (2 + 3) * 4 ))%n")
        );
        Assert.assertEquals("20\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - nested ${X-${Y-default}}: outer
     * default falls back to inner default when both vars are unset.
     */
    @Test
    public void parameterExpansionNested() throws Exception {
        final File script = this.writeScript(
            "nested.sh",
            String.format("echo ${UNSET_X-${UNSET_Y-fallback}}%n")
        );
        Assert.assertEquals("fallback\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7 Redirection - multiple redirections on one command apply
     * in order (later overrides earlier on same fd).
     */
    @Test
    public void multipleRedirectionsOnOneCommand() throws Exception {
        final File data = this.tempdir.newFile("multi.dat");
        Files.writeString(data.toPath(), "line\n");
        final File script = this.writeScript(
            "multi.sh",
            String.format("cat <%s 2>&1%n", data.getAbsolutePath())
        );
        Assert.assertEquals("line\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.1 Simple Commands - a command with assignments and no
     * command name persists the assignments in the shell environment.
     */
    @Test
    public void assignmentOnlyCommandPersists() throws Exception {
        final File script = this.writeScript(
            "assignonly.sh",
            String.format("FOO=set%necho [$FOO]%n")
        );
        Assert.assertEquals("[set]\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.2 Pipelines - leading `!` inverts the pipeline exit status.
     */
    @Test
    public void bangBeforePipelineInverts() throws Exception {
        final File script = this.writeScript(
            "bangpipe.sh",
            String.format("! false; echo $?%n! true; echo $?%n")
        );
        Assert.assertEquals("0\n1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.4 Case - {@code *)} matches anything as the default arm.
     */
    @Test
    public void caseStarDefaultMatches() throws Exception {
        final File script = this.writeScript(
            "casestar.sh",
            String.format(
                "for w in apple banana cherry; do "
                    + "case $w in apple) echo a;; *) echo d;; esac; done%n"
            )
        );
        Assert.assertEquals("a\nd\nd\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.3 Command Substitution - backtick form is equivalent to $(...).
     */
    @Test
    public void backtickCommandSubstitution() throws Exception {
        final File script = this.writeScript(
            "btick.sh",
            String.format("X=`echo hello`%necho got=$X%n")
        );
        Assert.assertEquals("got=hello\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.4 nested case - case inside a case body works.
     */
    @Test
    public void nestedCaseStatement() throws Exception {
        final File script = this.writeScript(
            "ncase.sh",
            String.format(
                "X=a; Y=2%ncase $X in a) case $Y in 2) echo a2;; "
                    + "*) echo a-other;; esac;; esac%n"
            )
        );
        Assert.assertEquals("a2\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - bitwise AND / OR / XOR.
     */
    @Test
    public void arithmeticBitwiseOps() throws Exception {
        final File script = this.writeScript(
            "bitops.sh",
            String.format("echo $((6 & 3)) $((6 | 1)) $((6 ^ 3))%n")
        );
        Assert.assertEquals("2 7 5\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.14 exec with no command modifies the shell's own redirections
     * (here: redirecting stderr to stdout, so the next echo goes to stdout).
     */
    @Test
    public void execWithNoCommandRedirectsShell() throws Exception {
        final File script = this.writeScript(
            "execredir.sh",
            String.format("exec 2>&1%necho to-stderr 1>&2%n")
        );
        Assert.assertEquals("to-stderr\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - logical NOT (!) inverts truthiness.
     */
    @Test
    public void arithmeticLogicalNot() throws Exception {
        final File script = this.writeScript(
            "lnot.sh",
            String.format("echo $((!0)) $((!5)) $((!!7))%n")
        );
        Assert.assertEquals("1 0 1\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - unary plus and double-negate.
     */
    @Test
    public void arithmeticUnaryPlus() throws Exception {
        final File script = this.writeScript(
            "uplus.sh",
            String.format("echo $((+5)) $((- -7))%n")
        );
        Assert.assertEquals("5 7\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var:-}` with empty default keeps
     * the empty.
     */
    @Test
    public void parameterDefaultEmptyWord() throws Exception {
        final File script = this.writeScript(
            "ed.sh",
            String.format("echo [${UNSET:-}]%n")
        );
        Assert.assertEquals("[]\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.1 Lists - multiple commands separated by `;` all execute.
     */
    @Test
    public void multipleCommandsOnOneLine() throws Exception {
        final File script = this.writeScript(
            "multi-cmd.sh",
            String.format("echo a; echo b; echo c%n")
        );
        Assert.assertEquals("a\nb\nc\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - mixed-sign arithmetic.
     */
    @Test
    public void arithmeticMixedSign() throws Exception {
        final File script = this.writeScript(
            "mix.sh",
            String.format("echo $((-3 + 8)) $((5 - 10))%n")
        );
        Assert.assertEquals("5 -5\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.2.1 Escape Character - `\\n` outside quotes preserves the
     * literal 'n' character.
     */
    @Test
    public void backslashPreservesNextChar() throws Exception {
        final File script = this.writeScript(
            "esc.sh",
            String.format("echo a\\nb%n")
        );
        Assert.assertEquals("anb\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.3 Command Substitution - empty substitution yields empty
     * string.
     */
    @Test
    public void emptyCommandSubstitution() throws Exception {
        final File script = this.writeScript(
            "ecs.sh",
            String.format("echo a$()b%n")
        );
        Assert.assertEquals("ab\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7 Redirection - higher-numbered fd redirected to file.
     */
    @Test
    public void higherNumberedFdRedirection() throws Exception {
        final File data = this.tempdir.newFile("fd3.dat");
        Files.writeString(data.toPath(), "from-fd3\n");
        final File script = this.writeScript(
            "fd3.sh",
            String.format("cat 0<%s%n", data.getAbsolutePath())
        );
        Assert.assertEquals("from-fd3\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.2 For - empty word list means zero iterations.
     */
    @Test
    public void forEmptyWordListSkipsBody() throws Exception {
        final File script = this.writeScript(
            "empfor.sh",
            String.format("for i in; do echo $i; done%necho after%n")
        );
        Assert.assertEquals("after\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.2 For - loop variable retains last iterated value.
     */
    @Test
    public void forLoopVarPersistsAfter() throws Exception {
        final File script = this.writeScript(
            "forpersist.sh",
            String.format("for i in a b c; do :; done%necho $i%n")
        );
        Assert.assertEquals("c\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.7.2 Redirecting Output - `>|` overrides noclobber.
     */
    @Test
    public void clobberRedirectWritesFile() throws Exception {
        final File target = this.tempdir.newFile("clob.dat");
        Files.writeString(target.toPath(), "old\n");
        final File script = this.writeScript(
            "clob.sh",
            String.format("echo new >|%s%ncat %s%n",
                target.getAbsolutePath(), target.getAbsolutePath()
            )
        );
        Assert.assertEquals("new\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.4 Arithmetic - hex literal with `0x` prefix.
     */
    @Test
    public void arithmeticHexLiteral() throws Exception {
        final File script = this.writeScript(
            "hex.sh",
            String.format("echo $((0x10 + 0xF))%n")
        );
        Assert.assertEquals("31\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.3.1 alias chains - aliases expand transitively.
     */
    @Test
    public void aliasChain() throws Exception {
        final File script = this.writeScript(
            "alchain.sh",
            String.format("alias a=b%nalias b='echo final'%na arg%n")
        );
        Assert.assertEquals("final arg\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.5.2 special params - $0 inside a function is the script name
     * (not the function name) per POSIX.
     */
    @Test
    public void dollarZeroInsideFunction() throws Exception {
        final File script = this.writeScript(
            "fn0.sh",
            String.format("f() { echo $0; }%nf%n")
        );
        final String out = this.runChildJvm(script).trim();
        Assert.assertTrue(
            "expected $0 to end with /fn0.sh, got: " + out, out.endsWith("/fn0.sh")
        );
    }

    /**
     * POSIX 2.7.2 Redirecting Output - the last `>` to the same file wins.
     */
    @Test
    public void multipleOutputRedirectsLastWins() throws Exception {
        final File a = this.tempdir.newFile("a.dat");
        final File b = this.tempdir.newFile("b.dat");
        final File script = this.writeScript(
            "mout.sh",
            String.format("echo hi >%s >%s%ncat %s%n",
                a.getAbsolutePath(), b.getAbsolutePath(), b.getAbsolutePath()
            )
        );
        Assert.assertEquals("hi\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.6.7 Quote Removal - mixed nested quotes preserve each region.
     */
    @Test
    public void quoteRemovalMixedNested() throws Exception {
        final File script = this.writeScript(
            "qmix.sh",
            String.format("echo 'a'\"b\"'c'%n")
        );
        Assert.assertEquals("abc\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.8.2 Exit Status - {@code true; echo $?} reports 0.
     */
    @Test
    public void exitStatusTrueIsZero() throws Exception {
        final File script = this.writeScript(
            "trueq.sh",
            String.format("true%necho $?%n")
        );
        Assert.assertEquals("0\n", this.runChildJvm(script));
    }

    /**
     * POSIX 2.9.4.3 While - explicit while-false loop runs zero iterations.
     */
    @Test
    public void whileFalseZeroIterations() throws Exception {
        final File script = this.writeScript(
            "wfalse.sh",
            String.format("while false; do echo never; done%necho done%n")
        );
        Assert.assertEquals("done\n", this.runChildJvm(script));
    }

    private File writeScript(final String name, final String content) throws IOException {
        final File script = this.tempdir.newFile(name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assert.assertTrue(script.setExecutable(true));
        return script;
    }

    private String runChildJvmAllowingFailure(final File script) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-cp",
            System.getProperty("java.class.path"),
            "io.github.andreoss.sh4j.Main",
            script.getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        proc.waitFor();
        return out;
    }

    private String runChildJvm(final File script) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-cp",
            System.getProperty("java.class.path"),
            "io.github.andreoss.sh4j.Main",
            script.getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        final int exitCode = proc.waitFor();
        Assert.assertEquals(
            String.format("child sh4j exit code; output: %s", out),
            0,
            exitCode
        );
        return out;
    }

    private static byte[] readAllBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
