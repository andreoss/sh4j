/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(this.run(String.format("true%n")), is(0));
    }

    @Test
    public void falseExitOne() throws IOException {
        assertThat(this.run(String.format("false%n")), is(1));
    }

    @Test
    public void exitFortyTwo() throws IOException {
        assertThat(this.run(String.format("exit 42%n")), is(42));
    }

    @Test
    public void exitZero() throws IOException {
        assertThat(this.run(String.format("exit 0%n")), is(0));
    }

    @Test
    public void exitOne() throws IOException {
        assertThat(this.run(String.format("exit 1%n")), is(1));
    }

    @Test
    public void echoHello() throws IOException {
        assertThat(this.run(String.format("echo hello%n")), is(0));
    }

    @Test
    public void echoArgs() throws IOException {
        assertThat(this.run(String.format("echo a b c%n")), is(0));
    }

    @Test
    public void pipeline() throws IOException {
        assertThat(this.run(String.format("echo hello | cat%n")), is(0));
    }

    @Test
    public void pipelineMulti() throws IOException {
        assertThat(this.run(String.format("echo hello | cat | wc -c%n")), is(0));
    }

    @Test
    public void andIf() throws IOException {
        assertThat(this.run(String.format("true && echo ok%n")), is(0));
    }

    @Test
    public void andIfStop() throws IOException {
        assertThat(this.run(String.format("false && echo nope%n")), is(1));
    }

    @Test
    public void orIf() throws IOException {
        assertThat(this.run(String.format("false || echo ok%n")), is(0));
    }

    @Test
    public void orIfStop() throws IOException {
        assertThat(this.run(String.format("true || echo nope%n")), is(0));
    }

    @Test
    public void bangFalse() throws IOException {
        assertThat(this.run(String.format("! false%n")), is(0));
    }

    @Test
    public void bangTrue() throws IOException {
        assertThat(this.run(String.format("! true%n")), is(1));
    }

    @Test
    public void semicolon() throws IOException {
        assertThat(this.run(String.format("true; false%n")), is(1));
    }

    @Test
    public void background() throws IOException {
        assertThat(this.run(String.format("sleep 0 &%n")), is(0));
    }

    @Test
    public void assignThenCmd() throws IOException {
        assertThat(this.run(String.format("MSG=hello echo $MSG%n")), is(0));
    }

    @Test
    public void assignOnly() throws IOException {
        assertThat(this.run(String.format("MSG=hello%necho $MSG%n")), is(0));
    }

    @Test
    public void multiAssign() throws IOException {
        assertThat(this.run(String.format("A=1 B=2%n")), is(0));
    }

    @Test
    public void varExpand() throws IOException {
        assertThat(this.run(String.format("echo $HOME%n")), is(0));
    }

    @Test
    public void cmdNotFound() throws IOException {
        assertThat(this.run(String.format("nonexistent_cmd_xyz%n")), is(127));
    }

    @Test
    public void pwd() throws IOException {
        assertThat(this.run(String.format("pwd%n")), is(0));
    }

    private int run(final String script) throws IOException {
        final File file = this.tempdir.newFile("t.sh");
        Files.writeString(file.toPath(), script);
        return Main.runScript(file.getAbsolutePath(), Collections.singletonList(file.getAbsolutePath()));
    }
}
