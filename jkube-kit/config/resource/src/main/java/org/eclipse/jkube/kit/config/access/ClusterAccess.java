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
package org.eclipse.jkube.kit.config.access;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

/**
 * @author roland
 */
public class ClusterAccess {

    private final ClusterConfiguration clusterConfiguration;

    public ClusterAccess(ClusterConfiguration clusterConfiguration) {
        this.clusterConfiguration = clusterConfiguration == null ?
            ClusterConfiguration.builder().build() : clusterConfiguration;
    }

    public KubernetesClient createDefaultClient() {
        return new KubernetesClientBuilder().withConfig(createDefaultConfig()).build();
    }

    private Config createDefaultConfig() {
        return this.clusterConfiguration.getConfig();
    }

    public String getNamespace() {
        return this.clusterConfiguration.getNamespace();
    }

}

