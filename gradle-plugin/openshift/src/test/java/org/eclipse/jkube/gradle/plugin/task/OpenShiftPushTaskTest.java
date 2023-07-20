/*
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftPushTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<OpenshiftBuildService> openshiftBuildServiceMockedConstruction;
  private TestOpenShiftExtension extension;

  @BeforeEach
  void setUp() {
    openshiftBuildServiceMockedConstruction = mockConstruction(OpenshiftBuildService.class,
        (mock, ctx) -> when(mock.isApplicable()).thenReturn(true));
    extension = new TestOpenShiftExtension();
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
  void run_withImageConfigurationAndS2IBuildStrategy_shouldPushImage() throws JKubeServiceException {
      // Given
      final OpenShiftPushTask openShiftPushTask = new OpenShiftPushTask(OpenShiftExtension.class);
      // When
      openShiftPushTask.runTask();
      // Then
      assertThat(openshiftBuildServiceMockedConstruction.constructed()).hasSize(1);
      verify(openshiftBuildServiceMockedConstruction.constructed().iterator().next())
          .push(eq(extension.images), eq(0), any(), eq(false));
  }

  @Test
  void run_withSkipPush_shouldNotPushImage() {
    // Given
    extension.isSkipPush = true;
    final OpenShiftPushTask openShiftPushTask = new OpenShiftPushTask(OpenShiftExtension.class);
    // When
    openShiftPushTask.runTask();
    // Then
    assertThat(openshiftBuildServiceMockedConstruction.constructed()).isEmpty();
  }
}
