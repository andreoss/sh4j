/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(hist.size(), equalTo(0));
        assertThat(hist.lastNumber(), equalTo(0));
        assertThat(hist.nextNumber(), equalTo(1));
    }

    @Test
    public void addIncrementsSizeAndNumber() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo hello");
        assertThat(hist.size(), equalTo(1));
        assertThat(hist.lastNumber(), equalTo(1));
        assertThat(hist.nextNumber(), equalTo(2));
    }

    @Test
    public void getReturnsCorrectCommand() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        hist.add("echo two");
        assertThat(hist.get(1), equalTo("echo one"));
        assertThat(hist.get(2), equalTo("echo two"));
    }

    @Test
    public void getReturnsNullForOutOfRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        assertThat(hist.get(0), nullValue());
        assertThat(hist.get(2), nullValue());
        assertThat(hist.get(-1), nullValue());
    }

    @Test
    public void listAllEntries() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        final List<CommandHistory.Entry> entries = hist.list(1, 0);
        assertThat(entries.size(), equalTo(3));
        assertThat(entries.get(0).number(), equalTo(1));
        assertThat(entries.get(0).command(), equalTo("cmd1"));
        assertThat(entries.get(2).number(), equalTo(3));
        assertThat(entries.get(2).command(), equalTo("cmd3"));
    }

    @Test
    public void listWithNegativeRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        final List<CommandHistory.Entry> entries = hist.list(-2, -1);
        assertThat(entries.size(), equalTo(2));
        assertThat(entries.get(0).number(), equalTo(2));
        assertThat(entries.get(1).number(), equalTo(3));
    }

    @Test
    public void listWithExplicitRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        final List<CommandHistory.Entry> entries = hist.list(2, 2);
        assertThat(entries.size(), equalTo(1));
        assertThat(entries.get(0).command(), equalTo("cmd2"));
    }

    @Test
    public void listEmptyWhenOutOfRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        final List<CommandHistory.Entry> entries = hist.list(5, 10);
        assertThat(entries.isEmpty(), is(true));
    }

    @Test
    public void ringBufferWraps() {
        final CommandHistory hist = new CommandHistory(3);
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        hist.add("cmd4");
        assertThat(hist.size(), equalTo(3));
        assertThat(hist.lastNumber(), equalTo(4));
        assertThat(hist.get(1), nullValue());
        assertThat(hist.get(2), equalTo("cmd2"));
        assertThat(hist.get(3), equalTo("cmd3"));
        assertThat(hist.get(4), equalTo("cmd4"));
    }

    @Test
    public void listAfterWrap() {
        final CommandHistory hist = new CommandHistory(3);
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        hist.add("cmd4");
        final List<CommandHistory.Entry> entries = hist.list(2, 0);
        assertThat(entries.size(), equalTo(3));
        assertThat(entries.get(0).command(), equalTo("cmd2"));
        assertThat(entries.get(2).command(), equalTo("cmd4"));
    }

    @Test
    public void emptyCommandsAreIgnored() {
        final CommandHistory hist = new CommandHistory();
        hist.add("");
        hist.add(null);
        assertThat(hist.size(), equalTo(0));
    }

    @Test
    public void negativeCapacityThrows() {
        try {
            new CommandHistory(0);
            // Should not reach here
            assertThat(true, is(false));
        } catch (IllegalArgumentException e) {
            assertThat(e.getClass(), equalTo(IllegalArgumentException.class));
        }
    }

    @Test
    public void listEmptyHistoryReturnsEmpty() {
        final CommandHistory hist = new CommandHistory();
        assertThat(hist.list(1, 0).isEmpty(), is(true));
    }

    @Test
    public void capacityOneAndRangeBoundaries() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        assertThat(hist.size(), equalTo(1));
        assertThat(hist.get(2), equalTo("two"));
        assertThat(hist.get(1), nullValue());
        assertThat(hist.get(3), nullValue());
        assertThat(hist.list(0, 0).isEmpty(), is(true));
        assertThat(hist.list(-99, 0).isEmpty(), is(true));
        assertThat(hist.list(3, 2).isEmpty(), is(true));
    }

    @Test
    public void negativeRelativeNumbersClampToBuffer() {
        final CommandHistory hist = new CommandHistory(2);
        hist.add("one");
        hist.add("two");
        hist.add("three");
        assertThat(hist.list(-1, -1).get(0).number(), equalTo(3));
        assertThat(hist.list(-1, -1).get(0).command(), equalTo("three"));
        assertThat(hist.list(-99, -1).isEmpty(), is(true));
    }
}
