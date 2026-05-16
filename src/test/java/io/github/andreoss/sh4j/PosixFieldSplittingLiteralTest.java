/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: POSIX 2.6.5 field splitting applies only to the
 * results of unquoted expansions, never to the literal text of the word.
 *
 * @since 1.0
 */
final class PosixFieldSplittingLiteralTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void literalSeparatorIsNotSplit() {
        Assertions.assertEquals(
            "[a:b]",
            Sh4j.INSTANCE.out("IFS=:; printf '[%s]' a:b")
        );
    }

    @Test
    void unquotedExpansionBetweenQuotedPartsIsSplit() {
        Assertions.assertEquals(
            "[a1][2b]",
            Sh4j.INSTANCE.out("v=\"1 2\"; printf '[%s]' \"a\"$v\"b\"")
        );
    }

    @Test
    void literalTextJoinsSplitExpansion() {
        Assertions.assertEquals(
            "[a:x][y:b]",
            Sh4j.INSTANCE.out("IFS=:; v=x:y; printf '[%s]' a:$v:b")
        );
    }

    @Test
    void commandSubstitutionIsSplitButLiteralIsNot() {
        Assertions.assertEquals(
            "[1][2,3]",
            Sh4j.INSTANCE.out("IFS=,; printf '[%s]' $(echo 1,2),3")
        );
    }

    @Test
    void arithmeticIsSplitButLiteralIsNot() {
        Assertions.assertEquals(
            "[101][][1]",
            Sh4j.INSTANCE.out("IFS=0; printf '[%s]' 10$((100))1")
        );
    }

    @Test
    void quotedLiteralBeforeExpansionIsNotSplit() {
        Assertions.assertEquals(
            "[a:p][q]",
            Sh4j.INSTANCE.out("IFS=:; v=p:q; printf '[%s]' \"a:\"$v")
        );
    }

    @Test
    void defaultWordIsSplit() {
        Assertions.assertEquals(
            "[a][b]",
            Sh4j.INSTANCE.out("IFS=:; printf '[%s]' ${u:-a:b}")
        );
    }

    @Test
    void quotedDefaultWordIsNotSplit() {
        Assertions.assertEquals(
            "[a:b:c]",
            Sh4j.INSTANCE.out("IFS=:; printf '[%s]' ${u:-\"a:b\"}:c")
        );
    }

    @Test
    void doubleQuotedExpansionStaysOneField() {
        Assertions.assertEquals(
            "[1 2]",
            Sh4j.INSTANCE.out("v=\"1 2\"; printf '[%s]' \"$v\"")
        );
    }

    @Test
    void emptySingleQuotesGiveOneField() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("f() { echo $#; }; f ''")
        );
    }

    @Test
    void unquotedEmptyExpansionGivesNoField() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("f() { echo $#; }; f $empty")
        );
    }

    @Test
    void literalBeforeEmptyExpansionGivesOneField() {
        Assertions.assertEquals(
            "[a]",
            Sh4j.INSTANCE.out("f() { printf '[%s]' \"$@\"; }; f a$empty")
        );
    }

    @Test
    void tildeResultIsNotSplit() {
        Assertions.assertEquals(
            "[/h/x]",
            Sh4j.INSTANCE.out("HOME=/h/x; IFS=/; printf '[%s]' ~")
        );
    }

    @Test
    void literalPatternStillGlobsWhenItIsSeparator() throws IOException {
        final TempDirs dirs = new TempDirs(PosixFieldSplittingLiteralTest.root);
        final File dir = dirs.folder("star");
        new TempDirs(dir.toPath()).file("ab");
        new TempDirs(dir.toPath()).file("ac");
        Assertions.assertEquals(
            "[ab][ac]",
            Sh4j.INSTANCE.out(
                String.format(
                    "cd %s; IFS='*'; printf '[%%s]' a*", dir.getAbsolutePath()
                )
            )
        );
    }

    @Test
    void expansionKeepsPrivateCharacterAsData() {
        final String mark = String.valueOf((char) 0xE000);
        Assertions.assertEquals(
            String.format("[x%s][y]", mark),
            Sh4j.INSTANCE.out(String.format("IFS=:; v='x%s:y'; printf '[%%s]' $v", mark))
        );
    }
}
