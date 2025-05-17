/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IntegrationTest {
    @TempDir
    Path tempDir;

    private File writeScript(String name, String content) throws IOException {
        final File script = this.tempDir.resolve(name).toFile();
        try (FileWriter w = new FileWriter(script)) {
            w.write(content);
        }
        assertTrue(script.setExecutable(true));
        return script;
    }

    @Test
    void echoHello() throws IOException {
        final File s = this.writeScript("echo.sh", "echo hello\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void trueCommand() throws IOException {
        final File s = this.writeScript("true.sh", "true\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void falseCommand() throws IOException {
        final File s = this.writeScript("false.sh", "false\n");
        assertEquals(1, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void pwdCommand() throws IOException {
        final File s = this.writeScript("pwd.sh", "pwd\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void exit42() throws IOException {
        final File s = this.writeScript("exit42.sh", "exit 42\n");
        assertEquals(42, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void pipelineEchoCat() throws IOException {
        final File s = this.writeScript("pipe.sh", "echo hello | cat\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @org.junit.jupiter.api.Test
    void childJvmCatFileProducesVisibleStdout() throws Exception {
        final File probe = this.tempDir.resolve("probe.txt").toFile();
        try (FileWriter w = new FileWriter(probe)) { w.write("hello-from-cat\n"); }
        final File script = this.writeScript("catprobe.sh", "cat " + probe.getAbsolutePath() + "\n");
        final ProcessBuilder pb = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp",
                System.getProperty("java.class.path"),
                "io.github.andreoss.sh4j.Main", script.getAbsolutePath());
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), "sh4j exit code; output: " + out);
        assertTrue(out.contains("hello-from-cat"),
                "external cat stdout must reach test; got: '" + out + "'");
    }

    @org.junit.jupiter.api.Test
    void nativeBinaryCatProbeSmoke() throws Exception {
        final File nativeBin = new File("target/sh4j");
        org.junit.jupiter.api.Assumptions.assumeTrue(nativeBin.isFile() && nativeBin.canExecute(),
                "target/sh4j native binary not built; skipping native smoke");
        final File probe = this.tempDir.resolve("nprobe.txt").toFile();
        try (FileWriter w = new FileWriter(probe)) { w.write("hello-native\n"); }
        final File script = this.writeScript("ncatprobe.sh", "cat " + probe.getAbsolutePath() + "\n");
        final ProcessBuilder pb = new ProcessBuilder(nativeBin.getAbsolutePath(),
                script.getAbsolutePath());
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), "native sh4j exit code; output: " + out);
        assertTrue(out.contains("hello-native"),
                "native sh4j must propagate external cat stdout; got: '" + out + "'");
    }

    private String runChildJvm(File script) throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp",
                System.getProperty("java.class.path"),
                "io.github.andreoss.sh4j.Main", script.getAbsolutePath());
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), "child sh4j exit code; output: " + out);
        return out;
    }

    @org.junit.jupiter.api.Test
    void childJvmEchoPipeCat() throws Exception {
        final File s = this.writeScript("h_pipe.sh", "echo child-pipe | cat\n");
        final String out = this.runChildJvm(s);
        assertTrue(out.contains("child-pipe"), "pipeline stdout missing; got: '" + out + "'");
    }

    @org.junit.jupiter.api.Test
    void childJvmCatProbe() throws Exception {
        final File probe = this.tempDir.resolve("h_probe.txt").toFile();
        try (FileWriter w = new FileWriter(probe)) { w.write("child-probe\n"); }
        final File s = this.writeScript("h_cat.sh", "cat " + probe.getAbsolutePath() + "\n");
        final String out = this.runChildJvm(s);
        assertTrue(out.contains("child-probe"), "cat stdout missing; got: '" + out + "'");
    }

    @org.junit.jupiter.api.Test
    void childJvmForLoop() throws Exception {
        final File s = this.writeScript("h_for.sh", "for i in 1 2 3; do echo $i; done\n");
        final String out = this.runChildJvm(s);
        assertTrue(out.contains("1") && out.contains("2") && out.contains("3"),
                "for-loop body missing; got: '" + out + "'");
    }

    @org.junit.jupiter.api.Test
    void nativeBinaryPipelineSmoke() throws Exception {
        final File nativeBin = new File("target/sh4j");
        org.junit.jupiter.api.Assumptions.assumeTrue(nativeBin.isFile() && nativeBin.canExecute(),
                "target/sh4j native binary not built; skipping native pipeline smoke");
        final ProcessBuilder pb = new ProcessBuilder(nativeBin.getAbsolutePath(),
                this.writeScript("npipe.sh", "echo native-pipe | cat\n").getAbsolutePath());
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), "native sh4j pipeline exit code; output: " + out);
        assertTrue(out.contains("native-pipe"),
                "native sh4j pipeline must propagate stdout; got: '" + out + "'");
    }

    @Test
    void pipelineMultiple() throws IOException {
        final File s = this.writeScript("pipe_multi.sh", "echo hello | cat | wc -c\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void andIfBothSucceed() throws IOException {
        final File s = this.writeScript("and.sh", "true && echo ok\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void andIfFirstFails() throws IOException {
        final File s = this.writeScript("and_fail.sh", "false && echo not_reached\n");
        assertEquals(1, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void orIfFirstFails() throws IOException {
        final File s = this.writeScript("or.sh", "false || echo ok\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void orIfFirstSucceeds() throws IOException {
        final File s = this.writeScript("or_succ.sh", "true || echo not_reached\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void bangInvertsExit() throws IOException {
        final File s = this.writeScript("bang.sh", "! false\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void bangInvertsTrueToFalse() throws IOException {
        final File s = this.writeScript("bang_true.sh", "! true\n");
        assertEquals(1, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void sequentialSemicolon() throws IOException {
        final File s = this.writeScript("semi.sh", "echo a; echo b\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void backgroundAmpersand() throws IOException {
        final File s = this.writeScript("bg.sh", "sleep 0 &\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void assignmentThenCommand() throws IOException {
        final File s = this.writeScript("assign.sh", "FOO=hello echo $FOO\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void assignmentOnlyPersists() throws IOException {
        final File s = this.writeScript("assign_only.sh", "FOO=bar\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void multipleAssignments() throws IOException {
        final File s = this.writeScript("multi_assign.sh", "A=1 B=2\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void outputRedirection() throws IOException {
        final File outFile = this.tempDir.resolve("out.txt").toFile();
        final File s = this.writeScript("redir_out.sh", "echo test > " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists());
        assertEquals("test", Files.readString(outFile.toPath()).trim());
    }

    @Test
    void inputRedirection() throws IOException {
        final File inFile = this.tempDir.resolve("in.txt").toFile();
        Files.writeString(inFile.toPath(), "input data\n");
        final File s = this.writeScript("redir_in.sh", "cat < " + inFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void appendRedirection() throws IOException {
        final File outFile = this.tempDir.resolve("append.txt").toFile();
        final File s = this.writeScript("append.sh", "echo line2 >> " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists(), "append.txt should exist");
        assertEquals("line2", Files.readString(outFile.toPath()).trim());
    }

    @Test
    void clobberRedirection() throws IOException {
        final File outFile = this.tempDir.resolve("clobber.txt").toFile();
        final File s = this.writeScript("clobber.sh", "echo test >| " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists());
    }

    @Test
    void stderrRedirect() throws IOException {
        final File s = this.writeScript("stderr_redir.sh", "echo test 2>" + this.tempDir.resolve("stderr.txt").toFile().getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void variableExpansionWithRedirect() throws IOException {
        final File outFile = this.tempDir.resolve("out2.txt").toFile();
        final File s = this.writeScript("varexp_redir.sh", "echo $HOME > " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists(), "out2.txt should exist");
        assertFalse(Files.readString(outFile.toPath()).trim().isEmpty(), "content should not be empty");
    }

    @Test
    void complexScript() throws IOException {
        final File s = this.writeScript("complex.sh",
            "MSG=hello\n"
            + "echo $MSG\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void variableExpansion() throws IOException {
        assertEquals(0, Main.runScript(this.writeScript("varexp.sh", "echo $SHELL\n").getAbsolutePath()));
    }

    @Test
    void commandNotFound() throws IOException {
        assertEquals(127, Main.runScript(this.writeScript("notfound.sh", "nonexistent_cmd_xyz\n").getAbsolutePath()));
    }
}
