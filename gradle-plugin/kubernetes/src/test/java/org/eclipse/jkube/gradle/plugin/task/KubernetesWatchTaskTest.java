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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.watcher.api.WatcherManager;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class KubernetesWatchTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<DockerAccessFactory> dockerAccessFactoryMockedConstruction;
  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;
  private TestKubernetesExtension extension;
  private KubernetesClient kubernetesClient;

  @BeforeEach
  void setUp() {
    // Mock required for environments with no DOCKER available (don't remove)
    dockerAccessFactoryMockedConstruction = mockConstruction(DockerAccessFactory.class,
        (mock, ctx) -> when(mock.createDockerAccess(any())).thenReturn(mock(DockerAccess.class)));
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class, (mock, ctx) -> when(mock.isApplicable()).thenReturn(true));
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    extension = new TestKubernetesExtension();
    extension.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    kubernetesHelperMockedStatic.when(KubernetesHelper::getDefaultNamespace).thenReturn(null);
    extension.isFailOnNoKubernetesJson = false;
  }

  @AfterEach
  void tearDown() {
    watcherManagerMockedStatic.close();
    kubernetesHelperMockedStatic.close();
    dockerBuildServiceMockedConstruction.close();
    dockerAccessFactoryMockedConstruction.close();
  }

  @Test
  void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesWatchTask watchTask = new KubernetesWatchTask(KubernetesExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(watchTask::runTask)
        .withMessageContaining("An error has occurred while while trying to watch the resources");
    assertThat(watchTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(DockerBuildService.class);
  }

  @Test
  void runTask_withManifest_shouldWatchEntities() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final KubernetesWatchTask watchTask = new KubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    AssertionsForClassTypes.assertThat(watchTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(DockerBuildService.class);
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq(kubernetesClient.getNamespace()), any(), any()), times(1));
  }

  @Test
  void runTask_withSkip_shouldDoNothing() {
    // Given
    extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getSkip() {
        return super.getSkip().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    final KubernetesWatchTask kubernetesWatchTask = new KubernetesWatchTask(KubernetesExtension.class);
    when(kubernetesWatchTask.getName()).thenReturn("k8sWatch");

    // When
    kubernetesWatchTask.runTask();

    // Then - verify no build service was constructed and init() was not called
    assertThat(dockerBuildServiceMockedConstruction.constructed()).isEmpty();
    assertThat(kubernetesWatchTask.jKubeServiceHub).isNull();
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("k8s: `k8sWatch` task is skipped."));
  }

  @Test
  @DisplayName("generatorContextBuilder should have WATCH generator mode")
  void generatorContextBuilder_shouldHaveWatchGeneratorMode() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("generatorMode", GeneratorMode.WATCH);
  }

  // Documents the contract only: prePackagePhase is a primitive boolean whose builder default
  // is already false, so this assertion cannot fail if .prePackagePhase(false) is dropped.
  @Test
  @DisplayName("generatorContextBuilder should have prePackagePhase false")
  void generatorContextBuilder_shouldHavePrePackagePhaseFalse() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("prePackagePhase", false);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate default watch mode")
  void generatorContextBuilder_shouldPropagateDefaultWatchMode() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("watchMode", WatchMode.both);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate configured watch mode")
  void generatorContextBuilder_shouldPropagateConfiguredWatchMode() throws Exception {
    // Given
    extension.watchMode = WatchMode.copy;
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("watchMode", WatchMode.copy);
  }

  @Test
  @DisplayName("generatorContextBuilder should have Kubernetes runtime mode")
  void generatorContextBuilder_shouldHaveKubernetesRuntimeMode() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("runtimeMode", RuntimeMode.KUBERNETES);
  }

  @Test
  @DisplayName("generatorContextBuilder should have docker build strategy")
  void generatorContextBuilder_shouldHaveDockerBuildStrategy() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("strategy", JKubeBuildStrategy.docker);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate project")
  void generatorContextBuilder_shouldPropagateProject() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .extracting(GeneratorContext::getProject)
        .isSameAs(extension.javaProject);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate build timestamp")
  void generatorContextBuilder_shouldPropagateBuildTimestamp() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .extracting(GeneratorContext::getBuildTimestamp)
        .isNotNull();
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate useProjectClasspath")
  void generatorContextBuilder_shouldPropagateUseProjectClasspath() throws Exception {
    // Given
    extension.isUseProjectClassPath = true;
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("useProjectClasspath", true);
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate source directory")
  void generatorContextBuilder_shouldPropagateSourceDirectory() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("sourceDirectory", "src/main/docker");
  }

  @Test
  @DisplayName("generatorContextBuilder should propagate configured filter")
  void generatorContextBuilder_shouldPropagateFilter() throws Exception {
    // Given
    extension.filter = "my-image";
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .hasFieldOrPropertyWithValue("filter", "my-image");
  }

  @Test
  @DisplayName("generatorContextBuilder should have null filter when not configured")
  void generatorContextBuilder_shouldHaveNullFilterWhenNotConfigured() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final TestKubernetesWatchTask watchTask = new TestKubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    assertThat(watchTask.capturedGeneratorContext)
        .isNotNull()
        .extracting(GeneratorContext::getFilter)
        .isNull();
  }

  private static class TestKubernetesWatchTask extends KubernetesWatchTask {

    GeneratorContext capturedGeneratorContext;

    TestKubernetesWatchTask(Class<? extends KubernetesExtension> extensionClass) {
      super(extensionClass);
    }

    @Override
    protected GeneratorContext.GeneratorContextBuilder initGeneratorContextBuilder() {
      GeneratorContext.GeneratorContextBuilder builder = super.initGeneratorContextBuilder();
      capturedGeneratorContext = builder.build();
      return builder;
    }
  }
}
