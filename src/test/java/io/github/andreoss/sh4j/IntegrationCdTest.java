/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
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
 * Tests for {@link Main}: cd.
 *
 * @since 1.0
 */
final class IntegrationCdTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    /**
     * POSIX 2.14 cd builtin - changes the working directory.
     */
    @Test
    void cdChangesWorkingDirectoryFirst() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "cd.sh",
                String.format(
                    "cd %s%npwd%n",
                    new TempDirs(IntegrationCdTest.root).folder("cd-target").getAbsolutePath()
                )
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/cd-target"),
            String.format("expected to land in /cd-target, got %s", out)
        );
    }

    @Test
    void cdChangesWorkingDirectorySecond() throws Exception {
        final File target = new TempDirs(IntegrationCdTest.root).folder("cd-target");
        Assertions.assertTrue(
            this.runChildJvm(
                this.writeScript("cd.sh", String.format("cd %s%npwd%n", target.getAbsolutePath()))
            ).trim().contains(target.getName()),
            String.format("expected pwd to mention %s", target.getName())
        );
    }

    /**
     * POSIX 2.14 cd builtin - {@code cd -} jumps back to the previous
     * directory recorded in OLDPWD.
     */
    @Test
    void cdDashRevisitsPreviousDirectoryFirst() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript(
                "cdback.sh",
                String.format(
                    "cd %s%ncd %s%ncd - >/dev/null%npwd%n",
                    new TempDirs(IntegrationCdTest.root).folder("first").getAbsolutePath(),
                    new TempDirs(IntegrationCdTest.root).folder("second").getAbsolutePath()
                )
            )
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/first"),
            String.format("expected to return to /first, got %s", out)
        );
    }

    @Test
    void cdDashRevisitsPreviousDirectorySecond() throws Exception {
        final File first = new TempDirs(IntegrationCdTest.root).folder("first");
        final File second = new TempDirs(IntegrationCdTest.root).folder("second");
        Assertions.assertNotEquals(
            second.getAbsolutePath(),
            this.runChildJvm(
                this.writeScript(
                    "cdback.sh",
                    String.format(
                        "cd %s%ncd %s%ncd - >/dev/null%npwd%n",
                        first.getAbsolutePath(),
                        second.getAbsolutePath()
                    )
                )
            ).trim(),
            "cd - must not land on the directory just left"
        );
    }

    @Test
    void childJvmCatFileProducesVisibleStdoutFirst() throws Exception {
        final File probe = new TempDirs(IntegrationCdTest.root).file("probe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("hello-from-cat%n"));
        }
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-XX:-UsePerfData",
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
    }

    @Test
    void childJvmCatFileProducesVisibleStdoutSecond() throws Exception {
        final File probe = new TempDirs(IntegrationCdTest.root).file("probe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("hello-from-cat%n"));
        }
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-XX:-UsePerfData",
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
        proc.exitValue();
        Assertions.assertTrue(
            out.contains("hello-from-cat"),
            String.format("external cat stdout must reach test; got: '%s'", out)
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
        final File probe = new TempDirs(IntegrationCdTest.root).file("h_probe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("child-probe%n"));
        }
        final String out = this.runChildJvm(
            this.writeScript("h_cat.sh", String.format("cat %s%n", probe.getAbsolutePath()))
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
    void clobberRedirectionFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "clobber.sh",
                    String.format(
                        "echo test >| %s",
                        new TempDirs(IntegrationCdTest.root).file("clobber.txt").getAbsolutePath()
                    )
                    + String.format("%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void clobberRedirectionSecond() throws IOException {
        final File outfile = new TempDirs(IntegrationCdTest.root).file("clobber.txt");
        Main.runScript(
            this.writeScript(
                "clobber.sh",
                String.format("echo test >| %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertTrue(outfile.exists());
    }

    /**
     * POSIX 2.7.2 Redirecting Output - `>|` overrides noclobber.
     */
    @Test
    void clobberRedirectWritesFile() throws Exception {
        final File target = new TempDirs(IntegrationCdTest.root).file("clob.dat");
        Files.writeString(target.toPath(), String.format("old%n"));
        Assertions.assertEquals(
            String.format("new%n"),
            this.runChildJvm(
                this.writeScript(
                    "clob.sh",
                    String.format(
                        "echo new >|%s%ncat %s%n",
                        target.getAbsolutePath(),
                        target.getAbsolutePath()
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
                this.writeScript("close.sh", String.format("cat <<EOF%nin%nEOF%necho post%n"))
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
            this.runChildJvm(this.writeScript("colon.sh", String.format(":%necho $?%n")))
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
     * POSIX 2.6.3 Command Substitution - $(...) replaced by its stdout.
     */
    @Test
    void commandSubstitutionDollarParen() throws Exception {
        Assertions.assertEquals(
            String.format("nested%n"),
            this.runChildJvm(this.writeScript("cs.sh", String.format("echo $(echo nested)%n")))
        );
    }

    /**
     * POSIX 2.14 command -v - prints the command location / kind.
     */
    @Test
    void commandDashVReportsBuiltin() throws Exception {
        Assertions.assertEquals(
            "cd",
            this.runChildJvm(this.writeScript("cmdv.sh", String.format("command -v cd%n"))).trim()
        );
    }

    @Test
    void complexScript() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "complex.sh",
                    String.format("MSG=hello%necho $MSG%n")
                ).getAbsolutePath()
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
            this.writeScript("fn0.sh", String.format("f() { echo $0; }%nf%n"))
        ).trim();
        Assertions.assertTrue(
            out.endsWith("/fn0.sh"),
            String.format("expected $0 to end with /fn0.sh, got: %s", out)
        );
    }

    /**
     * POSIX 2.2.3 Double-Quotes - variables expand, single quotes are literal.
     */
    @Test
    void doubleQuotesAllowVariableExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("X=val%n"),
            this.runChildJvm(this.writeScript("dq.sh", String.format("X=val%necho \"X=$X\"%n")))
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

    @Test
    void echoHello() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("echo.sh", String.format("echo hello%n")).getAbsolutePath()
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
            this.runChildJvm(this.writeScript("ecs.sh", String.format("echo a$()b%n")))
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
                this.writeScript("eval.sh", String.format("CMD='echo dynamic'%neval $CMD%n"))
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

    @Test
    void exitFortyTwo() throws IOException {
        Assertions.assertEquals(
            42,
            Main.runScript(
                this.writeScript("exitFortyTwo.sh", String.format("exit 42%n")).getAbsolutePath()
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
            this.runChildJvm(this.writeScript("dollarq.sh", String.format("(exit 7)%necho $?%n")))
        );
    }

    /**
     * POSIX 2.8.2 Exit Status - {@code true; echo $?} reports 0.
     */
    @Test
    void exitStatusTrueIsZero() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(this.writeScript("trueq.sh", String.format("true%necho $?%n")))
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
                this.writeScript("export.sh", String.format("X=outer%nexport X%n(echo $X)%n"))
            )
        );
    }

    @Test
    void falseCommand() throws IOException {
        Assertions.assertEquals(
            1,
            Main.runScript(
                this.writeScript("false.sh", String.format("false%n")).getAbsolutePath()
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
                    "fs.sh",
                    String.format("IFS=:%nX=a:b:c%nfor i in $X; do echo $i; done%n")
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
     * POSIX 2.9.5 Function Definition Command - functions persist and bind
     * positional parameters.
     */
    @Test
    void functionDefinitionAndCall() throws Exception {
        Assertions.assertEquals(
            String.format("hi world%n"),
            this.runChildJvm(
                this.writeScript("fn.sh", String.format("greet() { echo hi $1; }%ngreet world%n"))
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
                        "echo() { %s wrapped: $*; }%necho hi%n",
                        "/usr/bin/env echo"
                    )
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
                this.writeScript("ret.sh", String.format("f() { return 42; }%nf%necho $?%n"))
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
                        String.join(
                            "",
                            "set -- -a foo%n",
                            "while getopts a: opt; do echo $opt:$OPTARG; done%n"
                        )
                    )
                )
            )
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
            this.runChildJvm(this.writeScript("hashr.sh", String.format("hash -r%necho $?%n")))
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
                this.writeScript("comment.sh", String.format("# this is a comment%necho after%n"))
            )
        );
    }

    private File writeScript(final String name, final String content) throws IOException {
        final File script = new TempDirs(IntegrationCdTest.root).file(name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assertions.assertTrue(script.setExecutable(true));
        return script;
    }

    private String runChildJvm(final File script) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-XX:-UsePerfData",
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
        return input.readAllBytes();
    }
}
