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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class DockerBuildService implements BuildService {

    private final RuntimeMode runtimeMode;
    private final BuildServiceConfig buildServiceConfig;
    private final JKubeConfiguration jKubeConfiguration;
    private final ServiceHub dockerServices;

    public DockerBuildService(JKubeServiceHub jKubeServiceHub) {
        this.runtimeMode = jKubeServiceHub.getRuntimeMode();
        this.buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
            "BuildServiceConfig is required");
        this.jKubeConfiguration = Objects.requireNonNull(jKubeServiceHub.getConfiguration(),
            "JKubeConfiguration is required");
        this.dockerServices = Objects.requireNonNull(jKubeServiceHub.getDockerServiceHub(),
            "Docker Service Hub is required");

    }

    @Override
    public boolean isApplicable() {
        return runtimeMode == RuntimeMode.KUBERNETES;
    }

    @Override
    public void build(ImageConfiguration imageConfig) throws JKubeServiceException {
        try {
            dockerServices.getBuildService().buildImage(imageConfig, buildServiceConfig.getImagePullManager(), jKubeConfiguration);

            // Assume we always want to tag
            dockerServices.getBuildService().tagImage(imageConfig.getName(), imageConfig);
        } catch (IOException ex) {
            throw new JKubeServiceException("Error while trying to build the image: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        try {
            dockerServices.getRegistryService().pushImages(imageConfigs, retries, registryConfig, skipTag);
        } catch (IOException ex) {
            throw new JKubeServiceException("Error while trying to push the image: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void postProcess() {
        // No post processing required
    }

}
