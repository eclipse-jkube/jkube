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

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.watcher.api.WatcherManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class WatchMojoTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File kubernetesManifestFile;

  private KitLogger logger;
  private JKubeServiceHub mockedJKubeServiceHub;
  private ClusterAccess mockedClusterAccess;
  private MavenProject mavenProject;
  private JavaProject mockedJavaProject;
  private Settings mavenSettings;
  private DefaultKubernetesClient defaultKubernetesClient;
  private ImageConfigResolver mockedImageConfigResolver;
  private DockerAccessFactory mockedDockerAccessFactory;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;

  private WatchMojo watchMojo;

  @Before
  public void setUp() throws IOException {
    kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
    logger = mock(KitLogger.class);
    mockedJKubeServiceHub = mock(JKubeServiceHub.class);
    mavenProject = mock(MavenProject.class);
    mavenSettings = mock(Settings.class);
    mockedImageConfigResolver = mock(ImageConfigResolver.class);
    defaultKubernetesClient = mock(DefaultKubernetesClient.class);
    mockedDockerAccessFactory = mock(DockerAccessFactory.class);
    mockedJavaProject = mock(JavaProject.class);
    mockedClusterAccess = mock(ClusterAccess.class);
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);

    when(mockedJKubeServiceHub.getApplyService()).thenReturn(new ApplyService(defaultKubernetesClient, logger));
    when(mockedJavaProject.getProperties()).thenReturn(new Properties());
    when(mavenProject.getArtifactId()).thenReturn("artifact-id");
    when(mavenProject.getVersion()).thenReturn("1337");
    when(mavenProject.getDescription()).thenReturn("A description from Maven");
    when(mavenProject.getParent()).thenReturn(null);
    when(defaultKubernetesClient.isAdaptable(OpenShiftClient.class)).thenReturn(false);
    when(defaultKubernetesClient.getMasterUrl()).thenReturn(URI.create("https://www.example.com").toURL());
    when(mockedClusterAccess.getNamespace()).thenReturn("namespace-from-config");
  }

  @After
  public void tearDown() {
    mavenProject = null;
    watchMojo = null;
    watcherManagerMockedStatic.close();
  }

  @Test
  public void executeInternal_whenNoNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws MojoExecutionException {
    // Given
    watchMojo = new WatchMojo() { {
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = mockedImageConfigResolver;
      dockerAccessFactory = mockedDockerAccessFactory;
      kubernetesClient = defaultKubernetesClient;
      javaProject = mockedJavaProject;
      jkubeServiceHub = mockedJKubeServiceHub;
      clusterAccess = mockedClusterAccess;
    }};

    // When
    watchMojo.executeInternal();

    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-config"), any(), any()), times(1));
  }

  @Test
  public void executeInternal_whenNamespaceConfiguredInResourceConfig_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws MojoExecutionException {
    // Given
    ResourceConfig mockedResourceConfig = mock(ResourceConfig.class);
    when(mockedResourceConfig.getNamespace()).thenReturn("namespace-from-resourceconfig");
    watchMojo = new WatchMojo() { {
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = mockedImageConfigResolver;
      dockerAccessFactory = mockedDockerAccessFactory;
      kubernetesClient = defaultKubernetesClient;
      javaProject = mockedJavaProject;
      jkubeServiceHub = mockedJKubeServiceHub;
      clusterAccess = mockedClusterAccess;
      resources = mockedResourceConfig;
    }};

    // When
    watchMojo.executeInternal();

    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("namespace-from-resourceconfig"), any(), any()), times(1));
  }

  @Test
  public void executeInternal_whenNamespaceConfigured_shouldDelegateToWatcherManagerWithClusterAccessNamespace() throws MojoExecutionException {
    // Given
    watchMojo = new WatchMojo() { {
      project = mavenProject;
      settings = mavenSettings;
      kubernetesManifest = kubernetesManifestFile;
      imageConfigResolver = mockedImageConfigResolver;
      dockerAccessFactory = mockedDockerAccessFactory;
      kubernetesClient = defaultKubernetesClient;
      javaProject = mockedJavaProject;
      jkubeServiceHub = mockedJKubeServiceHub;
      clusterAccess = mockedClusterAccess;
      namespace = "configured-namespace";
    }};

    // When
    watchMojo.executeInternal();

    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq("configured-namespace"), any(), any()), times(1));
  }
}
