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
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
      .pushRegistryConfig(RegistryConfig.builder().settings(new ArrayList<>()).build()).build();
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

  @Nested
  @DisplayName("generateHelmCharts with fragments")
  class GenerateHelmChartsWithFragments {
    @BeforeEach
    void setUp() {
      helmConfig.types(Collections.singletonList(HelmType.KUBERNETES));
    }

    @Nested
    @DisplayName("valid fragments")
    class ValidFragments {
      @BeforeEach
      void setUp() {
        resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
            new File(Objects.requireNonNull(getClass().getResource("/valid-helm-fragments")).getFile())))
          .build();
      }

      @Test
      @DisplayName("when valid chart.yaml fragment provided, then merge chart.yaml")
      void withValidChartYamlFragment_usesMergedChart() throws Exception {
        // Given
        jKubeConfiguration.getProject().getProperties().put("chart.name", "name-from-fragment");
        helmConfig
          .apiVersion("v1")
          .chart("Chart Name")
          .version("1337")
          .description("Description from helmconfig")
          .home("https://example.com")
          .sources(Collections.singletonList("https://source.example.com"))
          .keywords(Collections.singletonList("ci"))
          .maintainers(Collections.singletonList(Maintainer.builder().name("maintainer-from-config").build()))
          .icon("test-icon")
          .appVersion("1.33.7")
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
          .hasFieldOrPropertyWithValue("appVersion", "1.33.7")
          .hasFieldOrPropertyWithValue("engine", "gotpl")
          .hasFieldOrPropertyWithValue("keywords", Collections.singletonList("fragment"))
          .hasFieldOrPropertyWithValue("sources", Collections.singletonList("https://source.example.com"))
          .hasFieldOrPropertyWithValue("maintainers", Collections.singletonList(Collections.singletonMap("name", "maintainer-from-config")))
          .hasFieldOrPropertyWithValue("dependencies", Collections.singletonList(Collections.singletonMap("name", "dependency-from-config")))
          .extracting("annotations").asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
          .containsOnly(entry("example.com/jkube", "norad"));
      }

      @Test
      @DisplayName("when valid values.yaml fragment provided, then merge values.yaml")
      void withValidValuesYamlFragment_usesMergedValues() throws Exception {
        // Given
        jKubeConfiguration.getProject().getProperties().put("property.in.fragment", "the-value");
        helmConfig
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
      @DisplayName("when helm test fragment provided, then helm test resource added to templates/tests directory")
      void whenHelmTestFragmentProvided_thenFragmentCopiedToTemplatesTestDir() throws IOException {
        // Given
        jKubeConfiguration.getProject().getProperties().put("application.name", "name-configured-via-properties");
        jKubeConfiguration.getProject().getProperties().put("application.port", "79");
        resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
            new File(Objects.requireNonNull(getClass().getResource("/valid-helm-fragments")).getFile())))
          .build();
        // When
        new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger())
          .generateHelmCharts(helmConfig.build());
        // Then
        final Pod helmTestConnectionPod = Serialization.unmarshal(helmOutputDirectory.resolve("kubernetes").resolve("templates").resolve("tests").resolve("test-connection.yaml").toFile(), Pod.class);
        assertThat(helmTestConnectionPod)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "Pod")
          .hasFieldOrPropertyWithValue("metadata.name", "name-configured-via-properties-test-connection")
          .hasFieldOrPropertyWithValue("metadata.annotations", Collections.singletonMap("helm.sh/hook", "test"))
          .hasFieldOrPropertyWithValue("spec.restartPolicy", "Never")
          .extracting(Pod::getSpec)
          .extracting(PodSpec::getContainers)
          .asInstanceOf(InstanceOfAssertFactories.list(Container.class))
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "wget")
          .hasFieldOrPropertyWithValue("image", "busybox")
          .hasFieldOrPropertyWithValue("command", Collections.singletonList("wget"))
          .hasFieldOrPropertyWithValue("args", Collections.singletonList("name-configured-via-properties:79"));
      }

      @Test
      @DisplayName("when helm test fragment provided with helm parameters, then helm test resource added and interpolated to templates/tests directory")
      void whenHelmTestFragmentAndHelmParameters_thenFragmentCopiedToTemplatesTestDirAndHelmParametersInterpolated() throws IOException {
        // Given
        helmConfig.parameters(Arrays.asList(
          HelmParameter.builder().name("application.name").value("name-configured-via-parameters").build(),
          HelmParameter.builder().name("application.port").value("79").build(),
          HelmParameter.builder().name("image").value("{{ .Values.image | default \"busybox\" }}").build()
        ));
        resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
            new File(Objects.requireNonNull(getClass().getResource("/helm-test-fragment-with-parameters")).getFile())))
          .build();
        // When
        new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger())
          .generateHelmCharts(helmConfig.build());
        // Then
        assertThat(new String(Files.readAllBytes(helmOutputDirectory.resolve("kubernetes").resolve("templates").resolve("tests").resolve("test-connection.yaml"))))
          .isEqualTo(String.format("---%n" +
            "apiVersion: v1%n" +
            "kind: Pod%n" +
            "metadata:%n" +
            "  annotations:%n" +
            "    helm.sh/hook: test%n" +
            "  name: \"{{ .Values.application.name }}-test-connection\"%n" +
            "spec:%n" +
            "  containers:%n" +
            "  - image: {{ .Values.image | default \"busybox\" }}%n" +
            "    args:%n" +
            "    - \"{{ .Values.application.name }}:{{ .Values.application.port }}\"%n"));
      }
    }

    @Nested
    @DisplayName("invalid fragments")
    class InvalidFragments {
      @Test
      @DisplayName("invalid chart.yaml fragment, throw exception")
      void withInvalidChartYamlFragmentProvided_throwsException() {
        // Given
        givenResourceDir("/invalid-helm-chart-fragment");
        // When + Then
        assertThatIllegalArgumentException()
          .isThrownBy(() ->
            new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger()).generateHelmCharts(helmConfig.build()))
          .withMessageStartingWith("Failure in parsing Helm fragment (Chart.helm.yaml): ");
      }

      @Test
      @DisplayName("invalid values.yaml fragment, throw exception")
      void withInvalidValuesYamlFragmentProvided_throwsException() {
        // Given
        givenResourceDir("/invalid-helm-values-fragment");
        // When + Then
        assertThatIllegalArgumentException()
          .isThrownBy(() ->
            new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger()).generateHelmCharts(helmConfig.build()))
          .withMessageStartingWith("Failure in parsing Helm fragment (values.helm.yaml): ");
      }

      @Test
      @DisplayName("invalid test.helm.yaml fragment, throw exception")
      void withInvalidHelmTestYamlFragmentProvided_throwsException() {
        // Given
        givenResourceDir("/invalid-helm-test-fragment");
        // When + Then
        assertThatIllegalArgumentException()
          .isThrownBy(() ->
            new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger()).generateHelmCharts(helmConfig.build()))
          .withMessageStartingWith("Failure in parsing Helm fragment (test-pod.test.helm.yaml)");
      }
    }

    private void givenResourceDir(String resourceDir) {
      resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(
          new File(Objects.requireNonNull(getClass().getResource(resourceDir)).getFile())))
        .build();
    }
  }

  @Test
  void generateHelmCharts_whenInvoked_thenGeneratedValuesYamlInAlphabeticalOrder() throws IOException {
    // Given
    Path unsortedValuesYaml = helmSourceDirectory.resolve("values.helm.yaml");
    Files.write(unsortedValuesYaml, String.format(
      "root:%n" +
      "  ingress:%n" +
      "    className: \"IngressClass\"%n" +
      "    annotations:%n" +
      "      tls-acme: \"true\"%n" +
      "      ingress.class: nginx%n" +
      "    enabled: false%n" +
      "  country-codes:%n" +
      "    countries:%n" +
      "      spain: \"+34\"%n" +
      "      france: \"+33\"%n" +
      "      india: \"+91\"").getBytes());
    resourceServiceConfig = ResourceServiceConfig.builder().resourceDirs(Collections.singletonList(helmSourceDirectory.toFile())).build();
    helmConfig.types(Collections.singletonList(HelmType.KUBERNETES));

    // When
    new HelmService(jKubeConfiguration, resourceServiceConfig, new KitLogger.SilentLogger())
        .generateHelmCharts(helmConfig.build());

    // Then
    File generatedValuesYaml = helmOutputDirectory.resolve("kubernetes").resolve("values.yaml").toFile();
    assertThat(new String(Files.readAllBytes(generatedValuesYaml.toPath())))
        .isEqualTo(String.format("---%n" +
        "root:%n" +
        "  country-codes:%n" +
        "    countries:%n" +
        "      france: \"+33\"%n" +
        "      india: \"+91\"%n" +
        "      spain: \"+34\"%n" +
        "  ingress:%n" +
        "    annotations:%n" +
        "      ingress.class: nginx%n" +
        "      tls-acme: \"true\"%n" +
        "    className: IngressClass%n" +
        "    enabled: false%n"));
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
      .extracting("dependencies").asInstanceOf(InstanceOfAssertFactories.list(HelmDependency.class)).singleElement()
      .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .contains(
        entry("name", "nginx"),
        entry("version", "1.2.3."),
        entry("repository", "repository")
      );
    }
}
