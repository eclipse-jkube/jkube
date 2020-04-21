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

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import java.util.Properties;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;
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

    private enum Config implements Configs.Key {
        readinessProbeInitialDelaySeconds   {{ d = "10"; }},
        readinessProbePeriodSeconds,
        path                                {{ d = "/health"; }},
        livenessProbeInitialDelaySeconds    {{ d = "180"; }},
        livenessProbePeriodSeconds,
        failureThreshold                    {{ d = "3"; }},
        successThreshold                    {{ d = "1"; }},
        timeoutSeconds;

        public String def() { return d; } protected String d;
    }

    public SpringBootHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    protected Probe getReadinessProbe() {
        Integer initialDelay = Configs.asInteger(getConfig(Config.readinessProbeInitialDelaySeconds));
        Integer period = Configs.asInteger(getConfig(Config.readinessProbePeriodSeconds));
        Integer timeout = Configs.asInteger(getConfig(Config.timeoutSeconds));
        Integer failureThreshold = Configs.asInteger(getConfig(Config.failureThreshold));
        Integer successThreshold = Configs.asInteger(getConfig(Config.successThreshold));
        return discoverSpringBootHealthCheck(initialDelay, period, timeout, failureThreshold, successThreshold);
    }

    @Override
    protected Probe getLivenessProbe() {
        Integer initialDelay = Configs.asInteger(getConfig(Config.livenessProbeInitialDelaySeconds));
        Integer period = Configs.asInteger(getConfig(Config.livenessProbePeriodSeconds));
        Integer timeout = Configs.asInteger(getConfig(Config.timeoutSeconds));
        Integer failureThreshold = Configs.asInteger(getConfig(Config.failureThreshold));
        Integer successThreshold = Configs.asInteger(getConfig(Config.successThreshold));
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
                withNewHttpGet().withNewPort(port).withPath(normalizeMultipleSlashes(prefix + actuatorBasePath + Configs.asString(getConfig(Config.path)))).withScheme(scheme).endHttpGet();

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

