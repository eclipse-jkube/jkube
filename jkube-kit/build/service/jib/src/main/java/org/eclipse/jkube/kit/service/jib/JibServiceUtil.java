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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.buildplan.Platform;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.Port;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import static com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer.DEFAULT_FILE_PERMISSIONS_PROVIDER;

public class JibServiceUtil {

  private static final String BUSYBOX = "busybox:latest";
  private static final Platform DEFAULT_PLATFORM = new Platform("amd64", "linux");

  private JibServiceUtil() {
  }

  public static JibContainerBuilder containerFromImageConfiguration(
    ImageConfiguration imageConfiguration, String pullRegistry, Credential pullRegistryCredential
  ) {
    final String baseImage = getBaseImage(imageConfiguration, pullRegistry);
    final JibContainerBuilder containerBuilder;
    if (baseImage.equals(ImageReference.scratch() + ":latest")) {
      containerBuilder = Jib.fromScratch();
    } else {
      containerBuilder = Jib.from(toRegistryImage(baseImage, pullRegistryCredential));
    }
    containerBuilder.setFormat(ImageFormat.Docker);
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

  static ImageReference toImageReference(ImageConfiguration imageConfiguration) {
    try {
      return ImageReference.parse(imageConfiguration.getName());
    } catch (InvalidImageReferenceException e) {
      throw new JKubeException("Invalid image reference: " + imageConfiguration.getName(), e);
    }
  }

  static Set<Platform> platforms(ImageConfiguration imageConfiguration) {
    final List<String> targetPlatforms = Optional.ofNullable(imageConfiguration)
      .map(ImageConfiguration::getBuildConfiguration)
      .map(BuildConfiguration::getPlatforms)
      .orElse(Collections.emptyList());
    final Set<Platform> ret = new LinkedHashSet<>();
    for (String targetPlatform : targetPlatforms) {
      final int slashIndex = targetPlatform.indexOf('/');
      if (slashIndex >= 0) {
        final String os = targetPlatform.substring(0, slashIndex);
        final String arch = targetPlatform.substring(slashIndex + 1);
        ret.add(new Platform(arch, os));
      }
    }
    if (ret.isEmpty()) {
      ret.add(DEFAULT_PLATFORM);
    }
    return ret;
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
}
