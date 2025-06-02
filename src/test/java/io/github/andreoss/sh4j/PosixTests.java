/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class PosixTests {

    @TempDir
    Path tempDir;

    @Test
    void trueExitZero() throws IOException {
        assertEquals(0, this.run("true\n"));
    }

    @Test
    void falseExitOne() throws IOException {
        assertEquals(1, this.run("false\n"));
    }

    @Test
    void exitFortyTwo() throws IOException {
        assertEquals(42, this.run("exit 42\n"));
    }

    @Test
    void exitZero() throws IOException {
        assertEquals(0, this.run("exit 0\n"));
    }

    @Test
    void exitOne() throws IOException {
        assertEquals(1, this.run("exit 1\n"));
    }

    @Test
    void echoHello() throws IOException {
        assertEquals(0, this.run("echo hello\n"));
    }

    @Test
    void echoArgs() throws IOException {
        assertEquals(0, this.run("echo a b c\n"));
    }

    @Test
    void pipeline() throws IOException {
        assertEquals(0, this.run("echo hello | cat\n"));
    }

    @Test
    void pipelineMulti() throws IOException {
        assertEquals(0, this.run("echo hello | cat | wc -c\n"));
    }

    @Test
    void andIf() throws IOException {
        assertEquals(0, this.run("true && echo ok\n"));
    }

    @Test
    void andIfStop() throws IOException {
        assertEquals(1, this.run("false && echo nope\n"));
    }

    @Test
    void orIf() throws IOException {
        assertEquals(0, this.run("false || echo ok\n"));
    }

    @Test
    void orIfStop() throws IOException {
        assertEquals(0, this.run("true || echo nope\n"));
    }

    @Test
    void bangFalse() throws IOException {
        assertEquals(0, this.run("! false\n"));
    }

    @Test
    void bangTrue() throws IOException {
        assertEquals(1, this.run("! true\n"));
    }

    @Test
    void semicolon() throws IOException {
        assertEquals(1, this.run("true; false\n"));
    }

    @Test
    void background() throws IOException {
        assertEquals(0, this.run("sleep 0 &\n"));
    }

    @Test
    void assignThenCmd() throws IOException {
        assertEquals(0, this.run("MSG=hello echo $MSG\n"));
    }

    @Test
    void assignOnly() throws IOException {
        assertEquals(0, this.run("MSG=hello\necho $MSG\n"));
    }

    @Test
    void multiAssign() throws IOException {
        assertEquals(0, this.run("A=1 B=2\n"));
    }

    @Test
    void varExpand() throws IOException {
        assertEquals(0, this.run("echo $HOME\n"));
    }

    @Test
    void cmdNotFound() throws IOException {
        assertEquals(127, this.run("nonexistent_cmd_xyz\n"));
    }

    @Test
    void pwd() throws IOException {
        assertEquals(0, this.run("pwd\n"));
    }

    private int run(final String script) throws IOException {
        final File file = this.tempDir.resolve("t.sh").toFile();
        Files.writeString(file.toPath(), script);
        return Main.runScript(file.getAbsolutePath(), List.of(file.getAbsolutePath()));
    }

}
