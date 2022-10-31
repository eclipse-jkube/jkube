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

import java.util.Arrays;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
public class DefaultServiceEnricherAddMissingPartsTest {

  private Properties properties;
  private DefaultServiceEnricher enricher;

  @Before
  public void setUp() {
    properties = new Properties();
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
      .log(new KitLogger.SilentLogger())
      .build();
    enricher = new DefaultServiceEnricher(context);
  }

  @Test
  public void defaultValuesAndEmptyOriginal() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().build());
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
  public void defaultValuesAndOriginalWithPorts() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().editOrNewSpec().addNewPort()
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
  public void configuredTypeAndOriginalWithNoType() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().editOrNewSpec().withClusterIP("1.3.3.7").endSpec().build());
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
  public void configuredTypeAndOriginalWithType() {
    // Given
    imageConfigurationWithPort("80");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().editOrNewSpec().withType("LoadBalancer").endSpec().build());
    properties.put("jkube.enricher.jkube-service.type", "NodePort");
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.type", "LoadBalancer");
  }

  @Test
  public void configuredHeadlessAndOriginalWithNoClusterIP() {
    // Given
    properties.put("jkube.enricher.jkube-service.headless", "true");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().editOrNewSpec().withType("LoadBalancer").endSpec().build());
    // When
    enricher.create(null, klb);
    // Then
    assertThat(klb.buildItem(0))
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("spec.clusterIP", "None");
  }

  @Test
  public void configuredHeadlessAndOriginalWithClusterIP() {
    // Given
    properties.put("jkube.enricher.jkube-service.headless", "true");
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(
        new ServiceBuilder().editOrNewSpec().withClusterIP("1.3.3.7").endSpec().build());
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
}
