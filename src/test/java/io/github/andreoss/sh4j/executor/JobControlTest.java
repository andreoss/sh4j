/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JobControl}.
 *
 * @since 1.0
 */
final class JobControlTest {

    @Test
    void attachInterruptsWhenSignalArrivedBeforeStart() {
        final JobControl control = new JobControl();
        control.send(15);
        control.attach();
        final boolean interrupted = Thread.currentThread().isInterrupted();
        control.detach();
        Assertions.assertTrue(interrupted);
    }

    @Test
    void trackDestroysProcessStartedAfterSignal() throws IOException, InterruptedException {
        final JobControl control = new JobControl();
        control.attach();
        control.send(15);
        final Process late = new ProcessBuilder("sleep", "30").start();
        JobControl.track(late);
        control.detach();
        Assertions.assertEquals(143, late.waitFor());
    }

    @Test
    void killSignalDestroysTrackedProcessForcibly() throws IOException, InterruptedException {
        final JobControl control = new JobControl();
        control.attach();
        final Process first = new ProcessBuilder("true").start();
        first.waitFor();
        JobControl.track(first);
        final Process slow = new ProcessBuilder("sleep", "30").start();
        JobControl.track(slow);
        final Process last = new ProcessBuilder("true").start();
        last.waitFor();
        JobControl.track(last);
        control.send(9);
        control.detach();
        Assertions.assertEquals(137, slow.waitFor());
    }

    @Test
    void awaitOfCancelledJobReturnsOne() {
        final Job job = new Job(1, "work", new JobControl());
        job.watch(new CompletableFuture<>());
        job.cancel();
        Assertions.assertEquals(1, job.await());
    }

    @Test
    void sendFailsAfterJobEnded() {
        final JobControl control = new JobControl();
        control.attach();
        control.detach();
        Assertions.assertFalse(control.send(15));
    }
}
