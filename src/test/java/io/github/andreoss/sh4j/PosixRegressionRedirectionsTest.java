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
 * Tests for {@link Sh4j}: redirections.
 *
 * @since 1.0
 */
final class PosixRegressionRedirectionsTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void redirectionsResolveAgainstShellCwdFirst() throws IOException {
        Assertions.assertEquals(
            String.format("seed%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"",
                    new TempDirs(PosixRegressionRedirectionsTest.root).folder(
                        "cwdtest"
                    ).getAbsolutePath()
                )
            )
        );
    }

    @Test
    void redirectionsResolveAgainstShellCwdSecond() throws IOException {
        final String base = new TempDirs(PosixRegressionRedirectionsTest.root).folder(
            "cwdtest"
        ).getAbsolutePath();
        Sh4j.INSTANCE.out(
            String.format(
                "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"",
                base
            )
        );
        Assertions.assertEquals(
            String.format("fd-ok%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; exec 3>fdout.txt; echo fd-ok >&3; exec 3>&-; cat fdout.txt",
                    base
                )
            )
        );
    }

    @Test
    void redirectionsResolveAgainstShellCwdThird() throws IOException {
        final File dir = new TempDirs(PosixRegressionRedirectionsTest.root).folder("cwdtest");
        final String base = dir.getAbsolutePath();
        Sh4j.INSTANCE.out(
            String.format(
                "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"",
                base
            )
        );
        Sh4j.INSTANCE.out(
            String.format(
                "cd %s; exec 3>fdout.txt; echo fd-ok >&3; exec 3>&-; cat fdout.txt",
                base
            )
        );
        Assertions.assertEquals(
            String.format("fd-ok%n"),
            new String(
                Files.readAllBytes(new File(dir, "fdout.txt").toPath()),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void returnInsideFunction() {
        Assertions.assertEquals(4, Sh4j.INSTANCE.run("f() { return 4; }; f"));
    }

    @Test
    void setPlusEClearsErrexit() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; set +e; false; echo alive")
        );
    }

    @Test
    void setDashOOptions() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -o nounset; echo $MISSING_OPTION_VAR"));
    }

    @Test
    void shellVariablesAreNotExported() {
        Assertions.assertEquals(
            String.format("unset%n"),
            Sh4j.INSTANCE.out("X=secret; sh -c 'echo ${X-unset}'")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsFirst() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsSecond() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Assertions.assertEquals(
            String.format("count=2%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsThird() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsFourth() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("lit%n"),
            Sh4j.INSTANCE.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsFifth() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac");
        Sh4j.INSTANCE.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac");
        Assertions.assertEquals(String.format("after%n"), Sh4j.INSTANCE.out("break; echo after"));
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsSixth() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac");
        Sh4j.INSTANCE.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac");
        Sh4j.INSTANCE.out("break; echo after");
        Assertions.assertEquals(
            String.format("1a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 2; done; done")
        );
    }

    @Test
    void specialBuiltinAssignmentPersists() {
        Assertions.assertEquals(
            String.format("12%n"),
            Sh4j.INSTANCE.out("X=1 export Y=2; echo $X$Y")
        );
    }

    @Test
    void starJoinsWithFirstIfsCharacter() {
        Assertions.assertEquals(
            String.format("a:b%n"),
            Sh4j.INSTANCE.out("set -- a b; IFS=:; echo \"$*\"")
        );
    }

    @Test
    void subshellCdDoesNotChangeParentCwd() {
        Assertions.assertEquals(String.format("/tmp%n"), Sh4j.INSTANCE.out("cd /tmp; (cd /); pwd"));
    }

    @Test
    void subshellExitOnlyLeavesSubshell() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("(exit 3); echo after")
        );
    }

    @Test
    void subshellBodyRunsInChildScope() {
        Assertions.assertEquals(String.format("s%n"), Sh4j.INSTANCE.out("(echo s)"));
    }

    @Test
    void threeStagePipelineRunsEveryStage() {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            Sh4j.INSTANCE.out("echo a; echo b; echo c")
        );
    }

    @Test
    void trapDotAndTimesWorkFirst() throws IOException {
        final File lib = new TempDirs(PosixRegressionRedirectionsTest.root).file("lib.sh");
        Files.write(lib.toPath(), String.format("LIB=ok%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                String.format(
                    "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                    lib.getAbsolutePath()
                )
            ).stdout().contains("lib=ok")
        );
    }

    @Test
    void trapDotAndTimesWorkSecond() throws IOException {
        final File lib = new TempDirs(PosixRegressionRedirectionsTest.root).file("lib.sh");
        Files.write(lib.toPath(), String.format("LIB=ok%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                String.format(
                    "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                    lib.getAbsolutePath()
                )
            ).stdout().contains("0m0.000s")
        );
    }

    @Test
    void trapDotAndTimesWorkThird() throws IOException {
        final File lib = new TempDirs(PosixRegressionRedirectionsTest.root).file("lib.sh");
        Files.write(lib.toPath(), String.format("LIB=ok%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                String.format(
                    "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                    lib.getAbsolutePath()
                )
            ).stdout().contains("caught")
        );
    }

    @Test
    void trapListingAndRemovalFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
            ).stdout().contains("trap -- 'echo t' EXIT")
        );
    }

    @Test
    void trapListingAndRemovalSecond() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
            ).stdout().contains("body")
        );
    }

    @Test
    void trapListingAndRemovalThird() {
        Assertions.assertFalse(
            Sh4j.INSTANCE.runFull(
                "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
            ).stdout().contains(String.format("t%n"))
        );
    }

    @Test
    void trapRuntimeErrorDoesNotCrashExit() {
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stdout()
        );
    }

    @Test
    void trapEmptyActionIsIgnored() {
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.out("trap '' EXIT; echo body")
        );
    }

    @Test
    void typeRecognisesReadBuiltin() {
        Assertions.assertEquals(
            String.format("read is a shell built-in%n"),
            Sh4j.INSTANCE.out("type read")
        );
    }

    @Test
    void unsetFunctionAndCommandLookupFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("f() { :; }; unset -f f; type f"));
    }

    @Test
    void unsetFunctionAndCommandLookupSecond() {
        Sh4j.INSTANCE.run("f() { :; }; unset -f f; type f");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("command"));
    }

    @Test
    void unsetFunctionAndCommandLookupThird() {
        Sh4j.INSTANCE.run("f() { :; }; unset -f f; type f");
        Sh4j.INSTANCE.run("command");
        Assertions.assertEquals(String.format("echo%n"), Sh4j.INSTANCE.out("command -v echo"));
    }

    @Test
    void untilLoopRunsUntilConditionHolds() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out("i=0; until [ $i -ge 2 ]; do i=$((i+1)); echo $i; done")
        );
    }

    @Test
    void waitUnknownPidReturnsError() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("wait 999999"));
    }

    @Test
    void xtraceWritesCommandToStderrFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("set -x; echo hi").stderr().contains("+ echo hi")
        );
    }

    @Test
    void xtraceWritesCommandToStderrSecond() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.runFull("set -x; echo hi").stdout()
        );
    }
}
