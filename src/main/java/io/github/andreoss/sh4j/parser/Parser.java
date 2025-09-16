/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.Token;
import io.github.andreoss.sh4j.lexer.TokenType;

/**
 * Recursive-descent parser for the POSIX shell grammar (section 2.10).
 * Pulls tokens from a {@link Lexer} and emits a {@link ListNode} AST that
 * the executor walks.
 * @since 1.0
 */
public final class Parser {

    /** Source token stream. */
    private final Lexer lexer;
    /** Current token (the one the parser is about to consume). */
    private Token current;
    /** Cached one-token lookahead (null when not yet peeked). */
    private Token lookahead;
    /** Cached two-token lookahead (only valid when {@link #hasLookahead2} is true). */
    private Token lookahead2;
    /** True when {@link #lookahead2} has been filled. */
    private boolean hasLookahead2;

    /**
     * Creates a parser over the given lexer and primes the first token.
     * @param lexer The token source
     */
    public Parser(final Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
        this.lookahead = null;
        this.lookahead2 = null;
        this.hasLookahead2 = false;
    }

    /**
     * Parses the entire token stream as a top-level command list.
     * @return The AST root
     */
    public ListNode parse() {
        this.skipNewlines();
        return this.parseList(TokenType.END_OF_FILE);
    }

    private Token consume() {
        final Token cur = this.current;
        if (this.lookahead == null) {
            this.current = this.lexer.nextToken();
        } else {
            this.current = this.lookahead;
            if (this.hasLookahead2) {
                this.lookahead = this.lookahead2;
            } else {
                this.lookahead = null;
            }
            this.hasLookahead2 = false;
        }
        return cur;
    }

    private Token peek() {
        if (this.lookahead == null) {
            this.lookahead = this.lexer.nextToken();
            this.hasLookahead2 = false;
        }
        return this.lookahead;
    }

    private Token peek2() {
        this.peek();
        if (!this.hasLookahead2) {
            this.lookahead2 = this.lexer.nextToken();
            this.hasLookahead2 = true;
        }
        return this.lookahead2;
    }

    private Token expect(final TokenType type) {
        if (this.current.type() != type) {
            throw new ParseException(
                "Expected " + type + " but got " + this.current.type()
                    + " at line " + this.current.line()
                    + " column " + this.current.column()
            );
        }
        return this.consume();
    }

    private boolean check(final TokenType type) {
        return this.current.type() == type;
    }

    private void skipNewlines() {
        while (this.check(TokenType.NEWLINE)) {
            this.consume();
        }
    }

    private ListNode parseList(final TokenType... terminators) {
        final ListNode list = new ListNode();
        Pipeline pipeline = this.parsePipeline();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        boolean done = false;
        while (!done && this.isListSeparator()) {
            final ListNode.Separator sep = this.currentSeparator();
            this.consume();
            this.skipNewlines();
            final int lastIdx = list.getPipelines().size() - 1;
            list.getSeparators().set(lastIdx, sep);
            if (this.isTerminator(terminators) || this.check(TokenType.END_OF_FILE)) {
                done = true;
            } else {
                pipeline = this.parsePipeline();
                list.addPipeline(pipeline, ListNode.Separator.NONE);
            }
        }
        return list;
    }

    private boolean isListSeparator() {
        return this.check(TokenType.SEMI) || this.check(TokenType.AMP)
            || this.check(TokenType.AND_IF) || this.check(TokenType.OR_IF)
            || this.check(TokenType.NEWLINE);
    }

    private ListNode.Separator currentSeparator() {
        final ListNode.Separator sep;
        if (this.check(TokenType.SEMI) || this.check(TokenType.NEWLINE)) {
            sep = ListNode.Separator.SEMI;
        } else if (this.check(TokenType.AMP)) {
            sep = ListNode.Separator.AMP;
        } else if (this.check(TokenType.AND_IF)) {
            sep = ListNode.Separator.AND_IF;
        } else {
            sep = ListNode.Separator.OR_IF;
        }
        return sep;
    }

    private boolean isTerminator(final TokenType... terminators) {
        boolean result = false;
        for (final TokenType type : terminators) {
            if (this.check(type)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean isReservedWord(final String... words) {
        boolean result = false;
        if (this.check(TokenType.WORD)) {
            for (final String word : words) {
                if (this.current.text().equals(word)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private Token expectWord(final String text) {
        if (!this.check(TokenType.WORD) || !this.current.text().equals(text)) {
            throw new ParseException(
                "Expected '" + text + "' but got " + this.current.text()
                    + " at line " + this.current.line()
                    + " column " + this.current.column()
            );
        }
        return this.consume();
    }

    private Pipeline parsePipeline() {
        return this.parsePipeline(new HashSet<String>());
    }

    private Pipeline parsePipeline(final Set<String> reservedTerminators) {
        boolean bang = false;
        if (this.check(TokenType.WORD) && "!".equals(this.current.text())) {
            bang = true;
            this.consume();
        }
        final Pipeline pipeline = new Pipeline(bang);
        final Node first = this.parsePipelineStage(reservedTerminators);
        if (first != null) {
            pipeline.addCommand(first);
            boolean done = false;
            while (!done && this.check(TokenType.PIPE)) {
                this.consume();
                final Node next = this.parsePipelineStage(reservedTerminators);
                if (next == null) {
                    done = true;
                } else {
                    pipeline.addCommand(next);
                }
            }
        }
        return pipeline;
    }

    private Node parsePipelineStage(final Set<String> reservedTerminators) {
        Node result;
        if (this.check(TokenType.LPAREN)) {
            this.consume();
            final ListNode inner = this.parseList(TokenType.RPAREN);
            this.expect(TokenType.RPAREN);
            result = new Subshell(inner);
        } else if (this.check(TokenType.LBRACE)) {
            this.consume();
            final ListNode inner = this.parseList(TokenType.RBRACE);
            this.expect(TokenType.RBRACE);
            result = new Grouping(inner);
        } else if (this.check(TokenType.WORD) && "if".equals(this.current.text())) {
            result = this.parseIfCommand();
        } else if (this.check(TokenType.WORD) && "for".equals(this.current.text())) {
            result = this.parseForCommand();
        } else if (this.check(TokenType.WORD)
            && ("while".equals(this.current.text()) || "until".equals(this.current.text()))) {
            result = this.parseWhileCommand();
        } else if (this.check(TokenType.WORD) && "case".equals(this.current.text())) {
            result = this.parseCaseCommand();
        } else if (this.check(TokenType.WORD)
            && reservedTerminators.contains(this.current.text())) {
            result = null;
        } else if (this.check(TokenType.WORD)
            && this.peek().type() == TokenType.LPAREN
            && this.peek2().type() == TokenType.RPAREN) {
            result = this.parseFunctionDefinition();
        } else {
            result = this.parseSimpleCommand();
        }
        return result;
    }

    private WhileNode parseWhileCommand() {
        final boolean until = "until".equals(this.current.text());
        this.consume();
        final ListNode condition = this.parseListUntilReserved("do");
        if (this.check(TokenType.SEMI)) {
            this.consume();
        }
        this.expectWord("do");
        final ListNode body = this.parseListUntilReserved("done");
        this.expectWord("done");
        return new WhileNode(condition, body, until);
    }

    private ForNode parseForCommand() {
        this.consume();
        final String varName = this.expect(TokenType.WORD).text();
        final List<String> words;
        if (this.check(TokenType.WORD) && "in".equals(this.current.text())) {
            this.consume();
            words = new ArrayList<>();
            while (this.check(TokenType.WORD) && !this.isReservedWord("do")) {
                words.add(this.current.text());
                this.consume();
            }
        } else {
            words = null;
        }
        if (this.check(TokenType.SEMI)) {
            this.consume();
        }
        this.expectWord("do");
        final ListNode body = this.parseListUntilReserved("done");
        this.expectWord("done");
        return new ForNode(varName, words, body);
    }

    private FunctionNode parseFunctionDefinition() {
        final String name = this.consume().text();
        this.consume();
        this.consume();
        final Node body = this.parseCompoundCommandBody();
        return new FunctionNode(name, body);
    }

    private Node parseCompoundCommandBody() {
        final Node body;
        if (this.check(TokenType.LPAREN)) {
            this.consume();
            final ListNode inner = this.parseList(TokenType.RPAREN);
            this.expect(TokenType.RPAREN);
            body = new Subshell(inner);
        } else if (this.check(TokenType.LBRACE)) {
            this.consume();
            final ListNode inner = this.parseList(TokenType.RBRACE);
            this.expect(TokenType.RBRACE);
            body = new Grouping(inner);
        } else if (this.isReservedWord("if")) {
            body = this.parseIfCommand();
        } else if (this.isReservedWord("for")) {
            body = this.parseForCommand();
        } else if (this.isReservedWord("while", "until")) {
            body = this.parseWhileCommand();
        } else if (this.isReservedWord("case")) {
            body = this.parseCaseCommand();
        } else {
            throw new ParseException(
                "Expected compound command after function definition at line "
                    + this.current.line() + " column " + this.current.column()
            );
        }
        return body;
    }

    private CaseNode parseCaseCommand() {
        this.consume();
        final String word = this.expect(TokenType.WORD).text();
        this.expectWord("in");
        this.skipNewlines();
        final List<CaseNode.CaseItem> items = new ArrayList<>();
        while (!this.isReservedWord("esac")) {
            if (this.check(TokenType.LPAREN)) {
                this.consume();
            }
            final List<String> patterns = new ArrayList<>();
            patterns.add(this.expect(TokenType.WORD).text());
            while (this.check(TokenType.PIPE)) {
                this.consume();
                patterns.add(this.expect(TokenType.WORD).text());
            }
            this.expect(TokenType.RPAREN);
            final ListNode body;
            if (this.check(TokenType.DSEMI)) {
                body = new ListNode();
            } else {
                body = this.parseList(TokenType.DSEMI);
            }
            this.expect(TokenType.DSEMI);
            this.skipNewlines();
            items.add(new CaseNode.CaseItem(patterns, body));
        }
        this.expectWord("esac");
        return new CaseNode(word, items);
    }

    private IfNode parseIfCommand() {
        this.consume();
        final ListNode condition = this.parseListUntilReserved("then");
        this.expectWord("then");
        final ListNode thenBody = this.parseListUntilReserved("else", "elif", "fi");
        ListNode elseBody = null;
        if (this.isReservedWord("else")) {
            this.consume();
            elseBody = this.parseListUntilReserved("fi");
        } else if (this.isReservedWord("elif")) {
            elseBody = this.parseElifChain();
        }
        this.expectWord("fi");
        return new IfNode(condition, thenBody, elseBody);
    }

    private ListNode parseElifChain() {
        this.consume();
        final ListNode cond = this.parseListUntilReserved("then");
        this.expectWord("then");
        final ListNode body = this.parseListUntilReserved("else", "elif", "fi");
        ListNode trailing = null;
        if (this.isReservedWord("elif")) {
            trailing = this.parseElifChain();
        } else if (this.isReservedWord("else")) {
            this.consume();
            trailing = this.parseListUntilReserved("fi");
        }
        final IfNode elifNode = new IfNode(cond, body, trailing);
        final Pipeline pipe = new Pipeline(false);
        pipe.addCommand(elifNode);
        final ListNode wrapper = new ListNode();
        wrapper.addPipeline(pipe, ListNode.Separator.NONE);
        return wrapper;
    }

    private ListNode parseListUntilReserved(final String... reservedWordTerminators) {
        final ListNode list = new ListNode();
        final Set<String> reserved = new HashSet<>(Arrays.asList(reservedWordTerminators));
        if (!this.isReservedWord(reservedWordTerminators)) {
            boolean done = false;
            while (!done) {
                final Pipeline pipe = this.parsePipeline(reserved);
                list.addPipeline(pipe, ListNode.Separator.NONE);
                if (this.isPipelineSeparator()) {
                    final ListNode.Separator sep = this.pipelineSeparator();
                    this.consume();
                    final int lastIdx = list.getPipelines().size() - 1;
                    list.getSeparators().set(lastIdx, sep);
                    if (this.isReservedWord(reservedWordTerminators)) {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        }
        return list;
    }

    private boolean isPipelineSeparator() {
        return this.check(TokenType.SEMI) || this.check(TokenType.AMP)
            || this.check(TokenType.AND_IF) || this.check(TokenType.OR_IF);
    }

    private ListNode.Separator pipelineSeparator() {
        final ListNode.Separator sep;
        if (this.check(TokenType.SEMI)) {
            sep = ListNode.Separator.SEMI;
        } else if (this.check(TokenType.AMP)) {
            sep = ListNode.Separator.AMP;
        } else if (this.check(TokenType.AND_IF)) {
            sep = ListNode.Separator.AND_IF;
        } else {
            sep = ListNode.Separator.OR_IF;
        }
        return sep;
    }

    private SimpleCommand parseSimpleCommand() {
        final SimpleCommand cmd;
        if (this.startsSimpleCommand()) {
            cmd = this.startSimpleCommand();
            this.continueSimpleCommand(cmd);
        } else {
            cmd = new SimpleCommand("");
        }
        return cmd;
    }

    private boolean startsSimpleCommand() {
        return this.check(TokenType.WORD) || this.check(TokenType.ASSIGNMENT_WORD)
            || this.isRedirectionStart() || this.check(TokenType.IO_NUMBER);
    }

    private boolean isRedirectionStart() {
        return this.check(TokenType.LESS) || this.check(TokenType.GREAT)
            || this.check(TokenType.CLOBBER) || this.check(TokenType.DGREATER)
            || this.check(TokenType.DLESS) || this.check(TokenType.DLESSDASH)
            || this.check(TokenType.LESSAND) || this.check(TokenType.GREATAND)
            || this.check(TokenType.LESSGREATER);
    }

    private SimpleCommand startSimpleCommand() {
        final SimpleCommand cmd;
        if (this.check(TokenType.ASSIGNMENT_WORD)) {
            cmd = this.startWithAssignments();
        } else if (this.check(TokenType.IO_NUMBER)) {
            this.consume();
            cmd = new SimpleCommand("");
        } else {
            final String name = this.current.text();
            final int line = this.current.line();
            this.consume();
            cmd = new SimpleCommand(name);
            cmd.setLineNumber(line);
        }
        return cmd;
    }

    private SimpleCommand startWithAssignments() {
        SimpleCommand cmd = new SimpleCommand("");
        while (this.check(TokenType.ASSIGNMENT_WORD)) {
            cmd.addAssignment(this.current.text());
            this.consume();
        }
        if (this.check(TokenType.WORD)) {
            final String name = this.current.text();
            final int line = this.current.line();
            this.consume();
            final SimpleCommand named = new SimpleCommand(name);
            named.setLineNumber(line);
            named.getAssignments().addAll(cmd.getAssignments());
            cmd = named;
        }
        return cmd;
    }

    private void continueSimpleCommand(final SimpleCommand cmd) {
        int pendingFd = -1;
        if (this.check(TokenType.IO_NUMBER)) {
            pendingFd = Integer.parseInt(this.current.text());
            this.consume();
        }
        final boolean hasName = !cmd.getName().isEmpty();
        while (this.startsSimpleCommand()) {
            if (this.check(TokenType.ASSIGNMENT_WORD)) {
                if (hasName) {
                    cmd.addArgument(this.current.text());
                } else {
                    cmd.addAssignment(this.current.text());
                }
                this.consume();
            } else if (this.check(TokenType.IO_NUMBER)) {
                pendingFd = Integer.parseInt(this.current.text());
                this.consume();
            } else if (this.isRedirectionStart()) {
                this.consumeRedirection(cmd, pendingFd);
                pendingFd = -1;
            } else if (this.check(TokenType.WORD)) {
                cmd.addArgument(this.current.text());
                this.consume();
            }
        }
    }

    private void consumeRedirection(final SimpleCommand cmd, final int pendingFd) {
        if (this.check(TokenType.DLESS) || this.check(TokenType.DLESSDASH)) {
            final boolean stripTabs = this.current.type() == TokenType.DLESSDASH;
            this.consume();
            final String rawDelim = this.expect(TokenType.WORD).text();
            final boolean quoted = isQuotedHereDocDelim(rawDelim);
            final String delimiter;
            if (quoted) {
                delimiter = stripDelimQuotes(rawDelim);
            } else {
                delimiter = rawDelim;
            }
            final String content = this.lexer.readHereDocContent(delimiter, stripTabs);
            final int desc = pendingFd >= 0 ? pendingFd : 0;
            cmd.addRedirection(
                new Redirection(desc, RedirectionType.HERE_DOC, delimiter, content, quoted)
            );
        } else {
            final RedirectionType type = this.redirectionTypeForCurrent();
            final int defaultFd = defaultFdFor(type);
            this.consume();
            final String target = this.expect(TokenType.WORD).text();
            final int desc = pendingFd >= 0 ? pendingFd : defaultFd;
            cmd.addRedirection(new Redirection(desc, type, target));
        }
    }

    private RedirectionType redirectionTypeForCurrent() {
        final RedirectionType type;
        if (this.check(TokenType.LESSAND)) {
            type = RedirectionType.DUP_INPUT;
        } else if (this.check(TokenType.LESSGREATER)) {
            type = RedirectionType.READWRITE;
        } else if (this.check(TokenType.LESS)) {
            type = RedirectionType.INPUT;
        } else if (this.check(TokenType.GREATAND)) {
            type = RedirectionType.DUP_OUTPUT;
        } else if (this.check(TokenType.GREAT)) {
            type = RedirectionType.OUTPUT;
        } else if (this.check(TokenType.CLOBBER)) {
            type = RedirectionType.CLOBBER;
        } else {
            type = RedirectionType.APPEND;
        }
        return type;
    }

    private static boolean isQuotedHereDocDelim(final String word) {
        return word.length() >= 2
            && (word.charAt(0) == '\'' && word.charAt(word.length() - 1) == '\''
                || word.charAt(0) == '"' && word.charAt(word.length() - 1) == '"'
                || word.indexOf('\\') >= 0);
    }

    private static String stripDelimQuotes(final String word) {
        final StringBuilder out = new StringBuilder(word.length());
        boolean escaped = false;
        for (int idx = 0; idx < word.length(); idx = idx + 1) {
            final char chr = word.charAt(idx);
            if (escaped) {
                out.append(chr);
                escaped = false;
            } else if (chr == '\\') {
                escaped = true;
            } else if (chr != '\'' && chr != '"') {
                out.append(chr);
            }
        }
        return out.toString();
    }

    private static int defaultFdFor(final RedirectionType type) {
        final int desc;
        if (type == RedirectionType.DUP_INPUT
            || type == RedirectionType.READWRITE
            || type == RedirectionType.INPUT) {
            desc = 0;
        } else {
            desc = 1;
        }
        return desc;
    }
}
