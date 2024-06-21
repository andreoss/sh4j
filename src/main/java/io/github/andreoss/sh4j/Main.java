/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

public class Main {
    public static void main(String[] args) {
        System.exit(exitCode(args));
    }

    static int exitCode(String[] args) {
        if (args.length > 0) {
            return runScript(args[0]);
        }
        return interactive();
    }

    static int runScript(String filename) {
        return 0;
    }

    static int interactive() {
        Executor executor = new Executor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("$ ");
            try {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                Lexer lexer = new Lexer(new CharReader(line));
                Parser parser = new Parser(lexer);
                ListNode ast = parser.parse();
                executor.execute(ast);
            } catch (IOException e) {
                break;
            }
        }
        return 0;
    }
}
