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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplyMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File kubernetesManifestFile;
  private Properties mavenProperties;

  @Mocked
  private KitLogger logger;
  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private ClusterAccess clusterAccess;
  @Mocked
  private MavenProject mavenProject;
  @Mocked
  private Settings mavenSettings;
  @Mocked
  private DefaultKubernetesClient defaultKubernetesClient;

  private ApplyMojo applyMojo;

  @Before
  public void setUp() throws IOException {
    kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
    mavenProperties = new Properties();
    // @formatter:off
    applyMojo = new ApplyMojo() { {
        project = mavenProject;
        settings = mavenSettings;
        kubernetesManifest = kubernetesManifestFile;
    }};
    new Expectations(){{
      jKubeServiceHub.getApplyService(); result = new ApplyService(defaultKubernetesClient, logger);
      mavenProject.getProperties(); result = mavenProperties;
      mavenProject.getBuild().getOutputDirectory(); result = "target/classes";
      mavenProject.getBuild().getDirectory(); result = "target";
      mavenProject.getArtifactId(); result = "artifact-id";
      mavenProject.getVersion(); result = "1337";
      mavenProject.getDescription(); result = "A description from Maven";
      mavenProject.getParent(); result = null;
      defaultKubernetesClient.isAdaptable(OpenShiftClient.class); result = false;
      defaultKubernetesClient.getMasterUrl();
      result = URI.create("https://www.example.com").toURL();
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
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
    assertThat(applyMojo.applyService.getFallbackNamespace()).isNull();
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
    new Expectations() {{
      clusterAccess.getNamespace();
      result = "clusteraccess-namespace";
    }};
    // When
    applyMojo.execute();
    // Then
    assertThat(applyMojo.applyService.getNamespace()).isNull();
    assertThat(applyMojo.applyService.getFallbackNamespace()).isEqualTo("clusteraccess-namespace");
  }

}
