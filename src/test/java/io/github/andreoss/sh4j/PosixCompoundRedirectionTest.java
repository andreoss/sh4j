/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: redirections after compound commands apply to
 * the whole command (POSIX 2.9.4).
 *
 * @since 1.0
 */
final class PosixCompoundRedirectionTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void groupOutputGoesToFile() throws IOException {
        Assertions.assertEquals(
            String.format("a%nb%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "{ echo a; echo b; } > bg; cat bg"
                )
            )
        );
    }

    @Test
    void groupAppendsToFile() throws IOException {
        Assertions.assertEquals(
            String.format("a%nk%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "{ echo a; } > ap; { echo k; } >> ap; cat ap"
                )
            )
        );
    }

    @Test
    void subshellOutputGoesToFile() throws IOException {
        Assertions.assertEquals(
            String.format("p%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside("(echo p) > bp; cat bp")
            )
        );
    }

    @Test
    void whileLoopReadsRedirectedInput() throws IOException {
        Assertions.assertEquals(
            String.format("got l1%ngot l2%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    String.join(
                        "; ",
                        "printf 'l1\\nl2\\n' > in",
                        "while read -r l; do echo \"got $l\"; done < in"
                    )
                )
            )
        );
    }

    @Test
    void groupDiscardsErrors() {
        Assertions.assertEquals(
            String.format("o%n"),
            Sh4j.INSTANCE.out("{ echo e >&2; echo o; } 2>/dev/null")
        );
    }

    @Test
    void groupReadsHereDocument() {
        Assertions.assertEquals(
            String.format("hd%n"),
            Sh4j.INSTANCE.out(String.format("{ cat; } <<E%nhd%nE"))
        );
    }

    @Test
    void ifOutputGoesToFile() throws IOException {
        Assertions.assertEquals(
            String.format("i%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "if true; then echo i; fi > fi.txt; cat fi.txt"
                )
            )
        );
    }

    @Test
    void forOutputGoesToFile() throws IOException {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "for v in 1 2; do echo \"$v\"; done > fo.txt; cat fo.txt"
                )
            )
        );
    }

    @Test
    void caseOutputGoesToFile() throws IOException {
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "case x in x) echo c;; esac > ca.txt; cat ca.txt"
                )
            )
        );
    }

    @Test
    void untilStatusSurvivesRedirection() throws IOException {
        Assertions.assertEquals(
            String.format("u0%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "until true; do :; done > u.txt; echo \"u$?\""
                )
            )
        );
    }

    @Test
    void subshellInPipelineMergesErrors() {
        Assertions.assertEquals(
            String.format("s%nt%n"),
            Sh4j.INSTANCE.out("(echo s; echo t >&2) 2>&1 | cat")
        );
    }

    @Test
    void subshellInPipelineWritesFile() throws IOException {
        Assertions.assertEquals(
            String.format("z%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside("(echo z) > pz | cat; cat pz")
            )
        );
    }

    @Test
    void nestedGroupRedirectsInner() throws IOException {
        Assertions.assertEquals(
            String.format("o%ni%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "{ { echo i; } > ni; echo o; }; cat ni"
                )
            )
        );
    }

    @Test
    void failedGroupRedirectionSkipsBody() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("{ echo nope; } > /nonexistent/x; echo \"rc=$?\"")
        );
    }

    @Test
    void errexitStopsAfterRedirectedSubshell() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside("set -e; (false) > se; echo no")
            )
        );
    }

    @Test
    void errexitIgnoresRedirectedGroupStatus() throws IOException {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out(
                PosixCompoundRedirectionTest.inside(
                    "set -e; { false && true; } > sg; echo after"
                )
            )
        );
    }

    private static String inside(final String script) throws IOException {
        return String.format(
            "cd %s && %s",
            new TempDirs(PosixCompoundRedirectionTest.root).folder().getAbsolutePath(),
            script
        );
    }
}
