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
package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory.DockerAccessContext.DEFAULT_MAX_CONNECTIONS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class KubernetesBuildTaskTest {
  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private Project project;
  private Logger logger;
  private JKubeServiceHub mockedJKubeServiceHub;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class,
        (mock, ctx) -> when(mock.getProject()).thenReturn(project));
    logger = mock(Logger.class, RETURNS_DEEP_STUBS);
    mockedJKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    final KubernetesExtension extension = mock(KubernetesExtension.class, RETURNS_DEEP_STUBS);
    File projectBaseDir = temporaryFolder.newFolder("test-build-task");
    File projectBuildDir = new File(projectBaseDir, "build");
    when(project.getProjectDir()).thenReturn(projectBaseDir);
    when(project.getBuildDir()).thenReturn(projectBuildDir);
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getProperties()).thenReturn(Collections.emptyMap());
    when(project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    when(project.getGroup()).thenReturn("org.eclipse.jkube");
    when(project.getName()).thenReturn("test-build-task");
    when(project.getVersion()).thenReturn("0.0.1-SNAPSHOT");
    when(extension.getOffline().getOrElse(false)).thenReturn(true);
    when(extension.getSkipExtendedAuth().getOrElse(false)).thenReturn(false);
    when(extension.getBuildStrategy().getOrElse(JKubeBuildStrategy.docker)).thenReturn(JKubeBuildStrategy.docker);
    when(extension.getMaxConnections().getOrElse(DEFAULT_MAX_CONNECTIONS)).thenReturn(DEFAULT_MAX_CONNECTIONS);
    when(extension.getSkipMachine().getOrElse(false)).thenReturn(false);
    when(extension.getForcePull().getOrElse(false)).thenReturn(false);
    when(extension.getRuntimeMode()).thenReturn(RuntimeMode.KUBERNETES);
    when(extension.getSourceDirectory().getOrElse("src/main/docker")).thenReturn("src/main/docker");
    extension.images = Collections.singletonList(ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .dockerFile("Dockerfile")
            .build())
        .build());
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
  }

  @Test
  public void run_withImageConfiguration_shouldGetBuildService() {
    // Given
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class) {
      @Override
      public Logger getLogger() {
        return logger;
      }

      @Override
      protected JKubeServiceHub initJKubeServiceHub() {
        return mockedJKubeServiceHub;
      }
    };
    buildTask.replaceLogger(logger);
    // When
    buildTask.runTask();
    // Then
    assertThat(buildTask.resolvedImages)
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "foo/bar:latest");
  }
}
