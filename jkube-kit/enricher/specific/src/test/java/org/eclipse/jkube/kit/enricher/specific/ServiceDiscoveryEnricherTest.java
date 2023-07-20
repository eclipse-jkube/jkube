/*
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceDiscoveryEnricherTest {

    // configured properties
    private static final String path = "/rest-3scale";
    private static final String port = "8080";
    private static final String scheme = "http";
    private static final String descriptionPath = "/api-doc";
    private static final String discoverable = "true";
    private static final String discoveryVersion = "v1337";

    private JKubeEnricherContext context;

    private ServiceDiscoveryEnricher enricher;

    private ServiceBuilder builder;


    @BeforeEach
    void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
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
        when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
        enricher = new ServiceDiscoveryEnricher(context);
        builder = new ServiceBuilder();
    }

    @Test
    void discoveryLabel() {
        enricher.addAnnotations(builder);
        String value = builder.buildMetadata().getLabels().get("discovery.3scale.net");
        assertThat(value).isEqualTo(discoverable);
    }

    @Test
    void defaultDiscoveryVersionAnnotation() {
        context.getConfiguration().getProcessorConfig().getConfig().get("jkube-service-discovery").remove("discoveryVersion");
        enricher.addAnnotations(builder);
        assertThat(builder.buildMetadata().getAnnotations())
            .containsEntry("discovery.3scale.net/discovery-version", "v1");
    }

    @Test
    void addedAnnotations() {
      // Given + When
      enricher.addAnnotations(builder);

      // Then
      assertThat(builder.buildMetadata().getAnnotations())
          .containsEntry("discovery.3scale.net/description-path", descriptionPath)
          .containsEntry("discovery.3scale.net/discovery-version", discoveryVersion)
          .containsEntry("discovery.3scale.net/path", "/rest-3scale")
          .containsEntry("discovery.3scale.net/port", port)
          .containsEntry("discovery.3scale.net/scheme", scheme);
    }

    @Test
    void serviceWithNullPort_shouldThrowException() {
        // Given
        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .withNewMetadata().withName("test-svc").endMetadata()
                .withNewSpec()
                .addNewPort()
                .withName("foo")
                .endPort()
                .endSpec();

        // When + Then
        assertThatNullPointerException()
            .isThrownBy(() -> enricher.addAnnotations(serviceBuilder))
            .withMessage("Service test-svc .spec.ports[0].port: required value");
    }

}
