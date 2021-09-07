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
package org.eclipse.jkube.springboot.enricher;

import java.util.Properties;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Enriches spring-boot containers with health checks if the actuator module is present.
 */
public class SpringBootHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public static final String ENRICHER_NAME = "jkube-healthcheck-spring-boot";

    protected static final String[] REQUIRED_CLASSES = {
            "org.springframework.boot.actuate.health.HealthIndicator",
            "org.springframework.web.context.support.GenericWebApplicationContext"
    };

    private static final String SCHEME_HTTPS = "HTTPS";
    private static final String SCHEME_HTTP = "HTTP";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        READINESS_PROBE_INITIAL_DELAY_SECONDS("readinessProbeInitialDelaySeconds", "10"),
        READINESS_PROBE_PERIOD_SECONDS("readinessProbePeriodSeconds", null),
        PATH("path", "/health"),
        LIVENESS_PROBE_INITIAL_DELAY_SECONDS("livenessProbeInitialDelaySeconds", "180"),
        LIVENESS_PROBE_PERIOD_SECONDS("livenessProbePeriodSeconds", null),
        FAILURE_THRESHOLD("failureThreshold", "3"),
        SUCCESS_THRESHOLD("successThreshold", "1"),
        TIMEOUT_SECONDS("timeoutSeconds", null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public SpringBootHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    protected Probe getReadinessProbe() {
        Integer initialDelay = Configs.asInteger(getConfig(Config.READINESS_PROBE_INITIAL_DELAY_SECONDS));
        Integer period = Configs.asInteger(getConfig(Config.READINESS_PROBE_PERIOD_SECONDS));
        Integer timeout = Configs.asInteger(getConfig(Config.TIMEOUT_SECONDS));
        Integer failureThreshold = Configs.asInteger(getConfig(Config.FAILURE_THRESHOLD));
        Integer successThreshold = Configs.asInteger(getConfig(Config.SUCCESS_THRESHOLD));
        return discoverSpringBootHealthCheck(initialDelay, period, timeout, failureThreshold, successThreshold);
    }

    @Override
    protected Probe getLivenessProbe() {
        Integer initialDelay = Configs.asInteger(getConfig(Config.LIVENESS_PROBE_INITIAL_DELAY_SECONDS));
        Integer period = Configs.asInteger(getConfig(Config.LIVENESS_PROBE_PERIOD_SECONDS));
        Integer timeout = Configs.asInteger(getConfig(Config.TIMEOUT_SECONDS));
        Integer failureThreshold = Configs.asInteger(getConfig(Config.FAILURE_THRESHOLD));
        Integer successThreshold = Configs.asInteger(getConfig(Config.SUCCESS_THRESHOLD));
        return discoverSpringBootHealthCheck(initialDelay, period, timeout, failureThreshold, successThreshold);
    }

    protected Probe discoverSpringBootHealthCheck(Integer initialDelay, Integer period, Integer timeout, Integer failureTh, Integer successTh) {
        try {
            if (getContext().getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES)) {
                Properties properties = SpringBootUtil.getSpringBootApplicationProperties(getContext().getProjectClassLoaders().getCompileClassLoader());
                return buildProbe(properties, initialDelay, period, timeout, failureTh, successTh);
            }
        } catch (Exception ex) {
            log.error("Error while reading the spring-boot configuration", ex);
        }
        return null;
    }

    protected Probe buildProbe(Properties springBootProperties, Integer initialDelay, Integer period, Integer timeout, Integer failureTh, Integer successTh) {
        SpringBootConfigurationHelper propertyHelper = new SpringBootConfigurationHelper(getContext().getDependencyVersion(SpringBootConfigurationHelper.SPRING_BOOT_GROUP_ID, SpringBootConfigurationHelper.SPRING_BOOT_ARTIFACT_ID));
        Integer managementPort = propertyHelper.getManagementPort(springBootProperties);
        boolean usingManagementPort = managementPort != null;

        Integer port = managementPort;
        if (port == null) {
            port = propertyHelper.getServerPort(springBootProperties);
        }

        String scheme;
        String prefix;
        if (usingManagementPort) {
            scheme = StringUtils.isNotBlank(springBootProperties.getProperty(propertyHelper.getManagementKeystorePropertyKey())) ? SCHEME_HTTPS : SCHEME_HTTP;
            prefix = springBootProperties.getProperty(propertyHelper.getManagementContextPathPropertyKey(), "");
        } else {
            scheme = StringUtils.isNotBlank(springBootProperties.getProperty(propertyHelper.getServerKeystorePropertyKey())) ? SCHEME_HTTPS : SCHEME_HTTP;
            prefix = springBootProperties.getProperty(propertyHelper.getServerContextPathPropertyKey(), "");
            prefix += springBootProperties.getProperty(propertyHelper.getServletPathPropertyKey(), "");
            prefix += springBootProperties.getProperty(propertyHelper.getManagementContextPathPropertyKey(), "");
        }

        String actuatorBasePathKey = propertyHelper.getActuatorBasePathPropertyKey();
        String actuatorBasePath = propertyHelper.getActuatorDefaultBasePath();
        if (actuatorBasePathKey != null) {
            actuatorBasePath = springBootProperties.getProperty(actuatorBasePathKey, actuatorBasePath);
        }

        // lets default to adding a spring boot actuator health check
        ProbeBuilder probeBuilder = new ProbeBuilder().
                withNewHttpGet().withNewPort(port).withPath(normalizeMultipleSlashes(prefix + actuatorBasePath + Configs.asString(getConfig(Config.PATH)))).withScheme(scheme).endHttpGet();

        if (initialDelay != null) {
            probeBuilder = probeBuilder.withInitialDelaySeconds(initialDelay);
        }
        if (period != null) {
            probeBuilder = probeBuilder.withPeriodSeconds(period);
        }
        if (timeout != null) {
            probeBuilder.withTimeoutSeconds(timeout);
        }
        if(failureTh != null) {
            probeBuilder.withFailureThreshold(failureTh);
        }
        if(successTh != null) {
            probeBuilder.withSuccessThreshold(successTh);
        }

        return probeBuilder.build();
    }

    private String normalizeMultipleSlashes(String s) {
        //substitute multiple consecutive "/" with a single occurrence (i.e. ////a//b///c////////d -> /a/b/c/d)
        return s.replaceAll("/{2,}","/");
    }
}

