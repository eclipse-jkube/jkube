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
package io.jkube.kit.config.access;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.OpenshiftHelper;
import io.jkube.kit.config.resource.RuntimeMode;

import java.net.UnknownHostException;

/**
 * @author roland
 * @since 17/07/16
 */
public class ClusterAccess {

    private ClusterConfiguration clusterConfiguration;

    private KubernetesClient client;

    public ClusterAccess(ClusterConfiguration clusterConfiguration) {
        this.clusterConfiguration = clusterConfiguration;

        if (this.clusterConfiguration == null) {
            this.clusterConfiguration = new ClusterConfiguration.Builder().build();
        }

        this.client = null;
    }

    @Deprecated
    public ClusterAccess(String namespace) {
        ClusterConfiguration.Builder clusterConfigurationBuilder = new ClusterConfiguration.Builder();
        clusterConfigurationBuilder.namespace(namespace);
        this.clusterConfiguration = clusterConfigurationBuilder.build();
        this.client = null;
    }

    public ClusterAccess(ClusterConfiguration clusterConfiguration, KubernetesClient client) {
        this.clusterConfiguration = clusterConfiguration;
        this.client = client;
    }

    public <T extends KubernetesClient> T createDefaultClient(KitLogger log) {
        if (isOpenShift(log)) {
            return (T) createOpenShiftClient();
        }

        return (T) createKubernetesClient();
    }

    public KubernetesClient createKubernetesClient() {
        return new DefaultKubernetesClient(createDefaultConfig());
    }

    public OpenShiftClient createOpenShiftClient() {
        return new DefaultOpenShiftClient(createDefaultConfig());
    }

    // ============================================================================

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
    public boolean isOpenShiftImageStream(KitLogger log) {
        if (isOpenShift(log)) {
            OpenShiftClient openShiftClient = null;
            if (this.client == null) {
                openShiftClient = createOpenShiftClient();
            } else if (this.client instanceof OpenShiftClient) {
                openShiftClient = (OpenShiftClient) this.client;
            } else if (this.client.isAdaptable(OpenShiftClient.class)) {
                openShiftClient = client.adapt(OpenShiftClient.class);
            } else {
                return false;
            }
            return openShiftClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.IMAGE);
        }
        return false;
    }

    public boolean isOpenShift(KitLogger log) {
        try {
            return this.client == null ?
                    OpenshiftHelper.isOpenShift(createKubernetesClient()) :
                    OpenshiftHelper.isOpenShift(this.client);
        } catch (KubernetesClientException exp) {
            Throwable cause = exp.getCause();
            String prefix = cause instanceof UnknownHostException ? "Unknown host " : "";
            log.warn("Cannot access cluster for detecting mode: %s%s",
                    prefix,
                    cause != null ? cause.getMessage() : exp.getMessage());
        }
        return false;
    }

    public RuntimeMode resolveRuntimeMode(RuntimeMode mode, KitLogger log) {
        RuntimeMode resolvedMode;
        if (mode == null) {
            mode = RuntimeMode.DEFAULT;
        }
        if (mode.isAuto()) {
            resolvedMode = isOpenShiftImageStream(log) ? RuntimeMode.openshift : RuntimeMode.kubernetes;
        } else {
            resolvedMode = mode;
        }
        return resolvedMode;
    }
}

