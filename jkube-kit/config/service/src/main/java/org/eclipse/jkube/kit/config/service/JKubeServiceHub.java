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
package org.eclipse.jkube.kit.config.service;

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.service.MigrateService;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceService;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;
import org.eclipse.jkube.kit.config.service.plugins.PluginManager;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import static org.eclipse.jkube.kit.common.util.OpenshiftHelper.isOpenShift;

public class JKubeServiceHub implements Closeable {

    @Getter
    private final JKubeConfiguration configuration;
    @Getter
    private final KitLogger log;
    @Getter
    private final DockerServiceHub dockerServiceHub;
    @Getter
    private final BuildServiceConfig buildServiceConfig;
    @Getter
    private final ResourceServiceConfig resourceServiceConfig;
    private final ClusterAccess clusterAccess;
    @Getter
    @Setter
    private RuntimeMode platformMode;
    private LazyBuilder<JKubeServiceHub, BuildServiceManager> buildServiceManager;
    private LazyBuilder<JKubeServiceHub, PluginManager> pluginManager;
    private final LazyBuilder<JKubeServiceHub, ResourceService> resourceService;
    private LazyBuilder<JKubeServiceHub, PortForwardService> portForwardService;
    private LazyBuilder<JKubeServiceHub, ApplyService> applyService;
    private LazyBuilder<JKubeServiceHub, UndeployService> undeployService;
    private LazyBuilder<JKubeServiceHub, MigrateService> migrateService;
    private LazyBuilder<JKubeServiceHub, DebugService> debugService;
    private LazyBuilder<JKubeServiceHub, HelmService> helmService;
    private LazyBuilder<JKubeServiceHub, ClusterAccess> clusterAccessLazyBuilder;
    private LazyBuilder<JKubeServiceHub, KubernetesClient> kubernetesClientLazyBuilder;
    private final boolean offline;

    @Builder(toBuilder = true)
    public JKubeServiceHub(
            ClusterAccess clusterAccess, RuntimeMode platformMode, KitLogger log,
            DockerServiceHub dockerServiceHub, JKubeConfiguration configuration,
            BuildServiceConfig buildServiceConfig, ResourceServiceConfig resourceServiceConfig,
            LazyBuilder<JKubeServiceHub, ResourceService> resourceService,
            boolean offline) {
        this.clusterAccess = clusterAccess;
        this.platformMode = platformMode;
        this.log = log;
        this.dockerServiceHub = dockerServiceHub;
        this.configuration = configuration;
        this.buildServiceConfig = buildServiceConfig;
        this.resourceServiceConfig = resourceServiceConfig;
        this.resourceService = resourceService;
        this.offline = offline;
        init();
    }

    @Override
    public void close() {
        if (kubernetesClientLazyBuilder.hasInstance()) {
            kubernetesClientLazyBuilder.get(this).close();
        }
        Optional.ofNullable(dockerServiceHub).map(DockerServiceHub::getDockerAccess).ifPresent(DockerAccess::shutdown);
    }

    private void init() {
        Objects.requireNonNull(configuration, "JKubeConfiguration is required");
        Objects.requireNonNull(log, "log is a required parameter");
        Objects.requireNonNull(platformMode, "platformMode is a required parameter");
        initLazyBuilders();
    }

    private void initLazyBuilders() {
        clusterAccessLazyBuilder = new LazyBuilder<>(JKubeServiceHub::initClusterAccessIfNecessary);
        kubernetesClientLazyBuilder = new LazyBuilder<>(hub -> getClusterAccess().createDefaultClient());
        buildServiceManager = new LazyBuilder<>(BuildServiceManager::new);
        pluginManager = new LazyBuilder<>(PluginManager::new);
        applyService = new LazyBuilder<>(ApplyService::new);
        portForwardService = new LazyBuilder<>(hub -> {
            getClient();
            return new PortForwardService(log);
        });
        debugService = new LazyBuilder<>(hub ->
            new DebugService(log, getClient(), portForwardService.get(hub), applyService.get(hub)));
        undeployService = new LazyBuilder<>(hub -> {
            final KubernetesClient client = getClient();
            if (platformMode == RuntimeMode.OPENSHIFT && isOpenShift(client)) {
                return new OpenshiftUndeployService(hub, log);
            }
            return new KubernetesUndeployService(hub, log);
        });
        migrateService = new LazyBuilder<>(hub -> new MigrateService(getConfiguration().getBasedir(), log));
        helmService = new LazyBuilder<>(hub -> new HelmService(hub.getConfiguration(), hub.getResourceServiceConfig(), log));
    }

    private ClusterAccess initClusterAccessIfNecessary() {
        if (offline) {
            throw new IllegalArgumentException("Connection to Cluster required. Please check if offline mode is set to false");
        }
        if (clusterAccess != null) {
            return clusterAccess;
        }
        return new ClusterAccess(ClusterConfiguration.from(
          System.getProperties(), configuration.getProject().getProperties()).build());
    }

    public RuntimeMode getRuntimeMode() {
        return platformMode;
    }

    public BuildService getBuildService() {
        return buildServiceManager.get(this).resolveBuildService();
    }

    public PluginManager getPluginManager() {
        return pluginManager.get(this);
    }

    public ResourceService getResourceService() {
        return resourceService.get(this);
    }

    public ApplyService getApplyService() {
        return applyService.get(this);
    }

    public UndeployService getUndeployService() {
        return undeployService.get(this);
    }

    public MigrateService getMigrateService() {
        return migrateService.get(this);
    }

    public PortForwardService getPortForwardService() {
        return portForwardService.get(this);
    }

    public DebugService getDebugService() {
        return debugService.get(this);
    }

    public HelmService getHelmService() {
        return helmService.get(this);
    }

    public KubernetesClient getClient() {
        return kubernetesClientLazyBuilder.get(this);
    }

    public ClusterAccess getClusterAccess() {
        return clusterAccessLazyBuilder.get(this);
    }

}
