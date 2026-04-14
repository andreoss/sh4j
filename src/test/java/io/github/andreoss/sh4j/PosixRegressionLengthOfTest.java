/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: length of.
 *
 * @since 1.0
 */
final class PosixRegressionLengthOfTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void lengthOfStarAndAtCountsPositionalParametersFourth() {
        Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"");
        Sh4j.INSTANCE.out("set --; echo \"${#*}\" \"${#@}\"");
        Sh4j.INSTANCE.out("v=abcd; echo \"${#v}\"");
        Assertions.assertEquals(String.format("2%n"), Sh4j.INSTANCE.out("set -- a b; echo \"$#\""));
    }

    /**
     * POSIX 2.9.4 Compound Commands: nested loop level unwinding.
     */
    @Test
    void nestedBreakAndContinueLevelsFirst() {
        Assertions.assertEquals(
            String.format("1a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 2; done; done")
        );
    }

    @Test
    void nestedBreakAndContinueLevelsSecond() {
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 2; done; done");
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do for j in a b; do echo $i$j; continue 2; done; done"
            )
        );
    }

    @Test
    void noglobDisablesPathnameExpansionFirst() throws IOException {
        Assertions.assertTrue(
            new File(
                new TempDirs(PosixRegressionLengthOfTest.root).folder("noglob"),
                "a.txt"
            ).createNewFile()
        );
    }

    @Test
    void noglobDisablesPathnameExpansionSecond() throws IOException {
        final File dir = new TempDirs(PosixRegressionLengthOfTest.root).folder("noglob");
        new File(dir, "a.txt").createNewFile();
        Assertions.assertEquals(
            String.format("*.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; set -f; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void nounsetAbortsScriptFirst() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull("set -u; echo \"$MISSING\"; echo after").exitCode()
        );
    }

    @Test
    void nounsetAbortsScriptSecond() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("set -u; echo \"$MISSING\"; echo after").stdout()
        );
    }

    @Test
    void nounsetAbortsScriptThird() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("set -u; echo \"$MISSING\"; echo after").stderr().contains(
                "MISSING"
            )
        );
    }

    @Test
    void pipelineRunsCompoundStages() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out("if true; then echo hi; fi | cat")
        );
    }

    @Test
    void pipelineStageRedirectionIsAppliedFirst() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(
                String.format(
                    "echo hi > %s | cat",
                    new TempDirs(PosixRegressionLengthOfTest.root).file("pipe.out").getAbsolutePath()
                )
            )
        );
    }

    @Test
    void pipelineStageRedirectionIsAppliedSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionLengthOfTest.root).file("pipe.out");
        Sh4j.INSTANCE.out(String.format("echo hi > %s | cat", file.getAbsolutePath()));
        Assertions.assertEquals(
            String.format("hi%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void pipelineCanFeedBuiltinRead() {
        Assertions.assertEquals(
            String.format("v=x%n"),
            Sh4j.INSTANCE.out("printf 'x\\n' | { read v; echo \"v=$v\"; }")
        );
    }

    @Test
    void pipelineStatusComesFromLastStageFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("true | false"));
    }

    @Test
    void pipelineStatusComesFromLastStageSecond() {
        Sh4j.INSTANCE.run("true | false");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("false | true"));
    }

    @Test
    void pipelineExitAndRuntimeSignalsStayInSubshellFirst() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.runFull("exit 1 | cat; echo after").stdout()
        );
    }

    @Test
    void pipelineExitAndRuntimeSignalsStayInSubshellSecond() {
        Sh4j.INSTANCE.runFull("exit 1 | cat; echo after");
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.runFull("break | cat; echo after").stdout()
        );
    }

    @Test
    void pipelineAfterSequentialListKeepsOrder() {
        Assertions.assertEquals(String.format("b%n"), Sh4j.INSTANCE.out("echo x | grep y; echo b"));
    }

    /**
     * POSIX 2.4 Reserved Words: reserved words are only reserved in command
     * position, so they stay ordinary arguments elsewhere.
     */
    @Test
    void posixReservedWordsAsArguments() {
        Assertions.assertEquals(
            String.format("if then fi%n"),
            Sh4j.INSTANCE.out("echo if then fi")
        );
    }

    /**
     * POSIX 2.10.1 Shell Grammar Lexical Conventions, part of POSIX 2.10 Shell
     * Grammar.: a {@code #} starts a
     * comment only at a token position.
     */
    @Test
    void posixLexicalConventionsFirst() {
        Assertions.assertEquals(String.format("one#two%n"), Sh4j.INSTANCE.out("echo one#two"));
    }

    @Test
    void posixLexicalConventionsSecond() {
        Sh4j.INSTANCE.out("echo one#two");
        Assertions.assertEquals(String.format("a%n"), Sh4j.INSTANCE.out("echo a  #trailing"));
    }

    /**
     * POSIX 2.10.2 Shell Grammar Rules: assignments preceding the command
     * name affect only that command's environment.
     */
    @Test
    void posixAssignmentPrefixScope() {
        Assertions.assertEquals(
            String.format("hi%n[]%n"),
            Sh4j.INSTANCE.out("x=1 echo hi; echo [$x]")
        );
    }

    /**
     * POSIX 2.11 Signals and Error Handling: {@code set -e} stops the shell
     * on a failing simple command.
     */
    @Test
    void posixSetEErrorHandlingFirst() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "set -e; false; echo nope"
            ).exitCode()
        );
    }

    @Test
    void posixSetEErrorHandlingSecond() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "set -e; false; echo nope"
            ).stdout()
        );
    }

    /**
     * POSIX 2.12 Shell Execution Environment: a subshell inherits variables
     * but its directory change does not affect the parent.
     */
    @Test
    void posixSubshellEnvironment() {
        Assertions.assertEquals(
            String.format(String.format("1%n/%n%%s%n"), Sh4j.INSTANCE.out("pwd").trim()),
            Sh4j.INSTANCE.out("v=1; (echo $v); (cd /; pwd); pwd")
        );
    }

    /**
     * POSIX 2.13.1 Patterns Matching a Single Character, part of POSIX 2.13
     * Pattern Matching Notation.: {@code ?} and
     * bracket negation with {@code !}.
     */
    @Test
    void posixSingleCharacterPatternsFirst() {
        Assertions.assertEquals(
            String.format("q%n"),
            Sh4j.INSTANCE.out("case ab in a?) echo q;; *) echo n;; esac")
        );
    }

    @Test
    void posixSingleCharacterPatternsSecond() {
        Sh4j.INSTANCE.out("case ab in a?) echo q;; *) echo n;; esac");
        Assertions.assertEquals(
            String.format("n%n"),
            Sh4j.INSTANCE.out("case z in [!abc]) echo n;; *) echo no;; esac")
        );
    }

    @Test
    void posixSingleCharacterPatternsThird() {
        Sh4j.INSTANCE.out("case ab in a?) echo q;; *) echo n;; esac");
        Sh4j.INSTANCE.out("case z in [!abc]) echo n;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("k%n"),
            Sh4j.INSTANCE.out("case b in [abc]) echo k;; *) echo no;; esac")
        );
    }

    /**
     * POSIX 2.13.2 Patterns Matching Multiple Characters: {@code *} spans
     * any run of characters.
     */
    @Test
    void posixMultipleCharacterPatterns() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out("case aaab in a*b) echo m;; *) echo no;; esac")
        );
    }

    /**
     * POSIX 2.13.3 Patterns Used for Filename Expansion: glob characters
     * expand against the shell's working directory.
     */
    @Test
    void posixFilenameExpansion() throws IOException {
        final File dir = new TempDirs(PosixRegressionLengthOfTest.root).folder("pathexpand");
        Files.write(new File(dir, "one.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "two.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "skip.log").toPath(), new byte[0]);
        Assertions.assertEquals(
            String.format("one.txt two.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void quotedAtPreservesArgumentBoundaries() {
        Assertions.assertEquals(
            String.format("[a b]%n[c]%n"),
            Sh4j.INSTANCE.out("set -- \"a b\" c; for x in \"$@\"; do echo \"[$x]\"; done")
        );
    }

    @Test
    void readConsumesOneLinePerCall() {
        Assertions.assertEquals(
            String.format("a=x b=y%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(String.format("x%ny%n").getBytes(StandardCharsets.UTF_8)),
                "read a; read b; echo \"a=$a b=$b\""
            )
        );
    }

    @Test
    void readReplyAndRawModeFirst() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("hello%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read; echo \"$REPLY\""
            )
        );
    }

    @Test
    void readReplyAndRawModeSecond() {
        Assertions.assertEquals(
            String.format("a\\tb%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read -r x; echo \"$x\""
            )
        );
    }

    @Test
    void readAtEofReturnsOne() {
        Assertions.assertEquals(
            String.format("status=1%n"),
            Sh4j.INSTANCE.stdout(new ByteArrayInputStream(new byte[0]), "read x; echo status=$?")
        );
    }

    @Test
    void readLastVariableReceivesRemainder() {
        Assertions.assertEquals(
            String.format("a=x b=y z%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("x y z%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read a b; echo \"a=$a b=$b\""
            )
        );
    }

    @Test
    void readonlyAssignmentTerminatesShellFirst() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "readonly R=1; R=2; echo after"
            ).exitCode()
        );
    }

    @Test
    void readonlyAssignmentTerminatesShellSecond() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                "readonly R=1; R=2; echo after"
            ).stdout()
        );
    }

    @Test
    void readonlySurvivesUnsetFirst() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.runFull("readonly R='a b'; unset R; echo after").exitCode()
        );
    }

    @Test
    void readonlySurvivesUnsetSecond() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("readonly R='a b'; unset R; echo after").stdout()
        );
    }

    @Test
    void readonlySurvivesUnsetThird() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("readonly R='a b'; unset R; echo after").stderr().contains(
                "readonly"
            )
        );
    }
}
