/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Static word-classification helpers for {@link Lexer}.
 *
 * @since 1.0
 */
final class LexerSupport {

    private LexerSupport() {
    }

    /**
     * Checks whether a word is a POSIX assignment.
     *
     * @param text Any word
     * @return True when the word is a valid assignment
     */
    static boolean isAssignmentWord(final String text) {
        boolean result = false;
        if (!text.isEmpty() && !"=".equals(text)) {
            final int equal = text.indexOf('=');
            if (equal > 0) {
                result = LexerSupport.isValidVarName(text.substring(0, equal));
            }
        }
        return result;
    }

    /**
     * Checks whether a name is a valid POSIX variable name.
     *
     * @param name A candidate identifier
     * @return True when the name is valid
     */
    static boolean isValidVarName(final String name) {
        boolean valid = !name.isEmpty();
        if (valid) {
            final char first = name.charAt(0);
            valid = Character.isLetter(first) || first == '_';
        }
        for (int idx = 1; valid && idx < name.length(); idx = idx + 1) {
            final char chr = name.charAt(idx);
            valid = Character.isLetterOrDigit(chr) || chr == '_';
        }
        return valid;
    }

    /**
     * Checks whether a string is non-empty and contains only digits.
     *
     * @param text Any string
     * @return True when the string contains only digits
     */
    static boolean isAllDigits(final String text) {
        boolean result = !text.isEmpty();
        for (int idx = 0; result && idx < text.length(); idx = idx + 1) {
            if (!Character.isDigit(text.charAt(idx))) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Checks whether a character is a POSIX operator character.
     *
     * @param chr A character
     * @return True when the character ends a word
     */
    static boolean isOperatorChar(final int chr) {
        return LexerSupport.isRedirectionChar(chr) || LexerSupport.isGroupingChar(chr);
    }

    private static boolean isRedirectionChar(final int chr) {
        return LexerSupport.isControlChar(chr) || chr == '<' || chr == '>';
    }

    private static boolean isControlChar(final int chr) {
        return chr == '&' || chr == '|' || chr == ';';
    }

    private static boolean isGroupingChar(final int chr) {
        return chr == '(' || chr == ')' || chr == '{' || chr == '}';
    }
}
