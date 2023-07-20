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

import java.net.URL;
import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.ApplyService;

import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftApplyTaskTest {

  @RegisterExtension
  public final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedConstruction<ApplyService> applyServiceMockedConstruction;
  private TestOpenShiftExtension extension;

  @BeforeEach
  void setUp() {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final OpenShiftClient openShiftClient = mock(OpenShiftClient.class);
      when(mock.createDefaultClient()).thenReturn(openShiftClient);
      when(openShiftClient.getMasterUrl()).thenReturn(new URL("http://openshiftapps-com-cluster:6443"));
      when(openShiftClient.hasApiGroup("openshift.io", false)).thenReturn(true);
    });
    applyServiceMockedConstruction = mockConstruction(ApplyService.class);
    extension = new TestOpenShiftExtension();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    extension.isFailOnNoKubernetesJson = false;
  }

  @AfterEach
  void tearDown() {
    applyServiceMockedConstruction.close();
    clusterAccessMockedConstruction.close();
  }

  @Test
  void runTask_withOffline_shouldThrowException() {
    // Given
    extension.isOffline = true;
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);

    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(ocApplyTask::runTask)
        .withMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(ocApplyTask::runTask)
        .withMessageMatching("No such generated manifest file: .+openshift\\.yml");
  }

  @Test
  void configureApplyService_withManifest_shouldSetDefaults() throws Exception {
    // Given
    taskEnvironment.withOpenShiftManifest();
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);
    // When
    ocApplyTask.runTask();
    // Then
    final ApplyService as = applyServiceMockedConstruction.constructed().iterator().next();
    verify(as, times(1)).setAllowCreate(true);
    verify(as, times(1)).setServicesOnlyMode(false);
    verify(as, times(1)).setIgnoreServiceMode(false);
    verify(as, times(1)).setLogJsonDir(any());
    verify(as, times(1)).setBasedir(taskEnvironment.getRoot());
    verify(as, times(1)).setSupportOAuthClients(true);
    verify(as, times(1)).setIgnoreRunningOAuthClients(true);
    verify(as, times(1)).setProcessTemplatesLocally(false);
    verify(as, times(1)).setDeletePodsOnReplicationControllerUpdate(true);
    verify(as, times(1)).setRollingUpgrade(false);
    verify(as, times(1)).setRollingUpgradePreserveScale(false);
    verify(as, times(1)).setRecreateMode(false);
    verify(as, times(1)).setNamespace(null);
    verify(as, times(1)).setFallbackNamespace(null);
  }

  @Test
  void runTask_withManifest_shouldApplyEntities() throws Exception {
    // Given
    taskEnvironment.withOpenShiftManifest();
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);
    // When
    ocApplyTask.runTask();
    // Then
    assertThat(applyServiceMockedConstruction.constructed()).hasSize(1);
    verify(applyServiceMockedConstruction.constructed().iterator().next(), times(1))
      .applyEntities(any(), eq(Collections.emptyList()));
  }
}
