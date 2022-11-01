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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultNamespaceEnricherTest {

  private EnricherContext context;

  private void setExpectations(Properties properties, ResourceConfig resourceConfig) {
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
  void noNameShouldReturnEmpty() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).isEmpty();
  }

  @Test
  void create_withPropertiesAndUnknownTypeInKubernetes_shouldAddNothing() {
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
  void create_withPropertiesInKubernetes_shouldAddNamespace() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .isInstanceOf(Namespace.class)
        .hasFieldOrPropertyWithValue("metadata.name", "example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  void create_withPropertiesInOpenShift_shouldAddProject() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .isInstanceOf(Project.class)
        .hasFieldOrPropertyWithValue("metadata.name", "example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  void create_withPropertiesAndConfigInKubernetes_shouldAddConfigNamespace() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", "config-example");
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).singleElement()
        .isInstanceOf(Namespace.class)
        .hasFieldOrPropertyWithValue("metadata.name", "config-example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  void enrich_withPropertiesInKubernetes_shouldAddNamespaceWithStatus() {
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
    assertThat(klb.build().getItems()).hasSize(2)
        .last()
        .hasFieldOrPropertyWithValue("metadata.namespace", "example");
  }

  @Test
  void enrich_withPropertiesInKubernetes_shouldAddProjectWithStatus() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new ProjectBuilder()
            .withNewMetadata().withName("name").endMetadata()
            .withNewStatus().withPhase("active").endStatus().build());
    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("metadata.namespace", null);
  }

  @Test
  void enrich_withNamespaceFragmentWithNoStatus() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new NamespaceBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("metadata.name", "test-jkube")
        .hasFieldOrPropertyWithValue("metadata.namespace", null);
  }

  @Test
  void enrich_withOpenShiftProjectFragmentWithNoStatus() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ProjectBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("metadata.name", "test-jkube")
        .hasFieldOrPropertyWithValue("metadata.namespace", null);
  }

  @Test
  void noNamespaceConfiguredInMetadataIfNoPropertyProvided() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems())
        .hasSize(2)
        .extracting("metadata.namespace")
        .containsExactly(null, null);
  }

  @Test
  void namespaceSetInResourceConfig_shouldConfigureNamespaceInMetadataOnPlatformKubernetes() {
    // Given
    setNamespaceInResourceConfig("mynamespace-configured");
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems())
        .hasSize(2)
        .extracting("metadata.namespace")
        .containsExactly("mynamespace-configured", "mynamespace-configured");
  }

  @Test
  void namespaceSetInResourceConfig_shouldConfigureNamespaceInMetadataOnPlatformOpenShift() {
    // Given
    setNamespaceInResourceConfig("mynamespace-configured");
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems())
        .hasSize(2)
        .extracting("metadata.namespace")
        .containsExactly("mynamespace-configured", "mynamespace-configured");
  }

  @Test
  void namespaceSetInResource_shouldNotBeOverwritten() {
    // Given
    setNamespaceInResourceConfig("mynamespace-configured");
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ConfigMapBuilder()
            .withNewMetadata().withName("cm1").withNamespace("ns1").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).singleElement()
        .hasFieldOrPropertyWithValue("metadata.namespace", "ns1");
  }

  @Test
  void namespaceSetInResource_getsOverwrittenWhenForceEnabled() {
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
    assertThat(kubernetesListBuilder.build().getItems()).singleElement()
        .hasFieldOrPropertyWithValue("metadata.namespace", "mynamespace-configured");
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
