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
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;
import org.eclipse.jkube.kit.config.service.plugins.PluginManager;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import static org.eclipse.jkube.kit.common.util.OpenshiftHelper.isOpenShift;

/**
 * @author nicola
 */
public class JKubeServiceHub implements Closeable {

    @Getter
    private final JKubeConfiguration configuration;
    @Getter
    private final KitLogger log;
    @Getter
    private final DockerServiceHub dockerServiceHub;
    @Getter
    private final BuildServiceConfig buildServiceConfig;
    private final ClusterAccess clusterAccess;
    @Getter
    @Setter
    private RuntimeMode platformMode;
    private LazyBuilder<BuildServiceManager> buildServiceManager;
    private LazyBuilder<PluginManager> pluginManager;
    private final LazyBuilder<ResourceService> resourceService;
    private LazyBuilder<PortForwardService> portForwardService;
    private LazyBuilder<ApplyService> applyService;
    private LazyBuilder<UndeployService> undeployService;
    private LazyBuilder<MigrateService> migrateService;
    private LazyBuilder<DebugService> debugService;
    private LazyBuilder<HelmService> helmService;
    private LazyBuilder<ClusterAccess> clusterAccessLazyBuilder;
    private LazyBuilder<KubernetesClient> kubernetesClientLazyBuilder;
    private final boolean offline;

    @Builder
    public JKubeServiceHub(
            ClusterAccess clusterAccess, RuntimeMode platformMode, KitLogger log,
            DockerServiceHub dockerServiceHub, JKubeConfiguration configuration,
            BuildServiceConfig buildServiceConfig,
            LazyBuilder<ResourceService> resourceService, boolean offline) {
        this.clusterAccess = clusterAccess;
        this.platformMode = platformMode;
        this.log = log;
        this.dockerServiceHub = dockerServiceHub;
        this.configuration = configuration;
        this.buildServiceConfig = buildServiceConfig;
        this.resourceService = resourceService;
        this.offline = offline;
        init();
    }

    @Override
    public void close() {
        if (kubernetesClientLazyBuilder.hasInstance()) {
            kubernetesClientLazyBuilder.get().close();
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
        clusterAccessLazyBuilder = new LazyBuilder<>(this::initClusterAccessIfNecessary);
        kubernetesClientLazyBuilder = new LazyBuilder<>(() -> getClusterAccess().createDefaultClient());
        buildServiceManager = new LazyBuilder<>(() -> new BuildServiceManager(this));
        pluginManager = new LazyBuilder<>(() -> new PluginManager(this));
        applyService = new LazyBuilder<>(() -> new ApplyService(getClient(), log));
        portForwardService = new LazyBuilder<>(() -> {
            getClient();
            return new PortForwardService(log);
        });
        debugService = new LazyBuilder<>(() ->
            new DebugService(log, getClient(), portForwardService.get(), applyService.get()));
        undeployService = new LazyBuilder<>(() -> {
            final KubernetesClient client = getClient();
            if (platformMode == RuntimeMode.OPENSHIFT && isOpenShift(client)) {
                return new OpenshiftUndeployService(this, log);
            }
            return new KubernetesUndeployService(this, log);
        });
        migrateService = new LazyBuilder<>(() -> new MigrateService(getConfiguration().getBasedir(), log));
        helmService = new LazyBuilder<>(() -> new HelmService(getConfiguration(), log));
    }

    private ClusterAccess initClusterAccessIfNecessary() {
        if (offline) {
            throw new IllegalArgumentException("Connection to Cluster required. Please check if offline mode is set to false");
        }
        if (clusterAccess != null) {
            return clusterAccess;
        }
        return new ClusterAccess(log,
            ClusterConfiguration.from(System.getProperties(), configuration.getProject().getProperties()).build());
    }

    public RuntimeMode getRuntimeMode() {
        return platformMode;
    }

    public BuildService getBuildService() {
        return buildServiceManager.get().resolveBuildService();
    }

    public PluginManager getPluginManager() {
        return pluginManager.get();
    }

    public ResourceService getResourceService() {
        return resourceService.get();
    }

    public ApplyService getApplyService() {
        return applyService.get();
    }

    public UndeployService getUndeployService() {
        return undeployService.get();
    }

    public MigrateService getMigrateService() {
        return migrateService.get();
    }

    public PortForwardService getPortForwardService() {
        return portForwardService.get();
    }

    public DebugService getDebugService() {
        return debugService.get();
    }

    public HelmService getHelmService() {
        return helmService.get();
    }

    public KubernetesClient getClient() {
        return kubernetesClientLazyBuilder.get();
    }

    public ClusterAccess getClusterAccess() {
        return clusterAccessLazyBuilder.get();
    }
}
