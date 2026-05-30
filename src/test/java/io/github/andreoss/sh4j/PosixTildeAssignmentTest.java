/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: in an assignment, tilde-prefixes also follow an
 * unquoted colon and end at an unquoted slash or colon (POSIX 2.6.1).
 *
 * @since 1.0
 */
final class PosixTildeAssignmentTest {

    @Test
    void expandsTildeOnBothSidesOfColon() {
        Assertions.assertEquals(
            String.format("/h:/h/b%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=~:~/b; echo \"$z\"")
        );
    }

    @Test
    void expandsTildeBetweenColons() {
        Assertions.assertEquals(
            String.format("a:/h:b%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=a:~:b; echo \"$z\"")
        );
    }

    @Test
    void keepsDoubleQuotedTilde() {
        Assertions.assertEquals(
            String.format("a:~:b%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=a:\"~\":b; echo \"$z\"")
        );
    }

    @Test
    void keepsTildeAfterColonOutsideAssignment() {
        Assertions.assertEquals(
            String.format("a:~%n"),
            Sh4j.INSTANCE.out("HOME=/h; echo a:~")
        );
    }

    @Test
    void expandsTildeAfterColonInExport() {
        Assertions.assertEquals(
            String.format("/h:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; export z=~:~; echo \"$z\"")
        );
    }

    @Test
    void expandsTildeAfterColonInReadonly() {
        Assertions.assertEquals(
            String.format("/h:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; readonly z=~:~; echo \"$z\"")
        );
    }

    @Test
    void keepsTildeInsideWord() {
        Assertions.assertEquals(
            String.format("x~:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=x~:~; echo \"$z\"")
        );
    }

    @Test
    void quotesHomeAfterColon() {
        Assertions.assertEquals(
            String.format("/a b*:/a b*%n"),
            Sh4j.INSTANCE.out("HOME='/a b*'; z=~:~; echo \"$z\"")
        );
    }

    @Test
    void expandsTildeAfterColonInPrefixAssignment() {
        Assertions.assertEquals(
            String.format("z=/h:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=~:~ env | grep '^z='")
        );
    }

    @Test
    void keepsTildeAfterEscapedColon() {
        Assertions.assertEquals(
            String.format("a:~%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=a\\:~; echo \"$z\"")
        );
    }

    @Test
    void keepsTildeAfterSingleQuotedColon() {
        Assertions.assertEquals(
            String.format("a:~%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=a':'~; echo \"$z\"")
        );
    }

    @Test
    void keepsTildeWithQuotedSuffix() {
        Assertions.assertEquals(
            String.format("a:~:b%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=a:~\"\":b; echo \"$z\"")
        );
    }

    @Test
    void expandsTildesEndedBySlashes() {
        Assertions.assertEquals(
            String.format("a:/h/:/h/c:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=a:~/:~/c:~; echo \"$z\"")
        );
    }

    @Test
    void keepsTildeInCommandSubstitution() {
        Assertions.assertEquals(
            String.format("a:~/b:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=$(echo a:~/b):~; echo \"$z\"")
        );
    }

    @Test
    void keepsTildeInBackquotes() {
        Assertions.assertEquals(
            String.format("a:~/b:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=`echo a:~/b`:~; echo \"$z\"")
        );
    }

    @Test
    void expandsTildeAfterArithmetic() {
        Assertions.assertEquals(
            String.format("3:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; z=$((1+(2))):~; echo \"$z\"")
        );
    }

    @Test
    void expandsTildeAfterBracedParameter() {
        Assertions.assertEquals(
            String.format("v:/h%n"),
            Sh4j.INSTANCE.out("HOME=/h; x=v; z=${x}:~; echo \"$z\"")
        );
    }

    @Test
    void expandsTildeAfterPlainParameter() {
        Assertions.assertEquals(
            String.format("v:/h:$%n"),
            Sh4j.INSTANCE.out("HOME=/h; x=v; z=$x:~:$; echo \"$z\"")
        );
    }
}
