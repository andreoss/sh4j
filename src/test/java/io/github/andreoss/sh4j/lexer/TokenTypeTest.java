/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for TokenType.
 * @since 1.0
 */
public final class TokenTypeTest {

    @Test
    public void allEnumValuesPresent() {
        assertThat(
            "TokenType should declare at least one constant",
            TokenType.values().length,
            not(equalTo(0))
        );
    }

    @Test
    public void valueOfAllTypes() {
        assertThat(
            "every enum constant should round-trip through valueOf",
            Stream.of(TokenType.values())
                .map(TokenType::name)
                .map(TokenType::valueOf)
                .collect(Collectors.toList()),
            equalTo(Arrays.asList(TokenType.values()))
        );
    }
}
