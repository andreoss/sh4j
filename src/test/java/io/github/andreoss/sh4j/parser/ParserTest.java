/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;

class ParserTest {
    private Parser parser(String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    @Test
    void parseSimpleCommand() {
        ListNode list = parser("echo hello").parse();
        assertEquals(1, list.getPipelines().size());
        
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        
        SimpleCommand cmd = pipeline.getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertEquals(1, cmd.getArguments().size());
        assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    void parsePipeline() {
        ListNode list = parser("echo a | grep b").parse();
        assertEquals(1, list.getPipelines().size());
        
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertEquals("echo", pipeline.getCommands().get(0).getName());
        assertEquals("grep", pipeline.getCommands().get(1).getName());
    }

    @Test
    void parseListWithSemicolon() {
        ListNode list = parser("cmd1 ; cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAnd() {
        ListNode list = parser("cmd1 && cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithOr() {
        ListNode list = parser("cmd1 || cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAmp() {
        ListNode list = parser("cmd1 & cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    void parseBang() {
        ListNode list = parser("! echo hello").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertTrue(pipeline.isBang());
    }

    @Test
    void parseCommandWithNoArguments() {
        ListNode list = parser("ls").parse();
        SimpleCommand cmd = list.getPipelines().get(0).getCommands().get(0);
        assertEquals("ls", cmd.getName());
        assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void parseInputRedirection() {
        ListNode list = parser("cat < file.txt").parse();
        SimpleCommand cmd = list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(0, cmd.getRedirections().get(0).getFd());
        assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirection() {
        ListNode list = parser("echo hello > out.txt").parse();
        SimpleCommand cmd = list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(1, cmd.getRedirections().get(0).getFd());
        assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
        assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirection() {
        ListNode list = parser("echo hello >> out.txt").parse();
        SimpleCommand cmd = list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
        assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirection() {
        ListNode list = parser("echo test >| file.txt").parse();
        SimpleCommand cmd = list.getPipelines().get(0).getCommands().get(0);
        assertTrue(cmd.getRedirections().size() >= 1);
        boolean hasClobber = cmd.getRedirections().stream()
            .anyMatch(r -> r.getType() == RedirectionType.CLOBBER);
        assertTrue(hasClobber);
    }

    @Test
    void parseCommandWithDollar() {
        ListNode list = parser("echo $VAR").parse();
        SimpleCommand cmd = list.getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertEquals("$VAR", cmd.getArguments().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparators() {
        ListNode list = parser("cmd1; cmd2 & cmd3 || cmd4 && cmd5").parse();
        assertEquals(5, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(1));
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(3));
    }

    @Test
    void parseEmptyPipeline() {
        ListNode list = parser("").parse();
        assertEquals(1, list.getPipelines().size());
    }
}
