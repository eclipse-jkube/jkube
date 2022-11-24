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
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteEnricherTest {
    private JKubeEnricherContext context;
    private static final String OPENSHIFT_GENERATE_ROUTE = "jkube.openshift.generateRoute";
    private static final String ROUTE_TLS_TERMINATION = "jkube.enricher.jkube-openshift-route.tlsTermination";
    private static final String EDGE_TERMINATION_POLICY = "jkube.enricher.jkube-openshift-route.tlsInsecureEdgeTerminationPolicy";
    private Properties properties;
    private ProcessorConfig processorConfig;
    private KubernetesListBuilder klb;

    @BeforeEach
    void setUpExpectations() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        properties = new Properties();
        processorConfig = new ProcessorConfig();
        klb = new KubernetesListBuilder();
    }

    @Test
    void create_withGenerateExtraPropertyInOpenShift_shouldNotAddRoute() {
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder().build());
        properties.put(OPENSHIFT_GENERATE_ROUTE, "false");
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(1)
            .extracting("kind")
            .containsExactly("Service");
    }

    @Test
    void create_withGenerateEnricherPropertyInOpenShift_shouldNotAddRoute() {
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder().build());
        properties.put("jkube.enricher.jkube-openshift-route.generateRoute", "false");
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(1)
            .extracting("kind")
            .containsExactly("Service");
    }

    @Test
    void create_withDefaultsAndRouteDomainInOpenShift_shouldAddRouteWithDomainPostfix() {
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder().build());
        mockJKubeEnricherContextPropertiesNotCalled();
        mockJKubeEnricherContextResourceConfigWithRouteDomain("jkube.eclipse.org");
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .satisfies(i -> assertThat(i)
                .extracting("kind")
                .containsExactly("Service", "Route")
            )
            .last()
            .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
            .contains("test-svc", "test-svc.jkube.eclipse.org", "Service", "test-svc", 8080);
    }

    @Test
    void create_withDefaultsAndExistingRouteWithMatchingNameInBuilderInOpenShift_shouldReuseExistingRoute() {
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder().build());
        klb.addToItems(new RouteBuilder()
            .editOrNewMetadata()
                .withName("test-svc")
            .endMetadata()
            .editOrNewSpec()
                .withHost("example.com")
                .editOrNewPort()
                    .withNewTargetPort(1337)
                .endPort()
            .endSpec()
            .build());
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(2)
            .satisfies(i -> assertThat(i)
                .extracting("kind")
                .containsExactly("Service", "Route")
            )
            .last()
            .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
            .contains("test-svc", "example.com", "Service", "test-svc", 1337);
    }

    @Test
    void mergeRoute_withRouteFragmentAtZeroIndex() {
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(new RouteBuilder()
                .editOrNewMetadata()
                .withName("test-svc")
                .endMetadata()
                .editOrNewSpec()
                .withNewTls()
                .withInsecureEdgeTerminationPolicy("Redirect")
                .withTermination("edge")
                .endTls()
                .withNewTo()
                .withKind("Service")
                .withName("jkube-app")
                .endTo()
                .endSpec()
                .build());
        klb.addToItems(getMockServiceBuilder().build());
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems()).hasSize(2)
            .satisfies(i -> assertThat(i)
                .extracting("kind")
                .containsExactly("Service", "Route")
            )
            .last()
            .extracting("metadata.name", "spec.to.kind", "spec.to.name", "spec.tls.insecureEdgeTerminationPolicy", "spec.tls.termination")
            .contains("test-svc", "Service", "test-svc", "Redirect", "edge");
    }

    @Test
    void create_opinionatedRouteFromService() {
        // Given
        ServiceBuilder serviceBuilder = getMockServiceBuilder();

        // When
        Route route = RouteEnricher.createOpinionatedRouteFromService(serviceBuilder, "example.com", "edge", "Allow", false);

        // Then
        assertThat(route).isNotNull()
            .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
            .contains("test-svc", "example.com", "Service", "test-svc", 8080);
    }

    @Test
    void create_opinionatedRouteFromServiceWithNullService() {
        // Given
        ServiceBuilder serviceBuilder = new ServiceBuilder();

        // When
        Route route = RouteEnricher.createOpinionatedRouteFromService(serviceBuilder, "example.com", "edge", "Allow", false);

        // Then
        assertThat(route).isNull();
    }

    @Test
    void mergeRoute_withEmptyFragment() {
        // Given
        Route opinionatedRoute = getMockOpinionatedRoute();
        Route fragmentRoute = new RouteBuilder().build();

        // When
        Route result = RouteEnricher.mergeRoute(fragmentRoute, opinionatedRoute);

        // Then
        assertThat(result).isNotNull()
            .isEqualTo(opinionatedRoute);
    }

    @Test
    void mergeRoute_withNonEmptyFragment() {
        // Given
        Route opinionatedRoute = getMockOpinionatedRoute();
        Route fragmentRoute = new RouteBuilder()
                .withNewSpec()
                .withNewTls()
                .withInsecureEdgeTerminationPolicy("Redirect")
                .withTermination("edge")
                .endTls()
                .endSpec()
                .build();

        // When
        Route result = RouteEnricher.mergeRoute(fragmentRoute, opinionatedRoute);

        // Then
        assertThat(result).isNotNull()
                .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name",
                        "spec.port.targetPort.intVal", "spec.tls.insecureEdgeTerminationPolicy", "spec.tls.termination")
                .contains("test-svc", "example.com", "Service", "test-svc",
                        8080, "Redirect", "edge");
    }

    @Test
    void enrichNoTls(){
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder().build());
        properties.put(OPENSHIFT_GENERATE_ROUTE, "true");
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
                .hasSize(2)
                .extracting("kind")
                .containsExactly("Service", "Route");

        assertThat(klb.build().getItems().stream().filter(h -> h.getKind().equals("Route")).findFirst().orElse(null))
                .extracting("spec.tls")
                .isNull();

    }

    @Test
    void enrichWithTls(){
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder().build());
        properties.put(ROUTE_TLS_TERMINATION, "edge");
        properties.put(EDGE_TERMINATION_POLICY, "Allow");
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
                .hasSize(2)
                .extracting("kind")
                .containsExactly("Service", "Route");

        assertThat(klb.build().getItems().stream().filter(h -> h.getKind().equals("Route")).findFirst().orElse(null))
                .extracting("spec.tls.insecureEdgeTerminationPolicy", "spec.tls.termination")
                .contains("Allow","edge");
    }

    @Test
    void routeTargetPortFromServicePort() {
        // Given
        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("test-svc")
                .addToLabels("expose", "true")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("http")
                .withPort(8080)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(8080))
                .endPort()
                .addToSelector("group", "test")
                .withType("LoadBalancer")
                .editMatchingPort(e -> Boolean.TRUE).withPort(80).endPort().endSpec();

        // When
        Route route = RouteEnricher.createOpinionatedRouteFromService(serviceBuilder, "example.com", "edge", "Allow", false);

        // Then
        assertThat(route).isNotNull()
                .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
                .contains("test-svc", "example.com", "Service", "test-svc", 80);
    }

    @Test
    void create_withNoExposeLabelPort8443_shouldCreateRoute() {
        // Given
        mockJKubeEnricherContext();
        klb.addToItems(getMockServiceBuilder(8443, Collections.emptyMap()).build());

        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);

        // Then
        assertThat(klb.build().getItems())
            .hasSize(2)
            .extracting("kind")
            .containsExactly("Service", "Route");
    }

    private ServiceBuilder getMockServiceBuilder() {
        return getMockServiceBuilder(8080, Collections.singletonMap("expose", "true"));
    }

    private ServiceBuilder getMockServiceBuilder(int port, Map<String, String> labels) {
        // @formatter:off
        return new ServiceBuilder()
                .editOrNewMetadata()
                .withName("test-svc")
                .withLabels(labels)
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("http")
                .withPort(port)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(port))
                .endPort()
                .addToSelector("group", "test")
                .withType("LoadBalancer")
                .endSpec();
        // @formatter:on
    }

    private Route getMockOpinionatedRoute() {
        // @formatter:off
        return new RouteBuilder()
                .withNewMetadata().withName("test-svc").endMetadata()
                .withNewSpec()
                .withNewPort()
                .withNewTargetPort().withValue(8080).endTargetPort()
                .endPort()
                .withHost("example.com")
                .withNewTo().withKind("Service").withName("test-svc").endTo()
                .withNewTls()
                .withInsecureEdgeTerminationPolicy("Redirect")
                .withTermination("edge")
                .endTls()
                .addNewAlternateBackend()
                .withKind("Service")
                .withName("test-svc-2")
                .withWeight(10)
                .endAlternateBackend()
                .endSpec()
                .build();
        // @formatter:on
    }

    private void mockJKubeEnricherContext() {
        when(context.getProperties()).thenReturn(properties);
        when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
    }

    private void mockJKubeEnricherContextPropertiesNotCalled() {
        when(context.getProperties()).thenReturn(properties);
    }

    private void mockJKubeEnricherContextResourceConfigWithRouteDomain(String routeDomain) {
        when(context.getConfiguration().getResource().getRouteDomain()).thenReturn(routeDomain);
    }
}

