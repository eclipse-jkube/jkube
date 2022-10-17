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
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.watcher.api.WatcherManager;

import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class WatchMojoTest {
  private File kubernetesManifestFile;

  private JKubeServiceHub mockedJKubeServiceHub;
  private ClusterAccess mockedClusterAccess;
  private MavenProject mavenProject;
  private JavaProject mockedJavaProject;
  private Settings mavenSettings;
  private OpenShiftClient mockKubernetesClient;
  private ImageConfigResolver mockedImageConfigResolver;
  private DockerAccessFactory mockedDockerAccessFactory;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;

  private WatchMojo watchMojo;

  @BeforeEach
  void setUp(@TempDir File temporaryFolder) throws IOException {
    kubernetesManifestFile =  File.createTempFile("kubernetes", ".yml", temporaryFolder);
    mockedJKubeServiceHub = mock(JKubeServiceHub.class);
    mavenProject = mock(MavenProject.class);
    mavenSettings = mock(Settings.class);
    mockedImageConfigResolver = mock(ImageConfigResolver.class);
    mockKubernetesClient = mock(OpenShiftClient.class);
    mockedDockerAccessFactory = mock(DockerAccessFactory.class);
    mockedJavaProject = mock(JavaProject.class);
    mockedClusterAccess = mock(ClusterAccess.class);
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    SummaryService mockedSummaryService = mock(SummaryService.class);

    when(mockedJKubeServiceHub.getApplyService()).thenReturn(new ApplyService(mockKubernetesClient, new KitLogger.SilentLogger(), mockedSummaryService));
    when(mockedJavaProject.getProperties()).thenReturn(new Properties());
    when(mavenProject.getArtifactId()).thenReturn("artifact-id");
    when(mavenProject.getVersion()).thenReturn("1337");
    when(mavenProject.getDescription()).thenReturn("A description from Maven");
    when(mavenProject.getParent()).thenReturn(null);
    when(mockKubernetesClient.adapt(OpenShiftClient.class)).thenReturn(mockKubernetesClient);
    when(mockKubernetesClient.isSupported()).thenReturn(false);
    when(mockKubernetesClient.getMasterUrl()).thenReturn(URI.create("https://www.example.com").toURL());
    when(mockedClusterAccess.getNamespace()).thenReturn("namespace-from-config");
  }

  @AfterEach
  void tearDown() {
    mavenProject = null;
    watchMojo = null;
    watcherManagerMockedStatic.close();
  }

  @Test
  void executeInternal_whenNoNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws MojoExecutionException {
    // Given
    // @formatter:off
    watchMojo = new WatchMojo() {{
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = mockedImageConfigResolver;
      dockerAccessFactory = mockedDockerAccessFactory;
      kubernetesClient = mockKubernetesClient;
      javaProject = mockedJavaProject;
      jkubeServiceHub = mockedJKubeServiceHub;
      clusterAccess = mockedClusterAccess;
    }};
    // @formatter:on

    // When
    watchMojo.executeInternal();

    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-config"), any(), any()), times(1));
  }

  @Test
  void executeInternal_whenNamespaceConfiguredInResourceConfig_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws MojoExecutionException {
    // Given
    ResourceConfig mockedResourceConfig = mock(ResourceConfig.class);
    when(mockedResourceConfig.getNamespace()).thenReturn("namespace-from-resourceconfig");
    // @formatter:off
    watchMojo = new WatchMojo() {{
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = mockedImageConfigResolver;
      dockerAccessFactory = mockedDockerAccessFactory;
      kubernetesClient = mockKubernetesClient;
      javaProject = mockedJavaProject;
      jkubeServiceHub = mockedJKubeServiceHub;
      clusterAccess = mockedClusterAccess;
      resources = mockedResourceConfig;
    }};
    // @formatter:on

    // When
    watchMojo.executeInternal();

    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-resourceconfig"), any(), any()), times(1));
  }

  @Test
  void executeInternal_whenNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws MojoExecutionException {
    // Given
    // @formatter:off
    watchMojo = new WatchMojo() {{
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = mockedImageConfigResolver;
      dockerAccessFactory = mockedDockerAccessFactory;
      kubernetesClient = mockKubernetesClient;
      javaProject = mockedJavaProject;
      jkubeServiceHub = mockedJKubeServiceHub;
      clusterAccess = mockedClusterAccess;
      namespace = "configured-namespace";
    }};
    // @formatter:on

    // When
    watchMojo.executeInternal();

    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("configured-namespace"), any(), any()), times(1));
  }
}
