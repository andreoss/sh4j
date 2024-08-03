/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.lexer.Token;
import io.github.andreoss.sh4j.lexer.TokenType;

public class Parser {
    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    private Token consume() {
        Token t = current;
        current = lexer.nextToken();
        return t;
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

    private Pipeline parsePipeline() {
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
            } else {
                SimpleCommand cmd = parseSimpleCommand();
                pipeline.addCommand(cmd);
            }
        }

        return pipeline;
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
