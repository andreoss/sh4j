/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * The kinds of I/O redirection the executor recognises, mirroring the
 * operators in POSIX 2.7.
 * @since 1.0
 */
public enum RedirectionType {

    /** Read from a file ({@code <}). */
    INPUT,
    /** Write to a file, truncating ({@code >}). */
    OUTPUT,
    /** Append to a file ({@code >>}). */
    APPEND,
    /** Write to a file, even with no-clobber set ({@code >|}). */
    CLOBBER,
    /** Open a file for read and write ({@code <>}). */
    READWRITE,
    /** Inline here-document content ({@code <<}). */
    HERE_DOC,
    /** Tab-stripping inline here-document ({@code <<-}). */
    HERE_DOC_DASH,
    /** Duplicate an input file descriptor ({@code <&}). */
    DUP_INPUT,
    /** Duplicate an output file descriptor ({@code >&}). */
    DUP_OUTPUT
}
