/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArithmeticEvaluatorTest {

    private final ArithmeticEvaluator e = new ArithmeticEvaluator();

    private final Map<String, String> env = new HashMap<>();

    @Test
    void add() {
        assertEquals("7", this.e.evalArithmetic("3 + 4", this.env));
    }

    @Test
    void sub() {
        assertEquals("3", this.e.evalArithmetic("7 - 4", this.env));
    }

    @Test
    void mul() {
        assertEquals("12", this.e.evalArithmetic("3 * 4", this.env));
    }

    @Test
    void div() {
        assertEquals("2", this.e.evalArithmetic("9 / 4", this.env));
    }

    @Test
    void mod() {
        assertEquals("1", this.e.evalArithmetic("9 % 4", this.env));
    }

    @Test
    void precedence() {
        assertEquals("14", this.e.evalArithmetic("2 + 3 * 4", this.env));
    }

    @Test
    void parens() {
        assertEquals("20", this.e.evalArithmetic("(2 + 3) * 4", this.env));
    }

    @Test
    void shiftLeft() {
        assertEquals("32", this.e.evalArithmetic("4 << 3", this.env));
    }

    @Test
    void shiftRight() {
        assertEquals("4", this.e.evalArithmetic("32 >> 3", this.env));
    }

    @Test
    void lt() {
        assertEquals("1", this.e.evalArithmetic("3 < 5", this.env));
    }

    @Test
    void ge() {
        assertEquals("1", this.e.evalArithmetic("5 >= 5", this.env));
    }

    @Test
    void eqEq() {
        assertEquals("1", this.e.evalArithmetic("5 == 5", this.env));
    }

    @Test
    void neq() {
        assertEquals("1", this.e.evalArithmetic("5 != 4", this.env));
    }

    @Test
    void bitAnd() {
        assertEquals("4", this.e.evalArithmetic("12 & 6", this.env));
    }

    @Test
    void bitOr() {
        assertEquals("14", this.e.evalArithmetic("12 | 6", this.env));
    }

    @Test
    void bitXor() {
        assertEquals("10", this.e.evalArithmetic("12 ^ 6", this.env));
    }

    @Test
    void logicalAnd() {
        assertEquals("1", this.e.evalArithmetic("1 && 2", this.env));
    }

    @Test
    void logicalOr() {
        assertEquals("1", this.e.evalArithmetic("0 || 5", this.env));
    }

    @Test
    void unaryNot() {
        assertEquals("1", this.e.evalArithmetic("!0", this.env));
    }

    @Test
    void unaryBitNot() {
        assertEquals("-1", this.e.evalArithmetic("~0", this.env));
    }

    @Test
    void ternaryTrue() {
        assertEquals("10", this.e.evalArithmetic("1 ? 10 : 20", this.env));
    }

    @Test
    void ternaryFalse() {
        assertEquals("20", this.e.evalArithmetic("0 ? 10 : 20", this.env));
    }

    @Test void assign() {
        assertEquals("5", this.e.evalArithmetic("x = 5", this.env));
        assertEquals("5", this.env.get("x"));
    }

    @Test void plusAssign() {
        this.env.put("y", "10");
        assertEquals("13", this.e.evalArithmetic("y += 3", this.env));
        assertEquals("13", this.env.get("y"));
    }

    @Test void shortCircuitAndSuppressesAssignment() {
        this.e.evalArithmetic("0 && (z = 99)", this.env);
        assertNull(this.env.get("z"));
    }

    @Test void shortCircuitOrSuppressesAssignment() {
        this.e.evalArithmetic("1 || (q = 99)", this.env);
        assertNull(this.env.get("q"));
    }

    @Test void varNameResolvesToEnvValue() {
        this.env.put("v", "7");
        assertEquals("9", this.e.evalArithmetic("v + 2", this.env));
    }

    @Test void nonNumericVarResolvesToZero() {
        this.env.put("v", "abc");
        assertEquals("2", this.e.evalArithmetic("v + 2", this.env));
    }

    @Test void divByZeroIsZero() {
        assertEquals("0", this.e.evalArithmetic("5 / 0", this.env));
    }

    @Test void modByZeroIsZero() {
        assertEquals("0", this.e.evalArithmetic("5 % 0", this.env));
    }

    @Test void emptyExprIsZero() {
        assertEquals("0", this.e.evalArithmetic("", this.env));
    }

    @Test void whitespaceOnlyIsZero() {
        assertEquals("0", this.e.evalArithmetic("   ", this.env));
    }

    @Test void unmatchedParenStillEvalsInner() {
        // missing closing paren — parser stops at end of input
        assertEquals("3", this.e.evalArithmetic("(1+2", this.env));
    }

    @Test void leadingPlus() {
        assertEquals("5", this.e.evalArithmetic("+5", this.env));
    }

    @Test void doubleNegation() {
        assertEquals("5", this.e.evalArithmetic("--5", this.env));
    }

    @Test void doubleBitwiseNot() {
        assertEquals("5", this.e.evalArithmetic("~~5", this.env));
    }

    @Test void doubleLogicalNot() {
        assertEquals("1", this.e.evalArithmetic("!!5", this.env));
    }

    @Test void compoundAssignToUnsetVar() {
        // unset var treated as 0
        assertEquals("5", this.e.evalArithmetic("new += 5", this.env));
        assertEquals("5", this.env.get("new"));
    }

    @Test void chainedAssignmentRightAssoc() {
        assertEquals("7", this.e.evalArithmetic("a = b = c = 7", this.env));
        assertEquals("7", this.env.get("a"));
        assertEquals("7", this.env.get("b"));
        assertEquals("7", this.env.get("c"));
    }

    @Test void ternaryFalseEvaluatesElseOnly() {
        // arithMute should suppress the THEN side's assignment
        this.e.evalArithmetic("0 ? (z = 1) : (z = 2)", this.env);
        assertEquals("2", this.env.get("z"));
    }

    @Test void numericLiteralWithLeadingZeros() {
        assertEquals("42", this.e.evalArithmetic("042", this.env));
    }
}
