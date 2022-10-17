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
import java.util.Collections;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.ApplyService;

import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesApplyTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() throws IOException {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      // OpenShiftClient instance needed due to OpenShift checks performed in KubernetesApply
      final OpenShiftClient kubernetesClient = mock(OpenShiftClient.class);
      when(kubernetesClient.getMasterUrl()).thenReturn(new URL("http://kubernetes-cluster"));
      when(kubernetesClient.adapt(OpenShiftClient.class)).thenReturn(kubernetesClient);
      when(mock.createDefaultClient()).thenReturn(kubernetesClient);
    });
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    extension.isFailOnNoKubernetesJson = false;
  }

  @AfterEach
  void tearDown() {
    clusterAccessMockedConstruction.close();
  }

  @Test
  void runTask_withOffline_shouldThrowException() {
    // Given
    extension.isOffline = true;
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);

    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(applyTask::runTask)
        .withMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(applyTask::runTask)
        .withMessageMatching("No such generated manifest file: .+kubernetes\\.yml");
  }

  @Test
  void configureApplyService_withManifest_shouldSetDefaults() throws Exception {
    try (MockedConstruction<ApplyService> applyServiceMockedConstruction = mockConstruction(ApplyService.class)) {
      // Given
      taskEnvironment.withKubernetesManifest();
      final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
      // When
      applyTask.runTask();
      // Then
      final ApplyService as = applyServiceMockedConstruction.constructed().iterator().next();
      verify(as, times(1)).setAllowCreate(true);
      verify(as, times(1)).setServicesOnlyMode(false);
      verify(as, times(1)).setIgnoreServiceMode(false);
      verify(as, times(1)).setLogJsonDir(any());
      verify(as, times(1)).setBasedir(taskEnvironment.getRoot());
      verify(as, times(1)).setSupportOAuthClients(false);
      verify(as, times(1)).setIgnoreRunningOAuthClients(true);
      verify(as, times(1)).setProcessTemplatesLocally(true);
      verify(as, times(1)).setDeletePodsOnReplicationControllerUpdate(true);
      verify(as, times(1)).setRollingUpgrade(false);
      verify(as, times(1)).setRollingUpgradePreserveScale(false);
      verify(as, times(1)).setRecreateMode(false);
      verify(as, times(1)).setNamespace(null);
      verify(as, times(1)).setFallbackNamespace(null);
    }
  }

  @Test
  void configureApplySerVvice_withManifest_shouldSetDefaults() throws Exception {
    try (MockedConstruction<ApplyService> applyServiceMockedConstruction = mockConstruction(ApplyService.class)) {
      // Given
      taskEnvironment.withKubernetesManifest();
      final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
      // When
      applyTask.runTask();
      // Then
      final ApplyService as = applyServiceMockedConstruction.constructed().iterator().next();
      verify(as, times(1)).setAllowCreate(true);
      verify(as, times(1)).setServicesOnlyMode(false);
      verify(as, times(1)).setIgnoreServiceMode(false);
      verify(as, times(1)).setLogJsonDir(any());
      verify(as, times(1)).setBasedir(taskEnvironment.getRoot());
      verify(as, times(1)).setSupportOAuthClients(false);
      verify(as, times(1)).setIgnoreRunningOAuthClients(true);
      verify(as, times(1)).setProcessTemplatesLocally(true);
      verify(as, times(1)).setDeletePodsOnReplicationControllerUpdate(true);
      verify(as, times(1)).setRollingUpgrade(false);
      verify(as, times(1)).setRollingUpgradePreserveScale(false);
      verify(as, times(1)).setRecreateMode(false);
      verify(as, times(1)).setNamespace(null);
      verify(as, times(1)).setFallbackNamespace(null);
    }
  }

  @Test
  void runTask_withManifest_shouldApplyEntities() throws Exception {
    try (MockedConstruction<ApplyService> applyServiceMockedConstruction = mockConstruction(ApplyService.class)) {
      // Given
      taskEnvironment.withKubernetesManifest();
      final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
      // When
      applyTask.runTask();
      // Then
      assertThat(applyServiceMockedConstruction.constructed()).hasSize(1);
      verify(applyServiceMockedConstruction.constructed().iterator().next(), times(1))
          .applyEntities(any(), eq(Collections.emptyList()), any(), eq(5L));
    }
  }

  @Test
  void runTask_withSkipApply_shouldDoNothing() {
    try (MockedConstruction<ApplyService> applyServiceMockedConstruction = mockConstruction(ApplyService.class)) {
      // Given
      extension = new TestKubernetesExtension() {
        @Override
        public Property<Boolean> getSkipApply() {
          return new DefaultProperty<>(Boolean.class).value(true);
        }
      };
      when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
      final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);

      // When
      applyTask.runTask();

      // Then
      assertThat(applyServiceMockedConstruction.constructed()).isEmpty();
    }
  }

  @Test
  void runTask_whenApplyServiceFails_thenExceptionThrown() {
    try (MockedConstruction<ApplyService> applyServiceMockedConstruction = mockConstruction(ApplyService.class, (mock, ctx) -> {
      doThrow(new KubernetesClientException("failure")).when(mock).applyEntities(anyString(), any(), any(), anyLong());
    })) {
      // Given
      final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);

      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(applyTask::runTask)
          .withMessage("failure");
      assertThat(applyServiceMockedConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void runTask_whenLoadingManifestFails_thenExceptionThrown() {
    try (MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class)) {
      // Given
      kubernetesHelperMockedStatic.when(() -> KubernetesHelper.loadResources(any())).thenThrow(new IOException("failure"));
      final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);

      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(applyTask::runTask)
          .withMessageContaining("failure");
    }
  }
}
