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
import java.util.Objects;

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.AbstractImageBuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class DockerBuildService extends AbstractImageBuildService {

    private final RuntimeMode runtimeMode;
    private final BuildServiceConfig buildServiceConfig;
    private final JKubeConfiguration jKubeConfiguration;
    private final DockerServiceHub dockerServices;

    public DockerBuildService(JKubeServiceHub jKubeServiceHub) {
        super(jKubeServiceHub);
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
    public void buildSingleImage(ImageConfiguration imageConfig) throws JKubeServiceException {
        try {
            dockerServices.getBuildService().buildImage(imageConfig, buildServiceConfig.getImagePullManager(), jKubeConfiguration);

            // Assume we always want to tag
            dockerServices.getBuildService().tagImage(imageConfig.getName(), imageConfig);
        } catch (IOException ex) {
            throw new JKubeServiceException("Error while trying to build the image: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        try {
            dockerServices.getRegistryService().pushImage(imageConfiguration, retries, registryConfig, skipTag);
        } catch (IOException ex) {
            throw new JKubeServiceException(getHintFromException(ex), ex);
        }
    }

    @Override
    public void postProcess() {
        // No post processing required
    }

}
