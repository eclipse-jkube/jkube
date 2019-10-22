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
package io.jkube.kit.config.resource;

import java.util.Collections;
import java.util.List;

/**
 * @author roland
 * @since 22/03/16
 */
public class ServiceConfig {

    // Service name
    private String name;

    // Ports to expose
    List<Port> ports;

    // Whether this is a headless service
    private boolean headless = false;

    // If the expose label is added to the service
    private boolean expose = false;

    // Service type
    private String type;

    // Whether to normalize service port numbering
    private boolean normalizePort = false;

    public String getName() {
        return name;
    }

    public List<Port> getPorts() {
        return ports != null ? ports : Collections.<Port>emptyList();
    }

    public boolean isHeadless() {
        return headless;
    }

    public boolean isExpose() {
        return expose;
    }

    public String getType() {
        return type;
    }

    public boolean isNormalizePort() {  return normalizePort; }

    // =============================================================

    public static class Builder {
        private ServiceConfig config = new ServiceConfig();

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder ports(List<Port> ports) {
            config.ports = ports;
            return this;
        }

        public Builder headless(boolean headless) {
            config.headless = headless;
            return this;
        }

        public Builder expose(boolean expose) {
            config.expose = expose;
            return this;
        }

        public Builder type(String type) {
            config.type = type;
            return this;
        }

        public Builder normalizePort(boolean normalize) {
            config.normalizePort = normalize;
            return this;
        }

        public ServiceConfig build() {
            return config;
        }
    }


    // =============================================================

    public static class Port {

        // Protocol to use. Can be either "tcp" or "udp"
        String protocol;

        // Container port to expose
        int port;

        // Target port to expose
        int targetPort;

        // Port to expose from the port
        Integer nodePort;

        // Name of the port
        String name;

        public ServiceProtocol getProtocol() {
            return protocol != null ? ServiceProtocol.valueOf(protocol.toUpperCase()) : null;
        }

        public int getPort() {
            return port;
        }

        public int getTargetPort() {
            return targetPort;
        }

        public Integer getNodePort() {
            return nodePort;
        }

        public String getName() {
            return name;
        }

        // =====================================================================================

        public static class Builder {

            Port config = new Port();

            public static Builder from(Port port) {
                Builder ret = new Builder();
                ret.config = port;
                return ret;
            }

            public Builder name(String name) {
                config.name = name;
                return this;
            }

            public Builder protocol(ServiceProtocol protocol) {
                config.protocol = protocol != null ? protocol.name() : null;
                return this;
            }

            public Builder protocol(String protocol) {
                config.protocol = protocol;
                return this;
            }

            public Builder port(int port) {
                config.port = port;
                return this;
            }

            public Builder targetPort(int targetPort) {
                config.targetPort = targetPort;
                return this;
            }

            public Builder nodePort(Integer nodePort) {
                config.nodePort = nodePort;
                return this;
            }

            public Port build() {
                return config;
            }
        }
    }
}
