/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for Integration.
 * @since 1.0
 */
final class IntegrationTest {

    /** Field. */
    @TempDir
    Path tempdir;

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

    @org.junit.jupiter.api.Test
    void childJvmCatFileProducesVisibleStdout() throws Exception {
        final File probe = this.tempdir.resolve("probe.txt").toFile();
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
        final String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Assertions.assertEquals(
            0,
            proc.waitFor(),
            String.format("sh4j exit code; output: %s", out)
        );
        Assertions.assertTrue(
            out.contains("hello-from-cat"),
            String.format("external cat stdout must reach test; got: '%s'", out)
        );
    }

    @org.junit.jupiter.api.Test
    void nativeBinaryCatProbeSmoke() throws Exception {
        final File nativebin = new File("target/sh4j");
        org.junit.jupiter.api.Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native smoke"
        );
        final File probe = this.tempdir.resolve("nprobe.txt").toFile();
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
        final String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Assertions.assertEquals(
            0,
            proc.waitFor(),
            String.format("native sh4j exit code; output: %s", out)
        );
        Assertions.assertTrue(
            out.contains("hello-native"),
            String.format("native sh4j must propagate external cat stdout; got: '%s'", out)
        );
    }

    @org.junit.jupiter.api.Test
    void childJvmEchoPipeCat() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("h_pipe.sh", String.format("echo child-pipe | cat%n"))
        );
        Assertions.assertTrue(
            out.contains("child-pipe"),
            String.format("pipeline stdout missing; got: '%s'", out)
        );
    }

    @org.junit.jupiter.api.Test
    void childJvmCatProbe() throws Exception {
        final File probe = this.tempdir.resolve("h_probe.txt").toFile();
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

    @org.junit.jupiter.api.Test
    void childJvmForLoop() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("h_for.sh", String.format("for i in 1 2 3; do echo $i; done%n"))
        );
        Assertions.assertTrue(
            out.contains("1") && out.contains("2") && out.contains("3"),
            String.format("for-loop body missing; got: '%s'", out)
        );
    }

    @org.junit.jupiter.api.Test
    void nativeBinaryPipelineSmoke() throws Exception {
        final File nativebin = new File("target/sh4j");
        org.junit.jupiter.api.Assumptions.assumeTrue(
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
        final String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Assertions.assertEquals(
            0,
            proc.waitFor(),
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
        final File outfile = this.tempdir.resolve("out.txt").toFile();
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
        Assertions.assertEquals("test", Files.readString(outfile.toPath()).trim());
    }

    @Test
    void inputRedirection() throws IOException {
        final File infile = this.tempdir.resolve("in.txt").toFile();
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
        final File outfile = this.tempdir.resolve("append.txt").toFile();
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
        Assertions.assertEquals("line2", Files.readString(outfile.toPath()).trim());
    }

    @Test
    void clobberRedirection() throws IOException {
        final File outfile = this.tempdir.resolve("clobber.txt").toFile();
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
                        this.tempdir.resolve("stderr.txt").toFile().getAbsolutePath()
                    )
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void variableExpansionWithRedirect() throws IOException {
        final File outfile = this.tempdir.resolve("out2.txt").toFile();
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
            Files.readString(outfile.toPath()).isBlank(),
            "content should not be empty"
        );
    }

    @Test
    void complexScript() throws IOException {
        final File str = this.writeScript(
            "complex.sh",
            String.format("MSG=hello%necho $MSG%n")
        );
        Assertions.assertEquals(0, Main.runScript(str.getAbsolutePath()));
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

    private File writeScript(final String name, final String content) throws IOException {
        final File script = this.tempdir.resolve(name).toFile();
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assertions.assertTrue(script.setExecutable(true));
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
        final String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Assertions.assertEquals(
            0,
            proc.waitFor(),
            String.format("child sh4j exit code; output: %s", out)
        );
        return out;
    }
}
