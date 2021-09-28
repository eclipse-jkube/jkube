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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.ApplyService;

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jgit.util.FileUtils;
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

public class OpenShiftApplyTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedConstruction<ApplyService> applyServiceMockedConstruction;
  private boolean isOffline;
  private boolean isFailOnUnknownKubernetesJson;

  @Before
  public void setUp() {
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final OpenShiftClient openShiftClient = mock(OpenShiftClient.class);
      when(openShiftClient.getMasterUrl()).thenReturn(new URL("http://openshiftapps-com-cluster:6443"));
      when(openShiftClient.isAdaptable(OpenShiftClient.class)).thenReturn(true);
      when(mock.createDefaultClient()).thenReturn(openShiftClient);
    });
    applyServiceMockedConstruction = mockConstruction(ApplyService.class);
    isOffline = false;
    isFailOnUnknownKubernetesJson = false;
    final OpenShiftExtension extension = new TestOpenShiftExtension() {
      @Override
      public Property<Boolean> getOffline() {
        return new DefaultProperty<>(Boolean.class).value(isOffline);
      }

      @Override
      public Property<Boolean> getFailOnNoKubernetesJson() {
        return new DefaultProperty<>(Boolean.class).value(isFailOnUnknownKubernetesJson);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
  }

  @After
  public void tearDown() {
    applyServiceMockedConstruction.close();
    clusterAccessMockedConstruction.close();
  }

  @Test
  public void runTask_withOffline_shouldThrowException() {
    // Given
    isOffline = true;
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);

    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, ocApplyTask::runTask);

    // Then
    assertThat(result)
      .hasMessage("Connection to Cluster required. Please check if offline mode is set to false");
  }

  @Test
  public void runTask_withNoManifest_shouldThrowException() {
    // Given
    isFailOnUnknownKubernetesJson = true;
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, ocApplyTask::runTask);
    // Then
    assertThat(result)
      .hasMessageMatching("No such generated manifest file: .+openshift\\.yml");
  }

  @Test
  public void configureApplyService_withManifest_shouldSetDefaults() throws Exception {
    // Given
    withOpenShiftManifest();
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
  public void runTask_withManifest_shouldApplyEntities() throws Exception {
    // Given
    withOpenShiftManifest();
    final OpenShiftApplyTask ocApplyTask = new OpenShiftApplyTask(OpenShiftExtension.class);
    // When
    ocApplyTask.runTask();
    // Then
    assertThat(applyServiceMockedConstruction.constructed()).hasSize(1);
    verify(applyServiceMockedConstruction.constructed().iterator().next(), times(1))
      .applyEntities(any(), eq(Collections.emptyList()), any(), eq(5L));
  }

  private void withOpenShiftManifest() throws IOException {
    final File manifestsDir = taskEnvironment.newFolder("build", "classes", "java", "main", "META-INF", "jkube");
    FileUtils.touch(new File(manifestsDir, "openshift.yml").toPath());
  }
}
