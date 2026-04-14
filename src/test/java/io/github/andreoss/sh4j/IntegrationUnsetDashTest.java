/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
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
 * Tests for {@link Main}: unset dash.
 *
 * @since 1.0
 */
public final class IntegrationUnsetDashTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    /**
     * POSIX 2.14 unset -f - removes a function definition.
     */
    @Test
    void unsetDashFRemovesFunction() throws Exception {
        Assertions.assertEquals(
            String.format("from-fn%ngone%n"),
            this.runChildJvm(
                this.writeScript(
                    "unsetf.sh",
                    String.format(
                        "f() { echo from-fn; }%nf%nunset -f f%nf 2>/dev/null || echo gone%n"
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.5 Until - body runs while the condition exits non-zero.
     */
    @Test
    void untilLoopCounts() throws Exception {
        Assertions.assertEquals(
            String.format("1%n2%n3%n"),
            this.runChildJvm(
                this.writeScript(
                    "until.sh",
                    String.format("i=1%nuntil [ $i -gt 3 ]; do echo $i; i=$((i+1)); done%n")
                )
            )
        );
    }

    @Test
    void variableExpansionWithRedirectFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "varexp_redir.sh",
                    String.format(
                        "echo $HOME > %s",
                        new TempDirs(IntegrationUnsetDashTest.root).file(
                            "out2.txt"
                        ).getAbsolutePath()
                    )
                        + String.format("%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void variableExpansionWithRedirectSecond() throws IOException {
        final File outfile = new TempDirs(IntegrationUnsetDashTest.root).file("out2.txt");
        Main.runScript(
            this.writeScript(
                "varexp_redir.sh",
                String.format("echo $HOME > %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertTrue(outfile.exists(), "out2.txt should exist");
    }

    @Test
    void variableExpansionWithRedirectThird() throws IOException {
        final File outfile = new TempDirs(IntegrationUnsetDashTest.root).file("out2.txt");
        Main.runScript(
            this.writeScript(
                "varexp_redir.sh",
                String.format("echo $HOME > %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertFalse(
            new String(
                Files.readAllBytes(outfile.toPath()),
                StandardCharsets.UTF_8
            ).trim().isEmpty(),
            "content should not be empty"
        );
    }

    @Test
    void variableExpansion() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("varexp.sh", String.format("echo $SHELL%n")).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.14 wait builtin - {@code wait} without args returns 0 when no
     * background jobs are running.
     */
    @Test
    void waitWithoutBackgroundJobsReturnsZero() throws Exception {
        Assertions.assertEquals(
            String.format("0%n"),
            this.runChildJvm(this.writeScript("wait.sh", String.format("wait%necho $?%n")))
        );
    }

    /**
     * POSIX 2.9.4.3 While - body runs while the condition exits zero.
     */
    @Test
    void whileLoopCounts() throws Exception {
        Assertions.assertEquals(
            String.format("1%n2%n3%n"),
            this.runChildJvm(
                this.writeScript(
                    "while.sh",
                    String.format("i=1%nwhile [ $i -le 3 ]; do echo $i; i=$((i+1)); done%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.3 While - explicit while-false loop runs zero iterations.
     */
    @Test
    void whileFalseZeroIterations() throws Exception {
        Assertions.assertEquals(
            String.format("done%n"),
            this.runChildJvm(
                this.writeScript(
                    "wfalse.sh",
                    String.format("while false; do echo never; done%necho done%n")
                )
            )
        );
    }

    private File writeScript(final String name, final String content) throws IOException {
        final File script = new TempDirs(IntegrationUnsetDashTest.root).file(name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assertions.assertTrue(script.setExecutable(true));
        return script;
    }

    private String runChildJvm(final File script) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-cp",
            System.getProperty("java.class.path"),
            "io.github.andreoss.sh4j.Main",
            script.getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("child sh4j exit code; output: %s", out)
        );
        return out;
    }

    private static byte[] readAllBytes(final InputStream input) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int num;
        while ((num = input.read(buf)) != -1) {
            bos.write(buf, 0, num);
        }
        return bos.toByteArray();
    }
}
