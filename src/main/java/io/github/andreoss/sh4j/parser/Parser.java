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
        throw new RuntimeException("Expected " + type + " but got " + current.type());
    }

    private boolean check(TokenType type) {
        return current.type() == type;
    }

    public ListNode parse() {
        return parseList();
    }

    private ListNode parseList() {
        ListNode list = new ListNode();
        
        Pipeline pipeline = parsePipeline();
        list.addPipeline(pipeline, ListNode.Separator.NONE);

        while (check(TokenType.SEMI) || check(TokenType.AMP) 
                || check(TokenType.AND_IF) || check(TokenType.OR_IF)) {
            ListNode.Separator sep = switch (current.type()) {
                case SEMI -> ListNode.Separator.SEMI;
                case AMP -> ListNode.Separator.AMP;
                case AND_IF -> ListNode.Separator.AND_IF;
                case OR_IF -> ListNode.Separator.OR_IF;
                default -> ListNode.Separator.NONE;
            };
            consume();
            
            int lastIdx = list.getPipelines().size() - 1;
            list.getSeparators().set(lastIdx, sep);
            
            pipeline = parsePipeline();
            list.addPipeline(pipeline, ListNode.Separator.NONE);
        }

        return list;
    }

    private Pipeline parsePipeline() {
        boolean bang = false;
        if (check(TokenType.WORD) && "!".equals(current.text())) {
            bang = true;
            consume();
        }
        Pipeline pipeline = new Pipeline(bang);
        SimpleCommand cmd = parseSimpleCommand();
        pipeline.addCommand(cmd);

        while (check(TokenType.PIPE)) {
            consume();
            cmd = parseSimpleCommand();
            pipeline.addCommand(cmd);
        }

        return pipeline;
    }

    private SimpleCommand parseSimpleCommand() {
        SimpleCommand cmd = null;
        if (check(TokenType.WORD) || check(TokenType.ASSIGNMENT_WORD)) {
            if (check(TokenType.ASSIGNMENT_WORD)) {
                cmd = new SimpleCommand("");
                while (check(TokenType.ASSIGNMENT_WORD)) {
                    cmd.addAssignment(current.text());
                    consume();
                }
                if (check(TokenType.WORD)) {
                    cmd.setName(current.text());
                    consume();
                }
            } else {
                String name = current.text();
                consume();
                cmd = new SimpleCommand(name);
            }

            while (check(TokenType.WORD) || check(TokenType.ASSIGNMENT_WORD) || check(TokenType.LESS) 
                    || check(TokenType.GREAT) || check(TokenType.CLOBBER) || check(TokenType.DGREATER)
                    || check(TokenType.IO_NUMBER)) {
                if (check(TokenType.ASSIGNMENT_WORD)) {
                    cmd.addAssignment(current.text());
                    consume();
                } else if (check(TokenType.IO_NUMBER)) {
                    consume();
                } else if (check(TokenType.LESS)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    cmd.addRedirection(Redirection.input(0, target));
                } else if (check(TokenType.GREAT)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    cmd.addRedirection(Redirection.output(1, target));
                } else if (check(TokenType.CLOBBER)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    cmd.addRedirection(new Redirection(1, RedirectionType.CLOBBER, target));
                } else if (check(TokenType.DGREATER)) {
                    consume();
                    String target = expect(TokenType.WORD).text();
                    cmd.addRedirection(Redirection.append(1, target));
                } else if (check(TokenType.WORD)) {
                    cmd.addArgument(current.text());
                    consume();
                }
            }
        }
        return cmd;
    }
}
