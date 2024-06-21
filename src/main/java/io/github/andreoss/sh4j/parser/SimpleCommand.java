/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;

public class SimpleCommand extends Node {
    private String name;
    private final List<String> arguments = new ArrayList<>();
    private final List<Redirection> redirections = new ArrayList<>();
    private final List<String> assignments = new ArrayList<>();

    public SimpleCommand(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public List<Redirection> getRedirections() {
        return redirections;
    }

    public List<String> getAssignments() {
        return assignments;
    }

    public void addArgument(String arg) {
        arguments.add(arg);
    }

    public void addRedirection(Redirection redir) {
        redirections.add(redir);
    }

    public void addAssignment(String assignment) {
        assignments.add(assignment);
    }
}
