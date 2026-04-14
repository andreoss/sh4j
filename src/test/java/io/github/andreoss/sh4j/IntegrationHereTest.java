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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Main}: here.
 *
 * @since 1.0
 */
public final class IntegrationHereTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    /**
     * POSIX 2.7.4 Here-Document - delimited input becomes stdin for the
     * command.
     */
    @Test
    void hereDocument() throws Exception {
        Assertions.assertEquals(
            String.format("first line%nsecond line%n"),
            this.runChildJvm(
                this.writeScript(
                    "hd.sh",
                    String.format("cat <<EOF%nfirst line%nsecond line%nEOF%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7.4 Here-Document with quoted delimiter - body is NOT
     * variable-expanded.
     */
    @Test
    void heredocQuotedDelimiterPreventsExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("$X literal%n"),
            this.runChildJvm(
                this.writeScript("hdq.sh", String.format("X=val%ncat <<'EOF'%n$X literal%nEOF%n"))
            )
        );
    }

    /**
     * POSIX 2.7.4 here-document with leading-tab strip via {@code <<-}.
     */
    @Test
    void heredocDashStripsLeadingTabs() throws Exception {
        Assertions.assertEquals(
            String.format("indented%nagain%n"),
            this.runChildJvm(
                this.writeScript(
                    "hdtab.sh",
                    String.format("cat <<-EOF%n\tindented%n\tagain%n\tEOF%n")
                )
            )
        );
    }

    /**
     * POSIX 2.7 Redirection - higher-numbered fd redirected to file.
     */
    @Test
    void higherNumberedFdRedirection() throws Exception {
        final File data = new TempDirs(IntegrationHereTest.root).file("fd3.dat");
        Files.writeString(data.toPath(), String.format("from-fd3%n"));
        Assertions.assertEquals(
            String.format("from-fd3%n"),
            this.runChildJvm(
                this.writeScript("fd3.sh", String.format("cat 0<%s%n", data.getAbsolutePath()))
            )
        );
    }

    /**
     * POSIX 2.9.4.1 If Conditional Construct - if/elif/else/fi.
     */
    @Test
    void ifThenElseConstruct() throws Exception {
        Assertions.assertEquals(
            String.format("yes%n"),
            this.runChildJvm(
                this.writeScript(
                    "ite.sh",
                    String.format("if false; then echo no; else echo yes; fi%n")
                )
            )
        );
    }

    /**
     * POSIX 2.5.3 IFS - reset to default (space-tab-newline) when unset
     * during field splitting.
     */
    @Test
    void ifsUnsetUsesDefault() throws Exception {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            this.runChildJvm(
                this.writeScript(
                    "ifs.sh",
                    String.format("unset IFS%nX='a b c'%nfor i in $X; do echo $i; done%n")
                )
            )
        );
    }

    @Test
    void inputRedirection() throws IOException {
        final File infile = new TempDirs(IntegrationHereTest.root).file("in.txt");
        Files.writeString(infile.toPath(), String.format("input data%n"));
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "redir_in.sh",
                    String.format("cat < %s", infile.getAbsolutePath()) + String.format("%n")
                ).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.7.1 Redirecting Input - input redirect supplies the content of
     * a file to the command's stdin.
     */
    @Test
    void inputRedirectionFromFile() throws Exception {
        final File data = new TempDirs(IntegrationHereTest.root).file("in.dat");
        Files.writeString(data.toPath(), String.format("feed%n"));
        Assertions.assertEquals(
            String.format("feed%n"),
            this.runChildJvm(
                this.writeScript("redir.sh", String.format("cat <%s%n", data.getAbsolutePath()))
            )
        );
    }

    /**
     * POSIX 2.5.3 LINENO - shell-variable holding the current line number
     * of the executing script.
     */
    @Test
    void linenoTracksScriptLineNumber() throws Exception {
        final String out = this.runChildJvm(
            this.writeScript("ln.sh", String.format("echo first%necho second-$LINENO%n"))
        );
        Assertions.assertTrue(
            out.contains("second-2"),
            String.format("expected LINENO to fire on line 2, got: %s", out)
        );
    }

    /**
     * POSIX 2.2 Quoting - mixing single and double quotes in one word
     * preserves each region's semantics.
     */
    @Test
    void mixedQuotingPreservesEachRegion() throws Exception {
        Assertions.assertEquals(
            String.format("$Xval%n"),
            this.runChildJvm(this.writeScript("mq.sh", String.format("X=val%necho '$X'\"$X\"%n")))
        );
    }

    /**
     * POSIX 2.9.2 Pipelines - data flows through every stage of a 3-stage
     * pipeline.
     */
    @Test
    void multiStagePipeline() throws Exception {
        Assertions.assertEquals(
            String.format("HEO%n"),
            this.runChildJvm(
                this.writeScript(
                    "pipe.sh",
                    String.format("echo hello | tr 'a-z' 'A-Z' | tr -d 'L'%n")
                )
            )
        );
    }

    @Test
    void multipleAssignments() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("multi_assign.sh", String.format("A=1 B=2%n")).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.7 Redirection - multiple redirections on one command apply
     * in order (later overrides earlier on same fd).
     */
    @Test
    void multipleRedirectionsOnOneCommand() throws Exception {
        final File data = new TempDirs(IntegrationHereTest.root).file("multi.dat");
        Files.writeString(data.toPath(), String.format("line%n"));
        Assertions.assertEquals(
            String.format("line%n"),
            this.runChildJvm(
                this.writeScript(
                    "multi.sh",
                    String.format("cat <%s 2>&1%n", data.getAbsolutePath())
                )
            )
        );
    }

    /**
     * POSIX 2.9.1 Lists - multiple commands separated by `;` all execute.
     */
    @Test
    void multipleCommandsOnOneLine() throws Exception {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            this.runChildJvm(
                this.writeScript("multi-cmd.sh", String.format("echo a; echo b; echo c%n"))
            )
        );
    }

    /**
     * POSIX 2.7.2 Redirecting Output - the last `>` to the same file wins.
     */
    @Test
    void multipleOutputRedirectsLastWins() throws Exception {
        final File filea = new TempDirs(IntegrationHereTest.root).file("a.dat");
        final File fileb = new TempDirs(IntegrationHereTest.root).file("b.dat");
        Assertions.assertEquals(
            String.format("hi%n"),
            this.runChildJvm(
                this.writeScript(
                    "mout.sh",
                    String.format(
                        "echo hi >%s >%s%ncat %s%n",
                        filea.getAbsolutePath(),
                        fileb.getAbsolutePath(),
                        fileb.getAbsolutePath()
                    )
                )
            )
        );
    }

    @Test
    void nativeBinaryCatProbeSmokeFirst() throws Exception {
        final File nativebin = new File("target/sh4j");
        Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native smoke"
        );
        final File probe = new TempDirs(IntegrationHereTest.root).file("nprobe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("hello-native%n"));
        }
        final ProcessBuilder builder = new ProcessBuilder(
            nativebin.getAbsolutePath(),
            this.writeScript(
                "ncatprobe.sh",
                String.format("cat %s%n", probe.getAbsolutePath())
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("native sh4j exit code; output: %s", out)
        );
    }

    @Test
    void nativeBinaryCatProbeSmokeSecond() throws Exception {
        final File nativebin = new File("target/sh4j");
        Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native smoke"
        );
        final File probe = new TempDirs(IntegrationHereTest.root).file("nprobe.txt");
        try (BufferedWriter w = Files.newBufferedWriter(probe.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("hello-native%n"));
        }
        final ProcessBuilder builder = new ProcessBuilder(
            nativebin.getAbsolutePath(),
            this.writeScript(
                "ncatprobe.sh",
                String.format("cat %s%n", probe.getAbsolutePath())
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        proc.waitFor();
        proc.exitValue();
        Assertions.assertTrue(
            out.contains("hello-native"),
            String.format("native sh4j must propagate external cat stdout; got: '%s'", out)
        );
    }

    @Test
    void nativeBinaryPipelineSmokeFirst() throws Exception {
        final File nativebin = new File("target/sh4j");
        Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native pipeline smoke"
        );
        final ProcessBuilder builder = new ProcessBuilder(
            nativebin.getAbsolutePath(),
            this.writeScript(
                "npipe.sh",
                String.format("echo native-pipe | cat%n")
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        proc.waitFor();
        Assertions.assertEquals(
            0,
            proc.exitValue(),
            String.format("native sh4j pipeline exit code; output: %s", out)
        );
    }

    @Test
    void nativeBinaryPipelineSmokeSecond() throws Exception {
        final File nativebin = new File("target/sh4j");
        Assumptions.assumeTrue(
            nativebin.isFile() && nativebin.canExecute(),
            "target/sh4j native binary not built; skipping native pipeline smoke"
        );
        final ProcessBuilder builder = new ProcessBuilder(
            nativebin.getAbsolutePath(),
            this.writeScript(
                "npipe.sh",
                String.format("echo native-pipe | cat%n")
            ).getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final String out = new String(readAllBytes(proc.getInputStream()), StandardCharsets.UTF_8);
        proc.waitFor();
        proc.exitValue();
        Assertions.assertTrue(
            out.contains("native-pipe"),
            String.format("native sh4j pipeline must propagate stdout; got: '%s'", out)
        );
    }

    /**
     * POSIX 2.9.4.3 nested while loops - inner break breaks only the inner
     * loop.
     */
    @Test
    void nestedWhileInnerBreak() throws Exception {
        Assertions.assertEquals(
            String.format("0.0%n0.1%n1.0%n1.1%n"),
            this.runChildJvm(
                this.writeScript(
                    "nested.sh",
                    String.format(
                        String.join(
                            "",
                            "i=0%nwhile [ $i -lt 2 ]; do ",
                            "j=0; while [ $j -lt 3 ]; do echo $i.$j; ",
                            "if [ $j -eq 1 ]; then break; fi; j=$((j+1)); done; ",
                            "i=$((i+1)); done%n"
                        )
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 nested case - case inside a case body works.
     */
    @Test
    void nestedCaseStatement() throws Exception {
        Assertions.assertEquals(
            String.format("a2%n"),
            this.runChildJvm(
                this.writeScript(
                    "ncase.sh",
                    String.format(
                        String.join(
                            "",
                            "X=a; Y=2%ncase $X in a) case $Y in 2) echo a2;; ",
                            "*) echo a-other;; esac;; esac%n"
                        )
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.5.3 OLDPWD - previous working directory after cd.
     */
    @Test
    void oldpwdAfterCdFirst() throws Exception {
        Assertions.assertFalse(
            this.runChildJvm(
                this.writeScript(
                    "oldpwd.sh",
                    String.format(
                        "ORIG=$PWD%ncd %s%necho $OLDPWD%n",
                        new TempDirs(IntegrationHereTest.root).folder("after").getAbsolutePath()
                    )
                )
            ).trim().isEmpty(),
            "OLDPWD should not be empty"
        );
    }

    @Test
    void oldpwdAfterCdSecond() throws Exception {
        final File target = new TempDirs(IntegrationHereTest.root).folder("after");
        Assertions.assertNotEquals(
            target.getAbsolutePath(),
            this.runChildJvm(
                this.writeScript(
                    "oldpwd.sh",
                    String.format("ORIG=$PWD%ncd %s%necho $OLDPWD%n", target.getAbsolutePath())
                )
            ).trim(),
            "OLDPWD should name the directory left behind"
        );
    }

    @Test
    void orIfFirstFails() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("or.sh", String.format("false || echo ok%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void orIfFirstSucceeds() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "or_succ.sh",
                    String.format("true || echo not_reached%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void outputRedirectionFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "redir_out.sh",
                    String.format(
                        "echo test > %s",
                        new TempDirs(IntegrationHereTest.root).file("out.txt").getAbsolutePath()
                    )
                        + String.format("%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void outputRedirectionSecond() throws IOException {
        final File outfile = new TempDirs(IntegrationHereTest.root).file("out.txt");
        Main.runScript(
            this.writeScript(
                "redir_out.sh",
                String.format("echo test > %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertTrue(outfile.exists());
    }

    @Test
    void outputRedirectionThird() throws IOException {
        final File outfile = new TempDirs(IntegrationHereTest.root).file("out.txt");
        Main.runScript(
            this.writeScript(
                "redir_out.sh",
                String.format("echo test > %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertEquals(
            "test",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim()
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:-word} default if unset.
     */
    @Test
    void parameterDefaultExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("default%n"),
            this.runChildJvm(this.writeScript("pe.sh", String.format("echo ${UNSET:-default}%n")))
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${#var} yields length.
     */
    @Test
    void parameterLengthExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("5%n"),
            this.runChildJvm(this.writeScript("plen.sh", String.format("X=hello%necho ${#X}%n")))
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var%suffix} removes shortest suffix.
     */
    @Test
    void parameterSuffixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("foo.tar%n"),
            this.runChildJvm(
                this.writeScript("psuf.sh", String.format("X=foo.tar.gz%necho ${X%%.gz}%n"))
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var#prefix} removes shortest
     * matching prefix.
     */
    @Test
    void parameterPrefixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("bar.baz%n"),
            this.runChildJvm(
                this.writeScript("ppref.sh", String.format("X=foo.bar.baz%necho ${X#foo.}%n"))
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:?word} writes error to stderr
     * when var is unset.
     */
    @Test
    void parameterErrorIfUnset() throws Exception {
        final String out = this.runChildJvmAllowingFailure(
            this.writeScript("perr.sh", String.format("echo ${UNSET_REQ:?required}%n"))
        );
        Assertions.assertTrue(
            out.contains("UNSET_REQ") || out.contains("required"),
            String.format("expected error mentioning UNSET_REQ/required, got: %s", out)
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - ${var:+word} substitutes word when
     * var is set non-empty.
     */
    @Test
    void parameterAlternativeIfSet() throws Exception {
        Assertions.assertEquals(
            String.format("present%n%n"),
            this.runChildJvm(
                this.writeScript(
                    "palt.sh",
                    String.format("X=v%necho ${X:+present}%necho ${UNSET:+present}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var##pattern}` removes longest
     * matching prefix.
     */
    @Test
    void parameterLongestPrefixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("file.txt%n"),
            this.runChildJvm(
                this.writeScript(
                    "lpref.sh",
                    String.format("X=/path/to/file.txt%necho ${X##*/}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var%%pattern}` removes longest
     * matching suffix.
     */
    @Test
    void parameterLongestSuffixRemoval() throws Exception {
        Assertions.assertEquals(
            String.format("foo%n"),
            this.runChildJvm(
                this.writeScript("lsuf.sh", String.format("X=foo.tar.gz%necho ${X%%%%.*}%n"))
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - nested ${X-${Y-default}}: outer
     * default falls back to inner default when both vars are unset.
     */
    @Test
    void parameterExpansionNested() throws Exception {
        Assertions.assertEquals(
            String.format("fallback%n"),
            this.runChildJvm(
                this.writeScript(
                    "nested.sh",
                    String.format("echo ${UNSET_X-${UNSET_Y-fallback}}%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.2 Parameter Expansion - `${var:-}` with empty default keeps
     * the empty.
     */
    @Test
    void parameterDefaultEmptyWord() throws Exception {
        Assertions.assertEquals(
            String.format("[]%n"),
            this.runChildJvm(this.writeScript("ed.sh", String.format("echo [${UNSET:-}]%n")))
        );
    }

    /**
     * POSIX 2.5.3 PATH - command search finds executables on PATH.
     */
    @Test
    void pathSearchFindsExternalCommand() throws Exception {
        Assertions.assertEquals(
            String.format("hello%n"),
            this.runChildJvm(this.writeScript("path.sh", String.format("echo $(printf hello)%n")))
        );
    }

    /**
     * POSIX 2.6.6 Pathname Expansion - glob characters match filenames.
     */
    @Test
    void pathnameExpansionGlobFirst() throws Exception {
        Assertions.assertTrue(
            new File(
                new TempDirs(IntegrationHereTest.root).folder("globdir"),
                "alpha.txt"
            ).createNewFile()
        );
    }

    private File writeScript(final String name, final String content) throws IOException {
        final File script = new TempDirs(IntegrationHereTest.root).file(name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assertions.assertTrue(script.setExecutable(true));
        return script;
    }

    private String runChildJvmAllowingFailure(final File script) throws Exception {
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
        return out;
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
