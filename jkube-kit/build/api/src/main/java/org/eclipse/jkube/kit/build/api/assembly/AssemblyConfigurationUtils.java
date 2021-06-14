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
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.apache.commons.lang3.StringUtils;

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
  static List<AssemblyFileSet> getJKubeAssemblyFileSets(@Nullable Assembly assembly) {
    return Optional.ofNullable(assembly)
        .map(Assembly::getFileSets)
        .orElse(Collections.emptyList());
  }

  @Nonnull
  static List<AssemblyFile> getJKubeAssemblyFiles(@Nullable Assembly assembly) {
    return Optional.ofNullable(assembly)
        .map(Assembly::getFiles)
        .orElse(Collections.emptyList());
  }

}