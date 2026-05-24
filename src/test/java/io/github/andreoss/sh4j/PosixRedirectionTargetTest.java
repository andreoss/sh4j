/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: expansion of the word after a redirection
 * operator, with quote removal and without field splitting (POSIX 2.7).
 *
 * @since 1.0
 */
final class PosixRedirectionTargetTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void removesSingleQuotesFromOutputTarget() throws IOException {
        Assertions.assertEquals(
            String.format("hi%n"),
            PosixRedirectionTargetTest.inside("echo hi > 'x y'; cat \"x y\"")
        );
    }

    @Test
    void createsFileWithUnquotedName() throws IOException {
        final File dir = new TempDirs(PosixRedirectionTargetTest.root).folder();
        Sh4j.INSTANCE.run(
            String.format("cd '%s' && echo hi > 'x y'", dir.getAbsolutePath())
        );
        Assertions.assertTrue(new File(dir, "x y").isFile());
    }

    @Test
    void expandsParameterInsideDoubleQuotedTarget() throws IOException {
        Assertions.assertEquals(
            String.format("a%n"),
            PosixRedirectionTargetTest.inside("f=out; echo a > \"$f.txt\"; cat out.txt")
        );
    }

    @Test
    void doesNotSplitUnquotedParameterTarget() throws IOException {
        Assertions.assertEquals(
            String.format("z%n"),
            PosixRedirectionTargetTest.inside("v='a b'; echo z > $v; cat 'a b'")
        );
    }

    @Test
    void keepsQuotesProducedByExpansion() throws IOException {
        Assertions.assertEquals(
            String.format("k%n"),
            PosixRedirectionTargetTest.inside("v=\"'q'\"; echo k > $v; cat \"'q'\"")
        );
    }

    @Test
    void removesQuotesFromAppendTarget() throws IOException {
        Assertions.assertEquals(
            String.format("p%nq%n"),
            PosixRedirectionTargetTest.inside(
                "echo p > 'x y'; echo q >> 'x y'; cat 'x y'"
            )
        );
    }

    @Test
    void removesQuotesFromExternalInputTarget() throws IOException {
        Assertions.assertEquals(
            String.format("r%n"),
            PosixRedirectionTargetTest.inside("cat < 'in put'")
        );
    }

    @Test
    void removesQuotesFromBuiltinInputTarget() throws IOException {
        Assertions.assertEquals(
            String.format("r%n"),
            PosixRedirectionTargetTest.inside("read l < \"in put\"; echo \"$l\"")
        );
    }

    @Test
    void removesQuotesFromExternalOutputTarget() throws IOException {
        Assertions.assertEquals(
            String.format("r%n"),
            PosixRedirectionTargetTest.inside("cat 'in put' > 'o u'; cat 'o u'")
        );
    }

    @Test
    void removesQuotesFromExecOutputTarget() throws IOException {
        Assertions.assertEquals(
            String.format("three%n"),
            PosixRedirectionTargetTest.inside(
                "exec 3> \"p q\"; echo three >&3; exec 3>&-; cat 'p q'"
            )
        );
    }

    @Test
    void removesQuotesFromExecInputTarget() throws IOException {
        Assertions.assertEquals(
            String.format("r%n"),
            PosixRedirectionTargetTest.inside(
                "exec 4< 'in put'; read l <&4; exec 4<&-; echo \"$l\""
            )
        );
    }

    @Test
    void removesQuotesFromGroupTarget() throws IOException {
        Assertions.assertEquals(
            String.format("g%n"),
            PosixRedirectionTargetTest.inside("{ echo g; } > 'g h'; cat 'g h'")
        );
    }

    @Test
    void removesQuotesFromFunctionTarget() throws IOException {
        Assertions.assertEquals(
            String.format("f%n"),
            PosixRedirectionTargetTest.inside("fn() { echo f; }; fn > 'i j'; cat 'i j'")
        );
    }

    @Test
    void removesQuotesFromErrorTarget() throws IOException {
        Assertions.assertEquals(
            String.format("e%n"),
            PosixRedirectionTargetTest.inside("echo e 2> 'e r' >&2; cat 'e r'")
        );
    }

    @Test
    void expandsCommandSubstitutionInTarget() throws IOException {
        Assertions.assertEquals(
            String.format("c%n"),
            PosixRedirectionTargetTest.inside(
                "echo c > \"$(echo 'c d')\"; cat 'c d'"
            )
        );
    }

    @Test
    void expandsTildeBeforeQuotedSuffix() throws IOException {
        Assertions.assertEquals(
            String.format("t%n"),
            PosixRedirectionTargetTest.inside(
                "HOME=\"$PWD\"; echo t > ~/'t z'; cat 't z'"
            )
        );
    }

    @Test
    void removesBackslashFromEscapedDollar() throws IOException {
        Assertions.assertEquals(
            String.format("d%n"),
            PosixRedirectionTargetTest.inside("echo d > \\$x; cat '$x'")
        );
    }

    @Test
    void noclobberRefusesExistingFileWithUnquotedName() throws IOException {
        final File dir = new TempDirs(PosixRedirectionTargetTest.root).folder();
        Files.writeString(new File(dir, "x y").toPath(), "old", StandardCharsets.UTF_8);
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.run(
                String.format("cd '%s' && set -C && echo a > 'x y'", dir.getAbsolutePath())
            )
        );
    }

    private static String inside(final String script) throws IOException {
        final File dir = new TempDirs(PosixRedirectionTargetTest.root).folder();
        Files.writeString(
            new File(dir, "in put").toPath(), String.format("r%n"), StandardCharsets.UTF_8
        );
        return Sh4j.INSTANCE.out(
            String.format("cd '%s' && %s", dir.getAbsolutePath(), script)
        );
    }
}
