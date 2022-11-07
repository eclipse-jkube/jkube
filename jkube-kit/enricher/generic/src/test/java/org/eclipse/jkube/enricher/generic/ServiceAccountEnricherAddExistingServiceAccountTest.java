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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ServiceAccountConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;;

import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ServiceAccountEnricherAddExistingServiceAccountTest {
  private JKubeEnricherContext context;

  public static Stream<Arguments> data() {
    return Stream.of(
        arguments(createNewDeployment()),
        arguments(createNewDaemonSet()),
        arguments(createNewStatefulSet()),
        arguments(createNewReplicaSet()),
        arguments(createNewReplicationController()),
        arguments(createNewJob()));
  }

  @BeforeEach
  public void setUp() {
    context = JKubeEnricherContext.builder()
        .project(JavaProject.builder()
            .groupId("org.eclipse.jkube")
            .artifactId("test-artifact")
            .version("1.0.0")
            .properties(new Properties())
            .build())
        .build();
  }

  @ParameterizedTest
  @MethodSource("data")
  void create_withNullResourceConfig_shouldNotAddServiceAccountToController(HasMetadata controller) {
    // Given
    final KubernetesListBuilder builder = new KubernetesListBuilder();
    builder.addToItems(controller);

    // When
    new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

    // Then
    assertThat(builder.buildItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("spec.template.spec.serviceAccountName", null);
  }

  @ParameterizedTest
  @MethodSource("data")
  void create_withServiceAccountInResourceConfig_shouldAddServiceAccountToController(HasMetadata controller) {
    // Given
    givenResourceConfig(ResourceConfig.builder().serviceAccount("my-existing-serviceaccount").build());
    final KubernetesListBuilder builder = new KubernetesListBuilder();
    builder.addToItems(controller);

    // When
    new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

    // Then
    assertThat(builder.buildItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("spec.template.spec.serviceAccountName", "my-existing-serviceaccount");
  }

  @ParameterizedTest
  @MethodSource("data")
  void create_withServiceAccountsInResourceConfig_shouldAddServiceAccountToController(HasMetadata controller) {
    // Given
    givenResourceConfig(ResourceConfig.builder()
        .serviceAccount(ServiceAccountConfig.builder()
            .name("my-existing-serviceaccount")
            .deploymentRef("test-artifact")
            .generate(false)
            .bindToAllControllers(false)
            .build())
        .build());
    final KubernetesListBuilder builder = new KubernetesListBuilder();
    builder.addToItems(controller);

    // When
    new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

    // Then
    assertThat(builder.buildItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("spec.template.spec.serviceAccountName", "my-existing-serviceaccount");
  }

  private void givenResourceConfig(ResourceConfig resourceConfig) {
    context = context.toBuilder().resources(resourceConfig).build();
  }

  private static DaemonSet createNewDaemonSet() {
    return new DaemonSetBuilder()
        .withMetadata(createOpinionatedMetadata())
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static Deployment createNewDeployment() {
    return new DeploymentBuilder()
        .withMetadata(createOpinionatedMetadata())
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static Job createNewJob() {
    return new JobBuilder()
        .withMetadata(createOpinionatedMetadata())
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static ReplicaSet createNewReplicaSet() {
    return new ReplicaSetBuilder()
        .withMetadata(createOpinionatedMetadata())
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static ReplicationController createNewReplicationController() {
    return new ReplicationControllerBuilder()
        .withMetadata(createOpinionatedMetadata())
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static StatefulSet createNewStatefulSet() {
    return new StatefulSetBuilder()
        .withMetadata(createOpinionatedMetadata())
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static ObjectMeta createOpinionatedMetadata() {
    return new ObjectMetaBuilder().withName("test-artifact").build();
  }
}
