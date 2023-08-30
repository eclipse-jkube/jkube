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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class RouteEnricherBehavioralTest {

  private JKubeEnricherContext context;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    context = JKubeEnricherContext.builder()
      .project(JavaProject.builder()
        .properties(new Properties())
        .build())
      .resources(ResourceConfig.builder().build())
      .log(new KitLogger.SilentLogger())
      .build();
    klb = new KubernetesListBuilder();
  }

  @Test
  void create_withNoServices_shouldNotCreateRoute() {
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().isEmpty();
  }

  @Test
  void create_withServiceNotExposed_shouldNotCreateRoute() {
    // Given
    klb.addNewServiceItem().withNewMetadata().addToLabels("expose", "false").withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().singleElement()
      .isInstanceOf(Service.class);
  }

  @Test
  void create_withServiceWithNoWebPort_shouldNotCreateRoute() {
    // Given
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(21).endPort().endSpec().endServiceItem();
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().singleElement()
      .isInstanceOf(Service.class);
  }

  @Test
  void create_withServiceWithNoWebPortAndCreateExternalUrls_shouldCreateRoute() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(21).endPort().endSpec().endServiceItem();
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .last()
      .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 21)
      .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
      .hasFieldOrPropertyWithValue("spec.to.name", "http");
  }

  @Nested
  @DisplayName("With Service with Web Port")
  class ServiceWithWebPort {

    @BeforeEach
    void setUp() {
      klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
        .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    }

    @Test
    @DisplayName("in kubernetes, should not create route")
    void inKubernetes_shouldNotCreateRoute() {
      // When
      new RouteEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertThat(klb.build()).extracting(KubernetesList::getItems).asList().singleElement()
        .isInstanceOf(Service.class);
    }

    @Test
    @DisplayName("in openshift, should create route")
    void inOpenShift_shouldCreateRoute() {
      // When
      new RouteEnricher(context).create(PlatformMode.openshift, klb);
      // Then
      assertThat(klb.buildItems()).hasSize(2)
        .last()
        .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
        .hasFieldOrPropertyWithValue("metadata.name", "http")
        .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 80)
        .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
        .hasFieldOrPropertyWithValue("spec.to.name", "http");
    }

    @Test
    void withFragmentWithNoName_shouldMergeRoute() {
      // Given
      klb.addToItems(new RouteBuilder()
        .withNewMetadata().addToAnnotations("jkube.eclipse.dev/fragment", "true").endMetadata()
        .build());
      // When
      new RouteEnricher(context).create(PlatformMode.openshift, klb);
      // Then
      assertThat(klb.buildItems()).hasSize(2)
        .last()
        .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
        .hasFieldOrPropertyWithValue("metadata.name", "http")
        .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 80)
        .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
        .hasFieldOrPropertyWithValue("spec.to.name", "http")
        .extracting("metadata.annotations")
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
        .containsEntry("jkube.eclipse.dev/fragment", "true");
    }

    @Test
    void withFragmentWithMatchingName_shouldMergeRoute() {
      // Given
      klb.addToItems(new RouteBuilder()
        .withNewMetadata().addToAnnotations("jkube.eclipse.dev/fragment", "true")
        .withName("http")
        .endMetadata()
        .build());
      // When
      new RouteEnricher(context).create(PlatformMode.openshift, klb);
      // Then
      assertThat(klb.buildItems()).hasSize(2)
        .last()
        .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
        .hasFieldOrPropertyWithValue("metadata.name", "http")
        .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 80)
        .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
        .hasFieldOrPropertyWithValue("spec.to.name", "http")
        .extracting("metadata.annotations")
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
        .containsEntry("jkube.eclipse.dev/fragment", "true");
    }

    @Test
    void withFragmentWithDifferentName_shouldCreateNewRoute() {
      // Given
      klb.addToItems(new RouteBuilder()
        .withNewMetadata().addToAnnotations("jkube.eclipse.dev/fragment", "true")
        .withName("not-http")
        .endMetadata()
        .build());
      // When
      new RouteEnricher(context).create(PlatformMode.openshift, klb);
      // Then
      assertThat(klb.buildItems()).hasSize(3)
        .last()
        .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
        .hasFieldOrPropertyWithValue("metadata.name", "http")
        .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 80)
        .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
        .hasFieldOrPropertyWithValue("spec.to.name", "http")
        .extracting("metadata.annotations")
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
        .doesNotContainEntry("jkube.eclipse.dev/fragment", "true");
    }
  }

}
