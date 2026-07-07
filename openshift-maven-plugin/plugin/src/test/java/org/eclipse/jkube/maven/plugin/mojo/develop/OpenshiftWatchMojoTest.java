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
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.watcher.api.WatcherManager;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class OpenshiftWatchMojoTest {

  private KubernetesClient kubernetesClient;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private TestOpenshiftWatchMojo watchMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    final Path srcDir = Files.createDirectory(temporaryFolder.resolve("src"));
    final File targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    File kubernetesManifestFile = Files.createTempFile(srcDir, "kubernetes", ".yml").toFile();
    MavenProject mavenProject = mock(MavenProject.class, RETURNS_DEEP_STUBS);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    when(mavenProject.getCompileClasspathElements()).thenReturn(Collections.emptyList());
    when(mavenProject.getDependencies()).thenReturn(Collections.emptyList());
    when(mavenProject.getDevelopers()).thenReturn(Collections.emptyList());
    when(mavenProject.getBasedir()).thenReturn(temporaryFolder.toFile());
    when(mavenProject.getBuild().getDirectory()).thenReturn(targetDir.getAbsolutePath());
    when(mavenProject.getBuild().getOutputDirectory()).thenReturn(targetDir.getAbsolutePath());
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    // @formatter:off
    watchMojo = new TestOpenshiftWatchMojo() {{
      project = mavenProject;
      settings = mock(Settings.class);
      kubernetesManifest = kubernetesManifestFile;
      resourceDir = temporaryFolder.resolve("src").resolve("main").resolve("jkube").toFile().getAbsoluteFile();
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
  void executeInternal_shouldDelegateToWatcherManager() throws Exception {
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(
        () -> WatcherManager.watch(any(), eq("kubernetes-client-config-namespace"), any(), any()), times(1));
  }

  @Test
  void generatorContextBuilder_shouldHaveWatchGeneratorMode() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("generatorMode", GeneratorMode.WATCH);
  }

  @Test
  void generatorContextBuilder_shouldHavePrePackagePhaseFalse() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .hasFieldOrPropertyWithValue("prePackagePhase", false);
  }

  @Test
  void generatorContextBuilder_shouldHaveOpenshiftRuntimeMode() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .hasFieldOrPropertyWithValue("runtimeMode", RuntimeMode.OPENSHIFT);
  }

  @Test
  void generatorContextBuilder_shouldHaveS2iBuildStrategy() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .hasFieldOrPropertyWithValue("strategy", JKubeBuildStrategy.s2i);
  }

  @Test
  void generatorContextBuilder_shouldPropagateWatchMode() throws Exception {
    // Given
    watchMojo.setWatchMode(WatchMode.copy);
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .hasFieldOrPropertyWithValue("watchMode", WatchMode.copy);
  }

  @Test
  void generatorContextBuilder_shouldPropagateProject() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext.getProject()).isNotNull();
  }

  @Test
  void generatorContextBuilder_shouldPropagateBuildTimestamp() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext.getBuildTimestamp()).isNotNull();
  }

  @Test
  void generatorContextBuilder_shouldPropagateUseProjectClasspath() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext)
        .hasFieldOrPropertyWithValue("useProjectClasspath", false);
  }

  @Test
  void generatorContextBuilder_shouldPropagateSourceDirectory() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext.getSourceDirectory()).isNull();
  }

  @Test
  void generatorContextBuilder_shouldPropagateFilter() throws Exception {
    // When
    watchMojo.execute();
    // Then
    assertThat(watchMojo.capturedGeneratorContext.getFilter()).isNull();
  }

  private static class TestOpenshiftWatchMojo extends OpenshiftWatchMojo {

    GeneratorContext capturedGeneratorContext;

    @Override
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() {
      GeneratorContext.GeneratorContextBuilder builder = super.generatorContextBuilder();
      capturedGeneratorContext = builder.build();
      return builder;
    }

    void setWatchMode(WatchMode watchMode) {
      this.watchMode = watchMode;
    }
  }
}
