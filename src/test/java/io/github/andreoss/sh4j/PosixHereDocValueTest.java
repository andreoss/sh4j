/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: expansion values in here-document bodies (POSIX 2.7.4).
 *
 * @since 1.0
 */
final class PosixHereDocValueTest {

    /**
     * POSIX 2.7.4: the value of an expansion in an unquoted here-document
     * body is copied without escape characters.
     */
    @Test
    void valueOfParameterKeepsNoEscapes() {
        Assertions.assertEquals(
            String.format("a\"b\\c|a\"b\\c%n"),
            Sh4j.INSTANCE.out(String.format("y='a\"b\\c'%ncat <<E%n$y|${y}%nE%n"))
        );
    }

    @Test
    void valueOfParameterKeepsNoEscapesForBuiltin() {
        Assertions.assertEquals(
            String.format("a\"b\\c%n"),
            Sh4j.INSTANCE.out(
                String.format("y='a\"b\\c'%nread -r l <<E%n$y%nE%nprintf '%%s\\n' \"$l\"%n")
            )
        );
    }

    @Test
    void valueOfCommandSubstitutionKeepsNoEscapes() {
        Assertions.assertEquals(
            String.format("p\\q\"r%n"),
            Sh4j.INSTANCE.out(String.format("cat <<E%n$(printf '%%s' 'p\\q\"r')%nE%n"))
        );
    }
}
