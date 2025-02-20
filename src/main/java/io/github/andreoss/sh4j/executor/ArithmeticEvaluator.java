/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

public final class ArithmeticEvaluator {
    private Map<String, String> arithEnv;
    private boolean arithMute = false;

    public String evalArithmetic(String expr, Map<String, String> env) {
        Map<String, String> prev = this.arithEnv;
        this.arithEnv = env;
        try {
            return String.valueOf(parseArithmetic(expr.trim(), new int[]{0}));
        } finally {
            this.arithEnv = prev;
        }
    }

    private int parseArithmetic(String s, int[] pos) {
        return parseAssignment(s, pos);
    }

    private int parseAssignment(String s, int[] pos) {
        int save = pos[0];
        skipWhitespace(s, pos);
        if (pos[0] < s.length() && (Character.isLetter(s.charAt(pos[0])) || s.charAt(pos[0]) == '_')) {
            int j = pos[0];
            while (j < s.length() && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) j++;
            String name = s.substring(pos[0], j);
            int k = j;
            while (k < s.length() && Character.isWhitespace(s.charAt(k))) k++;
            if (k < s.length()) {
                char c0 = s.charAt(k);
                char c1 = k + 1 < s.length() ? s.charAt(k + 1) : 0;
                char c2 = k + 2 < s.length() ? s.charAt(k + 2) : 0;
                String op = null;
                int opLen = 0;
                if ((c0 == '<' && c1 == '<' && c2 == '=') || (c0 == '>' && c1 == '>' && c2 == '=')) {
                    op = "" + c0 + c1; opLen = 3;
                } else if (c1 == '=' && (c0 == '*' || c0 == '/' || c0 == '%' || c0 == '+'
                        || c0 == '-' || c0 == '&' || c0 == '^' || c0 == '|')) {
                    op = "" + c0; opLen = 2;
                } else if (c0 == '=' && c1 != '=') {
                    op = ""; opLen = 1;
                }
                if (op != null) {
                    pos[0] = k + opLen;
                    int rhs = parseAssignment(s, pos);
                    int cur = 0;
                    if (arithEnv != null) {
                        try { cur = Integer.parseInt(arithEnv.getOrDefault(name, "0").trim()); }
                        catch (NumberFormatException ignored) {}
                    }
                    int newVal = switch (op) {
                        case "" -> rhs;
                        case "+" -> cur + rhs;
                        case "-" -> cur - rhs;
                        case "*" -> cur * rhs;
                        case "/" -> rhs != 0 ? cur / rhs : 0;
                        case "%" -> rhs != 0 ? cur % rhs : 0;
                        case "<<" -> cur << rhs;
                        case ">>" -> cur >> rhs;
                        case "&" -> cur & rhs;
                        case "^" -> cur ^ rhs;
                        case "|" -> cur | rhs;
                        default -> rhs;
                    };
                    if (!arithMute && arithEnv != null) {
                        arithEnv.put(name, String.valueOf(newVal));
                    }
                    return newVal;
                }
            }
        }
        pos[0] = save;
        return parseTernary(s, pos);
    }

    private int parseTernary(String s, int[] pos) {
        int cond = parseLogicalOr(s, pos);
        skipWhitespace(s, pos);
        if (pos[0] < s.length() && s.charAt(pos[0]) == '?') {
            pos[0]++;
            boolean prevMute = arithMute;
            if (cond == 0) arithMute = true;
            int thenVal = parseAssignment(s, pos);
            arithMute = prevMute;
            skipWhitespace(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ':') pos[0]++;
            if (cond != 0) arithMute = true;
            int elseVal = parseAssignment(s, pos);
            arithMute = prevMute;
            return cond != 0 ? thenVal : elseVal;
        }
        return cond;
    }

    private int parseLogicalOr(String s, int[] pos) {
        int v = parseLogicalAnd(s, pos);
        while (matchOp(s, pos, "||")) {
            boolean prevMute = arithMute;
            if (v != 0) arithMute = true;
            int r = parseLogicalAnd(s, pos);
            arithMute = prevMute;
            v = (v != 0 || r != 0) ? 1 : 0;
        }
        return v;
    }

    private int parseLogicalAnd(String s, int[] pos) {
        int v = parseBitOr(s, pos);
        while (matchOp(s, pos, "&&")) {
            boolean prevMute = arithMute;
            if (v == 0) arithMute = true;
            int r = parseBitOr(s, pos);
            arithMute = prevMute;
            v = (v != 0 && r != 0) ? 1 : 0;
        }
        return v;
    }

    private int parseBitOr(String s, int[] pos) {
        int v = parseBitXor(s, pos);
        while (peekIsSingle(s, pos, '|', "||")) {
            pos[0]++;
            v |= parseBitXor(s, pos);
        }
        return v;
    }

    private int parseBitXor(String s, int[] pos) {
        int v = parseBitAnd(s, pos);
        while (peekIs(s, pos, '^')) {
            pos[0]++;
            v ^= parseBitAnd(s, pos);
        }
        return v;
    }

    private int parseBitAnd(String s, int[] pos) {
        int v = parseEquality(s, pos);
        while (peekIsSingle(s, pos, '&', "&&")) {
            pos[0]++;
            v &= parseEquality(s, pos);
        }
        return v;
    }

    private int parseEquality(String s, int[] pos) {
        int v = parseRelational(s, pos);
        while (true) {
            if (matchOp(s, pos, "==")) { v = (v == parseRelational(s, pos)) ? 1 : 0; }
            else if (matchOp(s, pos, "!=")) { v = (v != parseRelational(s, pos)) ? 1 : 0; }
            else break;
        }
        return v;
    }

    private int parseRelational(String s, int[] pos) {
        int v = parseShift(s, pos);
        while (true) {
            if (matchOp(s, pos, "<=")) { v = (v <= parseShift(s, pos)) ? 1 : 0; }
            else if (matchOp(s, pos, ">=")) { v = (v >= parseShift(s, pos)) ? 1 : 0; }
            else if (peekIsSingleNotFollowedBy(s, pos, '<', '<', '=')) { pos[0]++; v = (v < parseShift(s, pos)) ? 1 : 0; }
            else if (peekIsSingleNotFollowedBy(s, pos, '>', '>', '=')) { pos[0]++; v = (v > parseShift(s, pos)) ? 1 : 0; }
            else break;
        }
        return v;
    }

    private int parseShift(String s, int[] pos) {
        int v = parseAdditive(s, pos);
        while (true) {
            if (matchOp(s, pos, "<<")) { v <<= parseAdditive(s, pos); }
            else if (matchOp(s, pos, ">>")) { v >>= parseAdditive(s, pos); }
            else break;
        }
        return v;
    }

    private int parseAdditive(String s, int[] pos) {
        int value = parseTerm(s, pos);
        while (pos[0] < s.length()) {
            skipWhitespace(s, pos);
            if (pos[0] >= s.length()) break;
            char c = s.charAt(pos[0]);
            if (c == '+') { pos[0]++; value += parseTerm(s, pos); }
            else if (c == '-') { pos[0]++; value -= parseTerm(s, pos); }
            else break;
        }
        return value;
    }

    private int parseTerm(String s, int[] pos) {
        int value = parseFactor(s, pos);
        while (pos[0] < s.length()) {
            skipWhitespace(s, pos);
            if (pos[0] >= s.length()) break;
            char c = s.charAt(pos[0]);
            if (c == '*') { pos[0]++; value *= parseFactor(s, pos); }
            else if (c == '/') { pos[0]++; int d = parseFactor(s, pos); value = d != 0 ? value / d : 0; }
            else if (c == '%') { pos[0]++; int d = parseFactor(s, pos); value = d != 0 ? value % d : 0; }
            else break;
        }
        return value;
    }

    private int parseFactor(String s, int[] pos) {
        skipWhitespace(s, pos);
        if (pos[0] >= s.length()) return 0;
        char c = s.charAt(pos[0]);
        if (c == '(') {
            pos[0]++;
            int v = parseArithmetic(s, pos);
            skipWhitespace(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ')') pos[0]++;
            return v;
        }
        if (c == '+') { pos[0]++; return parseFactor(s, pos); }
        if (c == '-') { pos[0]++; return -parseFactor(s, pos); }
        if (c == '!') { pos[0]++; return parseFactor(s, pos) == 0 ? 1 : 0; }
        if (c == '~') { pos[0]++; return ~parseFactor(s, pos); }
        if (Character.isLetter(c) || c == '_') {
            int j = pos[0];
            while (j < s.length() && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) j++;
            String name = s.substring(pos[0], j);
            pos[0] = j;
            String val = arithEnv != null ? arithEnv.getOrDefault(name, "0") : "0";
            try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return 0; }
        }
        int num = 0;
        while (pos[0] < s.length() && Character.isDigit(s.charAt(pos[0]))) {
            num = num * 10 + (s.charAt(pos[0]) - '0');
            pos[0]++;
        }
        return num;
    }

    private boolean matchOp(String s, int[] pos, String op) {
        skipWhitespace(s, pos);
        if (pos[0] + op.length() > s.length()) return false;
        for (int k = 0; k < op.length(); k++) {
            if (s.charAt(pos[0] + k) != op.charAt(k)) return false;
        }
        pos[0] += op.length();
        return true;
    }

    private boolean peekIs(String s, int[] pos, char c) {
        skipWhitespace(s, pos);
        return pos[0] < s.length() && s.charAt(pos[0]) == c;
    }

    private boolean peekIsSingle(String s, int[] pos, char c, String avoidDouble) {
        skipWhitespace(s, pos);
        if (pos[0] >= s.length() || s.charAt(pos[0]) != c) return false;
        if (pos[0] + 1 < s.length()) {
            char next = s.charAt(pos[0] + 1);
            for (int k = 1; k < avoidDouble.length(); k++) {
                if (next == avoidDouble.charAt(k) && avoidDouble.charAt(0) == c) return false;
            }
        }
        return true;
    }

    private boolean peekIsSingleNotFollowedBy(String s, int[] pos, char c, char nf1, char nf2) {
        skipWhitespace(s, pos);
        if (pos[0] >= s.length() || s.charAt(pos[0]) != c) return false;
        if (pos[0] + 1 < s.length()) {
            char next = s.charAt(pos[0] + 1);
            if (next == nf1 || next == nf2) return false;
        }
        return true;
    }

    private void skipWhitespace(String s, int[] pos) {
        while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) pos[0]++;
    }
}
