/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for WordExpander.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class WordExpanderTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    /** ABC constant. */
    private static final String ABC = "abc";

    @Test public void tildeHome() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        Assert.assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test public void tildePlusUsesPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/cur");
        Assert.assertEquals("/cur/y", WordExpander.expandTilde("~+/y", env));
    }

    @Test public void tildeNoOpWhenNoLeadingTilde() {
        Assert.assertEquals(
            WordExpanderTest.ABC,
            WordExpander.expandTilde(WordExpanderTest.ABC, new HashMap<>())
        );
    }

    @Test public void normaliseBackticksBasic() {
        Assert.assertEquals("$(echo x)", WordExpander.normaliseBackticks("`echo x`"));
    }

    @Test public void normaliseBackticksDollarParenPassthrough() {
        Assert.assertEquals("$(cmd)", WordExpander.normaliseBackticks("$(cmd)"));
    }

    @Test public void splitFieldsDefaultIfs() {
        Assert.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a b c", String.format(" \t%n"))
        );
    }

    @Test public void splitFieldsCustomIfs() {
        Assert.assertEquals(Arrays.asList("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
    }

    @Test
    public void isQuotedDouble() {
        Assert.assertTrue(WordExpander.isQuoted("\"x\""));
    }

    @Test
    public void isQuotedSingle() {
        Assert.assertTrue(WordExpander.isQuoted("'x'"));
    }

    @Test
    public void isQuotedFalseMixed() {
        Assert.assertFalse(WordExpander.isQuoted("\"x'"));
    }

    @Test
    public void isQuotedFalseShort() {
        Assert.assertFalse(WordExpander.isQuoted("x"));
    }

    @Test
    public void removeQuotesDouble() {
        Assert.assertEquals("hello", WordExpander.removeQuotes("\"hello\""));
    }

    @Test
    public void removeQuotesSingle() {
        Assert.assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    public void removeQuotesUnquoted() {
        Assert.assertEquals(
            WordExpanderTest.ABC,
            WordExpander.removeQuotes(WordExpanderTest.ABC)
        );
    }

    @Test
    public void containsGlobCharsStar() {
        Assert.assertTrue(WordExpander.containsGlobChars("a*b"));
    }

    @Test
    public void containsGlobCharsQuestion() {
        Assert.assertTrue(WordExpander.containsGlobChars("a?b"));
    }

    @Test
    public void containsGlobCharsBracket() {
        Assert.assertTrue(WordExpander.containsGlobChars("a[b"));
    }

    @Test
    public void containsGlobCharsNone() {
        Assert.assertFalse(WordExpander.containsGlobChars(WordExpanderTest.ABC));
    }

    @Test public void pathnameExpandNoGlob() {
        Assert.assertEquals(
            Arrays.asList(WordExpanderTest.ABC),
            WordExpander.pathnameExpand(Arrays.asList(WordExpanderTest.ABC))
        );
    }

    @Test public void pathnameExpandNoMatchKeepsPattern() {
        Assert.assertEquals(
            Arrays.asList("__no_match_xyz__"),
            WordExpander.pathnameExpand(Arrays.asList("__no_match_xyz__"))
        );
    }

    @Test public void expandVariablesParityPlainText() {
        Assert.assertEquals(
            "hello world",
            WordExpander.expandVariablesImpl(
                "hello world",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesParityVarSubstitution() {
        final Map<String, String> env = new HashMap<>();
        env.put("MSG", "world");
        Assert.assertEquals(
            "hello world",
            WordExpander.expandVariablesImpl(
                "hello $MSG",
                env,
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesParityArithmetic() {
        Assert.assertEquals(
            "7",
            WordExpander.expandVariablesImpl(
                "$((3 + 4))",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesParityColonQuestionUnsetSetsError() {
        try {
            WordExpander.expandVariablesImpl(
                "${UNSET_VAR_FOR_PARITY:?required}",
                new HashMap<>(),
                new Executor(new HashMap<>())
            );
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
    }

    @Test public void expandVariablesUnclosedArithmetic() {
        Assert.assertEquals(
            "$((1+2",
            WordExpander.expandVariablesImpl(
                "$((1+2",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesUnclosedCmdSub() {
        Assert.assertEquals(
            "$(unclosed",
            WordExpander.expandVariablesImpl(
                "$(unclosed",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesDollarAtEnd() {
        Assert.assertEquals(
            "trailing$",
            WordExpander.expandVariablesImpl(
                "trailing$",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesDollarBraceWithoutClose() {
        Assert.assertEquals(
            "${unclosed",
            WordExpander.expandVariablesImpl(
                "${unclosed",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandVariablesDollarLetterPlainVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hi");
        Assert.assertEquals(
            "hi-suffix",
            WordExpander.expandVariablesImpl("$X-suffix", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void expandBracedColonDashUnsetReturnsDefault() {
        Assert.assertEquals(
            "fallback",
            WordExpander.expandBracedImpl(
                "x:-fallback",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandBracedColonEqUnsetAssigns() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(
            "42",
            WordExpander.expandBracedImpl("x:=42", env, new Executor(new HashMap<>()))
        );
        Assert.assertEquals("42", env.get("x"));
    }

    @Test public void expandBracedColonQuestionUnsetSetsError() {
        try {
            WordExpander.expandBracedImpl(
                "x:?required",
                new HashMap<>(),
                new Executor(new HashMap<>())
            );
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
    }

    @Test public void expandBracedHashLength() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        Assert.assertEquals(
            "5",
            WordExpander.expandBracedImpl("#v", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void expandBracedDashWithoutColonUnset() {
        Assert.assertEquals(
            "fallback",
            WordExpander.expandBracedImpl(
                "x-fallback",
                new HashMap<>(),
                new Executor(new HashMap<>())
            )
        );
    }

    @Test public void expandBracedDashWithoutColonSetEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assert.assertEquals(
            "set-but-empty: no fallback",
            "",
            WordExpander.expandBracedImpl("x-fallback", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void expandBracedPlusSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assert.assertEquals(
            "word",
            WordExpander.expandBracedImpl("x+word", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void expandBracedColonPlusSetNonEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "v");
        Assert.assertEquals(
            "word",
            WordExpander.expandBracedImpl("x:+word", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void expandBracedQuestionWithoutColonUnsetSetsError() {
        try {
            WordExpander.expandBracedImpl(
                "x?required",
                new HashMap<>(),
                new Executor(new HashMap<>())
            );
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
    }

    @Test public void expandBracedEqualsWithoutColonUnsetAssigns() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(
            "42",
            WordExpander.expandBracedImpl("x=42", env, new Executor(new HashMap<>()))
        );
        Assert.assertEquals("42", env.get("x"));
    }

    @Test public void expandBracedHashHashLongestPrefix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        Assert.assertEquals(
            "d",
            WordExpander.expandBracedImpl("f##*/", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void expandBracedPercentPercentLongestSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.b.c");
        Assert.assertEquals(
            "a",
            WordExpander.expandBracedImpl("f%%.*", env, new Executor(new HashMap<>()))
        );
    }

    @Test public void splitFieldsConsecutiveIfsCreatesEmptyField() {
        final java.util.List<String> result = WordExpander.splitFields("a::b", ":");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("a", result.get(0));
        Assert.assertEquals("", result.get(1));
        Assert.assertEquals("b", result.get(2));
    }

    @Test public void globMatchesEmptyPattern() {
        Assert.assertTrue(WordExpander.globMatches("", ""));
        Assert.assertFalse(WordExpander.globMatches("", "anything"));
    }

    @Test public void expandBracedPercentSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        Assert.assertEquals(
            "a",
            WordExpander.expandBracedImpl("f%.txt", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    public void globMatchesStar() {
        Assert.assertTrue(WordExpander.globMatches("a*", WordExpanderTest.ABC));
    }

    @Test
    public void globMatchesQuestion() {
        Assert.assertTrue(WordExpander.globMatches("a?c", WordExpanderTest.ABC));
    }

    @Test
    public void globMatchesCharClass() {
        Assert.assertTrue(WordExpander.globMatches("[abc]", "b"));
    }

    @Test
    public void globMatchesNoMatch() {
        Assert.assertFalse(WordExpander.globMatches("a*", "xyz"));
    }

    @Test
    public void escapeForQuoteRemovalEmpty() {
        Assert.assertEquals("", WordExpander.escapeForQuoteRemoval(""));
    }

    @Test
    public void escapeForQuoteRemovalPlain() {
        Assert.assertEquals("abc", WordExpander.escapeForQuoteRemoval("abc"));
    }

    @Test
    public void escapeForQuoteRemovalBackslash() {
        Assert.assertEquals(
            "a\\\\b",
            WordExpander.escapeForQuoteRemoval("a\\b")
        );
    }

    @Test
    public void escapeForQuoteRemovalSingleQuote() {
        Assert.assertEquals(
            "a\\'b",
            WordExpander.escapeForQuoteRemoval("a'b")
        );
    }

    @Test
    public void escapeForQuoteRemovalDoubleQuote() {
        Assert.assertEquals(
            "a\\\"b",
            WordExpander.escapeForQuoteRemoval("a\"b")
        );
    }

    @Test
    public void escapeForQuoteRemovalDollarNotEscaped() {
        Assert.assertEquals(
            "$VAR",
            WordExpander.escapeForQuoteRemoval("$VAR")
        );
    }

    @Test
    public void escapeForQuoteRemovalRoundTrip() {
        final String original = "a\\b'c\"d";
        Assert.assertEquals(
            original,
            WordExpander.removeQuotes(WordExpander.escapeForQuoteRemoval(original))
        );
    }

    @Test
    public void unclosedDollarParenAndNestedBraceStayLiteral() {
        Assert.assertEquals(
            "$(abc\n",
            io.github.andreoss.sh4j.Sh4j.out("echo $(abc")
        );
        Assert.assertEquals(
            "x\n",
            io.github.andreoss.sh4j.Sh4j.out("echo ${a${b}}x")
        );
        Assert.assertEquals(
            "${a\n",
            io.github.andreoss.sh4j.Sh4j.out("echo ${a")
        );
    }

    @Test
    public void defaultOperatorsDistinguishUnsetFromEmpty() {
        Assert.assertEquals(
            "default||||\n",
            io.github.andreoss.sh4j.Sh4j.out(
                "U=; echo \"${M-default}|${U-default}|${U:+alt}|${U:+}|${M:+alt}\""
            )
        );
    }

    @Test
    public void prefixAndSuffixOperatorsAtBoundaries() {
        Assert.assertEquals(
            "abc||abc||bc|ab|\n",
            io.github.andreoss.sh4j.Sh4j.out(
                "X=abc; echo \"${X#}|${X##*}|${X%}|${X%%*}|${X#a*}|${X%c}|${X##a*}\""
            )
        );
    }

    @Test
    public void errorOnNullWithoutWordUsesDefaultMessage() {
        final io.github.andreoss.sh4j.Sh4j.Result result =
            io.github.andreoss.sh4j.Sh4j.runFull("echo ${MISSING:?}");
        Assert.assertTrue(result.stderr().contains("$MISSING"));
    }

    @Test
    public void normaliseBacktickNestedAndUnclosed() {
        Assert.assertEquals(
            "$(echo ${x})",
            WordExpander.normaliseBackticks("`echo ${x}`")
        );
        Assert.assertEquals(
            "`unclosed",
            WordExpander.normaliseBackticks("`unclosed")
        );
        Assert.assertEquals(
            "${x}",
            WordExpander.normaliseBackticks("${x}")
        );
    }

    @Test
    public void removeQuotesBoundaryForms() {
        Assert.assertEquals("", WordExpander.removeQuotes(""));
        Assert.assertEquals("a", WordExpander.removeQuotes("'a"));
        Assert.assertEquals("a", WordExpander.removeQuotes("\"a"));
        Assert.assertEquals("a\\", WordExpander.removeQuotes("a\\"));
        Assert.assertEquals("a\\qb", WordExpander.removeQuotes("\"a\\qb\""));
    }

    @Test
    public void quoteAndGlobBoundaries() {
        Assert.assertFalse(WordExpander.isQuoted(null));
        Assert.assertFalse(WordExpander.isQuoted(""));
        Assert.assertFalse(WordExpander.isQuoted("\""));
        Assert.assertFalse(WordExpander.containsGlobChars("abc"));
        Assert.assertTrue(WordExpander.containsGlobChars("a?c"));
        Assert.assertTrue(WordExpander.containsGlobChars("a[c"));
    }

    @Test
    public void globMatchesBoundaries() {
        Assert.assertTrue(WordExpander.globMatches("", ""));
        Assert.assertFalse(WordExpander.globMatches("", "x"));
        Assert.assertTrue(WordExpander.globMatches("[abc", "[abc"));
        Assert.assertTrue(WordExpander.globMatches("a?c", "abc"));
        Assert.assertFalse(WordExpander.globMatches("a?c", "ac"));
        Assert.assertTrue(WordExpander.globMatches("a.b", "a.b"));
        Assert.assertFalse(WordExpander.globMatches("a.b", "axb"));
    }

    @Test
    public void appendDoubleQuotedEscapesInRemoveQuotes() {
        Assert.assertEquals(
            "a$b`c\"d\\e",
            WordExpander.removeQuotes("\"a\\$b\\`c\\\"d\\\\e\"")
        );
    }

    @Test
    public void identifierBoundariesAndEmptyBrace() {
        Assert.assertEquals(
            "zu a1 a_\n",
            io.github.andreoss.sh4j.Sh4j.out("Z=z; _=u; A1=a1; A_=a_; echo $Z$_ $A1 $A_")
        );
        Assert.assertEquals("${}x\n", io.github.andreoss.sh4j.Sh4j.out("echo ${}x"));
    }

    @Test
    public void substitutionBoundaryForms() {
        Assert.assertEquals("\n", io.github.andreoss.sh4j.Sh4j.out("echo $( )"));
        Assert.assertEquals("0\n", io.github.andreoss.sh4j.Sh4j.out("echo $(( ))"));
        Assert.assertEquals("$((1\n", io.github.andreoss.sh4j.Sh4j.out("echo $((1"));
        Assert.assertEquals("$(x\n", io.github.andreoss.sh4j.Sh4j.out("echo $(x"));
    }

    @Test
    public void nestedBraceMatching() {
        Assert.assertEquals("x\n", io.github.andreoss.sh4j.Sh4j.out("A=x; echo ${A:-${B:-d}}"));
        Assert.assertEquals("y\n", io.github.andreoss.sh4j.Sh4j.out("B=y; echo ${A:-${B:-d}}"));
        Assert.assertEquals("x\n", io.github.andreoss.sh4j.Sh4j.out("echo ${a{b}c}x"));
    }

    @Test
    public void errorOnNullWithCustomWord() {
        final io.github.andreoss.sh4j.Sh4j.Result result =
            io.github.andreoss.sh4j.Sh4j.runFull("echo ${MISSING:?msg}");
        Assert.assertTrue(result.stderr().contains("msg"));
    }

    @Test
    public void prefixAndSuffixFullMatchBoundaries() {
        Assert.assertEquals(
            "||||abc\n",
            io.github.andreoss.sh4j.Sh4j.out(
                "X=abc; echo \"${X#abc}|${X##abc}|${X%abc}|${X%%abc}|${X#z}\""
            )
        );
    }

    @Test
    public void assignmentOperatorRejectsInvalidNames() {
        Assert.assertNull(
            io.github.andreoss.sh4j.Sh4j.getVar("bad-name", "echo ${bad-name:=x}")
        );
    }

    @Test
    public void tildeBoundaries() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        Assert.assertEquals("/home/u", WordExpander.expandTilde("~", env));
        Assert.assertEquals("/cur", WordExpander.expandTilde("~+", env));
        Assert.assertEquals("", WordExpander.expandTilde("", env));
    }

    @Test
    public void normaliseDollarAndUnclosedForms() {
        Assert.assertEquals("$x", WordExpander.normaliseBackticks("$x"));
        Assert.assertEquals("$", WordExpander.normaliseBackticks("$"));
        Assert.assertEquals("$(x", WordExpander.normaliseBackticks("$(x"));
        Assert.assertEquals("${x", WordExpander.normaliseBackticks("${x"));
    }

    @Test
    public void quoteBoundaryAndEmptyEscape() {
        Assert.assertTrue(WordExpander.isQuoted("\"\""));
        Assert.assertTrue(WordExpander.isQuoted("''"));
        Assert.assertFalse(WordExpander.isQuoted("\"x"));
        Assert.assertNull(WordExpander.escapeForQuoteRemoval(null));
        Assert.assertEquals("", WordExpander.escapeForQuoteRemoval(""));
    }

    @Test
    public void removeQuotesEscapedDoubleAndEmpty() {
        Assert.assertEquals("", WordExpander.removeQuotes("\"\""));
        Assert.assertEquals("a\"b", WordExpander.removeQuotes("\"a\\\"b\""));
        Assert.assertEquals("a\\", WordExpander.removeQuotes("\"a\\\\\""));
    }

    @Test
    public void pathnameExpansionSkipsDotfilesAndSorts() throws IOException {
        final File dir = this.tempdir.newFolder("we-glob");
        Files.write(new File(dir, "b.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, ".hidden").toPath(), new byte[0]);
        Assert.assertEquals(
            Arrays.asList("a.txt", "b.txt"),
            WordExpander.pathnameExpand(Arrays.asList("*.txt"), dir)
        );
        Assert.assertEquals(
            Arrays.asList(".hidden"),
            WordExpander.pathnameExpand(Arrays.asList(".*"), dir)
        );
        Assert.assertEquals(
            Arrays.asList("no-match*"),
            WordExpander.pathnameExpand(Arrays.asList("no-match*"), dir)
        );
    }

    @Test
    public void globCharacterBoundaries() {
        Assert.assertFalse(WordExpander.containsGlobChars(""));
        Assert.assertTrue(WordExpander.globMatches("[", "["));
        Assert.assertFalse(WordExpander.globMatches("[", "x"));
        Assert.assertTrue(WordExpander.containsGlobChars("*"));
        Assert.assertTrue(WordExpander.containsGlobChars("?"));
        Assert.assertTrue(WordExpander.containsGlobChars("["));
        Assert.assertTrue(WordExpander.globMatches("[ab]", "a"));
        Assert.assertFalse(WordExpander.globMatches("[ab]", "c"));
    }

    @Test
    public void trailingBackslashAndUnclosedExpansions() {
        Assert.assertEquals("a\\\n", io.github.andreoss.sh4j.Sh4j.out("echo a\\"));
        Assert.assertEquals("${\n", io.github.andreoss.sh4j.Sh4j.out("echo ${"));
        Assert.assertEquals("$\n", io.github.andreoss.sh4j.Sh4j.out("echo $"));
    }

    @Test
    public void identifierAndSubstitutionBoundaries() {
        Assert.assertEquals(
            "zu n\n",
            io.github.andreoss.sh4j.Sh4j.out("Z=z; _=u; Z9=n; echo $Z$_ $Z9")
        );
        Assert.assertEquals(
            "1 true false\n",
            io.github.andreoss.sh4j.Sh4j.out("echo $((1)) $(echo true) $(echo false)")
        );
        Assert.assertEquals(
            "x 2\n",
            io.github.andreoss.sh4j.Sh4j.out("set -- x y; echo ${1} ${#}")
        );
    }

    @Test
    public void prefixAndSuffixNoMatchForms() {
        Assert.assertEquals(
            "abc|abc|abc|abc\n",
            io.github.andreoss.sh4j.Sh4j.out("X=abc; echo \"${X#z}|${X##z}|${X%z}|${X%%z}\"")
        );
    }

    @Test
    public void tildeUserAndRootBoundaries() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        Assert.assertEquals("~user/x", WordExpander.expandTilde("~user/x", env));
        Assert.assertEquals("/cur/", WordExpander.expandTilde("~+/", env));
    }

    @Test
    public void removeQuotesUnclosedAndEscapedForms() {
        Assert.assertEquals("a", WordExpander.removeQuotes("'a"));
        Assert.assertEquals("a", WordExpander.removeQuotes("\"a"));
        Assert.assertEquals("ab", WordExpander.removeQuotes("a\\b"));
        Assert.assertEquals("a\\", WordExpander.removeQuotes("a\\"));
    }
}
