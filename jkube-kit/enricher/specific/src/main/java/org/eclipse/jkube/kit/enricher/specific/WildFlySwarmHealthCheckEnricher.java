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
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

/**
 * Enriches wildfly-swarm containers with health checks if the monitoring fraction is present.
 */
public class WildFlySwarmHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public WildFlySwarmHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-wildfly-swarm");
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
