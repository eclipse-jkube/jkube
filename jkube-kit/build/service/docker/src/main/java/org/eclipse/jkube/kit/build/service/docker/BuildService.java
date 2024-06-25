/*
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedList;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.build.service.docker.access.BuildOptions;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.CleanupMode;

import static org.eclipse.jkube.kit.build.api.helper.BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings;
import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromConfiguration;

public class BuildService {

    private final DockerAccess docker;
    private final QueryService queryService;
    private final ArchiveService archiveService;
    private final RegistryService registryService;
    private final KitLogger log;

    BuildService(DockerAccess docker, QueryService queryService, RegistryService registryService, ArchiveService archiveService, KitLogger log) {
        this.docker = docker;
        this.queryService = queryService;
        this.registryService = registryService;
        this.archiveService = archiveService;
        this.log = log;
    }

    /**
     * Pull the base image if needed and run the build.
     *
     * @param imageConfig the image configuration
     * @param imagePullManager the image pull manager
     * @param configuration the project configuration
     * @throws IOException in case of any problems
     */
    public void buildImage(ImageConfiguration imageConfig, ImagePullManager imagePullManager, JKubeConfiguration configuration)
            throws IOException {

        Map<String, String> mergedBuildArgs = mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfig, configuration);

        if (imagePullManager != null) {
            autoPullBaseImage(imageConfig, imagePullManager, configuration, mergedBuildArgs);
        }

        buildImage(imageConfig, configuration, checkForNocache(imageConfig), mergedBuildArgs);
    }

    public void tagImage(String imageName, ImageConfiguration imageConfig) throws DockerAccessException {

        List<String> tags = imageConfig.getBuildConfiguration().getTags();
        if (!tags.isEmpty()) {
            log.info("%s: Tag with %s", imageConfig.getDescription(), String.join(",", tags));

            for (String tag : tags) {
                if (tag != null) {
                    docker.tag(imageName, new ImageName(imageName, tag).getFullName(), true);
                }
            }

            log.debug("Tagging image successful!");
        }
    }

    /**
     * Build an image
     *
     * @param imageConfig the image configuration
     * @param params mojo params for the project
     * @param noCache if not null, dictate the caching behaviour. Otherwise its taken from the build configuration
     * @param buildArgs maven build context
     * @throws DockerAccessException docker access exception
     * @throws IOException in case of any I/O exception
     */
    protected void buildImage(ImageConfiguration imageConfig, JKubeConfiguration params, boolean noCache, Map<String, String> buildArgs)
            throws IOException {

        String imageName = imageConfig.getName();
        ImageName.validate(imageName);

        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

        String oldImageId = null;

        CleanupMode cleanupMode = buildConfig.cleanupMode();
        if (cleanupMode.isRemove()) {
            oldImageId = queryService.getImageId(imageName);
        }

        long time = System.currentTimeMillis();

        if (buildConfig.getDockerArchive() != null) {
            docker.loadImage(imageName, buildConfig.getAbsoluteDockerTarPath(params.getSourceDirectory(), params.getProject().getBaseDirectory() != null
                    ? params.getProject().getBaseDirectory().toString() : null));
            log.info("%s: Loaded tarball in %s", buildConfig.getDockerArchive(), EnvUtil.formatDurationTill(time));
            return;
        }

        File dockerArchive = archiveService.createArchive(imageName, buildConfig, params, log);
        log.info("%s: Created %s in %s", imageConfig.getDescription(), dockerArchive.getName(), EnvUtil.formatDurationTill(time));

        // auto is now supported by docker, consider switching?
        BuildOptions opts =
                new BuildOptions(buildConfig.getBuildOptions())
                        .dockerfile(getDockerfileName(buildConfig))
                        .forceRemove(cleanupMode.isRemove())
                        .noCache(noCache)
                        .cacheFrom(buildConfig.getCacheFrom())
                        .buildArgs(buildArgs);
        String newImageId = doBuildImage(imageName, dockerArchive, opts);
        if (newImageId == null) {
            throw new IllegalStateException("Failure in building image, unable to find image built with name " + imageName);
        }
        log.info("%s: Built image %s", imageConfig.getDescription(), newImageId);

        if (oldImageId != null && !oldImageId.equals(newImageId)) {
            try {
                docker.removeImage(oldImageId, true);
                log.info("%s: Removed old image %s", imageConfig.getDescription(), oldImageId);
            } catch (DockerAccessException exp) {
                if (cleanupMode == CleanupMode.TRY_TO_REMOVE) {
                    log.warn("%s: %s (old image)%s", imageConfig.getDescription(), exp.getMessage(),
                            (exp.getCause() != null ? " [" + exp.getCause().getMessage() + "]" : ""));
                } else {
                    throw exp;
                }
            }
        }
    }

    private String getDockerfileName(BuildConfiguration buildConfig) {
        if (buildConfig.isDockerFileMode()) {
            return buildConfig.getDockerFile().getName();
        } else {
            return null;
        }
    }

    private String doBuildImage(String imageName, File dockerArchive, BuildOptions options)
            throws DockerAccessException {
        docker.buildImage(imageName, dockerArchive, options);
        return queryService.getImageId(imageName);
    }

    private void autoPullBaseImage(ImageConfiguration imageConfig, ImagePullManager imagePullManager,
            JKubeConfiguration configuration, Map<String, String> mergedBuildArgs)
            throws IOException {
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

        if (buildConfig.getDockerArchive() != null) {
            // No auto pull needed in archive mode
            return;
        }

        List<String> fromImages;
        if (buildConfig.isDockerFileMode()) {
            fromImages = extractBaseFromDockerfile(buildConfig, configuration, mergedBuildArgs);
        } else {
            fromImages = new LinkedList<>();
            String baseImage = extractBaseFromConfiguration(buildConfig);
            if (baseImage != null) {
                fromImages.add(baseImage);
            }
        }
        for (String fromImage : fromImages) {
            if (fromImage != null && !AssemblyManager.SCRATCH_IMAGE.equals(fromImage)) {
                registryService.pullImageWithPolicy(fromImage, imagePullManager, configuration.getPullRegistryConfig(), buildConfig);
            }
        }
    }

    private List<String> extractBaseFromDockerfile(BuildConfiguration buildConfig, JKubeConfiguration configuration,
            Map<String, String> mergedBuildArgs) {
        List<String> fromImage;
        try {
            File fullDockerFilePath = buildConfig.getAbsoluteDockerFilePath(configuration.getSourceDirectory(),
                Optional.ofNullable(configuration.getProject().getBaseDirectory()).map(File::toString) .orElse(null));

            fromImage = DockerFileUtil.extractBaseImages(fullDockerFilePath, configuration.getProperties(),
                    buildConfig.getFilter(), mergedBuildArgs);
        } catch (IOException e) {
            // Cant extract base image, so we wont try an auto pull. An error will occur later anyway when
            // building the image, so we are passive here.
            return Collections.emptyList();
        }
        return fromImage;
    }

    private boolean checkForNocache(ImageConfiguration imageConfig) {
        String nocache = System.getProperty("docker.nocache");
        if (nocache != null) {
            return nocache.length() == 0 || Boolean.parseBoolean(nocache);
        } else {
            BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
            return buildConfig.nocache();
        }
    }

}
