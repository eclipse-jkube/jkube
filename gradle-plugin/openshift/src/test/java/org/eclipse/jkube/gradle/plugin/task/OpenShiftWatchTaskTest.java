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
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.watcher.api.WatcherManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class OpenShiftWatchTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;
  private TestOpenShiftExtension extension;

  @BeforeEach
  void setUp() {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final OpenShiftClient openShiftClient = mock(OpenShiftClient.class);
      when(openShiftClient.getMasterUrl()).thenReturn(new URL("http://openshiftapps.com:6443"));
      when(openShiftClient.adapt(OpenShiftClient.class)).thenReturn(openShiftClient);
      when(mock.createDefaultClient()).thenReturn(openShiftClient);
    });
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    extension = new TestOpenShiftExtension();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    kubernetesHelperMockedStatic.when(KubernetesHelper::getDefaultNamespace).thenReturn(null);
    extension.isFailOnNoKubernetesJson = false;
  }

  @AfterEach
  void tearDown() {
    watcherManagerMockedStatic.close();
    clusterAccessMockedConstruction.close();
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
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), isNull(), any(), any()), times(1));
  }
}
