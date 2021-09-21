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

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;
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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private MockedConstruction<OpenshiftUndeployService> openshiftUndeployServiceMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private Project project;
  private boolean isOffline;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    final OpenShiftClient openShiftClient = mock(OpenShiftClient.class);
    when(openShiftClient.getMasterUrl()).thenReturn(new URL("http://api.openshiftapps.com:6443"));
    when(openShiftClient.isAdaptable(OpenShiftClient.class)).thenReturn(true);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(mock(Logger.class));
    });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> when(mock.createDefaultClient()).thenReturn(openShiftClient));
    openshiftUndeployServiceMockedConstruction = mockConstruction(OpenshiftUndeployService.class);
    isOffline = false;
    final OpenShiftExtension extension = new TestOpenShiftExtension() {
      @Override
      public Property<Boolean> getOffline() {
        return new DefaultProperty<>(Boolean.class).value(isOffline);
      }
    };
    when(project.getName()).thenReturn("test-project");
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
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
        isNull()
        , eq(temporaryFolder.getRoot().toPath().resolve(Paths.get("src", "main", "jkube")).toFile())
        , eq(ResourceConfig.builder().build())
        , eq(temporaryFolder.getRoot().toPath().resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "openshift.yml")).toFile()),
          eq(temporaryFolder.getRoot().toPath().resolve(Paths.get("build", "test-project-is.yml")).toFile())
      );
  }
}
