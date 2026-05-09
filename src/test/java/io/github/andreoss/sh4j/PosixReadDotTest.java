/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.Executor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the {@code read} exit status at end-of-file and for the
 * {@code .} special built-in error (POSIX 2.8.1, 2.14 dot, read).
 *
 * @since 1.0
 */
final class PosixReadDotTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void readPartialLineFailsButAssigns() {
        Assertions.assertEquals(
            String.format("rc=1 v=x%n"),
            Sh4j.INSTANCE.out("printf x | { read v; echo \"rc=$? v=$v\"; }")
        );
    }

    @Test
    void readEmptyInputFails() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("printf '' | { read v; echo \"$?\"; }")
        );
    }

    @Test
    void readFullLineSucceeds() {
        Assertions.assertEquals(
            String.format("rc=0 v=x%n"),
            Sh4j.INSTANCE.out("printf 'x\\n' | { read v; echo \"rc=$? v=$v\"; }")
        );
    }

    @Test
    void readEmptyLineSucceeds() {
        Assertions.assertEquals(
            String.format("rc=0 v=[]%n"),
            Sh4j.INSTANCE.out("printf '\\n' | { read v; echo \"rc=$? v=[$v]\"; }")
        );
    }

    @Test
    void readReplyPartialLineFailsButAssigns() {
        Assertions.assertEquals(
            String.format("rc=1 r=ab%n"),
            Sh4j.INSTANCE.out("printf ab | { read; echo \"rc=$? r=$REPLY\"; }")
        );
    }

    @Test
    void readLoopKeepsPartialLastLine() {
        Assertions.assertEquals(
            String.format("[a]%nlast=[b]%n"),
            Sh4j.INSTANCE.out(
                "printf 'a\\nb' | { while read v; do echo \"[$v]\"; done; echo \"last=[$v]\"; }"
            )
        );
    }

    @Test
    void dotMissingFileExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(
                String.format(
                    ". '%s' 2>/dev/null; echo after",
                    PosixReadDotTest.missing()
                )
            )
        );
    }

    @Test
    void dotMissingFileExitStatus() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run(
                String.format(
                    ". '%s' 2>/dev/null; echo after",
                    PosixReadDotTest.missing()
                )
            )
        );
    }

    @Test
    void commandDotMissingFileDoesNotExit() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "command . '%s' 2>/dev/null; echo \"after $?\"",
                    PosixReadDotTest.missing()
                )
            )
        );
    }

    @Test
    void dotMissingFileInSubshellExitsOnlySubshell() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "( . '%s' 2>/dev/null; echo inner ); echo \"after $?\"",
                    PosixReadDotTest.missing()
                )
            )
        );
    }

    @Test
    void commandExitStillExits() {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out("echo in; command exit 3; echo after")
        );
    }

    @Test
    void dotExistingFileReturnsItsStatus() throws IOException {
        final File file = new TempDirs(PosixReadDotTest.root).file("dotted.sh");
        Files.write(
            file.toPath(),
            "echo sourced; false".getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            String.format("sourced%nafter 1%n"),
            Sh4j.INSTANCE.out(
                String.format(". '%s'; echo \"after $?\"", file.getAbsolutePath())
            )
        );
    }

    @Test
    void interactiveShellSurvivesDotMissingFile() {
        final Executor executor = new Executor();
        Main.executeLine(
            executor,
            String.format(". '%s' 2>/dev/null", PosixReadDotTest.missing())
        );
        Main.executeLine(executor, "alive=yes");
        Assertions.assertEquals("yes", executor.getVariable("alive"));
    }

    private static String missing() {
        return PosixReadDotTest.root.resolve("missing-dot-qq").toString();
    }
}
