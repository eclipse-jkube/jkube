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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.watcher.api.WatcherManager;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class WatchMojoTest {
  private File kubernetesManifestFile;

  private MavenProject mavenProject;
  private Settings mavenSettings;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private WatchMojo watchMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    final File srcDir = Files.createDirectory(temporaryFolder.resolve("src")).toFile();
    final File targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
      withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
        final KubernetesClient kc = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);
        when(kc.getMasterUrl()).thenReturn(URI.create("https://www.example.com").toURL());
        when(mock.getClient()).thenReturn(kc);
        when(mock.getConfiguration()).thenReturn(JKubeConfiguration.builder()
          .project(JavaProject.builder()
            .baseDirectory(temporaryFolder.toFile())
            .build())
          .build());
      });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      when(mock.getNamespace()).thenReturn("namespace-from-config");
    });
    kubernetesManifestFile =  File.createTempFile("kubernetes", ".yml", srcDir);
    mavenProject = mock(MavenProject.class, RETURNS_DEEP_STUBS);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    when(mavenProject.getCompileClasspathElements()).thenReturn(Collections.emptyList());
    when(mavenProject.getDependencies()).thenReturn(Collections.emptyList());
    when(mavenProject.getDevelopers()).thenReturn(Collections.emptyList());
    when(mavenProject.getBuild().getDirectory()).thenReturn(targetDir.getAbsolutePath());
    when(mavenProject.getBuild().getOutputDirectory()).thenReturn(targetDir.getAbsolutePath());
    mavenSettings = mock(Settings.class);
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    // @formatter:off
    watchMojo = new WatchMojo() {{
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = new ImageConfigResolver();
      buildStrategy = JKubeBuildStrategy.jib;
      setPluginContext(new HashMap<>());
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    watcherManagerMockedStatic.close();
    clusterAccessMockedConstruction.close();
    jKubeServiceHubMockedConstruction.close();
  }

  @Test
  void executeInternal_whenNoNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws Exception {
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-config"), any(), any()), times(1));
  }

  @Test
  void executeInternal_whenNamespaceConfiguredInResourceConfig_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws Exception {
    // Given
    watchMojo.resources = ResourceConfig.builder()
      .namespace("namespace-from-resource_config")
      .build();
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-resource_config"), any(), any()), times(1));
  }

  @Test
  void executeInternal_whenNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws Exception {
    // Given
    watchMojo.namespace = "configured-namespace";
    // When
    watchMojo.execute();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("configured-namespace"), any(), any()), times(1));
  }
}
