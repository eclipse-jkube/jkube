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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class WellKnownLabelsEnricherTest {
  private WellKnownLabelsEnricher wellKnownLabelsEnricher;
  private Properties properties;
  private KubernetesListBuilder kubernetesListBuilder;
  private JKubeEnricherContext context;

  @BeforeEach
  void setup() {
    properties = new Properties();
    context = JKubeEnricherContext.builder()
        .project(JavaProject.builder()
            .groupId("org.example")
            .artifactId("test-project")
            .version("0.0.1")
            .properties(properties)
            .build())
        .build();
    kubernetesListBuilder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().endMetadata().build());
    wellKnownLabelsEnricher = new WellKnownLabelsEnricher(context);
  }

  static Stream<Arguments> resourcesContainingSelectorLabels() {
    return Stream.of(
        arguments(new DeploymentConfigBuilder().build()),
        arguments(new ServiceBuilder().build()),
        arguments(new ReplicationControllerBuilder().build())
    );
  }

  @ParameterizedTest
  @MethodSource("resourcesContainingSelectorLabels")
  void whenControllerResourcesPresentInBuilder_thenAddWellKnownLabelsToSelector(HasMetadata hasMetadata) {
    // Given
    kubernetesListBuilder = new KubernetesListBuilder().withItems(hasMetadata);

    // When
    wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertWellKnownLabelsAddedToResource("spec.selector", false);
  }

  static Stream<Arguments> resourcesContainingSelectorMatchLabels() {
    return Stream.of(
        arguments(new DeploymentBuilder().build()),
        arguments(new DaemonSetBuilder().build()),
        arguments(new ReplicaSetBuilder().build()),
        arguments(new StatefulSetBuilder().build())
    );
  }

  @ParameterizedTest
  @MethodSource("resourcesContainingSelectorMatchLabels")
  void whenControllerResourcesPresentInBuilder_thenAddWellKnownLabelsToMatchSelector(HasMetadata hasMetadata) {
    // Given
    kubernetesListBuilder = new KubernetesListBuilder().withItems(hasMetadata);

    // When
    wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertWellKnownLabelsAddedToResource("spec.selector.matchLabels", false);
  }

  @Nested
  @DisplayName("create")
  class Create {
    @Test
    @DisplayName("when resource already contains well-known-label, then do not overwrite")
    void whenWellKnownLabelAlreadyPresent_thenDoNotOverwriteLabel() {
      // Given
      kubernetesListBuilder = new KubernetesListBuilder().withItems(new DeploymentBuilder()
              .withNewSpec()
              .withNewSelector()
              .addToMatchLabels("app.kubernetes.io/part-of", "already-present-part-of")
              .addToMatchLabels("app.kubernetes.io/component", "already-present-component")
              .addToMatchLabels("app.kubernetes.io/managed-by", "already-present-managed-by")
              .addToMatchLabels("app.kubernetes.io/name", "already-present-name")
              .addToMatchLabels("app.kubernetes.io/version", "1.0.0-already-present")
              .endSelector()
              .endSpec()
          .build());

      // When
      wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      Deployment deployment = (Deployment) kubernetesListBuilder.buildFirstItem();
      assertThat(deployment)
          .extracting("spec.selector.matchLabels")
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("app.kubernetes.io/part-of", "already-present-part-of")
          .containsEntry("app.kubernetes.io/component", "already-present-component")
          .containsEntry("app.kubernetes.io/managed-by", "already-present-managed-by")
          .containsEntry("app.kubernetes.io/name", "already-present-name")
          .containsEntry("app.kubernetes.io/version", "1.0.0-already-present");
    }

    @Test
    @DisplayName("when labels configured via resource config's labels > all, then use configured labels")
    void whenWellKnownLabelConfiguredViaResourceConfigLabelAll_thenUseLabelsViaResourceConfig() {
      // Given
      Properties allLabels = new Properties();
      allLabels.put("app.kubernetes.io/part-of", "part-of-via-resource-config");
      allLabels.put("app.kubernetes.io/component", "component-via-resource-config");
      allLabels.put("app.kubernetes.io/managed-by", "managed-by-via-resource-config");
      allLabels.put("app.kubernetes.io/name", "name-via-resource-config");
      context = context.toBuilder()
          .resources(ResourceConfig.builder()
              .labels(MetaDataConfig.builder()
                  .all(allLabels)
                  .build())
              .build())
          .build();
      wellKnownLabelsEnricher = new WellKnownLabelsEnricher(context);

      // When
      wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      Deployment deployment = (Deployment) kubernetesListBuilder.buildFirstItem();
      assertThat(deployment)
          .extracting("spec.selector.matchLabels")
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("app.kubernetes.io/part-of", "part-of-via-resource-config")
          .containsEntry("app.kubernetes.io/component", "component-via-resource-config")
          .containsEntry("app.kubernetes.io/managed-by", "managed-by-via-resource-config")
          .containsEntry("app.kubernetes.io/name", "name-via-resource-config");
    }

    @Test
    @DisplayName("when labels configured via resource config's labels > deployment, then use configured labels for Deployment only")
    void whenWellKnownLabelConfiguredViaResourceConfigLabelForSpecificResource_thenUseLabelsViaResourceConfig() {
      // Given
      Properties deploymentLabels = new Properties();
      deploymentLabels.put("app.kubernetes.io/part-of", "part-of-via-resource-config-labels-deployment");
      deploymentLabels.put("app.kubernetes.io/component", "component-via-resource-config-labels-deployment");
      deploymentLabels.put("app.kubernetes.io/managed-by", "managed-by-via-resource-config-labels-deployment");
      deploymentLabels.put("app.kubernetes.io/name", "name-via-resource-config-labels-deployment");
      context = context.toBuilder()
          .resources(ResourceConfig.builder()
              .labels(MetaDataConfig.builder()
                  .deployment(deploymentLabels)
                  .build())
              .build())
          .build();
      kubernetesListBuilder.addToItems(new ServiceBuilder().build());
      wellKnownLabelsEnricher = new WellKnownLabelsEnricher(context);

      // When
      wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      assertThat(kubernetesListBuilder.buildItems())
          .hasSize(2)
          .satisfies(items -> assertThat(items.get(0))
              .extracting("spec.selector.matchLabels")
              .asInstanceOf(InstanceOfAssertFactories.MAP)
              .containsEntry("app.kubernetes.io/part-of", "part-of-via-resource-config-labels-deployment")
              .containsEntry("app.kubernetes.io/component", "component-via-resource-config-labels-deployment")
              .containsEntry("app.kubernetes.io/managed-by", "managed-by-via-resource-config-labels-deployment")
              .containsEntry("app.kubernetes.io/name", "name-via-resource-config-labels-deployment"))
          .satisfies(items -> assertThat(items.get(1))
              .extracting("spec.selector")
              .asInstanceOf(InstanceOfAssertFactories.MAP)
              .containsEntry("app.kubernetes.io/part-of", "org.example")
              .containsEntry("app.kubernetes.io/managed-by", "jkube")
              .containsEntry("app.kubernetes.io/name", "test-project"));
    }

    @Test
    @DisplayName("labels provided via enricher configuration, then add labels")
    void whenLabelsProvidedViaEnricherConfiguration_thenAddLabels() {
      // Given
      properties.put("jkube.enricher.jkube-well-known-labels.name", "test-app");
      properties.put("jkube.enricher.jkube-well-known-labels.component", "authentication");
      properties.put("jkube.enricher.jkube-well-known-labels.partOf", "auth-infra");
      properties.put("jkube.enricher.jkube-well-known-labels.managedBy", "qe");

      // When
      wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      Deployment deployment = (Deployment) kubernetesListBuilder.buildFirstItem();
      assertThat(deployment)
          .extracting("spec.selector.matchLabels")
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("app.kubernetes.io/part-of", "auth-infra")
          .containsEntry("app.kubernetes.io/component", "authentication")
          .containsEntry("app.kubernetes.io/managed-by", "qe")
          .containsEntry("app.kubernetes.io/name", "test-app");
    }

    @Test
    @DisplayName("jkube.kubernetes.well-known-labels=false , then do not add labels")
    void whenDisabledViaProperty_thenNoLabelsAddedToProject() {
      // Given
      properties.put("jkube.kubernetes.well-known-labels", "false");

      // When
      wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      assertNoWellKnownLabelsAddedToResource("spec.selector.matchLabels");
    }

    @Test
    @DisplayName("disable via enricher config, enabled=false , then do not add labels")
    void whenDisabledViaEnricherConfiguration_thenNoLabelsAddedToProject() {
      // Given
      properties.put("jkube.enricher.jkube-well-known-labels.enabled", "false");

      // When
      wellKnownLabelsEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      assertNoWellKnownLabelsAddedToResource("spec.selector.matchLabels");
    }
  }


  @Nested
  @DisplayName("enrich")
  class Enrich {
    @Test
    @DisplayName("zero configuration, infer well known labels from project")
    void whenNoConfigurationProvided_thenInferLabelsFromProject() {
      // When
      wellKnownLabelsEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      assertWellKnownLabelsAddedToResource("metadata.labels", true);
    }

    @Test
    @DisplayName("labels present, merge well known labels with existing labels")
    void whenLabelsPresent_thenMergeWellKnownLabelsWithExistingLabels() {
      // Given
      kubernetesListBuilder = new KubernetesListBuilder().withItems(new ServiceBuilder()
              .withNewMetadata()
              .addToLabels("release", "canary")
              .addToLabels("environment", "test")
              .endMetadata()
          .build());

      // When
      wellKnownLabelsEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      Service deployment = (Service) kubernetesListBuilder.buildFirstItem();
      assertThat(deployment)
          .extracting("metadata.labels")
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("release", "canary")
          .containsEntry("environment", "test")
          .containsEntry("app.kubernetes.io/managed-by", "jkube")
          .containsEntry("app.kubernetes.io/name", "test-project")
          .containsEntry("app.kubernetes.io/version", "0.0.1");
    }

    @Test
    @DisplayName("when resource already contains well-known-label, then do not overwrite")
    void whenWellKnownLabelAlreadyPresent_thenDoNotOverwriteLabel() {
      // Given
      kubernetesListBuilder = new KubernetesListBuilder().withItems(new DeploymentBuilder()
          .withNewMetadata()
          .addToLabels("app.kubernetes.io/part-of", "already-present-part-of")
          .addToLabels("app.kubernetes.io/component", "already-present-component")
          .addToLabels("app.kubernetes.io/managed-by", "already-present-managed-by")
          .addToLabels("app.kubernetes.io/name", "already-present-name")
          .addToLabels("app.kubernetes.io/version", "1.0.0-already-present")
          .endMetadata()
          .build());

      // When
      wellKnownLabelsEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      Deployment deployment = (Deployment) kubernetesListBuilder.buildFirstItem();
      assertThat(deployment)
          .extracting("metadata.labels")
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("app.kubernetes.io/part-of", "already-present-part-of")
          .containsEntry("app.kubernetes.io/component", "already-present-component")
          .containsEntry("app.kubernetes.io/managed-by", "already-present-managed-by")
          .containsEntry("app.kubernetes.io/name", "already-present-name")
          .containsEntry("app.kubernetes.io/version", "1.0.0-already-present");
    }

    @Test
    @DisplayName("labels provided via enricher configuration, then add labels")
    void whenLabelsProvidedViaEnricherConfiguration_thenAddLabels() {
      // Given
      properties.put("jkube.enricher.jkube-well-known-labels.name", "test-app");
      properties.put("jkube.enricher.jkube-well-known-labels.version", "v1.0.0-alpha1");
      properties.put("jkube.enricher.jkube-well-known-labels.component", "authentication");
      properties.put("jkube.enricher.jkube-well-known-labels.partOf", "auth-infra");
      properties.put("jkube.enricher.jkube-well-known-labels.managedBy", "qe");

      // When
      wellKnownLabelsEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      Deployment deployment = (Deployment) kubernetesListBuilder.buildFirstItem();
      assertThat(deployment)
          .extracting("metadata.labels")
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("app.kubernetes.io/part-of", "auth-infra")
          .containsEntry("app.kubernetes.io/component", "authentication")
          .containsEntry("app.kubernetes.io/managed-by", "qe")
          .containsEntry("app.kubernetes.io/name", "test-app")
          .containsEntry("app.kubernetes.io/version", "v1.0.0-alpha1");
    }

    @Test
    @DisplayName("disable via enricher config, enabled=false , then do not add labels")
    void whenDisabledViaEnricherConfiguration_thenNoLabelsAddedToProject() {
      // Given
      properties.put("jkube.enricher.jkube-well-known-labels.enabled", "false");

      // When
      wellKnownLabelsEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      assertNoWellKnownLabelsAddedToResource("metadata.labels");
    }

    @Test
    @DisplayName("jkube.kubernetes.well-known-labels=false , then do not add labels")
    void whenDisabledViaProperty_thenNoLabelsAddedToProject() {
      // Given
      properties.put("jkube.kubernetes.well-known-labels", "false");

      // When
      wellKnownLabelsEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

      // Then
      assertNoWellKnownLabelsAddedToResource("metadata.labels");
    }
  }

  private void assertWellKnownLabelsAddedToResource(String field, boolean containsVersion) {
    HasMetadata hasMetadata = kubernetesListBuilder.buildFirstItem();
    assertThat(hasMetadata)
        .extracting(field)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("app.kubernetes.io/managed-by", "jkube")
        .containsEntry("app.kubernetes.io/name", "test-project")
        .containsEntry("app.kubernetes.io/part-of", "org.example");
    if (containsVersion) {
      assertThat(hasMetadata)
          .extracting(field)
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsEntry("app.kubernetes.io/version", "0.0.1");
    }
  }

  private void assertNoWellKnownLabelsAddedToResource(String field) {
    HasMetadata hasMetadata = kubernetesListBuilder.buildFirstItem();
    assertThat(hasMetadata)
        .extracting(field)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .doesNotContainKey("app.kubernetes.io/managed-by")
        .doesNotContainKey("app.kubernetes.io/name")
        .doesNotContainKey("app.kubernetes.io/part-of")
        .doesNotContainKey("app.kubernetes.io/component")
        .doesNotContainKey("app.kubernetes.io/version");
  }
}
