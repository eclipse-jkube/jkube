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

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.CREATE_EXTERNAL_URLS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IngressEnricherTest {
    @Mocked
    private JKubeEnricherContext context;

    @Mocked
    private KitLogger logger;

    @Test
    public void testCreate() {
        // Given
        new Expectations() {{
            // Enable creation of Ingress for Service of type LoadBalancer
            context.getProperty(CREATE_EXTERNAL_URLS);
            result = "true";
        }};

        Service providedService = getTestService().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService);
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        Ingress ingress = (Ingress) kubernetesListBuilder.buildLastItem();
        assertEquals(2, kubernetesListBuilder.buildItems().size());
        assertNotNull(ingress);
        assertEquals(providedService.getMetadata().getName(), ingress.getMetadata().getName());
        assertNotNull(ingress.getSpec());
        assertEquals(providedService.getMetadata().getName(), ingress.getSpec().getBackend().getServiceName());
        assertEquals(providedService.getSpec().getPorts().get(0).getTargetPort(), ingress.getSpec().getBackend().getServicePort());
    }

    @Test
    public void testAddIngress() {
        // Given
        ServiceBuilder testSvcBuilder = getTestService();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(testSvcBuilder);

        // When
        Ingress ingress = IngressEnricher.addIngress(kubernetesListBuilder, testSvcBuilder, "org.eclipse.jkube", logger);

        // Then
        assertNotNull(ingress);
        assertEquals(testSvcBuilder.buildMetadata().getName(), ingress.getMetadata().getName());
        assertEquals(1, ingress.getSpec().getRules().size());
        assertEquals(testSvcBuilder.buildMetadata().getName() + "." + "org.eclipse.jkube", ingress.getSpec().getRules().get(0).getHost());
    }

    private ServiceBuilder getTestService() {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName("test-svc")
                .addToLabels("expose", "true")
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withName("http")
                .withPort(8080)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(8080))
                .endPort()
                .addToSelector("group", "test")
                .withType("LoadBalancer")
                .endSpec();
    }
}
