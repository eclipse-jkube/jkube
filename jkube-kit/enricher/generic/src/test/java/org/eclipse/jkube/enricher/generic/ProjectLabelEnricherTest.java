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

import java.util.Map;
import java.util.Properties;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test label generation.
 *
 * @author Tue Dissing
 */
class ProjectLabelEnricherTest {

  private Properties properties;
  private ProjectLabelEnricher projectLabelEnricher;
  private JKubeEnricherContext context;

  @BeforeEach
  void setupExpectations() {
    properties = new Properties();
    context = JKubeEnricherContext.builder()
        .project(JavaProject.builder()
            .properties(properties)
            .groupId("groupId")
            .artifactId("artifactId")
            .version("version")
            .build())
        .build();
    projectLabelEnricher = new ProjectLabelEnricher(context);
  }

  @Test
  void create_customAppName() {
    // Given
    properties.setProperty("jkube.enricher.jkube-project-label.app", "my-custom-app-name");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);
    // Then
    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting("spec.selector.matchLabels")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("group", "groupId")
        .containsEntry("app", "my-custom-app-name")
        .doesNotContainKey("version")
        .doesNotContainKey("project");
  }

  @Test
  void create_emptyCustomAppName() {
    // Given
    properties.setProperty("jkube.enricher.jkube-project-label.app", "");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);
    // Then
    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting("spec.selector.matchLabels")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("group", "groupId")
        .containsEntry("app", "")
        .doesNotContainKey("version")
        .doesNotContainKey("project");
  }

  @Test
  void create_defaultAppName() {
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting("spec.selector.matchLabels")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("group", "groupId")
        .containsEntry("app", "artifactId")
        .doesNotContainKey("version")
        .doesNotContainKey("project");
  }

  @Test
  void create_customProvider() {
    properties.setProperty("jkube.enricher.jkube-project-label.provider", "my-custom-provider");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting("spec.selector.matchLabels")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("provider", "my-custom-provider");
  }

  @Test
  void create_defaultProvider() {
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting("spec.selector.matchLabels")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("provider", "jkube");
  }

  @Test
  void create_withNoConfiguredGroup_shouldAddDefaultGroupInSelector() {
    // Given
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    // Then
    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getSelector)
        .extracting(LabelSelector::getMatchLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("group", "groupId");
  }

  @Test
  void create_withConfiguredGroup_shouldAddConfiguredGroupInSelector() {
    // Given
    properties.setProperty("jkube.enricher.jkube-project-label.group", "org.example.test");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    // Then
    Deployment deployment = (Deployment) builder.buildFirstItem();
    assertThat(deployment)
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getSelector)
        .extracting(LabelSelector::getMatchLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("group", "org.example.test");
  }

  @Test
  void create_withNoConfiguredVersion_shouldAddDefaultVersionInSelector() {
    // Given
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new StatefulSetBuilder().build());

    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    // Then
    StatefulSet statefulSet = (StatefulSet) builder.buildFirstItem();
    assertThat(statefulSet)
        .extracting(StatefulSet::getSpec)
        .extracting(StatefulSetSpec::getSelector)
        .extracting(LabelSelector::getMatchLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("version", "version");
  }

  @Test
  void create_withConfiguredVersion_shouldAddConfiguredVersionInSelector() {
    // Given
    properties.setProperty("jkube.enricher.jkube-project-label.version", "0.0.1");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new StatefulSetBuilder().build());

    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    // Then
    StatefulSet statefulSet = (StatefulSet) builder.buildFirstItem();
    assertThat(statefulSet)
        .extracting(StatefulSet::getSpec)
        .extracting(StatefulSetSpec::getSelector)
        .extracting(LabelSelector::getMatchLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("version", "0.0.1");
  }

  @Test
  @DisplayName("when labels configured via resource config's labels > all, then use configured labels")
  void create_whenProjectLabelConfiguredViaResourceConfigLabelAll_thenUseLabelsViaResourceConfig() {
    // Given
    Properties allLabels = new Properties();
    allLabels.put("app", "app-via-resource-config");
    allLabels.put("provider", "provider-via-resource-config");
    allLabels.put("group", "group-via-resource-config");
    context = context.toBuilder()
        .resources(ResourceConfig.builder()
            .labels(MetaDataConfig.builder()
                .all(allLabels)
                .build())
            .build())
        .build();
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher = new ProjectLabelEnricher(context);

    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    Deployment deployment = (Deployment) kubernetesListBuilder.buildFirstItem();
    assertThat(deployment)
        .extracting("spec.selector.matchLabels")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("app", "app-via-resource-config")
        .containsEntry("provider", "provider-via-resource-config")
        .containsEntry("group", "group-via-resource-config");
  }

  @Test
  @DisplayName("when labels configured via resource config's labels > deployment, then use configured labels for Deployment only")
  void create_whenProjectLabelConfiguredViaResourceConfigLabelForSpecificResource_thenUseLabelsViaResourceConfigForSpecificResourceOnly() {
    // Given
    Properties deploymentLabels = new Properties();
    deploymentLabels.put("app", "app-via-resource-config-labels-deployment");
    deploymentLabels.put("provider", "provider-via-resource-config-labels-deployment");
    deploymentLabels.put("group", "group-via-resource-config-labels-deployment");
    context = context.toBuilder()
        .resources(ResourceConfig.builder()
            .labels(MetaDataConfig.builder()
                .deployment(deploymentLabels)
                .build())
            .build())
        .build();
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder()
        .addToItems(new DeploymentBuilder().build())
        .addToItems(new ServiceBuilder().build());
    projectLabelEnricher = new ProjectLabelEnricher(context);

    // When
    projectLabelEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.buildItems())
        .hasSize(2)
        .satisfies(items -> assertThat(items.get(0))
            .extracting("spec.selector.matchLabels")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("app", "app-via-resource-config-labels-deployment")
            .containsEntry("provider", "provider-via-resource-config-labels-deployment")
            .containsEntry("group", "group-via-resource-config-labels-deployment"))
        .satisfies(items -> assertThat(items.get(1))
            .extracting("spec.selector")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("app", "artifactId")
            .containsEntry("group", "groupId")
            .containsEntry("provider", "jkube"));
  }

  @Nested
  @DisplayName("enrich with")
  class Enrich {
    @Test
    @DisplayName("custom app name")
    void customAppName() {
      // Setup
      properties.setProperty("jkube.enricher.jkube-project-label.app", "my-custom-app-name");

      KubernetesListBuilder builder = createListWithDeploymentConfig();
      projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
      KubernetesList list = builder.build();

      Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
      assertThat(labels).isNotNull()
          .containsEntry("group", "groupId")
          .containsEntry("app", "my-custom-app-name")
          .containsEntry("version", "version")
          .doesNotContainKey("project");
    }

    @Test
    @DisplayName("empty custom app name")
    void emptyCustomAppName() {
      // Setup
      properties.setProperty("jkube.enricher.jkube-project-label.app", "");

      KubernetesListBuilder builder = createListWithDeploymentConfig();
      projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
      KubernetesList list = builder.build();

      Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
      assertThat(labels).isNotNull()
          .containsEntry("group", "groupId")
          .containsEntry("app", "")
          .containsEntry("version", "version")
          .doesNotContainKey("project");
    }

    @Test
    @DisplayName("default app name")
    void defaultAppName() {
      KubernetesListBuilder builder = createListWithDeploymentConfig();
      projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
      KubernetesList list = builder.build();

      Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
      assertThat(labels).isNotNull()
          .containsEntry("group", "groupId")
          .containsEntry("app", "artifactId")
          .containsEntry("version", "version")
          .doesNotContainKey("project");
    }

    @Test
    @DisplayName("custom provider")
    void customProvider() {
      properties.setProperty("jkube.enricher.jkube-project-label.provider", "my-custom-provider");
      KubernetesListBuilder builder = createListWithDeploymentConfig();

      projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);

      Map<String, String> labels = builder.build().getItems().get(0).getMetadata().getLabels();
      assertThat(labels).isNotNull().containsEntry("provider", "my-custom-provider");
    }

    @Test
    @DisplayName("default provider")
    void defaultProvider() {
      KubernetesListBuilder builder = createListWithDeploymentConfig();

      projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);

      Map<String, String> labels = builder.build().getItems().get(0).getMetadata().getLabels();
      assertThat(labels).isNotNull().containsEntry("provider", "jkube");
    }
  }

  private KubernetesListBuilder createListWithDeploymentConfig() {
    return new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
        .withNewMetadata().endMetadata()
        .withNewSpec().endSpec()
        .build());
  }
}
