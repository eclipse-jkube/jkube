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

import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.Configs.asInteger;
import static org.eclipse.jkube.quarkus.QuarkusUtils.createHealthCheckPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusVersion;
import static org.eclipse.jkube.quarkus.QuarkusUtils.resolveCompleteQuarkusHealthRootPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.resolveQuarkusLivelinessRootPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.shouldUseAbsoluteHealthPaths;

/**
 * Enriches Quarkus containers with health checks if the quarkus-smallrye-health is present
 */
public class QuarkusHealthCheckEnricher extends AbstractHealthCheckEnricher {

    private static final String READY_SUBPATH = "ready";
    private static final String DEFAULT_HEALTH_PATH = "health";

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
        HEALTH_PATH("path", DEFAULT_HEALTH_PATH);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.READINESS_INTIAL_DELAY, "5")),
                this::findQuarkusHealthPathFromPropertiesOrConfig);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.LIVENESS_INITIAL_DELAY, "10")),
                this::findQuarkusHeathLivenessPath);
    }

    private Probe discoverQuarkusHealthCheck(int initialDelay, Supplier<String> pathSupplier) {
        if (!getContext().hasDependency("io.quarkus", "quarkus-smallrye-health")) {
            return null;
        }

        return new ProbeBuilder()
            .withNewHttpGet()
              .withNewPort(asInteger(getConfig(Config.PORT)))
              .withPath(pathSupplier.get())
              .withScheme(getConfig(Config.SCHEME))
            .endHttpGet()
            .withFailureThreshold(asInteger(getConfig(Config.FAILURE_THRESHOLD)))
            .withSuccessThreshold(asInteger(getConfig(Config.SUCCESS_THRESHOLD)))
            .withInitialDelaySeconds(initialDelay)
            .build();
    }

    private String findQuarkusHealthPathFromPropertiesOrConfig() {
        String defaultHealthPath = getConfig(Config.HEALTH_PATH);
        if (!defaultHealthPath.equals(DEFAULT_HEALTH_PATH)) {
            return createHealthCheckPath(defaultHealthPath, READY_SUBPATH);
        }
        return createHealthCheckPath(resolveCompleteQuarkusHealthRootPath(getContext().getProject()), READY_SUBPATH);
    }

    private String findQuarkusHeathLivenessPath() {
        String defaultHealthPath = getConfig(Config.HEALTH_PATH);
        String livelinessPath = resolveQuarkusLivelinessRootPath(getContext().getProject());
        if (!defaultHealthPath.equals(DEFAULT_HEALTH_PATH)) {
            return createHealthCheckPath(defaultHealthPath, livelinessPath);
        }

        String quarkusVersion = getQuarkusVersion(getContext().getProject()).orElse(null);
        if (shouldUseAbsoluteHealthPaths(quarkusVersion, livelinessPath)) {
            return livelinessPath;
        }

        return createHealthCheckPath(resolveCompleteQuarkusHealthRootPath(getContext().getProject()), livelinessPath);
    }
}
