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
import io.fabric8.openshift.client.OpenShiftClient;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.service.MigrateService;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.ArtifactResolverService;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;

import static org.eclipse.jkube.kit.common.util.OpenshiftHelper.isOpenShift;

/**
 * @author nicola
 */
public class JKubeServiceHub implements Closeable {

    @Getter
    private JKubeConfiguration configuration;
    @Getter
    private ClusterAccess clusterAccess;
    @Getter
    @Setter
    private RuntimeMode platformMode;
    private KitLogger log;
    @Getter
    private ServiceHub dockerServiceHub;
    @Getter
    private BuildServiceConfig buildServiceConfig;
    @Getter
    private KubernetesClient client;
    private LazyBuilder<ArtifactResolverService> artifactResolverService;
    private LazyBuilder<BuildService> buildService;
    private LazyBuilder<ResourceService> resourceService;
    private LazyBuilder<PortForwardService> portForwardService;
    private LazyBuilder<ApplyService> applyService;
    private LazyBuilder<UndeployService> undeployService;
    private LazyBuilder<MigrateService> migrateService;
    private LazyBuilder<DebugService> debugService;
    private final boolean offline;

    @Builder
    public JKubeServiceHub(
            ClusterAccess clusterAccess, RuntimeMode platformMode, KitLogger log,
            ServiceHub dockerServiceHub, JKubeConfiguration configuration,
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
        artifactResolverService = new LazyBuilder<>(() -> new JKubeArtifactResolverService(configuration.getProject()));
        migrateService = new LazyBuilder<>(() -> new MigrateService(getConfiguration().getBasedir(), log));
    }

    @Override
    public void close() {
        Optional.ofNullable(client).ifPresent(KubernetesClient::close);
        Optional.ofNullable(dockerServiceHub).map(ServiceHub::getDockerAccess).ifPresent(DockerAccess::shutdown);
    }

    public RuntimeMode getRuntimeMode() {
        return platformMode;
    }

    public ArtifactResolverService getArtifactResolverService() {
        return artifactResolverService.get();
    }

    public BuildService getBuildService() {
        return buildService.get();
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

    private void initClusterAccessAndLazyBuilders() {
        if (!offline) {
            if (clusterAccess == null) {
                clusterAccess = new ClusterAccess(log,
                        ClusterConfiguration.from(System.getProperties(), configuration.getProject().getProperties()).build());
            }
            this.client = clusterAccess.createDefaultClient();
        }
        applyService = new LazyBuilder<>(() -> {
            validateIfConnectedToCluster();
            return new ApplyService(client, log);
        });
        portForwardService = new LazyBuilder<>(() -> {
            validateIfConnectedToCluster();
            return new PortForwardService(client, log);
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
        buildService = new LazyBuilder<>(() -> {
            BuildService ret;
            if (JKubeBuildStrategy.jib == buildServiceConfig.getJKubeBuildStrategy()) {
                return new JibBuildService(JKubeServiceHub.this, log);
            }
            // Creating platform-dependent services
            if (platformMode == RuntimeMode.OPENSHIFT) {
                validateIfConnectedToCluster();
                if (!isOpenShift(client)) {
                    throw new IllegalStateException("OpenShift platform has been specified but OpenShift has not been detected!");
                }
                // OpenShift services
                ret = new OpenshiftBuildService((OpenShiftClient) client, log, JKubeServiceHub.this);
            } else {
                // Kubernetes services
                ret = new DockerBuildService(JKubeServiceHub.this);
            }
            return ret;
        });
    }

    private void validateIfConnectedToCluster() {
        if (client == null) {
            throw new IllegalArgumentException("Connection to Cluster required. Please check if offline mode is set to false");
        }
    }
}
