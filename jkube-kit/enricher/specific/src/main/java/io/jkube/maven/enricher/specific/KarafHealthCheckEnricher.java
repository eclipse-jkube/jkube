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
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.jkube.kit.common.Configs;
import io.jkube.maven.enricher.api.MavenEnricherContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enriches Karaf containers with health check probes.
 */
public class KarafHealthCheckEnricher extends AbstractHealthCheckEnricher {

    private static final int DEFAULT_HEALTH_CHECK_PORT = 8181;

    public KarafHealthCheckEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-karaf");
    }

    private enum Config implements Configs.Key {
        failureThreshold                    {{ d = "3"; }},
        successThreshold                    {{ d = "1"; }};

        protected String d;

        public String def() {
            return d;
        }
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverKarafProbe("/readiness-check", 10);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverKarafProbe("/health-check", 180);
    }

    //
    // Karaf has a readiness/health URL exposed if the jkube-karaf-check feature is installed.
    //
    private Probe discoverKarafProbe(String path, int initialDelay) {

        final Optional<Map<String, Object>> configurationValues = getContext().getConfiguration().getPluginConfiguration("maven", "karaf-maven-plugin");

        if (!configurationValues.isPresent()) {
            return null;
        }
        final Optional<Object> lookup = configurationValues.map(m -> m.get("startupFeatures"));

        if (!lookup.isPresent()) {
            return null;
        }

        Object startupFeatures = lookup.get();
        if (!(startupFeatures instanceof Map)) {
            throw new IllegalArgumentException(String.format("For element %s was expected a complex object but a simple object was found of type %s and value %s",
                "startupFeatures", startupFeatures.getClass(), startupFeatures.toString()));
        }

        final Map<String, Object> startUpFeaturesObject = (Map<String, Object>) startupFeatures;
        final Object feature = startUpFeaturesObject.get("feature");

        if (feature != null) {

            // It can be a single feature or a list of features

            if (feature instanceof List) {
                final List<String> features = (List<String>) feature;

                for (String featureValue : features) {
                    if ("jkube-karaf-checks".equals(featureValue)) {
                        return new ProbeBuilder().withNewHttpGet().withNewPort(DEFAULT_HEALTH_CHECK_PORT).withPath(path).endHttpGet()
                                .withSuccessThreshold(getSuccessThreshold())
                                .withFailureThreshold(getFailureThreshold())
                                .withInitialDelaySeconds(initialDelay).build();
                    }
                }
            } else {

                String featureValue = (String) feature;
                if ("jkube-karaf-checks".equals(featureValue)) {
                    return new ProbeBuilder().withNewHttpGet().withNewPort(DEFAULT_HEALTH_CHECK_PORT).withPath(path).endHttpGet()
                            .withSuccessThreshold(getSuccessThreshold())
                            .withFailureThreshold(getFailureThreshold())
                            .withInitialDelaySeconds(initialDelay).build();
                }
            }

        }

        return null;
    }

    protected int getFailureThreshold() { return Configs.asInteger(getConfig(Config.failureThreshold)); }

    protected int getSuccessThreshold() { return Configs.asInteger(getConfig(Config.successThreshold)); }
}
