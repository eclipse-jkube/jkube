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
package io.jkube.kit.config.service.kubernetes;

import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.ImagePullManager;
import io.jkube.kit.build.service.docker.ServiceHub;
import io.jkube.kit.config.service.BuildService;
import io.jkube.kit.config.service.JkubeServiceException;

import java.util.Objects;

/**
 * @author nicola
 * @since 17/02/2017
 */
public class DockerBuildService implements BuildService {

    private ServiceHub dockerServiceHub;

    private BuildServiceConfig config;

    public DockerBuildService(ServiceHub dockerServiceHub, BuildServiceConfig config) {
        Objects.requireNonNull(dockerServiceHub, "dockerServiceHub");
        Objects.requireNonNull(config, "config");

        this.dockerServiceHub = dockerServiceHub;
        this.config = config;
    }

    @Override
    public void build(ImageConfiguration imageConfig) throws JkubeServiceException {

        io.jkube.kit.build.service.docker.BuildService dockerBuildService = dockerServiceHub.getBuildService();
        io.jkube.kit.build.service.docker.BuildService.BuildContext dockerBuildContext = config.getDockerBuildContext();
        ImagePullManager imagePullManager = config.getImagePullManager();
        try {
            dockerBuildService.buildImage(imageConfig, imagePullManager, dockerBuildContext);

            // Assume we always want to tag
            dockerBuildService.tagImage(imageConfig.getName(), imageConfig);
        } catch (Exception ex) {
            throw new JkubeServiceException("Error while trying to build the image", ex);
        }
    }

    @Override
    public void postProcess(BuildServiceConfig config) {
        // No post processing required
    }

}
