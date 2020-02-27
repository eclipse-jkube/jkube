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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.generator.api.support.AbstractPortsExtractor;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
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
    private JkubeEnricherContext context;

    private void setupExpectations(Map<String, Object> config) {
        new Expectations() {{
            context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = true;

            Configuration.Builder configBuilder = new Configuration.Builder();
            configBuilder.pluginConfigLookup(getProjectLookup(config));

            context.getConfiguration();
            result = configBuilder.build();
        }};
    }

    private void setupExpectations(Properties props) {
        new Expectations() {{
            context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = true;

            Configuration.Builder configBuilder = new Configuration.Builder();
            configBuilder.properties(props);
            configBuilder.pluginConfigLookup(getProjectLookup(null));

            context.getConfiguration();
            result = configBuilder.build();
        }};
    }

    private void setupExpectations(Properties props, Map<String, Object> config) {
        new Expectations() {{
            context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = true;

            Configuration.Builder configBuilder = new Configuration.Builder();
            configBuilder.properties(props);
            configBuilder.pluginConfigLookup(getProjectLookup(config));

            context.getConfiguration();
            result = configBuilder.build();
        }};

    }

    @Test
    public void testDefaultConfiguration() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        setupExpectations(new Properties());

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
        assertEquals(probe.getHttpGet().getScheme(), "HTTP");
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 8080);
        assertEquals(probe.getHttpGet().getPath(), "/ping");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getScheme(), "HTTP");
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 8080);
        assertEquals(probe.getHttpGet().getPath(), "/ping");
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
        assertEquals(probe.getHttpGet().getScheme(), "HTTP");
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 8080);
        assertEquals(probe.getHttpGet().getPath(), "/ping");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getScheme(), "HTTP");
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 8080);
        assertEquals(probe.getHttpGet().getPath(), "/ready");
    }


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
    public void testWithCustomConfigurationComingFromConf() {

        final Map<String, Object> config = createFakeConfig("{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\"}");

        setupExpectations(config);

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getScheme(), "HTTPS");
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 1234);
        assertEquals(probe.getHttpGet().getPath(), "/health");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getScheme(), "HTTPS");
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 1234);
        assertEquals(probe.getHttpGet().getPath(), "/health");
    }

    private BiFunction<String, String, Optional<Map<String, Object>>> getProjectLookup(Map<String, Object> config) {
        return (s,i) -> {
            assertThat(s).isEqualTo("maven");
            return Optional.ofNullable(config);
        };
    }

    @Test
    public void testWithCustomConfigurationForLivenessAndReadinessComingFromConf() {
        final Map<String, Object> config = createFakeConfig(
                "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\",\"readiness\":{\"path\":\"/ready\"}}");

        setupExpectations(config);

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getScheme(), "HTTPS");
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 1234);
        assertEquals(probe.getHttpGet().getPath(), "/health");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getScheme(), "HTTPS");
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 1234);
        assertEquals(probe.getHttpGet().getPath(), "/ready");
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
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 8081);
        assertEquals(probe.getHttpGet().getPath(), "/health");

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getHttpGet().getScheme()).isEqualToIgnoringCase("https");
        assertNull(probe.getHttpGet().getHost());
        assertEquals(probe.getHttpGet().getPort().getIntVal().intValue(), 8081);
        assertEquals(probe.getHttpGet().getPath(), "/health");
    }

    @Test
    public void testWithHttpHeaders() {
        final Map<String, Object> config = createFakeConfig("{\"path\":\"health\",\"headers\":{\"X-Header\":\"X\",\"Y-Header\":\"Y\"}}");

        setupExpectations(config);

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getPath(), "/health");
        assertThat(probe.getHttpGet().getPort().getIntVal()).isEqualTo(8080);
        assertThat(probe.getHttpGet().getHttpHeaders()).hasSize(2)
                .contains(new HTTPHeader("X-Header", "X"), new HTTPHeader("Y-Header", "Y"));

        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getPath(), "/health");
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
    public void testDisabledUsingNegativePortUsingConfiguration() {
        final Map<String, Object> config = createFakeConfig(
                "{\"path\":\"/ping\",\"port\":\"-1\"}");

        setupExpectations(config);

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
        assertEquals(probe.getHttpGet().getPath(), "/ping");
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testReadinessDisabledUsingConfig() {

        final Map<String, Object> config = createFakeConfig(
                "{\"readiness\":{\"path\":\"\"},\"path\":\"/ping\"}");

        setupExpectations(config);

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getPath(), "/ping");
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
        assertEquals(probe.getHttpGet().getPath(), "/ping");

    }

    @Test
    public void testLivenessDisabledAndReadinessEnabledUsingConfig() {
        final Map<String, Object> config = createFakeConfig(
                "{\"readiness\":{\"path\":\"/ping\"},\"path\":\"\"}");
        setupExpectations(config);

        context.hasPlugin(VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP, VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT);

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getHttpGet().getPath(), "/ping");
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
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1234);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1235);
    }

    @Test
    public void testTCPSocketUsingConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1234\"},\"readiness\":{\"port\":\"1235\"}}");

        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1234);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1235);
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
        assertEquals(probe.getTcpSocket().getPort().getStrVal(), "health");
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getStrVal(), "ready");
    }

    @Test
    public void testTCPSocketUsingConfigAndPortName() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"tcp\",\"liveness\":{\"port-name\":\"health\"},\"readiness\":{\"port-name\":\"ready\"}}");
        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getStrVal(), "health");
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getStrVal(), "ready");
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
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1235);
    }

    @Test
    public void testTCPSocketUsingConfigLivenessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"tcp\",\"readiness\":{\"port\":\"1235\"}}");
        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1235);
    }

    @Test
    public void testTCPSocketUsingUserPropertiesReadinessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Properties props = new Properties();
        props.put("vertx.health.type", "tcp");
        props.put("vertx.health.port", "1235");
        props.put("vertx.health.readiness.port", "0");
        setupExpectations(props);

        Probe probe = enricher.getLivenessProbe();
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1235);
        assertNotNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testTCPSocketUsingConfigReadinessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1235\"},\"readiness\":{\"port\":\"-1\"}}");

        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertEquals(probe.getTcpSocket().getPort().getIntVal().intValue(), 1235);
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
    public void testTCPSocketUsingConfigIllegal() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"tcp\"," +
                        "\"liveness\":{\"port\":\"1234\",\"port-name\":\"foo\"}," +
                        "\"readiness\":{\"port\":\"1235\",\"port-name\":\"foo\"}}");
        setupExpectations(config);


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
    public void testTCPSocketUsingConfigDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
            "{\"type\":\"tcp\"}");
        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testExecUsingConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"exec\"," +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}" +
                        "}");
        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
    }

    @Test
    public void testExecUsingConfigLivenessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"exec\"," +
                        "\"readiness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"liveness\":{}" +
                        "}");

        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertThat(probe.getExec().getCommand()).hasSize(3);
    }

    @Test
    public void testExecUsingConfigReadinessDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"exec\"," +
                        "\"liveness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"readiness\":{}" +
                        "}");

        setupExpectations(config);

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
    public void testExecUsingConfigDisabled() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"exec\"}");

        setupExpectations(config);

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
    public void testUnknownTypeUsingConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"not a valid type\"}");

        setupExpectations(config);

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
    public void testThatWeCanUSeDifferentTypesForLivenessAndReadiness() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"path\":\"/ping\"}}");
        setupExpectations(config);

        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNotNull(probe.getExec());
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
    }

    @Test
    public void testThatGenericUserPropertiesOverrideSpecificConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"path\":\"/ping\"}}");

        Properties properties = new Properties();
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");
        setupExpectations(properties,config);


        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getHttpGet()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1234);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1234);
    }

    @Test
    public void testThatGenericUserPropertiesOverrideGenericConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}}"
        );
        Properties properties = new Properties();
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");

        setupExpectations(properties,config);

        Probe probe = enricher.getReadinessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1234);

        probe = enricher.getLivenessProbe();
        assertThat(probe).isNotNull();
        assertThat(probe.getTcpSocket()).isNotNull();
        assertThat(probe.getExec()).isNull();
        assertThat(probe.getTcpSocket().getPort().getIntVal()).isEqualTo(1234);
    }

    @Test
    public void testThatSpecificUserPropertiesOverrideSpecificConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"liveness\":{\"path\":\"/ping\"},\"readiness\":{\"path\":\"/ping\"}}");

        Properties properties = new Properties();
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        setupExpectations(properties, config);

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
    public void testThatSpecificUserPropertiesOverrideGenericConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"path\":\"/ping\",\"type\":\"http\"}");
        Properties properties = new Properties();
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        setupExpectations(properties, config);

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
    public void testThatSpecificConfigOverrideGenericConfig() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"liveness\":{\"path\":\"/live\"}," +
                        "\"readiness\":{\"path\":\"/ping\",\"port-name\":\"ready\"}," +
                        "\"path\":\"/health\",\"port-name\":\"health\"}");

        setupExpectations(config);

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
    public void testThatSpecificUserPropertiesOverrideGenericUserProperties() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        final Map<String, Object> config = createFakeConfig(
                "{\"path\":\"/ping\",\"type\":\"http\"}");
        Properties properties = new Properties();
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        properties.put("vertx.health.liveness.type", "tcp");
        properties.put("vertx.health.liveness.port", "1236");
        setupExpectations(properties, config);

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
