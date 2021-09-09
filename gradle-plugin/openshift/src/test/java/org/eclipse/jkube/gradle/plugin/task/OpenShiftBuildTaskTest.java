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

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;

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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenShiftBuildTaskTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private MockedConstruction<OpenshiftBuildService> openshiftBuildServiceMockedConstruction;
  private Project project;
  private OpenShiftExtension extension;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(mock(Logger.class));
    });
    openshiftBuildServiceMockedConstruction = mockConstruction(OpenshiftBuildService.class,
        (mock, ctx) -> when(mock.isApplicable()).thenReturn(true));
    extension = new TestOpenShiftExtension();
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @After
  public void tearDown() {
    openshiftBuildServiceMockedConstruction.close();
    defaultTaskMockedConstruction.close();
  }

  @Test
  public void getLogPrefix_withDefaults_shouldReturnOc() {
    assertThat(new OpenShiftBuildTask(OpenShiftExtension.class).getLogPrefix()).isEqualTo("oc: ");
  }

  @Test
  public void runTask_withImageConfiguration_shouldRunBuild() throws JKubeServiceException {
    // Given
    final OpenShiftBuildTask buildTask = new OpenShiftBuildTask(OpenShiftExtension.class);
    // When
    buildTask.runTask();
    // Then
    assertThat(buildTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(OpenshiftBuildService.class);
    verify(buildTask.jKubeServiceHub.getBuildService(), times(1)).build(any());
  }

}
