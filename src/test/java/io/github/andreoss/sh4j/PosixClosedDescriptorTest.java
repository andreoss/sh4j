/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: {@code >&-} and {@code <&-} close a standard
 * descriptor, so writing to it or reading from it fails (POSIX 2.7.5, 2.7.6).
 *
 * @since 1.0
 */
final class PosixClosedDescriptorTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void echoToClosedOutputFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("echo G >&-; echo \"rc=$?\"")
        );
    }

    @Test
    void echoToClosedOutputReportsError() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo G >&-").stderr().contains("echo")
        );
    }

    @Test
    void printfToClosedOutputFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("printf x >&-"));
    }

    @Test
    void externalToClosedOutputFails() {
        Assertions.assertNotEquals(0, Sh4j.INSTANCE.run("ls / >&-"));
    }

    @Test
    void groupToClosedOutputFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("{ echo a; } >&-"));
    }

    @Test
    void functionToClosedOutputFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("f() { echo a; }; f >&-"));
    }

    @Test
    void subshellToClosedOutputFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("(echo a) >&-"));
    }

    @Test
    void closeThroughInputOperatorFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo a 1<&-"));
    }

    @Test
    void outputIsBackAfterCommand() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("echo a >&-; echo b")
        );
    }

    @Test
    void execClosedOutputLosesEcho() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("exec >&-; echo x; echo y >&2"));
    }

    @Test
    void execClosedOutputKeepsErrors() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("exec >&-; echo x; echo y >&2")
                .stderr().endsWith(String.format("y%n"))
        );
    }

    @Test
    void execClosedOutputFailsExternal() {
        Assertions.assertNotEquals(
            0, Sh4j.INSTANCE.run("exec >&-; echo a | cat")
        );
    }

    @Test
    void readFromClosedInputFails() {
        Assertions.assertEquals(
            String.format("rc=1 v=%n"),
            Sh4j.INSTANCE.stdout(
                PosixClosedDescriptorTest.input(),
                "read v <&-; echo \"rc=$? v=$v\""
            )
        );
    }

    @Test
    void readFromInputClosedByOutputOperatorFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixClosedDescriptorTest.input(),
                "read v 0>&-; echo \"rc=$?\""
            )
        );
    }

    @Test
    void catFromClosedInputFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixClosedDescriptorTest.input(),
                "cat <&-; echo \"rc=$?\""
            )
        );
    }

    @Test
    void execClosedInputFailsRead() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixClosedDescriptorTest.input(),
                "exec <&-; read v; echo \"rc=$?\""
            )
        );
    }

    @Test
    void duplicatingClosedErrorFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo e 2>&- >&2"));
    }

    @Test
    void duplicatingClosedOutputFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo e >&- 2>&1"));
    }

    @Test
    void silentCommandWithClosedErrorSucceeds() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true 2>&-"));
    }

    @Test
    void echoWithClosedErrorSucceeds() {
        Assertions.assertEquals(String.format("o%n"), Sh4j.INSTANCE.out("echo o 2>&-"));
    }

    @Test
    void missingCommandWithClosedErrorIsSilent() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("nosuchcommandxyz 2>&-").stderr()
        );
    }

    @Test
    void externalErrorToClosedErrorIsLost() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("ls /nonexistent/xyz 2>&-").stderr()
        );
    }

    @Test
    void execClosedErrorRejectsDuplicate() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("exec 2>&-; echo x >&2; echo \"rc=$?\"")
        );
    }

    @Test
    void execClosedErrorSilencesExternal() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("exec 2>&-; ls /nonexistent/xyz").stderr()
        );
    }

    @Test
    void execClosedInputFailsExternal() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixClosedDescriptorTest.input(),
                "exec <&-; cat; echo \"rc=$?\""
            )
        );
    }

    @Test
    void readsFromFileNamedDash() throws IOException {
        final File dir = new TempDirs(PosixClosedDescriptorTest.root).folder();
        Files.writeString(
            new File(dir, "-").toPath(), String.format("d%n"), StandardCharsets.UTF_8
        );
        Assertions.assertEquals(
            String.format("v=d%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && read v < -; echo \"v=$v\"", dir.getAbsolutePath()
                )
            )
        );
    }

    private static InputStream input() {
        return new ByteArrayInputStream(
            String.format("line%n").getBytes(StandardCharsets.UTF_8)
        );
    }
}
