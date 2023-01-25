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
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;

import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesPushTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<DockerAccessFactory> dockerAccessFactoryMockedConstruction;
  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private KubernetesExtension extension;

  @BeforeEach
  void setUp() throws IOException {
    // Mock required for environments with no DOCKER available (don't remove)
    dockerAccessFactoryMockedConstruction = mockConstruction(DockerAccessFactory.class,
        (mock, ctx) -> when(mock.createDockerAccess(any())).thenReturn(mock(DockerAccess.class)));
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class,
        (mock, ctx) -> when(mock.isApplicable()).thenReturn(true));
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @AfterEach
  void tearDown() {
    dockerBuildServiceMockedConstruction.close();
    dockerAccessFactoryMockedConstruction.close();
  }

  @Test
  void run_withImageConfiguration_shouldPushImage() throws JKubeServiceException {
    // Given
    final KubernetesPushTask kubernetesPushTask = new KubernetesPushTask(KubernetesExtension.class);

    // When
    kubernetesPushTask.runTask();

    // Then
    assertThat(dockerBuildServiceMockedConstruction.constructed()).hasSize(1);
    verify(dockerBuildServiceMockedConstruction.constructed().iterator().next(), times(1))
        .push(eq(extension.images), eq(0), any(), eq(false));
  }


  @Test
  void runTask_withSkipPush_shouldDoNothing() throws JKubeServiceException {
    // Given
    extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getSkipPush() {
        return new DefaultProperty<>(Boolean.class).value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    final KubernetesPushTask pushTask = new KubernetesPushTask(KubernetesExtension.class);
    when(pushTask.getName()).thenReturn("k8sPush");

    // When
    pushTask.runTask();

    // Then
    assertThat(dockerBuildServiceMockedConstruction.constructed()).isEmpty();
    verify(pushTask.jKubeServiceHub.getBuildService(), times(0)).push(any(), anyInt(), any(), anyBoolean());
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("k8s: `k8sPush` task is skipped."));

  }
}
