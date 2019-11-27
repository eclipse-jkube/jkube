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

import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.JkubeServiceException;

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

        org.eclipse.jkube.kit.build.service.docker.BuildService dockerBuildService = dockerServiceHub.getBuildService();
        org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext dockerBuildContext = config.getDockerBuildContext();
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
