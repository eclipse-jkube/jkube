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
package org.eclipse.jkube.watcher.standard;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.build.service.docker.watch.CopyFilesTask;
import org.eclipse.jkube.kit.build.service.docker.watch.ExecTask;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.watcher.api.WatcherContext;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerImageWatcherTest {
  private WatcherContext watcherContext;
  private WatchService watchService;
  private DockerImageWatcher dockerImageWatcher;

  @BeforeEach
  public void setUp() {
    watcherContext = mock(WatcherContext.class, RETURNS_DEEP_STUBS);
    watchService = mock(WatchService.class);
    DockerServiceHub mockedDockerServiceHub = mock(DockerServiceHub.class,RETURNS_DEEP_STUBS);
    dockerImageWatcher = new DockerImageWatcher(watcherContext);
    when(mockedDockerServiceHub.getWatchService()).thenReturn(watchService);
    when(watcherContext.getJKubeServiceHub().getDockerServiceHub()).thenReturn(mockedDockerServiceHub);
    when(watcherContext.getWatchContext()).thenReturn(new WatchContext());
    when(watcherContext.getBuildContext().getProject()).thenReturn(JavaProject.builder()
        .groupId("org.example")
        .artifactId("test")
        .version("1.0.0-SNAPSHOT").build());
  }

  @Test
  void customizeImageName_whenImageConfigurationProvided_thenModifiesImageNameForWatch() {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("foo/example:latest")
        .build();

    // When
    dockerImageWatcher.customizeImageName(imageConfiguration);

    // Then
    assertThat(imageConfiguration.getName()).startsWith("foo/example:snapshot-");
  }

  @Test
  void watchShouldInitWatchContext() throws IOException {
    // Given
    ArgumentCaptor<WatchContext> watchContextArgumentCaptor = ArgumentCaptor.forClass(WatchContext.class);
    // When
    dockerImageWatcher.watch(null, null, null, null);
    // Then
    verify(watchService).watch(watchContextArgumentCaptor.capture(), any());
    assertThat(watchContextArgumentCaptor.getValue())
        .isNotNull()
        .extracting("imageCustomizer","containerRestarter","containerCommandExecutor","containerCopyTask")
        .doesNotContainNull();
  }

  @Test
  void watchExecuteCommandInPodTask() throws Exception {
    try (MockedConstruction<PodExecutor> podExecutorMockedConstruction = mockConstruction(PodExecutor.class)) {
      // Given
      ArgumentCaptor<WatchContext>watchContextArgumentCaptor=ArgumentCaptor.forClass(WatchContext.class);
      dockerImageWatcher.watch(null,null,null,null);
      verify(watchService).watch(watchContextArgumentCaptor.capture(), any());
      final ExecTask execTask=watchContextArgumentCaptor.getValue().getContainerCommandExecutor();
      //When
      execTask.exec("thecommand");
      // Then
      assertThat(podExecutorMockedConstruction.constructed()).hasSize(1);
      verify(podExecutorMockedConstruction.constructed().get(0)).executeCommandInPod(isNull(),eq("thecommand"));
    }
  }

  @Test
  void watchCopyFileToPod() throws Exception {
    try (MockedConstruction<PodExecutor> podExecutorMockedConstruction = mockConstruction(PodExecutor.class)) {
      // Given
      ArgumentCaptor<WatchContext> watchContextArgumentCaptor = ArgumentCaptor.forClass(WatchContext.class);
      dockerImageWatcher.watch(null,null,null,null);
      verify(watchService).watch(watchContextArgumentCaptor.capture(), any());
      final CopyFilesTask copyFilesTask = watchContextArgumentCaptor.getValue().getContainerCopyTask();
      // When
      copyFilesTask.copy(null);
      // Then
      assertThat(podExecutorMockedConstruction.constructed()).hasSize(1);
      verify(podExecutorMockedConstruction.constructed().get(0)).uploadChangedFilesToPod(isNull(), any());
    }
  }

  @Nested
  @DisplayName("Jetty 12 scan interval injection")
  class JettyScanInterval {

    private Container container;
    private List<HasMetadata> resources;
    private NonDeletingOperation<Deployment> nonDeleting;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUpClient() {
      nonDeleting = mock(NonDeletingOperation.class);
      RollableScalableResource<Deployment> rollable = mock(RollableScalableResource.class);
      when(rollable.unlock()).thenReturn(nonDeleting);
      NonNamespaceOperation<?, ?, ?> nsOp = mock(NonNamespaceOperation.class);
      doReturn(rollable).when(nsOp).resource(any());
      MixedOperation<?, ?, ?> deploymentOp = mock(MixedOperation.class);
      doReturn(nsOp).when(deploymentOp).inNamespace(any());
      AppsAPIGroupDSL appsApi = mock(AppsAPIGroupDSL.class);
      doReturn(deploymentOp).when(appsApi).deployments();
      KubernetesClient kubernetesClient = mock(KubernetesClient.class);
      when(kubernetesClient.apps()).thenReturn(appsApi);
      when(watcherContext.getJKubeServiceHub().getClient()).thenReturn(kubernetesClient);
    }

    private List<HasMetadata> deploymentWithImage(String imageName) {
      return Collections.singletonList(new DeploymentBuilder()
          .withNewMetadata().withName("test-deployment").endMetadata()
          .withNewSpec().withNewTemplate().withNewSpec()
          .addNewContainer().withName("webapp").withImage(imageName).endContainer()
          .endSpec().endTemplate().endSpec()
          .build());
    }

    @Test
    @DisplayName("in copy mode with Jetty 12 image, should inject JAVA_TOOL_OPTIONS")
    void copyModeWithJetty12_shouldInjectScanInterval() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.copy).build());
      resources = deploymentWithImage("quay.io/jkube/jkube-jetty12:0.0.28");
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv())
          .extracting("name", "value")
          .contains(tuple("JAVA_TOOL_OPTIONS", "-Djetty.deploy.scanInterval=1"));
      verify(nonDeleting).update();
    }

    @Test
    @DisplayName("in copy mode with non-Jetty image, should not inject env var")
    void copyModeWithNonJettyImage_shouldNotInjectEnvVar() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.copy).build());
      resources = deploymentWithImage("quay.io/jkube/jkube-tomcat10:0.0.28");
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv()).isNullOrEmpty();
      verify(nonDeleting, never()).update();
    }

    @Test
    @DisplayName("in build mode with Jetty 12 image, should not inject env var")
    void buildModeWithJetty12_shouldNotInjectEnvVar() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.build).build());
      resources = deploymentWithImage("quay.io/jkube/jkube-jetty12:0.0.28");
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv()).isNullOrEmpty();
      verify(nonDeleting, never()).update();
    }

    @Test
    @DisplayName("when user already set scanInterval, should preserve user value")
    void userAlreadySetScanInterval_shouldPreserveUserValue() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.copy).build());
      resources = Collections.singletonList(new DeploymentBuilder()
          .withNewMetadata().withName("test-deployment").endMetadata()
          .withNewSpec().withNewTemplate().withNewSpec()
          .addNewContainer().withName("webapp").withImage("quay.io/jkube/jkube-jetty12:0.0.28")
          .addNewEnv().withName("JAVA_TOOL_OPTIONS").withValue("-Djetty.deploy.scanInterval=5").endEnv()
          .endContainer()
          .endSpec().endTemplate().endSpec()
          .build());
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv())
          .extracting("name", "value")
          .contains(tuple("JAVA_TOOL_OPTIONS", "-Djetty.deploy.scanInterval=5"));
    }

    @Test
    @DisplayName("when JAVA_TOOL_OPTIONS has other flags, should append scanInterval")
    void existingToolOptions_shouldAppendScanInterval() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.copy).build());
      resources = Collections.singletonList(new DeploymentBuilder()
          .withNewMetadata().withName("test-deployment").endMetadata()
          .withNewSpec().withNewTemplate().withNewSpec()
          .addNewContainer().withName("webapp").withImage("quay.io/jkube/jkube-jetty12:0.0.28")
          .addNewEnv().withName("JAVA_TOOL_OPTIONS").withValue("-Xmx512m").endEnv()
          .endContainer()
          .endSpec().endTemplate().endSpec()
          .build());
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv())
          .extracting("name", "value")
          .contains(tuple("JAVA_TOOL_OPTIONS", "-Xmx512m -Djetty.deploy.scanInterval=1"));
    }

    @Test
    @DisplayName("with per-image copy mode and Jetty 12 image, should inject env var")
    void perImageCopyMode_shouldInjectScanInterval() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(new WatchContext());
      ImageConfiguration imageConfig = ImageConfiguration.builder()
          .name("quay.io/jkube/jkube-jetty12:0.0.28")
          .watch(WatchImageConfiguration.builder().mode(WatchMode.copy).build())
          .build();
      resources = deploymentWithImage("quay.io/jkube/jkube-jetty12:0.0.28");
      // When
      dockerImageWatcher.watch(Collections.singletonList(imageConfig), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv())
          .extracting("name", "value")
          .contains(tuple("JAVA_TOOL_OPTIONS", "-Djetty.deploy.scanInterval=1"));
    }

    @Test
    @DisplayName("with Jetty 9 image in copy mode, should not inject env var")
    void jetty9Image_shouldNotInjectEnvVar() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.copy).build());
      resources = deploymentWithImage("quay.io/jkube/jkube-jetty9:0.0.28");
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      container = getFirstContainer(resources);
      assertThat(container.getEnv()).isNullOrEmpty();
    }

    @Test
    @DisplayName("with mixed containers, should only inject into Jetty 12 container")
    void mixedContainers_shouldOnlyInjectIntoJetty12() {
      // Given
      when(watcherContext.getWatchContext()).thenReturn(
          WatchContext.builder().watchMode(WatchMode.copy).build());
      resources = Collections.singletonList(new DeploymentBuilder()
          .withNewMetadata().withName("test-deployment").endMetadata()
          .withNewSpec().withNewTemplate().withNewSpec()
          .addNewContainer().withName("webapp").withImage("quay.io/jkube/jkube-jetty12:0.0.28").endContainer()
          .addNewContainer().withName("sidecar").withImage("quay.io/jkube/jkube-tomcat10:0.0.28").endContainer()
          .endSpec().endTemplate().endSpec()
          .build());
      // When
      dockerImageWatcher.watch(Collections.emptyList(), "test-ns", resources, PlatformMode.kubernetes);
      // Then
      List<Container> containers = ((Deployment) resources.get(0))
          .getSpec().getTemplate().getSpec().getContainers();
      assertThat(containers.get(0).getEnv())
          .extracting("name", "value")
          .contains(tuple("JAVA_TOOL_OPTIONS", "-Djetty.deploy.scanInterval=1"));
      assertThat(containers.get(1).getEnv()).isNullOrEmpty();
    }

    private Container getFirstContainer(List<HasMetadata> resources) {
      return ((io.fabric8.kubernetes.api.model.apps.Deployment) resources.get(0))
          .getSpec().getTemplate().getSpec().getContainers().get(0);
    }
  }
}
