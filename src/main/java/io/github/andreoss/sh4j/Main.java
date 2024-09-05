/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import java.util.ArrayList;
import java.util.List;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

public class Main {
    public static void main(String[] args) {
        System.exit(exitCode(args));
    }

    static int exitCode(String[] args) {
        if (args.length > 0) {
            List<String> scriptArgs = new ArrayList<>();
            scriptArgs.add(args[0]);
            for (int i = 1; i < args.length; i++) {
                scriptArgs.add(args[i]);
            }
            return runScript(args[0], scriptArgs);
        }
        return interactive();
    }

    static int runScript(String filename, List<String> scriptArgs) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } catch (IOException e) {
            return 1;
        }
        CharReader charReader = new CharReader(content.toString());
        Lexer lexer = new Lexer(charReader);
        Parser parser = new Parser(lexer);
        ListNode ast = parser.parse();
        Executor executor = new Executor(System.getenv(), scriptArgs);
        return executor.execute(ast);
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
