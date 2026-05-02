/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.lexer.Nesting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: POSIX 2.2.3 double quotes and 2.6.3 command
 * substitution nested in words.
 *
 * @since 1.0
 */
final class PosixQuotedSubstitutionTest {

    @Test
    void singleQuoteInsideDoubleQuotesIsLiteral() {
        Assertions.assertEquals(
            String.format("'7'%n"),
            Sh4j.INSTANCE.out("x=7; echo \"'$x'\"")
        );
    }

    @Test
    void singleQuotesAmidDoubleQuotedText() {
        Assertions.assertEquals(
            String.format("a'b'7%n"),
            Sh4j.INSTANCE.out("x=7; echo \"a'b'$x\"")
        );
    }

    @Test
    void doubleQuotesNestInQuotedSubstitution() {
        Assertions.assertEquals(
            String.format("a  b%n"),
            Sh4j.INSTANCE.out("echo \"$(echo \"a  b\")\"")
        );
    }

    @Test
    void casePatternInsideSubstitution() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out("echo $(case x in x) echo m;; esac)")
        );
    }

    @Test
    void casePatternInsideQuotedSubstitution() {
        Assertions.assertEquals(
            String.format("q  r%n"),
            Sh4j.INSTANCE.out("echo \"$(case x in x) echo \"q  r\";; esac)\"")
        );
    }

    @Test
    void leadingParenCasePattern() {
        Assertions.assertEquals(
            String.format("n%n"),
            Sh4j.INSTANCE.out("echo $(case x in (x) echo n;; esac)")
        );
    }

    @Test
    void secondCasePattern() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("echo $(case y in x) echo a;; y) echo b;; esac)")
        );
    }

    @Test
    void nestedCaseInSubstitution() {
        Assertions.assertEquals(
            String.format("z%n"),
            Sh4j.INSTANCE.out("echo $(case x in x) case y in y) echo z;; esac;; esac)")
        );
    }

    @Test
    void listInCaseBody() {
        Assertions.assertEquals(
            String.format("p q%n"),
            Sh4j.INSTANCE.out("echo $(case x in x) echo p; echo q;; esac)")
        );
    }

    @Test
    void subshellInCaseBody() {
        Assertions.assertEquals(
            String.format("s%n"),
            Sh4j.INSTANCE.out("echo $(case x in x) (echo s);; esac)")
        );
    }

    @Test
    void caseAfterReservedWord() {
        Assertions.assertEquals(
            String.format("t%n"),
            Sh4j.INSTANCE.out("echo $(if true; then case x in x) echo t;; esac; fi)")
        );
    }

    @Test
    void caseOnNewLines() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(String.format("echo $(case x in x) echo m;;%cesac)", '\n'))
        );
    }

    @Test
    void caseWordAsArgument() {
        Assertions.assertEquals(
            String.format("case%n"),
            Sh4j.INSTANCE.out("echo $(echo case)")
        );
    }

    @Test
    void esacWordAsArgument() {
        Assertions.assertEquals(
            String.format("esac%n"),
            Sh4j.INSTANCE.out("echo $(echo esac)")
        );
    }

    @Test
    void quotedCaseWord() {
        Assertions.assertEquals(
            String.format("case in%n"),
            Sh4j.INSTANCE.out("echo $(echo \"case\" in)")
        );
    }

    @Test
    void singleQuotedParenInSubstitution() {
        Assertions.assertEquals(
            String.format(")%n"),
            Sh4j.INSTANCE.out("echo $(echo ')')")
        );
    }

    @Test
    void singleQuotedParenInQuotedSubstitution() {
        Assertions.assertEquals(
            String.format(")%n"),
            Sh4j.INSTANCE.out("echo \"$(echo ')')\"")
        );
    }

    @Test
    void escapedParenInSubstitution() {
        Assertions.assertEquals(
            String.format(")%n"),
            Sh4j.INSTANCE.out("echo $(echo \\))")
        );
    }

    @Test
    void backquoteInSubstitution() {
        Assertions.assertEquals(
            String.format(")%n"),
            Sh4j.INSTANCE.out("echo $(echo `echo ')'`)")
        );
    }

    @Test
    void nestedSubstitution() {
        Assertions.assertEquals(
            String.format("w%n"),
            Sh4j.INSTANCE.out("echo $(echo $(echo w))")
        );
    }

    @Test
    void braceInSubstitution() {
        Assertions.assertEquals(
            String.format("v%n"),
            Sh4j.INSTANCE.out("echo $(echo ${u:-v})")
        );
    }

    @Test
    void specialParameterInSubstitution() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("echo $(echo $#)")
        );
    }

    @Test
    void mixedQuotesInSubstitution() {
        Assertions.assertEquals(
            String.format("abc%n"),
            Sh4j.INSTANCE.out("echo $(echo a\"b\"c)")
        );
    }

    @Test
    void pipeInSubstitution() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("echo $(echo a |cat)")
        );
    }

    @Test
    void redirectInSubstitution() {
        Assertions.assertEquals(
            String.format("r%n"),
            Sh4j.INSTANCE.out("echo $(echo a>/dev/null; echo r)")
        );
    }

    @Test
    void commentInSubstitution() {
        Assertions.assertEquals(
            String.format("k%n"),
            Sh4j.INSTANCE.out(String.format("echo $(%c# c )%cecho k)", '\n', '\n'))
        );
    }

    @Test
    void arithmeticExpansion() {
        Assertions.assertEquals(
            String.format("3%n"),
            Sh4j.INSTANCE.out("echo $((1+2))")
        );
    }

    @Test
    void arithmeticWithParens() {
        Assertions.assertEquals(
            String.format("9%n"),
            Sh4j.INSTANCE.out("echo $(( (1+2)*3 ))")
        );
    }

    @Test
    void quotedDefaultInQuotedBrace() {
        Assertions.assertEquals(
            String.format("a b%n"),
            Sh4j.INSTANCE.out("echo \"${u:-\"a b\"}\"")
        );
    }

    @Test
    void quotedBraceInQuotedDefault() {
        Assertions.assertEquals(
            String.format("a}%n"),
            Sh4j.INSTANCE.out("echo \"${u:-\"a}\"}\"")
        );
    }

    @Test
    void quotedBraceInDefault() {
        Assertions.assertEquals(
            String.format("a}%n"),
            Sh4j.INSTANCE.out("echo ${u:-\"a}\"}")
        );
    }

    @Test
    void singleQuotedBraceInDefault() {
        Assertions.assertEquals(
            String.format("a}%n"),
            Sh4j.INSTANCE.out("echo ${u:-'a}'}")
        );
    }

    @Test
    void singleQuoteInQuotedBrace() {
        Assertions.assertEquals(
            String.format("'a'%n"),
            Sh4j.INSTANCE.out("echo \"${u:-'a}'\"")
        );
    }

    @Test
    void substitutionInQuotedBrace() {
        Assertions.assertEquals(
            String.format("}%n"),
            Sh4j.INSTANCE.out("echo \"${u:-$(printf %s \"}\")}\"")
        );
    }

    @Test
    void backquoteInDoubleQuotes() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out("echo \"`echo hi`\"")
        );
    }

    @Test
    void escapedQuoteInQuotedBackquote() {
        Assertions.assertEquals(
            String.format("hi%n"),
            Sh4j.INSTANCE.out("echo \"`echo \\\"hi\\\"`\"")
        );
    }

    @Test
    void escapedQuoteInBackquote() {
        Assertions.assertEquals(
            String.format("\"hi\"%n"),
            Sh4j.INSTANCE.out("echo `echo \\\"hi\\\"`")
        );
    }

    @Test
    void escapedDoubleQuoteBeforeSingle() {
        Assertions.assertEquals(
            String.format("a\"b'c%n"),
            Sh4j.INSTANCE.out("echo \"a\\\"b'c\"")
        );
    }

    @Test
    void doubleQuotesInsideSingleQuotes() {
        Assertions.assertEquals(
            String.format("\"$x\"%n"),
            Sh4j.INSTANCE.out("echo '\"$x\"'")
        );
    }

    @Test
    void alternatingQuotes() {
        Assertions.assertEquals(
            String.format("1$x1%n"),
            Sh4j.INSTANCE.out("x=1; echo \"$x\"'$x'\"$x\"")
        );
    }

    @Test
    void escapedParenInBackquoteInSubstitution() {
        Assertions.assertEquals(
            String.format(")%n"),
            Sh4j.INSTANCE.out("echo $(echo `echo \\)`)")
        );
    }

    @Test
    void tabInSubstitution() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("echo $(echo\ta)")
        );
    }

    @Test
    void escapedLetterInQuotedBackquote() {
        Assertions.assertEquals(
            String.format("aq%n"),
            Sh4j.INSTANCE.out("echo \"`printf %s a\\q`\"")
        );
    }

    @Test
    void hashInsideWordInSubstitution() {
        Assertions.assertEquals(
            String.format("a#b%n"),
            Sh4j.INSTANCE.out("echo $(echo a#b)")
        );
    }

    @Test
    void lastCaseItemWithoutTerminatorEndsAtEsac() {
        final String body = String.format("case x in x) echo m%cesac", '\n');
        Assertions.assertEquals(
            body.length() + 1,
            new Nesting(String.format("%s) tail", body)).paren(0)
        );
    }

    @Test
    void unterminatedSubstitutionHasNoEnd() {
        Assertions.assertEquals(-1, new Nesting("echo \"a)").paren(0));
    }

    @Test
    void commentToEndHidesParen() {
        Assertions.assertEquals(-1, new Nesting("# c )").paren(0));
    }
}
