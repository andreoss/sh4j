/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

class ExecutorTest {
    private Parser parser(String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    @Test
    void executeSimpleEcho() {
        ListNode list = parser("echo hello").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executePwd() {
        ListNode list = parser("pwd").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeTrue() {
        ListNode list = parser("true").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeFalse() {
        ListNode list = parser("false").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(1, exitCode);
    }

    @Test
    void executeWithArguments() {
        ListNode list = parser("echo a b c").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeSemicolonSeparator() {
        ListNode list = parser("echo a; echo b").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeAndIf() {
        ListNode list = parser("echo a && echo b").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeOrIf() {
        ListNode list = parser("false || echo b").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeAndIfStopsOnFailure() {
        ListNode list = parser("false && echo not reached").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(1, exitCode);
    }

    @Test
    void executeOrIfStopsOnSuccess() {
        ListNode list = parser("echo success || echo not reached").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeEmptyList() {
        ListNode list = parser("").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }
}
