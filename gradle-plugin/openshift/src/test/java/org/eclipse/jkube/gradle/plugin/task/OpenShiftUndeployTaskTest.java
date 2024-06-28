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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import io.fabric8.kubernetes.api.model.APIGroupBuilder;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;

import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class OpenShiftUndeployTaskTest {

  @RegisterExtension
  public final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<OpenshiftUndeployService> openshiftUndeployServiceMockedConstruction;
  private TestOpenShiftExtension extension;
  private KubernetesMockServer kubernetesMockServer;
  private OpenShiftClient openShiftClient;

  @BeforeEach
  void setUp() {
    openshiftUndeployServiceMockedConstruction = mockConstruction(OpenshiftUndeployService.class);
    extension = new TestOpenShiftExtension();
    extension.access = ClusterConfiguration.from(openShiftClient.getConfiguration()).build();
    kubernetesMockServer.expect().get().withPath("/apis")
      .andReturn(HTTP_OK, new APIGroupListBuilder()
        .addToGroups(new APIGroupBuilder().withName("test.openshift.io").build())
        .build())
      .always();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    when(taskEnvironment.project.getName()).thenReturn("test-project");
  }

  @AfterEach
  void tearDown() {
    openshiftUndeployServiceMockedConstruction.close();
  }

  @Test
  void runTask_withOffline_shouldThrowException() {
    // Given
    extension.isOffline = true;
    final OpenShiftUndeployTask undeployTask = new OpenShiftUndeployTask(OpenShiftExtension.class);

    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(undeployTask::runTask)
        .withMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  void runTask_withOfflineTrue_shouldUndeployResources() throws IOException {
    // Given
    final OpenShiftUndeployTask undeployTask = new OpenShiftUndeployTask(OpenShiftExtension.class);

    // When
    undeployTask.runTask();

    // Then
    assertThat(openshiftUndeployServiceMockedConstruction.constructed()).hasSize(1);
    verify(openshiftUndeployServiceMockedConstruction.constructed().iterator().next(), times(1))
      .undeploy(
            Collections.singletonList(taskEnvironment.getRoot().toPath().resolve(Paths.get("src", "main", "jkube"))
                .toFile()),
            ResourceConfig.builder().build(), taskEnvironment.getRoot().toPath()
                .resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "openshift.yml")).toFile(),
            taskEnvironment.getRoot().toPath().resolve(Paths.get("build", "test-project-is.yml")).toFile()
      );
  }

  @Test
  void runTask_withSkipUndeploy_shouldDoNothing() {
    // Given
    extension = new TestOpenShiftExtension() {
      @Override
      public Property<Boolean> getSkipUndeploy() {
        return super.getSkipUndeploy().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    final OpenShiftUndeployTask openShiftUndeployTask = new OpenShiftUndeployTask(OpenShiftExtension.class);

    // When
    openShiftUndeployTask.runTask();

    // Then
    assertThat(openshiftUndeployServiceMockedConstruction.constructed()).isEmpty();
  }
}
