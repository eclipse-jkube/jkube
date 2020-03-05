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
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.maven.enricher.specific.AbstractHealthCheckEnricher;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


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

    static final String VERTX_MAVEN_PLUGIN_GROUP = "io.reactiverse";
    static final String VERTX_MAVEN_PLUGIN_ARTIFACT = "vertx-maven-plugin";
    static final String VERTX_GROUPID = "io.vertx";

    private static final int DEFAULT_MANAGEMENT_PORT = 8080;
    private static final String SCHEME_HTTP = "HTTP";

    private static final String VERTX_HEALTH = "vertx.health.";
    private static final Function<? super String, String> TRIM = new Function<String, String>() {
        @Nullable
        @Override
        public String apply(@Nullable String input) {
            return input == null ? null : input.trim();
        }
    };
    protected static final String[] JKUBE_PLUGINS = {"kubernetes-maven-plugin", "openshift-maven-plugin"};

    public static final String ERROR_MESSAGE = "Location of %s should return a String but found %s with value %s";

    public VertxHealthCheckEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-vertx");
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
               || getContext().hasDependency(VERTX_GROUPID, null);
    }

    private String getSpecificPropertyName(boolean readiness, String attribute) {
        if (readiness) {
            return VERTX_HEALTH + "readiness." + attribute;
        } else {
            return VERTX_HEALTH + "liveness." + attribute;
        }
    }

    private Probe discoverVertxHealthCheck(boolean readiness) {
        if (!isApplicable()) {
            return null;
        }
        // We don't allow to set the HOST, because it should rather be configured in the HTTP header (Host header)
        // cf. https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/

        String type = getStringValue("type", readiness).orElse("http").toUpperCase();
        Optional<Integer> port = getIntegerValue("port", readiness);
        Optional<String> portName = getStringValue("port-name", readiness);
        String path = getStringValue("path", readiness)
                .map(input -> {
                    if (input.isEmpty() || input.startsWith("/")) {
                        return input;
                    }
                    return "/" + input;
                })
                .orElse(null);
        String scheme = getStringValue("scheme", readiness).orElse(SCHEME_HTTP).toUpperCase();
        Optional<Integer> initialDelay = getIntegerValue("initial-delay", readiness);
        Optional<Integer> period = getIntegerValue("period", readiness);
        Optional<Integer> timeout = getIntegerValue("timeout", readiness);
        Optional<Integer> successThreshold = getIntegerValue("success-threshold", readiness);
        Optional<Integer> failureThreshold = getIntegerValue("failure-threshold", readiness);
        List<String> command = getListValue("command", readiness).orElse(Collections.<String>emptyList());
        Map<String, String> headers = getMapValue("headers", readiness).orElse(Collections.<String, String>emptyMap());


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
        if (initialDelay.isPresent()) {
            builder.withInitialDelaySeconds(initialDelay.get());
        }
        if (period.isPresent()) {
            builder.withPeriodSeconds(period.get());
        }
        if (timeout.isPresent()) {
            builder.withTimeoutSeconds(timeout.get());
        }
        if (successThreshold.isPresent()) {
            builder.withSuccessThreshold(successThreshold.get());
        }
        if (failureThreshold.isPresent()) {
            builder.withFailureThreshold(failureThreshold.get());
        }

        switch (type) {
            case "HTTP":
                ProbeFluent.HttpGetNested<ProbeBuilder> http = builder.withNewHttpGet()
                        .withScheme(scheme)
                        .withPath(path);
                if (port.isPresent()) {
                    http.withNewPort(port.get());
                }
                if (portName.isPresent()) {
                    http.withNewPort(portName.get());
                }
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
                if (port.isPresent()) {
                    tcp.withNewPort(port.get());
                }
                if (portName.isPresent()) {
                    tcp.withNewPort(portName.get());
                }
                tcp.endTcpSocket();
                break;
            case "EXEC":
                builder.withNewExec().withCommand(command).endExec();
        }

        return builder.build();
    }

    private Optional<String> getStringValue(String attribute, boolean readiness) {

        String specific = getSpecificPropertyName(readiness, attribute);
        String generic = VERTX_HEALTH + attribute;
        // Check if we have the specific user property.
        Configuration contextConfig = getContext().getConfiguration();
        String property = contextConfig.getProperty(specific);
        if (property != null) {
            return Optional.of(property).map(TRIM);
        }

        property = contextConfig.getProperty(generic);
        if (property != null) {
            return Optional.of(property).map(TRIM);
        }


        String[] specificPath = new String[]{
                readiness ? "readiness" : "liveness",
                attribute
        };

        Optional<String> config = getValueFromConfig(specificPath).map(TRIM);
        if (!config.isPresent()) {
            // Generic path.
            return getValueFromConfig(attribute).map(TRIM);
        } else {
            return config;
        }

    }

    private Optional<List<String>> getListValue(String attribute, boolean readiness) {
        String[] path = new String[]{
                readiness ? "readiness" : "liveness",
                attribute
        };

        Optional<Object> element = getElement(path);
        if (!element.isPresent()) {
            element = getElement(attribute);
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
                    attribute, input.getClass(), input.toString()));
            }

        });
    }

    private Optional<Map<String, String>> getMapValue(String attribute, boolean readiness) {
        String[] path = new String[]{
                readiness ? "readiness" : "liveness",
                attribute
        };

        Optional<Object> element = getElement(path);
        if (!element.isPresent()) {
            element = getElement(attribute);
        }

        return element.map(input -> {
            if (input instanceof Map) {
                return (Map<String, String>) input;
            } else {
                throw new IllegalArgumentException(String.format(
                    ERROR_MESSAGE,
                    attribute, input.getClass(), input.toString()));
            }
        });
    }


    private Optional<Integer> getIntegerValue(String attribute, boolean readiness) {
        return getStringValue(attribute, readiness)
                .map(Integer::parseInt);
    }

    private Optional<String> getValueFromConfig(String... keys) {
        return getElement(keys).map(input -> {
            if (input instanceof String) {
                return (String) input;
            } else {
                throw new IllegalArgumentException(String.format(
                    ERROR_MESSAGE,
                Arrays.toString(keys), input.getClass(), input.toString()));
            }
        });
    }

    private Optional<Object> getElement(String... path) {
        final Optional<Map<String, Object>> configuration = getMavenPluginConfiguration();

        if (!configuration.isPresent()) {
            return Optional.empty();
        }


        String[] roots = new String[]{"enricher", "config", "jkube-healthcheck-vertx"};
        List<String> absolute = new ArrayList<>();
        absolute.addAll(Arrays.asList(roots));
        absolute.addAll(Arrays.asList(path));
        Object root = configuration.get();
        for (String key : absolute) {

            if (root instanceof Map) {
                Map<String, Object> rootMap = (Map<String, Object>) root;
                root = rootMap.get(key);
                if (root == null) {
                    return Optional.empty();
                }
            }

        }
        return Optional.of(root);
    }

    private Optional<Map<String, Object>> getMavenPluginConfiguration() {
        for(String pluginId : JKUBE_PLUGINS) {
            Optional<Map<String, Object>> configuration = getContext().getConfiguration().getPluginConfiguration("maven", pluginId);
            if(configuration != null && configuration.isPresent()) {
                return configuration;
            }
        }
        return Optional.empty();
    }

}
