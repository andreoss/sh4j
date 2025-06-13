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
import org.junit.jupiter.api.Assertions;

/**
 * Tests for Posix.
 * @since 1.0
 */
final class PosixTests {

    /** Field. */
    @TempDir
    Path tempdir;

    @Test
    void trueExitZero() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("true%n")));
    }

    @Test
    void falseExitOne() throws IOException {
        Assertions.assertEquals(1, this.run(String.format("false%n")));
    }

    @Test
    void exitFortyTwo() throws IOException {
        Assertions.assertEquals(42, this.run(String.format("exit 42%n")));
    }

    @Test
    void exitZero() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("exit 0%n")));
    }

    @Test
    void exitOne() throws IOException {
        Assertions.assertEquals(1, this.run(String.format("exit 1%n")));
    }

    @Test
    void echoHello() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("echo hello%n")));
    }

    @Test
    void echoArgs() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("echo a b c%n")));
    }

    @Test
    void pipeline() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("echo hello | cat%n")));
    }

    @Test
    void pipelineMulti() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("echo hello | cat | wc -c%n")));
    }

    @Test
    void andIf() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("true && echo ok%n")));
    }

    @Test
    void andIfStop() throws IOException {
        Assertions.assertEquals(1, this.run(String.format("false && echo nope%n")));
    }

    @Test
    void orIf() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("false || echo ok%n")));
    }

    @Test
    void orIfStop() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("true || echo nope%n")));
    }

    @Test
    void bangFalse() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("! false%n")));
    }

    @Test
    void bangTrue() throws IOException {
        Assertions.assertEquals(1, this.run(String.format("! true%n")));
    }

    @Test
    void semicolon() throws IOException {
        Assertions.assertEquals(1, this.run(String.format("true; false%n")));
    }

    @Test
    void background() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("sleep 0 &%n")));
    }

    @Test
    void assignThenCmd() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("MSG=hello echo $MSG%n")));
    }

    @Test
    void assignOnly() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("MSG=hello%necho $MSG%n")));
    }

    @Test
    void multiAssign() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("A=1 B=2%n")));
    }

    @Test
    void varExpand() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("echo $HOME%n")));
    }

    @Test
    void cmdNotFound() throws IOException {
        Assertions.assertEquals(127, this.run(String.format("nonexistent_cmd_xyz%n")));
    }

    @Test
    void pwd() throws IOException {
        Assertions.assertEquals(0, this.run(String.format("pwd%n")));
    }

    private int run(final String script) throws IOException {
        final File file = this.tempdir.resolve("t.sh").toFile();
        Files.writeString(file.toPath(), script);
        return Main.runScript(file.getAbsolutePath(), List.of(file.getAbsolutePath()));
    }

}
