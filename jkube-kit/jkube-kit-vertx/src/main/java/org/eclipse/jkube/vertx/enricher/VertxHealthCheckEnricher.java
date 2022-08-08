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
package org.eclipse.jkube.vertx.enricher;

import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ProbeFluent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.hasDependencyWithGroupId;

/**
 * Configures the health checks for a Vert.x project. Unlike other enricher this enricher extract the configuration from
 * the following project properties: `vertx.health.port`, `vertx.health.path`.
 * <p>
 * It builds a liveness probe and a readiness probe using:
 * <p>
 * <ul>
 * <li>`vertx.health.port` - the port, 8080 by default, a negative number disables the health check</li>
 * <li>`vertx.health.path` - the path, / by default, an empty (non null) value disables the health check</li>
 * <li>`vertx.health.scheme` - the scheme, HTTP by default, can be set to HTTPS (adjusts the port accordingly)</li>
 * </ul>
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxHealthCheckEnricher extends AbstractHealthCheckEnricher {

    private static final String ENRICHER_NAME = "jkube-healthcheck-vertx";
    private static final String READINESS = "readiness";
    private static final String LIVENESS = "liveness";
    private static final String VERTX_MAVEN_PLUGIN_GROUP = "io.reactiverse";
    private static final String VERTX_MAVEN_PLUGIN_ARTIFACT = "vertx-maven-plugin";
    static final String VERTX_GROUPID = "io.vertx";

    private static final int DEFAULT_MANAGEMENT_PORT = 8080;
    private static final String SCHEME_HTTP = "HTTP";

    private static final String VERTX_HEALTH = "vertx.health.";
    private static final Function<? super String, String> TRIM = input -> input == null ? null : input.trim();
    protected static final String[] JKUBE_PLUGINS = { "kubernetes-maven-plugin", "openshift-maven-plugin",
        "org.eclipse.jkube.kubernetes.gradle.plugin", "org.eclipse.jkube.openshift.gradle.plugin",
        "org.eclipse.jkube.gradle.plugin.KubernetesPlugin", "org.eclipse.jkube.gradle.plugin.OpenShiftPlugin"
    };

    public static final String ERROR_MESSAGE = "Location of %s should return a String but found %s with value %s";

    public VertxHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        TYPE("type"),
        PORT("port"),
        PORT_NAME("port-name"),
        PATH("path"),
        SCHEME("scheme"),
        INITIAL_DELAY("initial-delay"),
        PERIOD("period"),
        TIMEOUT("timeout"),
        SUCCESS_THRESHOLD("success-threshold"),
        FAILURE_THRESHOLD("failure-threshold"),
        COMMAND("command"),
        HEADERS("headers");

        @Getter
        protected String key;
    }

    @Override
    protected Probe getReadinessProbe() {
        return discoverVertxHealthCheck(true);
    }

    @Override
    protected Probe getLivenessProbe() {
        return discoverVertxHealthCheck(false);
    }

    private boolean isApplicable() {
        return getContext().hasPlugin(VERTX_MAVEN_PLUGIN_GROUP, VERTX_MAVEN_PLUGIN_ARTIFACT)
               || hasDependencyWithGroupId(getContext().getProject(), VERTX_GROUPID);
    }

    private String getSpecificPropertyName(boolean readiness, Config config) {
        if (readiness) {
            return VERTX_HEALTH + "readiness." + config.getKey();
        } else {
            return VERTX_HEALTH + "liveness." + config.getKey();
        }
    }

    private Probe discoverVertxHealthCheck(boolean readiness) {
        if (!isApplicable()) {
            return null;
        }
        // We don't allow to set the HOST, because it should rather be configured in the HTTP header (Host header)
        // cf. https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/
        String type = getStringValue(Config.TYPE, readiness).orElse("http").toUpperCase();
        Optional<Integer> port = getIntegerValue(Config.PORT, readiness);
        Optional<String> portName = getStringValue(Config.PORT_NAME, readiness);
        String path = getStringValue(Config.PATH, readiness)
                .map(input -> {
                    if (input.isEmpty() || input.startsWith("/")) {
                        return input;
                    }
                    return "/" + input;
                })
                .orElse(null);
        String scheme = getStringValue(Config.SCHEME, readiness).orElse(SCHEME_HTTP).toUpperCase();
        Optional<Integer> initialDelay = getIntegerValue(Config.INITIAL_DELAY, readiness);
        Optional<Integer> period = getIntegerValue(Config.PERIOD, readiness);
        Optional<Integer> timeout = getIntegerValue(Config.TIMEOUT, readiness);
        Optional<Integer> successThreshold = getIntegerValue(Config.SUCCESS_THRESHOLD, readiness);
        Optional<Integer> failureThreshold = getIntegerValue(Config.FAILURE_THRESHOLD, readiness);
        List<String> command = getListValue(Config.COMMAND, readiness).orElse(Collections.<String>emptyList());
        Map<String, String> headers = getMapValue(Config.HEADERS, readiness).orElse(Collections.<String, String>emptyMap());


        // Validate
        // Port and port-name cannot be set at the same time
        if (port.isPresent() && portName.isPresent()) {
            log.error("Invalid health check configuration - both 'port' and 'port-name' are set, only one of them can be used");
            throw new IllegalArgumentException("Invalid health check configuration - both 'port' and 'port-name' are set, only one of them can be used");
        }

        if (type.equalsIgnoreCase("TCP")) {
            if (!port.isPresent() && !portName.isPresent()) {
                log.info("TCP health check disabled (port not set)");
                return null;
            }
            if (port.isPresent() && port.get() <= 0) {
                log.info("TCP health check disabled (port set to a negative number)");
                return null;
            }
        } else if (type.equalsIgnoreCase("EXEC")) {
            if (command.isEmpty()) {
                log.info("TCP health check disabled (command not set)");
                return null;
            }
        } else if (type.equalsIgnoreCase("HTTP")) {
            if (port.isPresent() && port.get() <= 0) {
                log.info("HTTP health check disabled (port set to " + port.get());
                return null;
            }

            if (path == null) {
                log.info("HTTP health check disabled (path not set)");
                return null;
            }

            if (path.isEmpty()) {
                log.info("HTTP health check disabled (the path is empty)");
                return null;
            }

            // Set default port if not set
            if (!port.isPresent() && !portName.isPresent()) {
                log.info("Using default management port (8080) for HTTP health check probe");
                port = Optional.of(DEFAULT_MANAGEMENT_PORT);
            }

        } else {
            log.error("Invalid health check configuration - Unknown probe type, only 'exec', 'tcp' and 'http' (default) are supported");
            throw new IllegalArgumentException("Invalid health check configuration - Unknown probe type, only 'exec', 'tcp' and 'http' (default) are supported");
        }

        // Time to build the probe
        ProbeBuilder builder = new ProbeBuilder();
        initialDelay.ifPresent(builder::withInitialDelaySeconds);
        period.ifPresent(builder::withPeriodSeconds);
        timeout.ifPresent(builder::withTimeoutSeconds);
        successThreshold.ifPresent(builder::withSuccessThreshold);
        failureThreshold.ifPresent(builder::withFailureThreshold);

        switch (type) {
            case "HTTP":
                ProbeFluent.HttpGetNested<ProbeBuilder> http = builder.withNewHttpGet()
                        .withScheme(scheme)
                        .withPath(path);
                port.ifPresent(http::withNewPort);
                portName.ifPresent(http::withNewPort);
                if (!headers.isEmpty()) {
                    List<HTTPHeader> list = new ArrayList<>();
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        list.add(new HTTPHeader(entry.getKey(), entry.getValue()));
                    }
                    http.withHttpHeaders(list);
                }
                http.endHttpGet();
                break;
            case "TCP":
                ProbeFluent.TcpSocketNested<ProbeBuilder> tcp = builder.withNewTcpSocket();
                port.ifPresent(tcp::withNewPort);
                portName.ifPresent(tcp::withNewPort);
                tcp.endTcpSocket();
                break;
            case "EXEC":
                builder.withNewExec().withCommand(command).endExec();
        }

        return builder.build();
    }

    private Optional<String> getStringValue(Config config, boolean readiness) {
        final Optional<String> specificValue = getSpecificValueFromConfigOrProperties(config, readiness);
        if (specificValue.isPresent()) {
            return specificValue.map(TRIM);
        }
        return getGenericValueFromConfigOrProperties(config).map(TRIM);
    }

    private Optional<String> getSpecificValueFromConfigOrProperties(Config config, boolean readiness) {
        final Optional<String> configValue = getElementAsString(readiness ? READINESS : LIVENESS, config.getKey());
        if (configValue.isPresent()) {
            return configValue;
        }
        return Optional.ofNullable(Configs.getFromSystemPropertyWithPropertiesAsFallback(
            enricherContext.getProperties(), getSpecificPropertyName(readiness, config)));
    }

    private Optional<String> getGenericValueFromConfigOrProperties(Config config) {
        return Optional.ofNullable(getConfigWithFallback(config, VERTX_HEALTH + config.getKey(), null));
    }

    private Optional<List<String>> getListValue(Config config, boolean readiness) {
        Optional<Object> element = getElement(readiness ? READINESS : LIVENESS, config.getKey());
        if (!element.isPresent()) {
            element = getElement(config.getKey());
        }

        return element.map(input -> {
            if (input instanceof Map) {
                final Collection<Object> values = ((Map<String, Object>) input).values();
                List<String> elements = new ArrayList<>();
                for (Object value : values) {
                    if (value instanceof List) {
                        List<String> currentValues = (List<String>) value;
                        elements.addAll(currentValues);
                    } else {
                        elements.add((String) value);
                    }
                }

                return elements;
            } else {
                throw new IllegalArgumentException(String.format(
                    ERROR_MESSAGE,
                    config.getKey(), input.getClass(), input.toString()));
            }

        });
    }

    private Optional<Map<String, String>> getMapValue(Config config, boolean readiness) {
        Optional<Object> element = getElement(readiness ? READINESS : LIVENESS, config.getKey());
        if (!element.isPresent()) {
            element = getElement(config.getKey());
        }

        return element.map(input -> {
            if (input instanceof Map) {
                return (Map<String, String>) input;
            } else {
                throw new IllegalArgumentException(String.format(
                    ERROR_MESSAGE,
                    config.getKey(), input.getClass(), input.toString()));
            }
        });
    }


    private Optional<Integer> getIntegerValue(Config config, boolean readiness) {
        return getStringValue(config, readiness)
                .map(Integer::parseInt);
    }

    private Optional<String> getElementAsString(String... keys) {
        return getElement(keys).map(input -> {
            if (input instanceof String) {
                return (String) input;
            } else {
                throw new IllegalArgumentException(String.format(
                    ERROR_MESSAGE,
                Arrays.toString(keys), input.getClass(), input));
            }
        });
    }

    private Optional<Object> getElement(String... path) {
        // Can't use ProcessorConfig from Maven since regular Plexus deserialization won't get fields with nested properties (e.g. readiness)
        Object currentRoot  = getFromPluginConfiguration()
            .filter(map -> !map.isEmpty())
            // Gradle does support valid ProcessorConfig usage
            .orElse(Collections.singletonMap("enricher", Collections.singletonMap("config",
                getContext().getConfiguration().getProcessorConfig().getConfig())));
        for (String key : Stream.concat(Stream.of("enricher", "config", ENRICHER_NAME),
            Stream.of(path)).collect(Collectors.toList())) {
            if (currentRoot instanceof Map) {
                currentRoot = ((Map<String, Object>) currentRoot).get(key);
                if (currentRoot == null) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(currentRoot);
    }

    private Optional<Map<String, Object>> getFromPluginConfiguration() {
        for(String pluginId : JKUBE_PLUGINS) {
            Optional<Map<String, Object>> configuration = getContext().getConfiguration().getPluginConfiguration("maven", pluginId);
            if(configuration.isPresent()) {
                return configuration;
            }
        }
        return Optional.empty();
    }

}
