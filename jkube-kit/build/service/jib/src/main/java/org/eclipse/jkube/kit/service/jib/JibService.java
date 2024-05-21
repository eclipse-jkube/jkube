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
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.KitLogger;
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

import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.toRegistryImage;

public class JibService implements AutoCloseable {

  private static final long JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 10L;

  private final JKubeConfiguration configuration;
  private final ImageConfiguration imageConfiguration;
  private final JibLogger jibLogger;
  private final ExecutorService executorService;

  public JibService(KitLogger kitLogger, JKubeConfiguration configuration, ImageConfiguration imageConfiguration) {
    this.configuration = configuration;
    this.imageConfiguration = imageConfiguration;
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

  public final void push(Credential pushCredentials /* TODO: remove*/) {
    final String imageName = new ImageName(imageConfiguration.getName()).getFullName();
    final TarImage image = TarImage.at(getBuildTarArchive().toPath());
    final RegistryImage registryImage = toRegistryImage(imageName, pushCredentials);
    final Containerizer to = Containerizer.to(registryImage);
    final JibContainerBuilder from = Jib.from(image);
    containerize(from, to);
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


}
