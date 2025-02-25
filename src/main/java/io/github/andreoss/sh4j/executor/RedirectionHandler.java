/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.UnaryOperator;

public final class RedirectionHandler {

    public record StreamSwap(PrintStream out, PrintStream err,
                             PrintStream savedOut, PrintStream savedErr) {}

    public static StreamSwap redirectOutputForBuiltin(List<Redirection> redirs,
                                                     PrintStream currentOut,
                                                     PrintStream currentErr,
                                                     UnaryOperator<String> expander) {
        PrintStream out = currentOut;
        PrintStream err = currentErr;
        PrintStream savedOut = null;
        PrintStream savedErr = null;
        for (Redirection r : redirs) {
            RedirectionType t = r.getType();
            int fd = r.getFd();
            String target = expander.apply(r.getTarget());
            if (t == RedirectionType.OUTPUT || t == RedirectionType.CLOBBER) {
                try {
                    PrintStream fileOut = new PrintStream(new File(target));
                    if (fd == 1) { savedOut = out; out = fileOut; }
                    else if (fd == 2) { savedErr = err; err = fileOut; }
                } catch (IOException e) { /* ignore */ }
            } else if (t == RedirectionType.APPEND) {
                try {
                    PrintStream fileOut = new PrintStream(new FileOutputStream(target, true));
                    if (fd == 1) { savedOut = out; out = fileOut; }
                    else if (fd == 2) { savedErr = err; err = fileOut; }
                } catch (IOException e) { /* ignore */ }
            } else if (t == RedirectionType.DUP_OUTPUT) {
                if ("-".equals(target)) {
                    try {
                        PrintStream nullPs = new PrintStream(new FileOutputStream("/dev/null"));
                        if (fd == 1) { savedOut = out; out = nullPs; }
                        else if (fd == 2) { savedErr = err; err = nullPs; }
                    } catch (IOException e) { /* ignore */ }
                } else if ("1".equals(target) && fd == 2) {
                    savedErr = err;
                    err = out;
                } else if ("2".equals(target) && fd == 1) {
                    savedOut = out;
                    out = err;
                }
            }
        }
        return new StreamSwap(out, err, savedOut, savedErr);
    }

    public static void setupRedirections(ProcessBuilder pb,
                                         List<Redirection> redirections,
                                         UnaryOperator<String> expander) {
        for (Redirection redir : redirections) {
            int fd = redir.getFd();
            File file = new File(expander.apply(redir.getTarget()));
            RedirectionType type = redir.getType();
            if (type == RedirectionType.DUP_INPUT) {
                if ("-".equals(redir.getTarget()) && fd == 0) {
                    pb.redirectInput(new File("/dev/null"));
                }
            } else if (type == RedirectionType.DUP_OUTPUT) {
                if ("-".equals(redir.getTarget())) {
                    if (fd == 1) pb.redirectOutput(new File("/dev/null"));
                    else if (fd == 2) pb.redirectError(new File("/dev/null"));
                } else if ("1".equals(redir.getTarget()) && fd == 2) {
                    pb.redirectErrorStream(true);
                } else if ("2".equals(redir.getTarget()) && fd == 1) {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/stderr")));
                }
            } else if (type == RedirectionType.READWRITE) {
                if (fd == 0) pb.redirectInput(file);
                else if (fd == 1) pb.redirectOutput(ProcessBuilder.Redirect.to(file));
                else if (fd == 2) pb.redirectError(ProcessBuilder.Redirect.to(file));
            } else if (type == RedirectionType.INPUT) {
                if (fd == 0) pb.redirectInput(file);
            } else if (type == RedirectionType.HERE_DOC || type == RedirectionType.HERE_DOC_DASH) {
                String content = redir.getHereDocContent();
                if (content != null) {
                    content = expander.apply(content);
                }
                File src = heredocFile(content);
                if (src != null) {
                    if (fd == 0) pb.redirectInput(src);
                }
            } else if (type == RedirectionType.OUTPUT || type == RedirectionType.CLOBBER) {
                if (fd == 1) pb.redirectOutput(ProcessBuilder.Redirect.to(file));
                else if (fd == 2) pb.redirectError(ProcessBuilder.Redirect.to(file));
            } else if (type == RedirectionType.APPEND) {
                if (fd == 1) pb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
                else if (fd == 2) pb.redirectError(ProcessBuilder.Redirect.appendTo(file));
            }
        }
    }

    static File heredocFile(String content) {
        if (content == null) return null;
        try {
            File temp = File.createTempFile("sh4j_heredoc", ".tmp");
            temp.deleteOnExit();
            try (FileWriter w = new FileWriter(temp)) {
                w.write(content);
            }
            return temp;
        } catch (IOException e) {
            return heredocFileIoFailure();
        }
    }

    @GeneratedDefensive
    private static File heredocFileIoFailure() {
        return null;
    }
}
