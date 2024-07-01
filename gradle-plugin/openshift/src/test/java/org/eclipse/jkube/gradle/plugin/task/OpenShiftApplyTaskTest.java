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

import java.util.Collections;

import io.fabric8.kubernetes.api.model.APIGroupBuilder;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.service.ApplyService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class OpenShiftApplyTaskTest {

  @RegisterExtension
  public final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<ApplyService> applyServiceMockedConstruction;
  private TestOpenShiftExtension extension;
  private KubernetesMockServer kubernetesMockServer;
  private OpenShiftClient openShiftClient;

  @BeforeEach
  void setUp() {
    applyServiceMockedConstruction = mockConstruction(ApplyService.class);
    extension = new TestOpenShiftExtension();
    extension.access = ClusterConfiguration.from(openShiftClient.getConfiguration()).build();
    extension.isFailOnNoKubernetesJson = false;
    kubernetesMockServer.expect().get().withPath("/apis")
      .andReturn(HTTP_OK, new APIGroupListBuilder()
        .addToGroups(new APIGroupBuilder().withName("test.openshift.io").build())
        .build())
      .always();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    applyServiceMockedConstruction.close();
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
    verify(as, times(1)).setFallbackNamespace(openShiftClient.getNamespace());
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
