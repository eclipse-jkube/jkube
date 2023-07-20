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
package org.eclipse.jkube.kit.enricher.api;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceExposerTest {

  @Test
  void hasWebPorts_withWebPorts() {
    // When
    final boolean result = new ServiceExposerForTest()
      .hasWebPorts(new ServiceBuilder().withNewSpec()
        .addNewPort().withPort(21).endPort()
        .addNewPort().withPort(22).endPort()
        .addNewPort().withPort(80).endPort().endSpec());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasWebPorts_withNoWebPorts() {
    // When
    final boolean result = new ServiceExposerForTest()
      .hasWebPorts(new ServiceBuilder().withNewSpec()
        .addNewPort().withPort(21).endPort()
        .addNewPort().withPort(22).endPort()
        .addNewPort().withPort(9082).endPort().endSpec());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void canExposeService_withMultiplePorts() {
    // When
    final boolean result = new ServiceExposerForTest()
      .canExposeService(new ServiceBuilder().withNewSpec()
        .addNewPort().withPort(21).endPort()
        .addNewPort().withPort(22).endPort()
        .addNewPort().withPort(80).endPort().endSpec());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void canExposeService_withSinglePorts() {
    // When
    final boolean result = new ServiceExposerForTest()
      .canExposeService(new ServiceBuilder().withNewSpec()
        .addNewPort().withPort(21).endPort().endSpec());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void canExposeService_withSinglePortsAndKubernetesName() {
    // When
    final boolean result = new ServiceExposerForTest()
      .canExposeService(new ServiceBuilder()
        .withNewMetadata().withName("kubernetes").endMetadata()
        .withNewSpec().addNewPort().withPort(80).endPort().endSpec());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void canExposeService_withSinglePortsAndExposeFalse() {
    // When
    final boolean result = new ServiceExposerForTest()
      .canExposeService(new ServiceBuilder()
        .withNewMetadata().addToLabels("expose", "false").endMetadata()
        .withNewSpec().addNewPort().withPort(80).endPort().endSpec());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isExposeWithLabel_withExposeTrue() {
    // When
    final boolean result = new ServiceExposerForTest().isExposedWithLabel(new ServiceBuilder()
        .withNewMetadata().addToLabels("expose", "true").endMetadata());
    // Then
    assertThat(result).isTrue();
  }

  private static final class ServiceExposerForTest extends BaseEnricher implements ServiceExposer {

    public ServiceExposerForTest() {
      super(JKubeEnricherContext.builder()
        .log(new KitLogger.SilentLogger())
        .project(JavaProject.builder().build())
        .build(), "ServiceExposerForTest");
    }
  }
}
