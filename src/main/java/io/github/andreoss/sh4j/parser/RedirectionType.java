/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

public enum RedirectionType {
    INPUT,
    OUTPUT,
    APPEND,
    CLOBBER,
    READWRITE,
    HERE_DOC,
    HERE_DOC_DASH
}
