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

import io.fabric8.kubernetes.api.model.APIGroupBuilder;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.watcher.api.WatcherManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class OpenShiftWatchTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;
  private TestOpenShiftExtension extension;
  private KubernetesMockServer kubernetesMockServer;
  private OpenShiftClient openShiftClient;

  @BeforeEach
  void setUp() {
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    extension = new TestOpenShiftExtension();
    extension.access = ClusterConfiguration.from(openShiftClient.getConfiguration()).build();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    kubernetesMockServer.expect().get().withPath("/apis")
      .andReturn(HTTP_OK, new APIGroupListBuilder()
        .addToGroups(new APIGroupBuilder().withName("test.openshift.io").build())
        .build())
      .always();
    kubernetesHelperMockedStatic.when(KubernetesHelper::getDefaultNamespace).thenReturn(null);
    extension.isFailOnNoKubernetesJson = false;
  }

  @AfterEach
  void tearDown() {
    watcherManagerMockedStatic.close();
    kubernetesHelperMockedStatic.close();
  }

  @Test
  void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final OpenShiftWatchTask watchTask = new OpenShiftWatchTask(OpenShiftExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(watchTask::runTask)
        .withMessageContaining("An error has occurred while while trying to watch the resources");
  }

  @Test
  void runTask_withManifest_shouldWatchEntities() throws Exception {
    // Given
    taskEnvironment.withOpenShiftManifest();
    final OpenShiftWatchTask watchTask = new OpenShiftWatchTask(OpenShiftExtension.class);
    // When
    watchTask.runTask();
    // Then
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq(openShiftClient.getNamespace()), any(), any()), times(1));
  }
}
