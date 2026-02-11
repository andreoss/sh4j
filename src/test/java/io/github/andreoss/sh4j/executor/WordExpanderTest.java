/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.Sh4j;
import io.github.andreoss.sh4j.TempDirs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for WordExpander.
 *
 * @since 1.0
 */
final class WordExpanderTest {

    /** ABC constant. */
    private static final String ABC = "abc";

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void tildeHome() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        Assertions.assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test
    void tildePlusUsesPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/cur");
        Assertions.assertEquals("/cur/y", WordExpander.expandTilde("~+/y", env));
    }

    @Test
    void tildeNoOpWhenNoLeadingTilde() {
        Assertions.assertEquals(
            WordExpanderTest.ABC,
            WordExpander.expandTilde(WordExpanderTest.ABC, new HashMap<>())
        );
    }

    @Test
    void normaliseBackticksBasic() {
        Assertions.assertEquals("$(echo x)", WordExpander.normaliseBackticks("`echo x`"));
    }

    @Test
    void normaliseBackticksDollarParenPassthrough() {
        Assertions.assertEquals("$(cmd)", WordExpander.normaliseBackticks("$(cmd)"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a b c", String.format(" \t%n"))
        );
    }

    @Test
    void splitFieldsCustomIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"), WordExpander.splitFields("a:b:c", ":")
        );
    }

    @Test
    void isQuotedDouble() {
        Assertions.assertTrue(WordExpander.isQuoted("\"x\""));
    }

    @Test
    void isQuotedSingle() {
        Assertions.assertTrue(WordExpander.isQuoted("'x'"));
    }

    @Test
    void isQuotedFalseMixed() {
        Assertions.assertFalse(WordExpander.isQuoted("\"x'"));
    }

    @Test
    void isQuotedFalseShort() {
        Assertions.assertFalse(WordExpander.isQuoted("x"));
    }

    @Test
    void removeQuotesDouble() {
        Assertions.assertEquals("hello", WordExpander.removeQuotes("\"hello\""));
    }

    @Test
    void removeQuotesSingle() {
        Assertions.assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesUnquoted() {
        Assertions.assertEquals(
            WordExpanderTest.ABC,
            WordExpander.removeQuotes(WordExpanderTest.ABC)
        );
    }

    @Test
    void containsGlobCharsStar() {
        Assertions.assertTrue(WordExpander.containsGlobChars("a*b"));
    }

    @Test
    void containsGlobCharsQuestion() {
        Assertions.assertTrue(WordExpander.containsGlobChars("a?b"));
    }

    @Test
    void containsGlobCharsBracket() {
        Assertions.assertTrue(WordExpander.containsGlobChars("a[b"));
    }

    @Test
    void containsGlobCharsNone() {
        Assertions.assertFalse(WordExpander.containsGlobChars(WordExpanderTest.ABC));
    }

    @Test
    void pathnameExpandNoGlob() {
        Assertions.assertEquals(
            Collections.singletonList(WordExpanderTest.ABC),
            WordExpander.pathnameExpand(Collections.singletonList(WordExpanderTest.ABC))
        );
    }

    @Test
    void pathnameExpandNoMatchKeepsPattern() {
        Assertions.assertEquals(
            Collections.singletonList("__no_match_xyz__"),
            WordExpander.pathnameExpand(Collections.singletonList("__no_match_xyz__"))
        );
    }

    @Test
    void expandVariablesParityPlainText() {
        Assertions.assertEquals(
            "hello world",
            WordExpander.expandVariablesImpl(
                "hello world",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesParityVarSubstitution() {
        final Map<String, String> env = new HashMap<>();
        env.put("MSG", "world");
        Assertions.assertEquals(
            "hello world",
            WordExpander.expandVariablesImpl(
                "hello $MSG",
                env,
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesParityArithmetic() {
        Assertions.assertEquals(
            "7",
            WordExpander.expandVariablesImpl(
                "$((3 + 4))",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesParityColonQuestionUnsetSetsError() {
        try {
            WordExpander.expandVariablesImpl(
                "${UNSET_VAR_FOR_PARITY:?required}",
                new HashMap<>(),
                new Executor(new HashMap<>())
            );
            Assertions.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final ExitSignal ex) {
            Assertions.assertEquals(1, ex.getCode());
        }
    }

    @Test
    void expandVariablesUnclosedArithmetic() {
        Assertions.assertEquals(
            "$((1+2",
            WordExpander.expandVariablesImpl(
                "$((1+2",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesUnclosedCmdSub() {
        Assertions.assertEquals(
            "$(unclosed",
            WordExpander.expandVariablesImpl(
                "$(unclosed",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesDollarAtEnd() {
        Assertions.assertEquals(
            "trailing$",
            WordExpander.expandVariablesImpl(
                "trailing$",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesDollarBraceWithoutClose() {
        Assertions.assertEquals(
            "${unclosed",
            WordExpander.expandVariablesImpl(
                "${unclosed",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandVariablesDollarLetterPlainVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hi");
        Assertions.assertEquals(
            "hi-suffix",
            WordExpander.expandVariablesImpl(
                "$X-suffix", env, new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandBracedEmptyInnerReturnsNull() {
        Assertions.assertNull(
            WordExpander.expandBracedImpl(
                "",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandBracedColonDashUnsetReturnsDefault() {
        Assertions.assertEquals(
            "fallback",
            WordExpander.expandBracedImpl(
                "x:-fallback",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandBracedColonEqUnsetAssigns() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(
            "42",
            WordExpander.expandBracedImpl("x:=42", env, new Executor(new HashMap<>()))
        );
        Assertions.assertEquals("42", env.get("x"));
    }

    @Test
    void expandBracedColonQuestionUnsetSetsError() {
        try {
            WordExpander.expandBracedImpl(
                "x:?required",
                new HashMap<>(),
                new Executor(new HashMap<>())
            );
            Assertions.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final ExitSignal ex) {
            Assertions.assertEquals(1, ex.getCode());
        }
    }

    @Test
    void expandBracedHashLength() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        Assertions.assertEquals(
            "5",
            WordExpander.expandBracedImpl("#v", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedDashWithoutColonUnset() {
        Assertions.assertEquals(
            "fallback",
            WordExpander.expandBracedImpl(
                "x-fallback",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test
    void expandBracedDashWithoutColonSetEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assertions.assertEquals(
            "",
            WordExpander.expandBracedImpl("x-fallback", env, new Executor(new HashMap<>())),
            "set-but-empty: no fallback"
        );
    }

    @Test
    void expandBracedPlusSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assertions.assertEquals(
            "word",
            WordExpander.expandBracedImpl("x+word", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedColonPlusSetNonEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "v");
        Assertions.assertEquals(
            "word",
            WordExpander.expandBracedImpl("x:+word", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedQuestionWithoutColonUnsetSetsError() {
        try {
            WordExpander.expandBracedImpl(
                "x?required",
                new HashMap<>(),
                new Executor(new HashMap<>())
            );
            Assertions.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final ExitSignal ex) {
            Assertions.assertEquals(1, ex.getCode());
        }
    }

    @Test
    void expandBracedEqualsWithoutColonUnsetAssigns() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(
            "42",
            WordExpander.expandBracedImpl("x=42", env, new Executor(new HashMap<>()))
        );
        Assertions.assertEquals("42", env.get("x"));
    }

    @Test
    void expandBracedHashHashLongestPrefix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        Assertions.assertEquals(
            "d",
            WordExpander.expandBracedImpl("f##*/", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedPercentPercentLongestSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.b.c");
        Assertions.assertEquals(
            "a",
            WordExpander.expandBracedImpl("f%%.*", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void splitFieldsConsecutiveIfsCreatesEmptyField() {
        final List<String> result = WordExpander.splitFields("a::b", ":");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("", result.get(1));
        Assertions.assertEquals("b", result.get(2));
    }

    @Test
    void globMatchesEmptyPattern() {
        Assertions.assertTrue(WordExpander.globMatches("", ""));
        Assertions.assertFalse(WordExpander.globMatches("", "anything"));
    }

    @Test
    void expandBracedPercentSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        Assertions.assertEquals(
            "a",
            WordExpander.expandBracedImpl("f%.txt", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void globMatchesStar() {
        Assertions.assertTrue(WordExpander.globMatches("a*", WordExpanderTest.ABC));
    }

    @Test
    void globMatchesQuestion() {
        Assertions.assertTrue(WordExpander.globMatches("a?c", WordExpanderTest.ABC));
    }

    @Test
    void globMatchesCharClass() {
        Assertions.assertTrue(WordExpander.globMatches("[abc]", "b"));
    }

    @Test
    void globMatchesNoMatch() {
        Assertions.assertFalse(WordExpander.globMatches("a*", "xyz"));
    }

    @Test
    void escapeForQuoteRemovalEmpty() {
        Assertions.assertEquals("", WordExpander.escapeForQuoteRemoval(""));
    }

    @Test
    void escapeForQuoteRemovalPlain() {
        Assertions.assertEquals("abc", WordExpander.escapeForQuoteRemoval("abc"));
    }

    @Test
    void escapeForQuoteRemovalBackslash() {
        Assertions.assertEquals(
            "a\\\\b",
            WordExpander.escapeForQuoteRemoval("a\\b")
        );
    }

    @Test
    void escapeForQuoteRemovalSingleQuote() {
        Assertions.assertEquals(
            "a\\'b",
            WordExpander.escapeForQuoteRemoval("a'b")
        );
    }

    @Test
    void escapeForQuoteRemovalDoubleQuote() {
        Assertions.assertEquals(
            "a\\\"b",
            WordExpander.escapeForQuoteRemoval("a\"b")
        );
    }

    @Test
    void escapeForQuoteRemovalDollarNotEscaped() {
        Assertions.assertEquals(
            "$VAR",
            WordExpander.escapeForQuoteRemoval("$VAR")
        );
    }

    @Test
    void escapeForQuoteRemovalRoundTrip() {
        final String original = "a\\b'c\"d";
        Assertions.assertEquals(
            original,
            WordExpander.removeQuotes(
                WordExpander.escapeForQuoteRemoval(original)
            )
        );
    }

    @Test
    void unclosedDollarParenAndNestedBraceStayLiteral() {
        Assertions.assertEquals(
            String.format("$(abc%n"),
            Sh4j.INSTANCE.out("echo $(abc")
        );
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("echo ${a${b}}x")
        );
        Assertions.assertEquals(
            String.format("${a%n"),
            Sh4j.INSTANCE.out("echo ${a")
        );
    }

    @Test
    void defaultOperatorsDistinguishUnsetFromEmpty() {
        Assertions.assertEquals(
            String.format("default||||%n"),
            Sh4j.INSTANCE.out(
                "U=; echo \"${M-default}|${U-default}|${U:+alt}|${U:+}|${M:+alt}\""
            )
        );
    }

    @Test
    void prefixAndSuffixOperatorsAtBoundaries() {
        Assertions.assertEquals(
            String.format("abc||abc||bc|ab|%n"),
            Sh4j.INSTANCE.out(
                "X=abc; echo \"${X#}|${X##*}|${X%}|${X%%*}|${X#a*}|${X%c}|${X##a*}\""
            )
        );
    }

    @Test
    void errorOnNullWithoutWordUsesDefaultMessage() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo ${MISSING:?}")
                .stderr()
                .contains("$MISSING")
        );
    }

    @Test
    void normaliseBacktickNestedAndUnclosed() {
        Assertions.assertEquals(
            "$(echo ${x})",
            WordExpander.normaliseBackticks("`echo ${x}`")
        );
        Assertions.assertEquals(
            "`unclosed",
            WordExpander.normaliseBackticks("`unclosed")
        );
        Assertions.assertEquals(
            "${x}",
            WordExpander.normaliseBackticks("${x}")
        );
    }

    @Test
    void removeQuotesBoundaryForms() {
        Assertions.assertEquals("", WordExpander.removeQuotes(""));
        Assertions.assertEquals("a", WordExpander.removeQuotes("'a"));
        Assertions.assertEquals("a", WordExpander.removeQuotes("\"a"));
        Assertions.assertEquals("a\\", WordExpander.removeQuotes("a\\"));
        Assertions.assertEquals("a\\qb", WordExpander.removeQuotes("\"a\\qb\""));
    }

    @Test
    void quoteAndGlobBoundaries() {
        Assertions.assertFalse(WordExpander.isQuoted(null));
        Assertions.assertFalse(WordExpander.isQuoted(""));
        Assertions.assertFalse(WordExpander.isQuoted("\""));
        Assertions.assertFalse(WordExpander.containsGlobChars("abc"));
        Assertions.assertTrue(WordExpander.containsGlobChars("a?c"));
        Assertions.assertTrue(WordExpander.containsGlobChars("a[c"));
    }

    @Test
    void globMatchesBoundaries() {
        Assertions.assertTrue(WordExpander.globMatches("", ""));
        Assertions.assertFalse(WordExpander.globMatches("", "x"));
        Assertions.assertTrue(WordExpander.globMatches("[abc", "[abc"));
        Assertions.assertTrue(WordExpander.globMatches("a?c", "abc"));
        Assertions.assertFalse(WordExpander.globMatches("a?c", "ac"));
        Assertions.assertTrue(WordExpander.globMatches("a.b", "a.b"));
        Assertions.assertFalse(WordExpander.globMatches("a.b", "axb"));
    }

    @Test
    void appendDoubleQuotedEscapesInRemoveQuotes() {
        Assertions.assertEquals(
            "a$b`c\"d\\e",
            WordExpander.removeQuotes("\"a\\$b\\`c\\\"d\\\\e\"")
        );
    }

    @Test
    void identifierBoundariesAndEmptyBrace() {
        Assertions.assertEquals(
            String.format("zu a1 a_%n"),
            Sh4j.INSTANCE.out("Z=z; _=u; A1=a1; A_=a_; echo $Z$_ $A1 $A_")
        );
        Assertions.assertEquals(String.format("${}x%n"), Sh4j.INSTANCE.out("echo ${}x"));
    }

    @Test
    void substitutionBoundaryForms() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo $( )"));
        Assertions.assertEquals(String.format("0%n"), Sh4j.INSTANCE.out("echo $(( ))"));
        Assertions.assertEquals(String.format("$((1%n"), Sh4j.INSTANCE.out("echo $((1"));
        Assertions.assertEquals(String.format("$(x%n"), Sh4j.INSTANCE.out("echo $(x"));
    }

    @Test
    void nestedBraceMatching() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("A=x; echo ${A:-${B:-d}}")
        );
        Assertions.assertEquals(
            String.format("y%n"),
            Sh4j.INSTANCE.out("B=y; echo ${A:-${B:-d}}")
        );
        Assertions.assertEquals(String.format("x%n"), Sh4j.INSTANCE.out("echo ${a{b}c}x"));
    }

    @Test
    void errorOnNullWithCustomWord() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo ${MISSING:?msg}")
                .stderr()
                .contains("msg")
        );
    }

    @Test
    void prefixAndSuffixFullMatchBoundaries() {
        Assertions.assertEquals(
            String.format("||||abc%n"),
            Sh4j.INSTANCE.out(
                "X=abc; echo \"${X#abc}|${X##abc}|${X%abc}|${X%%abc}|${X#z}\""
            )
        );
    }

    @Test
    void assignmentOperatorRejectsInvalidNames() {
        Assertions.assertNull(
            Sh4j.INSTANCE.getVar("bad-name", "echo ${bad-name:=x}")
        );
    }

    @Test
    void tildeBoundaries() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        Assertions.assertEquals("/home/u", WordExpander.expandTilde("~", env));
        Assertions.assertEquals("/cur", WordExpander.expandTilde("~+", env));
        Assertions.assertEquals("", WordExpander.expandTilde("", env));
    }

    @Test
    void normaliseDollarAndUnclosedForms() {
        Assertions.assertEquals("$x", WordExpander.normaliseBackticks("$x"));
        Assertions.assertEquals("$", WordExpander.normaliseBackticks("$"));
        Assertions.assertEquals("$(x", WordExpander.normaliseBackticks("$(x"));
        Assertions.assertEquals("${x", WordExpander.normaliseBackticks("${x"));
    }

    @Test
    void quoteBoundaryAndEmptyEscape() {
        Assertions.assertTrue(WordExpander.isQuoted("\"\""));
        Assertions.assertTrue(WordExpander.isQuoted("''"));
        Assertions.assertFalse(WordExpander.isQuoted("\"x"));
        Assertions.assertNull(WordExpander.escapeForQuoteRemoval(null));
        Assertions.assertEquals("", WordExpander.escapeForQuoteRemoval(""));
    }

    @Test
    void removeQuotesEscapedDoubleAndEmpty() {
        Assertions.assertEquals("", WordExpander.removeQuotes("\"\""));
        Assertions.assertEquals("a\"b", WordExpander.removeQuotes("\"a\\\"b\""));
        Assertions.assertEquals("a\\", WordExpander.removeQuotes("\"a\\\\\""));
    }

    @Test
    void pathnameExpansionSkipsDotfilesAndSorts() throws IOException {
        final File dir = TempDirs.folder(WordExpanderTest.root, "we-glob");
        Files.write(new File(dir, "b.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, ".hidden").toPath(), new byte[0]);
        Assertions.assertEquals(
            Arrays.asList("a.txt", "b.txt"),
            WordExpander.pathnameExpand(Collections.singletonList("*.txt"), dir)
        );
        Assertions.assertEquals(
            Collections.singletonList(".hidden"),
            WordExpander.pathnameExpand(Collections.singletonList(".*"), dir)
        );
        Assertions.assertEquals(
            Collections.singletonList("no-match*"),
            WordExpander.pathnameExpand(Collections.singletonList("no-match*"), dir)
        );
    }

    @Test
    void globCharacterBoundaries() {
        Assertions.assertFalse(WordExpander.containsGlobChars(""));
        Assertions.assertTrue(WordExpander.globMatches("[", "["));
        Assertions.assertFalse(WordExpander.globMatches("[", "x"));
        Assertions.assertTrue(WordExpander.containsGlobChars("*"));
        Assertions.assertTrue(WordExpander.containsGlobChars("?"));
        Assertions.assertTrue(WordExpander.containsGlobChars("["));
        Assertions.assertTrue(WordExpander.globMatches("[ab]", "a"));
        Assertions.assertFalse(WordExpander.globMatches("[ab]", "c"));
    }

    @Test
    void trailingBackslashAndUnclosedExpansions() {
        Assertions.assertEquals(String.format("a\\%n"), Sh4j.INSTANCE.out("echo a\\"));
        Assertions.assertEquals(String.format("${%n"), Sh4j.INSTANCE.out("echo ${"));
        Assertions.assertEquals(String.format("$%n"), Sh4j.INSTANCE.out("echo $"));
    }

    @Test
    void identifierAndSubstitutionBoundaries() {
        Assertions.assertEquals(
            String.format("zu n%n"),
            Sh4j.INSTANCE.out("Z=z; _=u; Z9=n; echo $Z$_ $Z9")
        );
        Assertions.assertEquals(
            String.format("1 true false%n"),
            Sh4j.INSTANCE.out("echo $((1)) $(echo true) $(echo false)")
        );
        Assertions.assertEquals(
            String.format("x 2%n"),
            Sh4j.INSTANCE.out("set -- x y; echo ${1} ${#}")
        );
    }

    @Test
    void prefixAndSuffixNoMatchForms() {
        Assertions.assertEquals(
            String.format("abc|abc|abc|abc%n"),
            Sh4j.INSTANCE.out("X=abc; echo \"${X#z}|${X##z}|${X%z}|${X%%z}\"")
        );
    }

    @Test
    void tildeUserAndRootBoundaries() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        Assertions.assertEquals("~user/x", WordExpander.expandTilde("~user/x", env));
        Assertions.assertEquals("/cur/", WordExpander.expandTilde("~+/", env));
    }

    @Test
    void removeQuotesUnclosedAndEscapedForms() {
        Assertions.assertEquals("a", WordExpander.removeQuotes("'a"));
        Assertions.assertEquals("a", WordExpander.removeQuotes("\"a"));
        Assertions.assertEquals("ab", WordExpander.removeQuotes("a\\b"));
        Assertions.assertEquals("a\\", WordExpander.removeQuotes("a\\"));
    }
}
