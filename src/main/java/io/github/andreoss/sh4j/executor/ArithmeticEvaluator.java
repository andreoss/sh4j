/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Recursive-descent evaluator for the POSIX 2.6.4 arithmetic expansion
 * grammar. Operates entirely on integers; the public entry is
 * {@link #evalArithmetic(String, Map)} which returns the result as a
 * decimal string.
 *
 * <p>The grammar is implemented as a precedence-climbing chain:
 * assignment, ternary, logical-or, logical-and, bit-or, bit-xor, bit-and,
 * equality, relational, shift, additive, term, and factor.</p>
 *
 * @since 1.0
 */
public final class ArithmeticEvaluator {

    /** Variables used by identifier lookup and assignment. */
    private ArithmeticScope scope;

    /** Current nesting of variable value evaluation. */
    private int depth;

    /** When true, assignments and variable mutations are suppressed. */
    private boolean mute;

    /**
     * Default constructor.
     */
    public ArithmeticEvaluator() {
        this.mute = false;
    }

    /**
     * Evaluates an arithmetic expression in the context of {@code env}.
     *
     * @param expr The expression body (already stripped of {@code $((...))})
     * @param env Environment view for identifier lookup and mutation
     * @return The integer result formatted as a decimal string
     */
    public String evalArithmetic(final String expr, final Map<String, String> env) {
        return this.evaluate(expr, new ArithmeticScope(env));
    }

    /**
     * Evaluates an arithmetic expression against a variable scope.
     *
     * @param expr The expression body (already stripped of {@code $((...))})
     * @param vars Variables for identifier lookup and assignment
     * @return The integer result formatted as a decimal string
     */
    String evaluate(final String expr, final ArithmeticScope vars) {
        final ArithmeticScope prev = this.scope;
        this.scope = vars;
        final String result;
        try {
            result = String.valueOf(
                this.whole(new ArithmeticCursor(expr))
            );
        } finally {
            this.scope = prev;
        }
        return result;
    }

    private int whole(final ArithmeticCursor cursor) {
        cursor.skipWhitespace();
        int result = 0;
        if (!cursor.eof()) {
            result = this.parseArithmetic(cursor);
            cursor.skipWhitespace();
            if (!cursor.eof()) {
                throw ArithmeticEvaluator.unexpected(cursor, cursor.pos());
            }
        }
        return result;
    }

    private int parseArithmetic(final ArithmeticCursor cursor) {
        return this.parseAssignment(cursor);
    }

    private int parseAssignment(final ArithmeticCursor cursor) {
        final int save = cursor.pos();
        cursor.skipWhitespace();
        final int result;
        final ArithmeticEvaluator.AssignmentMatch match =
            ArithmeticEvaluator.tryAssignment(cursor);
        if (match == null) {
            cursor.pos(save);
            result = this.parseTernary(cursor);
        } else {
            cursor.pos(match.after());
            final int cur = this.readEnvInt(match.name());
            final int rhs = this.parseAssignment(cursor);
            if (ArithmeticEvaluator.isDivisionOp(match.oper())) {
                this.divisor(rhs);
            }
            final int value = ArithmeticEvaluator.applyAssignmentOp(
                match.oper(), cur, rhs
            );
            if (!this.mute) {
                this.scope.assign(match.name(), value);
            }
            result = value;
        }
        return result;
    }

    private static ArithmeticEvaluator.AssignmentMatch tryAssignment(
        final ArithmeticCursor cursor
    ) {
        ArithmeticEvaluator.AssignmentMatch match = null;
        if (ArithmeticEvaluator.isIdentifierStart(cursor)) {
            final int end = ArithmeticEvaluator.identifierEnd(cursor);
            final String name = cursor.text().substring(cursor.pos(), end);
            cursor.pos(end);
            cursor.skipWhitespace();
            match = ArithmeticEvaluator.detectAssignmentOp(cursor, name);
        }
        return match;
    }

    private static boolean isIdentifierStart(final ArithmeticCursor cursor) {
        boolean result = false;
        if (!cursor.eof()) {
            final char chr = cursor.charAt(0);
            result = Character.isLetter(chr) || chr == '_';
        }
        return result;
    }

    private static int identifierEnd(final ArithmeticCursor cursor) {
        int end = cursor.pos();
        while (end < cursor.text().length()) {
            final char chr = cursor.text().charAt(end);
            if (!Character.isLetterOrDigit(chr) && chr != '_') {
                break;
            }
            end = end + 1;
        }
        return end;
    }

    private static ArithmeticEvaluator.AssignmentMatch detectAssignmentOp(
        final ArithmeticCursor cursor, final String name
    ) {
        ArithmeticEvaluator.AssignmentMatch match =
            ArithmeticEvaluator.detectDoubleOp(cursor, name);
        if (match == null) {
            match = ArithmeticEvaluator.detectSingleOp(cursor, name);
        }
        return match;
    }

    private static ArithmeticEvaluator.AssignmentMatch detectDoubleOp(
        final ArithmeticCursor cursor, final String name
    ) {
        ArithmeticEvaluator.AssignmentMatch match = null;
        if (cursor.hasPrefix("<<=")) {
            match = new ArithmeticEvaluator.AssignmentMatch(
                name, "<<", cursor.pos() + 3
            );
        } else if (cursor.hasPrefix(">>=")) {
            match = new ArithmeticEvaluator.AssignmentMatch(
                name, ">>", cursor.pos() + 3
            );
        }
        return match;
    }

    private static ArithmeticEvaluator.AssignmentMatch detectSingleOp(
        final ArithmeticCursor cursor, final String name
    ) {
        ArithmeticEvaluator.AssignmentMatch match = null;
        final char first = cursor.charAt(0);
        final char second = cursor.charAt(1);
        if (ArithmeticEvaluator.isCompoundAssignFirst(first) && second == '=') {
            match = new ArithmeticEvaluator.AssignmentMatch(
                name, String.valueOf(first), cursor.pos() + 2
            );
        } else if (first == '=' && second != '=') {
            match = new ArithmeticEvaluator.AssignmentMatch(
                name, "", cursor.pos() + 1
            );
        }
        return match;
    }

    private static boolean isCompoundAssignFirst(final char chr) {
        return "*/%+-&^|".indexOf(chr) >= 0;
    }

    private static int applyAssignmentOp(
        final String oper, final int cur, final int rhs
    ) {
        final int result;
        if ("".equals(oper)) {
            result = rhs;
        } else if (ArithmeticEvaluator.isAdditiveOp(oper)) {
            result = ArithmeticEvaluator.applyAdditive(oper, cur, rhs);
        } else if (ArithmeticEvaluator.isMultiplicativeOp(oper)) {
            result = ArithmeticEvaluator.applyMultiplicative(oper, cur, rhs);
        } else {
            result = ArithmeticEvaluator.applyBitwise(oper, cur, rhs);
        }
        return result;
    }

    private static boolean isDivisionOp(final String oper) {
        return "/".equals(oper) || "%".equals(oper);
    }

    private static boolean isAdditiveOp(final String oper) {
        return "+".equals(oper) || "-".equals(oper);
    }

    private static boolean isMultiplicativeOp(final String oper) {
        return "*".equals(oper) || "/".equals(oper) || "%".equals(oper);
    }

    private static int applyAdditive(
        final String oper, final int cur, final int rhs
    ) {
        final int result;
        if ("+".equals(oper)) {
            result = cur + rhs;
        } else {
            result = cur - rhs;
        }
        return result;
    }

    private static int applyMultiplicative(
        final String oper, final int cur, final int rhs
    ) {
        final int result;
        if ("*".equals(oper)) {
            result = cur * rhs;
        } else if ("/".equals(oper)) {
            result = ArithmeticEvaluator.safeDivide(cur, rhs);
        } else {
            result = ArithmeticEvaluator.safeModulo(cur, rhs);
        }
        return result;
    }

    private static int applyBitwise(
        final String oper, final int cur, final int rhs
    ) {
        final int result;
        if ("<<".equals(oper)) {
            result = cur << rhs;
        } else if (">>".equals(oper)) {
            result = cur >> rhs;
        } else if ("&".equals(oper)) {
            result = cur & rhs;
        } else if ("^".equals(oper)) {
            result = cur ^ rhs;
        } else {
            result = cur | rhs;
        }
        return result;
    }

    private int divisor(final int rhs) {
        if (rhs == 0 && !this.mute) {
            throw new ArithmeticException("division by zero");
        }
        return rhs;
    }

    private static int safeDivide(final int cur, final int rhs) {
        final int result;
        if (rhs == 0) {
            result = 0;
        } else {
            result = cur / rhs;
        }
        return result;
    }

    private static int safeModulo(final int cur, final int rhs) {
        final int result;
        if (rhs == 0) {
            result = 0;
        } else {
            result = cur % rhs;
        }
        return result;
    }

    private int readEnvInt(final String name) {
        final String value = this.scope.value(name).trim();
        int result;
        try {
            result = Integer.parseInt(value);
        } catch (final NumberFormatException ignored) {
            result = this.nested(value);
        }
        return result;
    }

    private int nested(final String value) {
        if (this.depth >= 64) {
            throw new ArithmeticException("expression recursion level exceeded");
        }
        this.depth = this.depth + 1;
        final int result;
        try {
            result = this.whole(new ArithmeticCursor(value));
        } finally {
            this.depth = this.depth - 1;
        }
        return result;
    }

    private int parseTernary(final ArithmeticCursor cursor) {
        final int cond = this.parseLogicalOr(cursor);
        cursor.skipWhitespace();
        int result = cond;
        if (cursor.currentIs('?')) {
            cursor.advance();
            final boolean muted = this.mute;
            if (cond == 0) {
                this.mute = true;
            }
            final int truthy = this.parseAssignment(cursor);
            this.mute = muted;
            cursor.skipWhitespace();
            ArithmeticEvaluator.expect(cursor, ':');
            if (cond != 0) {
                this.mute = true;
            }
            final int falsy = this.parseAssignment(cursor);
            this.mute = muted;
            if (cond == 0) {
                result = falsy;
            } else {
                result = truthy;
            }
        }
        return result;
    }

    private int parseLogicalOr(final ArithmeticCursor cursor) {
        int value = this.parseLogicalAnd(cursor);
        while (ArithmeticEvaluator.matchOp(cursor, "||")) {
            final boolean muted = this.mute;
            if (value != 0) {
                this.mute = true;
            }
            final int rhs = this.parseLogicalAnd(cursor);
            this.mute = muted;
            if (value == 0 && rhs == 0) {
                value = 0;
            } else {
                value = 1;
            }
        }
        return value;
    }

    private int parseLogicalAnd(final ArithmeticCursor cursor) {
        int value = this.parseBitOr(cursor);
        while (ArithmeticEvaluator.matchOp(cursor, "&&")) {
            final boolean muted = this.mute;
            if (value == 0) {
                this.mute = true;
            }
            final int rhs = this.parseBitOr(cursor);
            this.mute = muted;
            if (value == 0 || rhs == 0) {
                value = 0;
            } else {
                value = 1;
            }
        }
        return value;
    }

    private int parseBitOr(final ArithmeticCursor cursor) {
        int value = this.parseBitXor(cursor);
        while (ArithmeticEvaluator.peekIsSingle(cursor, '|', "||")) {
            cursor.advance();
            value = value | this.parseBitXor(cursor);
        }
        return value;
    }

    private int parseBitXor(final ArithmeticCursor cursor) {
        int value = this.parseBitAnd(cursor);
        while (ArithmeticEvaluator.peekIs(cursor, '^')) {
            cursor.advance();
            value = value ^ this.parseBitAnd(cursor);
        }
        return value;
    }

    private int parseBitAnd(final ArithmeticCursor cursor) {
        int value = this.parseEquality(cursor);
        while (ArithmeticEvaluator.peekIsSingle(cursor, '&', "&&")) {
            cursor.advance();
            value = value & this.parseEquality(cursor);
        }
        return value;
    }

    private int parseEquality(final ArithmeticCursor cursor) {
        int value = this.parseRelational(cursor);
        boolean done = false;
        while (!done) {
            if (ArithmeticEvaluator.matchOp(cursor, "==")) {
                if (value == this.parseRelational(cursor)) {
                    value = 1;
                } else {
                    value = 0;
                }
            } else if (ArithmeticEvaluator.matchOp(cursor, "!=")) {
                if (value == this.parseRelational(cursor)) {
                    value = 0;
                } else {
                    value = 1;
                }
            } else {
                done = true;
            }
        }
        return value;
    }

    private int parseRelational(final ArithmeticCursor cursor) {
        int value = this.parseShift(cursor);
        boolean done = false;
        while (!done) {
            if (ArithmeticEvaluator.matchOp(cursor, "<=")) {
                value = ArithmeticEvaluator.boolValue(
                    value <= this.parseShift(cursor)
                );
            } else if (ArithmeticEvaluator.matchOp(cursor, ">=")) {
                value = ArithmeticEvaluator.boolValue(
                    value >= this.parseShift(cursor)
                );
            } else if (ArithmeticEvaluator.peekIsSingleNotFollowedBy(
                cursor, '<', "<="
            )) {
                cursor.advance();
                value = ArithmeticEvaluator.boolValue(
                    value < this.parseShift(cursor)
                );
            } else if (ArithmeticEvaluator.peekIsSingleNotFollowedBy(
                cursor, '>', ">="
            )) {
                cursor.advance();
                value = ArithmeticEvaluator.boolValue(
                    value > this.parseShift(cursor)
                );
            } else {
                done = true;
            }
        }
        return value;
    }

    private static int boolValue(final boolean flag) {
        final int result;
        if (flag) {
            result = 1;
        } else {
            result = 0;
        }
        return result;
    }

    private int parseShift(final ArithmeticCursor cursor) {
        int value = this.parseAdditive(cursor);
        boolean done = false;
        while (!done) {
            if (ArithmeticEvaluator.matchOp(cursor, "<<")) {
                value = value << this.parseAdditive(cursor);
            } else if (ArithmeticEvaluator.matchOp(cursor, ">>")) {
                value = value >> this.parseAdditive(cursor);
            } else {
                done = true;
            }
        }
        return value;
    }

    private int parseAdditive(final ArithmeticCursor cursor) {
        int value = this.parseTerm(cursor);
        boolean done = false;
        while (!done && !cursor.eof()) {
            cursor.skipWhitespace();
            if (cursor.eof()) {
                done = true;
            } else {
                final char chr = cursor.charAt(0);
                if (chr == '+') {
                    cursor.advance();
                    value = value + this.parseTerm(cursor);
                } else if (chr == '-') {
                    cursor.advance();
                    value = value - this.parseTerm(cursor);
                } else {
                    done = true;
                }
            }
        }
        return value;
    }

    private int parseTerm(final ArithmeticCursor cursor) {
        int value = this.parseFactor(cursor);
        boolean done = false;
        while (!done && !cursor.eof()) {
            cursor.skipWhitespace();
            if (cursor.eof()) {
                done = true;
            } else {
                final char chr = cursor.charAt(0);
                if (chr == '*') {
                    cursor.advance();
                    value = value * this.parseFactor(cursor);
                } else if (chr == '/') {
                    cursor.advance();
                    value = ArithmeticEvaluator.safeDivide(
                        value, this.divisor(this.parseFactor(cursor))
                    );
                } else if (chr == '%') {
                    cursor.advance();
                    value = ArithmeticEvaluator.safeModulo(
                        value, this.divisor(this.parseFactor(cursor))
                    );
                } else {
                    done = true;
                }
            }
        }
        return value;
    }

    private int parseFactor(final ArithmeticCursor cursor) {
        cursor.skipWhitespace();
        if (cursor.eof()) {
            throw new ArithmeticException("syntax error: operand expected");
        }
        return this.parseFactorToken(cursor);
    }

    private int parseFactorToken(final ArithmeticCursor cursor) {
        final char chr = cursor.charAt(0);
        final int result;
        if (chr == '(') {
            result = this.parseGroup(cursor);
        } else if (chr == '+') {
            cursor.advance();
            result = this.parseFactor(cursor);
        } else if (chr == '-') {
            cursor.advance();
            result = -this.parseFactor(cursor);
        } else if (chr == '!') {
            result = this.parseNot(cursor);
        } else if (chr == '~') {
            cursor.advance();
            result = ~this.parseFactor(cursor);
        } else if (Character.isLetter(chr) || chr == '_') {
            result = this.readIdentifier(cursor);
        } else {
            result = ArithmeticEvaluator.readNumber(cursor);
        }
        return result;
    }

    private int parseGroup(final ArithmeticCursor cursor) {
        cursor.advance();
        final int result = this.parseArithmetic(cursor);
        cursor.skipWhitespace();
        ArithmeticEvaluator.expect(cursor, ')');
        return result;
    }

    private int parseNot(final ArithmeticCursor cursor) {
        cursor.advance();
        final int result;
        if (this.parseFactor(cursor) == 0) {
            result = 1;
        } else {
            result = 0;
        }
        return result;
    }

    private int readIdentifier(final ArithmeticCursor cursor) {
        final int end = ArithmeticEvaluator.identifierEnd(cursor);
        final String name = cursor.text().substring(cursor.pos(), end);
        cursor.pos(end);
        return this.readEnvInt(name);
    }

    private static int readNumber(final ArithmeticCursor cursor) {
        final int start = cursor.pos();
        final int base = ArithmeticEvaluator.numberBase(cursor);
        int num = 0;
        while (!cursor.eof()) {
            final int digit = ArithmeticEvaluator.digitAt(cursor, base);
            if (digit < 0 || digit >= base) {
                break;
            }
            num = num * base + digit;
            cursor.advance();
        }
        if (cursor.pos() == start) {
            throw ArithmeticEvaluator.unexpected(cursor, start);
        }
        return num;
    }

    private static int numberBase(final ArithmeticCursor cursor) {
        int base = 10;
        if (cursor.hasPrefix("0x") || cursor.hasPrefix("0X")) {
            base = 16;
            cursor.advance(2);
        } else if (cursor.hasPrefix("0") && ArithmeticEvaluator.isOctalDigit(cursor, 1)) {
            base = 8;
            cursor.advance();
        }
        return base;
    }

    private static boolean isOctalDigit(final ArithmeticCursor cursor, final int offset) {
        final char chr = cursor.charAt(offset);
        return chr >= '0' && chr <= '7';
    }

    private static int digitAt(
        final ArithmeticCursor cursor, final int base
    ) {
        final char chr = cursor.charAt(0);
        final int digit;
        if (chr >= '0' && chr <= '9') {
            digit = chr - '0';
        } else if (base == 16 && chr >= 'a' && chr <= 'f') {
            digit = chr - 'a' + 10;
        } else if (base == 16 && chr >= 'A' && chr <= 'F') {
            digit = chr - 'A' + 10;
        } else {
            digit = -1;
        }
        return digit;
    }

    private static void expect(final ArithmeticCursor cursor, final char chr) {
        if (!cursor.currentIs(chr)) {
            throw new ArithmeticException(
                String.format("syntax error: \"%s\" expected", chr)
            );
        }
        cursor.advance();
    }

    private static ArithmeticException unexpected(
        final ArithmeticCursor cursor, final int start
    ) {
        return new ArithmeticException(
            String.format("syntax error: unexpected \"%s\"", cursor.text().substring(start))
        );
    }

    private static boolean matchOp(
        final ArithmeticCursor cursor, final String oper
    ) {
        cursor.skipWhitespace();
        final boolean match = cursor.hasPrefix(oper);
        if (match) {
            cursor.advance(oper.length());
        }
        return match;
    }

    private static boolean peekIs(
        final ArithmeticCursor cursor, final char chr
    ) {
        cursor.skipWhitespace();
        return cursor.currentIs(chr);
    }

    private static boolean peekIsSingle(
        final ArithmeticCursor cursor, final char chr,
        final String avoid
    ) {
        cursor.skipWhitespace();
        boolean result = cursor.currentIs(chr);
        if (result && avoid.indexOf(cursor.charAt(1)) >= 0) {
            result = false;
        }
        return result;
    }

    private static boolean peekIsSingleNotFollowedBy(
        final ArithmeticCursor cursor, final char chr,
        final String avoid
    ) {
        return ArithmeticEvaluator.peekIsSingle(cursor, chr, avoid);
    }

    private record AssignmentMatch(String name, String oper, int after) {
    }
}
