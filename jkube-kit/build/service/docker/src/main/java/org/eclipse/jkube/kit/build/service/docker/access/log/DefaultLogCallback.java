/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.build.service.docker.access.log;

import org.eclipse.jkube.kit.build.service.docker.helper.Timestamp;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author roland
 * @since 26/09/15
 */
public class DefaultLogCallback implements LogCallback {

    private static Map<String, SharedPrintStream> printStreamMap = new HashMap<>();

    private final LogOutputSpec outputSpec;
    private SharedPrintStream sps;

    public DefaultLogCallback(LogOutputSpec outputSpec) {
        this.outputSpec = outputSpec;
    }

    @Override
    public synchronized void open() throws IOException {
        if (this.sps == null) {
            String file = outputSpec.getFile();
            if (outputSpec.isLogStdout() || file == null) {
                this.sps = new SharedPrintStream(System.out);
            } else {
                SharedPrintStream cachedPs = printStreamMap.get(file);
                if (cachedPs == null) {
                    FileUtil.createParentDirs(new File(file));
                    PrintStream ps = new PrintStream(new FileOutputStream(file), true);
                    cachedPs = new SharedPrintStream(ps);
                    printStreamMap.put(file, cachedPs);
                } else {
                    cachedPs.allocate();
                }
                this.sps = cachedPs;
            }
        }
    }

    @Override
    public synchronized void close() {
        if (this.sps != null) {
            if (sps.close()) {
                String file = outputSpec.getFile();
                if (file != null) {
                    printStreamMap.remove(file);
                }
                this.sps = null;
            }
        }
    }

    private PrintStream ps() {
        return sps.getPrintStream();
    }

    @Override
    public void log(int type, Timestamp timestamp, String txt) {
        addLogEntry(ps(), new LogEntry(type, timestamp, txt));
    }

    @Override
    public void error(String error) {
        ps().println(error);
    }

    private void addLogEntry(PrintStream ps, LogEntry logEntry) {
        // TODO: Add the entry to a queue, and let the queue be picked up with a small delay from an extra
        // thread which then can sort the entries by time before printing it out in order to avoid race conditions.

        LogOutputSpec spec = outputSpec;
        if (spec == null) {
            spec = LogOutputSpec.DEFAULT;
        }
        String text = logEntry.getText();
        ps.println(spec.getPrompt(spec.isUseColor(),logEntry.getTimestamp()) + text);
    }

        // A single log-entry
    private static class LogEntry implements Comparable<LogEntry> {
        private final int type;
        private final Timestamp timestamp;
        private final String text;

        public LogEntry(int type, Timestamp timestamp, String text) {
            this.type = type;
            this.timestamp = timestamp;
            this.text = text;
        }

        public int getType() {
            return type;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public String getText() {
            return text;
        }

        @Override
        public int compareTo(LogEntry entry) {
            return timestamp.compareTo(entry.timestamp);
        }
    }

}
