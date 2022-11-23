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
package org.eclipse.jkube.enricher.generic.ingress;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpec;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathResourceConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NetworkingV1IngressGeneratorTest {

    @Test
    void generate() {
        // Given
        ServiceBuilder testSvcBuilder = initTestService();

        // When
        Ingress ingress = NetworkingV1IngressGenerator.generate(testSvcBuilder, "org.eclipse.jkube", null, Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(ingress).isNotNull()
            .hasFieldOrPropertyWithValue("metadata.name", "test-svc")
            .extracting(Ingress::getSpec).isNotNull()
            .extracting(IngressSpec::getRules).asList().singleElement()
            .hasFieldOrPropertyWithValue("host", "test-svc.org.eclipse.jkube")
            .extracting("http.paths").asList().singleElement()
            .hasFieldOrPropertyWithValue("path", "/")
            .hasFieldOrPropertyWithValue("pathType", "ImplementationSpecific");
    }

    @Test
    void generate_withNoHostOrRouteDomainConfigured() {
        // Given
        ServiceBuilder testSvcBuilder = initTestService();

        // When
        Ingress ingress = NetworkingV1IngressGenerator.generate(testSvcBuilder, null, null, Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(ingress).isNotNull()
            .hasFieldOrPropertyWithValue("metadata.name", "test-svc")
            .extracting("spec").isNotNull()
            .hasFieldOrPropertyWithValue("defaultBackend.service.name", "test-svc")
            .hasFieldOrPropertyWithValue("defaultBackend.service.port.number", 8080);
    }

    @Test
    void generate_withXMLConfig() {
        // Given
        ServiceBuilder testSvcBuilder = initTestService();
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
        Ingress ingress = NetworkingV1IngressGenerator.generate(testSvcBuilder, "org.eclipse.jkube", null, Collections.singletonList(ingressRuleConfig), Collections.singletonList(ingressTlsConfig));

        // Then
        assertThat(ingress)
            .hasFieldOrPropertyWithValue("metadata.name", "test-svc")
            .extracting("spec.rules").asList()
            .singleElement()
            .hasFieldOrPropertyWithValue("host", "foo.bar.com");
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
