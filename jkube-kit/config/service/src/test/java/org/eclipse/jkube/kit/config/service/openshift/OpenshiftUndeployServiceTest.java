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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParamsBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.TagReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@SuppressWarnings({"unused"})
@EnableKubernetesMockClient(crud = true)
class OpenshiftUndeployServiceTest {

  @TempDir
  private Path tempDir;
  private KubernetesMockServer mockServer;
  private OpenShiftClient openShiftClient;
  private ResourceConfig resourceConfig;
  private OpenshiftUndeployService openshiftUndeployService;

  @BeforeEach
  void setUp() {
    final KitLogger logger = spy(new KitLogger.SilentLogger());
    final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
      .log(logger)
      .platformMode(RuntimeMode.KUBERNETES)
      .configuration(JKubeConfiguration.builder().build())
      .clusterAccess(new ClusterAccess(logger, ClusterConfiguration.from(openShiftClient.getConfiguration()).namespace("test").build()))
      .build();
    resourceConfig = ResourceConfig.builder().namespace("test").build();
    openshiftUndeployService = new OpenshiftUndeployService(jKubeServiceHub, logger);
    // In OpenShift
    mockServer.expect()
      .get()
      .withPath("/apis")
      .andReturn(200, new APIGroupListBuilder()
        .addNewGroup().withName("build.openshift.io").withApiVersion("v1").endGroup()
        .build())
      .always();
  }

  @Test
  void undeploy_shouldDeleteProvidedEntity() throws Exception {
    // Given
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    openShiftClient.resource(pod).create();
    final File manifest = serializedManifest(pod);
    // When
    openshiftUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    assertThat(openShiftClient.pods().withName("MrPoddington").get()).isNull();
    assertThat(openShiftClient.pods().inAnyNamespace().list().getItems()).isEmpty();
  }

  @Test
  void undeploy_imageStream_shouldDeleteProvidedEntity() throws Exception {
    // Given
    final ImageStream entity = new ImageStreamBuilder()
        .withNewMetadata().withName("image").endMetadata()
        .withNewSpec().withTags().endSpec()
        .build();
    openShiftClient.resource(entity).create();
    final File manifest = serializedManifest(entity);
    // When
    openshiftUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    assertThat(openShiftClient.imageStreams().withName("image").get()).isNull();
    assertThat(openShiftClient.imageStreams().inAnyNamespace().list().getItems()).isEmpty();
  }

  @Test
  void undeploy_imageStream_shouldDeleteProvidedImageStreamAndRelatedBuildEntities()
      throws Exception {
    // Given
    final Build build = new BuildBuilder()
      .withNewMetadata().withName("build").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder()
      .withNewMetadata().withName("build-config").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfigUnrelated = new BuildConfigBuilder()
      .withNewMetadata().withName("build-config-unrelated").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream-other:latest")
      .endTo().endOutput().endSpec().build();
    final ImageStream imageStream = new ImageStreamBuilder()
      .withNewMetadata().withName("image-stream").endMetadata()
      .withNewSpec().withTags(new TagReferenceBuilder().withName("latest").build()).endSpec().build();
    for (HasMetadata entity : new HasMetadata[]{build, buildConfig, buildConfigUnrelated, imageStream}) {
      openShiftClient.resource(entity).create();
    }
    final File manifest = serializedManifest(imageStream);
    // When
    openshiftUndeployService.undeploy(null,  resourceConfig, manifest);
    // Then
    assertThat(openShiftClient.builds().withName("build").get()).isNull();
    assertThat(openShiftClient.builds().inAnyNamespace().list().getItems()).isEmpty();
    assertThat(openShiftClient.buildConfigs().withName("build-config").get()).isNull();
    assertThat(openShiftClient.buildConfigs().withName("build-config-unrelated").get()).isNotNull();
    assertThat(openShiftClient.imageStreams().withName("image-stream").get()).isNull();
    assertThat(openShiftClient.imageStreams().inAnyNamespace().list().getItems()).isEmpty();
  }

  @Test
  void undeploy_deploymentConfig_shouldDeleteProvidedDeploymentConfigAndRelatedBuildEntities()
      throws Exception {
    // Given
    final Build build = new BuildBuilder()
      .withNewMetadata().withName("build").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder()
      .withNewMetadata().withName("build-config").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final DeploymentConfig deploymentConfig = new DeploymentConfigBuilder()
        .withNewMetadata().withName("deployment-config").endMetadata()
        .withNewSpec().withTriggers(new DeploymentTriggerPolicyBuilder()
            .withType("ImageChange")
            .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
                .withFrom(new ObjectReferenceBuilder()
                    .withName("image-stream:latest")
                    .withKind("ImageStreamTag")
                    .build())
                .build())
            .build()
          )
        .endSpec().build();
    for (HasMetadata entity : new HasMetadata[]{build, buildConfig, deploymentConfig}) {
      openShiftClient.resource(entity).create();
    }
    final File manifest = serializedManifest(deploymentConfig);
    // When
    openshiftUndeployService.undeploy(null,  resourceConfig, manifest);
    // Then
    assertThat(openShiftClient.builds().withName("build").get()).isNull();
    assertThat(openShiftClient.builds().inAnyNamespace().list().getItems()).isEmpty();
    assertThat(openShiftClient.buildConfigs().withName("build-config").get()).isNull();
    assertThat(openShiftClient.buildConfigs().inAnyNamespace().list().getItems()).isEmpty();
    assertThat(openShiftClient.deploymentConfigs().withName("deployment-config").get()).isNull();
    assertThat(openShiftClient.deploymentConfigs().inAnyNamespace().list().getItems()).isEmpty();
  }

  @Test
  void undeploy_deploymentConfigNoMatchingLabel_shouldDeleteProvidedDeploymentConfigOnly()
      throws Exception {
    // Given
    final Build build = new BuildBuilder()
      .withNewMetadata().withName("build").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder()
      .withNewMetadata().withName("build-config").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final DeploymentConfig deploymentConfig = new DeploymentConfigBuilder()
      .withNewMetadata().withName("deployment-config").addToLabels("provider", "different").endMetadata()
      .withNewSpec().withTriggers(new DeploymentTriggerPolicyBuilder()
        .withType("ImageChange")
        .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
          .withFrom(new ObjectReferenceBuilder()
            .withName("image-stream:latest")
            .withKind("ImageStreamTag")
            .build())
          .build())
        .build()
      )
      .endSpec().build();
    for (HasMetadata entity : new HasMetadata[]{build, buildConfig, deploymentConfig}) {
      openShiftClient.resource(entity).create();
    }
    final File manifest = serializedManifest(deploymentConfig);
    // When
    openshiftUndeployService.undeploy(null,  resourceConfig, manifest);
    // Then
    assertThat(openShiftClient.builds().withName("build").get()).isNotNull();
    assertThat(openShiftClient.buildConfigs().withName("build-config").get()).isNotNull();
    assertThat(openShiftClient.deploymentConfigs().withName("deployment-config").get()).isNull();
    assertThat(openShiftClient.deploymentConfigs().inAnyNamespace().list().getItems()).isEmpty();
  }

  @Test
  void undeploy_deploymentConfig_shouldDeleteProvidedDeploymentConfigAndRelatedMatchingBuildEntities()
      throws Exception {
    // Given
    final Build build = new BuildBuilder()
      .withNewMetadata().withName("build").addToLabels("provider", "jkube").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final BuildConfig buildConfig = new BuildConfigBuilder()
      .withNewMetadata().withName("build-config").endMetadata()
      .withNewSpec().withNewOutput().withNewTo().withName("image-stream:latest")
      .endTo().endOutput().endSpec().build();
    final DeploymentConfig deploymentConfig = new DeploymentConfigBuilder()
      .withNewMetadata().withName("deployment-config").addToLabels("provider", "jkube").endMetadata()
      .withNewSpec().withTriggers(new DeploymentTriggerPolicyBuilder()
        .withType("ImageChange")
        .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
          .withFrom(new ObjectReferenceBuilder()
            .withName("image-stream:latest")
            .withKind("ImageStreamTag")
            .build())
          .build())
        .build()
      )
      .endSpec().build();
    for (HasMetadata entity : new HasMetadata[]{build, buildConfig, deploymentConfig}) {
      openShiftClient.resource(entity).create();
    }
    final File manifest = serializedManifest(deploymentConfig);
    // When
    openshiftUndeployService.undeploy(null,  resourceConfig, manifest);
    // Then
    assertThat(openShiftClient.builds().withName("build").get()).isNull();
    assertThat(openShiftClient.builds().inAnyNamespace().list().getItems()).isEmpty();
    assertThat(openShiftClient.buildConfigs().withName("build-config").get()).isNotNull();
    assertThat(openShiftClient.deploymentConfigs().withName("deployment-config").get()).isNull();
    assertThat(openShiftClient.deploymentConfigs().inAnyNamespace().list().getItems()).isEmpty();
  }

  private File serializedManifest(HasMetadata... resources) throws IOException {
    final File file = Files.createFile(tempDir.resolve("openshift.yml")).toFile();
    FileUtils.write(file,
      Serialization.asJson(new KubernetesListBuilder().addToItems(resources).build()),
      Charset.defaultCharset());
    return file;
  }
}
