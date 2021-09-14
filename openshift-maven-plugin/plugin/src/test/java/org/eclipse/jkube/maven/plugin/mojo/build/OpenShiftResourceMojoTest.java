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

import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.execution.MavenSession;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class OpenShiftResourceMojoTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private MavenProject mockedMavenProject;

  @Mocked
  private MavenSession mockedMavenSession;

  @Mocked
  private Settings mockedSettings;

  @Mocked
  private JKubeServiceHub mockedJKubeServiceHub;

  @Mocked
  private ClusterAccess mockedClusterAccess;

  @Mocked
  private KitLogger mockedLogger;

  @Mocked
  private MavenProjectHelper mockedMavenProjectHelper;

  @Mocked
  private ImageConfigResolver mockedImageConfigResolver;

  private OpenshiftResourceMojo openShiftResourceMojo;

  @Before
  public void setUp() throws IOException {
    this.openShiftResourceMojo = new OpenshiftResourceMojo();
    this.openShiftResourceMojo.jkubeServiceHub = mockedJKubeServiceHub;
    this.openShiftResourceMojo.project = mockedMavenProject;
    this.openShiftResourceMojo.session = mockedMavenSession;
    this.openShiftResourceMojo.clusterAccess = mockedClusterAccess;
    this.openShiftResourceMojo.settings = mockedSettings;
    this.openShiftResourceMojo.log = mockedLogger;
    this.openShiftResourceMojo.skipResourceValidation = true;
    this.openShiftResourceMojo.projectHelper = mockedMavenProjectHelper;
    this.openShiftResourceMojo.imageConfigResolver = mockedImageConfigResolver;
    File buildDirectory = temporaryFolder.newFolder("target");
    new Expectations() {{
      mockedMavenProject.getArtifactId();
      result = "test-project";

      mockedMavenProject.getGroupId();
      result = "org.eclipse.jkube";

      mockedMavenProject.getProperties();
      result = new Properties();

      mockedMavenProject.getBuild().getOutputDirectory();
      result = buildDirectory.getAbsolutePath();

      mockedMavenProject.getBuild().getDirectory();
      result = buildDirectory.getAbsolutePath();

      mockedMavenProject.getBasedir();
      result = temporaryFolder.getRoot();
    }};
  }

  @Test
  public void executeInternal_resolvesGroupInImageNameToClusterAccessNamespace_whenNamespaceDetected() throws MojoExecutionException, MojoFailureException {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("%g/%a")
      .build(BuildConfiguration.builder()
        .from("test-base-image:latest")
        .build())
      .build();
    new Expectations() {{
      mockedClusterAccess.getNamespace();
      result = "test-custom-namespace";

      mockedImageConfigResolver.resolve(imageConfiguration, (JavaProject) any);
      result = Collections.singletonList(imageConfiguration);
    }};
    this.openShiftResourceMojo.images = Collections.singletonList(imageConfiguration);

    // When
    openShiftResourceMojo.executeInternal();

    // Then
    assertEquals(1, openShiftResourceMojo.resolvedImages.size());
    assertEquals("test-custom-namespace/test-project", openShiftResourceMojo.resolvedImages.get(0).getName());
  }

  @Test
  public void executeInternal_resolvesGroupInImageNameToNamespaceSetViaConfiguration_whenNoNamespaceDetected() throws MojoExecutionException, MojoFailureException {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("%g/%a")
      .build(BuildConfiguration.builder()
        .from("test-base-image:latest")
        .build())
      .build();
    new Expectations() {{
      mockedImageConfigResolver.resolve(imageConfiguration, (JavaProject) any);
      result = Collections.singletonList(imageConfiguration);
    }};
    this.openShiftResourceMojo.images = Collections.singletonList(imageConfiguration);
    this.openShiftResourceMojo.namespace = "namespace-configured-via-plugin";

    // When
    openShiftResourceMojo.executeInternal();

    // Then
    assertEquals(1, openShiftResourceMojo.resolvedImages.size());
    assertEquals("namespace-configured-via-plugin/test-project", openShiftResourceMojo.resolvedImages.get(0).getName());
  }
}
