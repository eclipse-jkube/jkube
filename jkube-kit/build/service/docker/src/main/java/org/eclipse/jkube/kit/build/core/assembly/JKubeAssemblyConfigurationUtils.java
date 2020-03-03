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
package org.eclipse.jkube.kit.build.core.assembly;

import org.eclipse.jkube.kit.build.core.config.JKubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeAssemblyFile;
import org.eclipse.jkube.kit.common.JKubeAssemblyFileSet;
import org.eclipse.jkube.kit.common.JKubeProjectAssembly;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class JKubeAssemblyConfigurationUtils {

  private static final String DEFAULT_TARGET_DIR = "/maven";
  private static final String DEFAULT_USER = "root";

  private JKubeAssemblyConfigurationUtils() {}

  static JKubeAssemblyConfiguration getAssemblyConfigurationOrCreateDefault(JKubeBuildConfiguration buildConfiguration) {
    return Optional.ofNullable(buildConfiguration)
      .map(JKubeBuildConfiguration::getAssemblyConfiguration)
      .orElse(new JKubeAssemblyConfiguration.Builder().targetDir(DEFAULT_TARGET_DIR).user(DEFAULT_USER).build());
  }

  static List<JKubeAssemblyFileSet> getJKubeAssemblyFileSets(JKubeAssemblyConfiguration configuration) {
    return Optional.ofNullable(configuration)
      .map(JKubeAssemblyConfiguration::getInline)
      .map(JKubeProjectAssembly::getFileSets)
      .orElse(Collections.emptyList());
  }

  static List<String> getJKubeAssemblyFileSetsExcludes(JKubeAssemblyConfiguration assemblyConfiguration) {
    return getJKubeAssemblyFileSets(assemblyConfiguration).stream()
      .filter(Objects::nonNull)
      .map(JKubeAssemblyFileSet::getExludes)
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  static List<JKubeAssemblyFile> getJKubeAssemblyFiles(JKubeAssemblyConfiguration configuration) {
    return Optional.ofNullable(configuration)
      .map(JKubeAssemblyConfiguration::getInline)
      .map(JKubeProjectAssembly::getFiles)
      .orElse(Collections.emptyList());
  }
}
