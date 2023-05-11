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


import java.net.UnknownHostException;
import java.util.Optional;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * @author roland
 */
public class ClusterAccess {

    private final KitLogger kitLogger;
    private final ClusterConfiguration clusterConfiguration;

    public ClusterAccess(KitLogger kitLogger, ClusterConfiguration clusterConfiguration) {
        this.kitLogger = kitLogger;
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

    public boolean isOpenShift() {
        try (KubernetesClient client = createDefaultClient()) {
            return OpenshiftHelper.isOpenShift(client);
        } catch (KubernetesClientException exp) {
            Throwable cause = exp.getCause();
            String prefix = cause instanceof UnknownHostException ?
              "Unknown host" : Optional.ofNullable(cause).map(Object::getClass).map(Class::getSimpleName).orElse("");
            kitLogger.warn("Cannot access cluster for detecting mode: %s %s",
                    prefix,
                    cause != null && cause.getMessage() != null ? cause.getMessage() : exp.getMessage());
        }
        return false;
    }

}

