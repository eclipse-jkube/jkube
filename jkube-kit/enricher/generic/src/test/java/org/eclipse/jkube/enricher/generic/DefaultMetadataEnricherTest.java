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
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DefaultMetadataEnricherTest {

  @SuppressWarnings("unused")
  @Mocked
  private JKubeEnricherContext buildContext;

  private DefaultMetadataEnricher defaultMetadataEnricher;
  private ConfigMapBuilder configMap;
  private DeploymentBuilder deployment;
  private GenericKubernetesResourceBuilder genericResource;
  private io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder ingressV1;
  private io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder ingressV1beta1;
  private ServiceAccountBuilder serviceAccount;
  private RouteBuilder route;
  private KubernetesListBuilder klb;

  @Before
  public void setUp() throws Exception {
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
    // @formatter:off
    new Expectations() {{
      buildContext.getConfiguration(); result = configuration;
    }};
    // @formatter:on
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
  public void configMap() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(configMap.build().getMetadata().getAnnotations()).containsOnly(entry("all-annotation", "1"));
    assertThat(configMap.build().getMetadata().getLabels()).containsOnly(entry("all-label", "10"));
  }

  @Test
  public void deployment() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(deployment.build().getMetadata().getAnnotations())
        .containsOnly(entry("all-annotation", "1"), entry("deployment", "Deployment"));
    assertThat(deployment.build().getMetadata().getLabels())
        .containsOnly(entry("all-label", "10"), entry("deployment-label", "Deployment"));
  }

  @Test
  public void genericResource() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(genericResource.build().getMetadata().getAnnotations()).containsOnly(entry("all-annotation", "1"));
    assertThat(genericResource.build().getMetadata().getLabels()).containsOnly(entry("all-label", "10"));
  }

  @Test
  public void ingressV1() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(ingressV1.build().getMetadata().getAnnotations())
        .containsOnly(entry("all-annotation", "1"), entry("ingress", "Ingress"));
    assertThat(ingressV1.build().getMetadata().getLabels())
        .containsOnly(entry("all-label", "10"), entry("ingress-label", "Ingress"));
  }

  @Test
  public void ingressV1beta1() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(ingressV1beta1.build().getMetadata().getAnnotations())
        .containsOnly(entry("all-annotation", "1"), entry("ingress", "Ingress"));
    assertThat(ingressV1beta1.build().getMetadata().getLabels())
        .containsOnly(entry("all-label", "10"), entry("ingress-label", "Ingress"));
  }

  @Test
  public void serviceAccount() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(serviceAccount.build().getMetadata().getAnnotations())
        .containsOnly(entry("all-annotation", "1"), entry("service-account", "ServiceAccount"));
    assertThat(serviceAccount.build().getMetadata().getLabels())
        .containsOnly(entry("all-label", "10"), entry("service-account-label", "ServiceAccount"));
  }

  @Test
  public void route() {
    // When
    defaultMetadataEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(route.build().getMetadata().getAnnotations())
        .containsOnly(entry("all-annotation", "1"), entry("route", "Route"));
    assertThat(route.build().getMetadata().getLabels())
        .containsOnly(entry("all-label", "10"), entry("route-label", "Route"));
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