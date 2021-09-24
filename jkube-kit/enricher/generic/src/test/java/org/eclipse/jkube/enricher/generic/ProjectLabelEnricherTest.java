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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.Properties;

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

  private KubernetesListBuilder createListWithDeploymentConfig() {
    return new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
        .withNewMetadata().endMetadata()
        .withNewSpec().endSpec()
        .build());
  }

}
