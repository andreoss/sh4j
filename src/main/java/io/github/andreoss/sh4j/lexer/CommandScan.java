/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Scans the body of a command substitution for its closing parenthesis
 * (POSIX 2.6.3). Tracks nested parentheses and case commands, so the
 * parenthesis that ends a case pattern does not end the substitution.
 *
 * @since 1.0
 */
final class CommandScan {

    /** Reserved words after which a command starts. */
    private static final Set<String> OPENERS = Set.of(
        "if", "then", "else", "elif", "do", "while", "until", "!", "{"
    );

    /** Characters that end a word, besides blanks and newlines. */
    private static final String ENDS = ";&|()<>'\"`\\$";

    /** Characters that end a word but not a token. */
    private static final String QUOTES = "'\"`\\$";

    /** Nested-construct finder over the text. */
    private final Nesting nesting;

    /** Source text. */
    private final String text;

    /** Index where the body starts. */
    private final int first;

    /** Open constructs: parenthesis, case word, case pattern, case body. */
    private final Deque<Character> stack;

    /** Current index. */
    private int pos;

    /** Whether the next word is in command position. */
    private boolean head;

    /**
     * Ctor.
     *
     * @param nesting Nested-construct finder over the text
     * @param text Source text
     * @param start Index just after the opening dollar and parenthesis
     */
    CommandScan(final Nesting nesting, final String text, final int start) {
        this.nesting = nesting;
        this.text = text;
        this.first = start;
        this.stack = new ArrayDeque<>(4);
        this.pos = start;
        this.head = true;
    }

    /**
     * Finds the closing parenthesis.
     *
     * @return Index just after it, or -1 when absent
     */
    int close() {
        int result = -1;
        while (this.pos < this.text.length() && result < 0) {
            result = this.step();
        }
        return result;
    }

    private int step() {
        final char chr = this.text.charAt(this.pos);
        int result = -1;
        if (chr == ')') {
            result = this.closing();
        } else if (chr == '(') {
            this.opening();
        } else if (chr == ';') {
            this.semicolon();
        } else {
            this.other(chr);
        }
        return result;
    }

    private void other(final char chr) {
        if (chr == ' ' || chr == '\t') {
            this.pos = this.pos + 1;
        } else if (chr == '\n' || chr == '&' || chr == '|') {
            this.pos = this.pos + 1;
            this.head = true;
        } else if (CommandScan.ENDS.indexOf(chr) >= 0) {
            this.pos = this.nesting.skip(this.pos, false);
            this.head = false;
        } else if (chr == '#' && this.boundary()) {
            this.comment();
        } else {
            this.word();
        }
    }

    private int closing() {
        final char top = this.top();
        int result = -1;
        this.pos = this.pos + 1;
        if (top == ' ') {
            result = this.pos;
        } else if (top == 'p') {
            this.replace('b');
        } else {
            this.stack.pop();
        }
        this.head = true;
        return result;
    }

    private void opening() {
        if (this.top() != 'p') {
            this.stack.push('(');
        }
        this.pos = this.pos + 1;
        this.head = true;
    }

    private void semicolon() {
        if (this.top() == 'b' && this.text.startsWith(";;", this.pos)) {
            this.replace('p');
            this.pos = this.pos + 2;
        } else {
            this.pos = this.pos + 1;
        }
        this.head = true;
    }

    private boolean boundary() {
        return this.pos == this.first
            || CommandScan.QUOTES.indexOf(this.text.charAt(this.pos - 1)) < 0;
    }

    private void comment() {
        final int end = this.text.indexOf('\n', this.pos);
        if (end < 0) {
            this.pos = this.text.length();
        } else {
            this.pos = end;
        }
    }

    private void word() {
        int end = this.pos;
        while (end < this.text.length() && CommandScan.inword(this.text.charAt(end))) {
            end = end + 1;
        }
        final String word = this.text.substring(this.pos, end);
        this.pos = end;
        if (end < this.text.length()
            && CommandScan.QUOTES.indexOf(this.text.charAt(end)) >= 0) {
            this.head = false;
        } else {
            this.keyword(word);
        }
    }

    private void keyword(final String word) {
        final char top = this.top();
        boolean next = false;
        if (top == 'c') {
            if ("in".equals(word)) {
                this.replace('p');
            }
        } else if (top == 'p') {
            if ("esac".equals(word)) {
                this.stack.pop();
            }
        } else if (this.head) {
            next = this.command(word, top);
        }
        this.head = next;
    }

    private boolean command(final String word, final char top) {
        boolean result = false;
        if ("case".equals(word)) {
            this.stack.push('c');
        } else if (top == 'b' && "esac".equals(word)) {
            this.stack.pop();
        } else {
            result = CommandScan.OPENERS.contains(word);
        }
        return result;
    }

    private char top() {
        final char result;
        if (this.stack.isEmpty()) {
            result = ' ';
        } else {
            result = this.stack.peek();
        }
        return result;
    }

    private void replace(final char state) {
        this.stack.pop();
        this.stack.push(state);
    }

    private static boolean inword(final char chr) {
        return chr != ' ' && chr != '\t' && chr != '\n'
            && CommandScan.ENDS.indexOf(chr) < 0;
    }
}
