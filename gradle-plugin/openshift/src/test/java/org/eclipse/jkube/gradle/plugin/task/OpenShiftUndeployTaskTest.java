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
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;

import io.fabric8.openshift.client.OpenShiftClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenShiftUndeployTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<OpenshiftUndeployService> openshiftUndeployServiceMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private boolean isOffline;

  @Before
  public void setUp() {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final OpenShiftClient openShiftClient = mock(OpenShiftClient.class);
      when(mock.createDefaultClient()).thenReturn(openShiftClient);
      when(openShiftClient.isAdaptable(OpenShiftClient.class)).thenReturn(true);
    });
    openshiftUndeployServiceMockedConstruction = mockConstruction(OpenshiftUndeployService.class);
    isOffline = false;
    final OpenShiftExtension extension = new TestOpenShiftExtension() {
      @Override
      public Property<Boolean> getOffline() {
        return new DefaultProperty<>(Boolean.class).value(isOffline);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    when(taskEnvironment.project.getName()).thenReturn("test-project");
  }

  @After
  public void tearDown() {
    openshiftUndeployServiceMockedConstruction.close();
    clusterAccessMockedConstruction.close();
  }

  @Test
  public void runTask_withOffline_shouldThrowException() {
    // Given
    isOffline = true;
    final OpenShiftUndeployTask undeployTask = new OpenShiftUndeployTask(OpenShiftExtension.class);

    // When
    IllegalArgumentException illegalStateException = assertThrows(IllegalArgumentException.class, undeployTask::runTask);

    // Then
    assertThat(illegalStateException)
      .hasMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  public void runTask_withOfflineTrue_shouldUndeployResources() throws IOException {
    // Given
    final OpenShiftUndeployTask undeployTask = new OpenShiftUndeployTask(OpenShiftExtension.class);

    // When
    undeployTask.runTask();

    // Then
    assertThat(openshiftUndeployServiceMockedConstruction.constructed()).hasSize(1);
    verify(openshiftUndeployServiceMockedConstruction.constructed().iterator().next(), times(1))
      .undeploy(
            isNull(),
            eq(taskEnvironment.getRoot().toPath().resolve(Paths.get("src", "main", "jkube"))
                .toFile()),
            eq(ResourceConfig.builder().build()), eq(taskEnvironment.getRoot().toPath()
                .resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "openshift.yml")).toFile()),
            eq(taskEnvironment.getRoot().toPath().resolve(Paths.get("build", "test-project-is.yml")).toFile())
      );
  }
}
