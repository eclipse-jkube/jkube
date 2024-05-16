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
import org.eclipse.jkube.kit.service.jib.JibLogger;
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

/**
 * AbstractImageBuildService implementation for JIB build strategy.
 * <p>
 * Relies on Jib to perform the build and push operations.
 */
public class JibImageBuildService extends AbstractImageBuildService {

    private final KitLogger kitLogger;
    private final JibLogger jibLogger;
    private final AuthConfigFactory authConfigFactory;
    private final BuildServiceConfig buildServiceConfig;
    private final JKubeConfiguration configuration;

    public JibImageBuildService(JKubeServiceHub jKubeServiceHub) {
        this(jKubeServiceHub,
          new JibLogger(Objects.requireNonNull(jKubeServiceHub.getLog(), "Log is required")));
    }

    public JibImageBuildService(JKubeServiceHub jKubeServiceHub, JibLogger jibLogger) {
        super(jKubeServiceHub);
        this.jibLogger = jibLogger;
        kitLogger = jKubeServiceHub.getLog();
        authConfigFactory = new AuthConfigFactory(kitLogger);
        buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
            "BuildServiceConfig is required");
        configuration = Objects.requireNonNull(jKubeServiceHub.getConfiguration(),
            "JKubeConfiguration is required");
    }

    @Override
    public boolean isApplicable() {
        return buildServiceConfig.getJKubeBuildStrategy() == JKubeBuildStrategy.jib;
    }

    @Override
    public void buildSingleImage(ImageConfiguration imageConfiguration) throws JKubeServiceException {
        try {
            kitLogger.info("[[B]]JIB[[B]] image build started");
            if (imageConfiguration.getBuildConfiguration().isDockerFileMode()) {
                throw new JKubeServiceException("Dockerfile mode is not supported with JIB build strategy");
            }
            final ImageConfiguration imageConfigToBuild = prependPushRegistry(imageConfiguration, configuration.getPullRegistryConfig());
            final BuildDirs buildDirs = new BuildDirs(imageConfigToBuild.getName(), configuration);
            final String pullRegistry = getPullRegistry(imageConfigToBuild, configuration.getPullRegistryConfig());
            final Credential pullRegistryCredential = getRegistryCredentials(
                configuration.getPullRegistryConfig(), false, pullRegistry);
            final JibContainerBuilder containerBuilder = containerFromImageConfiguration(imageConfigToBuild, pullRegistry, pullRegistryCredential);

            final Map<Assembly, List<AssemblyFileEntry>> layers = AssemblyManager.getInstance()
                .copyFilesToFinalTarballDirectory(configuration, buildDirs,
                    AssemblyManager.getAssemblyConfiguration(imageConfigToBuild.getBuildConfiguration(), configuration));
            JibServiceUtil.layers(buildDirs, layers).forEach(containerBuilder::addFileEntriesLayer);

            // TODO: Improve Assembly Manager so that the effective assemblyFileEntries computed can be properly shared
            // the call to AssemblyManager.getInstance().createDockerTarArchive should not be necessary,
            // files should be added using the AssemblyFileEntry list. AssemblyManager, should provide
            // a common way to achieve this so that both the tar builder and any other builder could get a hold of
            // archive customizers, file entries, etc.
            File dockerTarArchive = getAssemblyTarArchive(imageConfigToBuild, configuration, kitLogger);
            JibServiceUtil.buildContainer(containerBuilder,
                TarImage.at(dockerTarArchive.toPath()).named(imageConfigToBuild.getName()), jibLogger);
            kitLogger.info(" %s successfully built", dockerTarArchive.getAbsolutePath());
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when building JIB image", ex);
        }
    }

    @Override
    protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
        try {
            final ImageConfiguration imageConfigToPush = prependPushRegistry(imageConfiguration, registryConfig);
            kitLogger.info("This push refers to: %s", imageConfigToPush.getName());
            kitLogger.info("Pushing image: %s", new ImageName(imageConfigToPush.getName()).getFullName());
            JibServiceUtil.jibPush(
              imageConfigToPush,
                getRegistryCredentials(registryConfig, true, getPushRegistry(imageConfigToPush, registryConfig)),
                getBuildTarArchive(imageConfigToPush, configuration),
              jibLogger
            );
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when push JIB image", ex);
        }
    }

    @Override
    public void postProcess() {
        // No post processing required
    }

    static ImageConfiguration prependPushRegistry(ImageConfiguration imageConfiguration, RegistryConfig registryConfig) {
        final ImageConfiguration.ImageConfigurationBuilder icBuilder = imageConfiguration.toBuilder();
        final ImageName imageName = new ImageName(imageConfiguration.getName());
        final String pushRegistry = getPushRegistry(imageConfiguration, registryConfig);
        if (!imageName.hasRegistry() && pushRegistry != null) {
            icBuilder.name(imageName.getFullName(pushRegistry));
            icBuilder.registry(pushRegistry);
        }
        return icBuilder.build();
    }

    static File getAssemblyTarArchive(ImageConfiguration imageConfig, JKubeConfiguration configuration, KitLogger log) throws IOException {
        log.info("Preparing assembly files");
        final String targetImage = imageConfig.getName();
        return AssemblyManager.getInstance()
                .createDockerTarArchive(targetImage, configuration, imageConfig.getBuildConfiguration(), log, null);
    }

    Credential getRegistryCredentials(RegistryConfig registryConfig, boolean isPush, String registry)
        throws IOException {

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

    static String getPullRegistry(ImageConfiguration imageConfiguration, RegistryConfig registryConfig) {
        return getApplicablePullRegistryFrom(getBaseImage(imageConfiguration, null), registryConfig);
    }

    static String getPushRegistry(ImageConfiguration imageConfiguration, RegistryConfig registryConfig) {
        return getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    }
}
