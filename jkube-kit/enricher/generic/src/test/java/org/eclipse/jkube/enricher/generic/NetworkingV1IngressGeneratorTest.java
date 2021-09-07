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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathResourceConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import mockit.Mocked;
import org.junit.Test;

public class NetworkingV1IngressGeneratorTest {
    @Mocked
    private KitLogger logger;

    @Test
    public void testGenerate() {
        // Given
        ServiceBuilder testSvcBuilder = initTestService();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(testSvcBuilder);

        // When
        Ingress ingress = NetworkingV1IngressGenerator.generate(kubernetesListBuilder, testSvcBuilder, "org.eclipse.jkube", null, Collections.emptyList(), Collections.emptyList(), logger);

        // Then
        assertThat(ingress)
                .isNotNull()
                .hasFieldOrPropertyWithValue("metadata.name", "test-svc")
                .extracting("spec.rules").asList()
                .hasSize(1).element(0)
                .hasFieldOrPropertyWithValue("host", "test-svc.org.eclipse.jkube");
        assertThat(ingress.getSpec()).isNotNull();
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath()).isEqualTo("/");
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPathType()).isEqualTo("Exact");
    }

    @Test
    public void testGenerateNoHostOrRouteDomainConfigured() {
        // Given
        ServiceBuilder testSvcBuilder = initTestService();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(testSvcBuilder);

        // When
        Ingress ingress = NetworkingV1IngressGenerator.generate(kubernetesListBuilder, testSvcBuilder, null, null, Collections.emptyList(), Collections.emptyList(), logger);

        // Then
        assertThat(ingress).isNotNull().hasFieldOrPropertyWithValue("metadata.name", "test-svc");
        assertThat(ingress.getSpec()).isNotNull();
        assertThat(ingress.getSpec().getDefaultBackend().getService().getName()).isEqualTo("test-svc");
        assertThat(ingress.getSpec().getDefaultBackend().getService().getPort().getNumber()).isEqualTo(8080);
    }

    @Test
    public void testGenerateWithXMLConfig() {
        // Given
        ServiceBuilder testSvcBuilder = initTestService();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(testSvcBuilder);
        IngressRuleConfig ingressRuleConfig = IngressRuleConfig.builder()
                .host("foo.bar.com")
                .path(IngressRulePathConfig.builder()
                        .path("/foo")
                        .pathType("Prefix")
                        .serviceName("test-svc")
                        .servicePort(8080)
                        .resource(IngressRulePathResourceConfig.builder()
                                .apiGroup("k8s.example.com")
                                .kind("StorageSets")
                                .name("icon-assets")
                                .build())
                        .build())
                .build();
        IngressTlsConfig ingressTlsConfig = IngressTlsConfig.builder()
                .host("foo.bar.com")
                .secretName("testsecret-tls")
                .build();

        // When
        Ingress ingress = NetworkingV1IngressGenerator.generate(kubernetesListBuilder, testSvcBuilder, "org.eclipse.jkube", null, Collections.singletonList(ingressRuleConfig), Collections.singletonList(ingressTlsConfig), logger);

        // Then
        assertThat(ingress)
                .hasFieldOrPropertyWithValue("metadata.name", "test-svc")
                .extracting("spec.rules").asList()
                .hasSize(1).element(0)
                .hasFieldOrPropertyWithValue("host", "foo.bar.com");
    }

    @Test
    public void testGenerateWithNullServiceMetadata() {
        // Given
        ServiceBuilder serviceBuilder = new ServiceBuilder().withMetadata(null);
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(serviceBuilder);

        // When
        Ingress ingress = NetworkingV1IngressGenerator.generate(kubernetesListBuilder, serviceBuilder, "org.eclipse.jkube", null, Collections.emptyList(), Collections.emptyList(), logger);

        // Then
        assertThat(ingress).isNull();
    }

    private ServiceBuilder initTestService() {
        return new ServiceBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("test-svc")
                        .addToLabels("expose", "true")
                        .build())
                .withSpec(new ServiceSpecBuilder()
                        .addNewPort()
                        .withName("http")
                        .withPort(8080)
                        .withProtocol("TCP")
                        .withTargetPort(new IntOrString(8080))
                        .endPort()
                        .addToSelector("group", "test")
                        .withType("LoadBalancer")
                        .build()
                );
    }
}
