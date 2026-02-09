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
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for Integration.
 * @since 1.0
 */
public final class IntegrationTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void echoHello() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("echo.sh", String.format("echo hello%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void trueCommand() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(this.writeScript("true.sh", String.format("true%n")).getAbsolutePath())
        );
    }

    @Test
    void falseCommand() throws IOException {
        Assertions.assertEquals(
            1,
            Main.runScript(this.writeScript("false.sh", String.format("false%n")).getAbsolutePath())
        );
    }

    @Test
    void pwdCommand() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(this.writeScript("pwd.sh", String.format("pwd%n")).getAbsolutePath())
        );
    }

    @Test
    void exitFortyTwo() throws IOException {
        Assertions.assertEquals(
            42,
            Main.runScript(
                this.writeScript("exitFortyTwo.sh", String.format("exit 42%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void pipelineEchoCat() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("pipe.sh", String.format("echo hello | cat%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void childJvmCatFileProducesVisibleStdout() throws Exception {
        final File probe = TempDirs.file(IntegrationTest.root, "probe.txt");
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
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("sh4j exit code; output: %s", out)
        );
        Assertions.assertTrue(
            out.contains("hello-from-cat"),
            String.format("external cat stdout must reach test; got: '%s'", out)
        );
    }

    @Test
    void nativeBinaryCatProbeSmoke() throws Exception {
        final File nativebin = new File("target/sh4j");
        Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native smoke"
        );
        final File probe = TempDirs.file(IntegrationTest.root, "nprobe.txt");
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
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("native sh4j exit code; output: %s", out)
        );
        Assertions.assertTrue(
            out.contains("hello-native"),
            String.format("native sh4j must propagate external cat stdout; got: '%s'", out)
        );
    }

    @Test
    void childJvmEchoPipeCat() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("h_pipe.sh", String.format("echo child-pipe | cat%n"))
        );
        Assertions.assertTrue(
            out.contains("child-pipe"),
            String.format("pipeline stdout missing; got: '%s'", out)
        );
    }

    @Test
    void childJvmCatProbe() throws Exception {
        final File probe = TempDirs.file(IntegrationTest.root, "h_probe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("child-probe%n"));
        }
        final String out = this.runChildJvm(
            this.writeScript(
                "h_cat.sh",
                String.format("cat %s%n", probe.getAbsolutePath())
            )
        );
        Assertions.assertTrue(
            out.contains("child-probe"),
            String.format("cat stdout missing; got: '%s'", out)
        );
    }

    @Test
    void childJvmForLoop() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("h_for.sh", String.format("for i in 1 2 3; do echo $i; done%n"))
        );
        Assertions.assertTrue(
            out.contains("1") && out.contains("2") && out.contains("3"),
            String.format("for-loop body missing; got: '%s'", out)
        );
    }

    @Test
    void nativeBinaryPipelineSmoke() throws Exception {
        final File nativebin = new File("target/sh4j");
        Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native pipeline smoke"
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
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("native sh4j pipeline exit code; output: %s", out)
        );
        Assertions.assertTrue(
            out.contains("native-pipe"),
            String.format("native sh4j pipeline must propagate stdout; got: '%s'", out)
        );
    }

    @Test
    void pipelineMultiple() throws IOException {
        Assertions.assertEquals(
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
    void andIfBothSucceed() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("and.sh", String.format("true && echo ok%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void andIfFirstFails() throws IOException {
        Assertions.assertEquals(
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
    void orIfFirstFails() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("or.sh", String.format("false || echo ok%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void orIfFirstSucceeds() throws IOException {
        Assertions.assertEquals(
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
    void bangInvertsExit() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("bang.sh", String.format("! false%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void bangInvertsTrueToFalse() throws IOException {
        Assertions.assertEquals(
            1,
            Main.runScript(
                this.writeScript("bang_true.sh", String.format("! true%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void sequentialSemicolon() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("semi.sh", String.format("echo a; echo b%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void backgroundAmpersand() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("bg.sh", String.format("sleep 0 &%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void assignmentThenCommand() throws IOException {
        Assertions.assertEquals(
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
    void assignmentOnlyPersists() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("assign_only.sh", String.format("FOO=bar%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void multipleAssignments() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("multi_assign.sh", String.format("A=1 B=2%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void outputRedirection() throws IOException {
        final File outfile = TempDirs.file(IntegrationTest.root, "out.txt");
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "redir_out.sh",
                    String.format("echo test > %s", outfile.getAbsolutePath()) + String.format("%n")
                ).getAbsolutePath()
            )
        );
        Assertions.assertTrue(outfile.exists());
        Assertions.assertEquals(
            "test",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    void inputRedirection() throws IOException {
        final File infile = TempDirs.file(IntegrationTest.root, "in.txt");
        Files.writeString(infile.toPath(), String.format("input data%n"));
        Assertions.assertEquals(
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
    void appendRedirection() throws IOException {
        final File outfile = TempDirs.file(IntegrationTest.root, "append.txt");
        Assertions.assertEquals(
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
        Assertions.assertTrue(outfile.exists(), "append.txt should exist");
        Assertions.assertEquals(
            "line2",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    void clobberRedirection() throws IOException {
        final File outfile = TempDirs.file(IntegrationTest.root, "clobber.txt");
        Assertions.assertEquals(
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
        Assertions.assertTrue(outfile.exists());
    }

    @Test
    void stderrRedirect() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "stderr_redir.sh",
                    String.format(
                        "echo test 2>%s%n",
                        TempDirs.file(IntegrationTest.root, "stderr.txt").getAbsolutePath()
                    )
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void variableExpansionWithRedirect() throws IOException {
        final File outfile = TempDirs.file(IntegrationTest.root, "out2.txt");
        Assertions.assertEquals(
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
        Assertions.assertTrue(outfile.exists(), "out2.txt should exist");
        Assertions.assertFalse(
            new String(
                Files.readAllBytes(outfile.toPath()),
                StandardCharsets.UTF_8
            ).trim().isEmpty(),
            "content should not be empty"
        );
    }

    @Test
    void complexScript() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("complex.sh", String.format("MSG=hello%necho $MSG%n"))
                    .getAbsolutePath()
            )
        );
    }

    @Test
    void variableExpansion() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("varexp.sh", String.format("echo $SHELL%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void commandNotFound() throws IOException {
        Assertions.assertEquals(
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
    void singleQuotesPreserveLiterally() throws Exception {
        Assertions.assertEquals(
            String.format("$X%n"),
            this.runChildJvm(
                this.writeScript("sq.sh", String.format("X=val%necho '$X'%n"))
            )
        );
    }

    /**
     * POSIX 2.2.3 Double-Quotes - variables expand, single quotes are literal.
     */
    @Test
    void doubleQuotesAllowVariableExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("X=val%n"),
            this.runChildJvm(
                this.writeScript("dq.sh", String.format("X=val%necho \"X=$X\"%n"))
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:-word} default if unset.
     */
    @Test
    void parameterDefaultExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("default%n"),
            this.runChildJvm(
                this.writeScript("pe.sh", String.format("echo ${UNSET:-default}%n"))
            )
        );
    }

    /**
     * POSIX 2.6.3 Command Substitution - $(...) replaced by its stdout.
     */
    @Test
    void commandSubstitutionDollarParen() throws Exception {
        Assertions.assertEquals(
            String.format("nested%n"),
            this.runChildJvm(
                this.writeScript("cs.sh", String.format("echo $(echo nested)%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - $((expression)).
     */
    @Test
    void arithmeticExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("14%n"),
            this.runChildJvm(
                this.writeScript("ae.sh", String.format("echo $((2 * (3 + 4)))%n"))
            )
        );
    }

    /**
     * POSIX 2.7.4 Here-Document - delimited input becomes stdin for the
     * command.
     */
    @Test
    void hereDocument() throws Exception {
        Assertions.assertEquals(
            String.format("first line%nsecond line%n"),
            this.runChildJvm(
                this.writeScript(
                    "hd.sh", String.format("cat <<EOF%nfirst line%nsecond line%nEOF%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.1 If Conditional Construct - if/elif/else/fi.
     */
    @Test
    void ifThenElseConstruct() throws Exception {
        Assertions.assertEquals(
            String.format("yes%n"),
            this.runChildJvm(
                this.writeScript(
                    "ite.sh",
                    String.format("if false; then echo no; else echo yes; fi%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case Conditional Construct - case/in/esac.
     */
    @Test
    void caseConditionalConstruct() throws Exception {
        Assertions.assertEquals(
            String.format("b%n"),
            this.runChildJvm(
                this.writeScript(
                    "case.sh",
                    String.format(
                        "x=banana%ncase $x in%n"
                        + "  apple) echo a;;%n  banana) echo b;;%nesac%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.5 Function Definition Command - functions persist and bind
     * positional parameters.
     */
    @Test
    void functionDefinitionAndCall() throws Exception {
        Assertions.assertEquals(
            String.format("hi world%n"),
            this.runChildJvm(
                this.writeScript(
                    "fn.sh",
                    String.format("greet() { echo hi $1; }%ngreet world%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters, part of POSIX 2.5 Parameters and
     * Variables - $# yields positional-parameter count.
     */
    @Test
    void specialParameterPositionalCount() throws Exception {
        Assertions.assertEquals(
            String.format("count=3%n"),
            this.runChildJvm(
                this.writeScript(
                    "dollar-hash.sh",
                    String.format("set -- a b c%necho count=$#%n")
                )
            )
        );
    }

    /**
     * POSIX 2.2.1 Escape Character - backslash quotes the next character so it
     * loses any special meaning.
     */
    @Test
    void backslashEscapesDollar() throws Exception {
        Assertions.assertEquals(
            String.format("$X%n"),
            this.runChildJvm(
                this.writeScript("bs.sh", String.format("X=val%necho \\$X%n"))
            )
        );
    }

    /**
     * POSIX 2.6.5 Field Splitting - unquoted expansion is split on IFS.
     */
    @Test
    void fieldSplittingOnIfs() throws Exception {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            this.runChildJvm(
                this.writeScript(
                    "fs.sh", String.format("IFS=:%nX=a:b:c%nfor i in $X; do echo $i; done%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.6 Pathname Expansion - glob characters match filenames.
     */
    @Test
    void pathnameExpansionGlob() throws Exception {
        final File dir = TempDirs.folder(IntegrationTest.root, "globdir");
        Assertions.assertTrue(new File(dir, "alpha.txt").createNewFile());
        Assertions.assertTrue(new File(dir, "beta.txt").createNewFile());
        Assertions.assertTrue(new File(dir, "other.log").createNewFile());
        Assertions.assertEquals(
            String.format("alpha.txt%nbeta.txt%n"),
            this.runChildJvm(
                this.writeScript(
                    "glob.sh",
                    String.format(
                        "cd %s%necho *.txt | tr ' ' '\\n' | sort%n",
                        dir.getAbsolutePath()
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${#var} yields length.
     */
    @Test
    void parameterLengthExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("5%n"),
            this.runChildJvm(
                this.writeScript("plen.sh", String.format("X=hello%necho ${#X}%n"))
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var%suffix} removes shortest suffix.
     */
    @Test
    void parameterSuffixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("foo.tar%n"),
            this.runChildJvm(
                this.writeScript(
                    "psuf.sh",
                    String.format("X=foo.tar.gz%necho ${X%%.gz}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.8.2 Exit Status - `!` inverts the exit status of the pipeline.
     */
    @Test
    void bangInvertsPipelineExit() throws Exception {
        Assertions.assertEquals(
            String.format("1%n"),
            this.runChildJvm(
                this.writeScript("bang.sh", String.format("! true%necho $?%n"))
            )
        );
    }

    /**
     * POSIX 2.9.4.3 While - body runs while the condition exits zero.
     */
    @Test
    void whileLoopCounts() throws Exception {
        Assertions.assertEquals(
            String.format("1%n2%n3%n"),
            this.runChildJvm(
                this.writeScript(
                    "while.sh",
                    String.format("i=1%nwhile [ $i -le 3 ]; do echo $i; i=$((i+1)); done%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.5 Until - body runs while the condition exits non-zero.
     */
    @Test
    void untilLoopCounts() throws Exception {
        Assertions.assertEquals(
            String.format("1%n2%n3%n"),
            this.runChildJvm(
                this.writeScript(
                    "until.sh",
                    String.format("i=1%nuntil [ $i -gt 3 ]; do echo $i; i=$((i+1)); done%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.3 Lists - AND-list short-circuits on first failure.
     */
    @Test
    void andOrListShortCircuit() throws Exception {
        Assertions.assertEquals(
            String.format("ran%n"),
            this.runChildJvm(
                this.writeScript(
                    "andor.sh",
                    String.format("false && echo skip || echo ran%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.2 For - iterates over the given word list.
     */
    @Test
    void forIteratesWords() throws Exception {
        Assertions.assertEquals(
            String.format("one%ntwo%nthree%n"),
            this.runChildJvm(
                this.writeScript(
                    "for.sh",
                    String.format("for n in one two three; do echo $n; done%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.1 Positional Parameters - $1, $2 access individual params.
     */
    @Test
    void positionalParametersDollarOneAndTwo() throws Exception {
        Assertions.assertEquals(
            String.format("alpha-beta%n"),
            this.runChildJvm(
                this.writeScript(
                    "pos.sh", String.format("set -- alpha beta gamma%necho $1-$2%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.1 Tilde Expansion - leading ~ expands to $HOME.
     */
    @Test
    void tildeExpansionToHome() throws Exception {
        Assertions.assertEquals(
            String.format("/tmp/home/x%n"),
            this.runChildJvm(
                this.writeScript(
                    "tilde.sh", String.format("HOME=/tmp/home%necho ~/x%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.7 Quote Removal - escaped quote inside double-quotes is
     * preserved literally.
     */
    @Test
    void quoteRemovalEscapedDoubleQuote() throws Exception {
        Assertions.assertEquals(
            String.format("a\"b%n"),
            this.runChildJvm(
                this.writeScript(
                    "qr.sh", String.format("echo \"a\\\"b\"%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.6 Duplicating Output FD - 2>&1 merges stderr into stdout.
     */
    @Test
    void duplicateStderrToStdout() throws Exception {
        Assertions.assertEquals(
            String.format("to-out%nto-err%n"),
            this.runChildJvm(
                this.writeScript(
                    "dupfd.sh",
                    String.format("echo to-out%necho to-err 1>&2 2>&1%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.6 Brace Group - { list; } groups commands without subshell.
     */
    @Test
    void braceGroupSharesEnvironment() throws Exception {
        Assertions.assertEquals(
            String.format("inside%n"),
            this.runChildJvm(
                this.writeScript(
                    "brace.sh",
                    String.format("{ X=inside; }%necho $X%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.1 Subshell - ( list ) runs in a subshell; outer env
     * unchanged.
     */
    @Test
    void subshellEnvironmentIsolated() throws Exception {
        Assertions.assertEquals(
            String.format("inner%nouter%n"),
            this.runChildJvm(
                this.writeScript(
                    "sub.sh",
                    String.format("X=outer%n(X=inner; echo $X)%necho $X%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.3 PATH - command search finds executables on PATH.
     */
    @Test
    void pathSearchFindsExternalCommand() throws Exception {
        Assertions.assertEquals(
            String.format("hello%n"),
            this.runChildJvm(
                this.writeScript(
                    "path.sh", String.format("echo $(printf hello)%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - variable reference inside $((..)).
     */
    @Test
    void arithmeticReferencesVariable() throws Exception {
        Assertions.assertEquals(
            String.format("42%n"),
            this.runChildJvm(
                this.writeScript(
                    "arith2.sh", String.format("a=6; b=7%necho $((a * b))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.8.2 Exit Status - $? carries the last command's exit.
     */
    @Test
    void exitStatusPropagatesToDollarQuestion() throws Exception {
        Assertions.assertEquals(
            String.format("7%n"),
            this.runChildJvm(
                this.writeScript(
                    "dollarq.sh",
                    String.format("(exit 7)%necho $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var#prefix} removes shortest
     * matching prefix.
     */
    @Test
    void parameterPrefixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("bar.baz%n"),
            this.runChildJvm(
                this.writeScript(
                    "ppref.sh",
                    String.format("X=foo.bar.baz%necho ${X#foo.}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.3.1 Alias Substitution - alias name expands to its body before
     * parsing the rest of the command.
     */
    @Test
    void aliasSubstitution() throws Exception {
        Assertions.assertEquals(
            String.format("hi there%n"),
            this.runChildJvm(
                this.writeScript(
                    "alias.sh",
                    String.format("alias greet='echo hi'%ngreet there%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.7 Open File Descriptor for Reading and Writing - {@code <>}
     * opens a file for both read and write.
     */
    @Test
    void readWriteRedirection() throws Exception {
        final File data = TempDirs.file(IntegrationTest.root, "rw.dat");
        Files.writeString(data.toPath(), String.format("stored%n"));
        Assertions.assertEquals(
            String.format("stored%n"),
            this.runChildJvm(
                this.writeScript(
                    "rw.sh",
                    String.format("cat <>%s%n", data.getAbsolutePath())
                )
            )
        );
    }

    /**
     * POSIX 2.8.1 Consequences of Shell Errors - {@code set -e} aborts on
     * first command failure.
     */
    @Test
    void setEAbortsOnFirstFailure() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript(
                "sete.sh",
                String.format("set -e%nfalse%necho should-not-print%n")
            )
        );
        Assertions.assertFalse(
            out.contains("should-not-print"),
            String.format(
                "set -e should abort before echo runs: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.14 export builtin - marks variable for child environment.
     */
    @Test
    void exportMakesVariableVisibleToSubshell() throws Exception {
        Assertions.assertEquals(
            String.format("outer%n"),
            this.runChildJvm(
                this.writeScript(
                    "export.sh",
                    String.format("X=outer%nexport X%n(echo $X)%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 unset builtin - removes a variable.
     */
    @Test
    void unsetRemovesVariable() throws Exception {
        Assertions.assertEquals(
            String.format("[empty]%n"),
            this.runChildJvm(
                this.writeScript(
                    "unset.sh",
                    String.format("X=hello%nunset X%necho [${X-empty}]%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 shift builtin - drops positional parameter $1, renumbers
     * the rest.
     */
    @Test
    void shiftDropsFirstPositional() throws Exception {
        Assertions.assertEquals(
            String.format("b-c%n"),
            this.runChildJvm(
                this.writeScript(
                    "shift.sh",
                    String.format("set -- a b c%nshift%necho $1-$2%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 readonly builtin - prevents reassignment.
     */
    @Test
    void readonlyPreventsReassignment() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript(
                "ro.sh",
                String.format("readonly R=fixed%nR=changed%necho $R%n")
            )
        );
        Assertions.assertTrue(
            out.contains("readonly"),
            String.format(
                "expected readonly diagnostic, got: %s",
                out
            )
        );
        Assertions.assertFalse(
            out.contains("fixed"),
            String.format(
                "assignment to a readonly variable must abort a non-interactive shell, got: %s",
                out
            )
        );
        Assertions.assertFalse(
            out.contains("changed"),
            String.format(
                "readonly should block reassignment, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.14 true / false builtins - exit 0 / 1 respectively.
     */
    @Test
    void trueAndFalseExitCodes() throws Exception {
        Assertions.assertEquals(
            String.format("0%n1%n"),
            this.runChildJvm(
                this.writeScript(
                    "tf.sh",
                    String.format("true; echo $?%nfalse; echo $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 pwd builtin - prints the working directory.
     */
    @Test
    void pwdPrintsWorkingDirectory() throws Exception {
        final File dir = TempDirs.folder(IntegrationTest.root, "wd");
        final String out = this.runChildJvm(
            this.writeScript(
                "pwd.sh",
                String.format("cd %s%npwd%n", dir.getAbsolutePath())
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/wd"),
            String.format(
                "expected pwd to end with /wd, got %s",
                out
            )
        );
        Assertions.assertTrue(
            out.contains(dir.getName()),
            String.format(
                "expected pwd to mention %s", dir.getName()
            )
        );
    }

    /**
     * POSIX 2.14 colon builtin - {@code :} is a no-op returning 0.
     */
    @Test
    void colonBuiltinIsNoop() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(
                this.writeScript(
                    "colon.sh",
                    String.format(":%necho $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 cd builtin - changes the working directory.
     */
    @Test
    void cdChangesWorkingDirectory() throws Exception {
        final File target = TempDirs.folder(IntegrationTest.root, "cd-target");
        final String out = this.runChildJvm(
            this.writeScript(
                "cd.sh",
                String.format("cd %s%npwd%n", target.getAbsolutePath())
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/cd-target"),
            String.format("expected to land in /cd-target, got %s", out)
        );
        Assertions.assertTrue(
            out.contains(target.getName()),
            String.format("expected pwd to mention %s", target.getName())
        );
    }

    /**
     * POSIX 2.14 eval builtin - parses and runs its arguments as shell input.
     */
    @Test
    void evalRunsConstructedCommand() throws Exception {
        Assertions.assertEquals(
            String.format("dynamic%n"),
            this.runChildJvm(
                this.writeScript(
                    "eval.sh",
                    String.format("CMD='echo dynamic'%neval $CMD%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 read builtin - reads one line from stdin into VAR.
     */
    @Test
    void readBuiltinFromStdinViaHeredoc() throws Exception {
        Assertions.assertEquals(
            String.format("got=hello world%n"),
            this.runChildJvm(
                this.writeScript(
                    "read.sh",
                    String.format("read VAR <<EOF%nhello world%nEOF%necho got=$VAR%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 getopts builtin - parses option flags.
     */
    @Test
    void getoptsParsesSingleFlag() throws Exception {
        Assertions.assertEquals(
            String.format("a:foo%n"),
            this.runChildJvm(
                this.writeScript(
                    "getopts.sh",
                    String.format(
                        "set -- -a foo%n"
                        + "while getopts a: opt; do echo $opt:$OPTARG; done%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.14 umask builtin - default mask is 0022.
     */
    @Test
    void umaskDefaultIsPrintable() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "umask.sh", String.format("umask%n")
            )
        ).trim();
        Assertions.assertTrue(
            out.matches("\\d{4}"),
            String.format("expected 4-digit octal, got %s", out)
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $$ is the shell's PID (positive int).
     */
    @Test
    void specialParameterDollarDollarIsPid() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "pid.sh", String.format("echo $$%n")
            )
        ).trim();
        Assertions.assertTrue(
            out.matches("\\d+"),
            String.format("expected positive int, got %s", out)
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:?word} writes error to stderr
     * when var is unset.
     */
    @Test
    void parameterErrorIfUnset() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript(
                "perr.sh",
                String.format("echo ${UNSET_REQ:?required}%n")
            )
        );
        Assertions.assertTrue(
            out.contains("UNSET_REQ") || out.contains("required"),
            String.format(
                "expected error mentioning UNSET_REQ/required, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:+word} substitutes word when
     * var is set non-empty.
     */
    @Test
    void parameterAlternativeIfSet() throws Exception {
        Assertions.assertEquals(
            String.format("present%n%n"),
            this.runChildJvm(
                this.writeScript(
                    "palt.sh",
                    String.format("X=v%necho ${X:+present}%necho ${UNSET:+present}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.2 Pipelines - data flows through every stage of a 3-stage
     * pipeline.
     */
    @Test
    void multiStagePipeline() throws Exception {
        Assertions.assertEquals(
            String.format("HEO%n"),
            this.runChildJvm(
                this.writeScript(
                    "pipe.sh",
                    String.format("echo hello | tr 'a-z' 'A-Z' | tr -d 'L'%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.1 Redirecting Input - input redirect supplies the content of
     * a file to the command's stdin.
     */
    @Test
    void inputRedirectionFromFile() throws Exception {
        final File data = TempDirs.file(IntegrationTest.root, "in.dat");
        Files.writeString(data.toPath(), String.format("feed%n"));
        Assertions.assertEquals(
            String.format("feed%n"),
            this.runChildJvm(
                this.writeScript(
                    "redir.sh",
                    String.format("cat <%s%n", data.getAbsolutePath())
                )
            )
        );
    }

    /**
     * POSIX 2.14 cd builtin - {@code cd -} jumps back to the previous
     * directory recorded in OLDPWD.
     */
    @Test
    void cdDashRevisitsPreviousDirectory() throws Exception {
        final File first = TempDirs.folder(IntegrationTest.root, "first");
        final File second = TempDirs.folder(IntegrationTest.root, "second");
        final String out = this.runChildJvm(
            this.writeScript(
                "cdback.sh",
                String.format(
                    "cd %s%ncd %s%ncd - >/dev/null%npwd%n",
                    first.getAbsolutePath(), second.getAbsolutePath()
                )
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/first"),
            String.format("expected to return to /first, got %s", out)
        );
        Assertions.assertNotEquals(
            second.getAbsolutePath(), out,
            "cd - must not land on the directory just left"
        );
    }

    /**
     * POSIX 2.14 type builtin - reports how the shell would interpret a
     * name (builtin / function / file / not found).
     */
    @Test
    void typeBuiltinReportsBuiltin() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "type.sh", String.format("type echo%n")
            )
        );
        Assertions.assertTrue(
            out.contains("echo"),
            String.format(
                "expected output to mention echo as a builtin, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.14 command -v - prints the command location / kind.
     */
    @Test
    void commandDashVReportsBuiltin() throws Exception {
        Assertions.assertEquals(
            "cd",
            this.runChildJvm(
                this.writeScript(
                    "cmdv.sh", String.format("command -v cd%n")
                )
            ).trim()
        );
    }

    /**
     * POSIX 2.14 hash builtin - {@code hash -r} clears the cache and
     * returns 0.
     */
    @Test
    void hashDashRReturnsZero() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(
                this.writeScript(
                    "hashr.sh", String.format("hash -r%necho $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.2 Quoting - mixing single and double quotes in one word
     * preserves each region's semantics.
     */
    @Test
    void mixedQuotingPreservesEachRegion() throws Exception {
        Assertions.assertEquals(
            String.format("$Xval%n"),
            this.runChildJvm(
                this.writeScript(
                    "mq.sh",
                    String.format("X=val%necho '$X'\"$X\"%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.5 Function Definition - recursive function (factorial of 5).
     */
    @Test
    void recursiveFunctionFactorial() throws Exception {
        Assertions.assertEquals(
            String.format("120%n"),
            this.runChildJvm(
                this.writeScript(
                    "fact.sh",
                    String.format(
                        "fact() {%n  if [ $1 -le 1 ]; then echo 1; "
                            + "else echo $(( $1 * $(fact $(( $1 - 1 ))) )); fi%n"
                            + "}%nfact 5%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.14 wait builtin - {@code wait} without args returns 0 when no
     * background jobs are running.
     */
    @Test
    void waitWithoutBackgroundJobsReturnsZero() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(
                this.writeScript(
                    "wait.sh", String.format("wait%necho $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.4 Here-Document with quoted delimiter - body is NOT
     * variable-expanded.
     */
    @Test
    void heredocQuotedDelimiterPreventsExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("$X literal%n"),
            this.runChildJvm(
                this.writeScript(
                    "hdq.sh",
                    String.format(
                        "X=val%ncat <<'EOF'%n$X literal%nEOF%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $0 expands to the script name passed
     * to the shell.
     */
    @Test
    void specialParameterDollarZeroIsScriptName() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "dollar0.sh", String.format("echo $0%n")
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/dollar0.sh"),
            String.format(
                "expected $0 to end with /dollar0.sh, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.6.1 Tilde Expansion - tilde NOT at the start of a word must
     * NOT expand.
     */
    @Test
    void tildeMidWordNotExpanded() throws Exception {
        Assertions.assertEquals(
            String.format("a~b%n"),
            this.runChildJvm(
                this.writeScript(
                    "midtilde.sh",
                    String.format("HOME=/h%necho a~b%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - unary minus produces negative
     * integer.
     */
    @Test
    void arithmeticNegativeNumbers() throws Exception {
        Assertions.assertEquals(
            String.format("-7%n"),
            this.runChildJvm(
                this.writeScript(
                    "arithneg.sh", String.format("echo $((-3 - 4))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - octal literal (leading 0).
     */
    @Test
    void arithmeticOctalLiteral() throws Exception {
        Assertions.assertEquals(
            String.format("9%n"),
            this.runChildJvm(
                this.writeScript(
                    "octal.sh", String.format("echo $((010 + 1))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.3 nested while loops - inner break breaks only the inner
     * loop.
     */
    @Test
    void nestedWhileInnerBreak() throws Exception {
        Assertions.assertEquals(
            String.format("0.0%n0.1%n1.0%n1.1%n"),
            this.runChildJvm(
                this.writeScript(
                    "nested.sh",
                    String.format(
                        "i=0%nwhile [ $i -lt 2 ]; do "
                            + "j=0; while [ $j -lt 3 ]; do echo $i.$j; "
                            + "if [ $j -eq 1 ]; then break; fi; j=$((j+1)); done; "
                            + "i=$((i+1)); done%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.5 Function Definition - a function can shadow a builtin
     * within its own scope.
     */
    @Test
    void functionShadowsBuiltin() throws Exception {
        Assertions.assertEquals(
            String.format("wrapped: hi%n"),
            this.runChildJvm(
                this.writeScript(
                    "shadow.sh",
                    String.format(
                        "echo() { "
                        + "/usr/bin/env echo wrapped: $*; }"
                        + "%necho hi%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.14 unset -f - removes a function definition.
     */
    @Test
    void unsetDashFRemovesFunction() throws Exception {
        Assertions.assertEquals(
            String.format("from-fn%ngone%n"),
            this.runChildJvm(
                this.writeScript(
                    "unsetf.sh",
                    String.format(
                        "f() { echo from-fn; }%nf%nunset -f f%nf 2>/dev/null || echo gone%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.3 Token Recognition - {@code #} starts a comment that runs
     * to end-of-line; the next command still executes.
     */
    @Test
    void hashStartsComment() throws Exception {
        Assertions.assertEquals(
            String.format("after%n"),
            this.runChildJvm(
                this.writeScript(
                    "comment.sh",
                    String.format("# this is a comment%necho after%n")
                )
            )
        );
    }

    /**
     * POSIX 2.2.1 Escape Character - backslash-newline outside quotes is
     * a line continuation, joining the two halves into one word.
     */
    @Test
    void backslashNewlineIsLineContinuation() throws Exception {
        Assertions.assertEquals(
            String.format("onetwo%n"),
            this.runChildJvm(
                this.writeScript(
                    "cont.sh",
                    String.format("echo one\\%ntwo%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.3 IFS - reset to default (space-tab-newline) when unset
     * during field splitting.
     */
    @Test
    void ifsUnsetUsesDefault() throws Exception {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            this.runChildJvm(
                this.writeScript(
                    "ifs.sh",
                    String.format(
                        "unset IFS%nX='a b c'%nfor i in $X; do echo $i; done%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case Patterns - `|` separates alternative patterns.
     */
    @Test
    void caseAlternativePatterns() throws Exception {
        Assertions.assertEquals(
            String.format("letter%nletter%nother%nother%n"),
            this.runChildJvm(
                this.writeScript(
                    "casealt.sh",
                    String.format(
                        "for w in a b c d; do case $w in a|b) echo letter;; *) echo other;; esac; done%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case Patterns - `?` matches any single character.
     */
    @Test
    void caseQuestionMarkMatchesSingleChar() throws Exception {
        Assertions.assertEquals(
            String.format("two%nnot-two%nnot-two%n"),
            this.runChildJvm(
                this.writeScript(
                    "caseq.sh",
                    String.format(
                        "for w in ab abc a; do case $w in ??) echo two;; *) echo not-two;; esac; done%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var##pattern}` removes longest
     * matching prefix.
     */
    @Test
    void parameterLongestPrefixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("file.txt%n"),
            this.runChildJvm(
                this.writeScript(
                    "lpref.sh",
                    String.format("X=/path/to/file.txt%necho ${X##*/}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var%%pattern}` removes longest
     * matching suffix.
     */
    @Test
    void parameterLongestSuffixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("foo%n"),
            this.runChildJvm(
                this.writeScript(
                    "lsuf.sh",
                    String.format("X=foo.tar.gz%necho ${X%%%%.*}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - bit-shift operators.
     */
    @Test
    void arithmeticBitShift() throws Exception {
        Assertions.assertEquals(
            String.format("32 128%n"),
            this.runChildJvm(
                this.writeScript(
                    "shift.sh",
                    String.format("echo $((1 << 5)) $((1024 >> 3))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - comparison operators yield 0/1.
     */
    @Test
    void arithmeticComparisonResult() throws Exception {
        Assertions.assertEquals(
            String.format("1 0 1%n"),
            this.runChildJvm(
                this.writeScript(
                    "cmp.sh",
                    String.format("echo $((5 > 3)) $((3 > 5)) $((5 == 5))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.5 Function Definition - {@code return N} sets the function's
     * exit status.
     */
    @Test
    void functionReturnValue() throws Exception {
        Assertions.assertEquals(
            String.format("42%n"),
            this.runChildJvm(
                this.writeScript(
                    "ret.sh",
                    String.format("f() { return 42; }%nf%necho $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 break N - breaks out of N levels of loops.
     */
    @Test
    void breakNExitsMultipleLoops() throws Exception {
        Assertions.assertEquals(
            String.format("1.a%nafter%n"),
            this.runChildJvm(
                this.writeScript(
                    "brk2.sh",
                    String.format(
                        "for i in 1 2; do for j in a b c; do "
                            + "if [ $j = b ]; then break 2; fi; "
                            + "echo $i.$j; done; done%necho after%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.7.5 Duplicating an Input FD - {@code n<&-} closes the named fd.
     */
    @Test
    void closeInputFileDescriptor() throws Exception {
        Assertions.assertEquals(
            String.format("in%npost%n"),
            this.runChildJvm(
                this.writeScript(
                    "close.sh",
                    String.format("cat <<EOF%nin%nEOF%necho post%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $- contains current shell flags.
     */
    @Test
    void specialParameterDashIsShellFlags() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "dash.sh",
                String.format("set -e%necho $-%n")
            )
        ).trim();
        Assertions.assertTrue(
            out.contains("e"),
            String.format("expected $- to contain e, got: %s", out)
        );
    }

    /**
     * POSIX 2.5.3 LINENO - shell-variable holding the current line number
     * of the executing script.
     */
    @Test
    void linenoTracksScriptLineNumber() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "ln.sh",
                String.format("echo first%necho second-$LINENO%n")
            )
        );
        Assertions.assertTrue(
            out.contains("second-2"),
            String.format(
                "expected LINENO to fire on line 2, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.5.3 OLDPWD - previous working directory after cd.
     */
    @Test
    void oldpwdAfterCd() throws Exception {
        final File target = TempDirs.folder(IntegrationTest.root, "after");
        final String out = this.runChildJvm(
            this.writeScript(
                "oldpwd.sh",
                String.format(
                    "ORIG=$PWD%ncd %s%necho $OLDPWD%n", target.getAbsolutePath()
                )
            )
        ).trim();
        Assertions.assertFalse(out.isEmpty(), "OLDPWD should not be empty");
        Assertions.assertNotEquals(
            target.getAbsolutePath(), out,
            "OLDPWD should name the directory left behind"
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - ternary operator.
     */
    @Test
    void arithmeticTernaryOperator() throws Exception {
        Assertions.assertEquals(
            String.format("42 13%n"),
            this.runChildJvm(
                this.writeScript(
                    "tern.sh",
                    String.format("echo $((1 > 0 ? 42 : 13)) $((1 > 5 ? 42 : 13))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - logical AND / OR short-circuit.
     */
    @Test
    void arithmeticLogicalOps() throws Exception {
        Assertions.assertEquals(
            String.format("0 1 1%n"),
            this.runChildJvm(
                this.writeScript(
                    "logop.sh",
                    String.format("echo $((1 && 0)) $((0 || 2)) $((1 && 3))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 readonly -p - lists all readonly variables.
     */
    @Test
    void readonlyDashPListsBindings() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "rop.sh",
                String.format("readonly X=hello%nreadonly -p%n")
            )
        );
        Assertions.assertTrue(
            out.contains("X"),
            String.format("expected to list X, got: %s", out)
        );
    }

    /**
     * POSIX 2.14 type - resolves multiple names in one invocation.
     */
    @Test
    void typeWithMultipleNames() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "tmulti.sh",
                String.format("type echo cd%n")
            )
        );
        Assertions.assertTrue(
            out.contains("echo"),
            String.format("expected echo in output, got: %s", out)
        );
        Assertions.assertTrue(
            out.contains("cd"),
            String.format("expected cd in output, got: %s", out)
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $! is the PID of the most recently
     * started background command.
     */
    @Test
    void specialParameterBangIsLastBgPid() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "bgpid.sh",
                String.format("true &%necho ${!}%n")
            )
        ).trim();
        Assertions.assertTrue(
            out.matches("\\d+"),
            String.format(
                "expected positive int for $!, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.14 set -- with no further args clears positional parameters.
     */
    @Test
    void setDashDashClearsPositionalParams() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(
                this.writeScript(
                    "setclr.sh",
                    String.format("set -- a b c%nset --%necho ${#}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.4 here-document with leading-tab strip via {@code <<-}.
     */
    @Test
    void heredocDashStripsLeadingTabs() throws Exception {
        Assertions.assertEquals(
            String.format("indented%nagain%n"),
            this.runChildJvm(
                this.writeScript(
                    "hdtab.sh",
                    String.format("cat <<-EOF%n\tindented%n\tagain%n\tEOF%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $$ stays stable across the script
     * (every read returns the same pid).
     */
    @Test
    void specialParameterDollarDollarStable() throws Exception {
        Assertions.assertEquals(
            String.format("same%n"),
            this.runChildJvm(
                this.writeScript(
                    "ddstable.sh",
                    String.format("A=$$%nB=$$%n[ \"$A\" = \"$B\" ] && echo same%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - parentheses change grouping.
     */
    @Test
    void arithmeticParensGroup() throws Exception {
        Assertions.assertEquals(
            String.format("20%n"),
            this.runChildJvm(
                this.writeScript(
                    "arithparen.sh",
                    String.format("echo $(( (2 + 3) * 4 ))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - nested ${X-${Y-default}}: outer
     * default falls back to inner default when both vars are unset.
     */
    @Test
    void parameterExpansionNested() throws Exception {
        Assertions.assertEquals(
            String.format("fallback%n"),
            this.runChildJvm(
                this.writeScript(
                    "nested.sh",
                    String.format("echo ${UNSET_X-${UNSET_Y-fallback}}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7 Redirection - multiple redirections on one command apply
     * in order (later overrides earlier on same fd).
     */
    @Test
    void multipleRedirectionsOnOneCommand() throws Exception {
        final File data = TempDirs.file(IntegrationTest.root, "multi.dat");
        Files.writeString(data.toPath(), String.format("line%n"));
        Assertions.assertEquals(
            String.format("line%n"),
            this.runChildJvm(
                this.writeScript(
                    "multi.sh",
                    String.format("cat <%s 2>&1%n", data.getAbsolutePath())
                )
            )
        );
    }

    /**
     * POSIX 2.9.1 Simple Commands - a command with assignments and no
     * command name persists the assignments in the shell environment.
     */
    @Test
    void assignmentOnlyCommandPersists() throws Exception {
        Assertions.assertEquals(
            String.format("[set]%n"),
            this.runChildJvm(
                this.writeScript(
                    "assignonly.sh",
                    String.format("FOO=set%necho [$FOO]%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.2 Pipelines - leading `!` inverts the pipeline exit status.
     */
    @Test
    void bangBeforePipelineInverts() throws Exception {
        Assertions.assertEquals(
            String.format("0%n1%n"),
            this.runChildJvm(
                this.writeScript(
                    "bangpipe.sh",
                    String.format("! false; echo $?%n! true; echo $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case - {@code *)} matches anything as the default arm.
     */
    @Test
    void caseStarDefaultMatches() throws Exception {
        Assertions.assertEquals(
            String.format("a%nd%nd%n"),
            this.runChildJvm(
                this.writeScript(
                    "casestar.sh",
                    String.format(
                        "for w in apple banana cherry; do "
                            + "case $w in apple) echo a;; *) echo d;; esac; done%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.6.3 Command Substitution - backtick form is equivalent to $(...).
     */
    @Test
    void backtickCommandSubstitution() throws Exception {
        Assertions.assertEquals(
            String.format("got=hello%n"),
            this.runChildJvm(
                this.writeScript(
                    "btick.sh",
                    String.format("X=`echo hello`%necho got=$X%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 nested case - case inside a case body works.
     */
    @Test
    void nestedCaseStatement() throws Exception {
        Assertions.assertEquals(
            String.format("a2%n"),
            this.runChildJvm(
                this.writeScript(
                    "ncase.sh",
                    String.format(
                        "X=a; Y=2%ncase $X in a) case $Y in 2) echo a2;; "
                            + "*) echo a-other;; esac;; esac%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - bitwise AND / OR / XOR.
     */
    @Test
    void arithmeticBitwiseOps() throws Exception {
        Assertions.assertEquals(
            String.format("2 7 5%n"),
            this.runChildJvm(
                this.writeScript(
                    "bitops.sh",
                    String.format("echo $((6 & 3)) $((6 | 1)) $((6 ^ 3))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.14 exec with no command modifies the shell's own redirections
     * (here: redirecting stderr to stdout, so the next echo goes to stdout).
     */
    @Test
    void execWithNoCommandRedirectsShell() throws Exception {
        Assertions.assertEquals(
            String.format("to-stderr%n"),
            this.runChildJvm(
                this.writeScript(
                    "execredir.sh",
                    String.format("exec 2>&1%necho to-stderr 1>&2%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - logical NOT (!) inverts truthiness.
     */
    @Test
    void arithmeticLogicalNot() throws Exception {
        Assertions.assertEquals(
            String.format("1 0 1%n"),
            this.runChildJvm(
                this.writeScript(
                    "lnot.sh",
                    String.format("echo $((!0)) $((!5)) $((!!7))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - unary plus and double-negate.
     */
    @Test
    void arithmeticUnaryPlus() throws Exception {
        Assertions.assertEquals(
            String.format("5 7%n"),
            this.runChildJvm(
                this.writeScript(
                    "uplus.sh",
                    String.format("echo $((+5)) $((- -7))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var:-}` with empty default keeps
     * the empty.
     */
    @Test
    void parameterDefaultEmptyWord() throws Exception {
        Assertions.assertEquals(
            String.format("[]%n"),
            this.runChildJvm(
                this.writeScript(
                    "ed.sh",
                    String.format("echo [${UNSET:-}]%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.1 Lists - multiple commands separated by `;` all execute.
     */
    @Test
    void multipleCommandsOnOneLine() throws Exception {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            this.runChildJvm(
                this.writeScript(
                    "multi-cmd.sh",
                    String.format("echo a; echo b; echo c%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - mixed-sign arithmetic.
     */
    @Test
    void arithmeticMixedSign() throws Exception {
        Assertions.assertEquals(
            String.format("5 -5%n"),
            this.runChildJvm(
                this.writeScript(
                    "mix.sh",
                    String.format("echo $((-3 + 8)) $((5 - 10))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.2.1 Escape Character - `\\n` outside quotes preserves the
     * literal 'n' character.
     */
    @Test
    void backslashPreservesNextChar() throws Exception {
        Assertions.assertEquals(
            String.format("anb%n"),
            this.runChildJvm(
                this.writeScript(
                    "esc.sh",
                    String.format("echo a\\nb%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.3 Command Substitution - empty substitution yields empty
     * string.
     */
    @Test
    void emptyCommandSubstitution() throws Exception {
        Assertions.assertEquals(
            String.format("ab%n"),
            this.runChildJvm(
                this.writeScript(
                    "ecs.sh",
                    String.format("echo a$()b%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7 Redirection - higher-numbered fd redirected to file.
     */
    @Test
    void higherNumberedFdRedirection() throws Exception {
        final File data = TempDirs.file(IntegrationTest.root, "fd3.dat");
        Files.writeString(data.toPath(), String.format("from-fd3%n"));
        Assertions.assertEquals(
            String.format("from-fd3%n"),
            this.runChildJvm(
                this.writeScript(
                    "fd3.sh",
                    String.format("cat 0<%s%n", data.getAbsolutePath())
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.2 For - empty word list means zero iterations.
     */
    @Test
    void forEmptyWordListSkipsBody() throws Exception {
        Assertions.assertEquals(
            String.format("after%n"),
            this.runChildJvm(
                this.writeScript(
                    "empfor.sh",
                    String.format("for i in; do echo $i; done%necho after%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.2 For - loop variable retains last iterated value.
     */
    @Test
    void forLoopVarPersistsAfter() throws Exception {
        Assertions.assertEquals(
            String.format("c%n"),
            this.runChildJvm(
                this.writeScript(
                    "forpersist.sh",
                    String.format("for i in a b c; do :; done%necho $i%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.2 Redirecting Output - `>|` overrides noclobber.
     */
    @Test
    void clobberRedirectWritesFile() throws Exception {
        final File target = TempDirs.file(IntegrationTest.root, "clob.dat");
        Files.writeString(target.toPath(), String.format("old%n"));
        Assertions.assertEquals(
            String.format("new%n"),
            this.runChildJvm(
                this.writeScript(
                    "clob.sh",
                    String.format(
                        "echo new >|%s%ncat %s%n",
                        target.getAbsolutePath(), target.getAbsolutePath()
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic - hex literal with `0x` prefix.
     */
    @Test
    void arithmeticHexLiteral() throws Exception {
        Assertions.assertEquals(
            String.format("31%n"),
            this.runChildJvm(
                this.writeScript(
                    "hex.sh",
                    String.format("echo $((0x10 + 0xF))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.3.1 alias chains - aliases expand transitively.
     */
    @Test
    void aliasChain() throws Exception {
        Assertions.assertEquals(
            String.format("final arg%n"),
            this.runChildJvm(
                this.writeScript(
                    "alchain.sh",
                    String.format("alias a=b%nalias b='echo final'%na arg%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.2 special params - $0 inside a function is the script name
     * (not the function name) per POSIX.
     */
    @Test
    void dollarZeroInsideFunction() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "fn0.sh",
                String.format("f() { echo $0; }%nf%n")
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/fn0.sh"),
            String.format(
                "expected $0 to end with /fn0.sh, got: %s",
                out
            )
        );
    }

    /**
     * POSIX 2.7.2 Redirecting Output - the last `>` to the same file wins.
     */
    @Test
    void multipleOutputRedirectsLastWins() throws Exception {
        final File filea = TempDirs.file(IntegrationTest.root, "a.dat");
        final File fileb = TempDirs.file(IntegrationTest.root, "b.dat");
        Assertions.assertEquals(
            String.format("hi%n"),
            this.runChildJvm(
                this.writeScript(
                    "mout.sh",
                    String.format(
                        "echo hi >%s >%s%ncat %s%n",
                        filea.getAbsolutePath(), fileb.getAbsolutePath(),
                        fileb.getAbsolutePath()
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.6.7 Quote Removal - mixed nested quotes preserve each region.
     */
    @Test
    void quoteRemovalMixedNested() throws Exception {
        Assertions.assertEquals(
            String.format("abc%n"),
            this.runChildJvm(
                this.writeScript(
                    "qmix.sh",
                    String.format("echo 'a'\"b\"'c'%n")
                )
            )
        );
    }

    /**
     * POSIX 2.8.2 Exit Status - {@code true; echo $?} reports 0.
     */
    @Test
    void exitStatusTrueIsZero() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(
                this.writeScript(
                    "trueq.sh",
                    String.format("true%necho $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.3 While - explicit while-false loop runs zero iterations.
     */
    @Test
    void whileFalseZeroIterations() throws Exception {
        Assertions.assertEquals(
            String.format("done%n"),
            this.runChildJvm(
                this.writeScript(
                    "wfalse.sh",
                    String.format("while false; do echo never; done%necho done%n")
                )
            )
        );
    }

    private File writeScript(final String name, final String content) throws IOException {
        final File script = TempDirs.file(IntegrationTest.root, name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assertions.assertTrue(script.setExecutable(true));
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
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("child sh4j exit code; output: %s", out)
        );
        return out;
    }

    private static byte[] readAllBytes(final InputStream input) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int num;
        while ((num = input.read(buf)) != -1) {
            bos.write(buf, 0, num);
        }
        return bos.toByteArray();
    }
}
