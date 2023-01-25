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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenShiftResourceMojoTest {
  private ClusterAccess mockedClusterAccess;
  private ImageConfigResolver mockedImageConfigResolver;

  private OpenshiftResourceMojo openShiftResourceMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    mockedClusterAccess = mock(ClusterAccess.class, RETURNS_DEEP_STUBS);
    mockedImageConfigResolver = mock(ImageConfigResolver.class, RETURNS_DEEP_STUBS);
    Properties properties = new Properties();
    MavenProject mockedMavenProject = mock(MavenProject.class, RETURNS_DEEP_STUBS);
    JavaProject javaProject = JavaProject.builder()
        .artifactId("test-project")
        .groupId("org.eclipse.jkube")
        .properties(properties)
        .build();
    JKubeServiceHub mockedJKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);

    this.openShiftResourceMojo = new OpenshiftResourceMojo();
    this.openShiftResourceMojo.project = mockedMavenProject;
    this.openShiftResourceMojo.settings = mock(Settings.class, RETURNS_DEEP_STUBS);
    this.openShiftResourceMojo.jkubeServiceHub = mockedJKubeServiceHub;
    this.openShiftResourceMojo.clusterAccess = mockedClusterAccess;
    this.openShiftResourceMojo.log = mock(KitLogger.class, RETURNS_DEEP_STUBS);
    this.openShiftResourceMojo.skipResourceValidation = true;
    this.openShiftResourceMojo.projectHelper = mock(MavenProjectHelper.class, RETURNS_DEEP_STUBS);
    this.openShiftResourceMojo.imageConfigResolver = mockedImageConfigResolver;
    this.openShiftResourceMojo.javaProject = javaProject;
    this.openShiftResourceMojo.interpolateTemplateParameters = true;
    this.openShiftResourceMojo.resourceDir = Files.createDirectories(temporaryFolder.resolve("src").resolve("main").resolve("jkube")).toFile();

    when(mockedMavenProject.getProperties()).thenReturn(properties);
    when(mockedJKubeServiceHub.getConfiguration().getProject()).thenReturn(javaProject);
    when(mockedJKubeServiceHub.getConfiguration().getBasedir()).thenReturn(temporaryFolder.toFile());
  }

  @Test
  void executeInternal_resolvesGroupInImageNameToClusterAccessNamespace_whenNamespaceDetected() throws MojoExecutionException, MojoFailureException {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("%g/%a")
      .build(BuildConfiguration.builder()
        .from("test-base-image:latest")
        .build())
      .build();
    when(mockedClusterAccess.getNamespace()).thenReturn("test-custom-namespace");
    when(mockedImageConfigResolver.resolve(eq(imageConfiguration), any())).thenReturn(Collections.singletonList(imageConfiguration));
    openShiftResourceMojo.images = Collections.singletonList(imageConfiguration);
    openShiftResourceMojo.skip = true;

    // When
    openShiftResourceMojo.initJKubeServiceHubBuilder(openShiftResourceMojo.javaProject);
    openShiftResourceMojo.executeInternal();

    // Then
    assertThat(openShiftResourceMojo.resolvedImages).singleElement()
            .hasFieldOrPropertyWithValue("name", "test-custom-namespace/test-project");
  }

  @Test
  void executeInternal_resolvesGroupInImageNameToNamespaceSetViaConfiguration_whenNoNamespaceDetected() throws Exception {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("%g/%a")
      .build(BuildConfiguration.builder()
        .from("test-base-image:latest")
        .build())
      .build();
    when(mockedImageConfigResolver.resolve(eq(imageConfiguration), any())).thenReturn(Collections.singletonList(imageConfiguration));
    openShiftResourceMojo.images = Collections.singletonList(imageConfiguration);
    openShiftResourceMojo.namespace = "namespace-configured-via-plugin";
    openShiftResourceMojo.skip = true;

    // When
    openShiftResourceMojo.initJKubeServiceHubBuilder(openShiftResourceMojo.javaProject);
    openShiftResourceMojo.executeInternal();

    // Then
    assertThat(openShiftResourceMojo.resolvedImages).singleElement()
            .hasFieldOrPropertyWithValue("name", "namespace-configured-via-plugin/test-project");
  }
}
