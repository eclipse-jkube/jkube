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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import mockit.Expectations;
import mockit.Mocked;

import org.junit.Before;
import org.junit.Test;

import javax.validation.constraints.Null;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ServiceDiscoveryEnricherTest {

    // configured properties
    private static final String path = "/rest-3scale";
    private static final String port = "8080";
    private static final String scheme = "http";
    private static final String descriptionPath = "/api-doc";
    private static final String discoverable = "true";
    private static final String discoveryVersion = "v1337";

    @SuppressWarnings("unused")
    @Mocked
    private JKubeEnricherContext context;

    private ServiceDiscoveryEnricher enricher;

    private ServiceBuilder builder;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setUp() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("path", path);
        configMap.put("port", port);
        configMap.put("scheme", scheme);
        configMap.put("descriptionPath", descriptionPath);
        configMap.put("discoverable", discoverable);
        configMap.put("discoveryVersion", discoveryVersion);

        ProcessorConfig processorConfig = new ProcessorConfig(
            null, null, Collections.singletonMap("jkube-service-discovery", configMap)
        );

        // Setup mock behaviour
        new Expectations() {{
            context.getConfiguration().getProcessorConfig(); result = processorConfig;
        }};

        enricher = new ServiceDiscoveryEnricher(context);
        builder = new ServiceBuilder();
    }

    @Test
    public void testDiscoveryLabel() {
        enricher.addAnnotations(builder);
        String value = builder.buildMetadata().getLabels().get("discovery.3scale.net");
        assertEquals(discoverable, value);
    }

    @Test
    public void testDescriptionPathAnnotation() {
        enricher.addAnnotations(builder);
        assertAnnotation(descriptionPath, "description-path");
    }

    @Test
    public void testDiscoveryVersionAnnotation() {
        enricher.addAnnotations(builder);
        assertAnnotation(discoveryVersion, "discovery-version");
    }

    @Test
    public void testDefaultDiscoveryVersionAnnotation() {
        context.getConfiguration().getProcessorConfig().getConfig().get("jkube-service-discovery").remove("discoveryVersion");
        enricher.addAnnotations(builder);
        assertAnnotation("v1", "discovery-version");
    }

    @Test
    public void testPathAnnotation() {
        enricher.addAnnotations(builder);
        assertAnnotation("/rest-3scale", "path");
    }

    @Test
    public void testPortAnnotation() {
        enricher.addAnnotations(builder);
        assertAnnotation(port, "port");
    }

    @Test
    public void testSchemeAnnotation() {
        enricher.addAnnotations(builder);
        assertAnnotation(scheme, "scheme");
    }

    @Test
    public void testServiceWithNullPort() {
        // Given
        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .withNewMetadata().withName("test-svc").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withName("foo")
                .endPort()
                .endSpec();

        // When
        NullPointerException npe = assertThrows(NullPointerException.class, () -> enricher.addAnnotations(serviceBuilder));

        // Then
        assertEquals("Service test-svc .spec.ports[0].port: required value", npe.getMessage());
    }

    private void assertAnnotation(String expectedValue, String annotation) {
        assertEquals(
            expectedValue,
            builder.buildMetadata().getAnnotations().get("discovery.3scale.net/" + annotation)
        );
    }

}