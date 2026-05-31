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
 * Tests for {@link Sh4j}: built-in diagnostics follow the redirections of
 * the command (POSIX 2.7).
 *
 * @since 1.0
 */
final class PosixBuiltinDiagnosticTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void cdFailureLandsInRedirectedFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(PosixBuiltinDiagnosticTest.probe("cd ./nowhere 2> f", "nowhere"))
        );
    }

    @Test
    void cdFailureInRedirectedGroupLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("{ cd ./nowhere; } 2> f", "nowhere")
            )
        );
    }

    @Test
    void cdFailureAfterExecRedirectLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("exec 2> f; cd ./nowhere", "nowhere")
            )
        );
    }

    @Test
    void cdFailureDupedToStdoutReachesPipe() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && x=$(cd ./nowhere 2>&1 | cat); %s",
                    PosixBuiltinDiagnosticTest.folder(),
                    "case \"$x\" in *nowhere*) echo in;; *) echo out;; esac"
                )
            )
        );
    }

    @Test
    void cdFailureToDevNullIsSilent() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                String.format(
                    "cd '%s' && cd ./nowhere 2>/dev/null",
                    PosixBuiltinDiagnosticTest.folder()
                )
            ).stderr()
        );
    }

    @Test
    void cdFailureWritesShellStderr() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                String.format("cd '%s' && cd ./nowhere", PosixBuiltinDiagnosticTest.folder())
            ).stderr().contains("nowhere")
        );
    }

    @Test
    void cdWithoutHomeReportsIt() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(PosixBuiltinDiagnosticTest.probe("unset HOME; cd 2> f", "HOME"))
        );
    }

    @Test
    void cdDashWithoutOldpwdReportsIt() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("unset OLDPWD; cd - 2> f", "OLDPWD")
            )
        );
    }

    @Test
    void umaskBadModeLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(PosixBuiltinDiagnosticTest.probe("umask 999 2> f", "999"))
        );
    }

    @Test
    void getoptsUsageLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(PosixBuiltinDiagnosticTest.probe("getopts 2> f", "getopts"))
        );
    }

    @Test
    void aliasMissLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("alias nosuchalias 2> f", "nosuchalias")
            )
        );
    }

    @Test
    void unaliasMissLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("unalias nosuchalias 2> f", "nosuchalias")
            )
        );
    }

    @Test
    void hashMissLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("hash nosuchcmd 2> f", "nosuchcmd")
            )
        );
    }

    @Test
    void typeMissLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("type nosuchcmd 2> f", "nosuchcmd")
            )
        );
    }

    @Test
    void commandVerboseMissLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("command -V nosuchcmd 2> f", "nosuchcmd")
            )
        );
    }

    @Test
    void commandLocateMissIsSilent() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("command -v nosuchcmd").stderr()
        );
    }

    @Test
    void waitUnknownJobLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(PosixBuiltinDiagnosticTest.probe("wait %9 2> f", "%9"))
        );
    }

    @Test
    void returnOutsideFunctionHonoursDevNull() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("(return 2>/dev/null); echo after").stderr()
        );
    }

    @Test
    void returnFromSubshellInFunctionIsSilent() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("f() { (return 3); echo $?; }; f").stderr()
        );
    }

    @Test
    void returnFromSubshellInFunctionKeepsStatus() {
        Assertions.assertEquals(
            String.format("3%n"),
            Sh4j.INSTANCE.out("f() { (return 3); echo $?; }; f")
        );
    }

    @Test
    void returnFromNestedSubshellInFunctionIsSilent() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("f() { ( (return 3); echo $? ); }; f").stderr()
        );
    }

    @Test
    void returnOutsideFunctionEndsShellWithStatus() {
        Assertions.assertEquals(
            4,
            Sh4j.INSTANCE.run("return 4 2>/dev/null; echo after")
        );
    }

    @Test
    void returnInsideDotScriptIsSilent() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                String.format(
                    "cd '%s' && echo 'return 5' > r.sh && . ./r.sh",
                    PosixBuiltinDiagnosticTest.folder()
                )
            ).stderr()
        );
    }

    @Test
    void breakOutsideLoopHonoursDevNull() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("break 2>/dev/null; echo after").stderr()
        );
    }

    @Test
    void continueOutsideLoopHonoursDevNull() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("continue 2>/dev/null; echo after").stderr()
        );
    }

    @Test
    void breakOutsideLoopCarriesOn() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("break 2>/dev/null; echo after")
        );
    }

    @Test
    void evalSyntaxErrorLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe("command eval 'if' 2> f", "eval")
            )
        );
    }

    @Test
    void evalSyntaxErrorStillExitsShell() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("eval 'if' 2>/dev/null; echo no"));
    }

    @Test
    void dotSyntaxErrorLandsInFile() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixBuiltinDiagnosticTest.probe(
                    "printf 'if\\n' > bad.sh; command . ./bad.sh 2> f", "bad.sh"
                )
            )
        );
    }

    private static String probe(final String cmd, final String word) throws IOException {
        return String.format(
            "cd '%s' && %s; case \"$(cat f)\" in *%s*) echo in;; *) echo out;; esac",
            PosixBuiltinDiagnosticTest.folder(), cmd, word
        );
    }

    private static String folder() throws IOException {
        return new TempDirs(PosixBuiltinDiagnosticTest.root).folder().getAbsolutePath();
    }
}
