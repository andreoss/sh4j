/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
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
 * Tests for {@link Main}: alias.
 *
 * @since 1.0
 */
final class IntegrationAliasTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    /**
     * POSIX 2.3.1 Alias Substitution - alias name expands to its body before
     * parsing the rest of the command.
     */
    @Test
    void aliasSubstitution() throws Exception {
        Assertions.assertEquals(
            String.format("hi there%n"),
            this.runChildJvm(
                this.writeScript(
                    "alias.sh",
                    String.format("alias greet='echo hi'%ngreet there%n")
                )
            )
        );
    }

    /**
     * POSIX 2.3.1 alias chains - aliases expand transitively.
     */
    @Test
    void aliasChain() throws Exception {
        Assertions.assertEquals(
            String.format("final arg%n"),
            this.runChildJvm(
                this.writeScript(
                    "alchain.sh",
                    String.format("alias a=b%nalias b='echo final'%na arg%n")
                )
            )
        );
    }

    @Test
    void andIfBothSucceed() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("and.sh", String.format("true && echo ok%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void andIfFirstFails() throws IOException {
        Assertions.assertEquals(
            1,
            Main.runScript(
                this.writeScript(
                    "and_fail.sh",
                    String.format("false && echo not_reached%n")
                ).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.9.3 Lists - AND-list short-circuits on first failure.
     */
    @Test
    void andOrListShortCircuit() throws Exception {
        Assertions.assertEquals(
            String.format("ran%n"),
            this.runChildJvm(
                this.writeScript("andor.sh", String.format("false && echo skip || echo ran%n"))
            )
        );
    }

    @Test
    void appendRedirectionFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "append.sh",
                    String.format(
                        "echo line2 >> %s",
                        new TempDirs(IntegrationAliasTest.root).file("append.txt").getAbsolutePath()
                    )
                    + String.format("%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void appendRedirectionSecond() throws IOException {
        final File outfile = new TempDirs(IntegrationAliasTest.root).file("append.txt");
        Main.runScript(
            this.writeScript(
                "append.sh",
                String.format("echo line2 >> %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertTrue(outfile.exists(), "append.txt should exist");
    }

    @Test
    void appendRedirectionThird() throws IOException {
        final File outfile = new TempDirs(IntegrationAliasTest.root).file("append.txt");
        Main.runScript(
            this.writeScript(
                "append.sh",
                String.format("echo line2 >> %s", outfile.getAbsolutePath()) + String.format("%n")
            ).getAbsolutePath()
        );
        Assertions.assertEquals(
            "line2",
            new String(Files.readAllBytes(outfile.toPath()), StandardCharsets.UTF_8).trim()
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - $((expression)).
     */
    @Test
    void arithmeticExpansion() throws Exception {
        Assertions.assertEquals(
            String.format("14%n"),
            this.runChildJvm(this.writeScript("ae.sh", String.format("echo $((2 * (3 + 4)))%n")))
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - variable reference inside $((..)).
     */
    @Test
    void arithmeticReferencesVariable() throws Exception {
        Assertions.assertEquals(
            String.format("42%n"),
            this.runChildJvm(
                this.writeScript("arith2.sh", String.format("a=6; b=7%necho $((a * b))%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - unary minus produces negative
     * integer.
     */
    @Test
    void arithmeticNegativeNumbers() throws Exception {
        Assertions.assertEquals(
            String.format("-7%n"),
            this.runChildJvm(this.writeScript("arithneg.sh", String.format("echo $((-3 - 4))%n")))
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - octal literal (leading 0).
     */
    @Test
    void arithmeticOctalLiteral() throws Exception {
        Assertions.assertEquals(
            String.format("9%n"),
            this.runChildJvm(this.writeScript("octal.sh", String.format("echo $((010 + 1))%n")))
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - bit-shift operators.
     */
    @Test
    void arithmeticBitShift() throws Exception {
        Assertions.assertEquals(
            String.format("32 128%n"),
            this.runChildJvm(
                this.writeScript("shift.sh", String.format("echo $((1 << 5)) $((1024 >> 3))%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - comparison operators yield 0/1.
     */
    @Test
    void arithmeticComparisonResult() throws Exception {
        Assertions.assertEquals(
            String.format("1 0 1%n"),
            this.runChildJvm(
                this.writeScript(
                    "cmp.sh",
                    String.format("echo $((5 > 3)) $((3 > 5)) $((5 == 5))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - ternary operator.
     */
    @Test
    void arithmeticTernaryOperator() throws Exception {
        Assertions.assertEquals(
            String.format("42 13%n"),
            this.runChildJvm(
                this.writeScript(
                    "tern.sh",
                    String.format("echo $((1 > 0 ? 42 : 13)) $((1 > 5 ? 42 : 13))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - logical AND / OR short-circuit.
     */
    @Test
    void arithmeticLogicalOps() throws Exception {
        Assertions.assertEquals(
            String.format("0 1 1%n"),
            this.runChildJvm(
                this.writeScript(
                    "logop.sh",
                    String.format("echo $((1 && 0)) $((0 || 2)) $((1 && 3))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - parentheses change grouping.
     */
    @Test
    void arithmeticParensGroup() throws Exception {
        Assertions.assertEquals(
            String.format("20%n"),
            this.runChildJvm(
                this.writeScript("arithparen.sh", String.format("echo $(( (2 + 3) * 4 ))%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - bitwise AND / OR / XOR.
     */
    @Test
    void arithmeticBitwiseOps() throws Exception {
        Assertions.assertEquals(
            String.format("2 7 5%n"),
            this.runChildJvm(
                this.writeScript(
                    "bitops.sh",
                    String.format("echo $((6 & 3)) $((6 | 1)) $((6 ^ 3))%n")
                )
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - logical NOT (!) inverts truthiness.
     */
    @Test
    void arithmeticLogicalNot() throws Exception {
        Assertions.assertEquals(
            String.format("1 0 1%n"),
            this.runChildJvm(
                this.writeScript("lnot.sh", String.format("echo $((!0)) $((!5)) $((!!7))%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - unary plus and double-negate.
     */
    @Test
    void arithmeticUnaryPlus() throws Exception {
        Assertions.assertEquals(
            String.format("5 7%n"),
            this.runChildJvm(
                this.writeScript("uplus.sh", String.format("echo $((+5)) $((- -7))%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic Expansion - mixed-sign arithmetic.
     */
    @Test
    void arithmeticMixedSign() throws Exception {
        Assertions.assertEquals(
            String.format("5 -5%n"),
            this.runChildJvm(
                this.writeScript("mix.sh", String.format("echo $((-3 + 8)) $((5 - 10))%n"))
            )
        );
    }

    /**
     * POSIX 2.6.4 Arithmetic - hex literal with `0x` prefix.
     */
    @Test
    void arithmeticHexLiteral() throws Exception {
        Assertions.assertEquals(
            String.format("31%n"),
            this.runChildJvm(this.writeScript("hex.sh", String.format("echo $((0x10 + 0xF))%n")))
        );
    }

    @Test
    void assignmentThenCommand() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript(
                    "assign.sh",
                    String.format("FOO=hello echo $FOO%n")
                ).getAbsolutePath()
            )
        );
    }

    @Test
    void assignmentOnlyPersists() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("assign_only.sh", String.format("FOO=bar%n")).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.9.1 Simple Commands - a command with assignments and no
     * command name persists the assignments in the shell environment.
     */
    @Test
    void assignmentOnlyCommandPersists() throws Exception {
        Assertions.assertEquals(
            String.format("[set]%n"),
            this.runChildJvm(
                this.writeScript("assignonly.sh", String.format("FOO=set%necho [$FOO]%n"))
            )
        );
    }

    @Test
    void backgroundAmpersand() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("bg.sh", String.format("sleep 0 &%n")).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.2.1 Escape Character - backslash quotes the next character so it
     * loses any special meaning.
     */
    @Test
    void backslashEscapesDollar() throws Exception {
        Assertions.assertEquals(
            String.format("$X%n"),
            this.runChildJvm(this.writeScript("bs.sh", String.format("X=val%necho \\$X%n")))
        );
    }

    /**
     * POSIX 2.2.1 Escape Character - backslash-newline outside quotes is
     * a line continuation, joining the two halves into one word.
     */
    @Test
    void backslashNewlineIsLineContinuation() throws Exception {
        Assertions.assertEquals(
            String.format("onetwo%n"),
            this.runChildJvm(this.writeScript("cont.sh", String.format("echo one\\%ntwo%n")))
        );
    }

    /**
     * POSIX 2.2.1 Escape Character - `\\n` outside quotes preserves the
     * literal 'n' character.
     */
    @Test
    void backslashPreservesNextChar() throws Exception {
        Assertions.assertEquals(
            String.format("anb%n"),
            this.runChildJvm(this.writeScript("esc.sh", String.format("echo a\\nb%n")))
        );
    }

    /**
     * POSIX 2.6.3 Command Substitution - backtick form is equivalent to $(...).
     */
    @Test
    void backtickCommandSubstitution() throws Exception {
        Assertions.assertEquals(
            String.format("got=hello%n"),
            this.runChildJvm(
                this.writeScript("btick.sh", String.format("X=`echo hello`%necho got=$X%n"))
            )
        );
    }

    @Test
    void bangInvertsExit() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runScript(
                this.writeScript("bang.sh", String.format("! false%n")).getAbsolutePath()
            )
        );
    }

    @Test
    void bangInvertsTrueToFalse() throws IOException {
        Assertions.assertEquals(
            1,
            Main.runScript(
                this.writeScript("bang_true.sh", String.format("! true%n")).getAbsolutePath()
            )
        );
    }

    /**
     * POSIX 2.8.2 Exit Status - `!` inverts the exit status of the pipeline.
     */
    @Test
    void bangInvertsPipelineExit() throws Exception {
        Assertions.assertEquals(
            String.format("1%n"),
            this.runChildJvm(this.writeScript("bang.sh", String.format("! true%necho $?%n")))
        );
    }

    /**
     * POSIX 2.9.2 Pipelines - leading `!` inverts the pipeline exit status.
     */
    @Test
    void bangBeforePipelineInverts() throws Exception {
        Assertions.assertEquals(
            String.format("0%n1%n"),
            this.runChildJvm(
                this.writeScript(
                    "bangpipe.sh",
                    String.format("! false; echo $?%n! true; echo $?%n")
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.6 Brace Group - { list; } groups commands without subshell.
     */
    @Test
    void braceGroupSharesEnvironment() throws Exception {
        Assertions.assertEquals(
            String.format("inside%n"),
            this.runChildJvm(
                this.writeScript("brace.sh", String.format("{ X=inside; }%necho $X%n"))
            )
        );
    }

    /**
     * POSIX 2.14 break N - breaks out of N levels of loops.
     */
    @Test
    void breakNExitsMultipleLoops() throws Exception {
        Assertions.assertEquals(
            String.format("1.a%nafter%n"),
            this.runChildJvm(
                this.writeScript(
                    "brk2.sh",
                    String.format(
                        String.join(
                            "",
                            "for i in 1 2; do for j in a b c; do ",
                            "if [ $j = b ]; then break 2; fi; ",
                            "echo $i.$j; done; done%necho after%n"
                        )
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case Conditional Construct - case/in/esac.
     */
    @Test
    void caseConditionalConstruct() throws Exception {
        Assertions.assertEquals(
            String.format("b%n"),
            this.runChildJvm(
                this.writeScript(
                    "case.sh",
                    String.format(
                        String.join(
                            "",
                            "x=banana%ncase $x in%n",
                            "  apple) echo a;;%n  banana) echo b;;%nesac%n"
                        )
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case Patterns - `|` separates alternative patterns.
     */
    @Test
    void caseAlternativePatterns() throws Exception {
        Assertions.assertEquals(
            String.format("letter%nletter%nother%nother%n"),
            this.runChildJvm(
                this.writeScript(
                    "casealt.sh",
                    String.format(
                        String.join(
                            "",
                            "for w in a b c d; do case $w in ",
                            "a|b) echo letter;; *) echo other;; esac; done%n"
                        )
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case Patterns - `?` matches any single character.
     */
    @Test
    void caseQuestionMarkMatchesSingleChar() throws Exception {
        Assertions.assertEquals(
            String.format("two%nnot-two%nnot-two%n"),
            this.runChildJvm(
                this.writeScript(
                    "caseq.sh",
                    String.format(
                        String.join(
                            "",
                            "for w in ab abc a; do case $w in ",
                            "??) echo two;; *) echo not-two;; esac; done%n"
                        )
                    )
                )
            )
        );
    }

    /**
     * POSIX 2.9.4.4 Case - {@code *)} matches anything as the default arm.
     */
    @Test
    void caseStarDefaultMatches() throws Exception {
        Assertions.assertEquals(
            String.format("a%nd%nd%n"),
            this.runChildJvm(
                this.writeScript(
                    "casestar.sh",
                    String.format(
                        String.join(
                            "",
                            "for w in apple banana cherry; do ",
                            "case $w in apple) echo a;; *) echo d;; esac; done%n"
                        )
                    )
                )
            )
        );
    }

    private File writeScript(final String name, final String content) throws IOException {
        final File script = new TempDirs(IntegrationAliasTest.root).file(name);
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(content);
        }
        Assertions.assertTrue(script.setExecutable(true));
        return script;
    }

    private String runChildJvm(final File script) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(
            String.format("%s/bin/java", System.getProperty("java.home")),
            "-XX:-UsePerfData",
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
        return input.readAllBytes();
    }
}
