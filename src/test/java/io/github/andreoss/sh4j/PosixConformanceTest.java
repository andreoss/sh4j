/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PosixConformanceTest {
    @TempDir
    java.nio.file.Path tempDir;

    private String run(String script, String... args) throws Exception {
        File s = tempDir.resolve("c.sh").toFile();
        try (FileWriter w = new FileWriter(s)) { w.write(script); }
        String javaBin = System.getProperty("java.home") + "/bin/java";
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add("io.github.andreoss.sh4j.Main");
        cmd.add(s.getAbsolutePath());
        for (String a : args) cmd.add(a);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return out;
    }

    @Test
    void positionalParamCount() throws Exception {
        assertTrue(run("echo $#\n", "a", "b", "c").trim().equals("3"));
    }

    @Test
    void positionalParamAt() throws Exception {
        assertTrue(run("echo $@\n", "x", "y", "z").trim().equals("x y z"));
    }

    @Test
    void positionalFirstParam() throws Exception {
        assertTrue(run("echo $1\n", "hello").trim().equals("hello"));
    }

    @Test
    void paramDefaultWhenUnset() throws Exception {
        assertTrue(run("echo ${UNSET_XYZ:-fallback}\n").trim().equals("fallback"));
    }

    @Test
    void paramLengthOfVar() throws Exception {
        assertTrue(run("V=hello; echo ${#V}\n").trim().equals("5"));
    }

    @Test
    void arithmeticPrecedence() throws Exception {
        assertTrue(run("echo $((1 + 2 * 3))\n").trim().equals("7"));
    }

    @Test
    void arithmeticParens() throws Exception {
        assertTrue(run("echo $(((1 + 2) * 3))\n").trim().equals("9"));
    }

    @Test
    void commandSubstitutionDollarParen() throws Exception {
        assertTrue(run("echo a$(echo b)c\n").trim().equals("abc"));
    }

    @Test
    void backtickSubstitution() throws Exception {
        assertTrue(run("echo a`echo b`c\n").trim().equals("abc"));
    }

    @Test
    void ifThenElse() throws Exception {
        assertTrue(run("if true; then echo yes; else echo no; fi\n").trim().equals("yes"));
    }

    @Test
    void forLoopBody() throws Exception {
        String out = run("for i in a b c; do echo $i; done\n").trim();
        assertEquals("a\nb\nc", out);
    }

    @Test
    void caseExact() throws Exception {
        assertTrue(run("case foo in bar) echo NO;; foo) echo YES;; esac\n").trim().equals("YES"));
    }

    @Test
    void echoWithDoubleQuotes() throws Exception {
        assertTrue(run("echo \"hello world\"\n").trim().equals("hello world"));
    }

    @Test
    void hereDocContent() throws Exception {
        String out = run("cat <<EOF\nline1\nline2\nEOF\n").trim();
        assertEquals("line1\nline2", out);
    }
}
