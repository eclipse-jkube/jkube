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
import java.util.Properties;

import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockSettings;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ApplyMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;
  private NamespacedOpenShiftClient defaultKubernetesClient;
  private String kubeConfigNamespace;

  private ApplyMojo applyMojo;

  @Before
  public void setUp() throws IOException {
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
          when(mock.getClient()).thenReturn(defaultKubernetesClient);
          when(mock.getClusterAccess().createDefaultClient()).thenReturn(defaultKubernetesClient);
          when(mock.getApplyService()).thenReturn(new ApplyService(defaultKubernetesClient, new KitLogger.SilentLogger()));
        });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, context) ->
        when(mock.getNamespace()).thenAnswer(invocation -> kubeConfigNamespace));
    kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
    mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    defaultKubernetesClient = mock(NamespacedOpenShiftClient.class);
    when(defaultKubernetesClient.adapt(any())).thenReturn(defaultKubernetesClient);
    when(defaultKubernetesClient.getMasterUrl()).thenReturn(URI.create("https://www.example.com").toURL());
    // @formatter:off
    applyMojo = new ApplyMojo() {{
        project = mavenProject;
        settings = mock(Settings.class);
        kubernetesManifest = kubernetesManifestFile;
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
    clusterAccessMockedConstruction.close();
    jKubeServiceHubMockedConstruction.close();
    mavenProject = null;
    applyMojo = null;
  }

  @Test
  public void executeInternalWithDefaults() throws Exception {
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("recreateMode", false);
  }

  @Test
  public void executeInternalWithProperties() throws Exception {
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
  public void testResolveEffectiveNamespaceWhenNamespacePropertySet() throws MojoExecutionException, MojoFailureException {
    // Given
    applyMojo.namespace = "configured-namespace";
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService.getNamespace()).isEqualTo("configured-namespace");
    assertThat(applyMojo.applyService.getFallbackNamespace()).isEqualTo("configured-namespace");
  }

  @Test
  public void testResolveEffectiveNamespaceWhenNamespaceSetInResourceConfig() throws MojoExecutionException, MojoFailureException {
    // Given
    applyMojo.resources = ResourceConfig.builder().namespace("xml-namespace").build();
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService.getNamespace()).isNull();
    assertThat(applyMojo.applyService.getFallbackNamespace()).isEqualTo("xml-namespace");
  }

  @Test
  public void testResolveEffectiveNamespaceWhenNoNamespaceConfigured() throws MojoExecutionException, MojoFailureException {
    // Given
    kubeConfigNamespace = "clusteraccess-namespace";
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService.getNamespace()).isNull();
    assertThat(applyMojo.applyService.getFallbackNamespace()).isEqualTo("clusteraccess-namespace");
  }

}
