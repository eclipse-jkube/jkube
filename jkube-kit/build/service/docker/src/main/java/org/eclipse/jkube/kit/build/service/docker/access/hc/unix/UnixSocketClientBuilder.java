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

import org.eclipse.jkube.kit.build.service.docker.access.hc.util.AbstractNativeClientBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.apache.http.conn.socket.ConnectionSocketFactory;

public class UnixSocketClientBuilder extends AbstractNativeClientBuilder {

    public UnixSocketClientBuilder(String unixSocketPath, int maxConnections, KitLogger log) {
        super(unixSocketPath, maxConnections, log);
    }

    @Override
    protected ConnectionSocketFactory getConnectionSocketFactory() {
        return new UnixConnectionSocketFactory(path);
    }

    @Override
    protected String getProtocol() {
        return "unix";
    }
}
