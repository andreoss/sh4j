/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The {@code printf} built-in: writes the format with its escapes decoded
 * and its conversions filled from the arguments, reusing the format while
 * arguments are left.
 *
 * @since 1.0
 */
final class Printf {

    /** Shell that runs the built-in. */
    private final BuiltinContext shell;

    /** Operands: the format, then the arguments. */
    private final List<String> operands;

    /**
     * Ctor.
     *
     * @param shell Shell that runs the built-in
     * @param operands Operands: the format, then the arguments
     */
    Printf(final BuiltinContext shell, final List<String> operands) {
        this.shell = shell;
        this.operands = operands;
    }

    /**
     * Runs the built-in.
     *
     * @return The exit status: 0, 1 after a conversion error, 2 without a
     *  format
     */
    int run() {
        List<String> words = this.operands;
        if (!words.isEmpty() && "--".equals(words.get(0))) {
            words = words.subList(1, words.size());
        }
        final int status;
        if (words.isEmpty()) {
            this.shell.errStream().println("sh: printf: usage: printf format [argument ...]");
            status = 2;
        } else {
            final PrintfArguments args = new PrintfArguments(
                words.subList(1, words.size()), this.shell.errStream()
            );
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean again = true;
            while (again) {
                final int before = args.position();
                again = Printf.pass(words.get(0), args, out)
                    && args.remain() && args.position() > before;
            }
            this.write(out.toByteArray());
            if (args.failed()) {
                status = 1;
            } else {
                status = 0;
            }
        }
        return status;
    }

    private void write(final byte[] bytes) {
        if (bytes.length > 0) {
            try {
                this.shell.outStream().bytes().write(bytes);
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
            this.shell.outStream().flush();
        }
    }

    private static boolean pass(
        final String format, final PrintfArguments args, final ByteArrayOutputStream out
    ) {
        final StringBuilder plain = new StringBuilder(format.length());
        boolean going = true;
        int idx = 0;
        while (going && idx < format.length()) {
            final char chr = format.charAt(idx);
            if (chr == '\\') {
                PrintfEscapes.flush(plain, out);
                idx = PrintfEscapes.format(format, idx, out);
            } else if (chr == '%') {
                PrintfEscapes.flush(plain, out);
                final PrintfSpec spec = PrintfSpec.read(format, idx, args);
                going = Printf.convert(spec, args, out);
                if (!going && spec.conversion() != 'b') {
                    args.fail(format.substring(idx, spec.end()), "invalid directive");
                }
                idx = spec.end();
            } else {
                plain.append(chr);
                idx += 1;
            }
        }
        PrintfEscapes.flush(plain, out);
        return going;
    }

    private static boolean convert(
        final PrintfSpec spec, final PrintfArguments args, final ByteArrayOutputStream out
    ) {
        boolean going = true;
        switch (spec.conversion()) {
            case '%' -> out.write('%');
            case 'd', 'i', 'o', 'u', 'x', 'X' -> out.writeBytes(spec.integer(args.integer()));
            case 'f', 'F', 'e', 'E', 'g', 'G' -> out.writeBytes(spec.real(args.real()));
            case 's' -> out.writeBytes(
                spec.text(args.text().getBytes(StandardCharsets.UTF_8))
            );
            case 'c' -> out.writeBytes(
                spec.pad(
                    Character.toString(args.text().codePoints().findFirst().orElse(0))
                        .getBytes(StandardCharsets.UTF_8)
                )
            );
            case 'b' -> {
                final ByteArrayOutputStream text = new ByteArrayOutputStream();
                going = PrintfEscapes.argument(args.text(), text);
                out.writeBytes(spec.text(text.toByteArray()));
            }
            default -> going = false;
        }
        return going;
    }
}
