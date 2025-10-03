/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Tokeniser for the POSIX shell grammar (section 2.10). Pulls characters
 * from a {@link CharReader} and emits {@link Token} instances; one call
 * to {@link #nextToken()} yields the next token (or
 * {@link TokenType#END_OF_FILE} when the input is exhausted).
 * @since 1.0
 */
public final class Lexer {

    /** Lookahead cursor over the input. */
    private final CharReader reader;

    /**
     * Creates a lexer over the given character source.
     * @param reader The input cursor
     */
    public Lexer(final CharReader reader) {
        this.reader = reader;
    }

    /**
     * Returns the next token. Skips whitespace before classifying.
     * @return The next token, or an END_OF_FILE token at end-of-input
     */
    public Token nextToken() {
        this.skipWhitespace();
        this.skipComment();
        final Token result;
        if (this.reader.isEof()) {
            result = Token.of(TokenType.END_OF_FILE, "", this.reader);
        } else if (this.reader.peek() == '\n') {
            this.reader.consume();
            result = Token.of(TokenType.NEWLINE, "\n", this.reader);
        } else {
            final Token operator = this.tryOperator();
            if (operator == null) {
                result = this.readWord();
            } else {
                result = operator;
            }
        }
        return result;
    }

    /**
     * Reads the body of a here-document up to (but not including) the
     * closing delimiter line. POSIX 2.7.4: each line ends with a newline,
     * including the last line before the delimiter.
     * @param delimiter The heredoc delimiter
     * @param stripTabs True for {@code <<-} (strip leading tabs on each line)
     * @return The heredoc content with trailing newline per POSIX
     */
    public String readHereDocContent(final String delimiter, final boolean stripTabs) {
        final StringBuilder content = new StringBuilder();
        if (this.reader.peek() == '\n') {
            this.reader.consume();
        }
        boolean done = false;
        while (!done && !this.reader.isEof()) {
            final String lineStr = this.readHereDocLine(stripTabs);
            if (!this.reader.isEof()) {
                this.reader.consume();
            }
            if (lineStr.equals(delimiter)) {
                done = true;
            } else {
                content.append(lineStr).append('\n');
            }
        }
        return content.toString();
    }

    /**
     * Reads one line of heredoc body, optionally stripping leading tabs.
     * @param stripTabs True to strip leading tabs (for {@code <<-})
     * @return The line text (without the trailing newline)
     */
    private String readHereDocLine(final boolean stripTabs) {
        final StringBuilder line = new StringBuilder();
        boolean stripping = stripTabs;
        boolean exit = false;
        while (!exit && !this.reader.isEof()) {
            final int chr = this.reader.peek();
            if (chr == '\n') {
                exit = true;
            } else if (stripping && chr == '\t') {
                this.reader.consume();
            } else {
                stripping = false;
                line.append((char) this.reader.consume());
            }
        }
        return line.toString();
    }

    /**
     * Consumes a comment when {@code #} starts a token position (POSIX
     * 2.10.1). A {@code #} inside a word stays a literal character.
     */
    private void skipComment() {
        while (!this.reader.isEof() && this.reader.peek() == '#') {
            while (!this.reader.isEof() && this.reader.peek() != '\n') {
                this.reader.consume();
            }
            this.skipWhitespace();
        }
    }

    /**
     * Consumes spaces and tabs.
     */
    private void skipWhitespace() {
        while (!this.reader.isEof()
            && (this.reader.peek() == ' ' || this.reader.peek() == '\t')) {
            this.reader.consume();
        }
    }

    /**
     * Tries to match an operator at the current position.
     * @return The operator token, or null when the current character is
     *         not an operator
     */
    private Token tryOperator() {
        final int chr = this.reader.peek();
        final int line = this.reader.getLine();
        final int col = this.reader.getColumn();
        final Token result;
        if (chr == '&') {
            result = this.tryAmpersand(line, col);
        } else if (chr == '|') {
            result = this.tryPipe(line, col);
        } else if (chr == ';') {
            result = this.trySemi(line, col);
        } else if (chr == '<') {
            result = this.tryLess(line, col);
        } else if (chr == '>') {
            result = this.tryGreater(line, col);
        } else if (chr == '(') {
            result = this.singleCharOp(TokenType.LPAREN, line, col);
        } else if (chr == ')') {
            result = this.singleCharOp(TokenType.RPAREN, line, col);
        } else if (chr == '{') {
            result = this.singleCharOp(TokenType.LBRACE, line, col);
        } else if (chr == '}') {
            result = this.singleCharOp(TokenType.RBRACE, line, col);
        } else {
            result = null;
        }
        return result;
    }

    private Token singleCharOp(final TokenType type, final int line, final int col) {
        final int chr = this.reader.consume();
        return new Token(type, String.valueOf((char) chr), line, col);
    }

    private Token tryAmpersand(final int line, final int col) {
        this.reader.consume();
        final Token result;
        if (this.reader.peek() == '&') {
            this.reader.consume();
            result = new Token(TokenType.AND_IF, "&&", line, col);
        } else {
            result = new Token(TokenType.AMP, "&", line, col);
        }
        return result;
    }

    private Token tryPipe(final int line, final int col) {
        this.reader.consume();
        final Token result;
        if (this.reader.peek() == '|') {
            this.reader.consume();
            result = new Token(TokenType.OR_IF, "||", line, col);
        } else {
            result = new Token(TokenType.PIPE, "|", line, col);
        }
        return result;
    }

    private Token trySemi(final int line, final int col) {
        this.reader.consume();
        final Token result;
        if (this.reader.peek() == ';') {
            this.reader.consume();
            result = new Token(TokenType.DSEMI, ";;", line, col);
        } else {
            result = new Token(TokenType.SEMI, ";", line, col);
        }
        return result;
    }

    private Token tryLess(final int line, final int col) {
        this.reader.consume();
        final Token result;
        final int next = this.reader.peek();
        if (next == '<') {
            this.reader.consume();
            if (this.reader.peek() == '-') {
                this.reader.consume();
                result = new Token(TokenType.DLESSDASH, "<<-", line, col);
            } else {
                result = new Token(TokenType.DLESS, "<<", line, col);
            }
        } else if (next == '&') {
            this.reader.consume();
            result = new Token(TokenType.LESSAND, "<&", line, col);
        } else if (next == '>') {
            this.reader.consume();
            result = new Token(TokenType.LESSGREATER, "<>", line, col);
        } else {
            result = new Token(TokenType.LESS, "<", line, col);
        }
        return result;
    }

    private Token tryGreater(final int line, final int col) {
        this.reader.consume();
        final Token result;
        final int next = this.reader.peek();
        if (next == '>') {
            this.reader.consume();
            result = new Token(TokenType.DGREATER, ">>", line, col);
        } else if (next == '|') {
            this.reader.consume();
            result = new Token(TokenType.CLOBBER, ">|", line, col);
        } else if (next == '&') {
            this.reader.consume();
            result = new Token(TokenType.GREATAND, ">&", line, col);
        } else {
            result = new Token(TokenType.GREAT, ">", line, col);
        }
        return result;
    }

    /**
     * Reads a single WORD token, honouring quoting and the
     * {@code $(…)}, {@code $((…))}, {@code ${…}}, and {@code `…`}
     * balanced word-boundaries (POSIX 2.10).
     * @return The next word token
     */
    private Token readWord() {
        final int line = this.reader.getLine();
        final int col = this.reader.getColumn();
        final StringBuilder buf = new StringBuilder();
        final QuoteState quotes = new QuoteState();
        boolean done = false;
        while (!done && !this.reader.isEof()) {
            done = this.readWordChar(buf, quotes);
        }
        final String text = buf.toString();
        final TokenType type;
        if (isAllDigits(text) && !this.reader.isEof()
            && (this.reader.peek() == '<' || this.reader.peek() == '>')) {
            type = TokenType.IO_NUMBER;
        } else if (isAssignmentWord(text)) {
            type = TokenType.ASSIGNMENT_WORD;
        } else {
            type = TokenType.WORD;
        }
        return new Token(type, text, line, col);
    }

    /**
     * Consumes one character into the word buffer.
     * @param buf    The accumulator for the current word
     * @param quotes Quote-state flags (mutated)
     * @return True when the word ends at the current position
     */
    private boolean readWordChar(final StringBuilder buf, final QuoteState quotes) {
        final int chr = this.reader.peek();
        boolean done = false;
        if (chr == '\\' && !quotes.inSingle && !quotes.inDouble) {
            this.consumeBackslash(buf);
        } else if (chr == '\'' && !quotes.inDouble) {
            this.reader.consume();
            buf.append('\'');
            quotes.inSingle = !quotes.inSingle;
        } else if (chr == '"' && !quotes.inSingle) {
            this.reader.consume();
            buf.append('"');
            quotes.inDouble = !quotes.inDouble;
        } else if (quotes.inSingle) {
            buf.append((char) this.reader.consume());
        } else if (quotes.inDouble) {
            this.consumeDoubleQuoted(buf, chr);
        } else if (chr == '$') {
            this.consumeDollar(buf);
        } else if (chr == '`') {
            this.consumeBackticks(buf);
        } else if (chr == '\n' || chr == ' ' || chr == '\t' || isOperatorChar(chr)) {
            done = true;
        } else {
            buf.append((char) this.reader.consume());
        }
        return done;
    }

    /**
     * Handles a backslash outside any quotes (line continuation or escape).
     */
    private void consumeBackslash(final StringBuilder buf) {
        this.reader.consume();
        if (this.reader.peek() == '\n') {
            this.reader.consume();
        } else {
            buf.append('\\');
            if (!this.reader.isEof()) {
                buf.append((char) this.reader.consume());
            }
        }
    }

    /**
     * Handles a character inside double-quotes.
     */
    private void consumeDoubleQuoted(final StringBuilder buf, final int chr) {
        if (chr == '\\') {
            this.reader.consume();
            buf.append('\\');
            final int next = this.reader.peek();
            if (next == '\n') {
                this.reader.consume();
                buf.deleteCharAt(buf.length() - 1);
            } else if (next == '$' || next == '`' || next == '"' || next == '\\') {
                buf.append((char) this.reader.consume());
            }
        } else {
            buf.append((char) this.reader.consume());
        }
    }

    /**
     * Handles a {@code $}-introduced expansion: {@code $(...)},
     * {@code $((...))}, {@code ${...}}, or a bare {@code $NAME}.
     */
    private void consumeDollar(final StringBuilder buf) {
        this.reader.consume();
        buf.append('$');
        if (!this.reader.isEof() && this.reader.peek() == '(') {
            this.consumeBalancedParens(buf);
        } else if (!this.reader.isEof() && this.reader.peek() == '{') {
            this.consumeBalancedBraces(buf);
        }
    }

    /**
     * Handles a {@code $(...)} or {@code $((...))} expansion by balancing
     * the opening parens.
     */
    private void consumeBalancedParens(final StringBuilder buf) {
        this.reader.consume();
        buf.append('(');
        final boolean arith = !this.reader.isEof() && this.reader.peek() == '(';
        int depth = 1;
        if (arith) {
            this.reader.consume();
            buf.append('(');
            depth = 2;
        }
        while (!this.reader.isEof() && depth > 0) {
            final int next = this.reader.consume();
            buf.append((char) next);
            if (next == '(') {
                depth = depth + 1;
            } else if (next == ')') {
                depth = depth - 1;
            }
        }
    }

    /**
     * Handles a {@code ${...}} expansion by balancing the opening brace.
     */
    private void consumeBalancedBraces(final StringBuilder buf) {
        this.reader.consume();
        buf.append('{');
        int depth = 1;
        while (!this.reader.isEof() && depth > 0) {
            final int next = this.reader.consume();
            buf.append((char) next);
            if (next == '{') {
                depth = depth + 1;
            } else if (next == '}') {
                depth = depth - 1;
            }
        }
    }

    /**
     * Handles a backtick-delimited command substitution.
     */
    private void consumeBackticks(final StringBuilder buf) {
        this.reader.consume();
        buf.append('`');
        boolean done = false;
        while (!done && !this.reader.isEof()) {
            final int next = this.reader.consume();
            buf.append((char) next);
            if (next == '`') {
                done = true;
            } else if (next == '\\' && !this.reader.isEof()) {
                buf.append((char) this.reader.consume());
            }
        }
    }

    /**
     * @param text Any word
     * @return True when the word is a valid POSIX assignment ({@code NAME=value})
     */
    private static boolean isAssignmentWord(final String text) {
        boolean result = false;
        if (!text.isEmpty() && !"=".equals(text)) {
            final int eqIndex = text.indexOf('=');
            if (eqIndex > 0) {
                result = isValidVarName(text.substring(0, eqIndex));
            }
        }
        return result;
    }

    /**
     * @param name A candidate identifier
     * @return True when {@code name} is a valid POSIX variable name
     */
    private static boolean isValidVarName(final String name) {
        boolean valid = true;
        final char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            valid = false;
        } else {
            for (int idx = 1; valid && idx < name.length(); idx = idx + 1) {
                final char chr = name.charAt(idx);
                if (!Character.isLetterOrDigit(chr) && chr != '_') {
                    valid = false;
                }
            }
        }
        return valid;
    }

    /**
     * @param text Any string
     * @return True when {@code text} is non-empty and contains only digits
     */
    private static boolean isAllDigits(final String text) {
        boolean result = !text.isEmpty();
        for (int idx = 0; result && idx < text.length(); idx = idx + 1) {
            if (!Character.isDigit(text.charAt(idx))) {
                result = false;
            }
        }
        return result;
    }

    /**
     * @param chr A character
     * @return True when {@code chr} is one of the POSIX operator characters
     *         that end a word
     */
    private static boolean isOperatorChar(final int chr) {
        return chr == '&' || chr == '|' || chr == ';' || chr == '<' || chr == '>'
            || chr == '(' || chr == ')' || chr == '{' || chr == '}';
    }

    /**
     * Mutable holder for the current quoting state inside {@link #readWord()}.
     * @since 1.0
     */
    private static final class QuoteState {

        /** True while the scanner is inside single quotes. */
        private boolean inSingle;
        /** True while the scanner is inside double quotes. */
        private boolean inDouble;

        /**
         * Default constructor; both flags start false.
         */
        QuoteState() {
            super();
        }
    }
}
