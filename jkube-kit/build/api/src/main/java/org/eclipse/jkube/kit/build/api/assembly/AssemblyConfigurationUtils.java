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
package org.eclipse.jkube.kit.build.api.assembly;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;

class AssemblyConfigurationUtils {

  private static final String LINUX_FILE_SEPARATOR = "/";
  private static final String DEFAULT_NAME = "maven";

  private AssemblyConfigurationUtils() {}

  static AssemblyConfiguration getAssemblyConfigurationOrCreateDefault(BuildConfiguration buildConfiguration) {
    final AssemblyConfiguration ac = Optional.ofNullable(buildConfiguration)
            .map(BuildConfiguration::getAssembly)
            .orElse(AssemblyConfiguration.builder().build());
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = ac.toBuilder();
    final String name;
    if (StringUtils.isBlank(ac.getName())) {
      builder.name(DEFAULT_NAME);
      name = DEFAULT_NAME;
    } else {
      name = ac.getName();
    }
    if (StringUtils.isBlank(ac.getTargetDir())) {
      builder.targetDir(LINUX_FILE_SEPARATOR.concat(name));
    }
    return builder.build();
  }

  static List<AssemblyFileSet> getJKubeAssemblyFileSets(Assembly assembly) {
    return Optional.ofNullable(assembly)
        .map(Assembly::getFileSets)
        .orElse(Collections.emptyList());
  }

  static List<AssemblyFile> getJKubeAssemblyFiles(Assembly assembly) {
    return Optional.ofNullable(assembly)
        .map(Assembly::getFiles)
        .orElse(Collections.emptyList());
  }

  static DockerFileBuilder createDockerFileBuilder(BuildConfiguration buildConfig, AssemblyConfiguration assemblyConfig,
      Map<Assembly, List<AssemblyFileEntry>> layers) {
    DockerFileBuilder builder =
        new DockerFileBuilder()
            .baseImage(buildConfig.getFrom())
            .env(buildConfig.getEnv())
            .labels(buildConfig.getLabels())
            .expose(buildConfig.getPorts())
            .run(buildConfig.getRunCmds())
            .volumes(buildConfig.getVolumes())
            .user(buildConfig.getUser());
    Optional.ofNullable(buildConfig.getMaintainer()).ifPresent(builder::maintainer);
    Optional.ofNullable(buildConfig.getWorkdir()).ifPresent(builder::workdir);
    Optional.ofNullable(buildConfig.getHealthCheck()).ifPresent(builder::healthCheck);
    Optional.ofNullable(buildConfig.getCmd()).ifPresent(builder::cmd);
    Optional.ofNullable(buildConfig.getEntryPoint()).ifPresent(builder::entryPoint);
    if (assemblyConfig != null) {
      builder.basedir(assemblyConfig.getTargetDir())
          .assemblyUser(assemblyConfig.getUser())
          .exportTargetDir(assemblyConfig.getExportTargetDir());
      if (layers.isEmpty()) {
        builder.add(assemblyConfig.getTargetDir(), "");
      }
      final List<Assembly> effectiveLayers = layers.entrySet().stream()
          .filter(e -> !e.getValue().isEmpty())
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());
      for (Assembly layer: effectiveLayers) {
        if (StringUtils.isNotBlank(layer.getId())) {
          builder.add(StringUtils.prependIfMissing(layer.getId(), "/") + assemblyConfig.getTargetDir(), "");
        } else {
          builder.add(assemblyConfig.getTargetDir(), "");
        }
      }
    } else {
      builder.exportTargetDir(false);
    }

    if (buildConfig.optimise()) {
      builder.optimise();
    }
    return builder;
  }
}