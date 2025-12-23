/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Creates files and directories inside a JUnit-managed temporary root.
 *
 * @since 1.0
 */
public final class TempDirs {

    private TempDirs() {
    }

    /**
     * Creates a uniquely named file inside the temporary root.
     *
     * @param root The {@code @TempDir} root
     * @return The created file
     * @throws IOException When the file cannot be created
     */
    public static File file(final Path root) throws IOException {
        return TempDirs.file(root, UUID.randomUUID().toString());
    }

    /**
     * Creates a named file inside the temporary root.
     *
     * @param root The {@code @TempDir} root
     * @param name The file name
     * @return The created file
     * @throws IOException When the file cannot be created
     */
    public static File file(final Path root, final String name)
        throws IOException {
        final Path file = root.resolve(name);
        Files.deleteIfExists(file);
        return Files.createFile(file).toFile();
    }

    /**
     * Creates a uniquely named directory inside the temporary root.
     *
     * @param root The {@code @TempDir} root
     * @return The created directory
     * @throws IOException When the directory cannot be created
     */
    public static File folder(final Path root) throws IOException {
        return TempDirs.folder(root, UUID.randomUUID().toString());
    }

    /**
     * Creates a named directory inside the temporary root, replacing any
     * existing one of the same name.
     *
     * @param root The {@code @TempDir} root
     * @param name The directory name
     * @return The created directory
     * @throws IOException When the directory cannot be created
     */
    public static File folder(final Path root, final String name)
        throws IOException {
        final Path dir = root.resolve(name);
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(TempDirs::remove);
            }
        }
        return Files.createDirectory(dir).toFile();
    }

    private static void remove(final Path target) {
        try {
            Files.delete(target);
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
