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
 * assignment → ternary → logical-or → logical-and → bit-or → bit-xor →
 * bit-and → equality → relational → shift → additive → term → factor.</p>
 * @since 1.0
 */
public final class ArithmeticEvaluator {

    /** Environment view used by identifier lookup; nullable. */
    private Map<String, String> arenv;
    /** When true, assignments + variable mutations are suppressed (used
     *  by short-circuit and ternary unselected branches). */
    private boolean mute;

    /**
     * Default constructor.
     */
    public ArithmeticEvaluator() {
        super();
    }

    /**
     * Evaluates an arithmetic expression in the context of {@code env}.
     * @param expr The expression body (already stripped of {@code $((...))})
     * @param env  Environment view for identifier lookup / mutation
     * @return The integer result formatted as a decimal string
     */
    public String evalArithmetic(final String expr, final Map<String, String> env) {
        final Map<String, String> prev = this.arenv;
        this.arenv = env;
        String result;
        try {
            result = String.valueOf(this.parseArithmetic(expr.trim(), new int[]{0}));
        } finally {
            this.arenv = prev;
        }
        return result;
    }

    private int parseArithmetic(final String str, final int[] pos) {
        return this.parseAssignment(str, pos);
    }

    private int parseAssignment(final String str, final int[] pos) {
        final int save = pos[0];
        ArithmeticEvaluator.skipWhitespace(str, pos);
        int result;
        final AssignmentMatch match = this.tryAssignment(str, pos);
        if (match == null) {
            pos[0] = save;
            result = this.parseTernary(str, pos);
        } else {
            pos[0] = match.afterOp;
            final int rhs = this.parseAssignment(str, pos);
            final int cur = this.readEnvInt(match.name);
            final int newVal = applyAssignmentOp(match.op, cur, rhs);
            if (!this.mute && this.arenv != null) {
                this.arenv.put(match.name, String.valueOf(newVal));
            }
            result = newVal;
        }
        return result;
    }

    /**
     * Tries to match an assignment-form prefix at the current position.
     * @return The match, or null when the current position is not an
     *         assignment target
     */
    private AssignmentMatch tryAssignment(final String str, final int[] pos) {
        AssignmentMatch match = null;
        if (pos[0] < str.length()
            && (Character.isLetter(str.charAt(pos[0])) || str.charAt(pos[0]) == '_')) {
            int end = pos[0];
            while (end < str.length()
                && (Character.isLetterOrDigit(str.charAt(end)) || str.charAt(end) == '_')) {
                end = end + 1;
            }
            final String name = str.substring(pos[0], end);
            int after = end;
            while (after < str.length() && Character.isWhitespace(str.charAt(after))) {
                after = after + 1;
            }
            match = detectAssignmentOp(str, after, name);
        }
        return match;
    }

    /**
     * Detects which compound assignment operator (or plain {@code =}) is
     * at position {@code start} of {@code str}.
     */
    private static AssignmentMatch detectAssignmentOp(
        final String str, final int start, final String name
    ) {
        AssignmentMatch match = null;
        if (start < str.length()) {
            final char ch0 = str.charAt(start);
            final char ch1 = start + 1 < str.length() ? str.charAt(start + 1) : 0;
            final char ch2 = start + 2 < str.length() ? str.charAt(start + 2) : 0;
            if ((ch0 == '<' || ch0 == '>') && ch1 == ch0 && ch2 == '=') {
                match = new AssignmentMatch(name, "" + ch0 + ch1, start + 3);
            } else if (ch1 == '=' && isCompoundAssignFirst(ch0)) {
                match = new AssignmentMatch(name, "" + ch0, start + 2);
            } else if (ch0 == '=' && ch1 != '=') {
                match = new AssignmentMatch(name, "", start + 1);
            }
        }
        return match;
    }

    private static boolean isCompoundAssignFirst(final char chr) {
        return chr == '*' || chr == '/' || chr == '%' || chr == '+'
            || chr == '-' || chr == '&' || chr == '^' || chr == '|';
    }

    private static int applyAssignmentOp(final String op, final int cur, final int rhs) {
        if ("".equals(op)) {
            return rhs;
        } else if ("+".equals(op)) {
            return cur + rhs;
        } else if ("-".equals(op)) {
            return cur - rhs;
        } else if ("*".equals(op)) {
            return cur * rhs;
        } else if ("/".equals(op)) {
            return rhs == 0 ? 0 : cur / rhs;
        } else if ("%".equals(op)) {
            return rhs == 0 ? 0 : cur % rhs;
        } else if ("<<".equals(op)) {
            return cur << rhs;
        } else if (">>".equals(op)) {
            return cur >> rhs;
        } else if ("&".equals(op)) {
            return cur & rhs;
        } else if ("^".equals(op)) {
            return cur ^ rhs;
        } else if ("|".equals(op)) {
            return cur | rhs;
        } else {
            return rhs;
        }
    }

    private int readEnvInt(final String name) {
        int cur = 0;
        if (this.arenv != null) {
            try {
                cur = Integer.parseInt(this.arenv.getOrDefault(name, "0").trim());
            } catch (final NumberFormatException ignored) {
                cur = 0;
            }
        }
        return cur;
    }

    private int parseTernary(final String str, final int[] pos) {
        final int cond = this.parseLogicalOr(str, pos);
        ArithmeticEvaluator.skipWhitespace(str, pos);
        int result = cond;
        if (pos[0] < str.length() && str.charAt(pos[0]) == '?') {
            pos[0] = pos[0] + 1;
            final boolean prevMute = this.mute;
            if (cond == 0) {
                this.mute = true;
            }
            final int thenVal = this.parseAssignment(str, pos);
            this.mute = prevMute;
            ArithmeticEvaluator.skipWhitespace(str, pos);
            if (pos[0] < str.length() && str.charAt(pos[0]) == ':') {
                pos[0] = pos[0] + 1;
            }
            if (cond != 0) {
                this.mute = true;
            }
            final int elseVal = this.parseAssignment(str, pos);
            this.mute = prevMute;
            result = cond == 0 ? elseVal : thenVal;
        }
        return result;
    }

    private int parseLogicalOr(final String str, final int[] pos) {
        int value = this.parseLogicalAnd(str, pos);
        while (this.matchOp(str, pos, "||")) {
            final boolean prevMute = this.mute;
            if (value != 0) {
                this.mute = true;
            }
            final int rhs = this.parseLogicalAnd(str, pos);
            this.mute = prevMute;
            value = value != 0 || rhs != 0 ? 1 : 0;
        }
        return value;
    }

    private int parseLogicalAnd(final String str, final int[] pos) {
        int value = this.parseBitOr(str, pos);
        while (this.matchOp(str, pos, "&&")) {
            final boolean prevMute = this.mute;
            if (value == 0) {
                this.mute = true;
            }
            final int rhs = this.parseBitOr(str, pos);
            this.mute = prevMute;
            value = value != 0 && rhs != 0 ? 1 : 0;
        }
        return value;
    }

    private int parseBitOr(final String str, final int[] pos) {
        int value = this.parseBitXor(str, pos);
        while (this.peekIsSingle(str, pos, '|', "||")) {
            pos[0] = pos[0] + 1;
            value = value | this.parseBitXor(str, pos);
        }
        return value;
    }

    private int parseBitXor(final String str, final int[] pos) {
        int value = this.parseBitAnd(str, pos);
        while (this.peekIs(str, pos, '^')) {
            pos[0] = pos[0] + 1;
            value = value ^ this.parseBitAnd(str, pos);
        }
        return value;
    }

    private int parseBitAnd(final String str, final int[] pos) {
        int value = this.parseEquality(str, pos);
        while (this.peekIsSingle(str, pos, '&', "&&")) {
            pos[0] = pos[0] + 1;
            value = value & this.parseEquality(str, pos);
        }
        return value;
    }

    private int parseEquality(final String str, final int[] pos) {
        int value = this.parseRelational(str, pos);
        boolean done = false;
        while (!done) {
            if (this.matchOp(str, pos, "==")) {
                value = value == this.parseRelational(str, pos) ? 1 : 0;
            } else if (this.matchOp(str, pos, "!=")) {
                value = value != this.parseRelational(str, pos) ? 1 : 0;
            } else {
                done = true;
            }
        }
        return value;
    }

    private int parseRelational(final String str, final int[] pos) {
        int value = this.parseShift(str, pos);
        boolean done = false;
        while (!done) {
            if (this.matchOp(str, pos, "<=")) {
                value = value <= this.parseShift(str, pos) ? 1 : 0;
            } else if (this.matchOp(str, pos, ">=")) {
                value = value >= this.parseShift(str, pos) ? 1 : 0;
            } else if (this.peekIsSingleNotFollowedBy(str, pos, '<', '<', '=')) {
                pos[0] = pos[0] + 1;
                value = value < this.parseShift(str, pos) ? 1 : 0;
            } else if (this.peekIsSingleNotFollowedBy(str, pos, '>', '>', '=')) {
                pos[0] = pos[0] + 1;
                value = value > this.parseShift(str, pos) ? 1 : 0;
            } else {
                done = true;
            }
        }
        return value;
    }

    private int parseShift(final String str, final int[] pos) {
        int value = this.parseAdditive(str, pos);
        boolean done = false;
        while (!done) {
            if (this.matchOp(str, pos, "<<")) {
                value = value << this.parseAdditive(str, pos);
            } else if (this.matchOp(str, pos, ">>")) {
                value = value >> this.parseAdditive(str, pos);
            } else {
                done = true;
            }
        }
        return value;
    }

    private int parseAdditive(final String str, final int[] pos) {
        int value = this.parseTerm(str, pos);
        boolean done = false;
        while (!done && pos[0] < str.length()) {
            ArithmeticEvaluator.skipWhitespace(str, pos);
            if (pos[0] >= str.length()) {
                done = true;
            } else {
                final char chr = str.charAt(pos[0]);
                if (chr == '+') {
                    pos[0] = pos[0] + 1;
                    value = value + this.parseTerm(str, pos);
                } else if (chr == '-') {
                    pos[0] = pos[0] + 1;
                    value = value - this.parseTerm(str, pos);
                } else {
                    done = true;
                }
            }
        }
        return value;
    }

    private int parseTerm(final String str, final int[] pos) {
        int value = this.parseFactor(str, pos);
        boolean done = false;
        while (!done && pos[0] < str.length()) {
            ArithmeticEvaluator.skipWhitespace(str, pos);
            if (pos[0] >= str.length()) {
                done = true;
            } else {
                final char chr = str.charAt(pos[0]);
                if (chr == '*') {
                    pos[0] = pos[0] + 1;
                    value = value * this.parseFactor(str, pos);
                } else if (chr == '/') {
                    pos[0] = pos[0] + 1;
                    final int div = this.parseFactor(str, pos);
                    value = div == 0 ? 0 : value / div;
                } else if (chr == '%') {
                    pos[0] = pos[0] + 1;
                    final int div = this.parseFactor(str, pos);
                    value = div == 0 ? 0 : value % div;
                } else {
                    done = true;
                }
            }
        }
        return value;
    }

    private int parseFactor(final String str, final int[] pos) {
        ArithmeticEvaluator.skipWhitespace(str, pos);
        int result;
        if (pos[0] >= str.length()) {
            result = 0;
        } else {
            final char chr = str.charAt(pos[0]);
            if (chr == '(') {
                pos[0] = pos[0] + 1;
                result = this.parseArithmetic(str, pos);
                ArithmeticEvaluator.skipWhitespace(str, pos);
                if (pos[0] < str.length() && str.charAt(pos[0]) == ')') {
                    pos[0] = pos[0] + 1;
                }
            } else if (chr == '+') {
                pos[0] = pos[0] + 1;
                result = this.parseFactor(str, pos);
            } else if (chr == '-') {
                pos[0] = pos[0] + 1;
                result = -this.parseFactor(str, pos);
            } else if (chr == '!') {
                pos[0] = pos[0] + 1;
                result = this.parseFactor(str, pos) == 0 ? 1 : 0;
            } else if (chr == '~') {
                pos[0] = pos[0] + 1;
                result = ~this.parseFactor(str, pos);
            } else if (Character.isLetter(chr) || chr == '_') {
                result = this.readIdentifier(str, pos);
            } else {
                result = readNumber(str, pos);
            }
        }
        return result;
    }

    private int readIdentifier(final String str, final int[] pos) {
        int end = pos[0];
        while (end < str.length()
            && (Character.isLetterOrDigit(str.charAt(end)) || str.charAt(end) == '_')) {
            end = end + 1;
        }
        final String name = str.substring(pos[0], end);
        pos[0] = end;
        final String val = this.arenv == null ? "0" : this.arenv.getOrDefault(name, "0");
        int result;
        try {
            result = Integer.parseInt(val.trim());
        } catch (final NumberFormatException ignored) {
            result = 0;
        }
        return result;
    }

    private static int readNumber(final String str, final int[] pos) {
        int num = 0;
        while (pos[0] < str.length() && Character.isDigit(str.charAt(pos[0]))) {
            num = num * 10 + (str.charAt(pos[0]) - '0');
            pos[0] = pos[0] + 1;
        }
        return num;
    }

    private boolean matchOp(final String str, final int[] pos, final String op) {
        ArithmeticEvaluator.skipWhitespace(str, pos);
        boolean match = pos[0] + op.length() <= str.length();
        for (int idx = 0; match && idx < op.length(); idx = idx + 1) {
            if (str.charAt(pos[0] + idx) != op.charAt(idx)) {
                match = false;
            }
        }
        if (match) {
            pos[0] = pos[0] + op.length();
        }
        return match;
    }

    private boolean peekIs(final String str, final int[] pos, final char chr) {
        ArithmeticEvaluator.skipWhitespace(str, pos);
        return pos[0] < str.length() && str.charAt(pos[0]) == chr;
    }

    private boolean peekIsSingle(
        final String str, final int[] pos, final char chr, final String avoid
    ) {
        ArithmeticEvaluator.skipWhitespace(str, pos);
        boolean result = pos[0] < str.length() && str.charAt(pos[0]) == chr;
        if (result && pos[0] + 1 < str.length()) {
            final char next = str.charAt(pos[0] + 1);
            for (int idx = 1; result && idx < avoid.length(); idx = idx + 1) {
                if (next == avoid.charAt(idx) && avoid.charAt(0) == chr) {
                    result = false;
                }
            }
        }
        return result;
    }

    private boolean peekIsSingleNotFollowedBy(
        final String str, final int[] pos, final char chr, final char first, final char second
    ) {
        ArithmeticEvaluator.skipWhitespace(str, pos);
        boolean result = pos[0] < str.length() && str.charAt(pos[0]) == chr;
        if (result && pos[0] + 1 < str.length()) {
            final char next = str.charAt(pos[0] + 1);
            if (next == first || next == second) {
                result = false;
            }
        }
        return result;
    }

    private static void skipWhitespace(final String str, final int[] pos) {
        while (pos[0] < str.length() && Character.isWhitespace(str.charAt(pos[0]))) {
            pos[0] = pos[0] + 1;
        }
    }

    /**
     * Holds the data for a matched assignment-form prefix.
     */
    private static final class AssignmentMatch {
        /** Target identifier. */
        private final String name;
        /** Operator string ({@code ""} for {@code =}, {@code "+"} for {@code +=}, ...). */
        private final String op;
        /** Position just past the operator. */
        private final int afterOp;

        AssignmentMatch(final String name, final String op, final int afterOp) {
            this.name = name;
            this.op = op;
            this.afterOp = afterOp;
        }
    }
}
