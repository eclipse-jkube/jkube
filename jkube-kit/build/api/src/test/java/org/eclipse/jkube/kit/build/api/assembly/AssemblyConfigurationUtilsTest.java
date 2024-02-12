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
package org.eclipse.jkube.kit.build.api.assembly;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.createDockerFileBuilder;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFiles;

class AssemblyConfigurationUtilsTest {

  private Assembly assembly;
  private BuildConfiguration buildConfig;

  @BeforeEach
  void setUp() {
    assembly = new Assembly();
    buildConfig = BuildConfiguration.builder().build();
  }

  @Test
  void getAssemblyConfigurationOrCreateDefault_withNoConfiguration_shouldReturnDefault() {
    // Given
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfig);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "maven")
        .hasFieldOrPropertyWithValue("targetDir", "/maven")
        .hasFieldOrPropertyWithValue("user", null);
  }

  @Test
  void getAssemblyConfigurationOrCreateDefault_withConfiguration_shouldReturnConfiguration() {
    // Given
    BuildConfiguration buildConfig = BuildConfiguration.builder()
            .assembly(AssemblyConfiguration.builder()
                    .user("OtherUser")
                    .name("ImageName")
                    .build())
            .build();
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfig);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "ImageName")
        .hasFieldOrPropertyWithValue("targetDir", "/ImageName")
        .hasFieldOrPropertyWithValue("user", "OtherUser");
  }

  @Test
  void getJKubeAssemblyFileSets_withNull_shouldReturnEmptyList() {
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getJKubeAssemblyFileSets_withNullFileSets_shouldReturnEmptyList() {
    // Given
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(assembly);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getJKubeAssemblyFileSets_withNotNullFileSets_shouldReturnFileSets() {
    // Given
    AssemblyFileSet fileSet = new AssemblyFileSet();
    assembly.setFileSets(Collections.singletonList(fileSet));
    fileSet.setDirectory(new File("1337"));
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(assembly);
    // Then
    assertThat(result)
        .isNotNull()
        .singleElement()
        .hasFieldOrPropertyWithValue("directory.name", "1337");
  }

  @Test
  void getJKubeAssemblyFiles_withNull_shouldReturnEmptyList() {
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getJKubeAssemblyFiles_withNullFiles_shouldReturnEmptyList() {
    // Given
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(assembly);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getJKubeAssemblyFiles_withNotNull_shouldReturnFiles() {
    // Given
    AssemblyFile file = new AssemblyFile();
    file.setSource(new File("1337"));
    assembly.setFiles(Collections.singletonList(file));
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(assembly);
    // Then
    assertThat(result)
        .isNotNull()
        .singleElement()
        .hasFieldOrPropertyWithValue("source.name", "1337");
  }

  @Test
  void createDockerFileBuilder_withEmptyBuildConfigurationNoAssembly_shouldReturnOnlyBase() {
    // Given
    // When
    final String result = createDockerFileBuilder(buildConfig, null, null).content();
    // Then
    assertThat(result)
        .doesNotContain("COPY", "VOLUME")
        .isEqualTo("FROM busybox\n");
  }

  @Test
  void createDockerFileBuilder_withNoAssembly_shouldReturnTransformedContent() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .putEnv("ENV_VAR", "VALUE")
        .label("LABEL", "LABEL_VALUE")
        .port("8080")
        .user("1000")
        .volume("VOLUME")
        .runCmd("chown -R 1000:1000 /opt")
        .maintainer("Aitana")
        .cmd(Arguments.builder().execArgument("sh").execArgument("-c").execArgument("server").build())
        .build();
    // When
    final String result = createDockerFileBuilder(buildConfig, null, null).content();
    // Then
    assertThat(result)
        .isEqualTo("FROM busybox\n" +
            "MAINTAINER Aitana\n" +
            "ENV ENV_VAR=VALUE\n" +
            "LABEL LABEL=LABEL_VALUE\n" +
            "EXPOSE 8080\n" +
            "RUN chown -R 1000:1000 /opt\n" +
            "VOLUME [\"VOLUME\"]\n" +
            "CMD [\"sh\",\"-c\",\"server\"]\n" +
            "USER 1000\n");
  }

  @Test
  void createDockerFileBuilder_withAssemblyAndFiles_shouldReturnTransformedContent() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .putEnv("ENV_VAR", "VALUE")
        .label("LABEL", "LABEL_VALUE")
        .port("8080")
        .user("1000")
        .volume("VOLUME")
        .runCmd("chown -R 1000:1000 /opt")
        .maintainer("Alex")
        .cmd(Arguments.builder().execArgument("sh").execArgument("-c").execArgument("server").build())
        .build();
    final AssemblyConfiguration assemblyConfiguration = AssemblyConfiguration.builder()
        .targetDir("/deployments")
        .layer(Assembly.builder().id("layer-with-id").build())
        .layer(Assembly.builder().build())
        .build();
    final Map<Assembly, List<AssemblyFileEntry>> layers = assemblyConfiguration.getLayers().stream().collect(
        Collectors.toMap(Function.identity(), a -> Collections.singletonList(
            new AssemblyFileEntry(new File(""), new File(""), null))));
    // When
    final String result = createDockerFileBuilder(buildConfig, assemblyConfiguration, layers).content();
    // Then
    assertThat(result)
        .isEqualTo("FROM busybox\n" +
            "MAINTAINER Alex\n" +
            "ENV ENV_VAR=VALUE\n" +
            "LABEL LABEL=LABEL_VALUE\n" +
            "EXPOSE 8080\n" +
            "COPY /layer-with-id/deployments /deployments/\n" +
            "COPY /deployments /deployments/\n" +
            "RUN chown -R 1000:1000 /opt\n" +
            "VOLUME [\"/deployments\"]\n" +
            "VOLUME [\"VOLUME\"]\n" +
            "CMD [\"sh\",\"-c\",\"server\"]\n" +
            "USER 1000\n");
  }

  @Test
  void createDockerFileBuilder_withAssemblyAndFilesInSingleLayer_shouldReturnTransformedContent() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .user("1000")
        .maintainer("Alex")
        .build();
    final AssemblyConfiguration assemblyConfiguration = AssemblyConfiguration.builder()
        .targetDir("/deployments")
        .layer(Assembly.builder().id("layer-with-id").build())
        .layer(assembly)
        .build();
    final Map<Assembly, List<AssemblyFileEntry>> layers = assemblyConfiguration.getLayers().stream().collect(
        Collectors.toMap(Function.identity(), a -> Collections.singletonList(
            new AssemblyFileEntry(new File(""), new File(""), null))));
    layers.put(assembly, Collections.emptyList());
    // When
    final String result = createDockerFileBuilder(buildConfig, assemblyConfiguration, layers).content();
    // Then
    assertThat(result)
        .isEqualTo("FROM busybox\n" +
            "MAINTAINER Alex\n" +
            "COPY /layer-with-id/deployments /deployments/\n" +
            "VOLUME [\"/deployments\"]\n" +
            "USER 1000\n");
  }
}
