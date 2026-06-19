/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: the word after {@code >&} and {@code <&} is
 * expanded once, then names a descriptor or closes one (POSIX 2.7.5, 2.7.6).
 *
 * @since 1.0
 */
final class PosixDuplicateTargetTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void echoesToSubstitutedStandardOutput() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("echo a >&\"$(echo 1)\"")
        );
    }

    @Test
    void echoesToLiteralStandardOutput() {
        Assertions.assertEquals(String.format("a%n"), Sh4j.INSTANCE.out("echo a >&1"));
    }

    @Test
    void duplicatesStandardErrorBeforeDiscardingIt() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out("{ fd=2; echo e >&\"$fd\" 2>/dev/null; } 2>&1")
        );
    }

    @Test
    void keepsStandardErrorDuplicatedOntoItself() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out("{ echo e 2>&\"$(echo 2)\" >&2; } 2>&1")
        );
    }

    @Test
    void closesDescriptorNamedByVariable() throws IOException {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run(
                PosixDuplicateTargetTest.inside(
                    "exec 3>f; c=-; exec 3>&\"$c\"; echo x >&3"
                )
            )
        );
    }

    @Test
    void execBindsSubstitutedDescriptor() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("exec 3>&\"$(echo 1)\"; echo b >&3")
        );
    }

    @Test
    void execBindsLiteralDescriptor() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("exec 3>&1; echo b >&3")
        );
    }

    @Test
    void execBindsDescriptorDuplicatedFromBoundOne() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("exec 3>&1; exec 4>&\"$(echo 3)\"; echo b >&4")
        );
    }

    @Test
    void execDuplicatesBeforeLaterRedirection() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("exec 3>&1 1>/dev/null; echo b >&3")
        );
    }

    @Test
    void execBindsStandardErrorDescriptor() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out("{ exec 3>&2; echo e >&3; } 2>&1")
        );
    }

    @Test
    void execDuplicatesStandardErrorOntoSubstitutedOutput() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out("exec 2>&\"$(echo 1)\"; echo e >&2")
        );
    }

    @Test
    void execFailsOnUnboundDescriptor() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("exec 4>&\"$(echo 7)\""));
    }

    @Test
    void expandsTargetOnce() {
        Assertions.assertEquals(
            String.format("a%n1%n"),
            Sh4j.INSTANCE.out("n=0; echo a >&\"$((n+=1))\"; echo \"$n\"")
        );
    }

    @Test
    void functionWritesToSubstitutedDescriptor() {
        Assertions.assertEquals(
            String.format("fn%n"),
            Sh4j.INSTANCE.out("f() { echo fn; }; f >&\"$(echo 1)\"")
        );
    }

    @Test
    void groupWritesToSubstitutedDescriptor() {
        Assertions.assertEquals(
            String.format("g%n"),
            Sh4j.INSTANCE.out("{ echo g; } >&\"$(echo 1)\"")
        );
    }

    @Test
    void readsFromSubstitutedInputDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("v=r%n"),
            Sh4j.INSTANCE.out(
                PosixDuplicateTargetTest.inside(
                    "exec 3<'in put'; read v <&\"$(echo 3)\"; echo \"v=$v\""
                )
            )
        );
    }

    @Test
    void closesInputDescriptorNamedByVariable() throws IOException {
        Assertions.assertEquals(
            String.format("u=%n"),
            Sh4j.INSTANCE.out(
                PosixDuplicateTargetTest.inside(
                    String.join(
                        " ",
                        "{ exec 7<'in put'; c=-; exec 7<&\"$c\";",
                        "read u <&7; echo \"u=$u\"; } </dev/null"
                    )
                )
            )
        );
    }

    @Test
    void externalReadsSubstitutedStandardInput() {
        Assertions.assertEquals(
            String.format("z%n"),
            Sh4j.INSTANCE.out("echo z | cat <&\"$(echo 0)\"")
        );
    }

    @Test
    void externalWritesErrorsToSubstitutedOutput() {
        Assertions.assertEquals(
            String.format("E%n"),
            Sh4j.INSTANCE.out("o=1; sh -c 'echo E >&2' 2>&\"$o\"")
        );
    }

    @Test
    void rejectsTargetThatIsNotNumber() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("echo q >&\"x\""));
    }

    private static String inside(final String script) throws IOException {
        final File dir = new TempDirs(PosixDuplicateTargetTest.root).folder();
        Files.writeString(
            new File(dir, "in put").toPath(), String.format("r%n"), StandardCharsets.UTF_8
        );
        return String.format("cd '%s' && %s", dir.getAbsolutePath(), script);
    }
}
