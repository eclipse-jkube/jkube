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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.watcher.api.WatcherManager;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class WatchMojoTest {

  private KubernetesClient kubernetesClient;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private TestWatchMojo watchMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    final Path srcDir = Files.createDirectory(temporaryFolder.resolve("src"));
    final File targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    kubernetesManifestFile =  Files.createTempFile(srcDir, "kubernetes", ".yml").toFile();
    mavenProject = mock(MavenProject.class, RETURNS_DEEP_STUBS);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    when(mavenProject.getCompileClasspathElements()).thenReturn(Collections.emptyList());
    when(mavenProject.getDependencies()).thenReturn(Collections.emptyList());
    when(mavenProject.getDevelopers()).thenReturn(Collections.emptyList());
    when(mavenProject.getBasedir()).thenReturn(temporaryFolder.toFile());
    when(mavenProject.getBuild().getDirectory()).thenReturn(targetDir.getAbsolutePath());
    when(mavenProject.getBuild().getOutputDirectory()).thenReturn(targetDir.getAbsolutePath());
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    // @formatter:off
    watchMojo = new TestWatchMojo() {{
      project = mavenProject;
      settings = mock(Settings.class);
      kubernetesManifest = kubernetesManifestFile;
      resourceDir = temporaryFolder.resolve("src").resolve("main").resolve("jkube").toFile().getAbsoluteFile();
      buildStrategy = JKubeBuildStrategy.jib;
      access = ClusterConfiguration.from(
        new ConfigBuilder(kubernetesClient.getConfiguration())
          .withNamespace("kubernetes-client-config-namespace")
          .build()
      ).build();
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    watcherManagerMockedStatic.close();
  }

  @Test
  void executeInternal_whenNoNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterConfigurationNamespace() throws Exception {
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("kubernetes-client-config-namespace"), any(), any()), times(1));
  }

  @Test
  void executeInternal_whenNamespaceConfiguredInResourceConfig_shouldDelegateToWatcherManagerWithResourceConfigNamespace() throws Exception {
    // Given
    ResourceConfig resources = ResourceConfig.builder()
      .namespace("namespace-from-resource_config")
      .build();
    watchMojo.setResources(resources);
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-resource_config"), any(), any()), times(1));
  }

  @Test
  void executeInternal_whenNamespaceConfigured_shouldDelegateToWatcherManagerWithConfiguredNamespace() throws Exception {
    // Given
    watchMojo.setNamespace("configured-namespace");
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("configured-namespace"), any(), any()), times(1));
  }

  @Test
  @DisplayName("generatorContextBuilder should have WATCH generator mode")
  void generatorContextBuilder_shouldHaveWatchGeneratorMode() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("generatorMode", GeneratorMode.WATCH);
  }

  @Test
  @DisplayName("generatorContextBuilder should have prePackagePhase false")
  void generatorContextBuilder_shouldHavePrePackagePhaseFalse() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("prePackagePhase", false);
  }

  @Test
  @DisplayName("generatorContextBuilder should have Kubernetes runtime mode")
  void generatorContextBuilder_shouldHaveKubernetesRuntimeMode() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("runtimeMode", RuntimeMode.KUBERNETES);
  }

  @Test
  @DisplayName("generatorContextBuilder should not set strategy")
  void generatorContextBuilder_shouldNotSetStrategy() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("strategy", null);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate default watch mode")
  void generatorContextBuilder_shouldPropagateDefaultWatchMode() throws Exception {
    // Given
    watchMojo.setWatchMode(WatchMode.both);
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("watchMode", WatchMode.both);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate configured watch mode")
  void generatorContextBuilder_shouldPropagateWatchMode() throws Exception {
    // Given
    watchMojo.setWatchMode(WatchMode.copy);
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("watchMode", WatchMode.copy);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate project")
  void generatorContextBuilder_shouldPropagateProject() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .extracting(GeneratorContext::getProject)
        .isNotNull();
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate build timestamp")
  void generatorContextBuilder_shouldPropagateBuildTimestamp() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .extracting(GeneratorContext::getBuildTimestamp)
        .isNotNull();
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate useProjectClasspath")
  void generatorContextBuilder_shouldPropagateUseProjectClasspath() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("useProjectClasspath", false);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate source directory")
  void generatorContextBuilder_shouldPropagateSourceDirectory() throws Exception {
    // Given
    watchMojo.setSourceDirectory("src/main/docker");
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("sourceDirectory", "src/main/docker");
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate configured filter")
  void generatorContextBuilder_shouldPropagateFilter() throws Exception {
    // Given
    watchMojo.setFilter("my-image");
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("filter", "my-image");
  }

  @Test
  @DisplayName("generatorContextBuilder should have null filter when not configured")
  void generatorContextBuilder_shouldHaveNullFilterWhenNotConfigured() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .extracting(GeneratorContext::getFilter)
        .isNull();
  }

  private static class TestWatchMojo extends WatchMojo {

    GeneratorContext capturedGeneratorContext;

    @Override
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() {
      GeneratorContext.GeneratorContextBuilder builder = super.generatorContextBuilder();
      capturedGeneratorContext = builder.build();
      return builder;
    }

    void setResources(ResourceConfig resources) {
      this.resources = resources;
    }

    void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    void setWatchMode(WatchMode watchMode) {
      this.watchMode = watchMode;
    }

    void setSourceDirectory(String sourceDirectory) {
      this.sourceDirectory = sourceDirectory;
    }

    void setFilter(String filter) {
      this.filter = filter;
    }
  }
}
