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
package org.eclipse.jkube.kit.build.api.helper;


import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromConfiguration;
import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromDockerfile;

import java.io.File;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.junit.Test;

public class BuildUtilTest {

  @Test
  public void extractBaseFromConfiguration_withEmptyBuildConfiguration_shouldReturnData() {
    // When
    final String result = extractBaseFromConfiguration(new BuildConfiguration());
    // Then
    assertThat(result).isEqualTo("busybox:latest");
  }

  @Test
  public void extractBaseFromConfiguration_withBuildConfigurationAssemblyAndNoFrom_shouldReturnNull() {
    // Given
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder()
        .assembly(new AssemblyConfiguration()).build();
    // When
    final String result = extractBaseFromConfiguration(buildConfiguration);
    // Then
    assertThat(result).isNull();
  }

  @Test
  public void extractBaseFromConfiguration_withBuildConfigurationAssemblyAndFrom_shouldReturnFrom() {
    // Given
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder()
        .from("alpine:latest")
        .assembly(new AssemblyConfiguration())
        .build();
    // When
    final String result = extractBaseFromConfiguration(buildConfiguration);
    // Then
    assertThat(result).isEqualTo("alpine:latest");
  }

  @Test
  public void extractBaseFromDockerfile_withNonExistentDockerfile_shouldReturnNull() {
    // Given
    final JKubeConfiguration jKubeConfiguration = JKubeConfiguration.builder()
        .sourceDirectory("src")
        .project(JavaProject.builder()
            .baseDirectory(new File("."))
            .build()).build();
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder()
        .dockerFileFile(new File("Dockerfile")).build();
    // When
    final String result = extractBaseFromDockerfile(jKubeConfiguration, buildConfiguration);
    // Then
    assertThat(result).isNull();
  }
}