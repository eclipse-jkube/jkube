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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Assembly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class AssemblyConfigurationUtils {

  private static final String LINUX_FILE_SEPARATOR = "/";
  private static final String DEFAULT_NAME = "maven";

  private AssemblyConfigurationUtils() {}

  @Nonnull
  static AssemblyConfiguration getAssemblyConfigurationOrCreateDefault(@Nullable BuildConfiguration buildConfiguration) {
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

  @Nonnull
  static List<AssemblyFileSet> getJKubeAssemblyFileSets(@Nullable AssemblyConfiguration configuration) {
    return Optional.ofNullable(configuration)
            .map(AssemblyConfiguration::getInline)
            .map(Assembly::getFileSets)
            .orElse(Collections.emptyList());
  }

  @Nonnull
  static List<AssemblyFile> getJKubeAssemblyFiles(AssemblyConfiguration configuration) {
    return Optional.ofNullable(configuration)
            .map(AssemblyConfiguration::getInline)
            .map(Assembly::getFiles)
            .orElse(Collections.emptyList());
  }
}