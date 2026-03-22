/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.Parser;
import java.util.List;
import java.util.Map;

/**
 * The command-history built-in.
 *
 * @since 1.0
 */
final class HistoryBuiltins {

    @GeneratedDefensive
    private HistoryBuiltins() {
    }

    /**
     * Implementation of {@code fc [-l] [-n] [-s pat=rep] [first [last]]}.
     * POSIX 2.14: lists or re-executes history entries.
     * {@code -l} lists entries (default), {@code -n} suppresses numbers,
     * {@code -s pat=rep} substitutes and re-executes the last command.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @param assigns The assignments
     * @return The exit code: 0 on success, 1 on error
     */
    static int doFc(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean listmode = true;
        boolean nonumbers = false;
        String substitute = null;
        int idx = 0;
        while (idx < args.size() && args.get(idx).startsWith("-")
            && !args.get(idx).matches("-\\d+")) {
            final String flag = args.get(idx);
            for (int fidx = 1; fidx < flag.length(); fidx = fidx + 1) {
                final char chr = flag.charAt(fidx);
                if (chr == 'l') {
                    listmode = true;
                } else if (chr == 'n') {
                    nonumbers = true;
                } else if (chr == 'e') {
                    idx = idx + 1;
                    if (idx >= args.size()) {
                        self.errStream().println("sh: fc: -e requires argument");
                        return 1;
                    }
                } else if (chr == 's') {
                    listmode = false;
                    if (idx + 1 < args.size() && args.get(idx + 1).contains("=")) {
                        substitute = args.get(idx + 1);
                        idx = idx + 1;
                    } else if (!assigns.isEmpty()) {
                        substitute = assigns.get(0);
                    } else {
                        self.errStream().println("sh: fc: -s requires pat=rep");
                        return 1;
                    }
                    fidx = flag.length();
                } else {
                    self.errStream().println(String.format("sh: fc: unknown option -%s", chr));
                    return 1;
                }
            }
            idx = idx + 1;
        }
        final CommandHistory hist = self.history();
        if (hist.size() == 0) {
            self.errStream().println("sh: fc: history is empty");
            return 1;
        }
        int first;
        if (idx < args.size()) {
            first = parseFcNumber(args.get(idx));
            idx = idx + 1;
        } else {
            first = hist.lastNumber() - 15;
            if (first < 1) {
                first = 1;
            }
        }
        final int last;
        if (idx < args.size()) {
            last = parseFcNumber(args.get(idx));
        } else {
            last = hist.lastNumber();
        }
        if (substitute != null) {
            return fcSubstitute(self, hist, substitute, first);
        }
        if (listmode) {
            return fcList(self, hist, first, last, nonumbers);
        }
        return 0;
    }

    private static int parseFcNumber(final String arg) {
        final int parsed;
        if (arg.matches("-?\\d+")) {
            parsed = Integer.parseInt(arg);
        } else {
            parsed = -1;
        }
        return parsed;
    }

    private static int fcList(
        final BuiltinContext self, final CommandHistory hist,
        final int first, final int last, final boolean nonumbers
    ) {
        final List<HistoryEntry> entries = hist.list(first, last);
        for (final HistoryEntry entry : entries) {
            if (nonumbers) {
                self.outStream().println(String.format("\t%s", entry.command()));
            } else {
                self.outStream().println(
                    String.format("%d\t%s", entry.number(), entry.command())
                );
            }
        }
        return 0;
    }

    private static int fcSubstitute(
        final BuiltinContext self, final CommandHistory hist,
        final String pattern, final int number
    ) {
        final int eqidx = pattern.indexOf('=');
        if (eqidx < 0) {
            self.errStream().println("sh: fc: invalid substitution pattern");
            return 1;
        }
        final String oldstr = pattern.substring(0, eqidx);
        final String newstr = pattern.substring(eqidx + 1);
        final String cmd = hist.get(number);
        if (cmd == null) {
            self.errStream().println(String.format("sh: fc: %d: no such history entry", number));
            return 1;
        }
        final String replaced = cmd.replace(oldstr, newstr);
        self.outStream().println(replaced);
        return self.execute(
            new Parser(new Lexer(new CharReader(replaced))).parse()
        );
    }
}
