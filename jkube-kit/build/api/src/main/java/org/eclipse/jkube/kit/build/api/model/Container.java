/*
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
package org.eclipse.jkube.kit.build.api.model;

import java.util.Map;

/**
 * Interface representing a container
 *
 * @author roland
 * @since 16/07/15
 */
public interface Container {

    long getCreated();

    String getId();

    String getImage();

    Map<String, String> getLabels();

    String getName();

    String getNetworkMode();

    Map<String, PortBinding> getPortBindings();

    boolean isRunning();

    /**
     * IP Address of the container if provided
     *
     * @return the IP address of the container or <code>null</code> if not provided.
     */
    String getIPAddress();

    /**
     * Return IP Addresses of custom networks, mapped to the network name as the key.
     * @return The mapping of network names to IP addresses, or null if none provided.
     */
    Map<String, String> getCustomNetworkIpAddresses();

    /**
     * Exit code of the container if it already has excited
     *
     * @return exit code if the container has excited, <code>null</code> if it is still running. Also null,
     * if the implementation doesn't support an exit code.
     */
    Integer getExitCode();

    class PortBinding {
        private final String hostIp;
        private final Integer hostPort;

        public PortBinding(Integer hostPort, String hostIp) {
            this.hostPort = hostPort;
            this.hostIp = hostIp;
        }

        public String getHostIp() {
            return hostIp;
        }

        public Integer getHostPort() {
            return hostPort;
        }
    }
}


