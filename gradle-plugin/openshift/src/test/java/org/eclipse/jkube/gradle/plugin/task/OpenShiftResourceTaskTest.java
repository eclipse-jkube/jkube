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

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class OpenShiftResourceTaskTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private Project project;
  private TestOpenShiftExtension testOpenShiftExtension;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(mock(Logger.class));
    });
    testOpenShiftExtension = new TestOpenShiftExtension();
    when(project.getGroup()).thenReturn("org.eclipse.jkube.testing");
    when(project.getName()).thenReturn("test-project");
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(testOpenShiftExtension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
  }

  @Test
  public void runTask_withImageConfiguration_shouldGenerateResources() {
    // Given
    OpenShiftResourceTask resourceTask = new OpenShiftResourceTask(OpenShiftExtension.class);
    // When
    resourceTask.runTask();
    // Then
    assertThat(temporaryFolder.getRoot().toPath()
      .resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "openshift.yml")))
      .exists()
      .hasContent("---\n" +
        "apiVersion: v1\n" +
        "kind: List\n");
  }

  @Test
  public void runTask_resolvesGroupInImageNameToNamespaceSetViaConfiguration_whenNoNamespaceDetected() {
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
      testOpenShiftExtension.images = Collections.singletonList(imageConfiguration);
      OpenShiftResourceTask resourceTask = new OpenShiftResourceTask(OpenShiftExtension.class);

      // When
      resourceTask.runTask();

      // Then
      assertThat(resourceTask.resolvedImages)
        .hasSize(1)
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "test-custom-namespace/test-project");
    }
  }
}
