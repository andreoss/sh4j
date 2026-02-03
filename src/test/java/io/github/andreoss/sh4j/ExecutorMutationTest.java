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
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(
            Sh4j.INSTANCE.out("alias a='echo A'; f() { echo F; }; set -- x; (a); (f); (echo $1)"),
            equalTo(String.format("A%nF%nx%n"))
        );
    }

    @Test
    void subshellInheritsFlagsAndCwd() throws IOException {
        final File dir = TempDirs.folder(ExecutorMutationTest.root, "subcwd");
        assertThat(
            Sh4j.INSTANCE.out(
                String.format("set -u; cd %s; (echo $-); (pwd)", dir.getAbsolutePath())
            ),
            equalTo(String.format("u%n") + dir.getCanonicalPath() + String.format("%n"))
        );
    }

    @Test
    void trapRuntimeErrorIsReported() {
        assertThat(
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stdout(),
            equalTo(String.format("body%n"))
        );
        assertThat(
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stderr(),
            CoreMatchers.containsString("sh: trap:")
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReported() {
        assertThat(Sh4j.INSTANCE.runFull("break").exitCode(), equalTo(0));
        assertThat(
            Sh4j.INSTANCE.runFull("break").stderr(),
            CoreMatchers.containsString("break: not in a loop")
        );
        assertThat(Sh4j.INSTANCE.runFull("continue").exitCode(), equalTo(0));
        assertThat(
            Sh4j.INSTANCE.runFull("continue").stderr(),
            CoreMatchers.containsString("continue: not in a loop")
        );
        assertThat(Sh4j.INSTANCE.runFull("return 5").exitCode(), equalTo(5));
        assertThat(
            Sh4j.INSTANCE.runFull("return 5").stderr(),
            CoreMatchers.containsString("return: not in a function")
        );
    }

    @Test
    void ifBranchesPropagateBodyStatus() {
        assertThat(Sh4j.INSTANCE.run("if true; then false; fi"), equalTo(1));
        assertThat(Sh4j.INSTANCE.run("if false; then :; fi"), equalTo(0));
        assertThat(Sh4j.INSTANCE.run("if false; then :; else false; fi"), equalTo(1));
    }

    @Test
    void forWithoutWordsAndBodyStatus() {
        assertThat(Sh4j.INSTANCE.out("for i; do echo $i; done"), equalTo(""));
        assertThat(Sh4j.INSTANCE.run("for i in a b; do false; done"), equalTo(1));
        assertThat(Sh4j.INSTANCE.run("for i in a b; do break; done"), equalTo(0));
    }

    @Test
    void caseWithoutMatchReturnsZero() {
        assertThat(Sh4j.INSTANCE.run("case x in y) false;; esac"), equalTo(0));
        assertThat(Sh4j.INSTANCE.out("case x in y) echo no;; esac"), equalTo(""));
    }

    @Test
    void aliasAndExecStatuses() {
        assertThat(Sh4j.INSTANCE.run("alias a='false'", "a"), equalTo(1));
        assertThat(Sh4j.INSTANCE.run("alias a='true'", "a"), equalTo(0));
        assertThat(Sh4j.INSTANCE.run("exec sh -c 'exit 7'"), equalTo(7));
    }

    @Test
    void aliasExpansionUsesTheTableCapturedAtReadTime() {
        assertThat(Sh4j.INSTANCE.run("alias q='echo Q'; q"), equalTo(127));
        assertThat(Sh4j.INSTANCE.out("alias q='echo Q'", "q"), equalTo(String.format("Q%n")));
        assertThat(Sh4j.INSTANCE.run("alias e='echo X'; f() { e; }; f"), equalTo(127));
    }

    @Test
    void functionRestoresPositionalsAndReturnStatus() {
        assertThat(
            Sh4j.INSTANCE.out("set -- outer; f() { echo $1; }; f inner; echo $1"),
            equalTo(String.format("inner%nouter%n"))
        );
        assertThat(Sh4j.INSTANCE.run("f() { return 6; }; f"), equalTo(6));
    }

    @Test
    void builtinHeredocInputForms() {
        assertThat(
            Sh4j.INSTANCE.runFull(String.format("read x <<EOF%nhi%nEOF%necho \"$x\"")).stdout(),
            equalTo(String.format("hi%n"))
        );
        assertThat(
            Sh4j.INSTANCE.runFull(
                String.format("Y=value; read x <<'EOF'%n$Y%nEOF%necho \"$x\"")
            ).stdout(),
            equalTo(String.format("$Y%n"))
        );
    }

    @Test
    void builtinInputFromMissingFileFallsBackToStdin() {
        assertThat(
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("fallback%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read x < /nonexistent/input; echo \"$x\""
            ).stdout(),
            equalTo(String.format("fallback%n"))
        );
    }

    @Test
    void externalInputOutputAndStderrPipes() throws IOException {
        assertThat(
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("piped%n").getBytes(StandardCharsets.UTF_8)),
                "cat"
            ).stdout(),
            equalTo(String.format("piped%n"))
        );
        final File file = TempDirs.file(ExecutorMutationTest.root, "ext.out");
        assertThat(
            Sh4j.INSTANCE.runFull(
                String.format("echo redirected > %s", file.getAbsolutePath())
            ).stdout(),
            equalTo("")
        );
        assertThat(
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8),
            equalTo(String.format("redirected%n"))
        );
        assertThat(
            Sh4j.INSTANCE.runFull("sh -c 'echo stderr >&2'").stderr(),
            CoreMatchers.containsString("stderr")
        );
    }

    @Test
    void expandFieldsQuotedAtAndEmptyPositionals() {
        assertThat(Sh4j.INSTANCE.out("for x in \"$@\"; do echo $x; done"), equalTo(""));
        assertThat(Sh4j.INSTANCE.out("echo \"$@\""), equalTo(String.format("%n")));
        assertThat(
            Sh4j.INSTANCE.out("IFS=; set -- a b; echo \"$*\""),
            equalTo(String.format("ab%n"))
        );
    }

    @Test
    void specialParameterBoundaries() {
        assertThat(Sh4j.INSTANCE.out("set -- a b; echo ${#}"), equalTo(String.format("2%n")));
        assertThat(Sh4j.INSTANCE.out("set -- a; echo $9"), equalTo(String.format("%n")));
        assertThat(Sh4j.INSTANCE.out("echo \"$@\""), equalTo(String.format("%n")));
        assertThat(Sh4j.INSTANCE.out("set --; echo $1"), equalTo(String.format("%n")));
    }

    @Test
    void nonExecutableFileReturnsError() throws IOException {
        final File file = TempDirs.file(ExecutorMutationTest.root, "not-executable.sh");
        Files.write(file.toPath(), String.format("echo hi%n").getBytes(StandardCharsets.UTF_8));
        assertThat(file.setExecutable(false), is(true));
        assertThat(Sh4j.INSTANCE.run(file.getAbsolutePath()), equalTo(126));
    }

    @Test
    void quotedGlobIsNotExpandedForExternalCommand() throws IOException {
        final File dir = TempDirs.folder(ExecutorMutationTest.root, "quoted-glob");
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        assertThat(
            Sh4j.INSTANCE.out(
                String.format("cd %s; printf '<%%s>\\n' \"*.txt\"", dir.getAbsolutePath())
            ),
            equalTo(String.format("<*.txt>%n"))
        );
        assertThat(
            Sh4j.INSTANCE.out(
                String.format("cd %s; printf '<%%s>\\n' *.txt", dir.getAbsolutePath())
            ),
            equalTo(String.format("<a.txt>%n"))
        );
    }

    @Test
    void functionStderrRedirection() throws IOException {
        final File file = TempDirs.file(ExecutorMutationTest.root, "fn-err.txt");
        assertThat(
            Sh4j.INSTANCE.runFull(
                String.format(
                    "f() { echo err >&2; }; f 2> %s; cat %s",
                    file.getAbsolutePath(),
                    file.getAbsolutePath()
                )
            ).stdout(),
            equalTo(String.format("err%n"))
        );
    }

    @Test
    void execInputRedirectionPersists() throws IOException {
        final File file = TempDirs.file(ExecutorMutationTest.root, "exec-in.txt");
        Files.write(file.toPath(), String.format("input%n").getBytes(StandardCharsets.UTF_8));
        assertThat(
            Sh4j.INSTANCE.out(String.format("exec < %s; read x; echo $x", file.getAbsolutePath())),
            equalTo(String.format("input%n"))
        );
    }

    @Test
    void breakThreeLevels() {
        assertThat(
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
            equalTo(String.format("1ax%n"))
        );
    }

    @Test
    void specialBuiltinEmptyAssignmentPersists() {
        assertThat(
            Sh4j.INSTANCE.out("X= export Y=1; echo \"${X-unset}:$Y\""),
            equalTo(String.format(":1%n"))
        );
    }
}
