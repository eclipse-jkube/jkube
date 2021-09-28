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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesPushTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private KubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class, (mock, ctx) -> {
      when(mock.isApplicable()).thenReturn(true);
    });
    extension = new TestKubernetesExtension() {
      @Override
      public boolean isDockerAccessRequired() { return false; }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @After
  public void tearDown() {
    dockerBuildServiceMockedConstruction.close();
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
        .push(eq(extension.images), eq(0), any(), eq(false));
  }

}
