/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for CommandHistory.
 * @since 1.0
 */
@SuppressWarnings({
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnnecessaryLocalRule",
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods"
})
public final class CommandHistoryTest {

    @Test
    public void emptyHistoryHasSizeZero() {
        final CommandHistory hist = new CommandHistory();
        Assert.assertEquals(0, hist.size());
        Assert.assertEquals(0, hist.lastNumber());
        Assert.assertEquals(1, hist.nextNumber());
    }

    @Test
    public void addIncrementsSizeAndNumber() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo hello");
        Assert.assertEquals(1, hist.size());
        Assert.assertEquals(1, hist.lastNumber());
        Assert.assertEquals(2, hist.nextNumber());
    }

    @Test
    public void getReturnsCorrectCommand() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        hist.add("echo two");
        Assert.assertEquals("echo one", hist.get(1));
        Assert.assertEquals("echo two", hist.get(2));
    }

    @Test
    public void getReturnsNullForOutOfRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        Assert.assertNull(hist.get(0));
        Assert.assertNull(hist.get(2));
        Assert.assertNull(hist.get(-1));
    }

    @Test
    public void listAllEntries() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        final java.util.List<CommandHistory.Entry> entries = hist.list(1, 0);
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(1, entries.get(0).number());
        Assert.assertEquals("cmd1", entries.get(0).command());
        Assert.assertEquals(3, entries.get(2).number());
        Assert.assertEquals("cmd3", entries.get(2).command());
    }

    @Test
    public void listWithNegativeRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        final java.util.List<CommandHistory.Entry> entries = hist.list(-2, -1);
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(2, entries.get(0).number());
        Assert.assertEquals(3, entries.get(1).number());
    }

    @Test
    public void listWithExplicitRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        final java.util.List<CommandHistory.Entry> entries = hist.list(2, 2);
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("cmd2", entries.get(0).command());
    }

    @Test
    public void listEmptyWhenOutOfRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        final java.util.List<CommandHistory.Entry> entries = hist.list(5, 10);
        Assert.assertTrue(entries.isEmpty());
    }

    @Test
    public void ringBufferWraps() {
        final CommandHistory hist = new CommandHistory(3);
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        hist.add("cmd4");
        Assert.assertEquals(3, hist.size());
        Assert.assertEquals(4, hist.lastNumber());
        Assert.assertNull(hist.get(1));
        Assert.assertEquals("cmd2", hist.get(2));
        Assert.assertEquals("cmd3", hist.get(3));
        Assert.assertEquals("cmd4", hist.get(4));
    }

    @Test
    public void listAfterWrap() {
        final CommandHistory hist = new CommandHistory(3);
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        hist.add("cmd4");
        final java.util.List<CommandHistory.Entry> entries = hist.list(2, 0);
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("cmd2", entries.get(0).command());
        Assert.assertEquals("cmd4", entries.get(2).command());
    }

    @Test
    public void emptyCommandsAreIgnored() {
        final CommandHistory hist = new CommandHistory();
        hist.add("");
        hist.add(null);
        Assert.assertEquals(0, hist.size());
    }

    @Test
    public void negativeCapacityThrows() {
        try {
            new CommandHistory(0);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void listEmptyHistoryReturnsEmpty() {
        final CommandHistory hist = new CommandHistory();
        Assert.assertTrue(hist.list(1, 0).isEmpty());
    }
}
