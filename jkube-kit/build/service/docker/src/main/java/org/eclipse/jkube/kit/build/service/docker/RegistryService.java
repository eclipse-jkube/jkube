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
package org.eclipse.jkube.kit.build.service.docker;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.access.CreateImageOptions;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.ImagePullPolicy;

/**
 * Allows to interact with registries, eg. to push/pull images.
 */
public class RegistryService {

    private final DockerAccess docker;
    private final QueryService queryService;
    private final KitLogger log;

    RegistryService(DockerAccess docker, QueryService queryService, KitLogger log) {
        this.docker = docker;
        this.queryService = queryService;
        this.log = log;
    }

    /**
     * Push a set of images to a registry
     *
     * @param imageConfig image to push (but only if they have a build configuration)
     * @param retries how often to retry
     * @param registryConfig a global registry configuration
     * @param skipTag flag to skip pushing tagged images
     * @throws IOException exception
     */
    public void pushImage(ImageConfiguration imageConfig,
                          int retries, RegistryConfig registryConfig, boolean skipTag) throws IOException {
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
        String name = imageConfig.getName();
        if (buildConfig != null) {
            ImageName imageName = new ImageName(imageConfig.getName());
            String configuredRegistry = EnvUtil.firstRegistryOf(
                imageName.getRegistry(),
                imageConfig.getRegistry(),
                registryConfig.getRegistry());


            SummaryUtil.setPushRegistry(Optional.ofNullable(configuredRegistry).orElse("docker.io"));
            AuthConfig authConfig = createAuthConfig(true, new ImageName(name).getUser(), configuredRegistry, registryConfig);

            long start = System.currentTimeMillis();
            docker.pushImage(name, authConfig, configuredRegistry, retries);
            log.info("Pushed %s in %s", name, EnvUtil.formatDurationTill(start));

            if (!skipTag) {
                for (String tag : imageConfig.getBuildConfiguration().getTags()) {
                    if (tag != null) {
                        docker.pushImage(new ImageName(name, tag).getFullName(), authConfig, configuredRegistry, retries);
                    }
                }
            }
        }
    }


    /**
     * Check an image, and, if <code>autoPull</code> is set to true, fetch it. Otherwise if the image
     * is not existent, throw an error
     *
     * @param image image
     * @param pullManager image pull manager
     * @param registryConfig registry configuration
     * @param buildConfiguration build configuration
     * @throws IOException exception
     */
    public void pullImageWithPolicy(String image, ImagePullManager pullManager,RegistryConfig registryConfig,
        BuildConfiguration buildConfiguration) throws IOException {

        // Already pulled, so we don't need to take care
        if (pullManager.hasAlreadyPulled(image)) {
            return;
        }

        // Check if a pull is required
        if (!imageRequiresPull(queryService.hasImage(image), pullManager.getImagePullPolicy(), image)) {
            return;
        }

        final ImageName imageName = new ImageName(image);
        final long pullStartTime = System.currentTimeMillis();
        final String actualRegistry = EnvUtil.firstRegistryOf(imageName.getRegistry(), registryConfig.getRegistry());
        final CreateImageOptions createImageOptions = new CreateImageOptions(buildConfiguration.getCreateImageOptions())
            .fromImage(imageName.getNameWithoutTag(actualRegistry))
            .tag(imageName.getDigest() != null ? imageName.getDigest() : imageName.getTag());

        docker.pullImage(imageName.getFullName(),
            createAuthConfig(false, null, actualRegistry, registryConfig),
            actualRegistry, createImageOptions);
        log.info("Pulled %s in %s", imageName.getFullName(), EnvUtil.formatDurationTill(pullStartTime));
        pullManager.pulled(image);

        if (actualRegistry != null && !imageName.hasRegistry()) {
            // If coming from a registry which was not contained in the original name, add a tag from the
            // full name with the registry to the short name with no-registry.
            docker.tag(imageName.getFullName(actualRegistry), image, false);
        }
    }


    // ============================================================================================================


    private boolean imageRequiresPull(boolean hasImage, ImagePullPolicy pullPolicy, String imageName)
        throws IOException {

        // The logic here is like this (see also #96):
        // otherwise: don't pull

        if (pullPolicy == ImagePullPolicy.Never) {
            if (!hasImage) {
                throw new IOException(
                    String.format("No image '%s' found and pull policy 'Never' is set. Please chose another pull policy or pull the image yourself)", imageName));
            }
            return false;
        }

        // If the image is not available and mode is not ImagePullPolicy.Never --> pull
        if (!hasImage) {
            return true;
        }

        // If pullPolicy == Always --> pull, otherwise not (we have it already)
        return pullPolicy == ImagePullPolicy.Always;
    }

    private AuthConfig createAuthConfig(boolean isPush, String user, String registry, RegistryConfig config)
            throws IOException {

        return new AuthConfigFactory(log).createAuthConfig(
            isPush, config.isSkipExtendedAuth(), config.getAuthConfig(),
            config.getSettings(), user, registry, config.getPasswordDecryptionMethod());
    }

}
