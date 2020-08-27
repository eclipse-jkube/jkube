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
package org.eclipse.jkube.thorntail.v2.enricher;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.ThorntailUtil;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import java.util.Properties;

/**
 * Enriches thorntail-v2 containers with health checks if the monitoring fraction is present.
 */
public class ThorntailV2HealthCheckEnricher extends AbstractHealthCheckEnricher {

    private static final String IO_THORNTAIL = "io.thorntail";

    public ThorntailV2HealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-thorntail-v2");
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
        return discoverThorntailHealthCheck(10);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverThorntailHealthCheck(180);
    }

    private Probe discoverThorntailHealthCheck(int initialDelay) {
        if (getContext().hasDependency(IO_THORNTAIL, "thorntail-kernel")) {
            // if there's thorntail-kernel, it's Thorntail v4
            return null;
        }

        if (getContext().hasDependency(IO_THORNTAIL, "monitor")
                || getContext().hasDependency(IO_THORNTAIL, "microprofile-health")) {
            Integer port = getPort();
            // scheme must be in upper case in k8s
            String scheme = getScheme().toUpperCase();
            String path = getPath();

            return new ProbeBuilder()
                     .withNewHttpGet().withNewPort(port).withPath(path).withScheme(scheme).endHttpGet()
                     .withFailureThreshold(getFailureThreshold())
                     .withSuccessThreshold(getSuccessThreshold())
                     .withInitialDelaySeconds(initialDelay).build();
        }
        return null;
    }

    protected int getFailureThreshold() { return Configs.asInteger(getConfig(Config.FAILURE_THRESHOLD)); }

    protected int getSuccessThreshold() { return Configs.asInteger(getConfig(Config.SUCCESS_THRESHOLD)); }

    protected String getScheme() {
        return getConfig(Config.SCHEME);
    }

    protected int getPort() {
        final Properties properties = ThorntailUtil.getThorntailProperties(getContext().getProjectClassLoaders().getCompileClassLoader());
        properties.putAll(System.getProperties());
        if (properties.containsKey("thorntail.http.port")) {
            return Integer.parseInt((String) properties.get("thorntail.http.port"));
        }

        return Configs.asInt(getConfig(Config.PORT));
    }

    protected String getPath() {
        return getConfig(Config.PATH);
    }
}