/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;

public class Pipeline extends Node {
    private final List<Node> commands = new ArrayList<>();
    private final boolean bang;

    public Pipeline(boolean bang) {
        this.bang = bang;
    }

    public List<Node> getCommands() {
        return commands;
    }

    public boolean isBang() {
        return bang;
    }

    public void addCommand(Node cmd) {
        commands.add(cmd);
    }
}
