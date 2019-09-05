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
package io.jshift.maven.enricher.specific;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.jshift.kit.common.Configs;
import io.jshift.maven.enricher.api.MavenEnricherContext;

/**
 * Enriches wildfly-swarm containers with health checks if the monitoring fraction is present.
 */
public class WildFlySwarmHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public WildFlySwarmHealthCheckEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jshift-healthcheck-wildfly-swarm");
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
        return discoverWildFlySwarmHealthCheck(10);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverWildFlySwarmHealthCheck(180);
    }

    private Probe discoverWildFlySwarmHealthCheck(int initialDelay) {
        if (getContext().hasDependency("org.wildfly.swarm", "monitor")
                || getContext().hasDependency("org.wildfly.swarm", "microprofile-health")) {
            Integer port = getPort();
            // scheme must be in upper case in k8s
            String scheme = getScheme().toUpperCase();
            String path = getPath();

            // lets default to adding a wildfly swarm health check
            return new ProbeBuilder()
                    .withNewHttpGet().withNewPort(port).withPath(path).withScheme(scheme).endHttpGet()
                    .withFailureThreshold(getFailureThreshold())
                    .withSuccessThreshold(getSuccessThreshold())
                    .withInitialDelaySeconds(initialDelay).build();
        }
        return null;
    }

    protected String getScheme() {
        return Configs.asString(getConfig(Config.scheme));
    }

    protected int getPort() {
        return Configs.asInt(getConfig(Config.port));
    }

    protected String getPath() {
        return Configs.asString(getConfig(Config.path));
    }

    protected int getFailureThreshold() { return Configs.asInteger(getConfig(Config.failureThreshold)); }

    protected int getSuccessThreshold() { return Configs.asInteger(getConfig(Config.successThreshold)); }

}
