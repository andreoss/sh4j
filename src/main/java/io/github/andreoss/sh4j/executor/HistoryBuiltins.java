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
     * @param call Arguments, environment and inline assignments
     * @return The exit code: 0 on success, 1 on error
     */
    static int doFc(final BuiltinContext self, final Invocation call) {
        final FcOptions opts = new FcOptions(
            call.arguments(), call.assignments()
        ).parse();
        final int code;
        if (opts.error().isPresent()) {
            self.errStream().println(opts.error().get());
            code = 1;
        } else if (self.history().size() == 0) {
            self.errStream().println("sh: fc: history is empty");
            code = 1;
        } else {
            code = HistoryBuiltins.run(self, opts);
        }
        return code;
    }

    private static int run(final BuiltinContext self, final FcOptions opts) {
        final CommandHistory hist = self.history();
        final List<String> rest = opts.operands();
        final int first;
        if (rest.isEmpty()) {
            first = Math.max(1, hist.lastNumber() - 15);
        } else {
            first = HistoryBuiltins.parseFcNumber(rest.get(0));
        }
        final int last;
        if (rest.size() > 1) {
            last = HistoryBuiltins.parseFcNumber(rest.get(1));
        } else {
            last = hist.lastNumber();
        }
        final int code;
        if (opts.substitution().isPresent()) {
            code = HistoryBuiltins.fcSubstitute(
                self, opts.substitution().get(), hist.get(first), first
            );
        } else if (opts.listing()) {
            HistoryBuiltins.fcList(self, hist.list(first, last), opts.unnumbered());
            code = 0;
        } else {
            code = 0;
        }
        return code;
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

    private static void fcList(
        final BuiltinIo self, final List<HistoryEntry> entries,
        final boolean nonumbers
    ) {
        for (final HistoryEntry entry : entries) {
            if (nonumbers) {
                self.outStream().println(String.format("\t%s", entry.command()));
            } else {
                self.outStream().println(
                    String.format("%d\t%s", entry.number(), entry.command())
                );
            }
        }
    }

    private static int fcSubstitute(
        final BuiltinContext self, final String pattern,
        final String cmd, final int number
    ) {
        final int eqidx = pattern.indexOf('=');
        final int code;
        if (eqidx < 0) {
            self.errStream().println("sh: fc: invalid substitution pattern");
            code = 1;
        } else if (cmd == null) {
            self.errStream().println(
                String.format("sh: fc: %d: no such history entry", number)
            );
            code = 1;
        } else {
            final String replaced = cmd.replace(
                pattern.substring(0, eqidx), pattern.substring(eqidx + 1)
            );
            self.outStream().println(replaced);
            code = self.execute(
                new Parser(new Lexer(new CharReader(replaced))).parse()
            );
        }
        return code;
    }
}
