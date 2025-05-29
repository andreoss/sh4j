/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import static org.junit.jupiter.api.Assertions.*;

final class RedirectionHandlerTest {

    private final UnaryOperator<String> identityExpander = s -> s;

    @Test
    void redirectStderrToStdout() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream());
        final PrintStream err = new PrintStream(new ByteArrayOutputStream());
        final RedirectionHandler.StreamSwap result =
                RedirectionHandler.redirectOutputForBuiltin(List.of(new Redirection(2, RedirectionType.DUP_OUTPUT, "1")), out, err, this.identityExpander);
        assertSame(out, result.err(), "2>&1 should make err point to out");
        assertSame(err, result.savedErr(), "savedErr should be original err for restore");
        assertNull(result.savedOut(), "stdout untouched");
    }

    @Test
    void redirectStdoutToStderr() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream());
        final PrintStream err = new PrintStream(new ByteArrayOutputStream());
        final RedirectionHandler.StreamSwap result =
                RedirectionHandler.redirectOutputForBuiltin(List.of(new Redirection(1, RedirectionType.DUP_OUTPUT, "2")), out, err, this.identityExpander);
        assertSame(err, result.out(), "1>&2 should make out point to err");
        assertSame(out, result.savedOut(), "savedOut should be original out for restore");
    }

    @Test
    void noRedirsLeavesStreamsUnchanged() {
        final PrintStream out = new PrintStream(new ByteArrayOutputStream());
        final PrintStream err = new PrintStream(new ByteArrayOutputStream());
        final RedirectionHandler.StreamSwap result =
                RedirectionHandler.redirectOutputForBuiltin(List.of(), out, err, this.identityExpander);
        assertSame(out, result.out());
        assertSame(err, result.err());
        assertNull(result.savedOut());
        assertNull(result.savedErr());
    }

    @Test
    void heredocFileNullContent() {
        assertNull(RedirectionHandler.heredocFile(null));
    }

    @Test
    void heredocFileRoundtrip() throws Exception {
        final java.io.File f = RedirectionHandler.heredocFile("alpha\nbeta\n");
        assertNotNull(f);
        assertTrue(f.isFile());
        assertEquals("alpha\nbeta\n", java.nio.file.Files.readString(f.toPath()));
    }
}
