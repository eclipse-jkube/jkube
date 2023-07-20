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

import java.nio.file.Paths;
import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OpenShiftResourceTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private OpenShiftExtension extension;

  @BeforeEach
  void setUp() {
    extension = new TestOpenShiftExtension();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    when(taskEnvironment.project.getGroup()).thenReturn("org.eclipse.jkube.testing");
    when(taskEnvironment.project.getName()).thenReturn("test-project");
  }

  @Test
  void runTask_withImageConfiguration_shouldGenerateResources() {
    // Given
    OpenShiftResourceTask resourceTask = new OpenShiftResourceTask(OpenShiftExtension.class);
    // When
    resourceTask.runTask();
    // Then
    assertThat(taskEnvironment.getRoot().toPath()
      .resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "openshift.yml")))
      .exists()
      .hasContent("---\n" +
        "apiVersion: v1\n" +
        "kind: List\n");
  }

  @Test
  void runTask_resolvesGroupInImageNameToNamespaceSetViaConfiguration_whenNoNamespaceDetected() {
    try (MockedStatic<KubernetesHelper> kubernetesHelper = Mockito.mockStatic(KubernetesHelper.class)) {
      // Given
      kubernetesHelper.when(KubernetesHelper::getDefaultNamespace).thenReturn("test-custom-namespace");
      kubernetesHelper.when(() -> KubernetesHelper.getKind(any())).thenReturn("DeploymentConfig");
      kubernetesHelper.when(() -> KubernetesHelper.getName((HasMetadata) any())).thenReturn("test-project");
      ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("%g/%a")
        .build(BuildConfiguration.builder()
          .from("test-base-image:latest")
          .build())
        .build();
      extension.images = Collections.singletonList(imageConfiguration);
      OpenShiftResourceTask resourceTask = new OpenShiftResourceTask(OpenShiftExtension.class);

      // When
      resourceTask.runTask();

      // Then
      assertThat(resourceTask.resolvedImages)
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "test-custom-namespace/test-project");
    }
  }
}
