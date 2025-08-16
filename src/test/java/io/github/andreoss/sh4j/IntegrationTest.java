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

    private File writeScript(final String name, final String content) throws IOException {
        final File script = this.tempdir.newFile(name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assert.assertTrue(script.setExecutable(true));
        return script;
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
