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

import static org.eclipse.jkube.kit.enricher.specific.ServiceDiscoveryEnricher.*;
import static org.junit.Assert.assertEquals;

public class ServiceDiscoveryEnricherTest {

    // configured properties
    private static final String path = "/rest-3scale";
    private static final String port = "8080";
    private static final String scheme = "http";
    private static final String descriptionPath = "/api-doc";
    private static final String discoverable = "true";
    private static final String discoveryVersion = "v1";

    @Mocked
    private JKubeEnricherContext context;

    private ServiceDiscoveryEnricher enricher;

    private ServiceBuilder builder;


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
            null, null, Collections.singletonMap(ENRICHER_NAME, configMap)
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
        String label = label();
        String value = builder.buildMetadata().getLabels().get(label);
        assertEquals(discoverable, value);
    }

    @Test
    public void testDescriptionPathAnnotation() {
        enricher.addAnnotations(builder);
        String annotation = annotation(DESCRIPTION_PATH);
        String value = builder.buildMetadata().getAnnotations().get(annotation);
        assertEquals(descriptionPath, value);
    }

    @Test
    public void testDiscoveryVersionAnnotation() {
        enricher.addAnnotations(builder);
        String annotation = annotation(DISCOVERY_VERSION);
        String value = builder.buildMetadata().getAnnotations().get(annotation);
        assertEquals(discoveryVersion, value);
    }

    @Test
    public void testPathAnnotation() {
        enricher.addAnnotations(builder);
        String annotation = annotation(PATH);
        String value = builder.buildMetadata().getAnnotations().get(annotation);
        assertEquals("/rest-3scale", value);
    }

    @Test
    public void testPortAnnotation() {
        enricher.addAnnotations(builder);
        String annotation = annotation(PORT);
        String value = builder.buildMetadata().getAnnotations().get(annotation);
        assertEquals(port, value);
    }

    @Test
    public void testSchemeAnnotation() {
        enricher.addAnnotations(builder);
        String annotation = annotation(SCHEME);
        String value = builder.buildMetadata().getAnnotations().get(annotation);
        assertEquals(scheme, value);
    }

    private String label() {
        return ServiceDiscoveryEnricher.PREFIX;
    }

    private String annotation(String name) {
        return ServiceDiscoveryEnricher.PREFIX + "/" + name;
    }

}