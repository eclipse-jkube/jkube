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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.ApplyService;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesApplyTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedConstruction<ApplyService> applyServiceMockedConstruction;
  private TestKubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final KubernetesClient kubernetesClient = mock(KubernetesClient.class);
      when(kubernetesClient.getMasterUrl()).thenReturn(new URL("http://kubernetes-cluster"));
      when(mock.createDefaultClient()).thenReturn(kubernetesClient);
    });
    applyServiceMockedConstruction = mockConstruction(ApplyService.class);
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    extension.isFailOnNoKubernetesJson = false;
  }

  @After
  public void tearDown() {
    applyServiceMockedConstruction.close();
    clusterAccessMockedConstruction.close();
  }

  @Test
  public void runTask_withOffline_shouldThrowException() {
    // Given
    extension.isOffline = true;
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);

    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, applyTask::runTask);

    // Then
    assertThat(result)
        .hasMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  public void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, applyTask::runTask);
    // Then
    assertThat(result)
        .hasMessageMatching("No such generated manifest file: .+kubernetes\\.yml");
  }

  @Test
  public void configureApplyService_withManifest_shouldSetDefaults() throws Exception {
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

  @Test
  public void runTask_withManifest_shouldApplyEntities() throws Exception {
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

  @Test
  public void runTask_withSkipApply_shouldDoNothing() {
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
