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
import java.util.stream.Stream;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.ApplyService;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jgit.util.FileUtils;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesApplyTaskTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedConstruction<ApplyService> applyServiceMockedConstruction;
  private Project project;
  private boolean isOffline;
  private boolean isFailOnUnknownKubernetesJson;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(mock(Logger.class));
    });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final KubernetesClient kubernetesClient = mock(KubernetesClient.class);
      when(kubernetesClient.getMasterUrl()).thenReturn(new URL("http://kubernetes-cluster"));
      when(mock.createDefaultClient()).thenReturn(kubernetesClient);
    });
    applyServiceMockedConstruction = mockConstruction(ApplyService.class);
    isOffline = false;
    isFailOnUnknownKubernetesJson = false;
    final KubernetesExtension extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getOffline() {
        return new DefaultProperty<>(Boolean.class).value(isOffline);
      }

      @Override
      public Property<Boolean> getFailOnNoKubernetesJson() {
        return new DefaultProperty<>(Boolean.class).value(isFailOnUnknownKubernetesJson);
      }
    };
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
  }

  @After
  public void tearDown() {
    applyServiceMockedConstruction.close();
    clusterAccessMockedConstruction.close();
    defaultTaskMockedConstruction.close();
  }

  @Test
  public void runTask_withOffline_shouldThrowException() {
    // Given
    isOffline = true;
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
    isFailOnUnknownKubernetesJson = true;
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
    withKubernetesManifest();
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
    // When
    applyTask.runTask();
    // Then
    final ApplyService as = applyServiceMockedConstruction.constructed().iterator().next();
    verify(as, times(1)).setAllowCreate(true);
    verify(as, times(1)).setServicesOnlyMode(false);
    verify(as, times(1)).setIgnoreServiceMode(false);
    verify(as, times(1)).setLogJsonDir(any());
    verify(as, times(1)).setBasedir(temporaryFolder.getRoot());
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
    withKubernetesManifest();
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
    // When
    applyTask.runTask();
    // Then
    assertThat(applyServiceMockedConstruction.constructed()).hasSize(1);
    verify(applyServiceMockedConstruction.constructed().iterator().next(), times(1))
        .applyEntities(any(), eq(Collections.emptyList()), any(), eq(5L));
  }

  private void withKubernetesManifest() throws IOException {
    final File manifestsDir = temporaryFolder.newFolder("build", "classes", "java", "main", "META-INF", "jkube");
    FileUtils.touch(new File(manifestsDir, "kubernetes.yml").toPath());
  }
}
