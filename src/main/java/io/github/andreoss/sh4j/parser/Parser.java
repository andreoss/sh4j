/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.Token;
import io.github.andreoss.sh4j.lexer.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser for the POSIX shell grammar (section 2.10).
 * Pulls tokens from a {@link Lexer} and emits a {@link ListNode} AST that
 * the executor walks.
 *
 * @since 1.0
 */
public final class Parser {

    /** Tokens that terminate a pipeline. */
    private static final Set<TokenType> SEPARATORS = Set.of(
        TokenType.SEMI, TokenType.AMP, TokenType.AND_IF,
        TokenType.OR_IF, TokenType.NEWLINE
    );

    /** Source token stream. */
    private final Lexer lexer;

    /** Current token (the one the parser is about to consume). */
    private Token current;

    /** Cached one-token lookahead. */
    private Token next;

    /** True when {@link #next} holds a peeked token. */
    private boolean hasnext;

    /** Cached two-token lookahead (only valid when {@link #cached} is true). */
    private Token second;

    /** True when {@link #second} has been filled. */
    private boolean cached;

    /**
     * Creates a parser over the given lexer.
     *
     * @param lexer The token source
     */
    public Parser(final Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Parses the entire token stream as a top-level command list.
     *
     * @return The AST root
     */
    public ListNode parse() {
        this.init();
        this.skipNewlines();
        return this.parseList(TokenType.END_OF_FILE);
    }

    private void init() {
        this.current = this.lexer.nextToken();
        this.next = this.current;
        this.cached = false;
        this.hasnext = false;
    }

    private Token consume() {
        final Token cur;
        if (this.hasnext) {
            cur = this.current;
            this.current = this.next;
            if (this.cached) {
                this.next = this.second;
                this.hasnext = true;
            } else {
                this.hasnext = false;
            }
            this.cached = false;
        } else {
            cur = this.current;
            this.current = this.lexer.nextToken();
        }
        return cur;
    }

    private Token peekNext() {
        if (!this.hasnext) {
            this.next = this.lexer.nextToken();
            this.hasnext = true;
            this.cached = false;
        }
        return this.next;
    }

    private Token peekSecond() {
        this.peekNext();
        if (!this.cached) {
            this.second = this.lexer.nextToken();
            this.cached = true;
        }
        return this.second;
    }

    private Token expect(final TokenType type) {
        if (this.current.type() != type) {
            throw new ParseException(
                String.format(
                    "Expected %s but got %s at line %d column %d",
                    type, this.current.type(),
                    this.current.line(), this.current.column()
                )
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
            final int last = list.getPipelines().size() - 1;
            list.getSeparators().set(last, sep);
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
        return this.isPipelineSeparator() || this.check(TokenType.NEWLINE);
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
                String.format(
                    "Expected '%s' but got %s at line %d column %d",
                    text, this.current.text(),
                    this.current.line(), this.current.column()
                )
            );
        }
        return this.consume();
    }

    private Pipeline parsePipeline() {
        return this.parsePipeline(new HashSet<>());
    }

    private Pipeline parsePipeline(final Set<String> reserved) {
        boolean bang = false;
        if (this.check(TokenType.WORD) && "!".equals(this.current.text())) {
            bang = true;
            this.consume();
        }
        final Pipeline pipeline = new Pipeline(bang);
        final Node first = this.parsePipelineStage(reserved);
        if (first != null) {
            pipeline.addCommand(first);
            final boolean done = false;
            while (!done && this.check(TokenType.PIPE)) {
                this.consume();
                final Node stage = this.parsePipelineStage(reserved);
                pipeline.addCommand(stage);
            }
        }
        return pipeline;
    }

    private Node parsePipelineStage(final Set<String> reserved) {
        final Node result;
        if (this.check(TokenType.LPAREN)) {
            result = this.parseSubshell();
        } else if (this.check(TokenType.LBRACE)) {
            result = this.parseGrouping();
        } else if (this.check(TokenType.WORD)) {
            result = this.parseWordStage(reserved);
        } else {
            result = this.parseSimpleCommand();
        }
        return result;
    }

    private Node parseSubshell() {
        this.consume();
        final ListNode inner;
        inner = this.parseList(TokenType.RPAREN);
        this.expect(TokenType.RPAREN);
        return new Subshell(inner);
    }

    private Node parseGrouping() {
        this.consume();
        final ListNode inner;
        inner = this.parseList(TokenType.RBRACE);
        this.expect(TokenType.RBRACE);
        return new Grouping(inner);
    }

    private Node parseWordStage(final Set<String> reserved) {
        final Node result;
        if ("if".equals(this.current.text())) {
            result = this.parseIfCommand();
        } else if ("for".equals(this.current.text())) {
            result = this.parseForCommand();
        } else if (this.isWhileWord()) {
            result = this.parseWhileCommand();
        } else if ("case".equals(this.current.text())) {
            result = this.parseCaseCommand();
        } else if (reserved.contains(this.current.text())) {
            result = null;
        } else if (this.isFunctionStart()) {
            result = this.parseFunctionDefinition();
        } else {
            result = this.parseSimpleCommand();
        }
        return result;
    }

    private boolean isWhileWord() {
        return "while".equals(this.current.text()) || "until".equals(this.current.text());
    }

    private boolean isFunctionStart() {
        return this.peekNext().type() == TokenType.LPAREN
            && this.peekSecond().type() == TokenType.RPAREN;
    }

    private WhileNode parseWhileCommand() {
        final boolean until;
        until = "until".equals(this.current.text());
        this.consume();
        final ListNode condition;
        condition = this.parseListUntilReserved("do");
        if (this.check(TokenType.SEMI)) {
            this.consume();
        }
        this.expectWord("do");
        final ListNode body;
        body = this.parseListUntilReserved("done");
        this.expectWord("done");
        return new WhileNode(condition, body, until);
    }

    private ForNode parseForCommand() {
        this.consume();
        final String name;
        name = this.expect(TokenType.WORD).text();
        final List<String> words;
        if (this.check(TokenType.WORD) && "in".equals(this.current.text())) {
            this.consume();
            words = new ArrayList<>(0);
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
        final ListNode body;
        body = this.parseListUntilReserved("done");
        this.expectWord("done");
        return new ForNode(name, words, body);
    }

    private FunctionNode parseFunctionDefinition() {
        final String name;
        name = this.consume().text();
        this.consume();
        this.consume();
        final Node body;
        body = this.parseCompoundCommandBody();
        return new FunctionNode(name, body);
    }

    private Node parseCompoundCommandBody() {
        final Node body;
        if (this.check(TokenType.LPAREN)) {
            this.consume();
            final ListNode inner;
            inner = this.parseList(TokenType.RPAREN);
            this.expect(TokenType.RPAREN);
            body = new Subshell(inner);
        } else if (this.check(TokenType.LBRACE)) {
            this.consume();
            final ListNode inner;
            inner = this.parseList(TokenType.RBRACE);
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
                String.format(
                    "Expected compound command after function definition at line %d column %d",
                    this.current.line(), this.current.column()
                )
            );
        }
        return body;
    }

    private CaseNode parseCaseCommand() {
        this.consume();
        final String word;
        word = this.expect(TokenType.WORD).text();
        this.expectWord("in");
        this.skipNewlines();
        final List<CaseItem> items = new ArrayList<>(0);
        while (!this.isReservedWord("esac")) {
            if (this.check(TokenType.LPAREN)) {
                this.consume();
            }
            final List<String> patterns = new ArrayList<>(0);
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
            items.add(new CaseItem(patterns, body));
        }
        this.expectWord("esac");
        return new CaseNode(word, items);
    }

    private IfNode parseIfCommand() {
        this.consume();
        final ListNode condition;
        condition = this.parseListUntilReserved("then");
        this.expectWord("then");
        final ListNode consequent;
        consequent = this.parseListUntilReserved("else", "elif", "fi");
        ListNode alternate = null;
        if (this.isReservedWord("else")) {
            this.consume();
            alternate = this.parseListUntilReserved("fi");
        } else if (this.isReservedWord("elif")) {
            alternate = this.parseElifChain();
        }
        this.expectWord("fi");
        return new IfNode(condition, consequent, alternate);
    }

    private ListNode parseElifChain() {
        this.consume();
        final ListNode cond;
        cond = this.parseListUntilReserved("then");
        this.expectWord("then");
        final ListNode body;
        body = this.parseListUntilReserved("else", "elif", "fi");
        ListNode trailing = null;
        if (this.isReservedWord("elif")) {
            trailing = this.parseElifChain();
        } else if (this.isReservedWord("else")) {
            this.consume();
            trailing = this.parseListUntilReserved("fi");
        }
        final IfNode chain;
        chain = new IfNode(cond, body, trailing);
        final Pipeline pipe = new Pipeline(false);
        pipe.addCommand(chain);
        final ListNode wrapper = new ListNode();
        wrapper.addPipeline(pipe, ListNode.Separator.NONE);
        return wrapper;
    }

    private ListNode parseListUntilReserved(final String... reserved) {
        final ListNode list = new ListNode();
        final Set<String> words = new HashSet<>(Arrays.asList(reserved));
        this.skipNewlines();
        if (!this.isReservedWord(reserved)) {
            boolean done = false;
            while (!done) {
                done = this.parseReservedEntry(list, words, reserved);
            }
        }
        return list;
    }

    private boolean parseReservedEntry(
        final ListNode list, final Set<String> words, final String... reserved
    ) {
        final Pipeline pipe;
        pipe = this.parsePipeline(words);
        list.addPipeline(pipe, ListNode.Separator.NONE);
        final boolean done;
        if (this.isPipelineSeparator()) {
            final ListNode.Separator sep;
            sep = this.pipelineSeparator();
            this.consume();
            this.skipNewlines();
            list.getSeparators().set(list.getPipelines().size() - 1, sep);
            done = this.isReservedWord(reserved);
        } else {
            done = true;
        }
        return done;
    }

    private boolean isPipelineSeparator() {
        return Parser.SEPARATORS.contains(this.current.type());
    }

    private ListNode.Separator pipelineSeparator() {
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
        return this.isInputRedirect() || this.isOutputRedirect();
    }

    private boolean isInputRedirect() {
        return this.check(TokenType.LESS) || this.check(TokenType.DLESS)
            || this.isInputExtended();
    }

    private boolean isInputExtended() {
        return this.check(TokenType.DLESSDASH) || this.check(TokenType.LESSAND)
            || this.check(TokenType.LESSGREATER);
    }

    private boolean isOutputRedirect() {
        return this.check(TokenType.GREAT) || this.check(TokenType.CLOBBER)
            || this.isOutputExtended();
    }

    private boolean isOutputExtended() {
        return this.check(TokenType.DGREATER) || this.check(TokenType.GREATAND)
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
            final String name;
            name = this.current.text();
            final int line;
            line = this.current.line();
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
            final String name;
            name = this.current.text();
            final int line;
            line = this.current.line();
            this.consume();
            final SimpleCommand named = new SimpleCommand(name);
            named.setLineNumber(line);
            named.getAssignments().addAll(cmd.getAssignments());
            cmd = named;
        }
        return cmd;
    }

    private void continueSimpleCommand(final SimpleCommand cmd) {
        int fdnum = -1;
        if (this.check(TokenType.IO_NUMBER)) {
            fdnum = Integer.parseInt(this.current.text());
            this.consume();
        }
        final boolean named = !cmd.getName().isEmpty();
        while (this.startsSimpleCommand()) {
            if (this.check(TokenType.ASSIGNMENT_WORD)) {
                if (named) {
                    cmd.addArgument(this.current.text());
                } else {
                    cmd.addAssignment(this.current.text());
                }
                this.consume();
            } else if (this.check(TokenType.IO_NUMBER)) {
                fdnum = Integer.parseInt(this.current.text());
                this.consume();
            } else if (this.isRedirectionStart()) {
                this.consumeRedirection(cmd, fdnum);
                fdnum = -1;
            } else if (this.check(TokenType.WORD)) {
                cmd.addArgument(this.current.text());
                this.consume();
            }
        }
    }

    private void consumeRedirection(final SimpleCommand cmd, final int fdnum) {
        if (this.check(TokenType.DLESS) || this.check(TokenType.DLESSDASH)) {
            final boolean strip;
            strip = this.current.type() == TokenType.DLESSDASH;
            this.consume();
            final String raw = this.expect(TokenType.WORD).text();
            final boolean quoted = isQuotedHereDocDelim(raw);
            final String delimiter;
            if (quoted) {
                delimiter = stripDelimQuotes(raw);
            } else {
                delimiter = raw;
            }
            final String content;
            content = this.lexer.readHereDocContent(delimiter, strip);
            final int desc;
            if (fdnum >= 0) {
                desc = fdnum;
            } else {
                desc = 0;
            }
            cmd.addRedirection(
                new Redirection(
                    desc, RedirectionType.HERE_DOC,
                    new Redirection.HereDoc(delimiter, content, quoted)
                )
            );
        } else {
            final RedirectionType type = this.redirectionTypeForCurrent();
            final int fallback = defaultFdFor(type);
            this.consume();
            final String target;
            target = this.expect(TokenType.WORD).text();
            final int desc;
            if (fdnum >= 0) {
                desc = fdnum;
            } else {
                desc = fallback;
            }
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
            && (Parser.isQuoted(word, '\'') || Parser.isQuoted(word, '"')
                || word.indexOf('\\') >= 0);
    }

    private static boolean isQuoted(final String word, final char quote) {
        return word.charAt(0) == quote && word.charAt(word.length() - 1) == quote;
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
