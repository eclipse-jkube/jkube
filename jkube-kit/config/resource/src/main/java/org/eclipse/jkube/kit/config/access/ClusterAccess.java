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
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;

import java.net.UnknownHostException;

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

    public <T extends KubernetesClient> T createDefaultClient() {
        if (isOpenShift()) {
            return (T) createOpenShiftClient();
        }
        return (T) createKubernetesClient();
    }

    private KubernetesClient createKubernetesClient() {
        return new DefaultKubernetesClient(createDefaultConfig());
    }

    private OpenShiftClient createOpenShiftClient() {
        return new DefaultOpenShiftClient(createDefaultConfig());
    }

    private Config createDefaultConfig() {
        return this.clusterConfiguration.getConfig();
    }

    public String getNamespace() {
        return this.clusterConfiguration.getNamespace();
    }

    /**
     * Returns true if this cluster is a traditional OpenShift cluster with the <code>/oapi</code> REST API
     * or supports the new <code>/apis/image.openshift.io</code> API Group
     */
    public boolean isOpenShiftImageStream() {
        if (isOpenShift()) {
            try (final OpenShiftClient client = createOpenShiftClient()) {
                return client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.IMAGE);
            }
        }
        return false;
    }

    public boolean isOpenShift() {
        try (KubernetesClient client = createKubernetesClient()) {
            return OpenshiftHelper.isOpenShift(client);
        } catch (KubernetesClientException exp) {
            Throwable cause = exp.getCause();
            String prefix = cause instanceof UnknownHostException ? "Unknown host " : "";
            kitLogger.warn("Cannot access cluster for detecting mode: %s%s",
                    prefix,
                    cause != null ? cause.getMessage() : exp.getMessage());
        }
        return false;
    }

}

