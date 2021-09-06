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

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
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

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class OpenShiftBuildTaskTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private OpenShiftBuildTask openShiftBuildTask;
  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private Project project;
  private OpenShiftExtension extension;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(mock(Logger.class));
    });
    extension = new TestOpenShiftExtension();
    extension.access =  ClusterConfiguration.builder().username("invalid").password("invalid").build();
    when(project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
    openShiftBuildTask = new OpenShiftBuildTask(OpenShiftExtension.class);
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @Test
  public void testGetLogPrefix() {
    assertThat(openShiftBuildTask.getLogPrefix()).isEqualTo("oc: ");
  }

  @Test
  public void runTask_withImageConfiguration_shouldThrowExceptionWhenNoOpenShift() {
    // Given
    final OpenShiftBuildTask openShiftBuildTask = new OpenShiftBuildTask(OpenShiftExtension.class);
    // When
    KubernetesClientException kubernetesClientException = assertThrows(KubernetesClientException.class, openShiftBuildTask::runTask);
    // Then
    assertThat(kubernetesClientException.getMessage())
      .isEqualTo("An error has occurred.");
  }

  @Test
  public void runTask_withImageConfigurationAndJibBuildStrategy_shouldGetBuildService() {
    // Given
    extension.buildStrategy = JKubeBuildStrategy.jib;
    final OpenShiftBuildTask openShiftBuildTask = new OpenShiftBuildTask(OpenShiftExtension.class);
    // When
    openShiftBuildTask.runTask();
    // Then
    AssertionsForInterfaceTypes.assertThat(openShiftBuildTask.resolvedImages)
      .singleElement()
      .hasFieldOrPropertyWithValue("name", "foo/bar:latest");
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
  }
}
