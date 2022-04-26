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
import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesUndeployTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<KubernetesUndeployService> kubernetesUndeployServiceMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private TestKubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final KubernetesClient kubernetesClient = mock(KubernetesClient.class);
      when(mock.createDefaultClient()).thenReturn(kubernetesClient);
      when(kubernetesClient.getMasterUrl()).thenReturn(new URL("http://kubernetes-cluster"));
    });
    kubernetesUndeployServiceMockedConstruction = mockConstruction(KubernetesUndeployService.class);
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @After
  public void tearDown() {
    kubernetesUndeployServiceMockedConstruction.close();
    clusterAccessMockedConstruction.close();
  }

  @Test
  public void runTask_withOffline_shouldThrowException() {
    // Given
    extension.isOffline = true;
    final KubernetesUndeployTask undeployTask = new KubernetesUndeployTask(KubernetesExtension.class);

    // When
    IllegalArgumentException illegalStateException = assertThrows(IllegalArgumentException.class, undeployTask::runTask);

    // Then
    assertThat(illegalStateException)
        .hasMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  public void runTask_withOfflineTrue_shouldUndeployResources() throws IOException {
    // Given
    final KubernetesUndeployTask undeployTask = new KubernetesUndeployTask(KubernetesExtension.class);

    // When
    undeployTask.runTask();

    // Then
    assertThat(kubernetesUndeployServiceMockedConstruction.constructed()).hasSize(1);
    verify(kubernetesUndeployServiceMockedConstruction.constructed().iterator().next(), times(1))
      .undeploy(
            Collections.singletonList(taskEnvironment.getRoot().toPath().resolve(Paths.get("src", "main", "jkube"))
                .toFile()),
            ResourceConfig.builder().build(), taskEnvironment.getRoot().toPath()
                .resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "kubernetes.yml")).toFile()
      );
  }
}
