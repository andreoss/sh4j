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
 * Tests for ArithmeticEvaluator.
 * @since 1.0
 */
final class ArithmeticEvaluatorTest {

    /** Tests addition. */
    @Test
    void add() {
        Assertions.assertEquals(
            "7",
            new ArithmeticEvaluator().evalArithmetic("3 + 4", new HashMap<>())
        );
    }

    @Test
    void sub() {
        Assertions.assertEquals(
            "3",
            new ArithmeticEvaluator().evalArithmetic("7 - 4", new HashMap<>())
        );
    }

    @Test
    void mul() {
        Assertions.assertEquals(
            "12",
            new ArithmeticEvaluator().evalArithmetic("3 * 4", new HashMap<>())
        );
    }

    @Test
    void div() {
        Assertions.assertEquals(
            "2",
            new ArithmeticEvaluator().evalArithmetic("9 / 4", new HashMap<>())
        );
    }

    @Test
    void mod() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("9 % 4", new HashMap<>())
        );
    }

    @Test
    void precedence() {
        Assertions.assertEquals(
            "14",
            new ArithmeticEvaluator().evalArithmetic("2 + 3 * 4", new HashMap<>())
        );
    }

    @Test
    void parens() {
        Assertions.assertEquals(
            "20",
            new ArithmeticEvaluator().evalArithmetic("(2 + 3) * 4", new HashMap<>())
        );
    }

    @Test
    void shiftLeft() {
        Assertions.assertEquals(
            "32",
            new ArithmeticEvaluator().evalArithmetic("4 << 3", new HashMap<>())
        );
    }

    @Test
    void shiftRight() {
        Assertions.assertEquals(
            "4",
            new ArithmeticEvaluator().evalArithmetic("32 >> 3", new HashMap<>())
        );
    }

    @Test
    void ltOperator() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("3 < 5", new HashMap<>())
        );
    }

    @Test
    void geOperator() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("5 >= 5", new HashMap<>())
        );
    }

    @Test
    void eqEq() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("5 == 5", new HashMap<>())
        );
    }

    @Test
    void neq() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("5 != 4", new HashMap<>())
        );
    }

    @Test
    void bitAnd() {
        Assertions.assertEquals(
            "4",
            new ArithmeticEvaluator().evalArithmetic("12 & 6", new HashMap<>())
        );
    }

    @Test
    void bitOr() {
        Assertions.assertEquals(
            "14",
            new ArithmeticEvaluator().evalArithmetic("12 | 6", new HashMap<>())
        );
    }

    @Test
    void bitXor() {
        Assertions.assertEquals(
            "10",
            new ArithmeticEvaluator().evalArithmetic("12 ^ 6", new HashMap<>())
        );
    }

    @Test
    void logicalAnd() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("1 && 2", new HashMap<>())
        );
    }

    @Test
    void logicalOr() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("0 || 5", new HashMap<>())
        );
    }

    @Test
    void unaryNot() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("!0", new HashMap<>())
        );
    }

    @Test
    void unaryBitNot() {
        Assertions.assertEquals(
            "-1",
            new ArithmeticEvaluator().evalArithmetic("~0", new HashMap<>())
        );
    }

    @Test
    void ternaryTrue() {
        Assertions.assertEquals(
            "10",
            new ArithmeticEvaluator().evalArithmetic("1 ? 10 : 20", new HashMap<>())
        );
    }

    @Test
    void ternaryFalse() {
        Assertions.assertEquals(
            "20",
            new ArithmeticEvaluator().evalArithmetic("0 ? 10 : 20", new HashMap<>())
        );
    }

    @Test void assign() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("x = 5", env));
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test void plusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("y", "10");
        Assertions.assertEquals("13", new ArithmeticEvaluator().evalArithmetic("y += 3", env));
        Assertions.assertEquals("13", env.get("y"));
    }

    @Test void shortCircuitAndSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 && (z = 99)", env);
        Assertions.assertNull(env.get("z"));
    }

    @Test void shortCircuitOrSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("1 || (q = 99)", env);
        Assertions.assertNull(env.get("q"));
    }

    @Test void varNameResolvesToEnvValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "7");
        Assertions.assertEquals("9", new ArithmeticEvaluator().evalArithmetic("v + 2", env));
    }

    @Test void nonNumericVarResolvesToZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "abc");
        Assertions.assertEquals("2", new ArithmeticEvaluator().evalArithmetic("v + 2", env));
    }

    @Test void divByZeroIsZero() {
        Assertions.assertEquals(
            "0",
            new ArithmeticEvaluator().evalArithmetic("5 / 0", new HashMap<>())
        );
    }

    @Test void modByZeroIsZero() {
        Assertions.assertEquals(
            "0",
            new ArithmeticEvaluator().evalArithmetic("5 % 0", new HashMap<>())
        );
    }

    @Test void emptyExprIsZero() {
        Assertions.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("", new HashMap<>()));
    }

    @Test void whitespaceOnlyIsZero() {
        Assertions.assertEquals(
            "0",
            new ArithmeticEvaluator().evalArithmetic("   ", new HashMap<>())
        );
    }

    @Test void unmatchedParenStillEvalsInner() {
        Assertions.assertEquals(
            "3",
            new ArithmeticEvaluator().evalArithmetic("(1+2", new HashMap<>())
        );
    }

    @Test void leadingPlus() {
        Assertions.assertEquals(
            "5",
            new ArithmeticEvaluator().evalArithmetic("+5", new HashMap<>())
        );
    }

    @Test void doubleNegation() {
        Assertions.assertEquals(
            "5",
            new ArithmeticEvaluator().evalArithmetic("--5", new HashMap<>())
        );
    }

    @Test void doubleBitwiseNot() {
        Assertions.assertEquals(
            "5",
            new ArithmeticEvaluator().evalArithmetic("~~5", new HashMap<>())
        );
    }

    @Test void doubleLogicalNot() {
        Assertions.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("!!5", new HashMap<>())
        );
    }

    @Test void compoundAssignToUnsetVar() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("new += 5", env));
        Assertions.assertEquals("5", env.get("new"));
    }

    @Test void chainedAssignmentRightAssoc() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(
            "7",
            new ArithmeticEvaluator().evalArithmetic("a = b = c = 7", env)
        );
        Assertions.assertEquals("7", env.get("a"));
        Assertions.assertEquals("7", env.get("b"));
        Assertions.assertEquals("7", env.get("c"));
    }

    @Test void ternaryFalseEvaluatesElseOnly() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 ? (z = 1) : (z = 2)", env);
        Assertions.assertEquals("2", env.get("z"));
    }

    @Test void numericLiteralWithLeadingZeros() {
        Assertions.assertEquals(
            "42",
            new ArithmeticEvaluator().evalArithmetic("042", new HashMap<>())
        );
    }
}
