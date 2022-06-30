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

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
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
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"AccessStaticViaInstance", "unused"})
class OpenshiftUndeployServiceTest {

  @TempDir
  File temporaryFolder;
  @Mock
  private KitLogger logger;
  @Mock
  private JKubeServiceHub jKubeServiceHub;
  @Mock
  private KubernetesClient kubernetesClient;
  @Mock
  private OpenShiftClient openShiftClient;
  @Mock
  private KubernetesHelper kubernetesHelper;
  private OpenshiftUndeployService openshiftUndeployService;

  @BeforeEach
  void setUp() {
    openshiftUndeployService = new OpenshiftUndeployService(jKubeServiceHub, logger);
    when(jKubeServiceHub.getClient().adapt(OpenShiftClient.class)).thenReturn(openShiftClient);
  }

  @AfterEach
  void tearDown() {
    openshiftUndeployService = null;
  }

  private void withLoadedEntities(HasMetadata... entities) throws Exception {
    when(kubernetesHelper.loadResources((File)any())).thenReturn(Arrays.asList(entities));
  }

  private void with(Build build, BuildConfig buildConfig) {
    when(openShiftClient.builds().inNamespace(null).list()).thenReturn(new BuildListBuilder().withItems(build).build());
    when(openShiftClient.buildConfigs().inNamespace(null).list()).thenReturn(new BuildConfigListBuilder().withItems(buildConfig).build());
  }

  private void assertDeleted(HasMetadata entity) {
    verify(jKubeServiceHub,times(1)).getClient().resource(entity).inNamespace(null) .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
  }

  private void assertDeleteCount(int totalDeletions) {
    verify(kubernetesHelper,times(totalDeletions)).getKind((HasMetadata)any());
  }

  @Test
  void deleteWithKubernetesClient_shouldOnlyDeleteProvidedEntity() throws Exception {
    // Given
    final Pod entity = new Pod();
    withLoadedEntities(entity);
    when(jKubeServiceHub.getClient().adapt(OpenShiftClient.class)).thenReturn(null);
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