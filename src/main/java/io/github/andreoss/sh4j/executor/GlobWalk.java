/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Walks a pathname pattern one slash-separated component at a time
 * (POSIX 2.13.3). A component with a pattern character is matched against
 * the entries of the directory reached so far; any other component is taken
 * literally. Empty components keep leading, repeated and trailing slashes.
 *
 * @since 1.0
 */
final class GlobWalk {

    /** The pattern components, in escaped pattern form. */
    private final List<String> parts;

    /** The directory relative paths start from. */
    private final File cwd;

    /**
     * Ctor.
     *
     * @param parts The pattern split at every slash, empty parts included
     * @param cwd The directory relative paths start from
     */
    GlobWalk(final List<String> parts, final File cwd) {
        this.parts = parts;
        this.cwd = cwd;
    }

    /**
     * Collects the existing pathnames the pattern matches.
     *
     * @return The matching pathnames, unsorted
     */
    List<String> matches() {
        return this.walk(0, "");
    }

    private List<String> walk(final int index, final String path) {
        final List<String> out;
        if (index == this.parts.size()) {
            out = this.leaf(path);
        } else {
            final GlobPattern glob = new GlobPattern(this.parts.get(index));
            if (glob.special()) {
                out = this.expand(index, path, glob);
            } else {
                out = this.walk(index + 1, GlobWalk.join(index, path, glob.literal()));
            }
        }
        return out;
    }

    private List<String> leaf(final String path) {
        final List<String> out = new ArrayList<>(1);
        if (this.exists(path)) {
            out.add(path);
        }
        return out;
    }

    private List<String> expand(final int index, final String path, final GlobPattern glob) {
        final String part = this.parts.get(index);
        final List<String> out = new ArrayList<>(0);
        for (final String name : this.names(index, path)) {
            if (GlobWalk.visible(part, name) && glob.matches(name)) {
                out.addAll(this.walk(index + 1, GlobWalk.join(index, path, name)));
            }
        }
        return out;
    }

    private String[] names(final int index, final String path) {
        final File dir;
        if (index == 0) {
            dir = this.cwd;
        } else {
            dir = this.file(String.format("%s/", path));
        }
        final String[] listed = dir.list();
        final String[] result;
        if (listed == null) {
            result = new String[0];
        } else {
            result = listed;
        }
        return result;
    }

    private boolean exists(final String path) {
        final File target = this.file(path);
        final boolean result;
        if (this.parts.get(this.parts.size() - 1).isEmpty()) {
            result = target.isDirectory();
        } else {
            result = Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS);
        }
        return result;
    }

    private File file(final String path) {
        final File result;
        if (path.startsWith("/")) {
            result = new File(path);
        } else {
            result = new File(this.cwd, path);
        }
        return result;
    }

    private static boolean visible(final String part, final String name) {
        return !name.startsWith(".") || part.startsWith(".");
    }

    private static String join(final int index, final String path, final String name) {
        final String result;
        if (index == 0) {
            result = name;
        } else {
            result = String.format("%s/%s", path, name);
        }
        return result;
    }
}
