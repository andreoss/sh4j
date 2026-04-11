/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for TokenType.
 *
 * @since 1.0
 */
final class TokenTypeTest {

    @Test
    void allEnumValuesPresent() {
        MatcherAssert.assertThat(
            "TokenType should declare at least one constant",
            TokenType.values().length,
            CoreMatchers.not(CoreMatchers.equalTo(0))
        );
    }

    @Test
    void valueOfAllTypes() {
        MatcherAssert.assertThat(
            "every enum constant should round-trip through valueOf",
            Stream.of(TokenType.values()).map(TokenType::name).map(TokenType::valueOf).collect(
                Collectors.toList()
            ),
            CoreMatchers.equalTo(Arrays.asList(TokenType.values()))
        );
    }
}
