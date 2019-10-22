/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.maven.enricher.specific;

import io.fabric8.kubernetes.api.model.Probe;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.model.Configuration;
import io.jkube.maven.enricher.api.util.MavenConfigurationExtractor;
import mockit.Expectations;
import mockit.Mocked;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WebAppHealthCeckEnricherTest {

    @Mocked
    private MavenEnricherContext context;

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
            assertThat(i).isEqualTo("io.jkube:jkube-maven-plugin");
            return Optional.ofNullable(config);
        };
    }

    private Map<String, Object> createFakeConfig(String config) {

        String content = "<configuration><enricher><config><jkube-healthcheck-webapp>"
            + config
            + "</jkube-healthcheck-webapp></config></enricher></configuration>";
        Xpp3Dom dom;
        try {
            dom = Xpp3DomBuilder.build(new StringReader(content));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return MavenConfigurationExtractor.extract(dom);

    }

}
