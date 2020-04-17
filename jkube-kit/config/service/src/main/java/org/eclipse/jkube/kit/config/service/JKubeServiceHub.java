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
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.config.JKubeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.ArtifactResolverService;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;

/**
 * @author nicola
 */
public class JKubeServiceHub implements Closeable {

    @Getter
    private JKubeConfiguration configuration;
    @Getter
    private ClusterAccess clusterAccess;
    private RuntimeMode platformMode;
    private KitLogger log;
    @Getter
    private ServiceHub dockerServiceHub;
    @Getter
    private BuildServiceConfig buildServiceConfig;
    private RuntimeMode resolvedMode;
    @Getter
    private KubernetesClient client;
    private LazyBuilder<ArtifactResolverService> artifactResolverService;
    private LazyBuilder<BuildService> buildService;
    private LazyBuilder<ApplyService> applyService;

    @Builder
    public JKubeServiceHub(
        ClusterAccess clusterAccess, RuntimeMode platformMode, KitLogger log,
        ServiceHub dockerServiceHub, JKubeConfiguration configuration, BuildServiceConfig buildServiceConfig) {
        this.clusterAccess = clusterAccess;
        this.platformMode = platformMode;
        this.log = log;
        this.dockerServiceHub = dockerServiceHub;
        this.configuration = configuration;
        this.buildServiceConfig = buildServiceConfig;
        init();
    }

    private void init() {
        Objects.requireNonNull(configuration, "JKubeKitConfiguration is required");
        Objects.requireNonNull(log, "log is a required parameter");
        if (platformMode == null) {
            platformMode = RuntimeMode.DEFAULT;
        }
        if (clusterAccess == null) {
            clusterAccess = new ClusterAccess(ClusterConfiguration.from(System.getProperties()).build());
        }
        this.resolvedMode = clusterAccess.resolveRuntimeMode(platformMode, log);
        if (resolvedMode != RuntimeMode.kubernetes && resolvedMode != RuntimeMode.openshift) {
            throw new IllegalArgumentException("Unknown platform mode " + platformMode + " resolved as "+ resolvedMode);
        }
        this.client = clusterAccess.createDefaultClient(log);

        // Lazily building services

        applyService = new LazyBuilder<ApplyService>() {
            @Override
            protected ApplyService build() {
                return new ApplyService(client, log);
            }
        };
        buildService = new LazyBuilder<BuildService>() {
            @Override
            protected BuildService build() {
                BuildService ret;
                // Creating platform-dependent services
                if (resolvedMode == RuntimeMode.openshift) {
                    if (!(client instanceof OpenShiftClient)) {
                        throw new IllegalStateException("Openshift platform has been specified but Openshift has not been detected!");
                    }
                    // Openshift services
                    ret = new OpenshiftBuildService((OpenShiftClient) client, log, JKubeServiceHub.this);
                } else {
                    // Kubernetes services
                    ret = new DockerBuildService(JKubeServiceHub.this);
                }
                return ret;
            }
        };
        artifactResolverService = new LazyBuilder<ArtifactResolverService>() {
            @Override
            protected ArtifactResolverService build() {
                return new JKubeArtifactResolverService(configuration.getProject()); }
        };
    }

    @Override
    public void close() {
        Optional.ofNullable(client).ifPresent(KubernetesClient::close);
        Optional.ofNullable(dockerServiceHub).map(ServiceHub::getDockerAccess).ifPresent(DockerAccess::shutdown);
    }

    public RuntimeMode getRuntimeMode() {
        return resolvedMode;
    }

    public ArtifactResolverService getArtifactResolverService() {
        return artifactResolverService.get();
    }

    public BuildService getBuildService() {
        return buildService.get();
    }

    public ApplyService getApplyService() {
        return applyService.get();
    }

}

