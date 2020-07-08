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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.jkube.generator.api.support.AbstractPortsExtractor;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.Probe;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxHealthCheckEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    private void setupExpectations(Map<String, Object> config, TreeMap<String, String> plexusMavenConfig) {
        final ProcessorConfig processorConfig = new ProcessorConfig();
        processorConfig.setConfig(Collections.singletonMap("jkube-healthcheck-vertx", plexusMavenConfig));
        // @formatter:off
        new Expectations() {{
            context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = true;
            context.getConfiguration().getProcessorConfig(); result = processorConfig; minTimes = 0;
            context.getConfiguration().getPluginConfiguration(anyString, anyString); result = Optional.of(config);
            context.getConfiguration().getPluginConfigLookup(); result = getProjectLookup(config); minTimes = 0;
        }};
        // @formatter:on
    }

    private void setupExpectations(Properties props) {
        // @formatter:off
        new Expectations() {{
            context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = true;

            context.getProperties(); result = props;
        }};
        // @formatter:on
    }

    private void setupExpectations(Properties props, Map<String, Object> config, TreeMap<String, String> plexusMavenConfig) {
        setupExpectations(props);
        setupExpectations(config, plexusMavenConfig);
    }

    @Test
    public void testDefaultConfiguration() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testDefaultConfiguration_Enabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.path", "/ping");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/ping");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/ping");
    }

    @Test
    public void testDifferentPathForLivenessAndReadiness() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.path", "/ping");
        props.put("vertx.health.readiness.path", "/ready");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/ping");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTP");
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/ready");
    }

    @SuppressWarnings("unchecked")
    private TreeMap<String, String> createFakeConfigLikeMaven(String config) throws Exception {
        Map<String, Object> nestedConfig = AbstractPortsExtractor.JSON_MAPPER.readValue(config, Map.class);
        return nestedConfig.entrySet().stream()
            .filter(e -> e.getValue() instanceof String)
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), (String)e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (old, v) -> v, TreeMap::new));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createFakeConfig(String config) {
        try {
            Map<String, Object> healthCheckVertxMap = AbstractPortsExtractor.JSON_MAPPER.readValue(config, Map.class);

            Map<String, Object> enricherConfigMap = new HashMap<>();
            enricherConfigMap.put("jkube-healthcheck-vertx", healthCheckVertxMap);

            Map<String, Object> enricherMap = new HashMap<>();
            enricherMap.put("config", enricherConfigMap);

            Map<String, Object> pluginConfigurationMap = new HashMap<>();
            pluginConfigurationMap.put("enricher", enricherMap);

            return pluginConfigurationMap;
        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
        }
        return null;
    }

    @Test
    public void testWithCustomConfigurationComingFromConf() throws Exception {
        String configString = "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\"}";
        setupExpectations(createFakeConfig(configString), createFakeConfigLikeMaven(configString));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTPS");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(1234);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTPS");
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(1234);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");
    }

    private BiFunction<String, String, Optional<Map<String, Object>>> getProjectLookup(Map<String, Object> config) {
        return (s,i) -> {
            assertThat(s).isEqualTo("maven");
            return Optional.ofNullable(config);
        };
    }

    @Test
    public void testWithCustomConfigurationForLivenessAndReadinessComingFromConf() throws Exception {
       final String config =
                "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\",\"readiness\":{\"path\":\"/ready\"}}";

        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTPS");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(1234);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getScheme()).isEqualTo("HTTPS");
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(1234);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/ready");
    }

    @Test
    public void testCustomConfiguration() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.path", "/health");
        props.put("vertx.health.port", " 8081 ");
        props.put("vertx.health.scheme", " https");

        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getScheme()).isEqualToIgnoringCase("https");
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8081);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getScheme()).isEqualToIgnoringCase("https");
        assertNull(probe.getHttpGet().getHost());
        assertThat(probe.getHttpGet().getPort().getIntVal().intValue()).isEqualTo(8081);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");
    }

    @Test
    public void testWithHttpHeaders() throws Exception {
        final String config = "{\"path\":\"health\",\"headers\":{\"X-Header\":\"X\",\"Y-Header\":\"Y\"}}";

        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getHttpHeaders()).hasSize(2)
                .contains(new HTTPHeader("X-Header", "X"), new HTTPHeader("Y-Header", "Y"));

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo( "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getHttpHeaders()).hasSize(2)
                .contains(new HTTPHeader("X-Header", "X"), new HTTPHeader("Y-Header", "Y"));
    }

    @Test
    public void testDisabledUsingEmptyPath() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.path", "");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testDisabledUsingNegativePort() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.port", " -1 ");
        props.put("vertx.health.path", " /ping ");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testDisabledUsingInvalidPort() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.port", "not an integer");
        props.put("vertx.health.path", " /ping ");
        setupExpectations(props);

        try {
            enricher.getLivenessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }

        try {
            enricher.getReadinessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }
    }

    @Test
    public void testDisabledUsingPortName() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.port-name", " health ");
        props.put("vertx.health.path", " /ping ");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertThat(probe.getHttpGet().getPort().getStrVal()).isEqualToIgnoringCase("health");
        probe = enricher.getReadinessProbe();
        assertThat(probe.getHttpGet().getPort().getStrVal()).isEqualToIgnoringCase("health");
    }

    @Test
    public void testDisabledUsingNegativePortUsingConfiguration() throws Exception {
        final String config = "{\"path\":\"/ping\",\"port\":\"-1\"}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testReadinessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.readiness.path", "");
        props.put("vertx.health.path", "/ping");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testReadinessDisabledUsingConfig() throws Exception {
        final String config = "{\"readiness\":{\"path\":\"\"},\"path\":\"/ping\"}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testLivenessDisabledAndReadinessEnabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.readiness.path", "/ping");
        props.put("vertx.health.path", "");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");

    }

    @Test
    public void testLivenessDisabledAndReadinessEnabledUsingConfig() throws Exception {
        final String config = "{\"readiness\":{\"path\":\"/ping\"},\"path\":\"\"}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
    }

    @Test
    public void testTCPSocketUsingUserProperties() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        props.put("vertx.health.port", "1234");
        props.put("vertx.health.readiness.port", "1235");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(1234);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(1235);
    }

    @Test
    public void testTCPSocketUsingConfig() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1234\"},\"readiness\":{\"port\":\"1235\"}}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(1234);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(1235);
    }

    @Test
    public void testTCPSocketUsingUserPropertiesAndPortName() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        props.put("vertx.health.port-name", "health");
        props.put("vertx.health.readiness.port-name", "ready");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "health");
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "ready");
    }

    @Test
    public void testTCPSocketUsingConfigAndPortName() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port-name\":\"health\"},\"readiness\":{\"port-name\":\"ready\"}}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "health");
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "ready");
    }

    @Test
    public void testTCPSocketUsingUserPropertiesLivenessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        props.put("vertx.health.readiness.port", "1235");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(1235, probe.getTcpSocket().getPort().getIntVal().intValue());
    }

    @Test
    public void testTCPSocketUsingConfigLivenessDisabled() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\",\"readiness\":{\"port\":\"1235\"}}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(1235, probe.getTcpSocket().getPort().getIntVal().intValue());
    }

    @Test
    public void testTCPSocketUsingUserPropertiesReadinessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        props.put("vertx.health.port", "1337");
        props.put("vertx.health.readiness.port", "0");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertEquals(1337, probe.getTcpSocket().getPort().getIntVal().intValue());
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testTCPSocketUsingConfigReadinessDisabled() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1235\"},\"readiness\":{\"port\":\"-1\"}}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertEquals(1235, probe.getTcpSocket().getPort().getIntVal().intValue());
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTCPSocketUsingUserPropertiesIllegal() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        props.put("vertx.health.port", "1235");
        props.put("vertx.health.port-name", "health");
        setupExpectations(props);

        enricher.getLivenessProbe();
    }

    @Test
    public void testTCPSocketUsingConfigIllegal() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\"," +
                        "\"liveness\":{\"port\":\"1234\",\"port-name\":\"foo\"}," +
                        "\"readiness\":{\"port\":\"1235\",\"port-name\":\"foo\"}}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));


        try {
            enricher.getLivenessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }

        try {
            enricher.getReadinessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }
    }


    @Test
    public void testTCPSocketUsingUserPropertiesDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testTCPSocketUsingConfigDisabled() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\"}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingConfig() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"exec\"," +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}" +
                        "}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
    }

    @Test
    public void testExecUsingConfigLivenessDisabled() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"exec\"," +
                        "\"readiness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"liveness\":{}" +
                        "}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
    }

    @Test
    public void testExecUsingConfigReadinessDisabled() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"exec\"," +
                        "\"liveness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"readiness\":{}" +
                        "}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertThat(probe.getExec().getCommand()).hasSize(3);
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingUserPropertiesDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "exec");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingConfigDisabled() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"exec\"}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testUnknownTypeUsingUserProperties() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "not a valid type");
        setupExpectations(props);

        try {
            enricher.getLivenessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }

        try {
            enricher.getReadinessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }
    }


    @Test
    public void testUnknownTypeUsingConfig() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"not a valid type\"}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        try {
            enricher.getLivenessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }

        try {
            enricher.getReadinessProbe();
            fail("Illegal configuration not detected");
        } catch (Exception e) {
            // OK.
        }
    }

    @Test
    public void testNotApplicableProject() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        new Expectations() {{
            context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = false;
        }};

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testThatWeCanUSeDifferentTypesForLivenessAndReadiness() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final String config = "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"path\":\"/ping\"}}";
        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNotNull(probe.getExec());
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
    }

    @Test
    public void testThatSpecificConfigOverrideGenericUserProperties() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final String config = "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"port\":\"1337\"}}";

        Properties properties = new Properties();
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");
        properties.put("vertx.health.path", "/path");
        setupExpectations(properties, createFakeConfig(config), createFakeConfigLikeMaven(config));


        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1337);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getExec()).isNotNull();
        assertThat(probe.getExec().getCommand().iterator().next()).isEqualTo("ls");
    }

    @Test
    public void testThatGenericConfigOverrideGenericUserProperties() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final String config = "{\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}}";
        Properties properties = new Properties();
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");

        setupExpectations(properties, createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getExec()).isNotNull();
        assertThat(probe.getExec().getCommand().iterator().next()).isEqualTo("ls");

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getExec()).isNotNull();
        assertThat(probe.getExec().getCommand().iterator().next()).isEqualTo("ls");
    }

    @Test
    public void testThatSpecificConfigOverrideSpecificUserProperties() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final String config = "{\"liveness\":{\"type\":\"http\",\"path\":\"/ping\"},\"readiness\":{\"port\":\"1337\"}}";
        Properties properties = new Properties();
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.path", "/pong");
        setupExpectations(properties, createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1337);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
    }

    @Test
    public void testThatSpecificUserPropertiesOverrideGenericConfig() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final String config = "{\"path\":\"/ping\",\"type\":\"http\"}";
        Properties properties = new Properties();
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        setupExpectations(properties, createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1234);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getTcpSocket()).isNull();
        assertThat(probe.getHttpGet().getPort().getIntVal()).isEqualTo(1235);
    }

    @Test
    public void testThatSpecificConfigOverrideGenericConfig() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final String config = "{\"liveness\":{\"path\":\"/live\"}," +
                        "\"readiness\":{\"path\":\"/ping\",\"port-name\":\"ready\"}," +
                        "\"path\":\"/health\",\"port-name\":\"health\"}";

        setupExpectations(createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPort().getStrVal()).isEqualTo("ready");
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNotNull();
        assertThat(probe.getHttpGet().getPort().getStrVal()).isEqualTo("health");
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/live");
    }

    @Test
    public void testThatSpecificUserPropertiesOverrideGenericUserProperties() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config =  "{\"path\":\"/ping\",\"type\":\"http\"}";
        Properties properties = new Properties();
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        properties.put("vertx.health.liveness.type", "tcp");
        properties.put("vertx.health.liveness.port", "1236");
        setupExpectations(properties, createFakeConfig(config), createFakeConfigLikeMaven(config));

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1234);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1236);
    }

}
