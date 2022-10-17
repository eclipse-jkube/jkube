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
package org.eclipse.jkube.enricher.generic.openshift;

import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentConfigEnricherTest {

  private JKubeEnricherContext context;
  private DeploymentConfigEnricher deploymentConfigEnricher;
  private KubernetesListBuilder kubernetesListBuilder;

  @BeforeEach
  void setUp() {
    KitLogger logger = new KitLogger.SilentLogger();
    context = JKubeEnricherContext.builder()
      .log(logger)
      .project(JavaProject.builder()
        .properties(new Properties())
        .build())
      .summaryService(new SummaryService(new File("target"), logger, false))
      .build();
    deploymentConfigEnricher = new DeploymentConfigEnricher(context);
    kubernetesListBuilder = new KubernetesListBuilder();
  }

  @Test
  void create_inKubernetes_shouldSkip() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    // When
    deploymentConfigEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .isInstanceOf(io.fabric8.kubernetes.api.model.apps.Deployment.class);
  }

  @Test
  void create_inOpenShiftWithSwitchToDeployment_shouldSkip() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    context.getProperties().put("jkube.build.switchToDeployment", "true");
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .isInstanceOf(io.fabric8.kubernetes.api.model.apps.Deployment.class);
  }

  @Test
  void create_inOpenShiftWithDefaultControllerEnricherTypeAsDeployment_shouldSkip() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    context.getProperties().put("jkube.enricher.jkube-controller.type", "Deployment");
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .isInstanceOf(io.fabric8.kubernetes.api.model.apps.Deployment.class);
  }

  @Test
  void create_inOpenShift_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Rolling"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams", null)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams.timeoutSeconds", 3600L);
  }

  @Test
  void create_inOpenShiftWithDefaultControllerEnricherTypeAsDeploymentConfig_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    context.getProperties().put("jkube.enricher.jkube-controller.type", "DeploymentConfig");
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Rolling"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams", null)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams.timeoutSeconds", 3600L);
  }

  @Test
  void create_inOpenShiftWithRecreateStrategy_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment()
        .editSpec().editOrNewStrategy().withType("Recreate").endStrategy().endSpec()
        .build());
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Recreate"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams.timeoutSeconds", 3600L)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams", null);
  }

  @Test
  void create_inOpenShiftWithRollingStrategy_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment()
        .editSpec().editOrNewStrategy().withType("Rolling").endStrategy().endSpec()
        .build());
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Rolling"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams", null)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams.timeoutSeconds", 3600L);
  }

  @Test
  void create_inOpenShiftWithCustomStrategy_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment()
        .editSpec().editOrNewStrategy().withType("Custom").endStrategy().endSpec()
        .build());
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Custom"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams", null)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams", null);
  }

  @Test
  void create_inOpenShiftWithExtensionsDeployment_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(extensionsV1beta1Deployment().build());
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Rolling"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams", null)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams.timeoutSeconds", 3600L);
  }

  @Test
  void create_inOpenShiftWithCustomTimeout_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    context.getProperties().put("jkube.openshift.deployTimeoutSeconds", "60");
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig("Rolling"))
        .hasFieldOrPropertyWithValue("spec.strategy.recreateParams", null)
        .hasFieldOrPropertyWithValue("spec.strategy.rollingParams.timeoutSeconds", 60L);
  }

  @Test
  void create_inOpenShiftWithNegativeTimeout_shouldConvert() {
    // Given
    kubernetesListBuilder.addToItems(appsV1Deployment().build());
    context.getProperties().put("jkube.openshift.deployTimeoutSeconds", "-60");
    // When
    deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.buildItems()).singleElement()
        .satisfies(assertDeploymentConfig(null))
        .hasFieldOrPropertyWithValue("spec.strategy", null);
  }

  private static io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder extensionsV1beta1Deployment() {
    return new io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder()
        .withMetadata(new ObjectMetaBuilder().withName("test-app").addToLabels("app", "test-app").build())
        .withNewSpec()
        .withReplicas(3)
        .withRevisionHistoryLimit(2)
        .withNewSelector().addToMatchLabels("app", "test-app").endSelector()
        .withNewTemplate()
        .withNewMetadata().addToLabels("app", "test-app").endMetadata()
        .withNewSpec()
        .addNewContainer()
        .withName("test-container")
        .withImage("test-image:1.0.0")
        .addNewPort()
        .withContainerPort(80)
        .endPort()
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }

  private static io.fabric8.kubernetes.api.model.apps.DeploymentBuilder appsV1Deployment() {
    return new io.fabric8.kubernetes.api.model.apps.DeploymentBuilder()
        .withMetadata(new ObjectMetaBuilder().withName("test-app").addToLabels("app", "test-app").build())
        .withNewSpec()
        .withReplicas(3)
        .withRevisionHistoryLimit(2)
        .withNewSelector().addToMatchLabels("app", "test-app").endSelector()
        .withNewTemplate()
        .withNewMetadata().addToLabels("app", "test-app").endMetadata()
        .withNewSpec()
        .addNewContainer()
        .withName("test-container")
        .withImage("test-image:1.0.0")
        .addNewPort()
        .withContainerPort(80)
        .endPort()
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }

  private Consumer<HasMetadata> assertDeploymentConfig(String strategyType) {
    return deploymentConfig -> assertThat(deploymentConfig)
        .isInstanceOf(DeploymentConfig.class)
        .hasFieldOrPropertyWithValue("metadata.name", "test-app")
        .hasFieldOrPropertyWithValue("spec.replicas", 3)
        .hasFieldOrPropertyWithValue("spec.revisionHistoryLimit", 2)
        .hasFieldOrPropertyWithValue("spec.template.metadata.labels.app", "test-app")
        .hasFieldOrPropertyWithValue("spec.strategy.type", strategyType)
        .extracting("spec.template.spec.containers").asList()
        .first()
        .hasFieldOrPropertyWithValue("name", "test-container")
        .hasFieldOrPropertyWithValue("image", "test-image:1.0.0");
  }
}
