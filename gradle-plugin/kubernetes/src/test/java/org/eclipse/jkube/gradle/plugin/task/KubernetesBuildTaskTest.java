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

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class KubernetesBuildTaskTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private MockedConstruction<DockerAccessFactory> dockerAccessFactoryMockedConstruction;
  private Project project;
  private KubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(mock(Logger.class));
    });
    dockerAccessFactoryMockedConstruction = mockConstruction(DockerAccessFactory.class, (mock, ctx) -> {
      when(mock.createDockerAccess(any())).thenReturn(mock(DockerAccess.class));
    });
    extension = new TestKubernetesExtension();
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getProperties()).thenReturn(Collections.emptyMap());
    when(project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
    when(project.getGroup()).thenReturn("org.eclipse.jkube");
    when(project.getName()).thenReturn("test-build-task");
    when(project.getVersion()).thenReturn("0.0.1-SNAPSHOT");
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @After
  public void tearDown() {
    dockerAccessFactoryMockedConstruction.close();
    defaultTaskMockedConstruction.close();
  }

  @Test
  public void runTask_withImageConfiguration_shouldGetBuildService() {
    // Given
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);
    // When
    buildTask.runTask();
    // Then
    assertThat(buildTask.resolvedImages)
      .singleElement()
      .hasFieldOrPropertyWithValue("name", "foo/bar:latest");
  }

  @Test
  public void runTask_withImageConfigurationAndJibBuildStrategy_shouldGetBuildService() {
    // Given
    extension.buildStrategy = JKubeBuildStrategy.jib;
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);
    // When
    buildTask.runTask();
    // Then
    assertThat(buildTask.resolvedImages)
      .singleElement()
      .hasFieldOrPropertyWithValue("name", "foo/bar:latest");
  }
}