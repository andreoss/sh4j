/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends Node {
    public enum Separator {
        NONE,
        SEMI,
        AMP,
        AND_IF,
        OR_IF
    }

    private final List<Pipeline> pipelines = new ArrayList<>();
    private final List<Separator> separators = new ArrayList<>();

    public List<Pipeline> getPipelines() {
        return pipelines;
    }

    public List<Separator> getSeparators() {
        return separators;
    }

    public void addPipeline(Pipeline pipeline, Separator sep) {
        pipelines.add(pipeline);
        separators.add(sep);
    }
}
