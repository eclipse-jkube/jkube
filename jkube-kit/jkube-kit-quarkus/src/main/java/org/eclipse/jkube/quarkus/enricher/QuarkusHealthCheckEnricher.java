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
package org.eclipse.jkube.quarkus.enricher;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.specific.AbstractHealthCheckEnricher;

import static org.eclipse.jkube.kit.common.Configs.asInteger;


/**
 * Enriches Quarkus containers with health checks if the quarkus-smallrye-health is present
 */
public class QuarkusHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public QuarkusHealthCheckEnricher(JKubeEnricherContext buildContext) {
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
