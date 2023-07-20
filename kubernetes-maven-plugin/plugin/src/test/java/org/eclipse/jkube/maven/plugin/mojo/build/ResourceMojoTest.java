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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ResourceMojoTest {

  @TempDir
  private Path projectDir;
  private KitLogger kitLogger;
  private ResourceMojo resourceMojo;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    resourceMojo = new ResourceMojo() {
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
    resourceMojo.mojoExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("k8s");
    resourceMojo.projectHelper = mock(MavenProjectHelper.class);
    resourceMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    resourceMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    resourceMojo.project.setFile(projectDir.resolve("target").toFile());
    resourceMojo.resourceDir = projectDir.resolve("src").resolve("main").resolve("jkube").toFile().getAbsoluteFile();
    resourceMojo.targetDir = projectDir.resolve("target").toFile().getAbsoluteFile();
    resourceMojo.workDir = projectDir.resolve("target").resolve("jkube").toFile().getAbsoluteFile();
  }

  @Test
  void execute_whenSkipTrue_shouldDoNothing() throws MojoExecutionException, MojoFailureException {
    // Given
    resourceMojo.skip = true;
    // When
    resourceMojo.execute();
    // Then
    verify(kitLogger).info("`%s` goal is skipped.", "k8s:resource");
  }

  @Test
  void execute_whenSkipResourceTrue_shouldDoNothing() throws MojoExecutionException, MojoFailureException {
    // Given
    resourceMojo.skipResource = true;
    // When
    resourceMojo.execute();
    // Then
    verify(kitLogger).info("`%s` goal is skipped.", "k8s:resource");
  }

  @Test
  void execute_generatesResourcesAndAttachesArtifact() throws Exception {
    // When
    resourceMojo.execute();
    // Then
    final File generatedArtifact = new File(resourceMojo.targetDir, "kubernetes.yml");
    assertThat(generatedArtifact)
      .exists()
      .content().isEqualTo("---\napiVersion: v1\nkind: List\n");
    verify(resourceMojo.projectHelper, times(1))
      .attachArtifact(resourceMojo.project, "yml", "kubernetes", generatedArtifact);
  }

  @Test
  void execute_writeResourcesFirstThenValidatesThem() throws Exception {
    // Given
    resourceMojo.skipResourceValidation = false;
    // When
    resourceMojo.execute();
    // Then
    final File generatedArtifact = new File(resourceMojo.targetDir, "kubernetes.yml");
    assertThat(generatedArtifact)
      .exists()
      .content().isEqualTo("---\napiVersion: v1\nkind: List\n");
    verify(resourceMojo.projectHelper, times(1))
      .attachArtifact(resourceMojo.project, "yml", "kubernetes", generatedArtifact);
    InOrder inOrder = inOrder(kitLogger);
    inOrder.verify(kitLogger).verbose("Generating resources");
    inOrder.verify(kitLogger).verbose("Validating resources");
  }
}
