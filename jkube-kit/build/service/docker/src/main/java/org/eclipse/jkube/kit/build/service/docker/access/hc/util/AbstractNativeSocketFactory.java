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
package org.eclipse.jkube.kit.build.service.docker.access.hc.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

/**
 * @author roland
 * @since 08/08/16
 */
public abstract class AbstractNativeSocketFactory implements ConnectionSocketFactory {

    final private String path;

    protected AbstractNativeSocketFactory(String path) {
        this.path = path;
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
        sock.connect(createSocketAddress(path), connectTimeout);
        return sock;
    }

    protected abstract SocketAddress createSocketAddress(String path);
    public abstract Socket createSocket(HttpContext context) throws IOException;
}
