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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

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

import static org.mockito.Mockito.mock;

class HelmMojoTest {

  @TempDir
  private Path projectDir;
  private Path kubernetesResources;
  private HelmMojo helmMojo;

  @BeforeEach
  void setUp() throws IOException {
    helmMojo = new HelmMojo();
    helmMojo.offline = true;
    helmMojo.project = new MavenProject();
    helmMojo.projectHelper = mock(MavenProjectHelper.class);
    helmMojo.settings = new Settings();
    helmMojo.interpolateTemplateParameters = true;
    final Path target = Files.createDirectories(projectDir.resolve("target"));
    final Path classes = Files.createDirectories(target.resolve("classes"));
    kubernetesResources = Files.createDirectories(classes.resolve("META-INF").resolve("jkube").resolve("kubernetes"));
    helmMojo.project.setFile(target.toFile());
    helmMojo.project.getBuild().setDirectory(target.toFile().getAbsolutePath());
    helmMojo.project.getBuild().setOutputDirectory(classes.toFile().getAbsolutePath());
  }

  @AfterEach
  void tearDown() {
    helmMojo = null;
  }

  @Test
  void executeInternal_withNoConfig_shouldInitConfigWithDefaultValues() throws Exception {
    // Given
    Serialization.saveYaml(kubernetesResources.resolve("empty-config.yaml").toFile(), new ConfigMapBuilder().build());
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
      .hasFieldOrPropertyWithValue("apiVersion", "v1")
      .hasFieldOrPropertyWithValue("chart", "artifact-id")
      .hasFieldOrPropertyWithValue("chartExtension", "tar.gz")
      .hasFieldOrPropertyWithValue("version", "1337")
      .hasFieldOrPropertyWithValue("description", "A description from Maven")
      .hasFieldOrPropertyWithValue("home", "https://project.url")
      .hasFieldOrPropertyWithValue("icon", null)
      .hasFieldOrPropertyWithValue("appVersion", "1337")
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
  }

  @Test
  void executeInternal_withNoConfigGenerateThrowsException_shouldRethrowWithMojoExecutionException() {
    // When & Then
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> helmMojo.execute());
  }

  @Test
  void executeInternal_findTemplatesFromProvidedFile() throws Exception {
    // Given
    helmMojo.kubernetesTemplate = kubernetesResources.toFile();
    Serialization.saveYaml(kubernetesResources.resolve("helm-parameters-template.yaml").toFile(),
      new TemplateBuilder().withNewMetadata().withName("the-template-for-params").endMetadata()
        .addNewParameter().withName("key").withValue("value").endParameter()
        .build());
    // When
    helmMojo.execute();
    // Then
    assertThat(helmMojo.helm.getParameterTemplates()).singleElement()
      .hasFieldOrPropertyWithValue("metadata.name", "the-template-for-params");
    final Map<String, Object> savedChart = Serialization.unmarshal(
      projectDir.resolve("target").resolve("jkube").resolve("helm").resolve("empty-project")
        .resolve("kubernetes").resolve("values.yaml").toFile(), new TypeReference<Map<String, Object>>() {});
    assertThat(savedChart).containsEntry("key", "value");
  }

  @Test
  void executeInternalFindIconUrlFromProvidedFile() throws Exception {
    // Given
    Serialization.saveYaml(kubernetesResources.resolve("config.yaml").toFile(), new KubernetesListBuilder()
      .addToItems(new ConfigMapBuilder().withNewMetadata()
        .addToAnnotations("jkube.eclipse.org/iconUrl", "https://my-icon").endMetadata().build())
      .build());
    // When
    helmMojo.execute();
    // Then
    assertThat(helmMojo.helm.getIcon()).isEqualTo("https://my-icon");
  }

}
