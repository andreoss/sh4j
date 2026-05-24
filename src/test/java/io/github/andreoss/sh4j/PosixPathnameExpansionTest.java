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
 * Tests for {@link Sh4j}: pathname expansion across directories (POSIX 2.6.6
 * and 2.13.3).
 *
 * @since 1.0
 */
final class PosixPathnameExpansionTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void expandsPatternDirectlyUnderRoot() {
        Assertions.assertEquals("[/tmp]", Sh4j.INSTANCE.out("printf '[%s]' /tm*"));
    }

    @Test
    void expandsQuestionMarkDirectlyUnderRoot() {
        Assertions.assertEquals("[/etc]", Sh4j.INSTANCE.out("printf '[%s]' /e?c"));
    }

    @Test
    void keepsDoubleLeadingSlash() {
        Assertions.assertEquals("[//tmp]", Sh4j.INSTANCE.out("printf '[%s]' //tm*"));
    }

    @Test
    void keepsDotComponentUnderRoot() {
        Assertions.assertEquals("[/./tmp]", Sh4j.INSTANCE.out("printf '[%s]' /./tm*"));
    }

    @Test
    void keepsTrailingSlashUnderRoot() {
        Assertions.assertEquals("[/etc/]", Sh4j.INSTANCE.out("printf '[%s]' /e?c/"));
    }

    @Test
    void expandsPatternsInSeveralAbsoluteComponents() throws IOException {
        final File dir = PosixPathnameExpansionTest.tree(
            "uaa/bin/", "uaa/lib/", "ubb/bxx/", "vcc/bin/"
        );
        Assertions.assertEquals(
            String.format("[%1$s/uaa/bin][%1$s/ubb/bxx]", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(
                String.format("printf '[%%s]' '%s'/u*/b*", dir.getAbsolutePath())
            )
        );
    }

    @Test
    void trailingSlashMatchesDirectoriesOnly() throws IOException {
        Assertions.assertEquals(
            "[sub/]",
            PosixPathnameExpansionTest.expand("*/", "sub/x/", "plain")
        );
    }

    @Test
    void starDoesNotMatchLeadingDot() throws IOException {
        Assertions.assertEquals(
            "[vis/x]",
            PosixPathnameExpansionTest.expand("*/x", "vis/x/", ".hid/x/")
        );
    }

    @Test
    void questionMarkDoesNotMatchLeadingDot() throws IOException {
        Assertions.assertEquals(
            "[ah/x]",
            PosixPathnameExpansionTest.expand("?h/x", ".h/x/", "ah/x/")
        );
    }

    @Test
    void leadingDotPatternSkipsDotAndDotDot() throws IOException {
        Assertions.assertEquals(
            "[.hid]",
            PosixPathnameExpansionTest.expand(".*", ".hid/x/", "vis/x/")
        );
    }

    @Test
    void leadingDotInInnerComponentMatchesDotFiles() throws IOException {
        Assertions.assertEquals(
            "[vis/.hid]",
            PosixPathnameExpansionTest.expand("v*/.h*", "vis/.hid/", "vis/top/")
        );
    }

    @Test
    void expandsPatternInMiddleComponent() throws IOException {
        Assertions.assertEquals(
            "[a/x/c][a/y/c]",
            PosixPathnameExpansionTest.expand("a/*/c", "a/x/c/", "a/y/c/", "a/z/d/")
        );
    }

    @Test
    void expandsLiteralComponentBetweenPatterns() throws IOException {
        Assertions.assertEquals(
            "[p/m/one][q/m/two]",
            PosixPathnameExpansionTest.expand("*/m/*", "p/m/one/", "q/m/two/", "r/n/six/")
        );
    }

    @Test
    void fileInMiddleComponentDoesNotMatch() throws IOException {
        Assertions.assertEquals(
            "[dir/c]",
            PosixPathnameExpansionTest.expand("*i*/c", "dir/c/", "fil")
        );
    }

    @Test
    void keepsPatternWhenNoPathMatches() throws IOException {
        Assertions.assertEquals(
            "[q*/b]",
            PosixPathnameExpansionTest.expand("q*/b", "a/b/")
        );
    }

    @Test
    void keepsPatternUnderMissingDirectory() throws IOException {
        Assertions.assertEquals(
            "[nope/*]",
            PosixPathnameExpansionTest.expand("nope/*", "a/b/")
        );
    }

    @Test
    void slashInsideBracketIsLiteral() throws IOException {
        Assertions.assertEquals(
            "[a[b/c]d]",
            PosixPathnameExpansionTest.expand("a[b/c]d", "abd/", "a/d/")
        );
    }

    @Test
    void quotedPatternCharacterInComponentIsLiteral() throws IOException {
        Assertions.assertEquals(
            "[k*/v]",
            PosixPathnameExpansionTest.expand("'k*'/?", "k*/v/", "kk/v/")
        );
    }

    private static String expand(final String pattern, final String... paths)
        throws IOException {
        return Sh4j.INSTANCE.out(
            String.format(
                "cd '%s'; printf '[%%s]' %s",
                PosixPathnameExpansionTest.tree(paths).getAbsolutePath(),
                pattern
            )
        );
    }

    private static File tree(final String... paths) throws IOException {
        final File dir = new TempDirs(PosixPathnameExpansionTest.root).folder();
        for (final String path : paths) {
            final Path full = dir.toPath().resolve(path);
            Files.createDirectories(full.getParent());
            if (path.endsWith("/")) {
                Files.createDirectory(full);
            } else {
                Files.write(full, new byte[0]);
            }
        }
        return dir;
    }
}
