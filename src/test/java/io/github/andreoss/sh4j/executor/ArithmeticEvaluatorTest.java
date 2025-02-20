/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ArithmeticEvaluatorTest {
    private final ArithmeticEvaluator e = new ArithmeticEvaluator();
    private final Map<String, String> env = new HashMap<>();

    @Test void add() { assertEquals("7", e.evalArithmetic("3 + 4", env)); }
    @Test void sub() { assertEquals("3", e.evalArithmetic("7 - 4", env)); }
    @Test void mul() { assertEquals("12", e.evalArithmetic("3 * 4", env)); }
    @Test void div() { assertEquals("2", e.evalArithmetic("9 / 4", env)); }
    @Test void mod() { assertEquals("1", e.evalArithmetic("9 % 4", env)); }
    @Test void precedence() { assertEquals("14", e.evalArithmetic("2 + 3 * 4", env)); }
    @Test void parens() { assertEquals("20", e.evalArithmetic("(2 + 3) * 4", env)); }

    @Test void shiftLeft() { assertEquals("32", e.evalArithmetic("4 << 3", env)); }
    @Test void shiftRight() { assertEquals("4", e.evalArithmetic("32 >> 3", env)); }
    @Test void lt() { assertEquals("1", e.evalArithmetic("3 < 5", env)); }
    @Test void ge() { assertEquals("1", e.evalArithmetic("5 >= 5", env)); }
    @Test void eqEq() { assertEquals("1", e.evalArithmetic("5 == 5", env)); }
    @Test void neq() { assertEquals("1", e.evalArithmetic("5 != 4", env)); }
    @Test void bitAnd() { assertEquals("4", e.evalArithmetic("12 & 6", env)); }
    @Test void bitOr() { assertEquals("14", e.evalArithmetic("12 | 6", env)); }
    @Test void bitXor() { assertEquals("10", e.evalArithmetic("12 ^ 6", env)); }
    @Test void logicalAnd() { assertEquals("1", e.evalArithmetic("1 && 2", env)); }
    @Test void logicalOr() { assertEquals("1", e.evalArithmetic("0 || 5", env)); }
    @Test void unaryNot() { assertEquals("1", e.evalArithmetic("!0", env)); }
    @Test void unaryBitNot() { assertEquals("-1", e.evalArithmetic("~0", env)); }

    @Test void ternaryTrue() { assertEquals("10", e.evalArithmetic("1 ? 10 : 20", env)); }
    @Test void ternaryFalse() { assertEquals("20", e.evalArithmetic("0 ? 10 : 20", env)); }

    @Test void assign() {
        assertEquals("5", e.evalArithmetic("x = 5", env));
        assertEquals("5", env.get("x"));
    }

    @Test void plusAssign() {
        env.put("y", "10");
        assertEquals("13", e.evalArithmetic("y += 3", env));
        assertEquals("13", env.get("y"));
    }

    @Test void shortCircuitAndSuppressesAssignment() {
        e.evalArithmetic("0 && (z = 99)", env);
        assertNull(env.get("z"));
    }

    @Test void shortCircuitOrSuppressesAssignment() {
        e.evalArithmetic("1 || (q = 99)", env);
        assertNull(env.get("q"));
    }

    @Test void varNameResolvesToEnvValue() {
        env.put("v", "7");
        assertEquals("9", e.evalArithmetic("v + 2", env));
    }

    @Test void nonNumericVarResolvesToZero() {
        env.put("v", "abc");
        assertEquals("2", e.evalArithmetic("v + 2", env));
    }

    @Test void divByZeroIsZero() {
        assertEquals("0", e.evalArithmetic("5 / 0", env));
    }
}
