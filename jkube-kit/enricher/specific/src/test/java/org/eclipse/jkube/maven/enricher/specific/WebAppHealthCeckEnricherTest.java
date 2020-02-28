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
package org.eclipse.jkube.maven.enricher.specific;

import io.fabric8.kubernetes.api.model.Probe;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WebAppHealthCeckEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    private void setupExpectations(Map<String, Object> config) {
        new Expectations() {{
            context.hasPlugin("org.apache.maven.plugins", "maven-war-plugin");
            result = true;

            Configuration.Builder configBuilder = new Configuration.Builder();
            configBuilder.pluginConfigLookup(getProjectLookup(config));
        }};
    }

    @Test
    public void noEnrichmentIfNoPath() {

        // given

        WebAppHealthCheckEnricher enricher = new WebAppHealthCheckEnricher(context);
        setupExpectations(new HashMap<>());

        // when

        Probe probeLiveness = enricher.getLivenessProbe();
        Probe probeReadiness = enricher.getReadinessProbe();

        // then
        assertThat(probeLiveness).isNull();
        assertThat(probeReadiness).isNull();
    }

    @Test
    public void enrichmentWithDefaultsIfPath() {

        // given

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
