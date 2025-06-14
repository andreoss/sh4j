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

    /** Arith. */
    private final ArithmeticEvaluator arith = new ArithmeticEvaluator();

    /** Env. */
    private final Map<String, String> env = new HashMap<>();

    /** Tests addition. */
    @Test
    void add() {
        Assertions.assertEquals("7", this.arith.evalArithmetic("3 + 4", this.env));
    }

    @Test
    void sub() {
        Assertions.assertEquals("3", this.arith.evalArithmetic("7 - 4", this.env));
    }

    @Test
    void mul() {
        Assertions.assertEquals("12", this.arith.evalArithmetic("3 * 4", this.env));
    }

    @Test
    void div() {
        Assertions.assertEquals("2", this.arith.evalArithmetic("9 / 4", this.env));
    }

    @Test
    void mod() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("9 % 4", this.env));
    }

    @Test
    void precedence() {
        Assertions.assertEquals("14", this.arith.evalArithmetic("2 + 3 * 4", this.env));
    }

    @Test
    void parens() {
        Assertions.assertEquals("20", this.arith.evalArithmetic("(2 + 3) * 4", this.env));
    }

    @Test
    void shiftLeft() {
        Assertions.assertEquals("32", this.arith.evalArithmetic("4 << 3", this.env));
    }

    @Test
    void shiftRight() {
        Assertions.assertEquals("4", this.arith.evalArithmetic("32 >> 3", this.env));
    }

    @Test
    void ltOperator() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("3 < 5", this.env));
    }

    @Test
    void geOperator() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("5 >= 5", this.env));
    }

    @Test
    void eqEq() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("5 == 5", this.env));
    }

    @Test
    void neq() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("5 != 4", this.env));
    }

    @Test
    void bitAnd() {
        Assertions.assertEquals("4", this.arith.evalArithmetic("12 & 6", this.env));
    }

    @Test
    void bitOr() {
        Assertions.assertEquals("14", this.arith.evalArithmetic("12 | 6", this.env));
    }

    @Test
    void bitXor() {
        Assertions.assertEquals("10", this.arith.evalArithmetic("12 ^ 6", this.env));
    }

    @Test
    void logicalAnd() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("1 && 2", this.env));
    }

    @Test
    void logicalOr() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("0 || 5", this.env));
    }

    @Test
    void unaryNot() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("!0", this.env));
    }

    @Test
    void unaryBitNot() {
        Assertions.assertEquals("-1", this.arith.evalArithmetic("~0", this.env));
    }

    @Test
    void ternaryTrue() {
        Assertions.assertEquals("10", this.arith.evalArithmetic("1 ? 10 : 20", this.env));
    }

    @Test
    void ternaryFalse() {
        Assertions.assertEquals("20", this.arith.evalArithmetic("0 ? 10 : 20", this.env));
    }

    @Test void assign() {
        Assertions.assertEquals("5", this.arith.evalArithmetic("x = 5", this.env));
        Assertions.assertEquals("5", this.env.get("x"));
    }

    @Test void plusAssign() {
        this.env.put("y", "10");
        Assertions.assertEquals("13", this.arith.evalArithmetic("y += 3", this.env));
        Assertions.assertEquals("13", this.env.get("y"));
    }

    @Test void shortCircuitAndSuppressesAssignment() {
        this.arith.evalArithmetic("0 && (z = 99)", this.env);
        Assertions.assertNull(this.env.get("z"));
    }

    @Test void shortCircuitOrSuppressesAssignment() {
        this.arith.evalArithmetic("1 || (q = 99)", this.env);
        Assertions.assertNull(this.env.get("q"));
    }

    @Test void varNameResolvesToEnvValue() {
        this.env.put("v", "7");
        Assertions.assertEquals("9", this.arith.evalArithmetic("v + 2", this.env));
    }

    @Test void nonNumericVarResolvesToZero() {
        this.env.put("v", "abc");
        Assertions.assertEquals("2", this.arith.evalArithmetic("v + 2", this.env));
    }

    @Test void divByZeroIsZero() {
        Assertions.assertEquals("0", this.arith.evalArithmetic("5 / 0", this.env));
    }

    @Test void modByZeroIsZero() {
        Assertions.assertEquals("0", this.arith.evalArithmetic("5 % 0", this.env));
    }

    @Test void emptyExprIsZero() {
        Assertions.assertEquals("0", this.arith.evalArithmetic("", this.env));
    }

    @Test void whitespaceOnlyIsZero() {
        Assertions.assertEquals("0", this.arith.evalArithmetic("   ", this.env));
    }

    @Test void unmatchedParenStillEvalsInner() {
        Assertions.assertEquals("3", this.arith.evalArithmetic("(1+2", this.env));
    }

    @Test void leadingPlus() {
        Assertions.assertEquals("5", this.arith.evalArithmetic("+5", this.env));
    }

    @Test void doubleNegation() {
        Assertions.assertEquals("5", this.arith.evalArithmetic("--5", this.env));
    }

    @Test void doubleBitwiseNot() {
        Assertions.assertEquals("5", this.arith.evalArithmetic("~~5", this.env));
    }

    @Test void doubleLogicalNot() {
        Assertions.assertEquals("1", this.arith.evalArithmetic("!!5", this.env));
    }

    @Test void compoundAssignToUnsetVar() {
        Assertions.assertEquals("5", this.arith.evalArithmetic("new += 5", this.env));
        Assertions.assertEquals("5", this.env.get("new"));
    }

    @Test void chainedAssignmentRightAssoc() {
        Assertions.assertEquals("7", this.arith.evalArithmetic("a = b = c = 7", this.env));
        Assertions.assertEquals("7", this.env.get("a"));
        Assertions.assertEquals("7", this.env.get("b"));
        Assertions.assertEquals("7", this.env.get("c"));
    }

    @Test void ternaryFalseEvaluatesElseOnly() {
        this.arith.evalArithmetic("0 ? (z = 1) : (z = 2)", this.env);
        Assertions.assertEquals("2", this.env.get("z"));
    }

    @Test void numericLiteralWithLeadingZeros() {
        Assertions.assertEquals("42", this.arith.evalArithmetic("042", this.env));
    }
}
