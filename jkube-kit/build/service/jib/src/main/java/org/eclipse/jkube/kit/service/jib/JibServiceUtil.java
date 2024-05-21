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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.Port;
import com.google.cloud.tools.jib.event.events.ProgressEvent;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import static com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer.DEFAULT_FILE_PERMISSIONS_PROVIDER;

public class JibServiceUtil {

  private JibServiceUtil() {
  }

  private static final long JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 10L;
  private static final String BUSYBOX = "busybox:latest";

  /**
   * Build container image using JIB
   *
   * @param jibContainerBuilder jib container builder object
   * @param image tarball for image
   * @param logger kit logger
   */
  public static void buildContainer(JibContainerBuilder jibContainerBuilder, TarImage image, JibLogger logger) {
    final ExecutorService jibBuildExecutor = Executors.newCachedThreadPool();
    try {
      jibContainerBuilder.setCreationTime(Instant.now());
      jibContainerBuilder.containerize(Containerizer.to(image)
        .setAllowInsecureRegistries(true)
        .setExecutorService(jibBuildExecutor)
        .addEventHandler(LogEvent.class, logger)
        .addEventHandler(ProgressEvent.class, logger.progressEventHandler()));
      logger.updateFinished();
    } catch (CacheDirectoryCreationException | IOException | ExecutionException | RegistryException ex) {
      throw new JKubeException("Unable to build the image tarball: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new JKubeException("Thread Interrupted", ex);
    } finally {
      shutdownAndWait(jibBuildExecutor);
    }
  }

  public static JibContainerBuilder containerFromImageConfiguration(
    ImageConfiguration imageConfiguration, String pullRegistry, Credential pullRegistryCredential) {
    final JibContainerBuilder containerBuilder = Jib
      .from(toRegistryImage(getBaseImage(imageConfiguration, pullRegistry), pullRegistryCredential))
      .setFormat(ImageFormat.Docker);
    if (imageConfiguration.getBuildConfiguration() != null) {
      final BuildConfiguration bic = imageConfiguration.getBuildConfiguration();
      Optional.ofNullable(bic.getEntryPoint())
        .map(Arguments::asStrings)
        .ifPresent(containerBuilder::setEntrypoint);
      Optional.ofNullable(bic.getEnv())
        .ifPresent(containerBuilder::setEnvironment);
      Optional.ofNullable(bic.getPorts()).map(List::stream)
        .map(s -> s.map(Integer::parseInt).map(Port::tcp))
        .map(s -> s.collect(Collectors.toSet()))
        .ifPresent(containerBuilder::setExposedPorts);
      Optional.ofNullable(bic.getLabels())
        .map(Map::entrySet)
        .ifPresent(labels -> labels.forEach(l -> {
          if (l.getKey() != null && l.getValue() != null) {
            containerBuilder.addLabel(l.getKey(), l.getValue());
          }
        }));
      Optional.ofNullable(bic.getCmd())
        .map(Arguments::asStrings)
        .ifPresent(containerBuilder::setProgramArguments);
      Optional.ofNullable(bic.getUser())
        .ifPresent(containerBuilder::setUser);
      Optional.ofNullable(bic.getVolumes())
        .map(List::stream)
        .map(s -> s.map(AbsoluteUnixPath::get))
        .map(s -> s.collect(Collectors.toSet()))
        .ifPresent(containerBuilder::setVolumes);
      Optional.ofNullable(bic.getWorkdir())
        .filter(((Predicate<String>) String::isEmpty).negate())
        .map(AbsoluteUnixPath::get)
        .ifPresent(containerBuilder::setWorkingDirectory);
    }
    return containerBuilder;
  }

  static RegistryImage toRegistryImage(String imageReference, Credential credential) {
    try {
      final RegistryImage registryImage = RegistryImage.named(imageReference);
      if (credential != null && !credential.getUsername().isEmpty() && !credential.getPassword().isEmpty()) {
        registryImage.addCredential(credential.getUsername(), credential.getPassword());
      }
      return registryImage;
    } catch (InvalidImageReferenceException e) {
      throw new JKubeException("Invalid image reference: " + imageReference, e);
    }
  }

  public static String getBaseImage(ImageConfiguration imageConfiguration, String optionalRegistry) {
    String baseImage = Optional.ofNullable(imageConfiguration)
      .map(ImageConfiguration::getBuildConfiguration)
      .map(BuildConfiguration::getFrom)
      .filter(((Predicate<String>) String::isEmpty).negate())
      .orElse(BUSYBOX);
    return new ImageName(baseImage).getFullName(optionalRegistry);
  }

  public static List<FileEntriesLayer> layers(BuildDirs buildDirs, Map<Assembly, List<AssemblyFileEntry>> layers) {
    final List<FileEntriesLayer> fileEntriesLayers = new ArrayList<>();
    for (Map.Entry<Assembly, List<AssemblyFileEntry>> layer : layers.entrySet()) {
      final FileEntriesLayer.Builder fel = FileEntriesLayer.builder();
      final String layerId = layer.getKey().getId();
      final Path outputPath;
      if (StringUtils.isBlank(layerId)) {
        outputPath = buildDirs.getOutputDirectory().toPath();
      } else {
        fel.setName(layerId);
        outputPath = new File(buildDirs.getOutputDirectory(), layerId).toPath();
      }
      for (AssemblyFileEntry afe : layer.getValue()) {
        final Path source = afe.getSource().toPath();
        final AbsoluteUnixPath target = AbsoluteUnixPath.get(StringUtils.prependIfMissing(
          FilenameUtils.separatorsToUnix(outputPath.relativize(afe.getDest().toPath()).normalize().toString()), "/"));
        final FilePermissions permissions = StringUtils.isNotBlank(afe.getFileMode()) ?
          FilePermissions.fromOctalString(StringUtils.right(afe.getFileMode(), 3)) :
          DEFAULT_FILE_PERMISSIONS_PROVIDER.get(source, target);
        fel.addEntry(source, target, permissions);
      }
      fileEntriesLayers.add(fel.build());
    }
    return fileEntriesLayers;
  }

  private static void shutdownAndWait(ExecutorService executorService) {
    try {
      executorService.shutdown();
      executorService.awaitTermination(JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JKubeException("Thread Interrupted", e);
    }
  }
}
