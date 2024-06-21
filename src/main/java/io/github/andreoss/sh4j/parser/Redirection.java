/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

public class Redirection {
    private final int fd;
    private final RedirectionType type;
    private final String target;

    public Redirection(int fd, RedirectionType type, String target) {
        this.fd = fd;
        this.type = type;
        this.target = target;
    }

    public int getFd() {
        return fd;
    }

    public RedirectionType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public static Redirection input(int fd, String target) {
        return new Redirection(fd, RedirectionType.INPUT, target);
    }

    public static Redirection output(int fd, String target) {
        return new Redirection(fd, RedirectionType.OUTPUT, target);
    }

    public static Redirection append(int fd, String target) {
        return new Redirection(fd, RedirectionType.APPEND, target);
    }
}
