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

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ExtensionsV1Beta1IngressConverterTest {
    @Test
    void convert_withNullInput() {
        assertThat(ExtensionsV1beta1IngressConverter.convert(null)).isNull();
    }

    @Test
    void convert_withNullSpec() {
        // Given
        final io.fabric8.kubernetes.api.model.networking.v1.Ingress from = new IngressBuilder()
            .withNewMetadata().withName("ingress").endMetadata().build();
        // When
        final Ingress result = ExtensionsV1beta1IngressConverter.convert(from);
        // Then
        assertThat(result)
            .hasFieldOrPropertyWithValue("spec", null)
            .hasFieldOrPropertyWithValue("metadata.name", "ingress");
    }

    @Test
    void convert() {
        // Given
        io.fabric8.kubernetes.api.model.networking.v1.Ingress networkV1Ingress = new IngressBuilder()
                .withNewMetadata().withName("test-ing").endMetadata()
                .withNewSpec()
                .withIngressClassName("external-lb")
                .addNewTl()
                .withHosts("test-svc.org.eclipse.jkube")
                .withSecretName("test-jkube-ingress")
                .endTl()
                .addNewRule()
                .withHost("test-svc.org.eclipse.jkube")
                .withNewHttp()
                .addNewPath()
                .withPath("/testpath")
                .withPathType("Prefix")
                .withNewBackend()
                .withNewService()
                .withName("test")
                .withNewPort()
                .withNumber(80)
                .endPort()
                .endService()
                .withNewResource("k8s.example.com", "StorageBucket", "icon-assets")
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec()
                .build();

        // When
        Ingress ingress = ExtensionsV1beta1IngressConverter.convert(networkV1Ingress);

        // Then
        assertThat(ingress)
            .hasFieldOrPropertyWithValue("metadata.name", "test-ing")
            .hasFieldOrPropertyWithValue("spec.ingressClassName", "external-lb")
            .satisfies(ir -> assertThat(ir.getSpec().getTls())
                .asList().singleElement()
                .hasFieldOrPropertyWithValue("secretName", "test-jkube-ingress")
                .extracting("hosts").asList().singleElement()
                .isEqualTo("test-svc.org.eclipse.jkube"))
            .extracting("spec.rules").asList().singleElement()
            .hasFieldOrPropertyWithValue("host", "test-svc.org.eclipse.jkube")
            .extracting("http.paths").asList().first()
            .hasFieldOrPropertyWithValue("path", "/testpath")
            .hasFieldOrPropertyWithValue("pathType", "Prefix")
            .hasFieldOrPropertyWithValue("backend.serviceName", "test")
            .hasFieldOrPropertyWithValue("backend.servicePort.intVal", 80)
            .hasFieldOrPropertyWithValue("backend.resource.apiGroup", "k8s.example.com")
            .hasFieldOrPropertyWithValue("backend.resource.kind", "StorageBucket")
            .hasFieldOrPropertyWithValue("backend.resource.name", "icon-assets");
    }

    @Test
    void convert_withDefaultBackend() {
        // Given
        io.fabric8.kubernetes.api.model.networking.v1.Ingress networkV1Ingress = new IngressBuilder()
                .withNewMetadata().withName("test-jkube").endMetadata()
                .withNewSpec()
                .withNewDefaultBackend()
                .withNewService()
                .withName("test-jkube-ingress")
                .withNewPort().withNumber(8080).endPort()
                .endService()
                .endDefaultBackend()
                .endSpec()
                .build();

        // When
        Ingress ingress = ExtensionsV1beta1IngressConverter.convert(networkV1Ingress);

        // Then
        assertThat(ingress)
            .isNotNull()
            .hasFieldOrPropertyWithValue("spec.backend.serviceName", "test-jkube-ingress")
            .hasFieldOrPropertyWithValue("spec.backend.servicePort.intVal", 8080);
    }
}
