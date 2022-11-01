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

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMetadataEnricherTest {

  private DefaultMetadataEnricher defaultMetadataEnricher;
  private ConfigMapBuilder configMap;
  private DeploymentBuilder deployment;
  private GenericKubernetesResourceBuilder genericResource;
  private io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder ingressV1;
  private io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder ingressV1beta1;
  private ServiceAccountBuilder serviceAccount;
  private RouteBuilder route;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() throws Exception {
    JKubeEnricherContext buildContext = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    Configuration configuration = Configuration.builder()
        .resource(ResourceConfig.builder()
            .annotations(MetaDataConfig.builder()
                .all(properties("all-annotation", 1))
                .deployment(properties("deployment", "Deployment"))
                .ingress(properties("ingress", "Ingress"))
                .serviceAccount(properties("service-account", "ServiceAccount"))
                .route(properties("route", "Route"))
                .build())
            .labels(MetaDataConfig.builder()
                .all(properties("all-label", 10L))
                .deployment(properties("deployment-label", "Deployment"))
                .ingress(properties("ingress-label", "Ingress"))
                .serviceAccount(properties("service-account-label", "ServiceAccount"))
                .route(properties("route-label", "Route"))
                .build())
            .build())
        .build();
    when(buildContext.getConfiguration()).thenReturn(configuration);
    defaultMetadataEnricher = new DefaultMetadataEnricher(buildContext);
    configMap = new ConfigMapBuilder().withNewMetadata().endMetadata();
    deployment = new DeploymentBuilder();
    genericResource = new GenericKubernetesResourceBuilder().withNewMetadata().endMetadata();
    ingressV1 = new io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder();
    ingressV1beta1 = new io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder();
    serviceAccount = new ServiceAccountBuilder();
    route = new RouteBuilder();
    klb = new KubernetesListBuilder().addToItems(configMap).addToItems(deployment)
        .addToItems(genericResource).addToItems(ingressV1).addToItems(ingressV1beta1)
        .addToItems(serviceAccount).addToItems(route);

  }

  @Test
  void configMap() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(configMap.build().getMetadata())
        .satisfies(m -> assertThat(m.getAnnotations())
            .containsOnly(entry("all-annotation", "1"))
        )
        .satisfies(m -> assertThat(m.getLabels())
            .containsOnly(entry("all-label", "10"))
        );
  }

  @Test
  void deployment() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(deployment.build().getMetadata())
        .satisfies(d -> assertThat(d.getAnnotations())
            .containsOnly(entry("all-annotation", "1"), entry("deployment", "Deployment"))
        )
        .satisfies(d -> assertThat(d.getLabels())
            .containsOnly(entry("all-label", "10"), entry("deployment-label", "Deployment"))
        );
  }

  @Test
  void genericResource() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(genericResource.build().getMetadata())
        .satisfies(r -> assertThat(r.getAnnotations())
            .containsOnly(entry("all-annotation", "1"))
        )
        .satisfies(r -> assertThat(r.getLabels()).containsOnly(entry("all-label", "10"))
        );
  }

  @Test
  void ingressV1() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(ingressV1.build().getMetadata())
        .satisfies(ir -> assertThat(ir.getAnnotations())
            .containsOnly(entry("all-annotation", "1"), entry("ingress", "Ingress"))
        )
        .satisfies(ir -> assertThat(ir.getLabels())
            .containsOnly(entry("all-label", "10"), entry("ingress-label", "Ingress"))
        );
  }

  @Test
  void ingressV1beta1() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(ingressV1beta1.build().getMetadata())
        .satisfies(ir -> assertThat(ir.getAnnotations())
            .containsOnly(entry("all-annotation", "1"), entry("ingress", "Ingress"))
        )
        .satisfies(ir -> assertThat(ir.getLabels())
            .containsOnly(entry("all-label", "10"), entry("ingress-label", "Ingress"))
        );
  }

  @Test
  void serviceAccount() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(serviceAccount.build().getMetadata())
        .satisfies(a -> assertThat(a.getAnnotations())
            .containsOnly(entry("all-annotation", "1"), entry("service-account", "ServiceAccount"))
        )
        .satisfies(a -> assertThat(a.getLabels())
            .containsOnly(entry("all-label", "10"), entry("service-account-label", "ServiceAccount"))
        );
  }

  @Test
  void route() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(route.build().getMetadata())
        .satisfies(r -> assertThat(r.getAnnotations())
            .containsOnly(entry("all-annotation", "1"), entry("route", "Route"))
        )
        .satisfies(r -> assertThat(r.getLabels())
            .containsOnly(entry("all-label", "10"), entry("route-label", "Route"))
        );
  }

  private static Properties properties(Object... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Expected an even number of key-values");
    }
    final Properties ret = new Properties();
    for (int it = 0; it < keyValuePairs.length; it++) {
      ret.put(keyValuePairs[it], keyValuePairs[++it]);
    }
    return ret;
  }
}