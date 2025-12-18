/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for ArithmeticEvaluator.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", "PMD.ExcessivePublicCount"})
public final class ArithmeticEvaluatorTest {

    /** Tests addition. */
    @Test
    public void add() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 + 4", new HashMap<>()),
            equalTo("7")
        );
    }

    @Test
    public void sub() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("7 - 4", new HashMap<>()),
            equalTo("3")
        );
    }

    @Test
    public void mul() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 * 4", new HashMap<>()),
            equalTo("12")
        );
    }

    @Test
    public void div() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("9 / 4", new HashMap<>()),
            equalTo("2")
        );
    }

    @Test
    public void mod() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("9 % 4", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void precedence() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2 + 3 * 4", new HashMap<>()),
            equalTo("14")
        );
    }

    @Test
    public void parens() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("(2 + 3) * 4", new HashMap<>()),
            equalTo("20")
        );
    }

    @Test
    public void shiftLeft() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("4 << 3", new HashMap<>()),
            equalTo("32")
        );
    }

    @Test
    public void shiftRight() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("32 >> 3", new HashMap<>()),
            equalTo("4")
        );
    }

    @Test
    public void ltOperator() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 < 5", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void geOperator() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 >= 5", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void eqEq() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 == 5", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void neq() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 != 4", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void bitAnd() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("12 & 6", new HashMap<>()),
            equalTo("4")
        );
    }

    @Test
    public void bitOr() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("12 | 6", new HashMap<>()),
            equalTo("14")
        );
    }

    @Test
    public void bitXor() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("12 ^ 6", new HashMap<>()),
            equalTo("10")
        );
    }

    @Test
    public void logicalAnd() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1 && 2", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void logicalOr() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0 || 5", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void unaryNot() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("!0", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void unaryBitNot() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("~0", new HashMap<>()),
            equalTo("-1")
        );
    }

    @Test
    public void ternaryTrue() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1 ? 10 : 20", new HashMap<>()),
            equalTo("10")
        );
    }

    @Test
    public void ternaryFalse() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0 ? 10 : 20", new HashMap<>()),
            equalTo("20")
        );
    }

    @Test
    public void assign() {
        final Map<String, String> env = new HashMap<>();
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 5", env),
            equalTo("5")
        );
        assertThat(env.get("x"), equalTo("5"));
    }

    @Test
    public void plusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("y", "10");
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("y += 3", env),
            equalTo("13")
        );
        assertThat(env.get("y"), equalTo("13"));
    }

    @Test
    public void shortCircuitAndSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 && (z = 99)", env);
        assertThat(env.get("z"), nullValue());
    }

    @Test
    public void shortCircuitOrSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("1 || (q = 99)", env);
        assertThat(env.get("q"), nullValue());
    }

    @Test
    public void varNameResolvesToEnvValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "7");
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("v + 2", env),
            equalTo("9")
        );
    }

    @Test
    public void nonNumericVarResolvesToZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "abc");
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("v + 2", env),
            equalTo("2")
        );
    }

    @Test
    public void divByZeroIsZero() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 / 0", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void modByZeroIsZero() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 % 0", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void emptyExprIsZero() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void whitespaceOnlyIsZero() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("   ", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void unmatchedParenStillEvalsInner() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("(1+2", new HashMap<>()),
            equalTo("3")
        );
    }

    @Test
    public void leadingPlus() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("+5", new HashMap<>()),
            equalTo("5")
        );
    }

    @Test
    public void doubleNegation() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("--5", new HashMap<>()),
            equalTo("5")
        );
    }

    @Test
    public void doubleBitwiseNot() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("~~5", new HashMap<>()),
            equalTo("5")
        );
    }

    @Test
    public void doubleLogicalNot() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("!!5", new HashMap<>()),
            equalTo("1")
        );
    }

    @Test
    public void compoundAssignToUnsetVar() {
        final Map<String, String> env = new HashMap<>();
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("new += 5", env),
            equalTo("5")
        );
        assertThat(env.get("new"), equalTo("5"));
    }

    @Test
    public void chainedAssignmentRightAssoc() {
        final Map<String, String> env = new HashMap<>();
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("a = b = c = 7", env),
            equalTo("7")
        );
        assertThat(env.get("a"), equalTo("7"));
        assertThat(env.get("b"), equalTo("7"));
        assertThat(env.get("c"), equalTo("7"));
    }

    @Test
    public void ternaryFalseEvaluatesElseOnly() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 ? (z = 1) : (z = 2)", env);
        assertThat(env.get("z"), equalTo("2"));
    }

    @Test
    public void numericLiteralWithLeadingZeros() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("042", new HashMap<>()),
            equalTo("34")
        );
    }

    @Test
    public void assignmentWithWhitespace() {
        final Map<String, String> env = new HashMap<>();
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 5", env),
            equalTo("5")
        );
        assertThat(env.get("x"), equalTo("5"));
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x += 3", env),
            equalTo("8")
        );
        assertThat(env.get("x"), equalTo("8"));
    }

    @Test
    public void shiftAssignments() {
        final Map<String, String> env = new HashMap<>();
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 2", env),
            equalTo("2")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x <<= 3", env),
            equalTo("16")
        );
        assertThat(env.get("x"), equalTo("16"));
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x >>= 1", env),
            equalTo("8")
        );
    }

    @Test
    public void relationalSingleCharacterOperators() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<3", new HashMap<>()),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("3>2", new HashMap<>()),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<1", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2>3", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void ternaryWithoutColon() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1?5", new HashMap<>()),
            equalTo("5")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0?5", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void unaryOperators() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("+3", new HashMap<>()),
            equalTo("3")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("-3", new HashMap<>()),
            equalTo("-3")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("!0", new HashMap<>()),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("!2", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("~0", new HashMap<>()),
            equalTo("-1")
        );
    }

    @Test
    public void unbalancedParenAndTrailingWhitespace() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("(2+3", new HashMap<>()),
            equalTo("5")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2 + 3   ", new HashMap<>()),
            equalTo("5")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void numberBasesAndInvalidDigits() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0Xf", new HashMap<>()),
            equalTo("15")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("077", new HashMap<>()),
            equalTo("63")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("09", new HashMap<>()),
            equalTo("9")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("@", new HashMap<>()),
            equalTo("0")
        );
    }

    @Test
    public void invalidEnvValueBecomesZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "not-a-number");
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x", env),
            equalTo("0")
        );
    }

    @Test
    public void compoundAssignmentVersusShift() {
        final Map<String, String> env = new HashMap<>();
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 1", env),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x <<= 1", env),
            equalTo("2")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("x <= 2", env),
            equalTo("1")
        );
    }

    @Test
    public void readNumberBoundaryForms() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0x0", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0XABC", new HashMap<>()),
            equalTo("2748")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("08", new HashMap<>()),
            equalTo("8")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("123abc", new HashMap<>()),
            equalTo("123")
        );
    }

    @Test
    public void trailingOperatorsAndSpacedTernary() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1+", new HashMap<>()),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1*", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1<", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("1 ? 2 : 3", new HashMap<>()),
            equalTo("2")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("0 ? 2 : 3", new HashMap<>()),
            equalTo("3")
        );
    }

    @Test
    public void singleCharacterRelationalBranches() {
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<<1", new HashMap<>()),
            equalTo("4")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<=2", new HashMap<>()),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<1", new HashMap<>()),
            equalTo("0")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2>1", new HashMap<>()),
            equalTo("1")
        );
        assertThat(
            new ArithmeticEvaluator().evalArithmetic("2>=2", new HashMap<>()),
            equalTo("1")
        );
    }
}
