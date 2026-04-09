/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for CommandHistory.
 *
 * @since 1.0
 */
final class CommandHistoryTest {

    @Test
    void emptyHistoryHasSizeZero() {
        MatcherAssert.assertThat(new CommandHistory().size(), CoreMatchers.equalTo(0));
    }

    @Test
    void emptyHistoryHasZeroLastNumber() {
        MatcherAssert.assertThat(new CommandHistory().lastNumber(), CoreMatchers.equalTo(0));
    }

    @Test
    void emptyHistoryHasCounterOne() {
        MatcherAssert.assertThat(new CommandHistory().counter(), CoreMatchers.equalTo(1));
    }

    @Test
    void addIncrementsSize() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo hello");
        MatcherAssert.assertThat(hist.size(), CoreMatchers.equalTo(1));
    }

    @Test
    void addIncrementsLastNumber() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo hello");
        MatcherAssert.assertThat(hist.lastNumber(), CoreMatchers.equalTo(1));
    }

    @Test
    void addIncrementsCounter() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo hello");
        MatcherAssert.assertThat(hist.counter(), CoreMatchers.equalTo(2));
    }

    @Test
    void returnsFirstCommand() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        hist.add("echo two");
        MatcherAssert.assertThat(hist.get(1), CoreMatchers.equalTo("echo one"));
    }

    @Test
    void returnsSecondCommand() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        hist.add("echo two");
        MatcherAssert.assertThat(hist.get(2), CoreMatchers.equalTo("echo two"));
    }

    @Test
    void returnsNullForZeroIndex() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        MatcherAssert.assertThat(hist.get(0), CoreMatchers.nullValue());
    }

    @Test
    void returnsNullForFutureIndex() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        MatcherAssert.assertThat(hist.get(2), CoreMatchers.nullValue());
    }

    @Test
    void returnsNullForNegativeIndex() {
        final CommandHistory hist = new CommandHistory();
        hist.add("echo one");
        MatcherAssert.assertThat(hist.get(-1), CoreMatchers.nullValue());
    }

    @Test
    void listAllEntries() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        MatcherAssert.assertThat(hist.list(1, 0).size(), CoreMatchers.equalTo(3));
    }

    @Test
    void listWithNegativeRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        MatcherAssert.assertThat(hist.list(-2, -1).size(), CoreMatchers.equalTo(2));
    }

    @Test
    void listWithExplicitRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        MatcherAssert.assertThat(hist.list(2, 2).size(), CoreMatchers.equalTo(1));
    }

    @Test
    void listEmptyWhenOutOfRange() {
        final CommandHistory hist = new CommandHistory();
        hist.add("cmd1");
        MatcherAssert.assertThat(hist.list(5, 10).isEmpty(), CoreMatchers.is(true));
    }

    @Test
    void ringBufferWraps() {
        final CommandHistory hist = new CommandHistory(3);
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        hist.add("cmd4");
        MatcherAssert.assertThat(hist.size(), CoreMatchers.equalTo(3));
    }

    @Test
    void listAfterWrap() {
        final CommandHistory hist = new CommandHistory(3);
        hist.add("cmd1");
        hist.add("cmd2");
        hist.add("cmd3");
        hist.add("cmd4");
        MatcherAssert.assertThat(hist.list(2, 0).size(), CoreMatchers.equalTo(3));
    }

    @Test
    void emptyCommandsAreIgnored() {
        final CommandHistory hist = new CommandHistory();
        hist.add("");
        hist.add(null);
        MatcherAssert.assertThat(hist.size(), CoreMatchers.equalTo(0));
    }

    @Test
    void negativeCapacityThrows() {
        boolean thrown = false;
        try {
            new CommandHistory(0);
        } catch (final IllegalArgumentException ex) {
            thrown = true;
        }
        MatcherAssert.assertThat(thrown, CoreMatchers.is(true));
    }

    @Test
    void listEmptyHistoryReturnsEmpty() {
        MatcherAssert.assertThat(new CommandHistory().list(1, 0).isEmpty(), CoreMatchers.is(true));
    }

    @Test
    void capacityOneAndRangeBoundaries() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.size(), CoreMatchers.equalTo(1));
    }

    @Test
    void capacityOneKeepsLastCommand() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.get(2), CoreMatchers.equalTo("two"));
    }

    @Test
    void capacityOneHasNoFirstCommand() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.get(1), CoreMatchers.nullValue());
    }

    @Test
    void capacityOneHasNoThirdCommand() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.get(3), CoreMatchers.nullValue());
    }

    @Test
    void capacityOneListZeroRangeIsEmpty() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.list(0, 0).isEmpty(), CoreMatchers.is(true));
    }

    @Test
    void capacityOneListNegativeRangeIsEmpty() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.list(-99, 0).isEmpty(), CoreMatchers.is(true));
    }

    @Test
    void capacityOneListReversedRangeIsEmpty() {
        final CommandHistory hist = new CommandHistory(1);
        hist.add("one");
        hist.add("two");
        MatcherAssert.assertThat(hist.list(3, 2).isEmpty(), CoreMatchers.is(true));
    }

    @Test
    void negativeRelativeNumbersClampToBuffer() {
        final CommandHistory hist = new CommandHistory(2);
        hist.add("one");
        hist.add("two");
        hist.add("three");
        MatcherAssert.assertThat(hist.list(-1, -1).get(0).number(), CoreMatchers.equalTo(3));
    }

    @Test
    void negativeRelativeKeepsLastCommand() {
        final CommandHistory hist = new CommandHistory(2);
        hist.add("one");
        hist.add("two");
        hist.add("three");
        MatcherAssert.assertThat(hist.list(-1, -1).get(0).command(), CoreMatchers.equalTo("three"));
    }

    @Test
    void negativeRelativeBeyondBufferIsEmpty() {
        final CommandHistory hist = new CommandHistory(2);
        hist.add("one");
        hist.add("two");
        hist.add("three");
        MatcherAssert.assertThat(hist.list(-99, -1).isEmpty(), CoreMatchers.is(true));
    }
}
