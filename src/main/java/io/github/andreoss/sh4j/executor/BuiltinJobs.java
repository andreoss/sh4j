/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Job-control, history, and shell-flag state exposed to built-ins.
 *
 * @since 1.0
 */
interface BuiltinJobs {

    /**
     * Stores a trap action.
     *
     * @param signal Signal name (for example EXIT)
     * @param action Shell text to run, or null to remove the trap
     */
    void setTrap(String signal, String action);

    /**
     * Returns the trap table.
     *
     * @return A live view of the trap table
     */
    Map<String, String> traps();

    /**
     * Returns the job manager.
     *
     * @return The job manager for background job control
     */
    JobManager jobManager();

    /**
     * Returns the command history.
     *
     * @return The command history for fc built-in
     */
    CommandHistory history();

    /**
     * Waits for all background jobs spawned by this executor to complete.
     * Used by the {@code wait} built-in.
     */
    void waitForBackgroundJobs();

    /**
     * Returns the last background job identifier.
     *
     * @return The identifier reported by {@code $!}, or 0 when no job ran
     */
    int lastBackgroundPid();

    /**
     * Returns the current shell flags.
     *
     * @return The current shell flags string (e.g. "e", "xu", etc.)
     */
    String shellFlags();

    /**
     * Sets the shell flags string.
     *
     * @param flags The new flags (e.g. "e", "xu")
     */
    void setShellFlags(String flags);
}
