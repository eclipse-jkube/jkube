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
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
  void createWithPropertiesAndUnknownTypeInKubernetesShouldAddNothing() {
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

  @DisplayName("create namespace")
  @ParameterizedTest(name = "with properties {0}")
  @MethodSource("create_namespaceTestData")
  void create_namespace(String description, String namespace, PlatformMode platformMode, Class<? extends HasMetadata> clazz,
      String expectedName) {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-namespace.namespace", namespace);
    setExpectations(properties, new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(platformMode, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .isInstanceOf(clazz)
        .hasFieldOrPropertyWithValue("metadata.name", expectedName)
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  static Stream<Arguments> create_namespaceTestData() {
    return Stream.of(
        arguments("in kubernetes should add namespace", "example", PlatformMode.kubernetes, Namespace.class, "example"),
        arguments("in openshift should add project", "example", PlatformMode.openshift, Project.class, "example"),
        arguments("and config in kubernetes should add config namespace", "config-example", PlatformMode.kubernetes, Namespace.class, "config-example")
    );
  }

  @Test
  void enrichWithPropertiesInKubernetesShouldAddNamespaceWithStatus() {
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

  @DisplayName("enrich with")
  @ParameterizedTest(name = "{0}")
  @MethodSource("enrichWithTestData")
  void enrich(String description, HasMetadata item, PlatformMode platformMode, String expectedNamespace, String expectedName) {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(item);
    // When
    new DefaultNamespaceEnricher(context).enrich(platformMode, klb);
    // Then
    assertThat(klb.build().getItems()).singleElement()
        .hasFieldOrPropertyWithValue("metadata.namespace", expectedNamespace)
        .hasFieldOrPropertyWithValue("metadata.name", expectedName);
  }

  static Stream<Arguments> enrichWithTestData() {
    return Stream.of(
        arguments("properties in kubernetes should add project with status", new ProjectBuilder()
            .withNewMetadata().withName("name").endMetadata().withNewStatus().withPhase("active").endStatus()
            .build(), PlatformMode.openshift, null, "name"),
        arguments("namespace fragment with no status", new NamespaceBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build(), PlatformMode.kubernetes, null, "test-jkube"),
        arguments("openshift project fragment with no status", new ProjectBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build(), PlatformMode.openshift, null, "test-jkube")
    );
  }

  @Test
  void noNamespaceConfiguredInMetadataIfNoPropertyProvided() {
    // Given
    setExpectations(new Properties(), new ResourceConfig());
    final KubernetesListBuilder kubernetesListBuilder = getKubernetesListBuilder();

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(2)
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
  void namespaceSetInResourceShouldNotBeOverwritten() {
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
  void namespaceSetInResourceGetsOverwrittenWhenForceEnabled() {
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
