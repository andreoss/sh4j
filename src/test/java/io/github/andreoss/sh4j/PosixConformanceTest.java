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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for PosixConformance.
 *
 * @since 1.0
 */
final class PosixConformanceTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void positionalParamCount() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo $#%n"), "a", "b", "c").trim(),
            CoreMatchers.equalTo("3")
        );
    }

    @Test
    void positionalParamAt() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo $@%n"), "x", "y", "z").trim(),
            CoreMatchers.equalTo("x y z")
        );
    }

    @Test
    void positionalFirstParam() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo $1%n"), "hello").trim(),
            CoreMatchers.equalTo("hello")
        );
    }

    @Test
    void paramDefaultWhenUnset() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo ${UNSET_XYZ:-fallback}%n")).trim(),
            CoreMatchers.equalTo("fallback")
        );
    }

    @Test
    void paramLengthOfVar() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("V=hello; echo ${#V}%n")).trim(),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void arithmeticPrecedence() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo $((1 + 2 * 3))%n")).trim(),
            CoreMatchers.equalTo("7")
        );
    }

    @Test
    void arithmeticParens() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo $(((1 + 2) * 3))%n")).trim(),
            CoreMatchers.equalTo("9")
        );
    }

    @Test
    void commandSubstitutionDollarParen() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo a$(echo b)c%n")).trim(),
            CoreMatchers.equalTo("abc")
        );
    }

    @Test
    void backtickSubstitution() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo a`echo b`c%n")).trim(),
            CoreMatchers.equalTo("abc")
        );
    }

    @Test
    void ifThenElse() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("if true; then echo yes; else echo no; fi%n")).trim(),
            CoreMatchers.equalTo("yes")
        );
    }

    @Test
    void forLoopBody() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("for i in a b c; do echo $i; done%n")).trim(),
            CoreMatchers.equalTo(String.format("a%nb%nc"))
        );
    }

    @Test
    void caseExact() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("case foo in bar) echo NO;; foo) echo YES;; esac%n")).trim(),
            CoreMatchers.equalTo("YES")
        );
    }

    @Test
    void echoWithDoubleQuotes() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("echo \"hello world\"%n")).trim(),
            CoreMatchers.equalTo("hello world")
        );
    }

    @Test
    void hereDocContent() throws Exception {
        MatcherAssert.assertThat(
            this.run(String.format("cat <<EOF%nline1%nline2%nEOF%n")).trim(),
            CoreMatchers.equalTo(String.format("line1%nline2"))
        );
    }

    private String run(final String script, final String... args) throws Exception {
        final File str = TempDirs.file(PosixConformanceTest.root, "c.sh");
        try (BufferedWriter w = Files.newBufferedWriter(str.toPath(), StandardCharsets.UTF_8)) {
            w.write(script);
        }
        final List<String> cmd = new ArrayList<>(7);
        cmd.add(String.format("%s/bin/java", System.getProperty("java.home")));
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add("io.github.andreoss.sh4j.Main");
        cmd.add(str.getAbsolutePath());
        cmd.addAll(Arrays.asList(args));
        final ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        proc.waitFor();
        return new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
    }

    private static byte[] readAllBytes(final InputStream input) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int num = input.read(buf);
        while (num != -1) {
            bos.write(buf, 0, num);
            num = input.read(buf);
        }
        return bos.toByteArray();
    }
}
