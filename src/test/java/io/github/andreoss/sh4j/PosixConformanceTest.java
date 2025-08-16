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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
        Assert.assertTrue(run(String.format("echo $#%n"), "a", "b", "c").trim().equals("3"));
    }

    @Test
    public void positionalParamAt() throws Exception {
        Assert.assertTrue(
            run(String.format("echo $@%n"), "x", "y", "z").trim().equals("x y z")
        );
    }

    @Test
    public void positionalFirstParam() throws Exception {
        Assert.assertTrue(this.run(String.format("echo $1%n"), "hello").trim().equals("hello"));
    }

    @Test
    public void paramDefaultWhenUnset() throws Exception {
        Assert.assertTrue(
            run(String.format("echo ${UNSET_XYZ:-fallback}%n")).trim().equals("fallback")
        );
    }

    @Test
    public void paramLengthOfVar() throws Exception {
        Assert.assertTrue(run(String.format("V=hello; echo ${#V}%n")).trim().equals("5"));
    }

    @Test
    public void arithmeticPrecedence() throws Exception {
        Assert.assertTrue(run(String.format("echo $((1 + 2 * 3))%n")).trim().equals("7"));
    }

    @Test
    public void arithmeticParens() throws Exception {
        Assert.assertTrue(run(String.format("echo $(((1 + 2) * 3))%n")).trim().equals("9"));
    }

    @Test
    public void commandSubstitutionDollarParen() throws Exception {
        Assert.assertTrue(run(String.format("echo a$(echo b)c%n")).trim().equals("abc"));
    }

    @Test
    public void backtickSubstitution() throws Exception {
        Assert.assertTrue(run(String.format("echo a`echo b`c%n")).trim().equals("abc"));
    }

    @Test
    public void ifThenElse() throws Exception {
        Assert.assertTrue(
            run(String.format("if true; then echo yes; else echo no; fi%n")).trim().equals("yes")
        );
    }

    @Test
    public void forLoopBody() throws Exception {
        Assert.assertEquals(
            String.format("a%nb%nc"),
            run(String.format("for i in a b c; do echo $i; done%n")).trim()
        );
    }

    @Test
    public void caseExact() throws Exception {
        Assert.assertTrue(
            run(
                String.format("case foo in bar) echo NO;; foo) echo YES;; esac%n")
            ).trim().equals("YES")
        );
    }

    @Test
    public void echoWithDoubleQuotes() throws Exception {
        Assert.assertTrue(
            run(String.format("echo \"hello world\"%n")).trim().equals("hello world")
        );
    }

    @Test
    public void hereDocContent() throws Exception {
        Assert.assertEquals(
            String.format("line1%nline2"),
            run(String.format("cat <<EOF%nline1%nline2%nEOF%n")).trim()
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
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
