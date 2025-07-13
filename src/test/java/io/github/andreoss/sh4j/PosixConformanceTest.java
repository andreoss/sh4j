/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for PosixConformance.
 * @since 1.0
 */
final class PosixConformanceTest {

    /** Field. */
    @TempDir
    java.nio.file.Path tempdir;

    @Test
    void positionalParamCount() throws Exception {
        Assertions.assertTrue(run(String.format("echo $#%n"), "a", "b", "c").trim().equals("3"));
    }

    @Test
    void positionalParamAt() throws Exception {
        Assertions.assertTrue(
            run(String.format("echo $@%n"), "x", "y", "z").trim().equals("x y z")
        );
    }

    @Test
    void positionalFirstParam() throws Exception {
        Assertions.assertTrue(this.run(String.format("echo $1%n"), "hello").trim().equals("hello"));
    }

    @Test
    void paramDefaultWhenUnset() throws Exception {
        Assertions.assertTrue(
            run(String.format("echo ${UNSET_XYZ:-fallback}%n")).trim().equals("fallback")
        );
    }

    @Test
    void paramLengthOfVar() throws Exception {
        Assertions.assertTrue(run(String.format("V=hello; echo ${#V}%n")).trim().equals("5"));
    }

    @Test
    void arithmeticPrecedence() throws Exception {
        Assertions.assertTrue(run(String.format("echo $((1 + 2 * 3))%n")).trim().equals("7"));
    }

    @Test
    void arithmeticParens() throws Exception {
        Assertions.assertTrue(run(String.format("echo $(((1 + 2) * 3))%n")).trim().equals("9"));
    }

    @Test
    void commandSubstitutionDollarParen() throws Exception {
        Assertions.assertTrue(run(String.format("echo a$(echo b)c%n")).trim().equals("abc"));
    }

    @Test
    void backtickSubstitution() throws Exception {
        Assertions.assertTrue(run(String.format("echo a`echo b`c%n")).trim().equals("abc"));
    }

    @Test
    void ifThenElse() throws Exception {
        Assertions.assertTrue(
            run(String.format("if true; then echo yes; else echo no; fi%n")).trim().equals("yes")
        );
    }

    @Test
    void forLoopBody() throws Exception {
        Assertions.assertEquals(
            String.format("a%nb%nc"),
            run(String.format("for i in a b c; do echo $i; done%n")).trim()
        );
    }

    @Test
    void caseExact() throws Exception {
        Assertions.assertTrue(
            run(
                String.format("case foo in bar) echo NO;; foo) echo YES;; esac%n")
            ).trim().equals("YES")
        );
    }

    @Test
    void echoWithDoubleQuotes() throws Exception {
        Assertions.assertTrue(
            run(String.format("echo \"hello world\"%n")).trim().equals("hello world")
        );
    }

    @Test
    void hereDocContent() throws Exception {
        Assertions.assertEquals(
            String.format("line1%nline2"),
            run(String.format("cat <<EOF%nline1%nline2%nEOF%n")).trim()
        );
    }

    private String run(final String script, final String... args) throws Exception {
        final File str = this.tempdir.resolve("c.sh").toFile();
        try (BufferedWriter w = Files.newBufferedWriter(str.toPath(), StandardCharsets.UTF_8)) {
            w.write(script);
        }
        final java.util.List<String> cmd = new java.util.ArrayList<>(7);
        cmd.add(String.format("%s/bin/java", System.getProperty("java.home")));
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add("io.github.andreoss.sh4j.Main");
        cmd.add(str.getAbsolutePath());
        cmd.addAll(java.util.Arrays.asList(args));
        final ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        proc.waitFor();
        return new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
