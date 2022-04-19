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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.junit.Before;
import org.junit.Test;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import mockit.Expectations;
import mockit.Mocked;

/**
 * Test label generation.
 *
 * @author Tue Dissing
 */
public class ProjectLabelEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  private Properties properties;
  private ProjectLabelEnricher projectLabelEnricher;

  @Before
  public void setupExpectations() {
    projectLabelEnricher = new ProjectLabelEnricher(context);
    properties = new Properties();
    // @formatter:off
    new Expectations() {{
      context.getProperties(); result = properties;
      context.getGav();
      result = new GroupArtifactVersion("groupId", "artifactId", "version");
    }};
    // @formatter:on
  }

  @Test
  public void testCustomAppName() {
    // Setup
    properties.setProperty("jkube.enricher.jkube-project-label.app", "my-custom-app-name");

    KubernetesListBuilder builder = createListWithDeploymentConfig();
    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
    KubernetesList list = builder.build();

    Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

    assertNotNull(labels);
    assertEquals("groupId", labels.get("group"));
    assertEquals("my-custom-app-name", labels.get("app"));
    assertEquals("version", labels.get("version"));
    assertNull(labels.get("project"));

    builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertEquals("groupId", selectors.get("group"));
    assertEquals("my-custom-app-name", selectors.get("app"));
    assertNull(selectors.get("version"));
    assertNull(selectors.get("project"));
  }

  @Test
  public void testEmptyCustomAppName() {
    // Setup
    properties.setProperty("jkube.enricher.jkube-project-label.app", "");

    KubernetesListBuilder builder = createListWithDeploymentConfig();
    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
    KubernetesList list = builder.build();

    Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

    assertNotNull(labels);
    assertEquals("groupId", labels.get("group"));
    assertEquals("", labels.get("app"));
    assertEquals("version", labels.get("version"));
    assertNull(labels.get("project"));

    builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertEquals("groupId", selectors.get("group"));
    assertEquals("", selectors.get("app"));
    assertNull(selectors.get("version"));
    assertNull(selectors.get("project"));
  }

  @Test
  public void testDefaultAppName() {
    KubernetesListBuilder builder = createListWithDeploymentConfig();
    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);
    KubernetesList list = builder.build();

    Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

    assertNotNull(labels);
    assertEquals("groupId", labels.get("group"));
    assertEquals("artifactId", labels.get("app"));
    assertEquals("version", labels.get("version"));
    assertNull(labels.get("project"));

    builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertEquals("groupId", selectors.get("group"));
    assertEquals("artifactId", selectors.get("app"));
    assertNull(selectors.get("version"));
    assertNull(selectors.get("project"));
  }

  @Test
  public void testEnrichCustomProvider() {
    properties.setProperty("jkube.enricher.jkube-project-label.provider", "my-custom-provider");
    KubernetesListBuilder builder = createListWithDeploymentConfig();

    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);

    Map<String, String> labels = builder.build().getItems().get(0).getMetadata().getLabels();
    assertNotNull(labels);
    assertEquals("my-custom-provider", labels.get("provider"));
  }

  @Test
  public void testCreateCustomProvider() {
    properties.setProperty("jkube.enricher.jkube-project-label.provider", "my-custom-provider");
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertEquals("my-custom-provider", selectors.get("provider"));
  }

  @Test
  public void testEnrichDefaultProvider() {
    KubernetesListBuilder builder = createListWithDeploymentConfig();

    projectLabelEnricher.enrich(PlatformMode.kubernetes, builder);

    Map<String, String> labels = builder.build().getItems().get(0).getMetadata().getLabels();
    assertNotNull(labels);
    assertEquals("jkube", labels.get("provider"));

  }

  @Test
  public void testCreateDefaultProvider() {
    KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());

    projectLabelEnricher.create(PlatformMode.kubernetes, builder);

    Deployment deployment = (Deployment) builder.buildFirstItem();
    Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
    assertEquals("jkube", selectors.get("provider"));
  }

  @Test
  public void create_withNoConfiguredGroup_shouldAddDefaultGroupInSelector() {
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
  public void create_withConfiguredGroup_shouldAddConfiguredGroupInSelector() {
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
  public void create_withNoConfiguredVersion_shouldAddDefaultVersionInSelector() {
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
  public void create_withConfiguredVersion_shouldAddConfiguredVersionInSelector() {
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
