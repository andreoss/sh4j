/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or constructor as defensive code that is not testable by
 * design (for example, an IOException catch on /dev/null) so JaCoCo and
 * other code-coverage tooling can ignore it.
 *
 * <p>JaCoCo's filter excludes methods annotated with any annotation whose
 * simple name contains "Generated"; this annotation rides that contract.</p>
 * @since 1.0
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface GeneratedDefensive {
}
