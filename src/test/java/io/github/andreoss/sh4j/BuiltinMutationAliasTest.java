/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: alias.
 *
 * @since 1.0
 */
final class BuiltinMutationAliasTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void aliasFormsFirst() {
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("alias a='echo hi'", "a"));
    }

    @Test
    void aliasFormsSecond() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Assertions.assertEquals(
            String.format("a='echo hi'%n"),
            Sh4j.INSTANCE.out("alias a='echo hi'; alias a")
        );
    }

    @Test
    void aliasFormsThird() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Sh4j.INSTANCE.out("alias a='echo hi'; alias a");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("alias missing"));
    }

    @Test
    void aliasFormsFourth() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Sh4j.INSTANCE.out("alias a='echo hi'; alias a");
        Sh4j.INSTANCE.run("alias missing");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("alias a=b; unalias a"));
    }

    @Test
    void aliasFormsFifth() {
        Sh4j.INSTANCE.out("alias a='echo hi'", "a");
        Sh4j.INSTANCE.out("alias a='echo hi'; alias a");
        Sh4j.INSTANCE.run("alias missing");
        Sh4j.INSTANCE.run("alias a=b; unalias a");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("alias a=b; unalias -a"));
    }

    @Test
    void aliasQuotedAndEmptyValuesFirst() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("alias e=''", "e"));
    }

    @Test
    void aliasQuotedAndEmptyValuesSecond() {
        Sh4j.INSTANCE.out("alias e=''", "e");
        Assertions.assertEquals(
            String.format("a='x y'%n"),
            Sh4j.INSTANCE.out("alias a=\"x y\"; alias a")
        );
    }

    @Test
    void arithmeticOperatorResidualBranchesFirst() {
        Assertions.assertEquals(
            String.format("1 6 24 31%n8 1 1 1 1%n2 7 12 3%n1 -1 2%n"),
            Sh4j.INSTANCE.out(
                "echo $((2 ^ 3)) $((1+2+3)) $((2*3*4)) $((0x1f))",
                "echo $((010)) $((3>2)) $((3>=3)) $((3!=2)) $((3==3))",
                "echo $((6&3)) $((6|3)) $((6<<1)) $((6>>1))",
                "echo $((!0)) $((~0)) $((0?1:2))"
            )
        );
    }

    @Test
    void arithmeticOperatorResidualBranchesSecond() {
        Sh4j.INSTANCE.out(
            "echo $((2 ^ 3)) $((1+2+3)) $((2*3*4)) $((0x1f))",
            "echo $((010)) $((3>2)) $((3>=3)) $((3!=2)) $((3==3))",
            "echo $((6&3)) $((6|3)) $((6<<1)) $((6>>1))",
            "echo $((!0)) $((~0)) $((0?1:2))"
        );
        Assertions.assertEquals(
            String.format("7 7 0 0%n"),
            Sh4j.INSTANCE.out("x=5; echo $((x+=2)) $((x)) $((x=0)) $((x))")
        );
    }

    @Test
    void breakZeroUsesOneLevel() {
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break 0; done; done")
        );
    }

    @Test
    void commandLocateAndExecuteFormsFirst() {
        Assertions.assertEquals(String.format("echo%n"), Sh4j.INSTANCE.out("command -v echo"));
    }

    @Test
    void commandLocateAndExecuteFormsSecond() {
        Sh4j.INSTANCE.out("command -v echo");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v missing_command"));
    }

    @Test
    void commandLocateAndExecuteFormsThird() {
        Sh4j.INSTANCE.out("command -v echo");
        Sh4j.INSTANCE.run("command -v missing_command");
        Assertions.assertEquals(
            String.format("echo is a shell built-in%n"),
            Sh4j.INSTANCE.out("command -V echo")
        );
    }

    @Test
    void commandLocateAndExecuteFormsFourth() {
        Sh4j.INSTANCE.out("command -v echo");
        Sh4j.INSTANCE.run("command -v missing_command");
        Sh4j.INSTANCE.out("command -V echo");
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("command echo hi"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFirst() throws IOException {
        Assertions.assertEquals(
            String.format("12 6 2 3 11 22 7 1 0%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    "",
                    "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                    " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
                )
            )
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesSecond() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Assertions.assertEquals(String.format("1%n"), Sh4j.INSTANCE.out("echo $((unknownvar+1))"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesThird() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Assertions.assertEquals("", Sh4j.INSTANCE.out("echo $((1+))"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFourth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Assertions.assertEquals("", Sh4j.INSTANCE.out("echo $((x=))"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFifth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("command"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesSixth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -v"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesSeventh() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -V"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesEighth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo x >&abc"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesNinth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull("echo ${missing_q?}").exitCode());
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesTenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo ${missing_q?}").stderr().contains("parameter not set")
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesEleventh() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationAliasTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Assertions.assertEquals(
            String.format("sub/a.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir))
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesTwelfth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationAliasTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Assertions.assertEquals(
            dir.resolve("sub").resolve("a.txt") + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir))
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesThirteenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationAliasTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir));
        Assertions.assertEquals(
            String.format("nodir/*.txt%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; echo nodir/*.txt", dir))
        );
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFourteenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationAliasTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("cd %s; echo nodir/*.txt", dir));
        Assertions.assertEquals(String.format("[]%n"), Sh4j.INSTANCE.out("echo []"));
    }

    @Test
    void compoundAssignmentAndGlobPrefixBranchesFifteenth() throws IOException {
        Sh4j.INSTANCE.out(
            String.join(
                "",
                "x=6; echo $((x<<=1)) $((x>>=1)) $((x&=3)) $((x^=1))",
                " $((x|=8)) $((x*=2)) $((x/=3)) $((x%=2)) $((x-=1))"
            )
        );
        Sh4j.INSTANCE.out("echo $((unknownvar+1))");
        Sh4j.INSTANCE.out("echo $((1+))");
        Sh4j.INSTANCE.out("echo $((x=))");
        Sh4j.INSTANCE.run("command");
        Sh4j.INSTANCE.run("command -v");
        Sh4j.INSTANCE.run("command -V");
        Sh4j.INSTANCE.run("echo x >&abc");
        Sh4j.INSTANCE.runFull("echo ${missing_q?}");
        final Path dir = new TempDirs(BuiltinMutationAliasTest.root).folder("glob").toPath();
        Files.write(dir.resolve("a.txt"), new byte[0]);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("a.txt"), new byte[0]);
        Sh4j.INSTANCE.out(String.format("cd %s; echo sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("echo %s/sub/*.txt", dir));
        Sh4j.INSTANCE.out(String.format("cd %s; echo nodir/*.txt", dir));
        Sh4j.INSTANCE.out("echo []");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("sleep 0.1 & kill -9 $!"));
    }

    @Test
    void dispatcherUncoveredBranchesFirst() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("f() { return abc; }; f"));
    }

    @Test
    void dispatcherUncoveredBranchesSecond() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Assertions.assertEquals(
            String.format("1a%n2a%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done"
            )
        );
    }

    @Test
    void dispatcherUncoveredBranchesThird() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("unset HOME; cd"));
    }

    @Test
    void dispatcherUncoveredBranchesFourth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("umask 999"));
    }

    @Test
    void dispatcherUncoveredBranchesFifth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("wait abc"));
    }

    @Test
    void dispatcherUncoveredBranchesSixth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Assertions.assertEquals(
            String.format("f is a function%n"),
            Sh4j.INSTANCE.out("f() { :; }; type f")
        );
    }

    @Test
    void dispatcherUncoveredBranchesSeventh() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Assertions.assertTrue(Sh4j.INSTANCE.out("command -v ls").startsWith("/"));
    }

    @Test
    void dispatcherUncoveredBranchesEighth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Sh4j.INSTANCE.out("command -v ls");
        Assertions.assertEquals(
            String.format("f%n"),
            Sh4j.INSTANCE.out("f() { :; }; command -v f")
        );
    }

    @Test
    void dispatcherUncoveredBranchesNinth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Sh4j.INSTANCE.out("command -v ls");
        Sh4j.INSTANCE.out("f() { :; }; command -v f");
        Assertions.assertEquals(
            String.format("a=x b=[]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("x%n").getBytes(StandardCharsets.UTF_8)),
                "read a b; echo \"a=$a b=[$b]\""
            ).stdout()
        );
    }

    @Test
    void dispatcherUncoveredBranchesTenth() {
        Sh4j.INSTANCE.run("f() { return abc; }; f");
        Sh4j.INSTANCE.out("for i in 1 2; do for j in a b; do echo $i$j; break abc; done; done");
        Sh4j.INSTANCE.run("unset HOME; cd");
        Sh4j.INSTANCE.run("umask 999");
        Sh4j.INSTANCE.run("wait abc");
        Sh4j.INSTANCE.out("f() { :; }; type f");
        Sh4j.INSTANCE.out("command -v ls");
        Sh4j.INSTANCE.out("f() { :; }; command -v f");
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format("x%n").getBytes(StandardCharsets.UTF_8)),
            "read a b; echo \"a=$a b=[$b]\""
        );
        Assertions.assertEquals(
            String.format("[][]%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("x%n").getBytes(StandardCharsets.UTF_8)),
                "read a b c; echo \"[$b][$c]\""
            ).stdout()
        );
    }

    @Test
    void dispatcherResidualBranchesFirst() throws IOException {
        Assertions.assertTrue(Sh4j.INSTANCE.out("command -V ls").contains(" is /"));
    }
}
