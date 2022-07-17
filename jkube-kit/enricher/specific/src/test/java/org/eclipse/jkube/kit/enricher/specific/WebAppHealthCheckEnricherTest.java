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

import io.fabric8.kubernetes.api.model.Probe;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class WebAppHealthCheckEnricherTest {

    private JKubeEnricherContext context;
    @Before
    public void setUp() {
        context = mock(JKubeEnricherContext.class);
    }
    private void setupExpectations(Map<String, Object> config) {
        when(context.hasPlugin("org.apache.maven.plugins", "maven-war-plugin")).thenReturn(true);
        Configuration.ConfigurationBuilder configBuilder = Configuration.builder();
        configBuilder.pluginConfigLookup(getProjectLookup(config));
    }

    @Test
    public void noEnrichmentIfNoPath() {

        // given

        try (MockedConstruction<WebAppHealthCheckEnricher> helmUploaderMockedConstruction = mockConstruction(WebAppHealthCheckEnricher.class)){
            WebAppHealthCheckEnricher enricher = new WebAppHealthCheckEnricher(context);
            setupExpectations(new HashMap<>());
            // when

            Probe probeLiveness = enricher.getLivenessProbe();
            Probe probeReadiness = enricher.getReadinessProbe();

            // then
            assertThat(probeLiveness).isNull();
            assertThat(probeReadiness).isNull();
        }

    }

    @Test
    public void enrichmentWithDefaultsIfPath() {

        // given
        try (MockedConstruction<WebAppHealthCheckEnricher> helmUploaderMockedConstruction = mockConstruction(WebAppHealthCheckEnricher.class)) {
            final Map<String, Object> config = createFakeConfig(
                    "<path>/health</path>");
            setupExpectations(config);

            WebAppHealthCheckEnricher enricher = new WebAppHealthCheckEnricher(context);

            // when

            Probe probeLiveness = enricher.getLivenessProbe();
            Probe probeReadiness = enricher.getReadinessProbe();

            // then
            assertThat(probeLiveness).isNull();
            assertThat(probeReadiness).isNull();
        }
    }

    private BiFunction<String, String, Optional<Map<String, Object>>> getProjectLookup(Map<String, Object> config) {
        return (s,i) -> {
            assertThat(s).isEqualTo("maven");
            assertThat(i).isEqualTo("org.eclipse.jkube:jkube-maven-plugin");
            return Optional.ofNullable(config);
        };
    }

    private Map<String, Object> createFakeConfig(String config) {

        Map<String, Object> jkubeHealthCheckWebapp = new HashMap<>();
        jkubeHealthCheckWebapp.put("jkube-healthcheck-webapp", config);

        Map<String, Object> enricherConfigHashMap = new HashMap<>();
        enricherConfigHashMap.put("config", jkubeHealthCheckWebapp);

        Map<String, Object> configurationHashmap = new HashMap<>();
        configurationHashmap.put("enricher", enricherConfigHashMap);

        return configurationHashmap;
    }

}
