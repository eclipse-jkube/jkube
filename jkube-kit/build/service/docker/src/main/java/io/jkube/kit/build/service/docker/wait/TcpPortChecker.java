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
package io.jkube.kit.build.service.docker.wait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Check whether a given TCP port is available
 */
public class TcpPortChecker implements WaitChecker {

    private static final int TCP_PING_TIMEOUT = 500;

    private final List<Integer> ports;

    private final List<InetSocketAddress> pending;

    public TcpPortChecker(String host, List<Integer> ports) {
        this.ports = ports;

        this.pending = new ArrayList<>();
        for (int port : ports) {
            this.pending.add(new InetSocketAddress(host, port));
        }

    }

    public List<Integer> getPorts() {
        return ports;
    }

    @Override
    public boolean check() {
        Iterator<InetSocketAddress> iter = pending.iterator();

        while (iter.hasNext()) {
            InetSocketAddress address = iter.next();

            try {
                Socket s = new Socket();
                s.connect(address, TCP_PING_TIMEOUT);
                s.close();
                iter.remove();
            } catch (IOException e) {
                // Ports isn't opened, yet. So don't remove from queue.
                // Can happen and is part of the flow
            }
        }
        return pending.isEmpty();
    }

    @Override
    public void cleanUp() {
        // No cleanup required
    }

    @Override
    public String getLogLabel() {
        return "on tcp port '" + pending + "'";
    }
}
