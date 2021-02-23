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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import static org.eclipse.jkube.kit.common.Configs.asInteger;
import static org.eclipse.jkube.kit.common.util.FileUtil.stripPrefix;


/**
 * Enriches Quarkus containers with health checks if the quarkus-smallrye-health is present
 */
public class QuarkusHealthCheckEnricher extends AbstractHealthCheckEnricher {

    private static final String READY_SUBPATH = "ready";
    private static final String LIVE_SUBPATH = "live";

    public QuarkusHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-quarkus");
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        SCHEME("scheme", "HTTP"),
        PORT("port", "8080"),
        FAILURE_THRESHOLD("failureThreshold", "3"),
        SUCCESS_THRESHOLD("successThreshold", "1"),
        LIVENESS_INITIAL_DELAY("livenessInitialDelay", null),
        READINESS_INTIAL_DELAY("readinessIntialDelay", null),
        HEALTH_PATH("path", "health");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.READINESS_INTIAL_DELAY, "5")),
            READY_SUBPATH);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.LIVENESS_INITIAL_DELAY, "10")),
            LIVE_SUBPATH);
    }

    private Probe discoverQuarkusHealthCheck(int initialDelay, String subPath) {
        if (!getContext().hasDependency("io.quarkus", "quarkus-smallrye-health")) {
            return null;
        }
        return new ProbeBuilder()
            .withNewHttpGet()
              .withNewPort(asInteger(getConfig(Config.PORT)))
              .withPath(String.format("/%s/%s", stripPrefix(getConfig(Config.HEALTH_PATH), "/"), subPath))
              .withScheme(getConfig(Config.SCHEME))
            .endHttpGet()
            .withFailureThreshold(asInteger(getConfig(Config.FAILURE_THRESHOLD)))
            .withSuccessThreshold(asInteger(getConfig(Config.SUCCESS_THRESHOLD)))
            .withInitialDelaySeconds(initialDelay)
            .build();
    }

}
