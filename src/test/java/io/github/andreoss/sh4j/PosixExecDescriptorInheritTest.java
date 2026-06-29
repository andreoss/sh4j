/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: descriptors opened by a redirection-only
 * {@code exec} are inherited by the commands the shell runs (POSIX 2.7,
 * {@code exec}), including external commands and the commands of
 * subshells, pipelines, command substitutions and asynchronous lists.
 *
 * <p>Known limitation: the JDK cannot pass a descriptor above 2 to a child
 * process. A redirection such as {@code >&3}, {@code <&3} or {@code 2>&3}
 * on an external command is mapped onto the standard descriptor of the
 * child and the data is pumped through a pipe to or from the bound
 * descriptor of the shell. A child that opens descriptor 3 itself, as in
 * {@code exec 3>out; sh -c 'echo x >&3'}, does not see it and fails with
 * "bad file descriptor". A child that reads from a bound input descriptor
 * through {@code <&3} is fed the whole remaining content, so a later reader
 * of that descriptor starts at its end rather than where the child stopped
 * reading.</p>
 *
 * @since 1.0
 */
final class PosixExecDescriptorInheritTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void pipelineExternalWritesToBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; echo a | cat >&3; exec 3>&-; cat out"
                )
            )
        );
    }

    @Test
    void externalReadsBoundInputDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("l1%nl2%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "printf 'l1\\nl2\\n' > in; exec 3<in; cat <&3"
                )
            )
        );
    }

    @Test
    void externalWritesToBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format(".%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within("exec 3>out; ls -d . >&3; cat out")
            )
        );
    }

    @Test
    void externalErrorAndOutputFollowBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format(".%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; ls -d . 2>&3 1>&2; cat out"
                )
            )
        );
    }

    @Test
    void builtinErrorAndOutputFollowBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("[b]%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; printf 'b\\n' 2>&3 1>&2; echo \"[$(cat out)]\""
                )
            )
        );
    }

    @Test
    void builtinAndExternalWritesKeepOrder() throws IOException {
        Assertions.assertEquals(
            String.format("one%ntwo%nthree%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    String.join(
                        "; ",
                        "exec 4>>log", "echo one >&4", "date >/dev/null",
                        "printf 'two\\n' >&4", "echo three >&4", "cat log"
                    )
                )
            )
        );
    }

    @Test
    void subshellWritesToBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("s%ne%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; (echo s; cat /dev/null) >&3; (printf 'e\\n' >&3); cat out"
                )
            )
        );
    }

    @Test
    void pipelineSubshellWritesToBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("f%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; printf 'f\\n' | (cat >&3); cat out"
                )
            )
        );
    }

    @Test
    void asynchronousListWritesToBoundDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("g%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; printf 'g\\n' >&3 & wait; cat out"
                )
            )
        );
    }

    @Test
    void substitutionReadsBoundInputDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("v=1%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "printf '1\\n2\\n' > in; exec 3<in; v=$(head -n 1 <&3); echo \"v=$v\""
                )
            )
        );
    }

    @Test
    void subshellCloseKeepsParentOutputDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("k%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "exec 3>out; (exec 3>&-); echo k >&3; cat out"
                )
            )
        );
    }

    @Test
    void subshellCloseKeepsParentInputDescriptor() throws IOException {
        Assertions.assertEquals(
            String.format("w=1%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    "printf '1\\n' > in; exec 3<in; (exec 3<&-); read w <&3; echo \"w=$w\""
                )
            )
        );
    }

    @Test
    void subshellReadAdvancesSharedOffset() throws IOException {
        Assertions.assertEquals(
            String.format("1 2%n"),
            Sh4j.INSTANCE.out(
                PosixExecDescriptorInheritTest.within(
                    String.join(
                        "; ",
                        "printf '1\\n2\\n' > in", "exec 3<in",
                        "(read a <&3; echo \"$a\" > got)", "read b <&3",
                        "echo \"$(cat got) $b\""
                    )
                )
            )
        );
    }

    private static String within(final String script) throws IOException {
        return String.format(
            "cd '%s' && { %s; }",
            new TempDirs(PosixExecDescriptorInheritTest.root).folder().getAbsolutePath(),
            script
        );
    }
}
