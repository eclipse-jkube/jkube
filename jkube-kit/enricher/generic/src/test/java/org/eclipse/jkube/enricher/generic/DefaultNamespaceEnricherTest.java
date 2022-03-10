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

import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class DefaultNamespaceEnricherTest {

  private EnricherContext context;

  public void setExpectations(Properties properties, ResourceConfig resourceConfig) {
    context = JKubeEnricherContext.builder()
        .log(new KitLogger.SilentLogger())
        .resources(resourceConfig)
        .project(JavaProject.builder()
            .properties(properties)
            .groupId("group")
            .artifactId("artifact-id")
            .build())
        .build();
  }

  @Test
  public void noNameShouldReturnEmpty() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).isEmpty();
  }

  @Test
  public void createWithPropertiesAndUnknownTypeInKubernetesShouldAddNothing() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.type", "unknown");
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).isEmpty();
  }

  @Test
  public void createWithPropertiesInKubernetesShouldAddNamespace() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .isInstanceOf(Namespace.class)
        .hasFieldOrPropertyWithValue("metadata.name", "example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  public void createWithPropertiesInOpenShiftShouldAddProject() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .isInstanceOf(Project.class)
        .hasFieldOrPropertyWithValue("metadata.name", "example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  public void createWithPropertiesAndConfigInKubernetesShouldAddConfigNamespace() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", "config-example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .isInstanceOf(Namespace.class)
        .hasFieldOrPropertyWithValue("metadata.name", "config-example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  public void enrichWithPropertiesInKubernetesShouldAddNamespaceWithStatus() {
    // Given
    setNamespaceInResourceConfig("example");
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    Namespace namespace = new NamespaceBuilder()
            .editOrNewMetadata().withName("name").withNamespace("to-be-overwritten").endMetadata()
            .editOrNewStatus().withPhase("active").endStatus().build();
    Deployment deployment = new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata().build();
    klb.addToItems(namespace, deployment);
    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(2);
    assertThat(klb.build().getItems().get(1))
        .hasFieldOrPropertyWithValue("metadata.namespace", "example");
  }

  @Test
  public void enrichWithPropertiesInKubernetesShouldAddProjectWithStatus() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new ProjectBuilder()
            .withNewMetadata().withName("name").endMetadata()
            .withNewStatus().withPhase("active").endStatus().build());
    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
            .hasFieldOrPropertyWithValue("metadata.namespace", null);
  }

  @Test
  public void enrichWithNamespaceFragmentWithNoStatus() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new NamespaceBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(1);
    assertThat(kubernetesListBuilder.build().getItems().iterator().next())
            .hasFieldOrPropertyWithValue("metadata.name", "test-jkube");
    assertNull(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace());
  }

  @Test
  public void enrichWithOpenShiftProjectFragmentWithNoStatus() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ProjectBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(1);
    assertThat(kubernetesListBuilder.build().getItems().iterator().next())
            .hasFieldOrPropertyWithValue("metadata.name", "test-jkube");
    assertNull(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace());
  }

  @Test
  public void testNoNamespaceConfiguredInMetadataIfNoPropertyProvided() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(2);
    assertThat(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace()).isNull();
    assertThat(kubernetesListBuilder.build().getItems().get(1).getMetadata().getNamespace()).isNull();
  }

  @Test
  public void testNamespaceSetInResourceConfigShouldConfigureNamespaceInMetadataOnPlatformKubernetes() {
    // Given
    setNamespaceInResourceConfig("mynamespace-configured");
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(2);
    assertThat(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace()).isEqualTo("mynamespace-configured");
    assertThat(kubernetesListBuilder.build().getItems().get(1).getMetadata().getNamespace()).isEqualTo("mynamespace-configured");
  }

  @Test
  public void testNamespaceSetInResourceConfigShouldConfigureNamespaceInMetadataOnPlatformOpenShift() {
    // Given
    setNamespaceInResourceConfig("mynamespace-configured");
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(2);
    assertThat(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace()).isEqualTo("mynamespace-configured");
    assertThat(kubernetesListBuilder.build().getItems().get(1).getMetadata().getNamespace()).isEqualTo("mynamespace-configured");
  }

  @Test
  public void namespaceSetInResourceShouldNotBeOverwritten() {
    // Given
    setNamespaceInResourceConfig("mynamespace-configured");
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ConfigMapBuilder()
            .withNewMetadata().withName("cm1").withNamespace("ns1").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(1);
    assertThat(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace()).isEqualTo("ns1");
  }

  @Test
  public void namespaceSetInResourceGetsOverwrittenWhenForceEnabled() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.force", "true");
    setExpectations(properties, ResourceConfig.builder().namespace("mynamespace-configured").build());
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ConfigMapBuilder()
            .withNewMetadata().withName("cm1").withNamespace("ns1").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(1);
    assertThat(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace()).isEqualTo("mynamespace-configured");
  }

  private void setNamespaceInResourceConfig(String namespace) {
    ResourceConfig resourceConfig = ResourceConfig.builder().namespace(namespace).build();
    setExpectations(new Properties(), resourceConfig);
  }

  private KubernetesListBuilder getKubernetesListBuilder() {
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ServiceBuilder()
            .withNewMetadata().withName("myservice1").endMetadata()
            .build());
    kubernetesListBuilder.addToItems(new DeploymentBuilder()
            .withNewMetadata().withName("mydeployment1").endMetadata()
            .build());
    return kubernetesListBuilder;
  }
}
