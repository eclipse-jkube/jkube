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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Properties;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test label generation.
 *
 * @author Tue Dissing
 */
class ProjectLabelEnricherTest {

  private Properties properties;
  private ProjectLabelEnricher projectLabelEnricher;

  @BeforeEach
  void setupExpectations() {
    JKubeEnricherContext context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    projectLabelEnricher = new ProjectLabelEnricher(context);
    properties = new Properties();
    when(context.getProperties()).thenReturn(properties);
    when(context.getGav()).thenReturn(new GroupArtifactVersion("groupId", "artifactId", "version"));
  }

  @Test
  void testCustomAppName() {
    // Setup
    properties.setProperty("jkube.enricher.jkube-project-label.app", "my-custom-app-name");

    KubernetesListBuilder builder = createListWithDeploymentConfig();
    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
    KubernetesList list = builder.build();

    Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
    assertThat(labels).isNotNull()
            .doesNotContainKey("project")
            .containsEntry("group","groupId")
            .containsEntry("app","my-custom-app-name")
            .containsEntry("version","version");

    builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertThat(selectors)
            .doesNotContainKey("version")
            .doesNotContainKey("project")
            .containsEntry("group","groupId")
            .containsEntry("app","my-custom-app-name");
  }

  @Test
  void testEmptyCustomAppName() {
    // Setup
    properties.setProperty("jkube.enricher.jkube-project-label.app", "");

    KubernetesListBuilder builder = createListWithDeploymentConfig();
    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
    KubernetesList list = builder.build();

    Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
    assertThat(labels)
            .isNotNull()
            .doesNotContainKey("project")
            .containsEntry("group","groupId")
            .containsEntry("app","")
            .containsEntry("version","version");
    builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertThat(selectors)
            .doesNotContainKey("version")
            .doesNotContainKey("project")
            .containsEntry("group","groupId")
            .containsEntry("app","");
  }

  @Test
  void testDefaultAppName() {
    KubernetesListBuilder builder = createListWithDeploymentConfig();
    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
    KubernetesList list = builder.build();

    Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

    assertThat(labels).isNotNull()
            .doesNotContainKey("project")
            .containsEntry("group","groupId")
            .containsEntry("app","artifactId").containsEntry("version","version");

    builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertThat(selectors)
            .doesNotContainKey("version")
            .doesNotContainKey("project")
            .containsEntry("group","groupId")
            .containsEntry("app","artifactId");
  }

  @Test
  void testEnrichCustomProvider() {
    properties.setProperty("jkube.enricher.jkube-project-label.provider", "my-custom-provider");
    KubernetesListBuilder builder = createListWithDeploymentConfig();

    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);

    Map<String, String> labels = builder.build().getItems().get(0).getMetadata().getLabels();
    assertThat(labels).isNotNull()
            .containsEntry("provider","my-custom-provider");
  }

  @Test
  void testCreateCustomProvider() {
    properties.setProperty("jkube.enricher.jkube-project-label.provider", "my-custom-provider");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertThat(selectors)
            .containsEntry("provider","my-custom-provider");
  }

  @Test
  void testEnrichDefaultProvider() {
    KubernetesListBuilder builder = createListWithDeploymentConfig();

    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);

    Map<String, String> labels = builder.build().getItems().get(0).getMetadata().getLabels();
    assertThat(labels).isNotNull()
            .containsEntry("provider","jkube");

  }

  @Test
  void testCreateDefaultProvider() {
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertThat(selectors)
            .containsEntry("provider","jkube");
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

  private KubernetesListBuilder createListWithDeploymentConfig() {
    return new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
        .withNewMetadata().endMetadata()
        .withNewSpec().endSpec()
        .build());
  }

}
