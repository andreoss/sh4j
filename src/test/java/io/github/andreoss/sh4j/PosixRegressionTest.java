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
 * Regression tests for the shell semantics repaired after the Step 37
 * conformance audit.
 *
 * @since 1.0
 */
final class PosixRegressionTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void commentStartsAtTokenPositionOnlyFirstCase() {
        Assertions.assertEquals(String.format("a%n"), Sh4j.INSTANCE.out("echo a #c"));
    }

    @Test
    void commentStartsAtTokenPositionOnlySecondCase() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out(String.format("#lead%necho b"))
        );
    }

    @Test
    void commentStartsAtTokenPositionOnlyThirdCase() {
        Assertions.assertEquals(String.format("one#two%n"), Sh4j.INSTANCE.out("echo one#two"));
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsFirst() {
        Assertions.assertEquals(
            String.format("notclass%n"),
            Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac")
        );
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsSecond() {
        Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("klass%n"),
            Sh4j.INSTANCE.out("case abc in [abc]*) echo klass;; *) echo no;; esac")
        );
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsThird() {
        Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac");
        Sh4j.INSTANCE.out("case abc in [abc]*) echo klass;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("if then fi%n"),
            Sh4j.INSTANCE.out("echo if then fi")
        );
    }

    @Test
    void bracketNegationAndReservedWordsAsArgumentsFourth() {
        Sh4j.INSTANCE.out("case zzz in [!abc]*) echo notclass;; *) echo no;; esac");
        Sh4j.INSTANCE.out("case abc in [abc]*) echo klass;; *) echo no;; esac");
        Sh4j.INSTANCE.out("echo if then fi");
        Assertions.assertEquals(
            String.format("multi%n"),
            Sh4j.INSTANCE.out("case aaab in a*b) echo multi;; esac")
        );
    }

    /**
     * POSIX 2.7.3 Appending Redirected Output and POSIX 2.7 Redirection.
     */
    @Test
    void appendRedirectAndAssignmentPrefixFirst() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("append.txt");
        Assertions.assertEquals(
            String.format("a%nb%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "echo a > %s; echo b >> %s; cat %s",
                    file.getAbsolutePath(), file.getAbsolutePath(),
                    file.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void appendRedirectAndAssignmentPrefixSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("append.txt");
        Sh4j.INSTANCE.out(
            String.format(
                "echo a > %s; echo b >> %s; cat %s",
                file.getAbsolutePath(), file.getAbsolutePath(),
                file.getAbsolutePath()
            )
        );
        Assertions.assertEquals(
            String.format("hi%n[]%n"),
            Sh4j.INSTANCE.out("x=1 echo hi; echo [$x]")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsFirst() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsSecond() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Assertions.assertEquals(
            String.format("count=2%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsThird() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsFourth() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac");
        Assertions.assertEquals(
            String.format("lit%n"),
            Sh4j.INSTANCE.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac")
        );
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsFifth() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac");
        Sh4j.INSTANCE.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac");
        Assertions.assertEquals(String.format("after%n"), Sh4j.INSTANCE.out("break; echo after"));
    }

    @Test
    void shiftBeyondCountAndQuotedCasePatternsSixth() {
        Sh4j.INSTANCE.out("set -- a; shift 5; echo \"rc=$?\"");
        Sh4j.INSTANCE.out("set -- a b c; shift 1; echo \"count=$#\"");
        Sh4j.INSTANCE.out("v='a*'; case aX in \"$v\") echo lit;; *) echo no;; esac");
        Sh4j.INSTANCE.out("v='a*'; case 'a*' in $v) echo lit;; *) echo no;; esac");
        Sh4j.INSTANCE.out("break; echo after");
        Assertions.assertEquals(
            String.format("1a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 2; done; done")
        );
    }

    @Test
    void readonlyAssignmentTerminatesShellFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "readonly R=1; R=2; echo after"
        ).exitCode());
    }

    @Test
    void readonlyAssignmentTerminatesShellSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "readonly R=1; R=2; echo after"
        ).stdout());
    }

    @Test
    void globExpandsDirectoryPrefix() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root).folder("sub");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "b.log").toPath(), new byte[0]);
        Assertions.assertEquals(
            String.format(String.format("%%s/a.txt%n"), dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(String.format("echo %s/*.txt", dir.getAbsolutePath()))
        );
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
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "set -e; false; echo nope"
        ).exitCode());
    }

    @Test
    void posixSetEErrorHandlingSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "set -e; false; echo nope"
        ).stdout());
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
        final File dir = new TempDirs(PosixRegressionTest.root)
            .folder("pathexpand");
        Files.write(new File(dir, "one.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "two.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "skip.log").toPath(), new byte[0]);
        Assertions.assertEquals(
            String.format("one.txt two.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void redirectionsResolveAgainstShellCwdFirst() throws IOException {
        Assertions.assertEquals(
            String.format("seed%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"", new TempDirs(PosixRegressionTest.root)
                                                                                                                .folder("cwdtest").getAbsolutePath()
                )
            )
        );
    }

    @Test
    void redirectionsResolveAgainstShellCwdSecond() throws IOException {
        final String base = new TempDirs(PosixRegressionTest.root)
                                .folder("cwdtest").getAbsolutePath();
        Sh4j.INSTANCE.out(
            String.format(
                "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"", base
            )
        );
        Assertions.assertEquals(
            String.format("fd-ok%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; exec 3>fdout.txt; echo fd-ok >&3; exec 3>&-; cat fdout.txt", base
                )
            )
        );
    }

    @Test
    void redirectionsResolveAgainstShellCwdThird() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root)
            .folder("cwdtest");
        final String base = dir.getAbsolutePath();
        Sh4j.INSTANCE.out(
            String.format(
                "cd %s; printf 'seed\\n' > rel.txt; read v < rel.txt; echo \"$v\"", base
            )
        );
        Sh4j.INSTANCE.out(
            String.format(
                "cd %s; exec 3>fdout.txt; echo fd-ok >&3; exec 3>&-; cat fdout.txt", base
            )
        );
        Assertions.assertEquals(
            String.format("fd-ok%n"),
            new String(
                Files.readAllBytes(new File(dir, "fdout.txt").toPath()),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void execRedirectionFailureTerminatesShellFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        ).exitCode());
    }

    @Test
    void execRedirectionFailureTerminatesShellSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        ).stdout());
    }

    @Test
    void execRedirectionFailureTerminatesShellThird() {
        final Sh4j.Result input = Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec >/nonexistent-dir-xyz/f; echo after"
        ).exitCode());
    }

    @Test
    void execRedirectionFailureTerminatesShellFourth() {
        final Sh4j.Result input = Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec 9</nonexistent-dir-xyz/f; echo after"
        );
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(new byte[0]),
            "exec >/nonexistent-dir-xyz/f; echo after"
        ).stdout());
    }

    @Test
    void descriptorRedirectionsPersistAcrossCommandsFirst() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root).file("fd.txt");
        Files.write(file.toPath(), String.format("seed%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("v=seed%n"),
            Sh4j.INSTANCE.out(
                String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", file.getAbsolutePath())
            )
        );
    }

    @Test
    void descriptorRedirectionsPersistAcrossCommandsSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root).file("fd.txt");
        Files.write(file.toPath(), String.format("seed%n").getBytes(StandardCharsets.UTF_8));
        final String path = file.getAbsolutePath();
        Sh4j.INSTANCE.out(
            String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", path)
        );
        Assertions.assertEquals(
            String.format("w=seed%n"),
            Sh4j.INSTANCE.out(
                String.format("exec 4<%s; read w <&4; echo \"w=$w\"", path)
            )
        );
    }

    @Test
    void descriptorRedirectionsPersistAcrossCommandsThird() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root).file("fd.txt");
        Files.write(file.toPath(), String.format("seed%n").getBytes(StandardCharsets.UTF_8));
        final String path = file.getAbsolutePath();
        Sh4j.INSTANCE.out(
            String.format("exec 3<>%s; read v <&3; echo \"v=$v\"", path)
        );
        Sh4j.INSTANCE.out(
            String.format("exec 4<%s; read w <&4; echo \"w=$w\"", path)
        );
        Assertions.assertEquals(
            String.format("y=[]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(new byte[0]),
                String.format("exec 5<%s; exec 5<&-; read y <&5; echo \"y=[$y]\"", path)
            ).stdout()
        );
    }

    /**
     * POSIX 2.8 Exit Status and POSIX 2.9 Shell Commands: `exit` ends the
     * list and propagates its status.
     */
    @Test
    void exitStopsListAndPropagatesStatusFirst() {
        Assertions.assertEquals(3, Sh4j.INSTANCE.runFull("exit 3; echo after").exitCode());
    }

    @Test
    void exitStopsListAndPropagatesStatusSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull("exit 3; echo after").stdout());
    }

    @Test
    void execRedirectionPersistsFirst() throws IOException {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull(
            String.format("exec > %s; echo redirected", new TempDirs(PosixRegressionTest.root)
                                                            .file("exec.out").getAbsolutePath())
        ).exitCode());
    }

    @Test
    void execRedirectionPersistsSecond() throws IOException {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            String.format("exec > %s; echo redirected", new TempDirs(PosixRegressionTest.root)
                                                            .file("exec.out").getAbsolutePath())
        ).stdout());
    }

    @Test
    void execRedirectionPersistsThird() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("exec.out");
        final Sh4j.Result result = Sh4j.INSTANCE.runFull(
            String.format("exec > %s; echo redirected", file.getAbsolutePath())
        );
        Assertions.assertEquals(
            String.format("redirected%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void execCommandDoesNotResumeShellFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("exec echo hi; echo after").exitCode());
    }

    @Test
    void execCommandDoesNotResumeShellSecond() {
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.runFull("exec echo hi; echo after").stdout());
    }

    @Test
    void subshellCdDoesNotChangeParentCwd() {
        Assertions.assertEquals(
            String.format("/tmp%n"),
            Sh4j.INSTANCE.out(
                "cd /tmp; (cd /); pwd"
            )
        );
    }

    @Test
    void shellVariablesAreNotExported() {
        Assertions.assertEquals(
            String.format("unset%n"),
            Sh4j.INSTANCE.out(
                "X=secret; sh -c 'echo ${X-unset}'"
            )
        );
    }

    @Test
    void exportedValueLosesQuotes() {
        Assertions.assertEquals(
            String.format("x y%n"),
            Sh4j.INSTANCE.out(
                "export E='x y'; sh -c 'printf \"%s\\n\" \"$E\"'"
            )
        );
    }

    @Test
    void readonlySurvivesUnsetFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            "readonly R='a b'; unset R; echo after"
        ).exitCode());
    }

    @Test
    void readonlySurvivesUnsetSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            "readonly R='a b'; unset R; echo after"
        ).stdout());
    }

    @Test
    void readonlySurvivesUnsetThird() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            "readonly R='a b'; unset R; echo after"
        ).stderr().contains("readonly"));
    }

    @Test
    void setPlusEClearsErrexit() {
        Assertions.assertEquals(
            String.format("alive%n"),
            Sh4j.INSTANCE.out("set -e; set +e; false; echo alive")
        );
    }

    @Test
    void nounsetAbortsScriptFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            "set -u; echo \"$MISSING\"; echo after"
        ).exitCode());
    }

    @Test
    void nounsetAbortsScriptSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            "set -u; echo \"$MISSING\"; echo after"
        ).stdout());
    }

    @Test
    void nounsetAbortsScriptThird() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            "set -u; echo \"$MISSING\"; echo after"
        ).stderr().contains("MISSING"));
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
    void quotedAtPreservesArgumentBoundaries() {
        Assertions.assertEquals(
            String.format("[a b]%n[c]%n"),
            Sh4j.INSTANCE.out("set -- \"a b\" c; for x in \"$@\"; do echo \"[$x]\"; done")
        );
    }

    @Test
    void starJoinsWithFirstIfsCharacter() {
        Assertions.assertEquals(
            String.format("a:b%n"),
            Sh4j.INSTANCE.out("set -- a b; IFS=:; echo \"$*\"")
        );
    }

    @Test
    void commandSubstitutionStripsAllTrailingNewlines() {
        Assertions.assertEquals(
            String.format("<a>%n"),
            Sh4j.INSTANCE.out("X=$(printf 'a\\n\\n\\n'); printf '<%s>\\n' \"$X\"")
        );
    }

    @Test
    void backtickInsideSingleQuotesIsLiteral() {
        Assertions.assertEquals(String.format("`date`%n"), Sh4j.INSTANCE.out("echo '`date`'"));
    }

    @Test
    void casePatternUndergoesExpansion() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("p='h*'; case hi in $p) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void globSkipsDotfilesAndSortsFirst() throws IOException {
        Assertions.assertTrue(new File(new TempDirs(PosixRegressionTest.root)
        .folder("globfix"), ".hidden").createNewFile());
    }

    @Test
    void globSkipsDotfilesAndSortsSecond() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root)
            .folder("globfix");
        new File(dir, ".hidden").createNewFile();
        Assertions.assertTrue(new File(dir, "b.txt").createNewFile());
    }

    @Test
    void globSkipsDotfilesAndSortsThird() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root)
            .folder("globfix");
        new File(dir, ".hidden").createNewFile();
        new File(dir, "b.txt").createNewFile();
        Assertions.assertTrue(new File(dir, "a.txt").createNewFile());
    }

    @Test
    void globSkipsDotfilesAndSortsFourth() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root)
            .folder("globfix");
        new File(dir, ".hidden").createNewFile();
        new File(dir, "b.txt").createNewFile();
        new File(dir, "a.txt").createNewFile();
        Assertions.assertEquals(
            String.format("a.txt b.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void noglobDisablesPathnameExpansionFirst() throws IOException {
        Assertions.assertTrue(new File(new TempDirs(PosixRegressionTest.root)
        .folder("noglob"), "a.txt").createNewFile());
    }

    @Test
    void noglobDisablesPathnameExpansionSecond() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root)
            .folder("noglob");
        new File(dir, "a.txt").createNewFile();
        Assertions.assertEquals(
            String.format("*.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; set -f; echo *.txt", dir.getAbsolutePath()))
        );
    }

    @Test
    void typeRecognisesReadBuiltin() {
        Assertions.assertEquals(
            String.format("read is a shell built-in%n"),
            Sh4j.INSTANCE.out("type read")
        );
    }

    @Test
    void commandBypassesFunctionLookupFirst() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.runFull(
            "f() { echo function; }; command f"
        ).exitCode());
    }

    @Test
    void commandBypassesFunctionLookupSecond() {
        Assertions.assertEquals("", Sh4j.INSTANCE.runFull(
            "f() { echo function; }; command f"
        ).stdout());
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
                String.format("echo hi > %s | cat", new TempDirs(PosixRegressionTest.root)
                                                        .file("pipe.out").getAbsolutePath())
            )
        );
    }

    @Test
    void pipelineStageRedirectionIsAppliedSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("pipe.out");
        Sh4j.INSTANCE.out(
            String.format("echo hi > %s | cat", file.getAbsolutePath())
        );
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
    void backgroundJobInheritsFunctionsAndWaitJoinsIt() {
        Assertions.assertEquals(
            String.format("F%n"),
            Sh4j.INSTANCE.out("f() { echo F; }; f & wait")
        );
    }

    @Test
    void dollarBangIsNotAnOsPid() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.runFull("sleep 0.1 & kill $!").exitCode());
    }

    @Test
    void trapDotAndTimesWorkFirst() throws IOException {
        final File lib = new TempDirs(PosixRegressionTest.root).file("lib.sh");
        Files.write(lib.toPath(), String.format("LIB=ok%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            String.format(
                "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                lib.getAbsolutePath()
            )
        ).stdout().contains("lib=ok"));
    }

    @Test
    void trapDotAndTimesWorkSecond() throws IOException {
        final File lib = new TempDirs(PosixRegressionTest.root).file("lib.sh");
        Files.write(lib.toPath(), String.format("LIB=ok%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            String.format(
                "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                lib.getAbsolutePath()
            )
        ).stdout().contains("0m0.000s"));
    }

    @Test
    void trapDotAndTimesWorkThird() throws IOException {
        final File lib = new TempDirs(PosixRegressionTest.root).file("lib.sh");
        Files.write(lib.toPath(), String.format("LIB=ok%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            String.format(
                "trap 'echo caught' EXIT; . %s; echo \"lib=$LIB\"; times",
                lib.getAbsolutePath()
            )
        ).stdout().contains("caught"));
    }

    @Test
    void externalCommandRunsInExecutorCwd() throws IOException {
        final File dir = new TempDirs(PosixRegressionTest.root).folder("cwd");
        Assertions.assertEquals(
            dir.getCanonicalPath() + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; sh -c pwd", dir.getAbsolutePath()))
        );
    }

    @Test
    void xtraceWritesCommandToStderrFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("set -x; echo hi").stderr().contains("+ echo hi"));
    }

    @Test
    void xtraceWritesCommandToStderrSecond() {
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.runFull("set -x; echo hi").stdout());
    }

    @Test
    void execMissingCommandReturnsError() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.runFull("exec nonexistent_exec_xyz").exitCode());
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersFirst() {
        Assertions.assertEquals(
            String.format("3 3%n"),
            Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"")
        );
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersSecond() {
        Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"");
        Assertions.assertEquals(
            String.format("0 0%n"),
            Sh4j.INSTANCE.out("set --; echo \"${#*}\" \"${#@}\"")
        );
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersThird() {
        Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"");
        Sh4j.INSTANCE.out("set --; echo \"${#*}\" \"${#@}\"");
        Assertions.assertEquals(String.format("4%n"), Sh4j.INSTANCE.out("v=abcd; echo \"${#v}\""));
    }

    @Test
    void lengthOfStarAndAtCountsPositionalParametersFourth() {
        Sh4j.INSTANCE.out("set -- a b c; echo \"${#*}\" \"${#@}\"");
        Sh4j.INSTANCE.out("set --; echo \"${#*}\" \"${#@}\"");
        Sh4j.INSTANCE.out("v=abcd; echo \"${#v}\"");
        Assertions.assertEquals(String.format("2%n"), Sh4j.INSTANCE.out("set -- a b; echo \"$#\""));
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithFirst() {
        Assertions.assertEquals(7, Sh4j.INSTANCE.runFull(
            "trap \"echo st=$?\" EXIT; exit 7"
        ).exitCode());
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithSecond() {
        Assertions.assertEquals(String.format("st=7%n"), Sh4j.INSTANCE.runFull(
            "trap \"echo st=$?\" EXIT; exit 7"
        ).stdout());
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithThird() {
        final Sh4j.Result exited = Sh4j.INSTANCE.runFull(
            "trap \"echo st=$?\" EXIT; exit 7"
        );
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(
            "trap 'echo exit=$?' EXIT; false"
        ).exitCode());
    }

    @Test
    void exitTrapSeesTheStatusTheShellExitsWithFourth() {
        final Sh4j.Result exited = Sh4j.INSTANCE.runFull(
            "trap \"echo st=$?\" EXIT; exit 7"
        );
        Assertions.assertEquals(String.format("exit=1%n"), Sh4j.INSTANCE.runFull(
            "trap 'echo exit=$?' EXIT; false"
        ).stdout());
    }

    @Test
    void trapListingAndRemovalFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
        ).stdout().contains("trap -- 'echo t' EXIT"));
    }

    @Test
    void trapListingAndRemovalSecond() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(
            "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
        ).stdout().contains("body"));
    }

    @Test
    void trapListingAndRemovalThird() {
        Assertions.assertFalse(Sh4j.INSTANCE.runFull(
            "trap 'echo t' EXIT; trap; trap - EXIT; echo body"
        ).stdout().contains(String.format("t%n")));
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileFirst() throws IOException {
        final Path returned = new TempDirs(PosixRegressionTest.root)
            .file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull(
            String.format(". %s; echo \"rc=$?\"", returned)
        ).exitCode());
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileSecond() throws IOException {
        final Path returned = new TempDirs(PosixRegressionTest.root)
            .file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(String.format("before%nrc=5%n"), Sh4j.INSTANCE.runFull(
            String.format(". %s; echo \"rc=$?\"", returned)
        ).stdout());
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileThird() throws IOException {
        final Path returned = new TempDirs(PosixRegressionTest.root)
            .file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        final Sh4j.Result result = Sh4j.INSTANCE.runFull(
            String.format(". %s; echo \"rc=$?\"", returned)
        );
        final Path exited = new TempDirs(PosixRegressionTest.root)
            .file("exit.sh").toPath();
        Files.write(
            exited,
            String.format("echo x; exit 3; echo y%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(3, Sh4j.INSTANCE.runFull(
            String.format(". %s; echo after", exited)
        ).exitCode());
    }

    @Test
    void dotReturnStopsOnlyTheSourcedFileFourth() throws IOException {
        final Path returned = new TempDirs(PosixRegressionTest.root)
            .file("ret.sh").toPath();
        Files.write(
            returned,
            String.format("echo before; return 5; echo after%n").getBytes(StandardCharsets.UTF_8)
        );
        final Sh4j.Result result = Sh4j.INSTANCE.runFull(
            String.format(". %s; echo \"rc=$?\"", returned)
        );
        final Path exited = new TempDirs(PosixRegressionTest.root)
            .file("exit.sh").toPath();
        Files.write(
            exited,
            String.format("echo x; exit 3; echo y%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(String.format("x%n"), Sh4j.INSTANCE.runFull(
            String.format(". %s; echo after", exited)
        ).stdout());
    }

    @Test
    void dotMissingFileReturnsOneFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull(". /nonexistent/library.sh").exitCode());
    }

    @Test
    void dotMissingFileReturnsOneSecond() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull(". /nonexistent/library.sh").stderr().contains("cannot open"));
    }

    @Test
    void dotSetsPositionalParameters() throws IOException {
        final File lib = new TempDirs(PosixRegressionTest.root)
            .file("args-lib.sh");
        Files.write(lib.toPath(), String.format("echo \"$1\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("arg%n"),
            Sh4j.INSTANCE.out(String.format(". %s arg", lib.getAbsolutePath()))
        );
    }

    @Test
    void killListsAndRejectsJobPidsFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("kill -l").contains("TERM"));
    }

    @Test
    void killListsAndRejectsJobPidsSecond() {
        Sh4j.INSTANCE.out("kill -l");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill notapid"));
    }

    @Test
    void killListsAndRejectsJobPidsThird() {
        Sh4j.INSTANCE.out("kill -l");
        Sh4j.INSTANCE.run("kill notapid");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("sleep 0.1 & kill $!"));
    }

    @Test
    void waitUnknownPidReturnsError() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("wait 999999"));
    }

    @Test
    void commandVerboseDescribesFunction() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("f() { :; }; command -V f").contains("f is a function")
        );
    }

    @Test
    void exportAndReadonlyPrintListsFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("export -p").contains("export PATH="));
    }

    @Test
    void exportAndReadonlyPrintListsSecond() {
        Sh4j.INSTANCE.out("export -p");
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("readonly R=1; readonly -p").contains("readonly R=1")
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
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(new byte[0]),
                "read x; echo status=$?"
            )
        );
    }

    @Test
    void functionRedirectionAndPrefixAssignmentFirst() throws IOException {
        new TempDirs(PosixRegressionTest.root).file("fn.out");
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("f() { echo $X; }; X=1 f")
        );
    }

    @Test
    void functionRedirectionAndPrefixAssignmentSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root).file("fn.out");
        Sh4j.INSTANCE.out("f() { echo $X; }; X=1 f");
        Sh4j.INSTANCE.run(String.format("f() { echo hi; }; f > %s", file.getAbsolutePath()));
        Assertions.assertEquals(
            String.format("hi%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
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
    void backgroundWaitReturnsJobStatus() {
        Assertions.assertEquals(3, Sh4j.INSTANCE.run("sh -c 'exit 3' & wait $!"));
    }

    @Test
    void setDashOOptions() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("set -o nounset; echo $MISSING_OPTION_VAR")
        );
    }

    @Test
    void subshellExitOnlyLeavesSubshell() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("(exit 3); echo after")
        );
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
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; continue 2; done; done")
        );
    }

    @Test
    void illegalLoopAndReturnDiagnosticsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("break"));
    }

    @Test
    void illegalLoopAndReturnDiagnosticsSecond() {
        Sh4j.INSTANCE.run("break");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("continue"));
    }

    @Test
    void illegalLoopAndReturnDiagnosticsThird() {
        Sh4j.INSTANCE.run("break");
        Sh4j.INSTANCE.run("continue");
        Assertions.assertEquals(5, Sh4j.INSTANCE.run("return 5"));
    }

    @Test
    void returnInsideFunction() {
        Assertions.assertEquals(4, Sh4j.INSTANCE.run("f() { return 4; }; f"));
    }

    @Test
    void forWithoutInUsesPositionalParameters() {
        Assertions.assertEquals(
            String.format("a%nb%n"),
            Sh4j.INSTANCE.out("set -- a b; for i; do echo $i; done")
        );
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
    void execWithCommandAndRedirectionFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.runFull(
                String.format("exec echo hi > %s", new TempDirs(PosixRegressionTest.root)
                                                       .file("exec-arg.out").getAbsolutePath())
            ).exitCode()
        );
    }

    @Test
    void execWithCommandAndRedirectionSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("exec-arg.out");
        Sh4j.INSTANCE.runFull(
            String.format("exec echo hi > %s", file.getAbsolutePath())
        );
        Assertions.assertEquals(
            String.format("hi%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void specialBuiltinAssignmentPersists() {
        Assertions.assertEquals(
            String.format("12%n"),
            Sh4j.INSTANCE.out("X=1 export Y=2; echo $X$Y")
        );
    }

    @Test
    void externalCommandReceivesPrefixAssignment() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("X=1 sh -c 'echo $X'")
        );
    }

    @Test
    void emptyAndMissingCommandsReturnErrorFirst() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("PATH=; ls"));
    }

    @Test
    void emptyAndMissingCommandsReturnErrorSecond() {
        Sh4j.INSTANCE.run("PATH=; ls");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("/nonexistent/command"));
    }

    @Test
    void emptyAndMissingCommandsReturnErrorThird() {
        Sh4j.INSTANCE.run("PATH=; ls");
        Sh4j.INSTANCE.run("/nonexistent/command");
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("PATH=:/bin; true; echo ok")
        );
    }

    @Test
    void trapRuntimeErrorDoesNotCrashExit() {
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stdout()
        );
    }

    @Test
    void unsetFunctionAndCommandLookupFirst() {
        Assertions.assertEquals(
            1, Sh4j.INSTANCE.run("f() { :; }; unset -f f; type f")
        );
    }

    @Test
    void unsetFunctionAndCommandLookupSecond() {
        Sh4j.INSTANCE.run("f() { :; }; unset -f f; type f");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("command"));
    }

    @Test
    void unsetFunctionAndCommandLookupThird() {
        Sh4j.INSTANCE.run("f() { :; }; unset -f f; type f");
        Sh4j.INSTANCE.run("command");
        Assertions.assertEquals(String.format("echo%n"), Sh4j.INSTANCE.out("command -v echo"));
    }

    @Test
    void hashWaitAndKillOptionFormsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash echo"));
    }

    @Test
    void hashWaitAndKillOptionFormsSecond() {
        Sh4j.INSTANCE.run("hash echo");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("sleep 0.1 & wait"));
    }

    @Test
    void hashWaitAndKillOptionFormsThird() {
        Sh4j.INSTANCE.run("hash echo");
        Sh4j.INSTANCE.run("sleep 0.1 & wait");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -s TERM 999999"));
    }

    @Test
    void hashWaitAndKillOptionFormsFourth() {
        Sh4j.INSTANCE.run("hash echo");
        Sh4j.INSTANCE.run("sleep 0.1 & wait");
        Sh4j.INSTANCE.run("kill -s TERM 999999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
    }

    @Test
    void trapEmptyActionIsIgnored() {
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.out("trap '' EXIT; echo body")
        );
    }

    @Test
    void dotWithoutFileReturnsOne() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("."));
    }

    @Test
    void readLastVariableReceivesRemainder() {
        Assertions.assertEquals(
            String.format("a=x b=y z%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(String.format("x y z%n").getBytes(StandardCharsets.UTF_8)),
                "read a b; echo \"a=$a b=$b\""
            )
        );
    }

    @Test
    void execOutputDescriptorWritesFile() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("fd-out.txt");
        Sh4j.INSTANCE.run(
            String.format("exec 6>%s; echo toFd >&6", file.getAbsolutePath())
        );
        Assertions.assertEquals(
            String.format("toFd%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void execOutputDescriptorCloseSucceedsFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(
                String.format("exec 7>%s; exec 7>&-", new TempDirs(PosixRegressionTest.root)
                                                          .file("fd-close.txt").getAbsolutePath())
            )
        );
    }

    @Test
    void execOutputDescriptorCloseSucceedsSecond() throws IOException {
        final File file = new TempDirs(PosixRegressionTest.root)
            .file("fd-close.txt");
        Sh4j.INSTANCE.run(
            String.format("exec 7>%s; exec 7>&-", file.getAbsolutePath())
        );
        Assertions.assertTrue(
            file.isFile() && file.length() == 0L,
            "closing the descriptor must leave an empty file"
        );
    }

    @Test
    void threeStagePipelineRunsEveryStage() {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            Sh4j.INSTANCE.out("echo a; echo b; echo c")
        );
    }

    @Test
    void pipelineAfterSequentialListKeepsOrder() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("echo x | grep y; echo b")
        );
    }

    @Test
    void untilLoopRunsUntilConditionHolds() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out(
                "i=0; until [ $i -ge 2 ]; do i=$((i+1)); echo $i; done"
            )
        );
    }

    @Test
    void groupingBodyRunsInPlace() {
        Assertions.assertEquals(
            String.format("g%n"),
            Sh4j.INSTANCE.out("{ echo g; }")
        );
    }

    @Test
    void subshellBodyRunsInChildScope() {
        Assertions.assertEquals(
            String.format("s%n"),
            Sh4j.INSTANCE.out("(echo s)")
        );
    }
}
