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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class IngressEnricherBehavioralTest {

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
  public void create_withNoServices_shouldNotCreateIngress() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().isEmpty();
  }

  @Test
  public void create_withServicesAndNoExternalUrls_shouldNotCreateIngress() {
    // Given
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().singleElement()
      .isInstanceOf(Service.class);
  }

  @Test
  public void create_withServiceAndMatchingNetworkingIngress_shouldNotCreateIngress() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    klb.addToItems(new IngressBuilder().withNewMetadata().withName("http").endMetadata().build());
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .element(1)
      .hasFieldOrPropertyWithValue("apiVersion", "networking.k8s.io/v1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec", null);
  }

  @Test
  public void create_withServiceAndMatchingExtensionsIngress_shouldNotCreateIngress() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    klb.addToItems(new io.fabric8.kubernetes.api.model.extensions.IngressBuilder()
      .withNewMetadata().withName("http").endMetadata().build());
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .element(1)
      .hasFieldOrPropertyWithValue("apiVersion", "extensions/v1beta1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec", null);
  }

  @Test
  public void create_withServiceNotExposed_shouldNotCreateIngress() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    klb.addNewServiceItem().withNewMetadata().addToLabels("expose", "false").withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build()).extracting(KubernetesList::getItems).asList().singleElement()
      .isInstanceOf(Service.class);
  }

  @Test
  public void create_withServices_shouldCreateNetworkingIngress() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .element(1)
      .hasFieldOrPropertyWithValue("apiVersion", "networking.k8s.io/v1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec.defaultBackend.service.name", "http")
      .hasFieldOrPropertyWithValue("spec.defaultBackend.service.port.number", 80)
      .extracting("spec.rules").asList().isEmpty();
  }

  @Test
  public void create_withServicesAndTargetExtensions_shouldCreateExtensionsIngress() {
    // Given
    context.getProject().getProperties().put("jkube.createExternalUrls", "true");
    context.getProject().getProperties().put("jkube.enricher.jkube-ingress.targetApiVersion", "extensions/v1beta1");
    klb.addNewServiceItem().withNewMetadata().withName("http").endMetadata()
      .withNewSpec().addNewPort().withPort(80).endPort().endSpec().endServiceItem();
    // When
    new IngressEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).hasSize(2)
      .element(1)
      .hasFieldOrPropertyWithValue("apiVersion", "extensions/v1beta1")
      .hasFieldOrPropertyWithValue("metadata.name", "http")
      .hasFieldOrPropertyWithValue("spec.backend.serviceName", "http")
      .hasFieldOrPropertyWithValue("spec.backend.servicePort.value", 80)
      .extracting("spec.rules").asList().isEmpty();
  }
}
