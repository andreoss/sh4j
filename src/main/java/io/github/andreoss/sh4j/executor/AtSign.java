/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.Nesting;
import io.github.andreoss.sh4j.lexer.QuoteState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rewrites a word holding a double-quoted {@code $@} or {@code ${@}} into
 * one word per positional parameter (POSIX 2.5.2): the text before the
 * expansion joins the first parameter and the text after it joins the
 * last one.
 *
 * @since 1.0
 */
final class AtSign {

    /** The raw word. */
    private final String word;

    /**
     * Ctor.
     *
     * @param word The raw word
     */
    AtSign(final String word) {
        this.word = word;
    }

    /**
     * Rewrites the word for the given positional parameters.
     *
     * @param params The positional parameters, without {@code $0}
     * @return The raw words, with each parameter double-quoted; the word
     *  itself when it holds no double-quoted {@code $@}
     */
    List<String> words(final List<String> params) {
        final String text = WordExpander.normaliseBackticks(this.word);
        final int start = AtSign.start(text);
        final List<String> result;
        if (start < 0) {
            result = Collections.singletonList(this.word);
        } else {
            int end = start + 2;
            if (text.charAt(start + 1) == '{') {
                end = start + 4;
            }
            result = AtSign.spread(
                text.substring(0, start), text.substring(end), params
            );
        }
        return result;
    }

    private static List<String> spread(
        final String prefix,
        final String suffix,
        final List<String> params
    ) {
        final List<String> result = new ArrayList<>(params.size());
        if (params.isEmpty()) {
            result.addAll(AtSign.none(prefix + suffix));
        } else {
            String head = prefix;
            final int last = params.size() - 1;
            for (int idx = 0; idx < last; idx = idx + 1) {
                result.add(
                    String.join("", head, AtSign.escape(params.get(idx)), "\"")
                );
                head = "\"";
            }
            result.addAll(
                new AtSign(
                    String.join("", head, AtSign.escape(params.get(last)), suffix)
                ).words(params)
            );
        }
        return result;
    }

    private static List<String> none(final String rest) {
        final List<String> result;
        if ("\"\"".equals(rest)) {
            result = Collections.emptyList();
        } else {
            result = new AtSign(rest).words(Collections.emptyList());
        }
        return result;
    }

    private static int start(final String text) {
        final QuoteState quotes = new QuoteState();
        int pos = 0;
        int result = -1;
        while (pos < text.length() && result < 0) {
            final char chr = text.charAt(pos);
            if (Quotes.literal(quotes, chr)) {
                pos = pos + 1;
            } else if (chr == '\\') {
                pos = pos + 2;
            } else if (chr == '$' && quotes.doubled() && AtSign.at(text, pos)) {
                result = pos;
            } else if (text.startsWith("$(", pos)) {
                pos = Math.max(new Nesting(text).paren(pos + 2), pos + 1);
            } else if (text.startsWith("${", pos)) {
                pos = Math.max(new Nesting(text).brace(pos + 2, quotes.doubled()), pos + 1);
            } else {
                pos = pos + 1;
            }
        }
        return result;
    }

    private static boolean at(final String text, final int pos) {
        return text.startsWith("$@", pos) || text.startsWith("${@}", pos);
    }

    private static String escape(final String param) {
        final StringBuilder out = new StringBuilder(param.length());
        for (int idx = 0; idx < param.length(); idx = idx + 1) {
            final char chr = param.charAt(idx);
            if ("\\\"$`".indexOf(chr) >= 0) {
                out.append('\\');
            }
            out.append(chr);
        }
        return out.toString();
    }
}
