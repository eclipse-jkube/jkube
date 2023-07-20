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
package org.eclipse.jkube.kit.enricher.specific;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrometheusEnricherTest {

  private JKubeEnricherContext context;
  private PrometheusEnricher prometheusEnricher;

  @BeforeEach
  void setUp() {
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    prometheusEnricher = new PrometheusEnricher(context);
  }

  private void initContext(ProcessorConfig config, ImageConfiguration imageConfiguration) {
    when(context.getConfiguration()).thenReturn(Configuration.builder()
        .processorConfig(config)
        .image(imageConfiguration)
        .build());
  }

  @Test
  void customPrometheusPort() {
    // Given
    initContext(new ProcessorConfig(
        null,
        null,
        Collections.singletonMap("jkube-prometheus", Collections.singletonMap("prometheusPort", "1234"))),
        null);
    final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(
        new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build()
    );
    // When
    prometheusEnricher.create(PlatformMode.kubernetes, builder);
    // Then
    assertThat(builder.buildFirstItem().getMetadata().getAnnotations())
        .hasSize(3)
        .containsEntry("prometheus.io/port", "1234")
        .containsEntry("prometheus.io/scrape", "true")
        .containsEntry("prometheus.io/path", "/metrics");
  }

  @Test
  void detectPrometheusPort() {
    // Given
    initContext(null,
        ImageConfiguration.builder().build(
            BuildConfiguration.builder()
                .ports(Arrays.asList("1337", null, " ", "9779", null))
                .build())
            .build());
    final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(
        new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build()
    );
    // When
    prometheusEnricher.create(PlatformMode.kubernetes, builder);
    // Then
    assertThat(builder.buildFirstItem().getMetadata().getAnnotations())
        .hasSize(3)
        .containsEntry("prometheus.io/port", "9779")
        .containsEntry("prometheus.io/scrape", "true")
        .containsEntry("prometheus.io/path", "/metrics");
  }

  @Test
  void noDefinedPrometheusPort() {
    // Given
    initContext(null,
        ImageConfiguration.builder().build(
            BuildConfiguration.builder()
                .ports(Collections.emptyList())
                .build())
            .build());
    final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(
        new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build()
    );
    // When
    prometheusEnricher.create(PlatformMode.kubernetes, builder);
    // Then
    assertThat(builder.buildFirstItem().getMetadata().getAnnotations()).isNullOrEmpty();
  }

  @Test
  void customPrometheusPath() {
    // Given
    initContext(new ProcessorConfig(
        null,
        null,
        Collections.singletonMap("jkube-prometheus", Collections.singletonMap("prometheusPath", "/prometheus"))),
        ImageConfiguration.builder().build(
            BuildConfiguration.builder()
                .ports(Arrays.asList("1337", null, " ", "9779", null))
                .build())
            .build());
    final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(
        new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build()
    );
    // When
    prometheusEnricher.create(PlatformMode.kubernetes, builder);
    // Then
    assertThat(builder.buildFirstItem().getMetadata().getAnnotations())
        .hasSize(3)
        .containsEntry("prometheus.io/port", "9779")
        .containsEntry("prometheus.io/scrape", "true")
        .containsEntry("prometheus.io/path", "/prometheus");
  }
}
