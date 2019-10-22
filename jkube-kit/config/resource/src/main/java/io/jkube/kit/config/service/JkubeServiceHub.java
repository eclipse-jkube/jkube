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
package io.jkube.kit.config.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.jkube.kit.build.service.docker.ServiceHub;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.service.ArtifactResolverService;
import io.jkube.kit.common.util.LazyBuilder;
import io.jkube.kit.config.access.ClusterAccess;
import io.jkube.kit.config.resource.RuntimeMode;
import io.jkube.kit.config.service.kubernetes.DockerBuildService;
import io.jkube.kit.config.service.openshift.OpenshiftBuildService;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class JkubeServiceHub {

    /*
     * Configured resources
     */
    private ClusterAccess clusterAccess;

    private RuntimeMode platformMode;

    private KitLogger log;

    private ServiceHub dockerServiceHub;

    private BuildService.BuildServiceConfig buildServiceConfig;

    private RepositorySystem repositorySystem;

    private MavenProject mavenProject;

    /**
     /*
     * Computed resources
     */

    private RuntimeMode resolvedMode;

    private KubernetesClient client;

    private ConcurrentHashMap<Class<?>, LazyBuilder<?>> services = new ConcurrentHashMap<>();

    private JkubeServiceHub() {
    }

    private void init() {
        Objects.requireNonNull(clusterAccess, "clusterAccess");
        Objects.requireNonNull(log, "log");

        this.resolvedMode = clusterAccess.resolveRuntimeMode(platformMode, log);
        if (resolvedMode != RuntimeMode.kubernetes && resolvedMode != RuntimeMode.openshift) {
            throw new IllegalArgumentException("Unknown platform mode " + platformMode + " resolved as "+ resolvedMode);
        }
        this.client = clusterAccess.createDefaultClient(log);

        // Lazily building services

        this.services.putIfAbsent(ApplyService.class, new LazyBuilder<ApplyService>() {
            @Override
            protected ApplyService build() {
                return new ApplyService(client, log);
            }
        });
        this.services.putIfAbsent(BuildService.class, new LazyBuilder<BuildService>() {
            @Override
            protected BuildService build() {
                BuildService buildService;
                // Creating platform-dependent services
                if (resolvedMode == RuntimeMode.openshift) {
                    if (!(client instanceof OpenShiftClient)) {
                        throw new IllegalStateException("Openshift platform has been specified but Openshift has not been detected!");
                    }
                    // Openshift services
                    buildService = new OpenshiftBuildService((OpenShiftClient) client, log, dockerServiceHub, buildServiceConfig);
                } else {
                    // Kubernetes services
                    buildService = new DockerBuildService(dockerServiceHub, buildServiceConfig);
                }
                return buildService;
            }
        });

        this.services.putIfAbsent(ArtifactResolverService.class, new LazyBuilder<ArtifactResolverService>() {
            @Override
            protected ArtifactResolverService build() {
                return new ArtifactResolverServiceMavenImpl(repositorySystem, mavenProject);
            }
        });
    }

    public BuildService getBuildService() {
        return (BuildService) this.services.get(BuildService.class).get();
    }

    public ArtifactResolverService getArtifactResolverService() {
        return (ArtifactResolverService) this.services.get(ArtifactResolverService.class).get();
    }

    // =================================================

    public static class Builder {

        private JkubeServiceHub hub;

        public Builder() {
            this.hub = new JkubeServiceHub();
        }

        public Builder clusterAccess(ClusterAccess clusterAccess) {
            hub.clusterAccess = clusterAccess;
            return this;
        }

        public Builder platformMode(RuntimeMode platformMode) {
            hub.platformMode = platformMode;
            return this;
        }

        public Builder log(KitLogger log) {
            hub.log = log;
            return this;
        }

        public Builder dockerServiceHub(ServiceHub dockerServiceHub) {
            hub.dockerServiceHub = dockerServiceHub;
            return this;
        }

        public Builder buildServiceConfig(BuildService.BuildServiceConfig buildServiceConfig) {
            hub.buildServiceConfig = buildServiceConfig;
            return this;
        }

        public Builder repositorySystem(RepositorySystem repositorySystem) {
            hub.repositorySystem = repositorySystem;
            return this;
        }

        public Builder mavenProject(MavenProject mavenProject) {
            hub.mavenProject = mavenProject;
            return this;
        }

        public JkubeServiceHub build() {
            hub.init();
            return hub;
        }
    }
}

