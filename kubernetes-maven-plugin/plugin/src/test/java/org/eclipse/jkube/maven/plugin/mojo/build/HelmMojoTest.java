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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HelmMojoTest {

  @Mocked
  private MavenProject mavenProject;
  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private HelmService helmService;

  private HelmMojo helmMojo;

  @BeforeEach
  void setUp() {
    helmMojo = new HelmMojo();
    helmMojo.offline = true;
    helmMojo.project = mavenProject;
    helmMojo.settings = new Settings();
    helmMojo.jkubeServiceHub = jKubeServiceHub;
    // @formatter:off
    new Expectations() {{
      jKubeServiceHub.getHelmService(); result = helmService;
      mavenProject.getProperties(); result = new Properties();
      mavenProject.getBuild().getOutputDirectory(); result = "target/classes";
      mavenProject.getBuild().getDirectory(); result = "target";
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    mavenProject = null;
    helmMojo = null;
    helmService = null;
  }

  @Test
  void executeInternal_withNoConfig_shouldInitConfigWithDefaultValuesAndGenerate(
      @Mocked Scm scm, @Mocked Developer developer) throws Exception {

    // Given
    assertThat(helmMojo.helm).isNull();
    // @formatter:off
    new Expectations() {{
      mavenProject.getArtifactId(); result = "artifact-id";
      mavenProject.getVersion(); result = "1337";
      mavenProject.getDescription(); result = "A description from Maven";
      mavenProject.getUrl(); result = "https://project.url";
      mavenProject.getScm(); result = scm;
      scm.getUrl(); result = "https://scm.url";
      mavenProject.getDevelopers(); result = Arrays.asList(developer, developer);
      developer.getName();
      result = "John"; result = "John"; result = null;
      developer.getEmail(); result = "john@example.com"; result = null;
    }};
    // @formatter:on
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
        .hasFieldOrPropertyWithValue("sourceDir", "target/classes/META-INF/jkube/")
        .hasFieldOrPropertyWithValue("outputDir", "target/jkube/helm/artifact-id")
        .satisfies(h -> assertThat(h.getSources()).contains("https://scm.url"))
        .satisfies(h -> assertThat(h.getMaintainers()).contains(
            new Maintainer("John", "john@example.com")))
        .satisfies(h -> assertThat(h.getAdditionalFiles()).isEmpty())
        .satisfies(h -> assertThat(h.getParameterTemplates()).isEmpty())
        .satisfies(h -> assertThat(h.getTypes()).contains(HelmConfig.HelmType.KUBERNETES))
        .satisfies(h -> assertThat(h.getTarballOutputDir()).endsWith("/target"));

    new Verifications() {{
      helmService.generateHelmCharts(helmMojo.helm);
      times = 1;
    }};
  }

  @Test
  void executeInternal_withNoConfigGenerateThrowsException_shouldRethrowWithMojoExecutionException()
      throws Exception {

    // Given
    // @formatter:off
    new Expectations() {{
      helmService.generateHelmCharts(withNotNull());
      result = new IOException("Exception is thrown");
    }};
    // formatter:on
    // When & Then
    assertThatExceptionOfType(MojoExecutionException.class)
            .isThrownBy(() -> helmMojo.execute());
  }

  @Test
  void executeInternal_findTemplatesFromProvidedFile(
      @Mocked File kubernetesTemplate, @Mocked ResourceUtil resourceUtil, @Mocked Template template) throws Exception {

    // Given
    helmMojo.kubernetesTemplate = kubernetesTemplate;
    new Expectations() {{
      ResourceUtil.load(kubernetesTemplate, KubernetesResource.class);
      result = template;
    }};
    // When
    helmMojo.execute();
    // Then
    assertThat(helmMojo.helm.getParameterTemplates()).contains(template);
  }

  @Test
  void executeInternal_findIconUrlFromProvidedFile(
      @Mocked File kubernetesManifest, @Mocked ResourceUtil resourceUtil, @Mocked HasMetadata listEntry) throws Exception {

    // Given
    helmMojo.kubernetesManifest = kubernetesManifest;
    new Expectations() {{
      kubernetesManifest.isFile();
      result = true;
      ResourceUtil.load(kubernetesManifest, KubernetesResource.class);
      result = new KubernetesList("List", Collections.singletonList(listEntry), "Invented", null);
      listEntry.getMetadata().getAnnotations();
      result = Collections.singletonMap("jkube.io/iconUrl", "https://my-icon");
    }};
    // When
    helmMojo.execute();
    // Then
    assertThat(helmMojo.helm.getIcon()).isEqualTo("https://my-icon");
  }

}
