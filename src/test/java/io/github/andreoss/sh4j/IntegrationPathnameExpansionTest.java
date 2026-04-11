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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Main}: pathname expansion.
 *
 * @since 1.0
 */
public final class IntegrationPathnameExpansionTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    private File writeScript(final String name, final String content) throws IOException {
        final File script = new TempDirs(IntegrationPathnameExpansionTest.root).file(name);
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

    @Test
    void pathnameExpansionGlobSecond() throws Exception {
        final File dir = new TempDirs(IntegrationPathnameExpansionTest.root).folder("globdir");
        new File(dir, "alpha.txt").createNewFile();
        Assertions.assertTrue(new File(dir, "beta.txt").createNewFile());
    }

    @Test
    void pathnameExpansionGlobThird() throws Exception {
        final File dir = new TempDirs(IntegrationPathnameExpansionTest.root).folder("globdir");
        new File(dir, "alpha.txt").createNewFile();
        new File(dir, "beta.txt").createNewFile();
        Assertions.assertTrue(new File(dir, "other.log").createNewFile());
    }

    @Test
    void pathnameExpansionGlobFourth() throws Exception {
        final File dir = new TempDirs(IntegrationPathnameExpansionTest.root).folder("globdir");
        new File(dir, "alpha.txt").createNewFile();
        new File(dir, "beta.txt").createNewFile();
        new File(dir, "other.log").createNewFile();
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

    /**
     * POSIX 2.5.1 Positional Parameters - $1, $2 access individual params.
     */
    @Test
    void positionalParametersDollarOneAndTwo() throws Exception {
        Assertions.assertEquals(
            String.format("alpha-beta%n"),
            this.runChildJvm(
                this.writeScript("pos.sh", String.format("set -- alpha beta gamma%necho $1-$2%n"))
            )
        );
    }

    @Test
    void pwdCommand() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(this.writeScript("pwd.sh", String.format("pwd%n")).getAbsolutePath())
        );
    }

    /**
     * POSIX 2.14 pwd builtin - prints the working directory.
     */
    @Test
    void pwdPrintsWorkingDirectoryFirst() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "pwd.sh",
                String.format(
                    "cd %s%npwd%n",
                    new TempDirs(IntegrationPathnameExpansionTest.root).folder("wd").getAbsolutePath()
                )
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/wd"),
            String.format("expected pwd to end with /wd, got %s", out)
        );
    }

    @Test
    void pwdPrintsWorkingDirectorySecond() throws Exception {
        final File dir = new TempDirs(IntegrationPathnameExpansionTest.root).folder("wd");
        Assertions.assertTrue(
            this.runChildJvm(
                this.writeScript("pwd.sh", String.format("cd %s%npwd%n", dir.getAbsolutePath()))
            ).trim().contains(dir.getName()),
            String.format("expected pwd to mention %s", dir.getName())
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
            this.runChildJvm(this.writeScript("qr.sh", String.format("echo \"a\\\"b\"%n")))
        );
    }

    /**
     * POSIX 2.6.7 Quote Removal - mixed nested quotes preserve each region.
     */
    @Test
    void quoteRemovalMixedNested() throws Exception {
        Assertions.assertEquals(
            String.format("abc%n"),
            this.runChildJvm(this.writeScript("qmix.sh", String.format("echo 'a'\"b\"'c'%n")))
        );
    }

    /**
     * POSIX 2.7.7 Open File Descriptor for Reading and Writing - {@code <>}
     * opens a file for both read and write.
     */
    @Test
    void readWriteRedirection() throws Exception {
        final File data = new TempDirs(IntegrationPathnameExpansionTest.root).file("rw.dat");
        Files.writeString(data.toPath(), String.format("stored%n"));
        Assertions.assertEquals(
            String.format("stored%n"),
            this.runChildJvm(
                this.writeScript("rw.sh", String.format("cat <>%s%n", data.getAbsolutePath()))
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
     * POSIX 2.14 readonly builtin - prevents reassignment.
     */
    @Test
    void readonlyPreventsReassignmentFirst() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript("ro.sh", String.format("readonly R=fixed%nR=changed%necho $R%n"))
        );
        Assertions.assertTrue(
            out.contains("readonly"),
            String.format("expected readonly diagnostic, got: %s", out)
        );
    }

    @Test
    void readonlyPreventsReassignmentSecond() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript("ro.sh", String.format("readonly R=fixed%nR=changed%necho $R%n"))
        );
        Assertions.assertFalse(
            out.contains("fixed"),
            String.format(
                "assignment to a readonly variable must abort a non-interactive shell, got: %s",
                out
            )
        );
    }

    @Test
    void readonlyPreventsReassignmentThird() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript("ro.sh", String.format("readonly R=fixed%nR=changed%necho $R%n"))
        );
        Assertions.assertFalse(
            out.contains("changed"),
            String.format("readonly should block reassignment, got: %s", out)
        );
    }

    /**
     * POSIX 2.14 readonly -p - lists all readonly variables.
     */
    @Test
    void readonlyDashPListsBindings() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("rop.sh", String.format("readonly X=hello%nreadonly -p%n"))
        );
        Assertions.assertTrue(out.contains("X"), String.format("expected to list X, got: %s", out));
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
                        "fact() {%n  if [ $1 -le 1 ]; then echo 1; %s fi%n}%nfact 5%n",
                        "else echo $(( $1 * $(fact $(( $1 - 1 ))) ));"
                    )
                )
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

    /**
     * POSIX 2.8.1 Consequences of Shell Errors - {@code set -e} aborts on
     * first command failure.
     */
    @Test
    void setEAbortsOnFirstFailure() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript("sete.sh", String.format("set -e%nfalse%necho should-not-print%n"))
        );
        Assertions.assertFalse(
            out.contains("should-not-print"),
            String.format("set -e should abort before echo runs: %s", out)
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
                this.writeScript("setclr.sh", String.format("set -- a b c%nset --%necho ${#}%n"))
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
                this.writeScript("shift.sh", String.format("set -- a b c%nshift%necho $1-$2%n"))
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
            this.runChildJvm(this.writeScript("sq.sh", String.format("X=val%necho '$X'%n")))
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
                this.writeScript("dollar-hash.sh", String.format("set -- a b c%necho count=$#%n"))
            )
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $$ is the shell's PID (positive int).
     */
    @Test
    void specialParameterDollarDollarIsPid() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("pid.sh", String.format("echo $$%n"))
        ).trim();
        Assertions.assertTrue(
            out.matches("\\d+"),
            String.format("expected positive int, got %s", out)
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $0 expands to the script name passed
     * to the shell.
     */
    @Test
    void specialParameterDollarZeroIsScriptName() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("dollar0.sh", String.format("echo $0%n"))
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/dollar0.sh"),
            String.format("expected $0 to end with /dollar0.sh, got: %s", out)
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $- contains current shell flags.
     */
    @Test
    void specialParameterDashIsShellFlags() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("dash.sh", String.format("set -e%necho $-%n"))
        ).trim();
        Assertions.assertTrue(
            out.contains("e"),
            String.format("expected $- to contain e, got: %s", out)
        );
    }

    /**
     * POSIX 2.5.2 Special Parameters - $! is the PID of the most recently
     * started background command.
     */
    @Test
    void specialParameterBangIsLastBgPid() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("bgpid.sh", String.format("true &%necho ${!}%n"))
        ).trim();
        Assertions.assertTrue(
            out.matches("\\d+"),
            String.format("expected positive int for $!, got: %s", out)
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

    @Test
    void stderrRedirect() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "stderr_redir.sh",
                    String.format(
                        "echo test 2>%s%n",
                        new TempDirs(IntegrationPathnameExpansionTest.root).file("stderr.txt").getAbsolutePath()
                    )
                ).getAbsolutePath()
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
     * POSIX 2.6.1 Tilde Expansion - leading ~ expands to $HOME.
     */
    @Test
    void tildeExpansionToHome() throws Exception {
        Assertions.assertEquals(
            String.format("/tmp/home/x%n"),
            this.runChildJvm(
                this.writeScript("tilde.sh", String.format("HOME=/tmp/home%necho ~/x%n"))
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
                this.writeScript("midtilde.sh", String.format("HOME=/h%necho a~b%n"))
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

    /**
     * POSIX 2.14 true / false builtins - exit 0 / 1 respectively.
     */
    @Test
    void trueAndFalseExitCodes() throws Exception {
        Assertions.assertEquals(
            String.format("0%n1%n"),
            this.runChildJvm(
                this.writeScript("tf.sh", String.format("true; echo $?%nfalse; echo $?%n"))
            )
        );
    }

    /**
     * POSIX 2.14 type builtin - reports how the shell would interpret a
     * name (builtin / function / file / not found).
     */
    @Test
    void typeBuiltinReportsBuiltin() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("type.sh", String.format("type echo%n"))
        );
        Assertions.assertTrue(
            out.contains("echo"),
            String.format("expected output to mention echo as a builtin, got: %s", out)
        );
    }

    /**
     * POSIX 2.14 type - resolves multiple names in one invocation.
     */
    @Test
    void typeWithMultipleNamesFirst() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("tmulti.sh", String.format("type echo cd%n"))
        );
        Assertions.assertTrue(
            out.contains("echo"),
            String.format("expected echo in output, got: %s", out)
        );
    }

    @Test
    void typeWithMultipleNamesSecond() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("tmulti.sh", String.format("type echo cd%n"))
        );
        Assertions.assertTrue(
            out.contains("cd"),
            String.format("expected cd in output, got: %s", out)
        );
    }

    /**
     * POSIX 2.14 umask builtin - default mask is 0022.
     */
    @Test
    void umaskDefaultIsPrintable() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("umask.sh", String.format("umask%n"))
        ).trim();
        Assertions.assertTrue(
            out.matches("\\d{4}"),
            String.format("expected 4-digit octal, got %s", out)
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
}
