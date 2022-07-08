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
package org.eclipse.jkube.kit.config.service.openshift;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigListBuilder;
import io.fabric8.openshift.api.model.BuildListBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParamsBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.TagReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static io.fabric8.kubernetes.api.model.DeletionPropagation.BACKGROUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused"})
class OpenshiftUndeployServiceTest {

  @TempDir
  File temporaryFolder;
  @Mock
  private KitLogger logger;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private JKubeServiceHub jKubeServiceHub;

  private OpenShiftClient openShiftClient;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;
  private OpenshiftUndeployService openshiftUndeployService;

  @BeforeEach
  void setUp() {
    openShiftClient = mock(OpenShiftClient.class,RETURNS_DEEP_STUBS);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    when(jKubeServiceHub.getClient().adapt(OpenShiftClient.class)).thenReturn(openShiftClient);
    final JKubeServiceHub jKubeServiceHub = mock(JKubeServiceHub.class);
    openShiftClient = mock(OpenShiftClient.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getClient()).thenReturn(openShiftClient);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    openshiftUndeployService = new OpenshiftUndeployService(jKubeServiceHub, new KitLogger.SilentLogger());
  }

  @AfterEach
  void tearDown() {
    openshiftUndeployService = null;
    kubernetesHelperMockedStatic.close();
  }

  private void withLoadedEntities(HasMetadata... entities) {
    kubernetesHelperMockedStatic.when(() -> KubernetesHelper.loadResources(any())).thenReturn(Arrays.asList(entities));
  }

  private void with(Build build, BuildConfig buildConfig) {
    when(openShiftClient.builds().inNamespace(any()))
        .thenReturn(mock(MixedOperation.class, RETURNS_DEEP_STUBS));
    when(openShiftClient.builds().inNamespace(null).list())
        .thenReturn(new BuildListBuilder().withItems(build).build());
    when(openShiftClient.buildConfigs().inNamespace(any()))
        .thenReturn(mock(MixedOperation.class, RETURNS_DEEP_STUBS));
    when(openShiftClient.buildConfigs().inNamespace(null).list())
        .thenReturn(new BuildConfigListBuilder().withItems(buildConfig).build());
  }

  private void assertDeleted(HasMetadata entity) {
    verify(openShiftClient.resource(entity).inNamespace(null).withPropagationPolicy(BACKGROUND),
        times(1)).delete();
  }

  private void assertDeleteCount(int totalDeletions) {
    kubernetesHelperMockedStatic.verify(()-> KubernetesHelper.getKind(any()), times(totalDeletions));
  }

  @Test
  void deleteWithKubernetesClient_shouldOnlyDeleteProvidedEntity() throws Exception {
    // Given
    final Pod entity = new Pod();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null, ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  void deleteWithOpenShiftClientAndNoImageStream_shouldOnlyDeleteProvidedEntity() throws Exception {
    // Given
    final Pod entity = new Pod();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  void deleteWithOpenShiftClientAndImageStream_shouldDeleteProvidedImageStream() throws Exception {
    // Given
    final ImageStream entity = new ImageStreamBuilder()
        .withNewMetadata().withName("image").endMetadata()
        .withNewSpec().withTags().endSpec()
        .build();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  void deleteWithOpenShiftClientAndImageStream_shouldDeleteProvidedImageStreamAndRelatedBuildEntities()
      throws Exception {

    // Given
    final Build build = new BuildBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    with(build, buildConfig);
    final ImageStream entity = new ImageStreamBuilder()
        .withNewMetadata().withName("image").endMetadata()
        .withNewSpec().withTags(new TagReferenceBuilder().withName("latest").build()).endSpec()
        .build();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(3);
    assertDeleted(entity);
    assertDeleted(build);
    assertDeleted(buildConfig);
  }


  @Test
  void deleteWithOpenShiftClientAndDeploymentConfig_shouldDeleteProvidedDeploymentConfigAndRelatedBuildEntities()
      throws Exception {

    // Given
    final Build build = new BuildBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    with(build, buildConfig);
    final DeploymentConfig entity = new DeploymentConfigBuilder()
        .withNewMetadata().withName("image").endMetadata()
        .withNewSpec().withTriggers(new DeploymentTriggerPolicyBuilder()
            .withType("ImageChange")
            .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
                .withFrom(new ObjectReferenceBuilder()
                    .withName("image:latest")
                    .withKind("ImageStreamTag")
                    .build())
                .build())
            .build()
          )
        .endSpec().build();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(3);
    assertDeleted(entity);
    assertDeleted(build);
    assertDeleted(buildConfig);
  }

  @Test
  void deleteWithOpenShiftClientAndDeploymentConfigNoMatchingLabel_shouldDeleteProvidedDeploymentConfigOnly()
      throws Exception {

    // Given
    final Build build = new BuildBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    with(build, buildConfig);
    final DeploymentConfig entity = new DeploymentConfigBuilder()
        .withNewMetadata().withName("image").withLabels(
            Collections.singletonMap("provider", "different")
        ).endMetadata()
        .withNewSpec().withTriggers(new DeploymentTriggerPolicyBuilder()
            .withType("ImageChange")
            .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
                .withFrom(new ObjectReferenceBuilder()
                    .withName("image:latest")
                    .withKind("ImageStreamTag")
                    .build())
                .build())
            .build()
        )
        .endSpec().build();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  void deleteWithOpenShiftClientAndDeploymentConfig_shouldDeleteProvidedDeploymentConfigAndRelatedMatchingBuildEntities()
      throws Exception {

    // Given
    final Build build = new BuildBuilder()
        .withNewMetadata().withLabels(Collections.singletonMap("provider", "jkube")).endMetadata()
        .withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder().withNewSpec().withNewOutput().withNewTo().withName("image:latest")
        .endTo().endOutput().endSpec().build();
    with(build, buildConfig);
    final DeploymentConfig entity = new DeploymentConfigBuilder()
        .withNewMetadata().withName("image").withLabels(
            Collections.singletonMap("provider", "jkube")
        ).endMetadata()
        .withNewSpec().withTriggers(new DeploymentTriggerPolicyBuilder()
            .withType("ImageChange")
            .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
                .withFrom(new ObjectReferenceBuilder()
                    .withName("image:latest")
                    .withKind("ImageStreamTag")
                    .build())
                .build())
            .build()
        )
        .endSpec().build();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), File.createTempFile("junit", "ext", temporaryFolder));
    // Then
    assertDeleteCount(2);
    assertDeleted(entity);
    assertDeleted(build);
  }
}