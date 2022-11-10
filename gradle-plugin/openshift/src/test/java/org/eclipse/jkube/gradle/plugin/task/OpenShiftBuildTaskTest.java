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

import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftBuildTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<OpenshiftBuildService> openshiftBuildServiceMockedConstruction;

  @BeforeEach
  void setUp() {
    openshiftBuildServiceMockedConstruction = mockConstruction(OpenshiftBuildService.class,
        (mock, ctx) -> when(mock.isApplicable()).thenReturn(true));
    final OpenShiftExtension extension = new TestOpenShiftExtension();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @AfterEach
  void tearDown() {
    openshiftBuildServiceMockedConstruction.close();
  }

  @Test
  void getLogPrefix_withDefaults_shouldReturnOc() {
    assertThat(new OpenShiftBuildTask(OpenShiftExtension.class).getLogPrefix()).isEqualTo("oc: ");
  }

  @Test
  void runTask_withImageConfiguration_shouldRunBuild() throws JKubeServiceException {
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
