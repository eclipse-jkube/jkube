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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Route;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RouteEnricherTest {
    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testCreate() {
        // Given
        Service providedService = getTestService().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService);
        RouteEnricher routeEnricher = new RouteEnricher(context);

        // When
        routeEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

        // Then
        Route route = (Route) kubernetesListBuilder.buildLastItem();
        assertEquals(2, kubernetesListBuilder.buildItems().size());
        assertNotNull(route);
        assertEquals(providedService.getMetadata().getName(), route.getMetadata().getName());
        assertNotNull(route.getSpec());
        assertEquals("Service", route.getSpec().getTo().getKind());
        assertEquals("test-svc", route.getSpec().getTo().getName());
        assertEquals(8080, route.getSpec().getPort().getTargetPort().getIntVal().intValue());
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
