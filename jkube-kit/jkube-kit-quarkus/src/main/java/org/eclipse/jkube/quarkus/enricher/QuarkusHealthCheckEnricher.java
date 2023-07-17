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

import java.util.function.Function;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;
import org.eclipse.jkube.quarkus.QuarkusUtils;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.Configs.asInteger;
import static org.eclipse.jkube.quarkus.QuarkusUtils.QUARKUS_GROUP_ID;
import static org.eclipse.jkube.quarkus.QuarkusUtils.extractPort;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.concatPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.isStartupEndpointSupported;
import static org.eclipse.jkube.quarkus.QuarkusUtils.resolveCompleteQuarkusHealthRootPath;

/**
 * Enriches Quarkus containers with health checks if the quarkus-smallrye-health is present
 */
public class QuarkusHealthCheckEnricher extends AbstractHealthCheckEnricher {

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
        READINESS_INITIAL_DELAY("readinessInitialDelay", null),
        STARTUP_INITIAL_DELAY("startupInitialDelay", null),
        HEALTH_PATH("path", null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.READINESS_INITIAL_DELAY, "5")),
            QuarkusUtils::resolveQuarkusReadinessPath);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverQuarkusHealthCheck(asInteger(getConfig(Config.LIVENESS_INITIAL_DELAY, "10")),
            QuarkusUtils::resolveQuarkusLivenessPath);
    }
    
    @Override
    protected Probe getStartupProbe() {
        if (isStartupEndpointSupported(getContext().getProject())) {
            return discoverQuarkusHealthCheck(asInteger(getConfig(Config.STARTUP_INITIAL_DELAY, "5")),
                QuarkusUtils::resolveQuarkusStartupPath);
        }
        return null;
    }

    private Probe discoverQuarkusHealthCheck(int initialDelay, Function<JavaProject, String> pathResolver) {
        if (!getContext().hasDependency(QUARKUS_GROUP_ID, "quarkus-smallrye-health")) {
            return null;
        }
        return new ProbeBuilder()
            .withNewHttpGet()
            .withNewPort(asInteger(extractPort(getQuarkusConfiguration(getContext().getProject()),getConfig(Config.PORT))))
              //.withNewPort(asInteger(getConfig(Config.PORT)))
              .withPath(resolveHealthPath(pathResolver.apply(getContext().getProject())))
              .withScheme(getConfig(Config.SCHEME))
            .endHttpGet()
            .withFailureThreshold(asInteger(getConfig(Config.FAILURE_THRESHOLD)))
            .withSuccessThreshold(asInteger(getConfig(Config.SUCCESS_THRESHOLD)))
            .withInitialDelaySeconds(initialDelay)
            .build();
    }

    private String resolveHealthPath(String subPath) {
        if (StringUtils.isNotBlank(getConfig(Config.HEALTH_PATH))) {
            return concatPath(getConfig(Config.HEALTH_PATH), subPath);
        }
        return resolveCompleteQuarkusHealthRootPath(getContext().getProject(), subPath);
    }
}
