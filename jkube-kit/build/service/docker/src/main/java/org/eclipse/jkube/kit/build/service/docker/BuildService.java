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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.LinkedList;

import com.google.common.collect.ImmutableMap;

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

import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromConfiguration;

public class BuildService {

    private static final String ARG_PREFIX = "docker.buildArg.";

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

        if (imagePullManager != null) {
            autoPullBaseImage(imageConfig, imagePullManager, configuration);
        }

        buildImage(imageConfig, configuration, checkForNocache(imageConfig), addBuildArgs(configuration));
    }

    public void tagImage(String imageName, ImageConfiguration imageConfig) throws DockerAccessException {

        List<String> tags = imageConfig.getBuildConfiguration().getTags();
        if (!tags.isEmpty()) {
            log.info("%s: Tag with %s", imageConfig.getDescription(), EnvUtil.stringJoin(tags, ","));

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

        Map<String, String> mergedBuildMap = prepareBuildArgs(buildArgs, buildConfig);

        // auto is now supported by docker, consider switching?
        BuildOptions opts =
                new BuildOptions(buildConfig.getBuildOptions())
                        .dockerfile(getDockerfileName(buildConfig))
                        .forceRemove(cleanupMode.isRemove())
                        .noCache(noCache)
                        .cacheFrom(buildConfig.getCacheFrom())
                        .buildArgs(mergedBuildMap);
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

    private Map<String, String> prepareBuildArgs(Map<String, String> buildArgs, BuildConfiguration buildConfig) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().putAll(buildArgs);
        if (buildConfig.getArgs() != null) {
            builder.putAll(buildConfig.getArgs());
        }
        return builder.build();
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

    private Map<String, String> addBuildArgs(JKubeConfiguration configuration) {
        Map<String, String> buildArgsFromProject = addBuildArgsFromProperties(configuration.getProject().getProperties());
        Map<String, String> buildArgsFromSystem = addBuildArgsFromProperties(System.getProperties());
        Map<String, String> buildArgsFromDockerConfig = addBuildArgsFromDockerConfig();
        return ImmutableMap.<String, String>builder()
                .putAll(buildArgsFromDockerConfig)
                .putAll(Optional.ofNullable(configuration.getBuildArgs()).orElse(Collections.emptyMap()))
                .putAll(buildArgsFromProject)
                .putAll(buildArgsFromSystem)
                .build();
    }

    private Map<String, String> addBuildArgsFromProperties(Properties properties) {
        Map<String, String> buildArgs = new HashMap<>();
        for (Object keyObj : properties.keySet()) {
            String key = (String) keyObj;
            if (key.startsWith(ARG_PREFIX)) {
                String argKey = key.replaceFirst(ARG_PREFIX, "");
                String value = properties.getProperty(key);

                if (!isEmpty(value)) {
                    buildArgs.put(argKey, value);
                }
            }
        }
        log.debug("Build args set %s", buildArgs);
        return buildArgs;
    }

    private Map<String, String> addBuildArgsFromDockerConfig() {
        final Map<String, Object> dockerConfig = DockerFileUtil.readDockerConfig();
        if (dockerConfig == null) {
            return Collections.emptyMap();
        }

        // add proxies
        Map<String, String> buildArgs = new HashMap<>();
        if (dockerConfig.containsKey("proxies")) {
            final Map<String, Object> proxies = (Map<String, Object>) dockerConfig.get("proxies");
            if (proxies.containsKey("default")) {
                final Map<String, String> defaultProxyObj = (Map<String, String>) proxies.get("default");
                String[] proxyMapping = new String[] {
                        "httpProxy", "http_proxy",
                        "httpsProxy", "https_proxy",
                        "noProxy", "no_proxy",
                        "ftpProxy", "ftp_proxy"
                };

                for(int index = 0; index < proxyMapping.length; index += 2) {
                    if (defaultProxyObj.containsKey(proxyMapping[index])) {
                        buildArgs.put(ARG_PREFIX + proxyMapping[index+1], defaultProxyObj.get(proxyMapping[index]));
                    }
                }
            }
        }
        log.debug("Build args set %s", buildArgs);
        return buildArgs;
    }

    private void autoPullBaseImage(ImageConfiguration imageConfig, ImagePullManager imagePullManager, JKubeConfiguration configuration)
            throws IOException {
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

        if (buildConfig.getDockerArchive() != null) {
            // No auto pull needed in archive mode
            return;
        }

        List<String> fromImages;
        if (buildConfig.isDockerFileMode()) {
            fromImages = extractBaseFromDockerfile(buildConfig, configuration);
        } else {
            fromImages = new LinkedList<>();
            String baseImage = extractBaseFromConfiguration(buildConfig);
            if (baseImage != null) {
                fromImages.add(baseImage);
            }
        }
        for (String fromImage : fromImages) {
            if (fromImage != null && !AssemblyManager.SCRATCH_IMAGE.equals(fromImage)) {
                registryService.pullImageWithPolicy(fromImage, imagePullManager, configuration.getRegistryConfig(), buildConfig);
            }
        }
    }

    private List<String> extractBaseFromDockerfile(BuildConfiguration buildConfig, JKubeConfiguration configuration) {
        List<String> fromImage;
        try {
            File fullDockerFilePath = buildConfig.getAbsoluteDockerFilePath(configuration.getSourceDirectory(),
                Optional.ofNullable(configuration.getProject().getBaseDirectory()).map(File::toString) .orElse(null));

            fromImage = DockerFileUtil.extractBaseImages(fullDockerFilePath, configuration.getProperties(), buildConfig.getFilter(), buildConfig.getArgs());
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

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
