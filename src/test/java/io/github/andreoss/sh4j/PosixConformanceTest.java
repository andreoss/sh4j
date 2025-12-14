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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for PosixConformance.
 * @since 1.0
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class PosixConformanceTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void positionalParamCount() throws Exception {
        assertThat(run(String.format("echo $#%n"), "a", "b", "c").trim(), equalTo("3"));
    }

    @Test
    public void positionalParamAt() throws Exception {
        assertThat(run(String.format("echo $@%n"), "x", "y", "z").trim(), equalTo("x y z"));
    }

    @Test
    public void positionalFirstParam() throws Exception {
        assertThat(run(String.format("echo $1%n"), "hello").trim(), equalTo("hello"));
    }

    @Test
    public void paramDefaultWhenUnset() throws Exception {
        assertThat(run(String.format("echo ${UNSET_XYZ:-fallback}%n")).trim(), equalTo("fallback"));
    }

    @Test
    public void paramLengthOfVar() throws Exception {
        assertThat(run(String.format("V=hello; echo ${#V}%n")).trim(), equalTo("5"));
    }

    @Test
    public void arithmeticPrecedence() throws Exception {
        assertThat(run(String.format("echo $((1 + 2 * 3))%n")).trim(), equalTo("7"));
    }

    @Test
    public void arithmeticParens() throws Exception {
        assertThat(run(String.format("echo $(((1 + 2) * 3))%n")).trim(), equalTo("9"));
    }

    @Test
    public void commandSubstitutionDollarParen() throws Exception {
        assertThat(run(String.format("echo a$(echo b)c%n")).trim(), equalTo("abc"));
    }

    @Test
    public void backtickSubstitution() throws Exception {
        assertThat(run(String.format("echo a`echo b`c%n")).trim(), equalTo("abc"));
    }

    @Test
    public void ifThenElse() throws Exception {
        assertThat(run(String.format("if true; then echo yes; else echo no; fi%n")).trim(), equalTo("yes"));
    }

    @Test
    public void forLoopBody() throws Exception {
        assertThat(run(String.format("for i in a b c; do echo $i; done%n")).trim(), equalTo(String.format("a%nb%nc")));
    }

    @Test
    public void caseExact() throws Exception {
        assertThat(
            run(String.format("case foo in bar) echo NO;; foo) echo YES;; esac%n")).trim(),
            equalTo("YES")
        );
    }

    @Test
    public void echoWithDoubleQuotes() throws Exception {
        assertThat(run(String.format("echo \"hello world\"%n")).trim(), equalTo("hello world"));
    }

    @Test
    public void hereDocContent() throws Exception {
        assertThat(
            run(String.format("cat <<EOF%nline1%nline2%nEOF%n")).trim(),
            equalTo(String.format("line1%nline2"))
        );
    }

    private String run(final String script, final String... args) throws Exception {
        final File str = this.tempdir.newFile("c.sh");
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
        return new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
    }

    private static byte[] readAllBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n != -1) {
            bos.write(buf, 0, n);
            n = in.read(buf);
        }
        return bos.toByteArray();
    }
}
