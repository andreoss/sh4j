/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.Sh4j;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WordExpander}: append.
 *
 * @since 1.0
 */
final class WordExpanderAppendTest {

    /** ABC constant. */
    private static final String ABC = "abc";

    private static int exitCode(final Runnable action) {
        int code = -1;
        try {
            action.run();
        } catch (final ExitSignal ex) {
            code = ex.getCode();
        }
        return code;
    }

    @Test
    void appendDoubleQuotedEscapesInRemoveQuotes() {
        Assertions.assertEquals(
            "a$b`c\"d\\e",
            WordExpander.removeQuotes("\"a\\$b\\`c\\\"d\\\\e\"")
        );
    }

    @Test
    void assignmentOperatorRejectsInvalidNames() {
        Assertions.assertNull(Sh4j.INSTANCE.getVar("bad-name", "echo ${bad-name:=x}"));
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
        Assertions.assertFalse(WordExpander.containsGlobChars(WordExpanderAppendTest.ABC));
    }

    @Test
    void defaultOperatorsDistinguishUnsetFromEmpty() {
        Assertions.assertEquals(
            String.format("default||||%n"),
            Sh4j.INSTANCE.out("U=; echo \"${M-default}|${U-default}|${U:+alt}|${U:+}|${M:+alt}\"")
        );
    }

    @Test
    void errorOnNullWithoutWordUsesDefaultMessage() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo ${MISSING:?}").stderr().contains("$MISSING")
        );
    }

    @Test
    void errorOnNullWithCustomWord() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo ${MISSING:?msg}").stderr().contains("msg")
        );
    }

    @Test
    void errorOnEmptyFiresForSetButEmptyVariable() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("v=; echo ${v:?boom}").stderr().contains("boom")
        );
    }

    @Test
    void errorOnNullIgnoresSetButEmptyVariable() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("v=; echo ${v?novalue}"));
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
        Assertions.assertEquals("a\\\\b", WordExpander.escapeForQuoteRemoval("a\\b"));
    }

    @Test
    void escapeForQuoteRemovalSingleQuote() {
        Assertions.assertEquals("a\\'b", WordExpander.escapeForQuoteRemoval("a'b"));
    }

    @Test
    void escapeForQuoteRemovalDoubleQuote() {
        Assertions.assertEquals("a\\\"b", WordExpander.escapeForQuoteRemoval("a\"b"));
    }

    @Test
    void escapeForQuoteRemovalDollarNotEscaped() {
        Assertions.assertEquals("$VAR", WordExpander.escapeForQuoteRemoval("$VAR"));
    }

    @Test
    void escapeForQuoteRemovalRoundTrip() {
        final String original = "a\\b'c\"d";
        Assertions.assertEquals(
            original,
            WordExpander.removeQuotes(WordExpander.escapeForQuoteRemoval(original))
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
            WordExpander.expandVariablesImpl("hello $MSG", env, new Executor(new HashMap<>()))
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
        Assertions.assertEquals(
            1,
            WordExpanderAppendTest.exitCode(
                () -> WordExpander.expandVariablesImpl(
                    "${UNSET_VAR_FOR_PARITY:?required}",
                    new HashMap<>(),
                    new Executor(new HashMap<>())
                )
            )
        );
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
            WordExpander.expandVariablesImpl("$X-suffix", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedEmptyInnerReturnsNull() {
        Assertions.assertNull(
            WordExpander.expandBracedImpl("", new HashMap<>(), new Executor(new HashMap<>()))
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
    void expandBracedColonEqUnsetAssignsFirst() {
        Assertions.assertEquals(
            "42",
            WordExpander.expandBracedImpl("x:=42", new HashMap<>(), new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedColonEqUnsetAssignsSecond() {
        final Map<String, String> env = new HashMap<>();
        WordExpander.expandBracedImpl("x:=42", env, new Executor(new HashMap<>()));
        Assertions.assertEquals("42", env.get("x"));
    }

    @Test
    void expandBracedColonQuestionUnsetSetsError() {
        Assertions.assertEquals(
            1,
            WordExpanderAppendTest.exitCode(
                () -> WordExpander.expandBracedImpl(
                    "x:?required",
                    new HashMap<>(),
                    new Executor(new HashMap<>())
                )
            )
        );
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
        Assertions.assertEquals(
            1,
            WordExpanderAppendTest.exitCode(
                () -> WordExpander.expandBracedImpl(
                    "x?required",
                    new HashMap<>(),
                    new Executor(new HashMap<>())
                )
            )
        );
    }

    @Test
    void expandBracedEqualsWithoutColonUnsetAssignsFirst() {
        Assertions.assertEquals(
            "42",
            WordExpander.expandBracedImpl("x=42", new HashMap<>(), new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedEqualsWithoutColonUnsetAssignsSecond() {
        final Map<String, String> env = new HashMap<>();
        WordExpander.expandBracedImpl("x=42", env, new Executor(new HashMap<>()));
        Assertions.assertEquals("42", env.get("x"));
    }
}
