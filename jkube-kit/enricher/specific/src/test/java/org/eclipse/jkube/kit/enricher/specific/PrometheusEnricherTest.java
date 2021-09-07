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
package org.eclipse.jkube.kit.enricher.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class PrometheusEnricherTest {

  @Mocked
  private JKubeEnricherContext context;
  private PrometheusEnricher prometheusEnricher;

  @Before
  public void setUp() {
    prometheusEnricher = new PrometheusEnricher(context);
  }
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void initContext(ProcessorConfig config, ImageConfiguration imageConfiguration) {
    // @formatter:off
    new Expectations() {{
      context.getConfiguration(); result = Configuration.builder().processorConfig(config).image(imageConfiguration).build();
    }};
    // @formatter:on
  }

  @Test
  public void testCustomPrometheusPort() {
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
  public void testDetectPrometheusPort() {
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
  public void testNoDefinedPrometheusPort() {
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
    assertThat(builder.buildFirstItem().getMetadata().getAnnotations()).isNull();
  }

  @Test
  public void testCustomPrometheusPath() {
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
