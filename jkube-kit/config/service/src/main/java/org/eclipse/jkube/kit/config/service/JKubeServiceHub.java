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
    @Getter
    private KubernetesClient client;
    @Getter
    private ClusterAccess clusterAccess;
    @Getter
    @Setter
    private RuntimeMode platformMode;
    private LazyBuilder<BuildServiceManager> buildServiceManager;
    private LazyBuilder<ResourceService> resourceService;
    private LazyBuilder<PortForwardService> portForwardService;
    private LazyBuilder<ApplyService> applyService;
    private LazyBuilder<UndeployService> undeployService;
    private LazyBuilder<MigrateService> migrateService;
    private LazyBuilder<DebugService> debugService;
    private LazyBuilder<HelmService> helmService;
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

    private void init() {
        Objects.requireNonNull(configuration, "JKubeKitConfiguration is required");
        Objects.requireNonNull(log, "log is a required parameter");
        Objects.requireNonNull(platformMode, "platformMode is a required parameter");

        initClusterAccessAndLazyBuilders();
    }

    @Override
    public void close() {
        Optional.ofNullable(client).ifPresent(KubernetesClient::close);
        Optional.ofNullable(dockerServiceHub).map(DockerServiceHub::getDockerAccess).ifPresent(DockerAccess::shutdown);
    }

    public RuntimeMode getRuntimeMode() {
        return platformMode;
    }

    public BuildService getBuildService() {
        return buildServiceManager.get().resolveBuildService();
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

    private void initClusterAccessAndLazyBuilders() {
        if (!offline) {
            if (clusterAccess == null) {
                clusterAccess = new ClusterAccess(log,
                        ClusterConfiguration.from(System.getProperties(), configuration.getProject().getProperties()).build());
            }
            this.client = clusterAccess.createDefaultClient();
        }
        buildServiceManager = new LazyBuilder<>(() -> new BuildServiceManager(this));
        applyService = new LazyBuilder<>(() -> {
            validateIfConnectedToCluster();
            return new ApplyService(client, log);
        });
        portForwardService = new LazyBuilder<>(() -> {
            validateIfConnectedToCluster();
            return new PortForwardService(log);
        });
        debugService = new LazyBuilder<>(() -> {
            validateIfConnectedToCluster();
            return new DebugService(log, client, portForwardService.get(), applyService.get());
        });
        undeployService = new LazyBuilder<>(() -> {
            validateIfConnectedToCluster();
            if (platformMode == RuntimeMode.OPENSHIFT && isOpenShift(client)) {
                return new OpenshiftUndeployService(this, log);
            }
            return new KubernetesUndeployService(this, log);
        });
        migrateService = new LazyBuilder<>(() -> new MigrateService(getConfiguration().getBasedir(), log));
        helmService = new LazyBuilder<>(() -> new HelmService(getConfiguration(), log));
    }

    private void validateIfConnectedToCluster() {
        if (client == null) {
            throw new IllegalArgumentException("Connection to Cluster required. Please check if offline mode is set to false");
        }
    }

}
