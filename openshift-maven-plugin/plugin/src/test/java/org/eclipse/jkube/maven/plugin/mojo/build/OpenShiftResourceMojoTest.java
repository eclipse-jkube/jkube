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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftResourceMojoTest {

  @TempDir
  private Path projectDir;
  private KitLogger kitLogger;
  private OpenshiftResourceMojo resourceMojo;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    resourceMojo = new OpenshiftResourceMojo() {
      @Override
      protected KitLogger createLogger(String prefix) {
        return kitLogger;
      }
    };
    resourceMojo.offline = true;
    resourceMojo.log = kitLogger;
    resourceMojo.interpolateTemplateParameters = true;
    resourceMojo.failOnValidationError = false;
    resourceMojo.project = new MavenProject();
    resourceMojo.settings = new Settings();
    resourceMojo.mojoExecution = new MojoExecution(new MojoDescriptor());
    resourceMojo.mojoExecution.getMojoDescriptor().setPluginDescriptor(new PluginDescriptor());
    resourceMojo.mojoExecution.getMojoDescriptor().setGoal("resource");
    resourceMojo.mojoExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("oc");
    resourceMojo.projectHelper = mock(MavenProjectHelper.class);
    resourceMojo.log = kitLogger;
    resourceMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    resourceMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    resourceMojo.project.setFile(projectDir.resolve("target").toFile());
    resourceMojo.resourceDir = projectDir.resolve("src").resolve("main").resolve("jkube").toFile().getAbsoluteFile();
    resourceMojo.targetDir = projectDir.resolve("target").toFile().getAbsoluteFile();
    resourceMojo.workDir = projectDir.resolve("target").resolve("jkube").toFile().getAbsoluteFile();
  }


  @Test
  void execute_generatesResourcesAndAttachesArtifact() throws Exception {
    // When
    resourceMojo.execute();
    // Then
    final File generatedArtifact = new File(resourceMojo.targetDir, "openshift.yml");
    assertThat(generatedArtifact)
      .exists()
      .content().isEqualTo("---\napiVersion: v1\nkind: List\n");
    verify(resourceMojo.projectHelper, times(1))
      .attachArtifact(resourceMojo.project, "yml", "openshift", generatedArtifact);
  }

  @Test
  void executeInternal_resolvesGroupInImageNameToClusterAccessNamespace_whenNamespaceDetected() throws MojoExecutionException, MojoFailureException {
    // Given
    resourceMojo.project.setArtifactId("test-project");
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("%g/%a")
      .build(BuildConfiguration.builder()
        .from("test-base-image:latest")
        .build())
      .build();
    resourceMojo.images = Collections.singletonList(imageConfiguration);
    resourceMojo.imageConfigResolver = mock(ImageConfigResolver.class);
    when(resourceMojo.imageConfigResolver.resolve(eq(imageConfiguration), any())).thenReturn(Collections.singletonList(imageConfiguration));
    resourceMojo.access = ClusterConfiguration.builder().namespace("namespace-from-cluster-access").build();
    // When
    resourceMojo.execute();
    // Then
    assertThat(resourceMojo.resolvedImages).singleElement()
      .hasFieldOrPropertyWithValue("name", "namespace-from-cluster-access/test-project");
  }

  @Test
  void execute_resolvesGroupInImageNameToNamespaceSetViaConfiguration_whenNoNamespaceDetected() throws Exception {
    // Given
    resourceMojo.project.setArtifactId("test-project");
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("%g/%a")
      .build(BuildConfiguration.builder()
        .from("test-base-image:latest")
        .build())
      .build();
    resourceMojo.images = Collections.singletonList(imageConfiguration);
    resourceMojo.imageConfigResolver = mock(ImageConfigResolver.class);
    when(resourceMojo.imageConfigResolver.resolve(eq(imageConfiguration), any())).thenReturn(Collections.singletonList(imageConfiguration));
    resourceMojo.namespace = "namespace-configured-via-plugin";
    // When
    resourceMojo.execute();
    // Then
    assertThat(resourceMojo.resolvedImages).singleElement()
      .hasFieldOrPropertyWithValue("name", "namespace-configured-via-plugin/test-project");
  }
}
