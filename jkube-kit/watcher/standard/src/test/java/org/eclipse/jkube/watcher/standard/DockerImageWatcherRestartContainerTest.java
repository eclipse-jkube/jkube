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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.TimeoutImageEditReplacePatchable;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.DeployableScalableResource;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DockerImageWatcherRestartContainerTest {
  private DockerImageWatcher dockerImageWatcher;
  private KubernetesClient mockedKubernetesClient;
  private WatchService.ImageWatcher mockedImageWatcher;

  @BeforeEach
  public void setUp() {
    WatcherContext watcherContext = mock(WatcherContext.class, RETURNS_DEEP_STUBS);
    mockedImageWatcher = mock(WatchService.ImageWatcher.class);
    mockedKubernetesClient = mock(KubernetesClient.class);
    when(watcherContext.getJKubeServiceHub().getClient()).thenReturn(mockedKubernetesClient);
    when(watcherContext.getNamespace()).thenReturn("test-ns");
    dockerImageWatcher = new DockerImageWatcher(watcherContext);
  }

  @Test
  void restartContainer_whenImageConfigurationAndDeploymentProvided_thenShouldDoRollingUpdate() {
    // Given
    Deployment deployment = createNewDeployment();
    RollableScalableResource<Deployment> rollableScalableResource = mockKubernetesClientAppsDeploymentCall();
    TimeoutImageEditReplacePatchable<Deployment> timeoutImageEditReplacePatchable = mock(TimeoutImageEditReplacePatchable.class);
    when(mockedImageWatcher.getImageConfiguration()).thenReturn(createNewImageConfiguration("foo/example-deployment:snapshot-1234"));
    when(rollableScalableResource.replace()).thenReturn(deployment);
    when(rollableScalableResource.rolling()).thenReturn(timeoutImageEditReplacePatchable);
    when(timeoutImageEditReplacePatchable.restart()).thenReturn(deployment);

    // When
    dockerImageWatcher.restartContainer(mockedImageWatcher, Collections.singletonList(deployment));

    // Then
    verify(rollableScalableResource).replace();
    verify(rollableScalableResource).rolling();
    verify(timeoutImageEditReplacePatchable).restart();
    assertPodTemplateSpecContainsImage(deployment.getSpec().getTemplate(), "foo/example-deployment:snapshot-1234");
  }

  @Test
  void restartContainer_whenImageConfigurationAndReplicaSetProvided_thenShouldDoRollingUpdate() {
    // Given
    ReplicaSet replicaSet = createNewReplicaSet();
    RollableScalableResource<ReplicaSet> rollableScalableResource = mockKubernetesClientAppsReplicaSetCall();
    TimeoutImageEditReplacePatchable<ReplicaSet> timeoutImageEditReplacePatchable = mock(TimeoutImageEditReplacePatchable.class);
    when(mockedImageWatcher.getImageConfiguration()).thenReturn(createNewImageConfiguration("foo/example-replicaset:snapshot-1234"));
    when(rollableScalableResource.replace()).thenReturn(replicaSet);
    when(rollableScalableResource.rolling()).thenReturn(timeoutImageEditReplacePatchable);
    when(timeoutImageEditReplacePatchable.restart()).thenReturn(replicaSet);

    // When
    dockerImageWatcher.restartContainer(mockedImageWatcher, Collections.singletonList(replicaSet));

    // Then
    verify(rollableScalableResource).replace();
    verify(rollableScalableResource).rolling();
    verify(timeoutImageEditReplacePatchable).restart();
    assertPodTemplateSpecContainsImage(replicaSet.getSpec().getTemplate(), "foo/example-replicaset:snapshot-1234");
  }

  @Test
  void restartContainer_whenImageConfigurationAndReplicationControllerProvided_thenShouldDoRollingUpdate() {
    // Given
    ReplicationController replicationController = createNewReplicationController();
    RollableScalableResource<ReplicationController> rollableScalableResource = mockKubernetesClientAppsReplicationControllerCall();
    TimeoutImageEditReplacePatchable<ReplicationController> timeoutImageEditReplacePatchable = mock(TimeoutImageEditReplacePatchable.class);
    when(mockedImageWatcher.getImageConfiguration()).thenReturn(createNewImageConfiguration("foo/example-replicationcontroller:snapshot-1234"));
    when(rollableScalableResource.replace()).thenReturn(replicationController);
    when(rollableScalableResource.rolling()).thenReturn(timeoutImageEditReplacePatchable);
    when(timeoutImageEditReplacePatchable.restart()).thenReturn(replicationController);

    // When
    dockerImageWatcher.restartContainer(mockedImageWatcher, Collections.singletonList(replicationController));

    // Then
    verify(rollableScalableResource).replace();
    verify(rollableScalableResource).rolling();
    verify(timeoutImageEditReplacePatchable).restart();
    assertPodTemplateSpecContainsImage(replicationController.getSpec().getTemplate(), "foo/example-replicationcontroller:snapshot-1234");
  }

  @Test
  void restartContainer_whenImageConfigurationAndDeploymentConfigProvided_thenShouldReplaceDeploymentConfig() {
    try (MockedStatic<OpenshiftHelper> openshiftHelperMockedStatic = mockStatic(OpenshiftHelper.class)) {
      // Given
      OpenShiftClient mockedOpenShiftClient = mock(OpenShiftClient.class);
      openshiftHelperMockedStatic.when(() -> OpenshiftHelper.asOpenShiftClient(mockedKubernetesClient))
          .thenReturn(mockedOpenShiftClient);
      DeploymentConfig deploymentConfig = createNewDeploymentConfig();
      DeployableScalableResource<DeploymentConfig> rollableScalableResource = mockOpenShiftClientDeploymentConfigCall(mockedOpenShiftClient);
      when(mockedImageWatcher.getImageConfiguration()).thenReturn(createNewImageConfiguration("foo/example-deploymentconfig:snapshot-1234"));
      when(rollableScalableResource.replace()).thenReturn(deploymentConfig);

      // When
      dockerImageWatcher.restartContainer(mockedImageWatcher, Collections.singletonList(deploymentConfig));

      // Then
      verify(rollableScalableResource).replace();
      assertPodTemplateSpecContainsImage(deploymentConfig.getSpec().getTemplate(), "foo/example-deploymentconfig:snapshot-1234");
    }
  }

  private void assertPodTemplateSpecContainsImage(PodTemplateSpec podTemplateSpec, String expectedImage) {
    assertThat(podTemplateSpec)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImage)
        .isEqualTo(expectedImage);
  }

  private ImageConfiguration createNewImageConfiguration(String imageName) {
    return ImageConfiguration.builder()
        .name(imageName)
        .build();
  }

  private RollableScalableResource<Deployment> mockKubernetesClientAppsDeploymentCall() {
    AppsAPIGroupDSL mockedApps = mock(AppsAPIGroupDSL.class);
    MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOp = mock(MixedOperation.class);
    NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentNonNsOp = mock(NonNamespaceOperation.class);
    RollableScalableResource<Deployment> rollableScalableResource = mock(RollableScalableResource.class);
    when(mockedKubernetesClient.apps()).thenReturn(mockedApps);
    when(mockedApps.deployments()).thenReturn(deploymentOp);
    when(deploymentOp.inNamespace(anyString())).thenReturn(deploymentNonNsOp);
    when(deploymentNonNsOp.resource(any())).thenReturn(rollableScalableResource);
    when(deploymentNonNsOp.withName(anyString())).thenReturn(rollableScalableResource);
    return rollableScalableResource;
  }

  private RollableScalableResource<ReplicaSet> mockKubernetesClientAppsReplicaSetCall() {
    AppsAPIGroupDSL mockedApps = mock(AppsAPIGroupDSL.class);
    MixedOperation<ReplicaSet, ReplicaSetList, RollableScalableResource<ReplicaSet>> replicaSetOp = mock(MixedOperation.class);
    NonNamespaceOperation<ReplicaSet, ReplicaSetList, RollableScalableResource<ReplicaSet>> replicaSetNonNsOp = mock(NonNamespaceOperation.class);
    RollableScalableResource<ReplicaSet> rollableScalableResource = mock(RollableScalableResource.class);
    when(mockedKubernetesClient.apps()).thenReturn(mockedApps);
    when(mockedApps.replicaSets()).thenReturn(replicaSetOp);
    when(replicaSetOp.inNamespace(anyString())).thenReturn(replicaSetNonNsOp);
    when(replicaSetNonNsOp.resource(any())).thenReturn(rollableScalableResource);
    when(replicaSetNonNsOp.withName(anyString())).thenReturn(rollableScalableResource);
    return rollableScalableResource;
  }

  private RollableScalableResource<ReplicationController> mockKubernetesClientAppsReplicationControllerCall() {
    MixedOperation<ReplicationController, ReplicationControllerList, RollableScalableResource<ReplicationController>> replicationControllerOp = mock(MixedOperation.class);
    NonNamespaceOperation<ReplicationController, ReplicationControllerList, RollableScalableResource<ReplicationController>> replicationControllerNonNsOp = mock(NonNamespaceOperation.class);
    RollableScalableResource<ReplicationController> rollableScalableResource = mock(RollableScalableResource.class);
    when(mockedKubernetesClient.replicationControllers()).thenReturn(replicationControllerOp);
    when(replicationControllerOp.inNamespace(anyString())).thenReturn(replicationControllerNonNsOp);
    when(replicationControllerNonNsOp.withName(anyString())).thenReturn(rollableScalableResource);
    when(replicationControllerNonNsOp.resource(any())).thenReturn(rollableScalableResource);
    return rollableScalableResource;
  }

  private DeployableScalableResource<DeploymentConfig> mockOpenShiftClientDeploymentConfigCall(OpenShiftClient mockedOpenShiftClient) {
    MixedOperation<DeploymentConfig, DeploymentConfigList, DeployableScalableResource<DeploymentConfig>> mixedOp = mock(MixedOperation.class);
    NonNamespaceOperation<DeploymentConfig, DeploymentConfigList, DeployableScalableResource<DeploymentConfig>> deploymentConfigNonNsOp = mock(NonNamespaceOperation.class);
    DeployableScalableResource<DeploymentConfig> deployableScalableResource = mock(DeployableScalableResource.class);
    when(mockedOpenShiftClient.deploymentConfigs()).thenReturn(mixedOp);
    when(mixedOp.inNamespace(anyString())).thenReturn(deploymentConfigNonNsOp);
    when(deploymentConfigNonNsOp.resource(any())).thenReturn(deployableScalableResource);
    return deployableScalableResource;
  }

  private Deployment createNewDeployment() {
    return new DeploymentBuilder()
        .withNewMetadata()
        .withName("test-deployment")
        .endMetadata()
        .withNewSpec()
        .withTemplate(createNewPodTemplateSpec("foo/example-deployment:oldTag"))
        .endSpec()
        .build();
  }
  private ReplicaSet createNewReplicaSet() {
    return new ReplicaSetBuilder()
        .withNewMetadata()
        .withName("test-replicaset")
        .endMetadata()
        .withNewSpec()
        .withTemplate(createNewPodTemplateSpec("foo/example-replicaset:oldTag"))
        .endSpec()
        .build();
  }

  private ReplicationController createNewReplicationController() {
    return new ReplicationControllerBuilder()
        .withNewMetadata()
        .withName("test-replicationcontroller")
        .endMetadata()
        .withNewSpec()
        .withTemplate(createNewPodTemplateSpec("foo/example-replicationcontroller:oldTag"))
        .endSpec()
        .build();
  }

  private DeploymentConfig createNewDeploymentConfig() {
    return new DeploymentConfigBuilder()
        .withNewMetadata()
        .withName("test-deploymentconfig")
        .endMetadata()
        .withNewSpec()
        .withTemplate(createNewPodTemplateSpec("foo/example-deploymentconfig:oldTag"))
        .endSpec()
        .build();
  }

  private PodTemplateSpec createNewPodTemplateSpec(String imageName) {
    return new PodTemplateSpecBuilder()
        .withNewSpec()
        .addNewContainer()
        .withImage(imageName)
        .endContainer()
        .endSpec()
        .build();
  }
}
