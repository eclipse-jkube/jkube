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
package org.eclipse.jkube.kit.resource.helm;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HelmServiceTest {

  private HelmConfig.HelmConfigBuilder helmConfig;
  private JKubeConfiguration jKubeConfiguration;
  private ResourceServiceConfig resourceServiceConfig;
  private HelmService helmService;

  @BeforeEach
  void setUp() {
    helmConfig = HelmConfig.builder();
    jKubeConfiguration = JKubeConfiguration.builder()
      .project(JavaProject.builder().properties(new Properties()).build())
      .registryConfig(RegistryConfig.builder().settings(new ArrayList<>()).build()).build();
    resourceServiceConfig = new ResourceServiceConfig();
    helmService = new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger());
  }

  @AfterEach
  void tearDown() {
    helmService = null;
    helmConfig = null;
  }

  @Test
  void prepareSourceDirValid_withNonExistentDirectory_shouldThrowException() {
    File file = mock(File.class);
    // Given
    when(file.isDirectory()).thenReturn(true);
    // When
    IOException result = assertThrows(IOException.class,
            () -> HelmService.prepareSourceDir(helmConfig.build(), HelmType.OPENSHIFT));
    // Then
    assertThat(result).isNotNull()
            .hasMessageStartingWith("Chart source directory ")
            .hasMessageEndingWith("you need run 'mvn kubernetes:resource' before.");
  }

  @Test
  void createChartYaml() throws Exception {
    try (MockedStatic<ResourceUtil> resourceUtilMockedStatic = mockStatic(ResourceUtil.class)) {
      File outputDir = Files.createTempDirectory("chart-output").toFile();
      // Given
      helmConfig.chart("Chart Name").version("1337");
      // When
      helmService.createChartYaml(helmConfig.build(), outputDir);
      // Then
      ArgumentCaptor<Chart> argumentCaptor = ArgumentCaptor.forClass(Chart.class);
      resourceUtilMockedStatic.verify(() -> ResourceUtil.save(notNull(), argumentCaptor.capture(), eq(ResourceFileType.yaml)));
      assertThat(argumentCaptor.getValue())
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("name", "Chart Name")
          .hasFieldOrPropertyWithValue("version", "1337");
    }
  }

  @Test
  void createChartYaml_whenInvalidChartYamlFragmentProvided_thenThrowException(@TempDir File outputDir) throws Exception {
    // Given
    File fragmentsDir = new File(Objects.requireNonNull(getClass().getResource("/invalid-helm-fragments")).getFile());
    jKubeConfiguration = jKubeConfiguration.toBuilder()
      .project(JavaProject.builder().properties(new Properties()).build())
      .build();
    resourceServiceConfig = ResourceServiceConfig.builder()
      .resourceDirs(Collections.singletonList(fragmentsDir)).build();
    // When + Then
    assertThatIllegalArgumentException()
      .isThrownBy(() ->
        new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger()).createChartYaml(helmConfig.build(), outputDir))
      .withMessageContaining("Failure in parsing Helm Chart fragment: ");
  }

  @Test
  void createChartYaml_whenValidChartYamlFragmentProvided_thenMergeFragmentChart(@TempDir File outputDir) throws Exception {
    // Given
    File fragmentsDir = new File(Objects.requireNonNull(getClass().getResource("/valid-helm-fragments")).getFile());
    Properties properties = new Properties();
    properties.put("chart.name", "name-from-fragment");
    jKubeConfiguration = jKubeConfiguration.toBuilder()
      .project(JavaProject.builder().properties(properties).build())
      .build();
    resourceServiceConfig = ResourceServiceConfig.builder()
      .resourceDirs(Collections.singletonList(fragmentsDir)).build();
    helmConfig
      .chart("Chart Name")
      .version("1337")
      .description("Description from helmconfig")
      .home("https://example.com")
      .sources(Collections.singletonList("https://source.example.com"))
      .keywords(Collections.singletonList("ci"))
      .maintainers(Collections.singletonList(Maintainer.builder().name("maintainer-from-config").build()))
      .icon("test-icon")
      .engine("gotpl")
      .dependencies(Collections.singletonList(HelmDependency.builder().name("dependency-from-config").build()));
    // When
    new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger())
      .createChartYaml(helmConfig.build(), outputDir);
    // Then
    final Map<?, ?> savedChart = Serialization.unmarshal(new File(outputDir, "Chart.yaml"), Map.class);
    assertThat(savedChart)
      .hasFieldOrPropertyWithValue("apiVersion", "v1")
      .hasFieldOrPropertyWithValue("name", "name-from-fragment")
      .hasFieldOrPropertyWithValue("version", "version-from-fragment")
      .hasFieldOrPropertyWithValue("description", "Description from helmconfig")
      .hasFieldOrPropertyWithValue("home", "https://example.com")
      .hasFieldOrPropertyWithValue("icon", "test-icon")
      .hasFieldOrPropertyWithValue("engine", "gotpl")
      .hasFieldOrPropertyWithValue("keywords", Collections.singletonList("fragment"))
      .hasFieldOrPropertyWithValue("sources", Collections.singletonList("https://source.example.com"))
      .hasFieldOrPropertyWithValue("maintainers", Collections.singletonList(Collections.singletonMap("name", "maintainer-from-config")))
      .hasFieldOrPropertyWithValue("dependencies", Collections.singletonList(Collections.singletonMap("name", "dependency-from-config")));
  }

  @Test
  void createChartYamlWithDependencies() throws Exception {
    try (MockedStatic<ResourceUtil> resourceUtilMockedStatic = mockStatic(ResourceUtil.class)) {
      // Given
      File outputDir = Files.createTempDirectory("chart-outputdir").toFile();
      HelmDependency helmDependency = new HelmDependency()
          .toBuilder()
          .name("nginx")
          .version("1.2.3.")
          .repository("repository")
          .build();
      helmConfig.chart("Chart Name").version("1337")
          .dependencies(Collections.singletonList(helmDependency));
      // When
      helmService.createChartYaml(helmConfig.build(), outputDir);
      // Then
      ArgumentCaptor<Chart> argumentCaptor = ArgumentCaptor.forClass(Chart.class);
      resourceUtilMockedStatic.verify(() -> ResourceUtil.save(notNull(), argumentCaptor.capture(), eq(ResourceFileType.yaml)));
      assertThat(argumentCaptor.getValue())
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("name", "Chart Name")
          .hasFieldOrPropertyWithValue("version", "1337")
          .hasFieldOrPropertyWithValue("dependencies",
              Collections.singletonList(helmDependency));
    }
  }
}
