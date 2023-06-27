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
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.AbstractImageBuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePullRegistryFrom;
import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePushRegistryFrom;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.containerFromImageConfiguration;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.getBaseImage;

public class JibBuildService extends AbstractImageBuildService {

    private final KitLogger log;
    private final BuildServiceConfig buildServiceConfig;
    private final JKubeConfiguration configuration;

    public JibBuildService(JKubeServiceHub jKubeServiceHub) {
        super(jKubeServiceHub);
        this.log = Objects.requireNonNull(jKubeServiceHub.getLog(), "Log is required");
        this.buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
            "BuildServiceConfig is required");
        this.configuration = Objects.requireNonNull(jKubeServiceHub.getConfiguration(),
            "JKubeConfiguration is required");
    }

    @Override
    public boolean isApplicable() {
        return buildServiceConfig.getJKubeBuildStrategy() == JKubeBuildStrategy.jib;
    }

    @Override
    public void buildSingleImage(ImageConfiguration imageConfig) throws JKubeServiceException {
        try {
            log.info("[[B]]JIB[[B]] image build started");
            if (imageConfig.getBuildConfiguration().isDockerFileMode()) {
                throw new JKubeServiceException("Dockerfile mode is not supported with JIB build strategy");
            }
            String pushRegistry = getApplicablePushRegistryFrom(imageConfig, configuration.getRegistryConfig());
            prependRegistry(imageConfig, pushRegistry);
            BuildDirs buildDirs = new BuildDirs(imageConfig.getName(), configuration);
            String pullRegistry = getApplicableRegistry(false, imageConfig, configuration.getRegistryConfig());
            final Credential pullRegistryCredential = getRegistryCredentials(
                configuration.getRegistryConfig(), false, pullRegistry, log);
            final JibContainerBuilder containerBuilder = containerFromImageConfiguration(imageConfig, pullRegistry, pullRegistryCredential);

            final Map<Assembly, List<AssemblyFileEntry>> layers = AssemblyManager.getInstance()
                .copyFilesToFinalTarballDirectory(configuration, buildDirs,
                    AssemblyManager.getAssemblyConfiguration(imageConfig.getBuildConfiguration(), configuration));
            JibServiceUtil.layers(buildDirs, layers).forEach(containerBuilder::addFileEntriesLayer);

            // TODO: Improve Assembly Manager so that the effective assemblyFileEntries computed can be properly shared
            // the call to AssemblyManager.getInstance().createDockerTarArchive should not be necessary,
            // files should be added using the AssemblyFileEntry list. AssemblyManager, should provide
            // a common way to achieve this so that both the tar builder and any other builder could get a hold of
            // archive customizers, file entries, etc.
            File dockerTarArchive = getAssemblyTarArchive(imageConfig, configuration, log);
            JibServiceUtil.buildContainer(containerBuilder,
                TarImage.at(dockerTarArchive.toPath()).named(imageConfig.getName()), log);
            log.info(" %s successfully built", dockerTarArchive.getAbsolutePath());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when building JIB image", ex);
        }
    }

    @Override
    protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        try {
            String pushRegistry = getApplicableRegistry(true, imageConfiguration, registryConfig);
            prependRegistry(imageConfiguration, pushRegistry);
            log.info("This push refers to: %s", imageConfiguration.getName());
            JibServiceUtil.jibPush(
                imageConfiguration,
                getRegistryCredentials(registryConfig, true, pushRegistry, log),
                getBuildTarArchive(imageConfiguration, configuration),
                log
            );
        } catch (Exception ex) {
            String message = "Error when push JIB image: " + ex.getMessage();
            if(ex.getMessage().contains("denied") || ex.getMessage().contains("unauthorized")) {
                message += "\nPossible issue: wrong image name or registry." +
                           "\nHint: Rename image name or registry with the jkube.generator.name property = registry name and user name and image name";
            }
            throw new JKubeServiceException(message, ex);
        }
    }

    @Override
    public void postProcess() {
        // No post processing required
    }

    static ImageConfiguration prependRegistry(ImageConfiguration imageConfiguration, String registry) {
        ImageName imageName = new ImageName(imageConfiguration.getName());
        if (!imageName.hasRegistry() && registry != null) {
            imageConfiguration.setName(imageName.getFullName(registry));
            imageConfiguration.setRegistry(registry);
        }
        return imageConfiguration;
    }

    static File getAssemblyTarArchive(ImageConfiguration imageConfig, JKubeConfiguration configuration, KitLogger log) throws IOException {
        log.info("Preparing assembly files");
        final String targetImage = imageConfig.getName();
        return AssemblyManager.getInstance()
                .createDockerTarArchive(targetImage, configuration, imageConfig.getBuildConfiguration(), log, null);
    }

    static Credential getRegistryCredentials(
        RegistryConfig registryConfig, boolean isPush, String registry, KitLogger log)
        throws IOException {
        AuthConfigFactory authConfigFactory = new AuthConfigFactory(log);
        AuthConfig standardAuthConfig = authConfigFactory.createAuthConfig(isPush, registryConfig.isSkipExtendedAuth(), registryConfig.getAuthConfig(), registryConfig.getSettings(), null, registry, registryConfig.getPasswordDecryptionMethod());
        Credential credentials = null;
        if (standardAuthConfig != null) {
            credentials = Credential.from(standardAuthConfig.getUsername(), standardAuthConfig.getPassword());
        }
        return credentials;
    }

    static File getBuildTarArchive(ImageConfiguration imageConfiguration, JKubeConfiguration configuration) {
        BuildDirs buildDirs = new BuildDirs(imageConfiguration.getName(), configuration);
        return new File(buildDirs.getTemporaryRootDirectory(), JKubeBuildTarArchiver.ARCHIVE_FILE_NAME + ArchiveCompression.none.getFileSuffix());
    }

    static String getApplicableRegistry(boolean isPush, ImageConfiguration imageConfiguration, RegistryConfig registryConfig) {
        String registry;
        if (isPush) {
            registry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
        } else {
            registry = getApplicablePullRegistryFrom(getBaseImage(imageConfiguration, null), registryConfig);
        }
        return registry;
    }
}
