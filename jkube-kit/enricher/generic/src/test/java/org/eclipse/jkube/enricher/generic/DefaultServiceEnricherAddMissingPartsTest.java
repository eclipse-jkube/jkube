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

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServiceEnricherAddMissingPartsTest {
  private Properties properties;
  private DefaultServiceEnricher enricher;

  @BeforeEach
  void setUp() {
    properties = new Properties();
    KitLogger logger = new KitLogger.SilentLogger();
    final JKubeEnricherContext context = JKubeEnricherContext.builder()
      .image(ImageConfiguration.builder()
        .name("test-image")
        .build(new BuildConfiguration())
        .build())
      .project(JavaProject.builder()
        .properties(properties)
        .groupId("group-id")
        .artifactId("artifact-id")
        .build())
      .log(logger)
      .summaryService(new SummaryService(new File("target"), logger, false))
      .build();
    enricher = new DefaultServiceEnricher(context);
  }

  @Test
  void defaultValuesAndEmptyOriginal() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder()
            .withMetadata(createDefaultFragmentMetadata()).build());
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.type", null)
        .extracting("spec.ports")
        .asList()
        .extracting("name", "port", "protocol")
        .containsOnly(new Tuple("http", 80, "TCP"));
  }

  @Test
  void defaultValuesAndOriginalWithPorts() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().withMetadata(createDefaultFragmentMetadata()).editOrNewSpec().addNewPort()
            .withProtocol("TCP").withPort(1337).endPort().endSpec().build());
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.type", null)
        .extracting("spec.ports")
        .asList()
        .extracting("name", "port", "protocol")
        .containsOnly(new Tuple("menandmice-dns", 1337, "TCP"));
  }

  @Test
  void configuredTypeAndOriginalWithNoType() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().withMetadata(createDefaultFragmentMetadata()).editOrNewSpec().withClusterIP("1.3.3.7").endSpec().build());
    properties.put("jkube.enricher.jkube-service.type", "NodePort");
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.clusterIP", "1.3.3.7")
        .hasFieldOrPropertyWithValue("spec.type", "NodePort");
  }

  @Test
  void configuredTypeAndOriginalWithType() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().withMetadata(createDefaultFragmentMetadata()).editOrNewSpec().withType("LoadBalancer").endSpec().build());
    properties.put("jkube.enricher.jkube-service.type", "NodePort");
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.type", "LoadBalancer");
  }

  @Test
  void configuredHeadlessAndOriginalWithNoClusterIP() {
    // Given
    properties.put("jkube.enricher.jkube-service.headless", "true");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().withMetadata(createDefaultFragmentMetadata()).editOrNewSpec().withType("LoadBalancer").endSpec().build());
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.clusterIP", "None");
  }

  @Test
  void configuredHeadlessAndOriginalWithClusterIP() {
    // Given
    properties.put("jkube.enricher.jkube-service.headless", "true");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().withMetadata(createDefaultFragmentMetadata()).editOrNewSpec().withClusterIP("1.3.3.7").endSpec().build());
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.clusterIP", "1.3.3.7");
  }

  private void imageConfigurationWithPort(String... ports) {
    enricher.getContext().getConfiguration().getImages().get(0)
      .setBuild(BuildConfiguration.builder().ports(Arrays.asList(ports)).build());
  }

  private ObjectMeta createDefaultFragmentMetadata() {
    return new ObjectMetaBuilder().withName("artifact-id").build();
  }
}
