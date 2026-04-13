/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: dispatcher residual.
 *
 * @since 1.0
 */
final class BuiltinMutationDispatcherResidualTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void dispatcherResidualBranchesSecond() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("command -V __no_such_cmd__"));
    }

    @Test
    void dispatcherResidualBranchesThird() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Assertions.assertEquals(
            String.format("?%n"),
            Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"")
        );
    }

    @Test
    void dispatcherResidualBranchesFourth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull("echo one; fc -s nope").exitCode());
    }

    @Test
    void dispatcherResidualBranchesFifth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one; fc -s nope").stderr().contains("-s requires pat=rep")
        );
    }

    @Test
    void dispatcherResidualBranchesSixth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Assertions.assertEquals(
            String.format("hi%necho hey%nhey%n"),
            Sh4j.INSTANCE.out("echo hi; fc -s hi=hey")
        );
    }

    @Test
    void dispatcherResidualBranchesSeventh() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Sh4j.INSTANCE.out("echo hi; fc -s hi=hey");
        Assertions.assertEquals(
            String.format("one%n1\techo one%n"),
            Sh4j.INSTANCE.out("echo one; fc -l abc")
        );
    }

    @Test
    void dispatcherResidualBranchesEighth() throws IOException {
        Sh4j.INSTANCE.out("command -V ls");
        Sh4j.INSTANCE.run("command -V __no_such_cmd__");
        Sh4j.INSTANCE.out("set -- -a b; getopts a v; getopts a v; echo \"$v\"");
        Sh4j.INSTANCE.runFull("echo one; fc -s nope");
        Sh4j.INSTANCE.out("echo hi; fc -s hi=hey");
        Sh4j.INSTANCE.out("echo one; fc -l abc");
        final Path fdesc = new TempDirs(BuiltinMutationDispatcherResidualTest.root).file("fdesc.txt").toPath();
        Sh4j.INSTANCE.run(String.format("echo hi; exec 3>%s; fc -s nope 2>&3; exec 3>&-", fdesc));
        Assertions.assertTrue(
            new String(Files.readAllBytes(fdesc), StandardCharsets.UTF_8).contains(
                "fc: -s requires pat=rep"
            )
        );
    }

    @Test
    void dotEmptyAndPositionalFormsFirst() throws IOException {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("."));
    }

    @Test
    void dotEmptyAndPositionalFormsSecond() throws IOException {
        Sh4j.INSTANCE.run(".");
        final Path lib = new TempDirs(BuiltinMutationDispatcherResidualTest.root).file("dot.sh").toPath();
        Files.write(lib, String.format("echo \"$1-$2\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("a-b%n"),
            Sh4j.INSTANCE.out(String.format(". %s a b", lib))
        );
    }

    @Test
    void dotRestoresPositionalParameters() throws IOException {
        final Path lib = new TempDirs(BuiltinMutationDispatcherResidualTest.root).file("restore.sh").toPath();
        Files.write(lib, String.format("echo \"in=$1\"%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("in=inner%nouter%n"),
            Sh4j.INSTANCE.out(String.format("set -- outer; . %s inner; echo $1", lib))
        );
    }

    @Test
    void echoNoNewlineAndEmptyFormsFirst() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("echo -n"));
    }

    @Test
    void echoNoNewlineAndEmptyFormsSecond() {
        Sh4j.INSTANCE.out("echo -n");
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo"));
    }

    @Test
    void echoNoNewlineAndEmptyFormsThird() {
        Sh4j.INSTANCE.out("echo -n");
        Sh4j.INSTANCE.out("echo");
        Assertions.assertEquals("a b", Sh4j.INSTANCE.out("echo -n a b"));
    }

    @Test
    void echoReportsExpansionError() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo ${MISSING:?required}"));
    }

    @Test
    void evalEmptyAndNonEmptyFormsFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("eval"));
    }

    @Test
    void evalEmptyAndNonEmptyFormsSecond() {
        Sh4j.INSTANCE.run("eval");
        Assertions.assertEquals(String.format("hi%n"), Sh4j.INSTANCE.out("eval echo hi"));
    }

    @Test
    void evalEmptyAndNonEmptyFormsThird() {
        Sh4j.INSTANCE.run("eval");
        Sh4j.INSTANCE.out("eval echo hi");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("eval ''"));
    }

    @Test
    void exportBareAndPrintFormsFirst() {
        Assertions.assertEquals("", Sh4j.INSTANCE.getVar("BARE", "export BARE"));
    }

    @Test
    void exportBareAndPrintFormsSecond() {
        Assertions.assertEquals(
            String.format("%n"),
            Sh4j.INSTANCE.out("export BARE; sh -c 'echo ${BARE-unset}'")
        );
    }

    @Test
    void exportBareAndPrintFormsThird() {
        Sh4j.INSTANCE.out("export BARE; sh -c 'echo ${BARE-unset}'");
        Assertions.assertTrue(Sh4j.INSTANCE.out("export -p").contains("export PATH="));
    }

    @Test
    void fcListAndSubstituteFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l").stdout().contains("echo one")
        );
    }

    @Test
    void fcListAndSubstituteSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo hello", "fc -s hello=bye").stdout().contains("bye")
        );
    }

    @Test
    void fcErrorFormsFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -e"));
    }

    @Test
    void fcErrorFormsSecond() {
        Sh4j.INSTANCE.run("fc -e");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -z"));
    }

    @Test
    void fcErrorFormsThird() {
        Sh4j.INSTANCE.run("fc -e");
        Sh4j.INSTANCE.run("fc -z");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -s"));
    }

    @Test
    void fcErrorFormsFourth() {
        Sh4j.INSTANCE.run("fc -e");
        Sh4j.INSTANCE.run("fc -z");
        Sh4j.INSTANCE.run("fc -s");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -l"));
    }

    @Test
    void fcErrorFormsFifth() {
        Sh4j.INSTANCE.run("fc -e");
        Sh4j.INSTANCE.run("fc -z");
        Sh4j.INSTANCE.run("fc -s");
        Sh4j.INSTANCE.run("fc -l");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fc -s broken"));
    }

    @Test
    void fcListModesAndRangesFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -n -l").stdout().contains("\techo one")
        );
    }

    @Test
    void fcListModesAndRangesSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -n -l");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l 1 1").stdout().contains("echo one")
        );
    }

    @Test
    void fcListModesAndRangesThird() {
        Sh4j.INSTANCE.runFull("echo one", "fc -n -l");
        Sh4j.INSTANCE.runFull("echo one", "fc -l 1 1");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -e true").stdout().contains("echo one")
        );
    }

    @Test
    void fcEditorAndRangeBoundariesFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -e true 1").stdout().contains("echo one")
        );
    }

    @Test
    void fcEditorAndRangeBoundariesSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -e true 1");
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 3 2").exitCode());
    }

    @Test
    void fcEditorAndRangeBoundariesThird() {
        Sh4j.INSTANCE.runFull("echo one", "fc -e true 1");
        Sh4j.INSTANCE.runFull("echo one", "fc -l 3 2");
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -e true 1 1").exitCode());
    }

    @Test
    void fcRangeEdgesAndSubstitutionFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 0").exitCode());
    }

    @Test
    void fcRangeEdgesAndSubstitutionSecond() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l 0");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -l -1").stdout().contains("echo one")
        );
    }

    @Test
    void fcRangeEdgesAndSubstitutionThird() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l 0");
        Sh4j.INSTANCE.runFull("echo one", "fc -l -1");
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("echo one", "fc -l 5").exitCode());
    }

    @Test
    void fcRangeEdgesAndSubstitutionFourth() {
        Sh4j.INSTANCE.runFull("echo one", "fc -l 0");
        Sh4j.INSTANCE.runFull("echo one", "fc -l -1");
        Sh4j.INSTANCE.runFull("echo one", "fc -l 5");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo one", "fc -s one=two").stdout().contains("two")
        );
    }

    @Test
    void fgExplicitAndMissingIdsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1").stdout().contains("sleep")
        );
    }

    @Test
    void fgExplicitAndMissingIdsSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg 999"));
    }

    @Test
    void fgExplicitAndMissingIdsThird() {
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Sh4j.INSTANCE.run("fg 999");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg notanumber"));
    }
}
