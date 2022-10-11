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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
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
import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelmMojoTest {
  private MavenProject mavenProject;
  private HelmMojo helmMojo;
  private MockedStatic<ResourceUtil> resourceUtilMockedStatic;

  @BeforeEach
  void setUp() throws DependencyResolutionRequiredException {
    resourceUtilMockedStatic = mockStatic(ResourceUtil.class);
    mavenProject = mock(MavenProject.class,RETURNS_DEEP_STUBS);
    JKubeServiceHub jKubeServiceHub = mock(JKubeServiceHub.class);
    helmMojo = new HelmMojo();
    helmMojo.offline = true;
    helmMojo.project = mavenProject;
    helmMojo.settings = new Settings();
    helmMojo.jkubeServiceHub = jKubeServiceHub;
    when(mavenProject.getProperties()).thenReturn(new Properties());
    when(mavenProject.getBuild().getOutputDirectory()).thenReturn("target/classes");
    when(mavenProject.getBuild().getDirectory()).thenReturn("target");
    when(mavenProject.getCompileClasspathElements()).thenReturn(Collections.emptyList());
    when(mavenProject.getDependencies()).thenReturn(Collections.emptyList());
    when(mavenProject.getDevelopers()).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    mavenProject = null;
    helmMojo = null;
    resourceUtilMockedStatic.close();
  }

  @Test
  void executeInternal_withNoConfig_shouldInitConfigWithDefaultValuesAndGenerate() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      Scm scm = mock(Scm.class);
      Developer developer = mock(Developer.class);
      // Given
      assertThat(helmMojo.helm).isNull();

      when(mavenProject.getArtifactId()).thenReturn("artifact-id");
      when(mavenProject.getVersion()).thenReturn("1337");
      when(mavenProject.getDescription()).thenReturn("A description from Maven");
      when(mavenProject.getUrl()).thenReturn("https://project.url");
      when(mavenProject.getScm()).thenReturn(scm);
      when(scm.getUrl()).thenReturn("https://scm.url");
      when(mavenProject.getDevelopers()).thenReturn(Arrays.asList(developer, developer));
      when(developer.getName()).thenReturn("John");
      when(developer.getEmail()).thenReturn("john@example.com");
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

      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      verify(helmServiceMockedConstruction.constructed().get(0), times(1)).generateHelmCharts(helmMojo.helm);
    }
  }

  @Test
  void executeInternal_withNoConfigGenerateThrowsException_shouldRethrowWithMojoExecutionException() {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class, (mock, ctx) -> {
      doThrow(new IOException("Exception is thrown")).when(mock).generateHelmCharts(any());
    })) {
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
      File kubernetesTemplate = mock(File.class);
      helmMojo.kubernetesTemplate = kubernetesTemplate;
      Template template = mock(Template.class);
      resourceUtilMockedStatic.when(() -> ResourceUtil.load(kubernetesTemplate, KubernetesResource.class)).thenReturn(template);
      resourceUtilMockedStatic.when(() -> ResourceUtil.load(kubernetesTemplate, KubernetesResource.class)).thenReturn(template);
      // When
      helmMojo.execute();
      // Then
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      assertThat(helmMojo.helm.getParameterTemplates()).contains(template);
    }
  }

  @Test
  void executeInternalFindIconUrlFromProvidedFile() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      File kubernetesManifest = Mockito.mock(File.class);
      HasMetadata listEntry = mock(HasMetadata.class, RETURNS_DEEP_STUBS);
      helmMojo.kubernetesManifest = kubernetesManifest;
      when(kubernetesManifest.isFile()).thenReturn(true);
      resourceUtilMockedStatic.when(() -> ResourceUtil.load(kubernetesManifest, KubernetesResource.class)).thenReturn(new KubernetesList("List", Collections.singletonList(listEntry), "Invented", null));
      when(listEntry.getMetadata().getAnnotations()).thenReturn(Collections.singletonMap("jkube.io/iconUrl", "https://my-icon"));

      // When
      helmMojo.execute();
      // Then
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      assertThat(helmMojo.helm.getIcon()).isEqualTo("https://my-icon");
    }
  }

}
