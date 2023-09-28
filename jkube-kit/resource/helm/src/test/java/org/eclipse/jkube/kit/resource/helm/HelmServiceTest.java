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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelmServiceTest {

  private HelmConfig.HelmConfigBuilder helmConfig;
  private JKubeConfiguration jKubeConfiguration;
  private ResourceServiceConfig resourceServiceConfig;
  private HelmService helmService;
  private Path helmSourceDirectory;
  private Path helmOutputDirectory;

  @BeforeEach
  void setUp(@TempDir Path tempDir) throws IOException {
    helmSourceDirectory = Files.createDirectory(tempDir.resolve("helm-source"));
    Files.createDirectory(helmSourceDirectory.resolve("kubernetes"));
    Files.createFile(helmSourceDirectory.resolve("kubernetes").resolve("deployment.yaml"));
    helmOutputDirectory = Files.createDirectory(tempDir.resolve("helm-output"));
    Files.createDirectory(helmOutputDirectory.resolve("kubernetes"));
    helmConfig = HelmConfig.builder()
      .sourceDir(helmSourceDirectory.toFile().getAbsolutePath())
      .outputDir(helmOutputDirectory.toFile().getAbsolutePath())
      .tarballOutputDir(helmOutputDirectory.toFile().getAbsolutePath());
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
  void prepareSourceDirValid_withNonExistentDirectory_shouldThrowException() throws IOException {
    // Given
    Files.createDirectory(helmSourceDirectory.resolve("openshift"));
    // When
    IOException result = assertThrows(IOException.class,
            () -> HelmService.prepareSourceDir(helmConfig.build(), HelmType.OPENSHIFT));
    // Then
    assertThat(result).isNotNull()
            .hasMessageStartingWith("Chart source directory ")
            .hasMessageEndingWith("you need run 'mvn kubernetes:resource' before.");
  }

  @Test
  void generateHelmCharts() throws IOException {
    // Given
    helmConfig
      .types(Collections.singletonList(HelmType.KUBERNETES))
      .apiVersion("v1337").chart("Chart Name").version("1337");
    // When
    helmService.generateHelmCharts(helmConfig.build());
    // Then
    final Map<String, Object> chartYaml = Serialization.unmarshal(
      helmOutputDirectory.resolve("kubernetes").resolve("Chart.yaml").toFile(),
      new TypeReference<Map<String, Object>>() {});
    assertThat(chartYaml)
      .contains(
        entry("apiVersion", "v1337"),
        entry("name", "Chart Name"),
        entry("version", "1337")
      );
  }

  @Test
  void generateHelmCharts_withInvalidChartYamlFragmentProvided_throwsException() {
    // Given
    helmConfig.types(Collections.singletonList(HelmType.KUBERNETES));
    resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
      new File(Objects.requireNonNull(getClass().getResource("/invalid-helm-chart-fragment")).getFile())))
      .build();
    // When + Then
    assertThatIllegalArgumentException()
      .isThrownBy(() ->
        new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger()).generateHelmCharts(helmConfig.build()))
      .withMessageStartingWith("Failure in parsing Helm fragment (Chart.helm.yaml): ");
  }

  @Test
  void generateHelmCharts_withInvalidValuesYamlFragmentProvided_throwsException() {
    // Given
    helmConfig.types(Collections.singletonList(HelmType.KUBERNETES));
    resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
      new File(Objects.requireNonNull(getClass().getResource("/invalid-helm-values-fragment")).getFile())))
      .build();
    // When + Then
    assertThatIllegalArgumentException()
      .isThrownBy(() ->
        new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger()).generateHelmCharts(helmConfig.build()))
      .withMessageStartingWith("Failure in parsing Helm fragment (values.helm.yaml): ");
  }

  @Test
  void generateHelmCharts_withValidChartYamlFragment_usesMergedChart() throws Exception {
    // Given
    jKubeConfiguration.getProject().getProperties().put("chart.name", "name-from-fragment");
    resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
        new File(Objects.requireNonNull(getClass().getResource("/valid-helm-fragments")).getFile())))
      .build();
    helmConfig
      .types(Collections.singletonList(HelmType.KUBERNETES))
      .apiVersion("v1")
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
      .generateHelmCharts(helmConfig.build());
    // Then
    final Map<?, ?> savedChart = Serialization.unmarshal(helmOutputDirectory.resolve("kubernetes").resolve("Chart.yaml").toFile(), Map.class);
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
  void generateHelmCharts_withValidValuesYamlFragment_usesMergedValues() throws Exception {
    // Given
    jKubeConfiguration.getProject().getProperties().put("property.in.fragment", "the-value");
    resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
        new File(Objects.requireNonNull(getClass().getResource("/valid-helm-fragments")).getFile())))
      .build();
    helmConfig
      .types(Collections.singletonList(HelmType.KUBERNETES))
      .parameters(Arrays.asList(
        HelmParameter.builder().name("ingress.name").value("the-ingress-from-parameter").build(),
        HelmParameter.builder().name("ingress.enabled").value("is overridden").build()
      ));
    // When
    new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger())
      .generateHelmCharts(helmConfig.build());
    // Then
    final Map<?, ?> savedValues = Serialization.unmarshal(helmOutputDirectory.resolve("kubernetes").resolve("values.yaml").toFile(), Map.class);
    assertThat(savedValues)
      .hasFieldOrPropertyWithValue("replaceableProperty", "the-value")
      .hasFieldOrPropertyWithValue("replicaCount", 1)
      .hasFieldOrPropertyWithValue("ingress.name", "the-ingress-from-parameter")
      .hasFieldOrPropertyWithValue("ingress.enabled", false)
      .hasFieldOrPropertyWithValue("ingress.tls", Collections.emptyList())
      .extracting("ingress.annotations").asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .containsOnly(
        entry("kubernetes.io/ingress.class", "nginx"),
        entry("kubernetes.io/tls-acme", "true")
      );
  }

  @Test
  void createChartYamlWithDependencies() throws Exception {
    // Given
    helmConfig.types(Collections.singletonList(HelmType.KUBERNETES));
    final HelmDependency helmDependency = new HelmDependency()
        .toBuilder()
        .name("nginx")
        .version("1.2.3.")
        .repository("repository")
        .build();
    helmConfig.apiVersion("v1").chart("Chart Name").version("1337")
        .dependencies(Collections.singletonList(helmDependency));
    // When
    helmService.generateHelmCharts(helmConfig.build());
    // Then
    final Map<String, Object> chartYaml = Serialization.unmarshal(
      helmOutputDirectory.resolve("kubernetes").resolve("Chart.yaml").toFile(),
      new TypeReference<Map<String, Object>>() {});
    assertThat(chartYaml)
      .contains(
        entry("apiVersion", "v1"),
        entry("name", "Chart Name"),
        entry("version", "1337")
      )
      .extracting("dependencies").asList().singleElement()
      .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .contains(
        entry("name", "nginx"),
        entry("version", "1.2.3."),
        entry("repository", "repository")
      );
    }
}
