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
package org.eclipse.jkube.kit.build.service.docker.access.hc.unix;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import org.eclipse.jkube.kit.build.service.docker.access.hc.util.AbstractNativeSocketFactory;

import jnr.unixsocket.UnixSocketAddress;
import org.apache.http.protocol.HttpContext;

final class UnixConnectionSocketFactory extends AbstractNativeSocketFactory {

    UnixConnectionSocketFactory(String unixSocketPath) {
        super(unixSocketPath);
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        return new UnixSocket();
    }

    @Override
    protected SocketAddress createSocketAddress(String path) {
        return new UnixSocketAddress(new File(path));
    }

}
