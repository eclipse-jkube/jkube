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
package io.jkube.quarkus.enricher;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.jkube.kit.common.Configs;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.specific.AbstractHealthCheckEnricher;

import static io.jkube.kit.common.Configs.asInteger;


/**
 * Enriches Quarkus containers with health checks if the quarkus-smallrye-health is present
 */
public class QuarkusHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public QuarkusHealthCheckEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-quarkus");
    }

    // Available configuration keys
    private enum Config implements Configs.Key {

        scheme {{
            d = "HTTP";
        }},
        port {{
            d = "8080";
        }},
        failureThreshold                    {{ d = "3"; }},
        successThreshold                    {{ d = "1"; }},
        livenessInitialDelay,
        readinessIntialDelay,
        path {{
            d = "/health";
        }};

        protected String d;

        public String def() {
            return d;
        }
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.readinessIntialDelay, "5")));
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.livenessInitialDelay, "10")));
    }

    private Probe discoverQuarkusHealthCheck(int initialDelay) {
        if (!getContext().hasDependency("io.quarkus", "quarkus-smallrye-health")) {
            return null;
        }

        return new ProbeBuilder()
            .withNewHttpGet()
              .withNewPort(asInteger(getConfig(Config.port)))
              .withPath(getConfig(Config.path))
              .withScheme(getConfig(Config.scheme))
            .endHttpGet()
            .withFailureThreshold(asInteger(getConfig(Config.failureThreshold)))
            .withSuccessThreshold(asInteger(getConfig(Config.successThreshold)))
            .withInitialDelaySeconds(initialDelay)
            .build();
    }

}
