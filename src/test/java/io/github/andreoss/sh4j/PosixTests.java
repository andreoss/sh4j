/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for Posix.
 *
 * @since 1.0
 */
final class PosixTests {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void trueExitZero() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("true%n")), CoreMatchers.is(0));
    }

    @Test
    void falseExitOne() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("false%n")), CoreMatchers.is(1));
    }

    @Test
    void exitFortyTwo() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("exit 42%n")), CoreMatchers.is(42));
    }

    @Test
    void exitZero() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("exit 0%n")), CoreMatchers.is(0));
    }

    @Test
    void exitOne() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("exit 1%n")), CoreMatchers.is(1));
    }

    @Test
    void echoHello() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("echo hello%n")), CoreMatchers.is(0));
    }

    @Test
    void echoArgs() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("echo a b c%n")), CoreMatchers.is(0));
    }

    @Test
    void pipeline() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("echo hello | cat%n")), CoreMatchers.is(0));
    }

    @Test
    void pipelineMulti() throws IOException {
        MatcherAssert.assertThat(
            this.run(String.format("echo hello | cat | wc -c%n")),
            CoreMatchers.is(0)
        );
    }

    @Test
    void andIf() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("true && echo ok%n")), CoreMatchers.is(0));
    }

    @Test
    void andIfStop() throws IOException {
        MatcherAssert.assertThat(
            this.run(String.format("false && echo nope%n")),
            CoreMatchers.is(1)
        );
    }

    @Test
    void orIf() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("false || echo ok%n")), CoreMatchers.is(0));
    }

    @Test
    void orIfStop() throws IOException {
        MatcherAssert.assertThat(
            this.run(String.format("true || echo nope%n")),
            CoreMatchers.is(0)
        );
    }

    @Test
    void bangFalse() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("! false%n")), CoreMatchers.is(0));
    }

    @Test
    void bangTrue() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("! true%n")), CoreMatchers.is(1));
    }

    @Test
    void semicolon() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("true; false%n")), CoreMatchers.is(1));
    }

    @Test
    void background() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("sleep 0 &%n")), CoreMatchers.is(0));
    }

    @Test
    void assignThenCmd() throws IOException {
        MatcherAssert.assertThat(
            this.run(String.format("MSG=hello echo $MSG%n")),
            CoreMatchers.is(0)
        );
    }

    @Test
    void assignOnly() throws IOException {
        MatcherAssert.assertThat(
            this.run(String.format("MSG=hello%necho $MSG%n")),
            CoreMatchers.is(0)
        );
    }

    @Test
    void multiAssign() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("A=1 B=2%n")), CoreMatchers.is(0));
    }

    @Test
    void varExpand() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("echo $HOME%n")), CoreMatchers.is(0));
    }

    @Test
    void cmdNotFound() throws IOException {
        MatcherAssert.assertThat(
            this.run(String.format("nonexistent_cmd_xyz%n")),
            CoreMatchers.is(127)
        );
    }

    @Test
    void pwd() throws IOException {
        MatcherAssert.assertThat(this.run(String.format("pwd%n")), CoreMatchers.is(0));
    }

    private int run(final String script) throws IOException {
        final File file = TempDirs.file(PosixTests.root, "t.sh");
        Files.writeString(file.toPath(), script);
        return Main.runScript(
            file.getAbsolutePath(),
            Collections.singletonList(file.getAbsolutePath())
        );
    }
}
