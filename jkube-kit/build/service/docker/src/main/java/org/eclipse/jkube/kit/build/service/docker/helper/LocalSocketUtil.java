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
package org.eclipse.jkube.kit.build.service.docker.helper;

import java.io.File;
import java.io.IOException;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

/**
 * Utilities around socket connections
 *
 * @author roland
 * @since 22/09/16
 */
public class LocalSocketUtil {

    private LocalSocketUtil() { }

    /**
     * Check whether we can connect to a local Unix socket
     *
     * @param path file provided
     * @return whether we can connect or not
     */
    public static boolean canConnectUnixSocket(File path) {
        try (UnixSocketChannel channel = UnixSocketChannel.open()) {
            return channel.connect(new UnixSocketAddress(path));
        } catch (IOException e) {
            return false;
        }
    }
}
