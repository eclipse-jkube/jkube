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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HelmMojoTest {

  @TempDir
  private Path projectDir;
  private HelmMojo helmMojo;

  @BeforeEach
  void setUp() {
    helmMojo = new HelmMojo();
    helmMojo.offline = true;
    helmMojo.project = new MavenProject();
    helmMojo.settings = new Settings();
    helmMojo.interpolateTemplateParameters = true;
    helmMojo.jkubeServiceHub = JKubeServiceHub.builder()
      .configuration(JKubeConfiguration.builder().build())
      .log(new KitLogger.SilentLogger())
      .platformMode(RuntimeMode.KUBERNETES)
      .build();
    helmMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmMojo.project.setFile(projectDir.resolve("target").toFile());
  }

  @AfterEach
  void tearDown() {
    helmMojo = null;
  }

  @Test
  void executeInternal_withNoConfig_shouldInitConfigWithDefaultValuesAndGenerate() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      assertThat(helmMojo.helm).isNull();

      helmMojo.project.setArtifactId("artifact-id");
      helmMojo.project.setVersion("1337");
      helmMojo.project.setDescription("A description from Maven");
      helmMojo.project.setUrl("https://project.url");
      helmMojo.project.setScm(new Scm());
      helmMojo.project.getScm().setUrl("https://scm.url");
      final Developer developer = new Developer();
      developer.setName("John");
      developer.setEmail("john@example.com");
      helmMojo.project.setDevelopers(Arrays.asList(developer, developer));
      // When
      helmMojo.execute();
      // Then
      assertThat(helmMojo.helm).isNotNull()
          .hasFieldOrPropertyWithValue("chart", "artifact-id")
          .hasFieldOrPropertyWithValue("chartExtension", "tar.gz")
          .hasFieldOrPropertyWithValue("version", "1337")
          .hasFieldOrPropertyWithValue("description", "A description from Maven")
          .hasFieldOrPropertyWithValue("home", "https://project.url")
          .hasFieldOrPropertyWithValue("icon", null)
          .hasFieldOrPropertyWithValue("sourceDir", projectDir.resolve("target")
            .resolve("classes").resolve("META-INF").resolve("jkube") + File.separator)
          .hasFieldOrPropertyWithValue("outputDir", projectDir.resolve("target")
            .resolve("jkube").resolve("helm").resolve("artifact-id").toString())
          .hasFieldOrPropertyWithValue("tarballOutputDir", projectDir.resolve("target")
            .resolve("jkube").resolve("helm").resolve("artifact-id").toString())
          .satisfies(h -> assertThat(h.getSources()).contains("https://scm.url"))
          .satisfies(h -> assertThat(h.getMaintainers()).contains(
              new Maintainer("John", "john@example.com")))
          .satisfies(h -> assertThat(h.getAdditionalFiles()).isEmpty())
          .satisfies(h -> assertThat(h.getParameterTemplates()).isEmpty())
          .satisfies(h -> assertThat(h.getTypes()).contains(HelmConfig.HelmType.KUBERNETES));

      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      verify(helmServiceMockedConstruction.constructed().get(0), times(1)).generateHelmCharts(helmMojo.helm);
    }
  }

  @Test
  void executeInternal_withNoConfigGenerateThrowsException_shouldRethrowWithMojoExecutionException() {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class,
      (mock, ctx) -> doThrow(new IOException("Exception is thrown")).when(mock).generateHelmCharts(any())
    )) {
      // When & Then
      assertThatExceptionOfType(MojoExecutionException.class)
          .isThrownBy(() -> helmMojo.execute());
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void executeInternal_findTemplatesFromProvidedFile() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      helmMojo.kubernetesTemplate = projectDir.resolve("kubernetes.yml").toFile();
      Serialization.saveYaml(helmMojo.kubernetesTemplate, new KubernetesListBuilder()
        .addToItems(new TemplateBuilder().withNewMetadata().withName("the-template-for-params").endMetadata().build())
        .build());
      // When
      helmMojo.execute();
      // Then
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      assertThat(helmMojo.helm.getParameterTemplates()).singleElement()
        .hasFieldOrPropertyWithValue("metadata.name", "the-template-for-params");
    }
  }

  @Test
  void executeInternalFindIconUrlFromProvidedFile() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      helmMojo.kubernetesManifest = projectDir.resolve("kubernetes.yml").toFile();
      Serialization.saveYaml(helmMojo.kubernetesManifest, new KubernetesListBuilder()
        .addToItems(new ConfigMapBuilder().withNewMetadata()
        .addToAnnotations("jkube.eclipse.org/iconUrl", "https://my-icon").endMetadata().build())
        .build());
      // When
      helmMojo.execute();
      // Then
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      assertThat(helmMojo.helm.getIcon()).isEqualTo("https://my-icon");
    }
  }

}
