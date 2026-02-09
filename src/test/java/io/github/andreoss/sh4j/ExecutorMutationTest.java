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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

/**
 * Additional executor state/control-flow tests selected from PIT survivors.
 *
 * @since 1.0
 */
final class ExecutorMutationTest {

    /** Temporary root directory. */

    @TempDir
    private static Path root;

    @Test
    void subshellInheritsAliasesFunctionsAndPositionals() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("alias a='echo A'; f() { echo F; }; set -- x; (a); (f); (echo $1)"),
            CoreMatchers.equalTo(String.format("A%nF%nx%n"))
        );
    }

    @Test
    void subshellInheritsFlagsAndCwd() throws IOException {
        final File dir = TempDirs.folder(ExecutorMutationTest.root, "subcwd");
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out(
                String.format("set -u; cd %s; (echo $-); (pwd)", dir.getAbsolutePath())
            ),
            CoreMatchers.equalTo(
                String.format("u%n%s%n", dir.getCanonicalPath())
            )
        );
    }

    @Test
    void trapRuntimeErrorIsReported() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stdout(),
            CoreMatchers.equalTo(String.format("body%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stderr(),
            CoreMatchers.containsString("sh: trap:")
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReported() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("break").exitCode(),
            CoreMatchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("break").stderr(),
            CoreMatchers.containsString("break: not in a loop")
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("continue").exitCode(),
            CoreMatchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("continue").stderr(),
            CoreMatchers.containsString("continue: not in a loop")
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("return 5").exitCode(),
            CoreMatchers.equalTo(5)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("return 5").stderr(),
            CoreMatchers.containsString("return: not in a function")
        );
    }

    @Test
    void ifBranchesPropagateBodyStatus() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("if true; then false; fi"),
            CoreMatchers.equalTo(1)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("if false; then :; fi"),
            CoreMatchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("if false; then :; else false; fi"),
            CoreMatchers.equalTo(1)
        );
    }

    @Test
    void forWithoutWordsAndBodyStatus() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("for i; do echo $i; done"),
            CoreMatchers.equalTo("")
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("for i in a b; do false; done"),
            CoreMatchers.equalTo(1)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("for i in a b; do break; done"),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void caseWithoutMatchReturnsZero() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("case x in y) false;; esac"),
            CoreMatchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("case x in y) echo no;; esac"),
            CoreMatchers.equalTo("")
        );
    }

    @Test
    void aliasAndExecStatuses() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("alias a='false'", "a"),
            CoreMatchers.equalTo(1)
        );
        MatcherAssert.assertThat(Sh4j.INSTANCE.run("alias a='true'", "a"), CoreMatchers.equalTo(0));
        MatcherAssert.assertThat(Sh4j.INSTANCE.run("exec sh -c 'exit 7'"), CoreMatchers.equalTo(7));
    }

    @Test
    void aliasExpansionUsesTheTableCapturedAtReadTime() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("alias q='echo Q'; q"),
            CoreMatchers.equalTo(127)
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("alias q='echo Q'", "q"),
            CoreMatchers.equalTo(String.format("Q%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("alias e='echo X'; f() { e; }; f"),
            CoreMatchers.equalTo(127)
        );
    }

    @Test
    void functionRestoresPositionalsAndReturnStatus() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("set -- outer; f() { echo $1; }; f inner; echo $1"),
            CoreMatchers.equalTo(String.format("inner%nouter%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run("f() { return 6; }; f"),
            CoreMatchers.equalTo(6)
        );
    }

    @Test
    void builtinHeredocInputForms() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull(String.format("read x <<EOF%nhi%nEOF%necho \"$x\"")).stdout(),
            CoreMatchers.equalTo(String.format("hi%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull(
                String.format("Y=value; read x <<'EOF'%n$Y%nEOF%necho \"$x\"")
            ).stdout(),
            CoreMatchers.equalTo(String.format("$Y%n"))
        );
    }

    @Test
    void builtinInputFromMissingFileFallsBackToStdin() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("fallback%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read x < /nonexistent/input; echo \"$x\""
            ).stdout(),
            CoreMatchers.equalTo(String.format("fallback%n"))
        );
    }

    @Test
    void externalInputOutputAndStderrPipes() throws IOException {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("piped%n").getBytes(StandardCharsets.UTF_8)),
                "cat"
            ).stdout(),
            CoreMatchers.equalTo(String.format("piped%n"))
        );
        final File file = TempDirs.file(ExecutorMutationTest.root, "ext.out");
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull(
                String.format("echo redirected > %s", file.getAbsolutePath())
            ).stdout(),
            CoreMatchers.equalTo("")
        );
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8),
            CoreMatchers.equalTo(String.format("redirected%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("sh -c 'echo stderr >&2'").stderr(),
            CoreMatchers.containsString("stderr")
        );
    }

    @Test
    void expandFieldsQuotedAtAndEmptyPositionals() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("for x in \"$@\"; do echo $x; done"),
            CoreMatchers.equalTo("")
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("echo \"$@\""),
            CoreMatchers.equalTo(String.format("%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("IFS=; set -- a b; echo \"$*\""),
            CoreMatchers.equalTo(String.format("ab%n"))
        );
    }

    @Test
    void specialParameterBoundaries() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("set -- a b; echo ${#}"),
            CoreMatchers.equalTo(String.format("2%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("set -- a; echo $9"),
            CoreMatchers.equalTo(String.format("%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("echo \"$@\""),
            CoreMatchers.equalTo(String.format("%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("set --; echo $1"),
            CoreMatchers.equalTo(String.format("%n"))
        );
    }

    @Test
    void nonExecutableFileReturnsError() throws IOException {
        final File file = TempDirs.file(ExecutorMutationTest.root, "not-executable.sh");
        Files.write(file.toPath(), String.format("echo hi%n").getBytes(StandardCharsets.UTF_8));
        MatcherAssert.assertThat(file.setExecutable(false), CoreMatchers.is(true));
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.run(file.getAbsolutePath()),
            CoreMatchers.equalTo(126)
        );
    }

    @Test
    void quotedGlobIsNotExpandedForExternalCommand() throws IOException {
        final File dir = TempDirs.folder(ExecutorMutationTest.root, "quoted-glob");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out(
                String.format("cd %s; printf '<%%s>\\n' \"*.txt\"", dir.getAbsolutePath())
            ),
            CoreMatchers.equalTo(String.format("<*.txt>%n"))
        );
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out(
                String.format("cd %s; printf '<%%s>\\n' *.txt", dir.getAbsolutePath())
            ),
            CoreMatchers.equalTo(String.format("<a.txt>%n"))
        );
    }

    @Test
    void functionStderrRedirection() throws IOException {
        final File file = TempDirs.file(ExecutorMutationTest.root, "fn-err.txt");
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull(
                String.format(
                    "f() { echo err >&2; }; f 2> %s; cat %s",
                    file.getAbsolutePath(),
                    file.getAbsolutePath()
                )
            ).stdout(),
            CoreMatchers.equalTo(String.format("err%n"))
        );
    }

    @Test
    void execInputRedirectionPersists() throws IOException {
        final File file = TempDirs.file(ExecutorMutationTest.root, "exec-in.txt");
        Files.write(file.toPath(), String.format("input%n").getBytes(StandardCharsets.UTF_8));
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out(String.format("exec < %s; read x; echo $x", file.getAbsolutePath())),
            CoreMatchers.equalTo(String.format("input%n"))
        );
    }

    @Test
    void breakThreeLevels() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out(
                """
                for i in 1 2; do
                    for j in a b; do
                        for k in x y; do
                            echo $i$j$k
                            break 3
                        done
                    done
                done
                """
            ),
            CoreMatchers.equalTo(String.format("1ax%n"))
        );
    }

    @Test
    void specialBuiltinEmptyAssignmentPersists() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("X= export Y=1; echo \"${X-unset}:$Y\""),
            CoreMatchers.equalTo(String.format(":1%n"))
        );
    }
}
