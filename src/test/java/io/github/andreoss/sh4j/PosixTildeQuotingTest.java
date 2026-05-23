/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: the result of tilde expansion is treated as if
 * quoted (POSIX 2.6.1).
 *
 * @since 1.0
 */
final class PosixTildeQuotingTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void removesPrefixWithLiteralStarOfHome() {
        Assertions.assertEquals(
            String.format("/a%n"),
            Sh4j.INSTANCE.out("HOME='/x*y'; x='/x*y/a'; echo \"${x#~}\"")
        );
    }

    @Test
    void doesNotMatchStarOfHomeAsPattern() {
        Assertions.assertEquals(
            String.format("/xQQy/a%n"),
            Sh4j.INSTANCE.out("HOME='/x*y'; x=/xQQy/a; echo \"${x#~}\"")
        );
    }

    @Test
    void caseTildePatternIsLiteral() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("HOME='/h*'; case /hZ in ~) echo bad;; *) echo ok;; esac")
        );
    }

    @Test
    void caseTildePatternMatchesHomeItself() {
        Assertions.assertEquals(
            String.format("lit%n"),
            Sh4j.INSTANCE.out("HOME='/h*'; case '/h*' in ~) echo lit;; esac")
        );
    }

    @Test
    void doesNotGlobHome() throws IOException {
        final File dir = new TempDirs(PosixTildeQuotingTest.root).folder("glob");
        Files.createDirectory(dir.toPath().resolve("h[a]"));
        Files.createDirectory(dir.toPath().resolve("ha"));
        final String home = new File(dir, "h[a]").getAbsolutePath();
        Assertions.assertEquals(
            String.format("%s%n", home),
            Sh4j.INSTANCE.out(String.format("HOME='%s'; echo ~", home))
        );
    }

    @Test
    void doesNotSplitHome() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("HOME='/a b'; set -- ~; echo \"$#\"")
        );
    }

    @Test
    void keepsQuoteCharacterOfHome() {
        Assertions.assertEquals(
            String.format("/it's/z%n"),
            Sh4j.INSTANCE.out("HOME=\"/it's\"; echo ~/z")
        );
    }

    @Test
    void keepsBackslashOfHome() {
        Assertions.assertEquals(
            String.format("/b\\x%n"),
            Sh4j.INSTANCE.out("HOME='/b\\x'; x=~; printf '%s\\n' \"$x\"")
        );
    }

    @Test
    void assignsTildePrefix() {
        Assertions.assertEquals(
            String.format("/q/z%n"),
            Sh4j.INSTANCE.out("HOME='/q'; x=~/z; echo \"$x\"")
        );
    }

    @Test
    void expandsTildePrefixOfArgument() {
        Assertions.assertEquals(
            String.format("/q/z%n"),
            Sh4j.INSTANCE.out("HOME='/q'; echo ~/z")
        );
    }
}
