/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PosixTests {
    @TempDir
    Path tempDir;

    private int run(String script) throws IOException {
        final File f = tempDir.resolve("t.sh").toFile();
        Files.writeString(f.toPath(), script);
        return Main.runScript(f.getAbsolutePath(), List.of(f.getAbsolutePath()));
    }

    @Test void trueExitZero() throws IOException { assertEquals(0, run("true\n")); }
    @Test void falseExitOne() throws IOException { assertEquals(1, run("false\n")); }
    @Test void exit42() throws IOException { assertEquals(42, run("exit 42\n")); }
    @Test void exitZero() throws IOException { assertEquals(0, run("exit 0\n")); }
    @Test void exitOne() throws IOException { assertEquals(1, run("exit 1\n")); }
    @Test void echoHello() throws IOException { assertEquals(0, run("echo hello\n")); }
    @Test void echoArgs() throws IOException { assertEquals(0, run("echo a b c\n")); }
    @Test void pipeline() throws IOException { assertEquals(0, run("echo hello | cat\n")); }
    @Test void pipelineMulti() throws IOException { assertEquals(0, run("echo hello | cat | wc -c\n")); }
    @Test void andIf() throws IOException { assertEquals(0, run("true && echo ok\n")); }
    @Test void andIfStop() throws IOException { assertEquals(1, run("false && echo nope\n")); }
    @Test void orIf() throws IOException { assertEquals(0, run("false || echo ok\n")); }
    @Test void orIfStop() throws IOException { assertEquals(0, run("true || echo nope\n")); }
    @Test void bangFalse() throws IOException { assertEquals(0, run("! false\n")); }
    @Test void bangTrue() throws IOException { assertEquals(1, run("! true\n")); }
    @Test void semicolon() throws IOException { assertEquals(1, run("true; false\n")); }
    @Test void background() throws IOException { assertEquals(0, run("sleep 0 &\n")); }
    @Test void assignThenCmd() throws IOException { assertEquals(0, run("MSG=hello echo $MSG\n")); }
    @Test void assignOnly() throws IOException { assertEquals(0, run("MSG=hello\necho $MSG\n")); }
    @Test void multiAssign() throws IOException { assertEquals(0, run("A=1 B=2\n")); }
    @Test void varExpand() throws IOException { assertEquals(0, run("echo $HOME\n")); }
    @Test void cmdNotFound() throws IOException { assertEquals(127, run("nonexistent_cmd_xyz\n")); }
    @Test void pwd() throws IOException { assertEquals(0, run("pwd\n")); }
}
