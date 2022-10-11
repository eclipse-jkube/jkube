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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class RouteEnricherBehavioralTest {

  private JKubeEnricherContext context;
  private KubernetesListBuilder klb;

  @Before
  public void setUp() throws Exception {
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
  public void create_withNoServices_shouldNotCreateRoute() {
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().isEmpty();
  }

  @Test
  public void create_withServicesInKubernetes_shouldNotCreateRoute() {
    // Given
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new RouteEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().singleElement()
      .isInstanceOf(Service.class);
  }

  @Test
  public void create_withServiceNotExposed_shouldNotCreateRoute() {
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
  public void create_withServiceWithNowWebPort_shouldNotCreateRoute() {
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
  public void create_withServiceWithNowWebPortAndCreateExternalUrls_shouldCreateRoute() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(21).endPort().endSpec().endServiceItem();
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .element(1)
      .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 21)
      .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
      .hasFieldOrPropertyWithValue("spec.to.name", "http");
  }

  @Test
  public void create_withServices_shouldCreateRoute() {
    // Given
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new RouteEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .element(1)
      .hasFieldOrPropertyWithValue("apiVersion", "route.openshift.io/v1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec.port.targetPort.value", 80)
      .hasFieldOrPropertyWithValue("spec.to.kind", "Service")
      .hasFieldOrPropertyWithValue("spec.to.name", "http");
  }
}
