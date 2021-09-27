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
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
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
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesPushTaskTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private List<ImageConfiguration> images;
  private Project project;
  private Logger logger;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, Mockito.RETURNS_DEEP_STUBS);
    logger = mock(Logger.class);

    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(logger);
    });
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class, (mock, ctx) -> {
      when(mock.isApplicable()).thenReturn(true);
    });
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    KubernetesExtension extension = new TestKubernetesExtension() {
      @Override
      public boolean isDockerAccessRequired() { return false; }
    };
    images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
    extension.images = images;
    when(project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
  }

  @Test
  public void run_withImageConfiguration_shouldPushImage() throws JKubeServiceException {
    // Given
    final KubernetesPushTask kubernetesPushTask = new KubernetesPushTask(KubernetesExtension.class);

    // When
    kubernetesPushTask.runTask();

    // Then
    assertThat(dockerBuildServiceMockedConstruction.constructed()).hasSize(1);
    verify(dockerBuildServiceMockedConstruction.constructed().iterator().next(), times(1))
      .push(eq(images), eq(0), any(), eq(false));
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
    dockerBuildServiceMockedConstruction.close();
  }
}
