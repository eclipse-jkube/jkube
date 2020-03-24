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
package org.eclipse.jkube.kit.config.resource;


public class ServiceAccountConfig {
    private String name;

    private String deploymentRef;

    public String getName() {
        return name;
    }

    public String getDeploymentRef() {
        return deploymentRef;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDeploymentRef(String deploymentRef) {
        this.deploymentRef = deploymentRef;
    }

    public static class Builder {
        private ServiceAccountConfig serviceAccountConfig = new ServiceAccountConfig();

        public Builder() { }

        public Builder(ServiceAccountConfig serviceAccountConfig) {
            if (serviceAccountConfig != null) {
                this.serviceAccountConfig.name = serviceAccountConfig.getName();
                this.serviceAccountConfig.deploymentRef = serviceAccountConfig.getDeploymentRef();
            }
        }

        public Builder withName(String name) {
            this.serviceAccountConfig.name = name;
            return this;
        }

        public Builder withDeploymentRef(String deploymentRef) {
            this.serviceAccountConfig.deploymentRef = deploymentRef;
            return this;
        }

        public ServiceAccountConfig build() {
            return serviceAccountConfig;
        }
    }
}

