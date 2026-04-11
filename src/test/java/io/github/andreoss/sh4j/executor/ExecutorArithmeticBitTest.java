/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: arithmetic bit.
 *
 * @since 1.0
 */
final class ExecutorArithmeticBitTest {

    @Test
    void arithmeticBitOr() {
        Assertions.assertEquals(
            "14",
            new Executor().expandVariables("$((12 | 6))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLogicalAndFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((1 && 2))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLogicalAndSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 && 2))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((0 && 2))", env));
    }

    @Test
    void arithmeticLogicalOrFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((0 || 5))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLogicalOrSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((0 || 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((0 || 0))", env));
    }

    @Test
    void arithmeticUnaryNotFirst() {
        Assertions.assertEquals("1", new Executor().expandVariables("$((!0))", new HashMap<>()));
    }

    @Test
    void arithmeticUnaryNotSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((!0))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((!5))", env));
    }

    @Test
    void arithmeticUnaryBitNotFirst() {
        Assertions.assertEquals("-1", new Executor().expandVariables("$((~0))", new HashMap<>()));
    }

    @Test
    void arithmeticUnaryBitNotSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((~0))", env);
        Assertions.assertEquals("-6", executor.expandVariables("$((~5))", env));
    }

    @Test
    void arithmeticTernaryTrue() {
        Assertions.assertEquals(
            "10",
            new Executor().expandVariables("$((1 ? 10 : 20))", new HashMap<>())
        );
    }

    @Test
    void arithmeticTernaryFalse() {
        Assertions.assertEquals(
            "20",
            new Executor().expandVariables("$((0 ? 10 : 20))", new HashMap<>())
        );
    }

    @Test
    void arithmeticTernaryNestedFirst() {
        Assertions.assertEquals(
            "30",
            new Executor().expandVariables("$((1 ? 2 ? 30 : 40 : 50))", new HashMap<>())
        );
    }

    @Test
    void arithmeticTernaryNestedSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env);
        Assertions.assertEquals("40", executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env));
    }

    @Test
    void arithmeticTernaryNestedThird() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env);
        executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env);
        Assertions.assertEquals("50", executor.expandVariables("$((0 ? 30 : 40 ? 50 : 60))", env));
    }

    @Test
    void arithmeticTernaryWithComparisonFirst() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        Assertions.assertEquals("1", executor.expandVariables("$((x > 3 ? 1 : 0))", env));
    }

    @Test
    void arithmeticTernaryWithComparisonSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        executor.expandVariables("$((x > 3 ? 1 : 0))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((x > 10 ? 1 : 0))", env));
    }

    @Test
    void arithmeticTernaryArithmeticBranchesFirst() {
        Assertions.assertEquals(
            "7",
            new Executor().expandVariables("$((1 ? 3 + 4 : 99))", new HashMap<>())
        );
    }

    @Test
    void arithmeticTernaryArithmeticBranchesSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? 3 + 4 : 99))", env);
        Assertions.assertEquals("99", executor.expandVariables("$((0 ? 3 + 4 : 99))", env));
    }

    @Test
    void arithmeticAssignFirst() {
        Assertions.assertEquals("5", new Executor().expandVariables("$((x = 5))", new HashMap<>()));
    }

    @Test
    void arithmeticAssignSecond() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((x = 5))", env);
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticPlusAssignFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("13", new Executor().expandVariables("$((x += 3))", env));
    }

    @Test
    void arithmeticPlusAssignSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        new Executor().expandVariables("$((x += 3))", env);
        Assertions.assertEquals("13", env.get("x"));
    }

    @Test
    void arithmeticMinusAssignFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("7", new Executor().expandVariables("$((x -= 3))", env));
    }

    @Test
    void arithmeticMinusAssignSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        new Executor().expandVariables("$((x -= 3))", env);
        Assertions.assertEquals("7", env.get("x"));
    }

    @Test
    void arithmeticMulDivModAssignFirst() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("30", executor.expandVariables("$((x *= 3))", env));
    }

    @Test
    void arithmeticMulDivModAssignSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        executor.expandVariables("$((x *= 3))", env);
        env.put("y", "20");
        Assertions.assertEquals("4", executor.expandVariables("$((y /= 5))", env));
    }

    @Test
    void arithmeticMulDivModAssignThird() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        executor.expandVariables("$((x *= 3))", env);
        env.put("y", "20");
        executor.expandVariables("$((y /= 5))", env);
        env.put("z", "23");
        Assertions.assertEquals("3", executor.expandVariables("$((z %= 5))", env));
    }

    @Test
    void arithmeticShiftAssignFirst() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        Assertions.assertEquals("32", executor.expandVariables("$((x <<= 3))", env));
    }

    @Test
    void arithmeticShiftAssignSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        executor.expandVariables("$((x <<= 3))", env);
        env.put("y", "32");
        Assertions.assertEquals("4", executor.expandVariables("$((y >>= 3))", env));
    }

    @Test
    void arithmeticBitAssignFirst() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        Assertions.assertEquals("4", executor.expandVariables("$((x &= 6))", env));
    }

    @Test
    void arithmeticBitAssignSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        executor.expandVariables("$((x &= 6))", env);
        env.put("y", "12");
        Assertions.assertEquals("10", executor.expandVariables("$((y ^= 6))", env));
    }

    @Test
    void arithmeticBitAssignThird() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        executor.expandVariables("$((x &= 6))", env);
        env.put("y", "12");
        executor.expandVariables("$((y ^= 6))", env);
        env.put("z", "12");
        Assertions.assertEquals("14", executor.expandVariables("$((z |= 6))", env));
    }

    @Test
    void arithmeticAssignRightAssocFirst() {
        Assertions.assertEquals(
            "7",
            new Executor().expandVariables("$((a = b = 7))", new HashMap<>())
        );
    }

    @Test
    void arithmeticAssignRightAssocSecond() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((a = b = 7))", env);
        Assertions.assertEquals("7", env.get("a"));
    }

    @Test
    void arithmeticAssignRightAssocThird() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((a = b = 7))", env);
        Assertions.assertEquals("7", env.get("b"));
    }

    @Test
    void arithmeticShortCircuitAndSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((0 && (x = 99)))", env);
        Assertions.assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticShortCircuitOrSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((1 || (x = 99)))", env);
        Assertions.assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticTernaryShortCircuitFirst() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        Assertions.assertEquals("1", env.get("a"));
    }

    @Test
    void arithmeticTernaryShortCircuitSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        Assertions.assertEquals(null, env.get("b"));
    }

    @Test
    void arithmeticTernaryShortCircuitThird() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        Assertions.assertEquals(null, env.get("c"));
    }
}
