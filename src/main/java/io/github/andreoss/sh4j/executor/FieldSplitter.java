/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles IFS field splitting (POSIX 2.6.5).
 *
 * @since 1.0
 */
public final class FieldSplitter {

    private FieldSplitter() {
    }

    /**
     * Splits a value into fields according to IFS.
     *
     * @param value The post-expansion text
     * @param ifs   The IFS value (each character is a separator)
     * @return One field per non-IFS run
     */
    public static List<String> splitFields(final String value, final String ifs) {
        final List<String> result;
        if (ifs == null || ifs.isEmpty()) {
            result = Collections.singletonList(value);
        } else {
            final String wschars;
            final String nwschars;
            final StringBuilder wsbuf = new StringBuilder();
            final StringBuilder nwsbuf = new StringBuilder();
            for (int idx = 0; idx < ifs.length(); idx = idx + 1) {
                final char chr = ifs.charAt(idx);
                if (chr == ' ' || chr == '\t' || chr == '\n') {
                    wsbuf.append(chr);
                } else {
                    nwsbuf.append(chr);
                }
            }
            wschars = wsbuf.toString();
            nwschars = nwsbuf.toString();
            result = splitByIfs(value, wschars, nwschars);
        }
        return result;
    }

    public static List<String> splitByIfs(
        final String value,
        final String wschars,
        final String nwschars
    ) {
        final List<String> result = new ArrayList<>(0);
        StringBuilder field = new StringBuilder();
        boolean infield = false;
        for (int idx = 0; idx < value.length(); idx = idx + 1) {
            final char chr = value.charAt(idx);
            if (nwschars.indexOf(chr) >= 0) {
                result.add(field.toString());
                field = new StringBuilder();
                infield = false;
            } else if (wschars.indexOf(chr) >= 0) {
                if (infield) {
                    result.add(field.toString());
                    field = new StringBuilder();
                    infield = false;
                }
            } else {
                field.append(chr);
                infield = true;
            }
        }
        if (infield) {
            result.add(field.toString());
        } else if (!nwschars.isEmpty() && !value.isEmpty()
            && nwschars.indexOf(value.charAt(value.length() - 1)) >= 0) {
            result.add(field.toString());
        }
        return Collections.unmodifiableList(result);
    }
}
