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

class NpipeSocketAddress extends java.net.SocketAddress {

	private static final long serialVersionUID = 1L;

	private String path;

    NpipeSocketAddress(File path) {
        this.path = path.getPath();
    }

    public String path() {
        return path;
    }

    @Override
    public String toString() {
        return "NpipeSocketAddress{path=" + path + "}";
    }

    @Override
    public boolean equals(Object _other) {
        return _other instanceof NpipeSocketAddress && path.equals(((NpipeSocketAddress) _other).path);
    }
}
