/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.CaseItem;
import io.github.andreoss.sh4j.parser.CaseNode;
import io.github.andreoss.sh4j.parser.ForNode;
import io.github.andreoss.sh4j.parser.FunctionNode;
import io.github.andreoss.sh4j.parser.IfNode;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.WhileNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Runs compound commands: {@code if}, {@code for}, {@code while},
 * {@code until}, {@code case}, and function definitions.
 *
 * @since 1.0
 */
final class Compound {

    /** The shell that runs the bodies. */
    private final BuiltinContext shell;

    /** State of the errexit option. */
    private final Errexit errexit;

    /** Nesting depth of active loop bodies. */
    private int loops;

    /** Loop depth at which the running function body started. */
    private int floor;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the bodies
     * @param errexit State of the errexit option
     */
    Compound(final BuiltinContext shell, final Errexit errexit) {
        this.shell = shell;
        this.errexit = errexit;
    }

    /**
     * Runs a compound command node.
     *
     * @param node The node
     * @return The exit status, 0 for a node that is not a compound command
     */
    int run(final Node node) {
        int code = 0;
        if (node instanceof IfNode branch) {
            code = this.branch(branch);
        } else if (node instanceof ForNode loop) {
            code = this.iterate(loop);
        } else if (node instanceof WhileNode loop) {
            code = this.repeat(loop);
        } else if (node instanceof CaseNode choice) {
            code = this.choose(choice);
        } else if (node instanceof FunctionNode func) {
            this.shell.functionsMap().put(func.getName(), func);
        }
        return code;
    }

    /**
     * Returns the number of loops lexically enclosing the running command.
     *
     * @return The loop depth, 0 outside any loop
     */
    int depth() {
        return this.loops - this.floor;
    }

    /**
     * Runs a function body, which no loop of its caller encloses.
     *
     * @param body The function body
     * @return The exit status of the body
     */
    int isolated(final IntSupplier body) {
        final int shift = this.depth();
        this.floor = this.floor + shift;
        try {
            return body.getAsInt();
        } finally {
            this.floor = this.floor - shift;
        }
    }

    /**
     * Copies the loop depth of the parent shell into this subshell.
     *
     * @param parent Runner of compound commands of the parent shell
     */
    void inherit(final Compound parent) {
        this.loops = parent.depth();
    }

    private int branch(final IfNode node) {
        final int condexit = this.condition(node.getCondition());
        final int code;
        if (condexit == 0) {
            code = this.shell.execute(node.getThenBody());
        } else if (node.getElseBody() == null) {
            code = 0;
        } else {
            code = this.shell.execute(node.getElseBody());
        }
        return code;
    }

    private int iterate(final ForNode node) {
        final Iterator<String> words = this.words(node).iterator();
        final LoopBody body = new LoopBody(this.shell, node.getBody());
        this.loops = this.loops + 1;
        try {
            while (body.running() && words.hasNext()) {
                this.shell.environmentMap().put(node.getVariable(), words.next());
                body.run();
            }
        } finally {
            this.loops = this.loops - 1;
        }
        return body.status();
    }

    private List<String> words(final ForNode node) {
        final List<String> words;
        if (node.getWords() == null) {
            words = Compound.positional(this.shell.positionalParamsList());
        } else {
            words = new ArrayList<>(0);
            for (final String raw : node.getWords()) {
                words.addAll(
                    this.shell.expandArguments(raw, this.shell.environmentMap())
                );
            }
        }
        return words;
    }

    private int repeat(final WhileNode node) {
        this.loops = this.loops + 1;
        final LoopBody body = new LoopBody(this.shell, node.getBody());
        try {
            while (body.running() && this.proceeds(node)) {
                body.run();
            }
        } finally {
            this.loops = this.loops - 1;
        }
        return body.status();
    }

    private boolean proceeds(final WhileNode node) {
        return node.isUntil() == (this.condition(node.getCondition()) != 0);
    }

    private int choose(final CaseNode node) {
        final String word = WordExpander.removeQuotes(
            this.shell.expandVariables(node.getWord(), this.shell.environmentMap())
        );
        int code = 0;
        boolean matched = false;
        final Iterator<CaseItem> items = node.getItems().iterator();
        while (!matched && items.hasNext()) {
            final CaseItem item = items.next();
            matched = item.getPatterns().stream().anyMatch(
                pattern -> this.hits(pattern, word)
            );
            if (matched) {
                code = this.shell.execute(item.getBody());
            }
        }
        return code;
    }

    private boolean hits(final String pattern, final String word) {
        return GlobExpander.escapedMatches(
            QuoteRemover.pattern(
                this.shell.expandVariables(pattern, this.shell.environmentMap())
            ),
            word
        );
    }

    private int condition(final ListNode list) {
        return this.errexit.ignoring(() -> this.shell.execute(list));
    }

    private static List<String> positional(final List<String> params) {
        final List<String> words;
        if (params.size() <= 1) {
            words = new ArrayList<>(0);
        } else {
            words = new ArrayList<>(params.subList(1, params.size()));
        }
        return words;
    }
}
