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
 * Tests for the {@code set} options {@code -e}, {@code -n} and {@code -a}
 * (POSIX 2.14 set).
 *
 * @since 1.0
 */
final class PosixSetOptionsTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void errexitIgnoresBangPipeline() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; ! true; echo alive")
        );
    }

    @Test
    void errexitIgnoresBangPipelineStatus() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; ! true; echo alive"));
    }

    @Test
    void errexitIgnoresBangGroupBody() {
        Assertions.assertEquals(
            String.format("in%nx%n"),
            Sh4j.INSTANCE.out("set -e; ! { false; echo in; }; echo x")
        );
    }

    @Test
    void errexitIgnoresLeftOfAndList() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; false && true; echo alive")
        );
    }

    @Test
    void errexitAppliesToLastOfOrList() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; false || false; echo x"));
    }

    @Test
    void errexitAppliesInsideLastOfOrList() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; false || { false; echo in; }; echo x")
        );
    }

    @Test
    void errexitIgnoresFunctionInAndList() {
        Assertions.assertEquals(
            String.format("inf%nok%nalive%n"),
            Sh4j.INSTANCE.out("set -e; f() { false; echo inf; }; f && echo ok; echo alive")
        );
    }

    @Test
    void errexitIgnoresIfCondition() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; if false; then :; fi; echo alive")
        );
    }

    @Test
    void errexitIgnoresSubshellInIfCondition() {
        Assertions.assertEquals(
            String.format("in%nx%n"),
            Sh4j.INSTANCE.out("set -e; if (false; echo in); then :; fi; echo x")
        );
    }

    @Test
    void errexitIgnoresWhileCondition() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; while false; do :; done; echo alive")
        );
    }

    @Test
    void errexitIgnoresUntilCondition() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; until true; do :; done; echo alive")
        );
    }

    @Test
    void errexitLeavesLoopBody() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -e; for i in 1 2; do echo \"$i\"; false; done; echo x")
        );
    }

    @Test
    void errexitSkipsGroupWithIgnoredFailure() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("set -e; { false && true; }; echo x")
        );
    }

    @Test
    void errexitAppliesToFunctionWithIgnoredFailure() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; f() { false && true; }; f; echo x")
        );
    }

    @Test
    void errexitAppliesToFailingSubshell() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; (false && true); echo x"));
    }

    @Test
    void errexitLongNameExits() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -o errexit; false; echo x"));
    }

    @Test
    void errexitLongNameStopsOutput() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -o errexit; false; echo x"));
    }

    @Test
    void errexitPlusDisables() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; set +e; false; echo alive")
        );
    }

    @Test
    void noexecSkipsCommands() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -n; echo no"));
    }

    @Test
    void noexecLongNameSkipsCommands() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -o noexec; echo no"));
    }

    @Test
    void noexecRunsEarlierCommands() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("echo yes; set -n; echo no")
        );
    }

    @Test
    void noexecIgnoresLaterPlusN() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -n; set +n; echo no"));
    }

    @Test
    void noexecSkipsBackgroundCommands() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -n; echo no & wait"));
    }

    @Test
    void noexecKeepsStatus() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -n; false"));
    }

    @Test
    void allexportExportsAssignment() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -a; AE=1; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportLongNameExportsAssignment() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -o allexport; AE=1; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportPlusStopsExporting() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("set -a; set +a; AE=1; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportExportsSameValueAssignment() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("AE=1; set -a; AE=1; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportKeepsEarlierVariablesUnexported() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("AE=1; set -a; BE=2; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportExportsRead() {
        Assertions.assertEquals(
            String.format("rd%n"),
            Sh4j.INSTANCE.out(String.format("set -a; read AE <<X%nrd%nX%nsh -c 'echo \"$AE\"'"))
        );
    }

    @Test
    void allexportExportsGetopts() {
        Assertions.assertEquals(
            String.format("a v%n"),
            Sh4j.INSTANCE.out("set -a; getopts a: AE -a v; sh -c 'echo \"$AE\" \"$OPTARG\"'")
        );
    }

    @Test
    void allexportExportsDefaultAssignment() {
        Assertions.assertEquals(
            String.format("dv%n"),
            Sh4j.INSTANCE.out("set -a; : \"${AE:=dv}\"; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportExportsArithmeticAssignment() {
        Assertions.assertEquals(
            String.format("5%n"),
            Sh4j.INSTANCE.out("set -a; : \"$((AE=5))\"; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportExportsForVariable() {
        Assertions.assertEquals(
            String.format("fv%n"),
            Sh4j.INSTANCE.out("set -a; for AE in fv; do sh -c 'echo \"$AE\"'; done")
        );
    }

    @Test
    void allexportExportsFunctionAssignment() {
        Assertions.assertEquals(
            String.format("fn%n"),
            Sh4j.INSTANCE.out("set -a; f() { AE=fn; }; f; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportExportsSpecialBuiltinPrefix() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -a; AE=1 :; sh -c 'echo \"$AE\"'")
        );
    }

    @Test
    void allexportReachesSubshellChildren() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("set -a; (AE=1; sh -c 'echo \"$AE\"')")
        );
    }

    @Test
    void allexportSkipsLineNumber() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("set -a; echo a > /dev/null; env | grep -c '^LINENO='")
        );
    }

    @Test
    void mainNoexecOptionSkipsScript() throws IOException {
        final File script = new TempDirs(PosixSetOptionsTest.root).file("noexec.sh");
        Files.write(script.toPath(), String.format("exit 42%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(0, Main.exitCode("-n", script.getAbsolutePath()));
    }

    @Test
    void mainErrexitOptionStopsScript() throws IOException {
        final File script = new TempDirs(PosixSetOptionsTest.root).file("errexit.sh");
        Files.write(
            script.toPath(),
            String.format("false%nexit 42%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(1, Main.exitCode("-e", script.getAbsolutePath()));
    }
}
