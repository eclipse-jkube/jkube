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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.LinkedList;

import com.google.gson.JsonObject;

import com.google.common.collect.ImmutableMap;

import org.eclipse.jkube.kit.build.core.JKubeBuildContext;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.helper.DockerFileUtil;
import org.eclipse.jkube.kit.build.core.assembly.DockerAssemblyManager;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.build.service.docker.access.BuildOptions;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.CleanupMode;

public class BuildService {

    private final String argPrefix = "docker.buildArg.";

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
     * @param buildContext the build context
     * @throws Exception in case of any problems
     */
    public void buildImage(ImageConfiguration imageConfig, ImagePullManager imagePullManager, BuildContext buildContext)
            throws Exception {

        if (imagePullManager != null) {
            autoPullBaseImage(imageConfig, imagePullManager, buildContext);
        }

        buildImage(imageConfig, buildContext.getMavenBuildContext(), checkForNocache(imageConfig), addBuildArgs(buildContext));
    }

    public void tagImage(String imageName, ImageConfiguration imageConfig) throws DockerAccessException {

        List<String> tags = imageConfig.getBuildConfiguration().getTags();
        if (tags.size() > 0) {
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
    protected void buildImage(ImageConfiguration imageConfig, JKubeBuildContext params, boolean noCache, Map<String, String> buildArgs)
            throws DockerAccessException, IOException {

        String imageName = imageConfig.getName();
        ImageName.validate(imageName);

        JKubeBuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

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
                        .buildArgs(mergedBuildMap);
        String newImageId = doBuildImage(imageName, dockerArchive, opts);
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

    private Map<String, String> addBuildArgs(BuildContext buildContext) {
        Map<String, String> buildArgsFromProject = addBuildArgsFromProperties(buildContext.getMavenBuildContext().getProject().getProperties());
        Map<String, String> buildArgsFromSystem = addBuildArgsFromProperties(System.getProperties());
        Map<String, String> buildArgsFromDockerConfig = addBuildArgsFromDockerConfig();
        return ImmutableMap.<String, String>builder()
                .putAll(buildArgsFromDockerConfig)
                .putAll(buildContext.getBuildArgs() != null ? buildContext.getBuildArgs() : Collections.emptyMap())
                .putAll(buildArgsFromProject)
                .putAll(buildArgsFromSystem)
                .build();
    }

    private Map<String, String> addBuildArgsFromProperties(Properties properties) {
        Map<String, String> buildArgs = new HashMap<>();
        for (Object keyObj : properties.keySet()) {
            String key = (String) keyObj;
            if (key.startsWith(argPrefix)) {
                String argKey = key.replaceFirst(argPrefix, "");
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
        JsonObject dockerConfig = DockerFileUtil.readDockerConfig();
        if (dockerConfig == null) {
            return Collections.emptyMap();
        }

        // add proxies
        Map<String, String> buildArgs = new HashMap<>();
        if (dockerConfig.has("proxies")) {
            JsonObject proxies = dockerConfig.getAsJsonObject("proxies");
            if (proxies.has("default")) {
                JsonObject defaultProxyObj = proxies.getAsJsonObject("default");
                String[] proxyMapping = new String[] {
                        "httpProxy", "http_proxy",
                        "httpsProxy", "https_proxy",
                        "noProxy", "no_proxy",
                        "ftpProxy", "ftp_proxy"
                };

                for(int index = 0; index < proxyMapping.length; index += 2) {
                    if (defaultProxyObj.has(proxyMapping[index])) {
                        buildArgs.put(argPrefix + proxyMapping[index+1], defaultProxyObj.get(proxyMapping[index]).getAsString());
                    }
                }
            }
        }
        log.debug("Build args set %s", buildArgs);
        return buildArgs;
    }

    private void autoPullBaseImage(ImageConfiguration imageConfig, ImagePullManager imagePullManager, BuildContext buildContext)
            throws Exception {
        BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();

        if (buildConfig.getDockerArchive() != null) {
            // No auto pull needed in archive mode
            return;
        }

        List<String> fromImages;
        if (buildConfig.isDockerFileMode()) {
            fromImages = extractBaseFromDockerfile(buildConfig, buildContext);
        } else {
            fromImages = new LinkedList<>();
            String baseImage = extractBaseFromConfiguration(buildConfig);
            if (baseImage!=null) {
                fromImages.add(extractBaseFromConfiguration(buildConfig));
            }
        }
        for (String fromImage : fromImages) {
            if (fromImage != null && !DockerAssemblyManager.SCRATCH_IMAGE.equals(fromImage)) {
                registryService.pullImageWithPolicy(fromImage, imagePullManager, buildContext.getRegistryConfig(), queryService.hasImage(fromImage));
            }
        }
    }

    private String extractBaseFromConfiguration(BuildConfiguration buildConfig) {
        String fromImage;
        fromImage = buildConfig.getFrom();
        if (fromImage == null) {
            AssemblyConfiguration assemblyConfig = buildConfig.getAssemblyConfiguration();
            if (assemblyConfig == null) {
                fromImage = DockerAssemblyManager.DEFAULT_DATA_BASE_IMAGE;
            }
        }
        return fromImage;
    }

    private List<String> extractBaseFromDockerfile(BuildConfiguration buildConfig, BuildContext buildContext) {
        List<String> fromImage;
        try {
            File fullDockerFilePath = buildConfig.getAbsoluteDockerFilePath(buildContext.getMavenBuildContext().getSourceDirectory(), buildContext.getMavenBuildContext().getProject().getBaseDirectory() != null
                    ? buildContext.getMavenBuildContext().getProject().getBaseDirectory().toString() : null);

            fromImage = DockerFileUtil.extractBaseImages(fullDockerFilePath, buildContext.getMavenBuildContext().getProperties());
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
            return nocache.length() == 0 || Boolean.valueOf(nocache);
        } else {
            BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
            return buildConfig.nocache();
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }


    // ===========================================


    public static class BuildContext implements Serializable {

        private JKubeBuildContext mojoParameters;

        private Map<String, String> buildArgs;

        private RegistryService.RegistryConfig registryConfig;

        public BuildContext() {
        }

        public JKubeBuildContext getMavenBuildContext() {
            return mojoParameters;
        }

        public Map<String, String> getBuildArgs() {
            return buildArgs;
        }

        public RegistryService.RegistryConfig getRegistryConfig() {
            return registryConfig;
        }

        public static class Builder {

            private BuildContext context;

            public Builder() {
                this.context = new BuildContext();
            }

            public Builder(BuildContext context) {
                this.context = context;
            }

            public Builder mojoParameters(JKubeBuildContext mojoParameters) {
                context.mojoParameters = mojoParameters;
                return this;
            }

            public Builder buildArgs(Map<String, String> buildArgs) {
                context.buildArgs = buildArgs;
                return this;
            }

            public Builder registryConfig(RegistryService.RegistryConfig registryConfig) {
                context.registryConfig = registryConfig;
                return this;
            }

            public BuildContext build() {
                return context;
            }
        }
    }

}
