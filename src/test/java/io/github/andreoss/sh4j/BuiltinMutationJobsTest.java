/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: jobs.
 *
 * @since 1.0
 */
final class BuiltinMutationJobsTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void jobsWithExplicitAndMissingIdsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait").stdout().contains("sleep")
        );
    }

    @Test
    void jobsWithExplicitAndMissingIdsSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs 999"));
    }

    @Test
    void jobsWithExplicitAndMissingIdsThird() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & jobs %1; wait");
        Sh4j.INSTANCE.run("jobs 999");
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("jobs notanumber"));
    }

    @Test
    void jobsBgFgExplicitIdsFirst() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1").stdout().contains("sleep")
        );
    }

    @Test
    void jobsBgFgExplicitIdsSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1");
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1").stdout().contains("sleep")
        );
    }

    @Test
    void jobsBgFgExplicitIdsThird() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1");
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("bg %999"));
    }

    @Test
    void jobsBgFgExplicitIdsFourth() {
        Sh4j.INSTANCE.runFull("sleep 0.2 & bg %1");
        Sh4j.INSTANCE.runFull("sleep 0.05 & fg %1");
        Sh4j.INSTANCE.run("bg %999");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("fg %999"));
    }

    @Test
    void jobsWithoutIdsUseMostRecentJobFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("sleep 0.1 & jobs").stdout().contains("sleep"));
    }

    @Test
    void jobsWithoutIdsUseMostRecentJobSecond() {
        Sh4j.INSTANCE.runFull("sleep 0.1 & jobs");
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("sleep 0.1 & bg").stdout().contains("sleep"));
    }

    @Test
    void jobsWithoutIdsUseMostRecentJobThird() {
        Sh4j.INSTANCE.runFull("sleep 0.1 & jobs");
        Sh4j.INSTANCE.runFull("sleep 0.1 & bg");
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("sleep 0.1 & fg").stdout().contains("sleep"));
    }

    @Test
    void killOptionFormsFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill"));
    }

    @Test
    void killOptionFormsSecond() {
        Sh4j.INSTANCE.run("kill");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("kill -s"));
    }

    @Test
    void killOptionFormsThird() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill notapid"));
    }

    @Test
    void killOptionFormsFourth() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Sh4j.INSTANCE.run("kill notapid");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -s TERM 999999"));
    }

    @Test
    void killOptionFormsFifth() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Sh4j.INSTANCE.run("kill notapid");
        Sh4j.INSTANCE.run("kill -s TERM 999999");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill -9 999999"));
    }

    @Test
    void killOptionFormsSixth() {
        Sh4j.INSTANCE.run("kill");
        Sh4j.INSTANCE.run("kill -s");
        Sh4j.INSTANCE.run("kill notapid");
        Sh4j.INSTANCE.run("kill -s TERM 999999");
        Sh4j.INSTANCE.run("kill -9 999999");
        Assertions.assertTrue(Sh4j.INSTANCE.out("kill -l").contains("TERM"));
    }

    @Test
    void killTermExitsZero() throws Exception {
        final Process term = new ProcessBuilder("sleep", "30").start();
        try {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill %d", term.toHandle().pid()))
            );
        } finally {
            term.destroyForcibly();
        }
    }

    @Test
    void killTermStopsProcess() throws Exception {
        final Process term = new ProcessBuilder("sleep", "30").start();
        try {
            Sh4j.INSTANCE.run(String.format("kill %d", term.toHandle().pid()));
            Assertions.assertTrue(term.waitFor(5, TimeUnit.SECONDS));
        } finally {
            term.destroyForcibly();
        }
    }

    @Test
    void killNineExitsZero() throws Exception {
        final Process force = new ProcessBuilder("sleep", "30").start();
        try {
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill -9 %d", force.toHandle().pid()))
            );
        } finally {
            force.destroyForcibly();
        }
    }

    @Test
    void killNineStopsProcess() throws Exception {
        final Process force = new ProcessBuilder("sleep", "30").start();
        try {
            Sh4j.INSTANCE.run(String.format("kill -9 %d", force.toHandle().pid()));
            Assertions.assertTrue(force.waitFor(5, TimeUnit.SECONDS));
        } finally {
            force.destroyForcibly();
        }
    }

    @Test
    void killTermOnStubbornExitsZero() throws Exception {
        final Process stubborn = BuiltinMutationJobsTest.stubborn();
        try {
            Thread.sleep(200L);
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill %d", stubborn.toHandle().pid()))
            );
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void killTermLeavesStubbornAlive() throws Exception {
        final Process stubborn = BuiltinMutationJobsTest.stubborn();
        try {
            Thread.sleep(200L);
            Sh4j.INSTANCE.run(String.format("kill %d", stubborn.toHandle().pid()));
            Thread.sleep(200L);
            Assertions.assertTrue(stubborn.isAlive(), "TERM-ignoring process should survive");
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void killKillOnStubbornExitsZero() throws Exception {
        final Process stubborn = BuiltinMutationJobsTest.stubborn();
        try {
            Thread.sleep(200L);
            final long pid = stubborn.toHandle().pid();
            Sh4j.INSTANCE.run(String.format("kill %d", pid));
            Thread.sleep(200L);
            Assertions.assertEquals(
                0,
                Sh4j.INSTANCE.run(String.format("kill -KILL %d", pid))
            );
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void killKillStopsStubborn() throws Exception {
        final Process stubborn = BuiltinMutationJobsTest.stubborn();
        try {
            Thread.sleep(200L);
            final long pid = stubborn.toHandle().pid();
            Sh4j.INSTANCE.run(String.format("kill %d", pid));
            Thread.sleep(200L);
            Sh4j.INSTANCE.run(String.format("kill -KILL %d", pid));
            Assertions.assertTrue(stubborn.waitFor(5L, TimeUnit.SECONDS));
        } finally {
            stubborn.destroyForcibly();
        }
    }

    @Test
    void literalBracesAndJobStateRefreshFirst() {
        Assertions.assertEquals(String.format("${}%n"), Sh4j.INSTANCE.out("x=1; echo \"${}\""));
    }

    @Test
    void literalBracesAndJobStateRefreshSecond() {
        Sh4j.INSTANCE.out("x=1; echo \"${}\"");
        Assertions.assertEquals(String.format("${ x}%n"), Sh4j.INSTANCE.out("x=1; echo \"${ x}\""));
    }

    @Test
    void literalBracesAndJobStateRefreshThird() {
        Sh4j.INSTANCE.out("x=1; echo \"${}\"");
        Sh4j.INSTANCE.out("x=1; echo \"${ x}\"");
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & sleep 0.3; jobs").contains("Done true"));
    }

    @Test
    void pipelineAndDotResidualBranchesFirst() throws IOException {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("__no_such__ | cat"));
    }

    @Test
    void pipelineAndDotResidualBranchesSecond() throws IOException {
        Sh4j.INSTANCE.run("__no_such__ | cat");
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("echo x | __no_such__"));
    }

    @Test
    void pipelineAndDotResidualBranchesThird() throws IOException {
        Sh4j.INSTANCE.run("__no_such__ | cat");
        Sh4j.INSTANCE.run("echo x | __no_such__");
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out("(exit 3); echo $?"));
    }

    @Test
    void pipelineAndDotResidualBranchesFourth() throws IOException {
        Sh4j.INSTANCE.run("__no_such__ | cat");
        Sh4j.INSTANCE.run("echo x | __no_such__");
        Sh4j.INSTANCE.out("(exit 3); echo $?");
        final Path dir = new TempDirs(BuiltinMutationJobsTest.root).folder("dotrel").toPath();
        Files.write(dir.resolve("s.sh"), String.format("X=dot%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            String.format("dot%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; . ./s.sh; echo \"$X\"", dir))
        );
    }

    @Test
    void pwdLogicalAndPhysicalFormsFirst() throws IOException {
        final Path real = new TempDirs(BuiltinMutationJobsTest.root).folder(
            "real"
        ).toPath().toRealPath();
        Assertions.assertEquals(
            real + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -L", real))
        );
    }

    @Test
    void pwdLogicalAndPhysicalFormsSecond() throws IOException {
        final Path real = new TempDirs(BuiltinMutationJobsTest.root).folder(
            "real"
        ).toPath().toRealPath();
        Sh4j.INSTANCE.out(String.format("cd %s; pwd -L", real));
        Assertions.assertEquals(
            real + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -P", real))
        );
    }

    @Test
    void pwdFlagOrder() throws IOException {
        final Path dir = new TempDirs(BuiltinMutationJobsTest.root).folder(
            "pwdorder"
        ).toPath().toRealPath();
        Assertions.assertEquals(
            dir + String.format("%n"),
            Sh4j.INSTANCE.out(String.format("cd %s; pwd -P -L", dir))
        );
    }

    @Test
    void readInputLineSurvivesIoException() {
        Assertions.assertEquals(
            String.format("rc=1 x=[]%n"),
            Sh4j.INSTANCE.runFull(
                BuiltinMutationJobsTest.failingInput(),
                "read x; echo \"rc=$? x=[$x]\""
            ).stdout()
        );
    }

    @Test
    void readCustomIfsAndWhitespaceFirst() {
        Assertions.assertEquals(
            String.format("a=x b=y:z%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readCustomIfsAndWhitespaceSecond() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assertions.assertEquals(
            String.format("a=x b=y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("   x   y   %n").getBytes(StandardCharsets.UTF_8)
                ),
                "read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readCustomIfsAndWhitespaceThird() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format("x:y:z%n").getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(
                String.format("   x   y   %n").getBytes(StandardCharsets.UTF_8)
            ),
            "read a b; echo \"a=$a b=$b\""
        );
        Assertions.assertEquals(
            String.format("a=x b=:%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format("x::%n").getBytes(StandardCharsets.UTF_8)),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readMoreSplittingFormsFirst() {
        Assertions.assertEquals(
            String.format("a= b=x:y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(String.format(":x:y%n").getBytes(StandardCharsets.UTF_8)),
                "IFS=:; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    @Test
    void readMoreSplittingFormsSecond() {
        Sh4j.INSTANCE.runFull(
            new ByteArrayInputStream(String.format(":x:y%n").getBytes(StandardCharsets.UTF_8)),
            "IFS=:; read a b; echo \"a=$a b=$b\""
        );
        Assertions.assertEquals(
            String.format("a=x b=y%n"),
            Sh4j.INSTANCE.runFull(
                new ByteArrayInputStream(
                    String.format("x : y%n").getBytes(StandardCharsets.UTF_8)
                ),
                "IFS=' :'; read a b; echo \"a=$a b=$b\""
            ).stdout()
        );
    }

    private static InputStream failingInput() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public int read(final byte[] buf, final int off, final int len)
                throws IOException {
                throw new IOException("boom");
            }
        };
    }

    private static Process stubborn() throws IOException {
        return new ProcessBuilder(
            "sh", "-c", "trap '' TERM; while true; do sleep 1; done"
        ).start();
    }
}
