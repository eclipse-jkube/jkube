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
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.Probe;
import org.junit.Before;
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

    private JKubeEnricherContext context;
    private TreeMap<String, String> plexusMavenConfig;
    private Map<String, Object> jKubePluginConfiguration;
    private Properties properties;

    @Before
    public void setUp() throws Exception {
        plexusMavenConfig = new TreeMap<>();
        jKubePluginConfiguration = new HashMap<>();
        properties = new Properties();
        final ProcessorConfig processorConfig = new ProcessorConfig();
        processorConfig.setConfig(Collections.singletonMap("jkube-healthcheck-vertx", plexusMavenConfig));
        context = JKubeEnricherContext.builder()
            .log(new KitLogger.SilentLogger())
            .processorConfig(processorConfig)
            .project(createNewJavaProjectWithVertxPlugin("io.reactiverse", "vertx-maven-plugin"))
            .build();
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
        properties.put("vertx.health.path", "/ping");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        properties.put("vertx.health.path", "/ping");
        properties.put("vertx.health.readiness.path", "/ready");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        Map<String, Object> nestedConfig = Serialization.jsonMapper().readValue(config, Map.class);
        return nestedConfig.entrySet().stream()
            .filter(e -> e.getValue() instanceof String)
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), (String)e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (old, v) -> v, TreeMap::new));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createFakeConfig(String config) throws JsonProcessingException {
        Map<String, Object> healthCheckVertxMap = Serialization.jsonMapper().readValue(config, Map.class);

        Map<String, Object> enricherConfigMap = new HashMap<>();
        enricherConfigMap.put("jkube-healthcheck-vertx", healthCheckVertxMap);

        Map<String, Object> enricherMap = new HashMap<>();
        enricherMap.put("config", enricherConfigMap);

        Map<String, Object> pluginConfigurationMap = new HashMap<>();
        pluginConfigurationMap.put("enricher", enricherMap);

        return pluginConfigurationMap;
    }

    @Test
    public void testWithCustomConfigurationComingFromProcessorConf() throws Exception {
        String configString = "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\"}";
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(configString));

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

    @Test
    public void testWithCustomConfigurationForLivenessAndReadinessComingFromConf() throws Exception {
       final String config =
                "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\",\"readiness\":{\"path\":\"/ready\"}}";
       jKubePluginConfiguration.putAll(createFakeConfig(config));
       plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertThat(enricher.getLivenessProbe())
            .hasFieldOrPropertyWithValue("httpGet.host", null)
            .hasFieldOrPropertyWithValue("httpGet.scheme", "HTTPS")
            .hasFieldOrPropertyWithValue("httpGet.port.intVal", 1234)
            .hasFieldOrPropertyWithValue("httpGet.path", "/health");
        assertThat(enricher.getReadinessProbe())
            .hasFieldOrPropertyWithValue("httpGet.host", null)
            .hasFieldOrPropertyWithValue("httpGet.scheme", "HTTPS")
            .hasFieldOrPropertyWithValue("httpGet.port.intVal", 1234)
            .hasFieldOrPropertyWithValue("httpGet.path", "/ready");
    }

    @Test
    public void testCustomConfiguration() {
        properties.put("vertx.health.path", "/health");
        properties.put("vertx.health.port", " 8081 ");
        properties.put("vertx.health.scheme", " https");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

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
        properties.put("vertx.health.path", "");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testDisabledUsingNegativePort() {
        properties.put("vertx.health.port", " -1 ");
        properties.put("vertx.health.path", " /ping ");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testDisabledUsingInvalidPort() {
        properties.put("vertx.health.port", "not an integer");
        properties.put("vertx.health.path", " /ping ");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        properties.put("vertx.health.port-name", " health ");
        properties.put("vertx.health.path", " /ping ");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertThat(probe.getHttpGet().getPort().getStrVal()).isEqualToIgnoringCase("health");
        probe = enricher.getReadinessProbe();
        assertThat(probe.getHttpGet().getPort().getStrVal()).isEqualToIgnoringCase("health");
    }

    @Test
    public void testDisabledUsingNegativePortUsingConfiguration() throws Exception {
        final String config = "{\"path\":\"/ping\",\"port\":\"-1\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testReadinessDisabled() {
        properties.put("vertx.health.readiness.path", "");
        properties.put("vertx.health.path", "/ping");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testReadinessDisabledUsingConfig() throws Exception {
        final String config = "{\"readiness\":{\"path\":\"\"},\"path\":\"/ping\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testLivenessDisabledAndReadinessEnabled() {
        properties.put("vertx.health.readiness.path", "/ping");
        properties.put("vertx.health.path", "");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");

    }

    @Test
    public void testLivenessDisabledAndReadinessEnabledUsingConfig() throws Exception {
        final String config = "{\"readiness\":{\"path\":\"/ping\"},\"path\":\"\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getPath()).isEqualTo("/ping");
    }

    @Test
    public void testTCPSocketUsingUserProperties() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");
        properties.put("vertx.health.readiness.port", "1235");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(1234);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getIntVal().intValue()).isEqualTo(1235);
    }

    @Test
    public void testTCPSocketUsingUserPropertiesAndPortName() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port-name", "health");
        properties.put("vertx.health.readiness.port-name", "ready");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "health");
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "ready");
    }

    @Test
    public void testTCPSocketUsingConfigAndPortName() throws Exception {
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port-name\":\"health\"},\"readiness\":{\"port-name\":\"ready\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "health");
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getTcpSocket().getPort().getStrVal()).isEqualTo( "ready");
    }

    @Test
    public void testTCPSocketUsingUserPropertiesLivenessDisabled() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.readiness.port", "1235");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(1235, probe.getTcpSocket().getPort().getIntVal().intValue());
    }

    @Test
    public void testTCPSocketUsingConfigLivenessDisabled() throws Exception {
        final String config = "{\"type\":\"tcp\",\"readiness\":{\"port\":\"1235\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(1235, probe.getTcpSocket().getPort().getIntVal().intValue());
    }

    @Test
    public void testTCPSocketUsingUserPropertiesReadinessDisabled() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1337");
        properties.put("vertx.health.readiness.port", "0");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertEquals(1337, probe.getTcpSocket().getPort().getIntVal().intValue());
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testTCPSocketUsingConfigReadinessDisabled() throws Exception {
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1235\"},\"readiness\":{\"port\":\"-1\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertEquals(1235, probe.getTcpSocket().getPort().getIntVal().intValue());
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTCPSocketUsingUserPropertiesIllegal() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1235");
        properties.put("vertx.health.port-name", "health");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        enricher.getLivenessProbe();
    }

    @Test
    public void testTCPSocketUsingConfigIllegal() throws Exception {
        final String config = "{\"type\":\"tcp\"," +
                        "\"liveness\":{\"port\":\"1234\",\"port-name\":\"foo\"}," +
                        "\"readiness\":{\"port\":\"1235\",\"port-name\":\"foo\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        properties.put("vertx.health.type", "tcp");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testTCPSocketUsingConfigDisabled() throws Exception {
        final String config = "{\"type\":\"tcp\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingConfig() throws Exception {
        final String config = "{\"type\":\"exec\"," +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}" +
                        "}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
    }

    @Test
    public void testExecUsingConfigLivenessDisabled() throws Exception {
        final String config = "{\"type\":\"exec\"," +
                        "\"readiness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"liveness\":{}" +
                        "}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
    }

    @Test
    public void testExecUsingConfigReadinessDisabled() throws Exception {
        final String config = "{\"type\":\"exec\"," +
                        "\"liveness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"readiness\":{}" +
                        "}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertThat(probe.getExec().getCommand()).hasSize(3);
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingUserPropertiesDisabled() {
        properties.put("vertx.health.type", "exec");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingConfigDisabled() throws Exception {
        final String config = "{\"type\":\"exec\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testUnknownTypeUsingUserProperties() {
        properties.put("vertx.health.type", "not a valid type");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        final String config = "{\"type\":\"not a valid type\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        context.getProject().setPlugins(Collections.emptyList());

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testApplicableProjectWithGradle() {
        properties.put("vertx.health.path", "/ping");
        context = context.toBuilder()
            .project(createNewJavaProjectWithVertxPlugin("io.vertx", "io.vertx.vertx-plugin"))
            .build();
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);


        Probe probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
    }

    @Test
    public void testThatWeCanUSeDifferentTypesForLivenessAndReadiness() throws Exception {
        final String config = "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"path\":\"/ping\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNotNull(probe.getExec());
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
    }

    @Test
    public void testThatSpecificConfigOverrideGenericUserProperties() throws Exception {
        final String config = "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"port\":\"1337\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");
        properties.put("vertx.health.path", "/path");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        final String config = "{\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        final String config = "{\"liveness\":{\"type\":\"http\",\"path\":\"/ping\"},\"readiness\":{\"port\":\"1337\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.path", "/pong");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        final String config = "{\"path\":\"/ping\",\"type\":\"http\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        final String config = "{\"liveness\":{\"path\":\"/live\"}," +
                        "\"readiness\":{\"path\":\"/ping\",\"port-name\":\"ready\"}," +
                        "\"path\":\"/health\",\"port-name\":\"health\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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
        final String config =  "{\"path\":\"/ping\",\"type\":\"http\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        properties.put("vertx.health.liveness.type", "tcp");
        properties.put("vertx.health.liveness.port", "1236");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

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

    private JavaProject createNewJavaProjectWithVertxPlugin(String vertxGroup, String vertxArtifact) {
        return JavaProject.builder()
            .properties(properties)
            .plugin(Plugin.builder()
                .groupId(vertxGroup)
                .artifactId(vertxArtifact)
                .build())
            .plugin(Plugin.builder()
                .groupId("org.eclipse.jkube")
                .artifactId("kubernetes-maven-plugin")
                .configuration(jKubePluginConfiguration)
                .build())
            .build();
    }

}
