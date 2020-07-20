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

import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.TarImage;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.RegistryConfig;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JibBuildService implements BuildService {
    private static final String DOCKER_LOGIN_DEFAULT_REGISTRY = "https://index.docker.io/v1/";
    private static final List<String> DEFAULT_DOCKER_REGISTRIES = Arrays.asList(
            "docker.io", "index.docker.io", "registry.hub.docker.com"
    );
    private static final String PUSH_REGISTRY = "jkube.docker.push.registry";
    private JKubeServiceHub jKubeServiceHub;
    private KitLogger log;

    public JibBuildService(JKubeServiceHub jKubeServiceHub, KitLogger logger) {
        Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(), "BuildServiceConfig is required");
        this.jKubeServiceHub = jKubeServiceHub;
        this.log = logger;
    }

    @Override
    public void build(ImageConfiguration imageConfig) throws JKubeServiceException {
        try {
            log.info("[[B]]JIB[[B]] image build started");
            final JKubeConfiguration configuration = jKubeServiceHub.getConfiguration();
            appendRegistry(imageConfig, configuration.getProperties().getProperty(PUSH_REGISTRY));
            BuildDirs buildDirs = new BuildDirs(imageConfig.getName(), configuration);
            File dockerTarArchive = getAssemblyTarArchive(imageConfig, configuration, log);

            final JibContainerBuilder containerBuilder = JibServiceUtil.containerFromImageConfiguration(imageConfig);
            // TODO: Improve Assembly Manager so that the effective assemblyFileEntries computed can be properly shared
            final Map<File, AssemblyFileEntry> files = AssemblyManager.getInstance()
                .copyFilesToFinalTarballDirectory(
                    configuration.getProject(), buildDirs,
                    AssemblyManager.getAssemblyConfiguration(imageConfig.getBuildConfiguration(), configuration)
                ).stream()
                .collect(Collectors.toMap(AssemblyFileEntry::getDest, Function.identity()));

            JibServiceUtil.copyToContainer(
                containerBuilder, buildDirs.getOutputDirectory(), buildDirs.getOutputDirectory().getAbsolutePath(), files);
            JibServiceUtil.buildContainer(containerBuilder,
                    TarImage.at(dockerTarArchive.toPath()).named(imageConfig.getName()), log);
            log.info(" %s successfully built", dockerTarArchive.getAbsolutePath());
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when building JIB image", ex);
        }
    }

    @Override
    public void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        try {
            for (ImageConfiguration imageConfiguration : imageConfigs) {
                appendRegistry(imageConfiguration, registryConfig.getRegistry());
                log.info("This push refers to: %s", imageConfiguration.getName());
                JibServiceUtil.jibPush(imageConfiguration, getRegistryCredentials(registryConfig, imageConfiguration, log), getBuildTarArchive(imageConfiguration, jKubeServiceHub), log);
            }
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when push JIB image", ex);
        }
    }

    @Override
    public void postProcess(BuildServiceConfig config) {
        // No post processing required
    }

    protected static ImageConfiguration appendRegistry(ImageConfiguration imageConfiguration, String registry) {
        ImageName imageName = new ImageName(imageConfiguration.getName());
        if (!imageName.hasRegistry() && registry != null) {
            imageConfiguration.setName(registry + "/" + imageConfiguration.getName());
            imageConfiguration.setRegistry(registry);
        }
        return imageConfiguration;
    }

    protected static File getAssemblyTarArchive(ImageConfiguration imageConfig, JKubeConfiguration configuration, KitLogger log) throws IOException {
        log.info("Preparing assembly files");
        final String targetImage = imageConfig.getName();
        return AssemblyManager.getInstance()
                .createDockerTarArchive(targetImage, configuration, imageConfig.getBuildConfiguration(), log, null);
    }

    protected static Credential getRegistryCredentials(RegistryConfig registryConfig, ImageConfiguration imageConfiguration, KitLogger log) throws IOException {
        String registry = EnvUtil.firstRegistryOf((new ImageName(imageConfiguration.getName())).getRegistry(), imageConfiguration.getRegistry(), registryConfig.getRegistry());
        if (registry == null || DEFAULT_DOCKER_REGISTRIES.contains(registry)) {
            registry = DOCKER_LOGIN_DEFAULT_REGISTRY; // Let's assume docker is default registry.
        }

        AuthConfigFactory authConfigFactory = new AuthConfigFactory(log);
        AuthConfig standardAuthConfig = authConfigFactory.createAuthConfig(true, registryConfig.isSkipExtendedAuth(), registryConfig.getAuthConfig(), registryConfig.getSettings(), null, registry, registryConfig.getPasswordDecryptionMethod());
        Credential credentials = null;
        if (standardAuthConfig != null) {
            credentials = Credential.from(standardAuthConfig.getUsername(), standardAuthConfig.getPassword());
        }
        return credentials;
    }

    protected static File getBuildTarArchive(ImageConfiguration imageConfiguration, JKubeServiceHub jKubeServiceHub) {
        BuildDirs buildDirs = new BuildDirs(imageConfiguration.getName(), jKubeServiceHub.getConfiguration());
        return new File(buildDirs.getTemporaryRootDirectory(), JKubeBuildTarArchiver.ARCHIVE_FILE_NAME + ArchiveCompression.none.getFileSuffix());
    }
}
