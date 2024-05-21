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
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.event.events.ProgressEvent;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePushRegistryFrom;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.toRegistryImage;

public class JibService implements AutoCloseable {

  private static final long JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 10L;

  private final AuthConfigFactory authConfigFactory;
  private final JKubeConfiguration configuration;
  private final ImageConfiguration imageConfiguration;
  private final JibLogger jibLogger;
  private final ExecutorService executorService;

  public JibService(KitLogger kitLogger, AuthConfigFactory authConfigFactory, JKubeConfiguration configuration, ImageConfiguration imageConfiguration) {
    this.authConfigFactory = authConfigFactory;
    this.configuration = configuration;
    this.imageConfiguration = prependPushRegistry(imageConfiguration, configuration);
    jibLogger = new JibLogger(kitLogger);
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

  public final void push() {
    final String imageName = getImageName().getFullName();
    final TarImage image = TarImage.at(getBuildTarArchive().toPath());
    final RegistryImage registryImage = toRegistryImage(imageName, getPushRegistryCredentials());
    final Containerizer to = Containerizer.to(registryImage);
    final JibContainerBuilder from = Jib.from(image);
    containerize(from, to);
  }

  public final ImageName getImageName() {
    return new ImageName(imageConfiguration.getName());
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

  private File getBuildTarArchive() {
    final BuildDirs buildDirs = new BuildDirs(imageConfiguration.getName(), configuration);
    return new File(buildDirs.getTemporaryRootDirectory(), JKubeBuildTarArchiver.ARCHIVE_FILE_NAME + ArchiveCompression.none.getFileSuffix());
  }

  private Credential getPushRegistryCredentials() {
    final RegistryConfig registryConfig = configuration.getPushRegistryConfig();
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    try {
      final AuthConfig standardAuthConfig = authConfigFactory.
        createAuthConfig(true, registryConfig.isSkipExtendedAuth(), registryConfig.getAuthConfig(), registryConfig.getSettings(), null, pushRegistry, registryConfig.getPasswordDecryptionMethod());
      Credential credentials = null;
      if (standardAuthConfig != null) {
        credentials = Credential.from(standardAuthConfig.getUsername(), standardAuthConfig.getPassword());
      }
      return credentials;
    } catch (IOException exception) {
      throw new JKubeException("Error when getting push registry credentials", exception);
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
