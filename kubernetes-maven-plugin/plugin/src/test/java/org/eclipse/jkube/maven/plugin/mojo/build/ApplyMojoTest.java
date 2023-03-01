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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ApplyMojoTest {

  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;
  private MavenSession mavenSession;
  private MojoExecution mockedMojoExecution;
  private NamespacedOpenShiftClient defaultKubernetesClient;
  private String kubeConfigNamespace;

  private ApplyMojo applyMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    SummaryService summaryService = mock(SummaryService.class);
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
          when(mock.getClient()).thenReturn(defaultKubernetesClient);
          when(mock.getClusterAccess().createDefaultClient()).thenReturn(defaultKubernetesClient);
          when(mock.getApplyService()).thenReturn(new ApplyService(defaultKubernetesClient, new KitLogger.SilentLogger(), summaryService));
        });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, context) ->
        when(mock.getNamespace()).thenAnswer(invocation -> kubeConfigNamespace));
    kubernetesManifestFile = Files.createFile(temporaryFolder.resolve("kubernetes.yml")).toFile();
    mavenProject = mock(MavenProject.class);
    mavenSession = mock(MavenSession.class);
    mockedMojoExecution = mock(MojoExecution.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:apply"));
    when(mockedMojoExecution.getGoal()).thenReturn("k8s:apply");
    defaultKubernetesClient = mock(NamespacedOpenShiftClient.class);
    when(defaultKubernetesClient.adapt(any())).thenReturn(defaultKubernetesClient);
    when(defaultKubernetesClient.getMasterUrl()).thenReturn(URI.create("https://www.example.com").toURL());
    // @formatter:off
    applyMojo = new ApplyMojo() {{
        project = mavenProject;
        session = mavenSession;
        mojoExecution = mockedMojoExecution;
        settings = mock(Settings.class);
        kubernetesManifest = kubernetesManifestFile;
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    clusterAccessMockedConstruction.close();
    jKubeServiceHubMockedConstruction.close();
    mavenProject = null;
    applyMojo = null;
  }

  @Test
  void executeInternal_withDefaults() throws Exception {
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("recreateMode", false);
  }

  @Test
  void executeInternal_withProperties() throws Exception {
    // Given
    applyMojo.recreate = true;
    applyMojo.namespace = "custom-namespace";
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("recreateMode", true)
        .hasFieldOrPropertyWithValue("namespace", "custom-namespace");
  }

  @Test
  void resolveEffectiveNamespace_whenNamespacePropertySet() throws MojoExecutionException, MojoFailureException {
    // Given
    applyMojo.namespace = "configured-namespace";
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("namespace", "configured-namespace")
        .hasFieldOrPropertyWithValue("fallbackNamespace", "configured-namespace");
  }

  @Test
  void resolveEffectiveNamespace_whenNamespaceSetInResourceConfig() throws MojoExecutionException, MojoFailureException {
    // Given
    applyMojo.resources = ResourceConfig.builder().namespace("xml-namespace").build();
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("namespace", null)
        .hasFieldOrPropertyWithValue("fallbackNamespace", "xml-namespace");
  }

  @Test
  void resolveEffectiveNamespace_whenNoNamespaceConfigured() throws MojoExecutionException, MojoFailureException {
    // Given
    kubeConfigNamespace = "clusteraccess-namespace";
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("namespace", null)
        .hasFieldOrPropertyWithValue("fallbackNamespace", "clusteraccess-namespace");
  }

}
