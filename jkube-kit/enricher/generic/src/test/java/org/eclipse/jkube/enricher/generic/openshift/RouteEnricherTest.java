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

import java.util.Properties;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.assertj.core.api.Condition;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class RouteEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    private Properties properties;
    private ProcessorConfig processorConfig;
    private KubernetesListBuilder klb;

    @Before
    public void setUp() {
        properties = new Properties();
        processorConfig = new ProcessorConfig();
        klb = new KubernetesListBuilder();
        // @formatter:off
        klb.addToItems(new ServiceBuilder()
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
                .endSpec()
            .build());
        new Expectations() {{
            context.getProperties(); result = properties;
            context.getConfiguration().getProcessorConfig(); result = processorConfig;
        }};
        // @formatter:on
    }

    @Test
    public void testCreateWithDefaultsInOpenShiftShouldAddRoute() {
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(2)
            .extracting("kind")
            .containsExactly("Service", "Route");
        assertThat(klb.build().getItems().get(1))
            .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
            .contains("test-svc", "test-svc", "Service", "test-svc", 8080);
    }

    @Test
    public void testCreateWithDefaultsInKubernetesShouldNotAddRoute() {
        // Given
        // @formatter:off
        new Expectations() {{
            context.getProperties(); minTimes = 0;
            context.getConfiguration().getProcessorConfig(); minTimes = 0;
        }};
        // @formatter:on
        // When
        new RouteEnricher(context).create(PlatformMode.kubernetes, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(1)
            .extracting("kind")
            .containsExactly("Service");
    }

    @Test
    public void testCreateWithGenerateExtraPropertyInOpenShiftShouldNotAddRoute() {
        // Given
        properties.put("jkube.openshift.generateRoute", "false");
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(1)
            .extracting("kind")
            .containsExactly("Service");
    }

    @Test
    public void testCreateWithGenerateEnricherPropertyInOpenShiftShouldNotAddRoute() {
        // Given
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
    public void testCreateWithDefaultsAndRouteDomainInOpenShiftShouldAddRouteWithDomainPostfix() {
        // Given
        // @formatter:off
        new Expectations() {{
            context.getProperties(); minTimes = 0;
            context.getConfiguration().getResource().getRouteDomain(); result = "jkube.eclipse.org";
        }};
        // @formatter:on
        // When
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
            .hasSize(2)
            .extracting("kind")
            .containsExactly("Service", "Route");
        assertThat(klb.build().getItems().get(1))
            .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
            .contains("test-svc", "test-svc.jkube.eclipse.org", "Service", "test-svc", 8080);
    }

    @Test
    public void testCreateWithDefaultsAndExistingRouteWithMatchingNameInBuilderInOpenShiftShouldReuseExistingRoute() {
        // Given
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
            .extracting("kind")
            .containsExactly("Service", "Route");
        assertThat(klb.build().getItems().get(1))
            .extracting("metadata.name", "spec.host", "spec.to.kind", "spec.to.name", "spec.port.targetPort.intVal")
            .contains("test-svc", "example.com", null, null, 1337);
    }

    @Test
    public void testEnrichNoTls(){
        // Given
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        final Condition<Object> doesNotExist = new Condition<Object>("Does not exist") {
            @Override
            public boolean matches(Object value) {
                return value == null;
            }
        };
        // When
        new RouteEnricher(context).enrich(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
                .hasSize(2)
                .extracting("kind")
                .containsExactly("Service", "Route");

        assertThat(klb.build().getItems().stream().filter(h -> h.getKind().equals("Route")).findFirst().get())
                .extracting("spec.tls")
                .containsNull();

    }

    @Test
    public void testEnrichWithTls(){
        // Given
        klb.addToItems(new RouteBuilder()
                .editOrNewSpec()
                    .editOrNewTls()
                        .withInsecureEdgeTerminationPolicy("passthrough")
                        .withTermination("Edge")
                    .endTls()
                .endSpec()
                .build());
        new RouteEnricher(context).create(PlatformMode.openshift, klb);
        // When
        new RouteEnricher(context).enrich(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems())
                .hasSize(2)
                .extracting("kind")
                .containsExactly("Service", "Route");

        assertThat(klb.build().getItems().stream().filter(h -> h.getKind().equals("Route")).findFirst().get())
                .extracting("spec.tls.insecureEdgeTerminationPolicy", "spec.tls.termination")
                .contains("passthrough","Edge");
    }
}
