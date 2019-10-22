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
package io.jkube.kit.build.service.docker.access.hc.win;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import io.jkube.kit.build.service.docker.access.hc.util.AbstractNativeSocketFactory;
import io.jkube.kit.common.KitLogger;
import org.apache.http.protocol.HttpContext;

final class NpipeConnectionSocketFactory extends AbstractNativeSocketFactory {

	// Logging
    private final KitLogger log;

    NpipeConnectionSocketFactory(String npipePath, KitLogger log) {
        super(npipePath);
        this.log = log;
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        return new NamedPipe(log);
    }

    @Override
    protected SocketAddress createSocketAddress(String path) {
        return new NpipeSocketAddress(new File(path));
    }
}
