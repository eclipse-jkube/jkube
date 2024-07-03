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
import java.util.Properties;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class ApplyMojoTest {

  private File kubernetesManifestFile;
  private MavenProject mavenProject;
  private KubernetesClient defaultKubernetesClient;

  private ApplyMojo applyMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    kubernetesManifestFile = Files.createFile(temporaryFolder.resolve("kubernetes.yml")).toFile();
    mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    // @formatter:off
    applyMojo = new ApplyMojo() {{
        access = ClusterConfiguration.from(
          new ConfigBuilder(defaultKubernetesClient.getConfiguration())
            .withNamespace("kubernetes-client-config-namespace")
            .build()
        ).build();
        project = mavenProject;
        settings = mock(Settings.class);
        kubernetesManifest = kubernetesManifestFile;
        interpolateTemplateParameters = true;
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
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
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService)
        .hasFieldOrPropertyWithValue("namespace", null)
        .hasFieldOrPropertyWithValue("fallbackNamespace", "kubernetes-client-config-namespace");
  }

}
