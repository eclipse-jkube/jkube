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
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings({"AccessStaticViaInstance", "unused"})
public class OpenshiftUndeployServiceTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mocked
  private KitLogger logger;
  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private KubernetesClient kubernetesClient;
  @Mocked
  private OpenShiftClient openShiftClient;
  @Mocked
  private KubernetesHelper kubernetesHelper;
  private OpenshiftUndeployService openshiftUndeployService;

  @Before
  public void setUp() {
    openshiftUndeployService = new OpenshiftUndeployService(jKubeServiceHub, logger);
    // @formatter:off
    new Expectations() {{
      jKubeServiceHub.getClient().adapt(OpenShiftClient.class); result = openShiftClient;
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
    openshiftUndeployService = null;
  }

  private void withLoadedEntities(HasMetadata... entities) throws Exception {
    // @formatter:off
    new Expectations() {{
      kubernetesHelper.loadResources((File)any); result = Arrays.asList(entities);
    }};
    // @formatter:on
  }

  private void with(Build build, BuildConfig buildConfig) {
    // @formatter:off
    new Expectations() {{
      openShiftClient.builds().inNamespace(null).list();
      result = new BuildListBuilder().withItems(build).build();
      openShiftClient.buildConfigs().inNamespace(null).list();
      result = new BuildConfigListBuilder().withItems(buildConfig).build();
    }};
    // @formatter:on
  }

  private void assertDeleted(HasMetadata entity) {
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getClient().resource(entity).inNamespace(null)
          .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
    }};
    // @formatter:on
  }

  private void assertDeleteCount(int totalDeletions) {
    // @formatter:off
    new Verifications() {{
      kubernetesHelper.getKind((HasMetadata)any); times = totalDeletions;
    }};
    // @formatter:on
  }

  @Test
  public void deleteWithKubernetesClientShouldOnlyDeleteProvidedEntity() throws Exception {
    // Given
    final Pod entity = new Pod();
    withLoadedEntities(entity);
    // @formatter:off
    new Expectations() {{
      jKubeServiceHub.getClient().adapt(OpenShiftClient.class); result = null;
    }};
    // @formatter:on
    // When
    openshiftUndeployService.undeploy(null, ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  public void deleteWithOpenShiftClientAndNoImageStreamShouldOnlyDeleteProvidedEntity() throws Exception {
    // Given
    final Pod entity = new Pod();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  public void deleteWithOpenShiftClientAndImageStreamShouldDeleteProvidedImageStream() throws Exception {
    // Given
    final ImageStream entity = new ImageStreamBuilder()
        .withNewMetadata().withName("image").endMetadata()
        .withNewSpec().withTags().endSpec()
        .build();
    withLoadedEntities(entity);
    // When
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  public void deleteWithOpenShiftClientAndImageStreamShouldDeleteProvidedImageStreamAndRelatedBuildEntities()
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
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(3);
    assertDeleted(entity);
    assertDeleted(build);
    assertDeleted(buildConfig);
  }


  @Test
  public void deleteWithOpenShiftClientAndDeploymentConfigShouldDeleteProvidedDeploymentConfigAndRelatedBuildEntities()
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
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(3);
    assertDeleted(entity);
    assertDeleted(build);
    assertDeleted(buildConfig);
  }

  @Test
  public void deleteWithOpenShiftClientAndDeploymentConfigNoMatchingLabelShouldDeleteProvidedDeploymentConfigOnly()
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
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(1);
    assertDeleted(entity);
  }

  @Test
  public void deleteWithOpenShiftClientAndDeploymentConfigShouldDeleteProvidedDeploymentConfigAndRelatedMatchingBuildEntities()
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
    openshiftUndeployService.undeploy(null,  ResourceConfig.builder().build(), temporaryFolder.newFile());
    // Then
    assertDeleteCount(2);
    assertDeleted(entity);
    assertDeleted(build);
  }
}