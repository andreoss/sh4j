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
        File script = tempDir.resolve(name).toFile();
        try (FileWriter w = new FileWriter(script)) {
            w.write(content);
        }
        assertTrue(script.setExecutable(true));
        return script;
    }

    @Test
    void echoHello() throws IOException {
        File s = writeScript("echo.sh", "echo hello\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void trueCommand() throws IOException {
        File s = writeScript("true.sh", "true\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void falseCommand() throws IOException {
        File s = writeScript("false.sh", "false\n");
        assertEquals(1, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void pwdCommand() throws IOException {
        File s = writeScript("pwd.sh", "pwd\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void exit42() throws IOException {
        File s = writeScript("exit42.sh", "exit 42\n");
        assertEquals(42, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void pipelineEchoCat() throws IOException {
        File s = writeScript("pipe.sh", "echo hello | cat\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void pipelineMultiple() throws IOException {
        File s = writeScript("pipe_multi.sh", "echo hello | cat | wc -c\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void andIfBothSucceed() throws IOException {
        File s = writeScript("and.sh", "true && echo ok\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void andIfFirstFails() throws IOException {
        File s = writeScript("and_fail.sh", "false && echo not_reached\n");
        assertEquals(1, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void orIfFirstFails() throws IOException {
        File s = writeScript("or.sh", "false || echo ok\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void orIfFirstSucceeds() throws IOException {
        File s = writeScript("or_succ.sh", "true || echo not_reached\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void bangInvertsExit() throws IOException {
        File s = writeScript("bang.sh", "! false\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void bangInvertsTrueToFalse() throws IOException {
        File s = writeScript("bang_true.sh", "! true\n");
        assertEquals(1, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void sequentialSemicolon() throws IOException {
        File s = writeScript("semi.sh", "echo a; echo b\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void backgroundAmpersand() throws IOException {
        File s = writeScript("bg.sh", "sleep 0 &\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void assignmentThenCommand() throws IOException {
        File s = writeScript("assign.sh", "FOO=hello echo $FOO\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void assignmentOnlyPersists() throws IOException {
        File s = writeScript("assign_only.sh", "FOO=bar\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void multipleAssignments() throws IOException {
        File s = writeScript("multi_assign.sh", "A=1 B=2\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void outputRedirection() throws IOException {
        File outFile = tempDir.resolve("out.txt").toFile();
        File s = writeScript("redir_out.sh", "echo test > " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists());
        String content = Files.readString(outFile.toPath()).trim();
        assertEquals("test", content);
    }

    @Test
    void inputRedirection() throws IOException {
        File inFile = tempDir.resolve("in.txt").toFile();
        Files.writeString(inFile.toPath(), "input data\n");
        File s = writeScript("redir_in.sh", "cat < " + inFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void appendRedirection() throws IOException {
        File outFile = tempDir.resolve("append.txt").toFile();
        String path = outFile.getAbsolutePath();
        File s = writeScript("append.sh", "echo line2 >> " + path + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists(), "append.txt should exist");
        String content = Files.readString(outFile.toPath()).trim();
        assertEquals("line2", content);
    }

    @Test
    void clobberRedirection() throws IOException {
        File outFile = tempDir.resolve("clobber.txt").toFile();
        File s = writeScript("clobber.sh", "echo test >| " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists());
    }

    @Test
    void variableExpansionWithRedirect() throws IOException {
        File outFile = tempDir.resolve("out2.txt").toFile();
        File s = writeScript("varexp_redir.sh", "echo $HOME > " + outFile.getAbsolutePath() + "\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
        assertTrue(outFile.exists(), "out2.txt should exist");
        String content = Files.readString(outFile.toPath()).trim();
        assertFalse(content.isEmpty(), "content should not be empty");
    }

    @Test
    void complexScript() throws IOException {
        File s = writeScript("complex.sh",
            "MSG=hello\n"
            + "echo $MSG\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void variableExpansion() throws IOException {
        File s = writeScript("varexp.sh", "echo $SHELL\n");
        assertEquals(0, Main.runScript(s.getAbsolutePath()));
    }

    @Test
    void commandNotFound() throws IOException {
        File s = writeScript("notfound.sh", "nonexistent_cmd_xyz\n");
        assertEquals(127, Main.runScript(s.getAbsolutePath()));
    }
}
