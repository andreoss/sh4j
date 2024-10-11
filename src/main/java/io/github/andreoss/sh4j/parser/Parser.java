/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.Token;
import io.github.andreoss.sh4j.lexer.TokenType;

public class Parser {
    private final Lexer lexer;
    private Token current;
    private Token lookahead;
    private Token lookahead2;
    private boolean hasLookahead2;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
        this.lookahead = null;
        this.lookahead2 = null;
        this.hasLookahead2 = false;
    }

    private Token consume() {
        Token t = current;
        if (lookahead != null) {
            current = lookahead;
            lookahead = hasLookahead2 ? lookahead2 : null;
            hasLookahead2 = false;
        } else {
            current = lexer.nextToken();
        }
        return t;
    }

    private Token peek() {
        if (lookahead == null) {
            lookahead = lexer.nextToken();
            hasLookahead2 = false;
        }
        return lookahead;
    }

    private Token peek2() {
        peek();
        if (!hasLookahead2) {
            lookahead2 = lexer.nextToken();
            hasLookahead2 = true;
        }
        return lookahead2;
    }

    private Token expect(TokenType type) {
        if (current.type() == type) {
            return consume();
        }
        throw new ParseException("Expected " + type + " but got " + current.type()
                + " at line " + current.line() + " column " + current.column());
    }

    private boolean check(TokenType type) {
        return current.type() == type;
    }

    public ListNode parse() {
        return parseList(TokenType.END_OF_FILE);
    }

    private ListNode parseList(TokenType... terminators) {
        ListNode list = new ListNode();

        Pipeline pipeline = parsePipeline();
        list.addPipeline(pipeline, ListNode.Separator.NONE);

        while (check(TokenType.SEMI) || check(TokenType.AMP)
                || check(TokenType.AND_IF) || check(TokenType.OR_IF)) {
            ListNode.Separator sep;
            if (check(TokenType.SEMI)) {
                sep = ListNode.Separator.SEMI;
            } else if (check(TokenType.AMP)) {
                sep = ListNode.Separator.AMP;
            } else if (check(TokenType.AND_IF)) {
                sep = ListNode.Separator.AND_IF;
            } else {
                sep = ListNode.Separator.OR_IF;
            }
            consume();
            
            int lastIdx = list.getPipelines().size() - 1;
            list.getSeparators().set(lastIdx, sep);

            if (isTerminator(terminators)) {
                break;
            }

            pipeline = parsePipeline();
            list.addPipeline(pipeline, ListNode.Separator.NONE);
        }

        return list;
    }

    private boolean isTerminator(TokenType... terminators) {
        for (TokenType t : terminators) {
            if (check(t)) return true;
        }
        return false;
    }

    private boolean isReservedWord(String... words) {
        if (!check(TokenType.WORD)) return false;
        for (String w : words) {
            if (current.text().equals(w)) return true;
        }
        return false;
    }

    private Token expectWord(String text) {
        if (check(TokenType.WORD) && current.text().equals(text)) {
            return consume();
        }
        throw new ParseException("Expected '" + text + "' but got " + current.text()
                + " at line " + current.line() + " column " + current.column());
    }

    private Pipeline parsePipeline() {
        return parsePipeline(Set.of());
    }

    private Pipeline parsePipeline(Set<String> reservedTerminators) {
        boolean bang = false;
        if (check(TokenType.WORD) && "!".equals(current.text())) {
            bang = true;
            consume();
        }
        Pipeline pipeline = new Pipeline(bang);

        if (check(TokenType.LPAREN)) {
            consume();
            ListNode inner = parseList(TokenType.RPAREN);
            expect(TokenType.RPAREN);
            pipeline.addCommand(new Subshell(inner));
        } else if (check(TokenType.LBRACE)) {
            consume();
            ListNode inner = parseList(TokenType.RBRACE);
            expect(TokenType.RBRACE);
            pipeline.addCommand(new Grouping(inner));
        } else if (check(TokenType.WORD) && "if".equals(current.text())) {
            pipeline.addCommand(parseIfCommand());
        } else if (check(TokenType.WORD) && "for".equals(current.text())) {
            pipeline.addCommand(parseForCommand());
        } else if (check(TokenType.WORD) && ("while".equals(current.text()) || "until".equals(current.text()))) {
            pipeline.addCommand(parseWhileCommand());
        } else if (check(TokenType.WORD) && "case".equals(current.text())) {
            pipeline.addCommand(parseCaseCommand());
        } else if (check(TokenType.WORD) && reservedTerminators.contains(current.text())) {
            return pipeline;
        } else if (check(TokenType.WORD) && peek().type() == TokenType.LPAREN
                && peek2().type() == TokenType.RPAREN) {
            pipeline.addCommand(parseFunctionDefinition());
        } else {
            SimpleCommand cmd = parseSimpleCommand();
            pipeline.addCommand(cmd);
        }

        while (check(TokenType.PIPE)) {
            consume();
            if (check(TokenType.LPAREN)) {
                consume();
                ListNode inner = parseList(TokenType.RPAREN);
                expect(TokenType.RPAREN);
                pipeline.addCommand(new Subshell(inner));
            } else if (check(TokenType.LBRACE)) {
                consume();
                ListNode inner = parseList(TokenType.RBRACE);
                expect(TokenType.RBRACE);
                pipeline.addCommand(new Grouping(inner));
            } else if (check(TokenType.WORD) && "if".equals(current.text())) {
                pipeline.addCommand(parseIfCommand());
            } else if (check(TokenType.WORD) && "for".equals(current.text())) {
                pipeline.addCommand(parseForCommand());
            } else if (check(TokenType.WORD) && ("while".equals(current.text()) || "until".equals(current.text()))) {
                pipeline.addCommand(parseWhileCommand());
            } else if (check(TokenType.WORD) && "case".equals(current.text())) {
                pipeline.addCommand(parseCaseCommand());
            } else if (check(TokenType.WORD) && reservedTerminators.contains(current.text())) {
                break;
            } else if (check(TokenType.WORD) && peek().type() == TokenType.LPAREN
                    && peek2().type() == TokenType.RPAREN) {
                pipeline.addCommand(parseFunctionDefinition());
            } else {
                SimpleCommand cmd = parseSimpleCommand();
                pipeline.addCommand(cmd);
            }
        }

        return pipeline;
    }

    private WhileNode parseWhileCommand() {
        boolean until = "until".equals(current.text());
        consume();
        ListNode condition = parseListUntilReserved("do");
        if (check(TokenType.SEMI)) {
            consume();
        }
        expectWord("do");
        ListNode body = parseListUntilReserved("done");
        expectWord("done");
        return new WhileNode(condition, body, until);
    }

    private ForNode parseForCommand() {
        consume(); // "for"
        String varName = expect(TokenType.WORD).text();
        List<String> words = new ArrayList<>();
        expectWord("in");
        while (check(TokenType.WORD) && !isReservedWord("do")) {
            words.add(current.text());
            consume();
        }
        if (check(TokenType.SEMI)) {
            consume();
        }
        expectWord("do");
        ListNode body = parseListUntilReserved("done");
        expectWord("done");
        return new ForNode(varName, words, body);
    }

    private FunctionNode parseFunctionDefinition() {
        String name = consume().text();
        consume(); // LPAREN
        consume(); // RPAREN
        Node body = parseCompoundCommandBody();
        return new FunctionNode(name, body);
    }

    private Node parseCompoundCommandBody() {
        if (check(TokenType.LPAREN)) {
            consume();
            ListNode inner = parseList(TokenType.RPAREN);
            expect(TokenType.RPAREN);
            return new Subshell(inner);
        } else if (check(TokenType.LBRACE)) {
            consume();
            ListNode inner = parseList(TokenType.RBRACE);
            expect(TokenType.RBRACE);
            return new Grouping(inner);
        } else if (isReservedWord("if")) {
            return parseIfCommand();
        } else if (isReservedWord("for")) {
            return parseForCommand();
        } else if (isReservedWord("while", "until")) {
            return parseWhileCommand();
        } else if (isReservedWord("case")) {
            return parseCaseCommand();
        }
        throw new ParseException("Expected compound command after function definition"
                + " at line " + current.line() + " column " + current.column());
    }

    private CaseNode parseCaseCommand() {
        consume(); // "case"
        String word = expect(TokenType.WORD).text();
        expectWord("in");
        List<CaseNode.CaseItem> items = new ArrayList<>();
        while (!isReservedWord("esac")) {
            if (check(TokenType.LPAREN)) {
                consume();
            }
            List<String> patterns = new ArrayList<>();
            patterns.add(expect(TokenType.WORD).text());
            while (check(TokenType.PIPE)) {
                consume();
                patterns.add(expect(TokenType.WORD).text());
            }
            expect(TokenType.RPAREN);
            ListNode body;
            if (check(TokenType.DSEMI)) {
                body = new ListNode();
            } else {
                body = parseList(TokenType.DSEMI);
            }
            expect(TokenType.DSEMI);
            items.add(new CaseNode.CaseItem(patterns, body));
        }
        expectWord("esac");
        return new CaseNode(word, items);
    }

    private IfNode parseIfCommand() {
        consume(); // "if"
        ListNode condition = parseListUntilReserved("then");
        expectWord("then");
        ListNode thenBody = parseListUntilReserved("else", "elif", "fi");
        ListNode elseBody = null;
        if (isReservedWord("else")) {
            consume();
            elseBody = parseListUntilReserved("fi");
        } else if (isReservedWord("elif")) {
            elseBody = parseElifChain();
        }
        expectWord("fi");
        return new IfNode(condition, thenBody, elseBody);
    }

    private ListNode parseElifChain() {
        consume(); // "elif"
        ListNode cond = parseListUntilReserved("then");
        expectWord("then");
        ListNode body = parseListUntilReserved("else", "elif", "fi");
        ListNode trailing = null;
        if (isReservedWord("elif")) {
            trailing = parseElifChain();
        } else if (isReservedWord("else")) {
            consume();
            trailing = parseListUntilReserved("fi");
        }
        IfNode elifNode = new IfNode(cond, body, trailing);
        Pipeline p = new Pipeline(false);
        p.addCommand(elifNode);
        ListNode wrapper = new ListNode();
        wrapper.addPipeline(p, ListNode.Separator.NONE);
        return wrapper;
    }

    private ListNode parseListUntilReserved(String... reservedWordTerminators) {
        ListNode list = new ListNode();
        Set<String> reserved = Set.of(reservedWordTerminators);

        if (isReservedWord(reservedWordTerminators)) {
            return list;
        }

        while (true) {
            Pipeline p = parsePipeline(reserved);
            list.addPipeline(p, ListNode.Separator.NONE);

            if (check(TokenType.SEMI) || check(TokenType.AMP)
                    || check(TokenType.AND_IF) || check(TokenType.OR_IF)) {
                ListNode.Separator sep;
                if (check(TokenType.SEMI)) {
                    sep = ListNode.Separator.SEMI;
                } else if (check(TokenType.AMP)) {
                    sep = ListNode.Separator.AMP;
                } else if (check(TokenType.AND_IF)) {
                    sep = ListNode.Separator.AND_IF;
                } else {
                    sep = ListNode.Separator.OR_IF;
                }
                consume();
                int lastIdx = list.getPipelines().size() - 1;
                list.getSeparators().set(lastIdx, sep);

                if (isReservedWord(reservedWordTerminators)) {
                    break;
                }
            } else {
                break;
            }
        }

        return list;
    }

    private SimpleCommand parseSimpleCommand() {
        SimpleCommand cmd = null;
        int pendingFd = -1;
        if (check(TokenType.WORD) || check(TokenType.ASSIGNMENT_WORD) 
                || check(TokenType.LESS) || check(TokenType.GREAT) 
                || check(TokenType.CLOBBER) || check(TokenType.DGREATER)
                || check(TokenType.DLESS) || check(TokenType.DLESSDASH)
                || check(TokenType.LESSAND) || check(TokenType.GREATAND)
                || check(TokenType.LESSGREATER)
                || check(TokenType.IO_NUMBER)) {
             if (check(TokenType.ASSIGNMENT_WORD)) {
                 cmd = new SimpleCommand("");
                 while (check(TokenType.ASSIGNMENT_WORD)) {
                     cmd.addAssignment(current.text());
                     consume();
                 }
                 if (check(TokenType.WORD)) {
                     SimpleCommand namedCmd = new SimpleCommand(current.text());
                     namedCmd.getAssignments().addAll(cmd.getAssignments());
                     cmd = namedCmd;
                     consume();
                 }
             } else if (check(TokenType.IO_NUMBER)) {
                 pendingFd = Integer.parseInt(current.text());
                 consume();
                 cmd = new SimpleCommand("");
             } else {
                 String name = current.text();
                 consume();
                 cmd = new SimpleCommand(name);
             }

            while (check(TokenType.WORD) || check(TokenType.ASSIGNMENT_WORD) || check(TokenType.LESS) 
                    || check(TokenType.GREAT) || check(TokenType.CLOBBER) || check(TokenType.DGREATER)
                    || check(TokenType.DLESS) || check(TokenType.DLESSDASH)
                    || check(TokenType.LESSAND) || check(TokenType.GREATAND)
                    || check(TokenType.LESSGREATER)
                    || check(TokenType.IO_NUMBER)) {
                if (check(TokenType.ASSIGNMENT_WORD)) {
                    cmd.addAssignment(current.text());
                    consume();
                } else if (check(TokenType.IO_NUMBER)) {
                    pendingFd = Integer.parseInt(current.text());
                    consume();
                } else if (check(TokenType.DLESS) || check(TokenType.DLESSDASH)) {
                    boolean stripTabs = current.type() == TokenType.DLESSDASH;
                    consume();
                    String delimiter = expect(TokenType.WORD).text();
                    String content = lexer.readHereDocContent(delimiter, stripTabs);
                    int fd = pendingFd >= 0 ? pendingFd : 0;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.HERE_DOC, delimiter, content));
                    pendingFd = -1;
                } else if (check(TokenType.LESSAND)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 0;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.DUP_INPUT, target));
                    pendingFd = -1;
                } else if (check(TokenType.LESSGREATER)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 0;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.READWRITE, target));
                    pendingFd = -1;
                } else if (check(TokenType.LESS)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 0;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.INPUT, target));
                    pendingFd = -1;
                } else if (check(TokenType.GREATAND)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 1;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.DUP_OUTPUT, target));
                    pendingFd = -1;
                } else if (check(TokenType.GREAT)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 1;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.OUTPUT, target));
                    pendingFd = -1;
                } else if (check(TokenType.CLOBBER)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 1;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.CLOBBER, target));
                    pendingFd = -1;
                } else if (check(TokenType.DGREATER)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    int fd = pendingFd >= 0 ? pendingFd : 1;
                    cmd.addRedirection(new Redirection(fd, RedirectionType.APPEND, target));
                    pendingFd = -1;
                } else if (check(TokenType.WORD)) {
                    cmd.addArgument(current.text());
                    consume();
                }
            }
            return cmd;
        } else {
            return new SimpleCommand("");
        }
    }
}
