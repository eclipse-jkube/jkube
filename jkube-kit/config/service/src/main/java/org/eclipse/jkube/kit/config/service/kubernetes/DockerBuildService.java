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

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.RegistryConfig;
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

    private JKubeServiceHub jKubeServiceHub;

    public DockerBuildService() { }

    DockerBuildService(JKubeServiceHub jKubeServiceHub) {
        Objects.requireNonNull(jKubeServiceHub.getDockerServiceHub(), "dockerServiceHub");
        Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(), "BuildServiceConfig is required");
        this.jKubeServiceHub = jKubeServiceHub;
    }

    @Override
    public void build(ImageConfiguration imageConfig) throws JKubeServiceException {
        try {
            jKubeServiceHub.getDockerServiceHub().getBuildService().buildImage(
                imageConfig,
                jKubeServiceHub.getBuildServiceConfig().getImagePullManager(),
                jKubeServiceHub.getConfiguration());

            // Assume we always want to tag
            jKubeServiceHub.getDockerServiceHub().getBuildService().tagImage(imageConfig.getName(), imageConfig);
        } catch (IOException ex) {
            throw new JKubeServiceException("Error while trying to build the image: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        try {
            jKubeServiceHub.getDockerServiceHub().getRegistryService()
                    .pushImages(imageConfigs, retries, registryConfig, skipTag);
        } catch (IOException ex) {
            throw new JKubeServiceException("Error while trying to push the image: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void postProcess(BuildServiceConfig config) {
        // No post processing required
    }

    @Override
    public boolean isApplicable(JKubeServiceHub jKubeServiceHub) {
        return jKubeServiceHub.getRuntimeMode() == RuntimeMode.KUBERNETES;
    }

    @Override
    public void setJKubeServiceHub(JKubeServiceHub jKubeServiceHub) {
        this.jKubeServiceHub = jKubeServiceHub;
    }
}
