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
package io.jkube.kit.build.service.docker.access.log;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

class SharedPrintStream {
    private PrintStream printStream;

    private AtomicInteger numUsers;

    SharedPrintStream(PrintStream ps) {
        this.printStream = ps;
        this.numUsers = new AtomicInteger(1);
    }

    PrintStream getPrintStream() {
        return printStream;
    }

    void allocate() {
        numUsers.incrementAndGet();
    }

    boolean close() {
        int nrUsers = numUsers.decrementAndGet();
        if (nrUsers == 0 && printStream != System.out) {
            printStream.close();
            return true;
        } else {
            return false;
        }
    }
}
