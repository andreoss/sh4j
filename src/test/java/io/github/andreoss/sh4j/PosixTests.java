/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for Posix.
 * @since 1.0
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class PosixTests {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void trueExitZero() throws IOException {
        Assert.assertEquals(0, this.run(String.format("true%n")));
    }

    @Test
    public void falseExitOne() throws IOException {
        Assert.assertEquals(1, this.run(String.format("false%n")));
    }

    @Test
    public void exitFortyTwo() throws IOException {
        Assert.assertEquals(42, this.run(String.format("exit 42%n")));
    }

    @Test
    public void exitZero() throws IOException {
        Assert.assertEquals(0, this.run(String.format("exit 0%n")));
    }

    @Test
    public void exitOne() throws IOException {
        Assert.assertEquals(1, this.run(String.format("exit 1%n")));
    }

    @Test
    public void echoHello() throws IOException {
        Assert.assertEquals(0, this.run(String.format("echo hello%n")));
    }

    @Test
    public void echoArgs() throws IOException {
        Assert.assertEquals(0, this.run(String.format("echo a b c%n")));
    }

    @Test
    public void pipeline() throws IOException {
        Assert.assertEquals(0, this.run(String.format("echo hello | cat%n")));
    }

    @Test
    public void pipelineMulti() throws IOException {
        Assert.assertEquals(0, this.run(String.format("echo hello | cat | wc -c%n")));
    }

    @Test
    public void andIf() throws IOException {
        Assert.assertEquals(0, this.run(String.format("true && echo ok%n")));
    }

    @Test
    public void andIfStop() throws IOException {
        Assert.assertEquals(1, this.run(String.format("false && echo nope%n")));
    }

    @Test
    public void orIf() throws IOException {
        Assert.assertEquals(0, this.run(String.format("false || echo ok%n")));
    }

    @Test
    public void orIfStop() throws IOException {
        Assert.assertEquals(0, this.run(String.format("true || echo nope%n")));
    }

    @Test
    public void bangFalse() throws IOException {
        Assert.assertEquals(0, this.run(String.format("! false%n")));
    }

    @Test
    public void bangTrue() throws IOException {
        Assert.assertEquals(1, this.run(String.format("! true%n")));
    }

    @Test
    public void semicolon() throws IOException {
        Assert.assertEquals(1, this.run(String.format("true; false%n")));
    }

    @Test
    public void background() throws IOException {
        Assert.assertEquals(0, this.run(String.format("sleep 0 &%n")));
    }

    @Test
    public void assignThenCmd() throws IOException {
        Assert.assertEquals(0, this.run(String.format("MSG=hello echo $MSG%n")));
    }

    @Test
    public void assignOnly() throws IOException {
        Assert.assertEquals(0, this.run(String.format("MSG=hello%necho $MSG%n")));
    }

    @Test
    public void multiAssign() throws IOException {
        Assert.assertEquals(0, this.run(String.format("A=1 B=2%n")));
    }

    @Test
    public void varExpand() throws IOException {
        Assert.assertEquals(0, this.run(String.format("echo $HOME%n")));
    }

    @Test
    public void cmdNotFound() throws IOException {
        Assert.assertEquals(127, this.run(String.format("nonexistent_cmd_xyz%n")));
    }

    @Test
    public void pwd() throws IOException {
        Assert.assertEquals(0, this.run(String.format("pwd%n")));
    }

    private int run(final String script) throws IOException {
        final File file = this.tempdir.newFile("t.sh");
        Files.writeString(file.toPath(), script);
        return Main.runScript(file.getAbsolutePath(), Arrays.asList(file.getAbsolutePath()));
    }
}
