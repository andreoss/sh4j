/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for ArithmeticEvaluator.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class ArithmeticEvaluatorTest {

    /** Tests addition. */
    @Test
    public void add() {
        Assert.assertEquals(
            "7",
            new ArithmeticEvaluator().evalArithmetic("3 + 4", new HashMap<>())
        );
    }

    @Test
    public void sub() {
        Assert.assertEquals(
            "3",
            new ArithmeticEvaluator().evalArithmetic("7 - 4", new HashMap<>())
        );
    }

    @Test
    public void mul() {
        Assert.assertEquals(
            "12",
            new ArithmeticEvaluator().evalArithmetic("3 * 4", new HashMap<>())
        );
    }

    @Test
    public void div() {
        Assert.assertEquals(
            "2",
            new ArithmeticEvaluator().evalArithmetic("9 / 4", new HashMap<>())
        );
    }

    @Test
    public void mod() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("9 % 4", new HashMap<>())
        );
    }

    @Test
    public void precedence() {
        Assert.assertEquals(
            "14",
            new ArithmeticEvaluator().evalArithmetic("2 + 3 * 4", new HashMap<>())
        );
    }

    @Test
    public void parens() {
        Assert.assertEquals(
            "20",
            new ArithmeticEvaluator().evalArithmetic("(2 + 3) * 4", new HashMap<>())
        );
    }

    @Test
    public void shiftLeft() {
        Assert.assertEquals(
            "32",
            new ArithmeticEvaluator().evalArithmetic("4 << 3", new HashMap<>())
        );
    }

    @Test
    public void shiftRight() {
        Assert.assertEquals(
            "4",
            new ArithmeticEvaluator().evalArithmetic("32 >> 3", new HashMap<>())
        );
    }

    @Test
    public void ltOperator() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("3 < 5", new HashMap<>())
        );
    }

    @Test
    public void geOperator() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("5 >= 5", new HashMap<>())
        );
    }

    @Test
    public void eqEq() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("5 == 5", new HashMap<>())
        );
    }

    @Test
    public void neq() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("5 != 4", new HashMap<>())
        );
    }

    @Test
    public void bitAnd() {
        Assert.assertEquals(
            "4",
            new ArithmeticEvaluator().evalArithmetic("12 & 6", new HashMap<>())
        );
    }

    @Test
    public void bitOr() {
        Assert.assertEquals(
            "14",
            new ArithmeticEvaluator().evalArithmetic("12 | 6", new HashMap<>())
        );
    }

    @Test
    public void bitXor() {
        Assert.assertEquals(
            "10",
            new ArithmeticEvaluator().evalArithmetic("12 ^ 6", new HashMap<>())
        );
    }

    @Test
    public void logicalAnd() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("1 && 2", new HashMap<>())
        );
    }

    @Test
    public void logicalOr() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("0 || 5", new HashMap<>())
        );
    }

    @Test
    public void unaryNot() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("!0", new HashMap<>())
        );
    }

    @Test
    public void unaryBitNot() {
        Assert.assertEquals(
            "-1",
            new ArithmeticEvaluator().evalArithmetic("~0", new HashMap<>())
        );
    }

    @Test
    public void ternaryTrue() {
        Assert.assertEquals(
            "10",
            new ArithmeticEvaluator().evalArithmetic("1 ? 10 : 20", new HashMap<>())
        );
    }

    @Test
    public void ternaryFalse() {
        Assert.assertEquals(
            "20",
            new ArithmeticEvaluator().evalArithmetic("0 ? 10 : 20", new HashMap<>())
        );
    }

    @Test public void assign() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("x = 5", env));
        Assert.assertEquals("5", env.get("x"));
    }

    @Test public void plusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("y", "10");
        Assert.assertEquals("13", new ArithmeticEvaluator().evalArithmetic("y += 3", env));
        Assert.assertEquals("13", env.get("y"));
    }

    @Test public void shortCircuitAndSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 && (z = 99)", env);
        Assert.assertNull(env.get("z"));
    }

    @Test public void shortCircuitOrSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("1 || (q = 99)", env);
        Assert.assertNull(env.get("q"));
    }

    @Test public void varNameResolvesToEnvValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "7");
        Assert.assertEquals("9", new ArithmeticEvaluator().evalArithmetic("v + 2", env));
    }

    @Test public void nonNumericVarResolvesToZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "abc");
        Assert.assertEquals("2", new ArithmeticEvaluator().evalArithmetic("v + 2", env));
    }

    @Test public void divByZeroIsZero() {
        Assert.assertEquals(
            "0",
            new ArithmeticEvaluator().evalArithmetic("5 / 0", new HashMap<>())
        );
    }

    @Test public void modByZeroIsZero() {
        Assert.assertEquals(
            "0",
            new ArithmeticEvaluator().evalArithmetic("5 % 0", new HashMap<>())
        );
    }

    @Test public void emptyExprIsZero() {
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("", new HashMap<>()));
    }

    @Test public void whitespaceOnlyIsZero() {
        Assert.assertEquals(
            "0",
            new ArithmeticEvaluator().evalArithmetic("   ", new HashMap<>())
        );
    }

    @Test public void unmatchedParenStillEvalsInner() {
        Assert.assertEquals(
            "3",
            new ArithmeticEvaluator().evalArithmetic("(1+2", new HashMap<>())
        );
    }

    @Test public void leadingPlus() {
        Assert.assertEquals(
            "5",
            new ArithmeticEvaluator().evalArithmetic("+5", new HashMap<>())
        );
    }

    @Test public void doubleNegation() {
        Assert.assertEquals(
            "5",
            new ArithmeticEvaluator().evalArithmetic("--5", new HashMap<>())
        );
    }

    @Test public void doubleBitwiseNot() {
        Assert.assertEquals(
            "5",
            new ArithmeticEvaluator().evalArithmetic("~~5", new HashMap<>())
        );
    }

    @Test public void doubleLogicalNot() {
        Assert.assertEquals(
            "1",
            new ArithmeticEvaluator().evalArithmetic("!!5", new HashMap<>())
        );
    }

    @Test public void compoundAssignToUnsetVar() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("new += 5", env));
        Assert.assertEquals("5", env.get("new"));
    }

    @Test public void chainedAssignmentRightAssoc() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(
            "7",
            new ArithmeticEvaluator().evalArithmetic("a = b = c = 7", env)
        );
        Assert.assertEquals("7", env.get("a"));
        Assert.assertEquals("7", env.get("b"));
        Assert.assertEquals("7", env.get("c"));
    }

    @Test public void ternaryFalseEvaluatesElseOnly() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 ? (z = 1) : (z = 2)", env);
        Assert.assertEquals("2", env.get("z"));
    }

    @Test public void numericLiteralWithLeadingZeros() {
        // POSIX 2.6.4: leading 0 is octal per the C standard; 042_8 = 34_10.
        Assert.assertEquals(
            "34",
            new ArithmeticEvaluator().evalArithmetic("042", new HashMap<>())
        );
    }

    @Test public void assignmentWithWhitespace() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("x = 5", env));
        Assert.assertEquals("5", env.get("x"));
        Assert.assertEquals("8", new ArithmeticEvaluator().evalArithmetic("x += 3", env));
        Assert.assertEquals("8", env.get("x"));
    }

    @Test public void shiftAssignments() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("2", new ArithmeticEvaluator().evalArithmetic("x = 2", env));
        Assert.assertEquals("16", new ArithmeticEvaluator().evalArithmetic("x <<= 3", env));
        Assert.assertEquals("16", env.get("x"));
        Assert.assertEquals("8", new ArithmeticEvaluator().evalArithmetic("x >>= 1", env));
    }

    @Test public void relationalSingleCharacterOperators() {
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("2<3", new HashMap<>()));
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("3>2", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("2<1", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("2>3", new HashMap<>()));
    }

    @Test public void ternaryWithoutColon() {
        Assert.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("1?5", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("0?5", new HashMap<>()));
    }

    @Test public void unaryOperators() {
        Assert.assertEquals("3", new ArithmeticEvaluator().evalArithmetic("+3", new HashMap<>()));
        Assert.assertEquals("-3", new ArithmeticEvaluator().evalArithmetic("-3", new HashMap<>()));
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("!0", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("!2", new HashMap<>()));
        Assert.assertEquals("-1", new ArithmeticEvaluator().evalArithmetic("~0", new HashMap<>()));
    }

    @Test public void unbalancedParenAndTrailingWhitespace() {
        Assert.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("(2+3", new HashMap<>()));
        Assert.assertEquals("5", new ArithmeticEvaluator().evalArithmetic("2 + 3   ", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("", new HashMap<>()));
    }

    @Test public void numberBasesAndInvalidDigits() {
        Assert.assertEquals("15", new ArithmeticEvaluator().evalArithmetic("0Xf", new HashMap<>()));
        Assert.assertEquals("63", new ArithmeticEvaluator().evalArithmetic("077", new HashMap<>()));
        Assert.assertEquals("9", new ArithmeticEvaluator().evalArithmetic("09", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("@", new HashMap<>()));
    }

    @Test public void invalidEnvValueBecomesZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "not-a-number");
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("x", env));
    }

    @Test public void compoundAssignmentVersusShift() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("x = 1", env));
        Assert.assertEquals("2", new ArithmeticEvaluator().evalArithmetic("x <<= 1", env));
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("x <= 2", env));
    }

    @Test public void readNumberBoundaryForms() {
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("0x0", new HashMap<>()));
        Assert.assertEquals("2748", new ArithmeticEvaluator().evalArithmetic("0XABC", new HashMap<>()));
        Assert.assertEquals("8", new ArithmeticEvaluator().evalArithmetic("08", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("0", new HashMap<>()));
        Assert.assertEquals("123", new ArithmeticEvaluator().evalArithmetic("123abc", new HashMap<>()));
    }

    @Test public void trailingOperatorsAndSpacedTernary() {
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("1+", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("1*", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("1<", new HashMap<>()));
        Assert.assertEquals("2", new ArithmeticEvaluator().evalArithmetic("1 ? 2 : 3", new HashMap<>()));
        Assert.assertEquals("3", new ArithmeticEvaluator().evalArithmetic("0 ? 2 : 3", new HashMap<>()));
    }

    @Test public void singleCharacterRelationalBranches() {
        Assert.assertEquals("4", new ArithmeticEvaluator().evalArithmetic("2<<1", new HashMap<>()));
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("2<=2", new HashMap<>()));
        Assert.assertEquals("0", new ArithmeticEvaluator().evalArithmetic("2<1", new HashMap<>()));
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("2>1", new HashMap<>()));
        Assert.assertEquals("1", new ArithmeticEvaluator().evalArithmetic("2>=2", new HashMap<>()));
    }
}
