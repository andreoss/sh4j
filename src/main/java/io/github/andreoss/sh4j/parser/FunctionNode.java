/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

public class FunctionNode extends Node {
    private final String name;
    private final Node body;

    public FunctionNode(String name, Node body) {
        this.name = name;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public Node getBody() {
        return body;
    }
}
