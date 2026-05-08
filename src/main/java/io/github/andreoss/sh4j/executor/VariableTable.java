/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Live shell-variable table honouring the {@code allexport} option
 * (POSIX 2.14 set -a): while it is on, every variable assigned a value is
 * marked for export.
 *
 * @since 1.0
 */
final class VariableTable extends AbstractMap<String, String> {

    /** The variables. */
    private final Map<String, String> vars;

    /** Names of variables visible to child processes. */
    private final Set<String> exported;

    /** True while the allexport option is on. */
    private boolean auto;

    /**
     * Ctor.
     *
     * @param vars The variables, owned by this table
     * @param exported Names of variables visible to child processes
     */
    VariableTable(final Map<String, String> vars, final Set<String> exported) {
        super();
        this.vars = vars;
        this.exported = exported;
    }

    @Override
    public String put(final String key, final String value) {
        this.mark(key);
        return this.vars.put(key, value);
    }

    @Override
    public void putAll(final Map<? extends String, ? extends String> others) {
        for (final Map.Entry<? extends String, ? extends String> entry : others.entrySet()) {
            if (!Objects.equals(this.vars.get(entry.getKey()), entry.getValue())) {
                this.mark(entry.getKey());
            }
        }
        this.vars.putAll(others);
    }

    @Override
    public String get(final Object key) {
        return this.vars.get(key);
    }

    @Override
    public boolean containsKey(final Object key) {
        return this.vars.containsKey(key);
    }

    @Override
    public String remove(final Object key) {
        return this.vars.remove(key);
    }

    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        return this.vars.entrySet();
    }

    /**
     * Turns the allexport option on or off.
     *
     * @param enabled True to export every assigned variable
     */
    void automatic(final boolean enabled) {
        this.auto = enabled;
    }

    private void mark(final String name) {
        if (this.auto && !"LINENO".equals(name)) {
            this.exported.add(name);
        }
    }
}
