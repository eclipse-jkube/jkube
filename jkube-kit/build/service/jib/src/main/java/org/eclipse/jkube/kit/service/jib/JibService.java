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
package org.eclipse.jkube.kit.service.jib;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.Platform;
import com.google.cloud.tools.jib.event.events.ProgressEvent;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePullRegistryFrom;
import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePushRegistryFrom;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.containerFromImageConfiguration;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.platforms;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.toImageReference;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.toRegistryImage;

public class JibService implements AutoCloseable {

  private static final long JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 10L;

  private final JibLogger jibLogger;
  private final AuthConfigFactory authConfigFactory;
  private final JKubeConfiguration configuration;
  private final ImageConfiguration imageConfiguration;
  private final ExecutorService executorService;

  public JibService(JibLogger jibLogger, AuthConfigFactory authConfigFactory, JKubeConfiguration configuration, ImageConfiguration imageConfiguration) {
    this.jibLogger = jibLogger;
    this.authConfigFactory = authConfigFactory;
    this.configuration = configuration;
    this.imageConfiguration = prependPushRegistry(imageConfiguration, configuration);
    executorService = Executors.newCachedThreadPool();
  }

  @Override
  public void close() throws Exception {
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JKubeException("Thread Interrupted", e);
    }
  }

  public final ImageName getImageName() {
    return new ImageName(imageConfiguration.getName());
  }

  /**
   * Builds a container Jib container image tarball.
   *
   * @return the location of the generated tarball files.
   */
  public final List<File> build() {
    final List<File> generatedTarballs = new ArrayList<>();
    for (Platform platform : platforms(imageConfiguration)) {
      final JibContainerBuilder from = assembleFrom();
      from.setPlatforms(Collections.singleton(platform));
      final File jibImageTarArchive = getJibImageTarArchive(platform);
      final Containerizer to = Containerizer.to(
        TarImage.at(jibImageTarArchive.toPath())
          .named(toImageReference(imageConfiguration))
      );
      containerize(from, to);
      generatedTarballs.add(jibImageTarArchive);
    }
    return generatedTarballs;
  }

  public final void push() {
    final Set<Platform> platforms = platforms(imageConfiguration);
    final JibContainerBuilder from;
    if (platforms.size() > 1) {
      from = assembleFrom();
      from.setPlatforms(platforms);
    } else {
      from = Jib.from(TarImage.at(getJibImageTarArchive(platforms.iterator().next()).toPath()));
    }
    final Containerizer to = Containerizer
      .to(toRegistryImage(getImageName().getFullName(), getPushRegistryCredentials()));
    containerize(from, to);
  }

  private JibContainerBuilder assembleFrom() {
    final BuildDirs buildDirs = new BuildDirs(imageConfiguration.getName(), configuration);
    final String pullRegistry = getApplicablePullRegistryFrom(imageConfiguration.getBuildConfiguration().getFrom(), configuration.getPullRegistryConfig());
    final Credential pullRegistryCredential = getPullRegistryCredentials();
    final JibContainerBuilder from = containerFromImageConfiguration(imageConfiguration, pullRegistry, pullRegistryCredential);
    try {
      // Prepare Assembly files
      final AssemblyManager assemblyManager = AssemblyManager.getInstance();
      final Map<Assembly, List<AssemblyFileEntry>> layers = assemblyManager.copyFilesToFinalTarballDirectory(
        configuration,
        buildDirs,
        AssemblyManager.getAssemblyConfiguration(imageConfiguration.getBuildConfiguration(), configuration)
      );
      JibServiceUtil.layers(buildDirs, layers).forEach(from::addFileEntriesLayer);
      // TODO: Improve Assembly Manager so that the effective assemblyFileEntries computed can be properly shared
      // the call to AssemblyManager.getInstance().createDockerTarArchive should not be necessary,
      // files should be added using the AssemblyFileEntry list. AssemblyManager, should provide
      // a common way to achieve this so that both the tar builder and any other builder could get a hold of
      // archive customizers, file entries, etc.
      assemblyManager.createDockerTarArchive(
        imageConfiguration.getName(), configuration, imageConfiguration.getBuildConfiguration(), jibLogger.logger, null);
      return from;
    } catch (IOException ex) {
      throw new JKubeException("Unable to build the image tarball: " + ex.getMessage(), ex);
    }
  }

  private void containerize(JibContainerBuilder from, Containerizer to) {
    to.setAllowInsecureRegistries(true);
    to.setExecutorService(executorService);
    to.addEventHandler(LogEvent.class, jibLogger);
    to.addEventHandler(ProgressEvent.class, jibLogger.progressEventHandler());
    if (imageConfiguration.getBuildConfiguration().getTags() != null) {
      imageConfiguration.getBuildConfiguration().getTags().forEach(to::withAdditionalTag);
    }
    from.setCreationTime(Instant.now());
    try {
      from.containerize(to);
      jibLogger.updateFinished();
    } catch (CacheDirectoryCreationException | IOException | ExecutionException | RegistryException ex) {
      throw new JKubeException("Unable to containerize image using Jib: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new JKubeException("Thread Interrupted", ex);
    }
  }

  private File getJibImageTarArchive(Platform platform) {
    final BuildDirs buildDirs = new BuildDirs(imageConfiguration.getName(), configuration);
    return new File(buildDirs.getTemporaryRootDirectory(), String.format("jib-image.%s-%s.%s",
      platform.getOs(), platform.getArchitecture(), ArchiveCompression.none.getFileSuffix()));
  }

  private Credential getPullRegistryCredentials() {
    final RegistryConfig registryConfig = configuration.getPullRegistryConfig();
    final String pullRegistry = getApplicablePullRegistryFrom(imageConfiguration.getBuildConfiguration().getFrom(), registryConfig);
    return getCredentials(false, registryConfig, pullRegistry);
  }

  private Credential getPushRegistryCredentials() {
    final RegistryConfig registryConfig = configuration.getPushRegistryConfig();
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    return getCredentials(true, registryConfig, pushRegistry);
  }

  private Credential getCredentials(boolean isPush, RegistryConfig registryConfig, String registry) {
    try {
      final AuthConfig standardAuthConfig = authConfigFactory.
        createAuthConfig(isPush, registryConfig.isSkipExtendedAuth(), registryConfig.getAuthConfig(), registryConfig.getSettings(), null, registry, registryConfig.getPasswordDecryptionMethod());
      Credential credentials = null;
      if (standardAuthConfig != null) {
        credentials = Credential.from(standardAuthConfig.getUsername(), standardAuthConfig.getPassword());
      }
      return credentials;
    } catch (IOException exception) {
      throw new JKubeException("Error when getting registry credentials", exception);
    }
  }

  private static ImageConfiguration prependPushRegistry(ImageConfiguration imageConfiguration, JKubeConfiguration configuration) {
    final ImageConfiguration.ImageConfigurationBuilder icBuilder = imageConfiguration.toBuilder();
    final ImageName imageName = new ImageName(imageConfiguration.getName());
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, configuration.getPushRegistryConfig());
    if (!imageName.hasRegistry() && pushRegistry != null) {
      icBuilder.name(imageName.getFullName(pushRegistry));
      icBuilder.registry(pushRegistry);
    }
    return icBuilder.build();
  }

}
