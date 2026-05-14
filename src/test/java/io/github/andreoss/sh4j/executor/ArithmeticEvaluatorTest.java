/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for ArithmeticEvaluator.
 *
 * @since 1.0
 */
final class ArithmeticEvaluatorTest {

    /** Tests addition. */
    @Test
    void add() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 + 4", new HashMap<>()),
            CoreMatchers.equalTo("7")
        );
    }

    @Test
    void sub() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("7 - 4", new HashMap<>()),
            CoreMatchers.equalTo("3")
        );
    }

    @Test
    void mul() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 * 4", new HashMap<>()),
            CoreMatchers.equalTo("12")
        );
    }

    @Test
    void div() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("9 / 4", new HashMap<>()),
            CoreMatchers.equalTo("2")
        );
    }

    @Test
    void mod() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("9 % 4", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void precedence() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2 + 3 * 4", new HashMap<>()),
            CoreMatchers.equalTo("14")
        );
    }

    @Test
    void parens() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("(2 + 3) * 4", new HashMap<>()),
            CoreMatchers.equalTo("20")
        );
    }

    @Test
    void shiftLeft() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("4 << 3", new HashMap<>()),
            CoreMatchers.equalTo("32")
        );
    }

    @Test
    void shiftRight() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("32 >> 3", new HashMap<>()),
            CoreMatchers.equalTo("4")
        );
    }

    @Test
    void ltOperator() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 < 5", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void geOperator() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 >= 5", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void eqEq() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 == 5", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void neq() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("5 != 4", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void bitAnd() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("12 & 6", new HashMap<>()),
            CoreMatchers.equalTo("4")
        );
    }

    @Test
    void bitOr() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("12 | 6", new HashMap<>()),
            CoreMatchers.equalTo("14")
        );
    }

    @Test
    void bitXor() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("12 ^ 6", new HashMap<>()),
            CoreMatchers.equalTo("10")
        );
    }

    @Test
    void logicalAnd() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("1 && 2", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void logicalOr() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0 || 5", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void unaryNot() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("!0", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void unaryBitNot() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("~0", new HashMap<>()),
            CoreMatchers.equalTo("-1")
        );
    }

    @Test
    void ternaryTrue() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("1 ? 10 : 20", new HashMap<>()),
            CoreMatchers.equalTo("10")
        );
    }

    @Test
    void ternaryFalse() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0 ? 10 : 20", new HashMap<>()),
            CoreMatchers.equalTo("20")
        );
    }

    @Test
    void assign() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 5", new HashMap<>()),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void assignStoresValue() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("x = 5", env);
        MatcherAssert.assertThat(env.get("x"), CoreMatchers.equalTo("5"));
    }

    @Test
    void plusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("y", "10");
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("y += 3", env),
            CoreMatchers.equalTo("13")
        );
    }

    @Test
    void plusAssignStoresValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("y", "10");
        new ArithmeticEvaluator().evalArithmetic("y += 3", env);
        MatcherAssert.assertThat(env.get("y"), CoreMatchers.equalTo("13"));
    }

    @Test
    void shortCircuitAndSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 && (z = 99)", env);
        MatcherAssert.assertThat(env.get("z"), CoreMatchers.nullValue());
    }

    @Test
    void shortCircuitOrSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("1 || (q = 99)", env);
        MatcherAssert.assertThat(env.get("q"), CoreMatchers.nullValue());
    }

    @Test
    void varNameResolvesToEnvValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "7");
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("v + 2", env),
            CoreMatchers.equalTo("9")
        );
    }

    @Test
    void nonNumericVarResolvesToZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "abc");
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("v + 2", env),
            CoreMatchers.equalTo("2")
        );
    }

    @Test
    void divByZeroIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("5 / 0", new HashMap<>())
        );
    }

    @Test
    void modByZeroIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("5 % 0", new HashMap<>())
        );
    }

    @Test
    void emptyExprIsZero() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("", new HashMap<>()),
            CoreMatchers.equalTo("0")
        );
    }

    @Test
    void whitespaceOnlyIsZero() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("   ", new HashMap<>()),
            CoreMatchers.equalTo("0")
        );
    }

    @Test
    void unmatchedParenIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("(1+2", new HashMap<>())
        );
    }

    @Test
    void leadingPlus() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("+5", new HashMap<>()),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void doubleNegation() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("--5", new HashMap<>()),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void doubleBitwiseNot() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("~~5", new HashMap<>()),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void doubleLogicalNot() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("!!5", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void compoundAssignToUnsetVar() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("new += 5", new HashMap<>()),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void compoundAssignStoresValue() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("new += 5", env);
        MatcherAssert.assertThat(env.get("new"), CoreMatchers.equalTo("5"));
    }

    @Test
    void chainedAssignmentRightAssoc() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("a = b = c = 7", new HashMap<>()),
            CoreMatchers.equalTo("7")
        );
    }

    @Test
    void chainedAssignmentStoresB() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("a = b = c = 7", env);
        MatcherAssert.assertThat(env.get("b"), CoreMatchers.equalTo("7"));
    }

    @Test
    void chainedAssignmentStoresC() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("a = b = c = 7", env);
        MatcherAssert.assertThat(env.get("c"), CoreMatchers.equalTo("7"));
    }

    @Test
    void ternaryFalseEvaluatesElseOnly() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("0 ? (z = 1) : (z = 2)", env);
        MatcherAssert.assertThat(env.get("z"), CoreMatchers.equalTo("2"));
    }

    @Test
    void numericLiteralWithLeadingZeros() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("042", new HashMap<>()),
            CoreMatchers.equalTo("34")
        );
    }

    @Test
    void assignmentWithWhitespace() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 5", new HashMap<>()),
            CoreMatchers.equalTo("5")
        );
    }

    @Test
    void assignWithWhitespaceStoresValue() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("x = 5", env);
        MatcherAssert.assertThat(env.get("x"), CoreMatchers.equalTo("5"));
    }

    @Test
    void compoundAssignWithWhitespace() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("x = 5", env);
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x += 3", env),
            CoreMatchers.equalTo("8")
        );
    }

    @Test
    void compoundAssignStoresSum() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("x = 5", env);
        new ArithmeticEvaluator().evalArithmetic("x += 3", env);
        MatcherAssert.assertThat(env.get("x"), CoreMatchers.equalTo("8"));
    }

    @Test
    void shiftAssignments() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 2", new HashMap<>()),
            CoreMatchers.equalTo("2")
        );
    }

    @Test
    void shiftAssignStoresValue() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("x = 2", env);
        new ArithmeticEvaluator().evalArithmetic("x <<= 3", env);
        MatcherAssert.assertThat(env.get("x"), CoreMatchers.equalTo("16"));
    }

    @Test
    void shiftAssignRightStoresValue() {
        final Map<String, String> env = new HashMap<>();
        new ArithmeticEvaluator().evalArithmetic("x = 2", env);
        new ArithmeticEvaluator().evalArithmetic("x <<= 3", env);
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x >>= 1", env),
            CoreMatchers.equalTo("8")
        );
    }

    @Test
    void relationalSingleCharacterOperators() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<3", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void ternaryWithoutColonIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("1?5", new HashMap<>())
        );
    }

    @Test
    void unaryOperators() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("+3", new HashMap<>()),
            CoreMatchers.equalTo("3")
        );
    }

    @Test
    void unbalancedParenIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("(2+3", new HashMap<>())
        );
    }

    @Test
    void numberBasesAndInvalidDigits() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0Xf", new HashMap<>()),
            CoreMatchers.equalTo("15")
        );
    }

    @Test
    void invalidEnvValueBecomesZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "not-a-number");
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x", env),
            CoreMatchers.equalTo("0")
        );
    }

    @Test
    void compoundAssignmentAssignsValue() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x = 1", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void compoundAssignmentVersusShift() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "1");
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("x <<= 1", env),
            CoreMatchers.equalTo("2")
        );
    }

    @Test
    void readNumberBoundaryFormsZeroHex() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0x0", new HashMap<>()),
            CoreMatchers.equalTo("0")
        );
    }

    @Test
    void readNumberBoundaryFormsUpperHex() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0XABC", new HashMap<>()),
            CoreMatchers.equalTo("2748")
        );
    }

    @Test
    void readNumberBoundaryFormsOctalEight() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("08", new HashMap<>()),
            CoreMatchers.equalTo("8")
        );
    }

    @Test
    void readNumberBoundaryFormsZero() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0", new HashMap<>()),
            CoreMatchers.equalTo("0")
        );
    }

    @Test
    void numberWithLetterSuffixIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("123abc", new HashMap<>())
        );
    }

    @Test
    void trailingPlusOperatorIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("1+", new HashMap<>())
        );
    }

    @Test
    void trailingMultiplyOperatorIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("1*", new HashMap<>())
        );
    }

    @Test
    void trailingLessThanOperatorIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("1<", new HashMap<>())
        );
    }

    @Test
    void spacedTernaryTrueBranch() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("1 ? 2 : 3", new HashMap<>()),
            CoreMatchers.equalTo("2")
        );
    }

    @Test
    void spacedTernaryFalseBranch() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("0 ? 2 : 3", new HashMap<>()),
            CoreMatchers.equalTo("3")
        );
    }

    @Test
    void singleCharacterRelationalBranches() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<<1", new HashMap<>()),
            CoreMatchers.equalTo("4")
        );
    }

    @Test
    void relationalLessOrEqualTrue() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<=2", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void relationalLessThanFalse() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2<1", new HashMap<>()),
            CoreMatchers.equalTo("0")
        );
    }

    @Test
    void relationalGreaterThanTrue() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2>1", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void relationalGreaterOrEqualTrue() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("2>=2", new HashMap<>()),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void trailingWhitespaceInsideExpressionIsIgnored() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("3 + 4   ", new HashMap<>()),
            CoreMatchers.equalTo("7")
        );
    }

    @Test
    void trailingOperatorBeforeWhitespaceIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("1 +  ", new HashMap<>())
        );
    }

    @Test
    void unknownNameWithoutEnvironmentEvaluatesToZero() {
        MatcherAssert.assertThat(
            new ArithmeticEvaluator().evalArithmetic("nope+1", null),
            CoreMatchers.equalTo("1")
        );
    }

    @Test
    void nonDecimalDigitIsError() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> new ArithmeticEvaluator().evalArithmetic("0z12", new HashMap<>())
        );
    }
}
