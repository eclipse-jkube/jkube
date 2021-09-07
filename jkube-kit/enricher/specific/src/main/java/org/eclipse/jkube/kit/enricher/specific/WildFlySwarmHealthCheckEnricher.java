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

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enriches wildfly-swarm containers with health checks if the monitoring fraction is present.
 */
public class WildFlySwarmHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public WildFlySwarmHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-wildfly-swarm");
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        SCHEME("scheme", "HTTP"),
        PORT("port", "8080"),
        FAILURE_THRESHOLD("failureThreshold", "3"),
        SUCCESS_THRESHOLD("successThreshold", "1"),
        PATH("path", "/health");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
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
        return getConfig(Config.SCHEME);
    }

    protected int getPort() {
        return Configs.asInt(getConfig(Config.PORT));
    }

    protected String getPath() {
        return getConfig(Config.PATH);
    }

    protected int getFailureThreshold() { return Configs.asInteger(getConfig(Config.FAILURE_THRESHOLD)); }

    protected int getSuccessThreshold() { return Configs.asInteger(getConfig(Config.SUCCESS_THRESHOLD)); }

}
