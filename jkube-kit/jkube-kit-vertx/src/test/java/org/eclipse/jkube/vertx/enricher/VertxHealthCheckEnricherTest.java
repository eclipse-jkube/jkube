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
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.Probe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
class VertxHealthCheckEnricherTest {

    private JKubeEnricherContext context;
    private TreeMap<String, String> plexusMavenConfig;
    private Map<String, Object> jKubePluginConfiguration;
    private Properties properties;

    @BeforeEach
    void setUp() {
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

    @Nested
    @DisplayName("with TCP health type")
    class TCPHealthType {
      @Test
      @DisplayName("and port using properties, should enable probes with configured port")
      void andPortUsingProperties_shouldConfigureProbesWithPort() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");
        properties.put("vertx.health.readiness.port", "1235");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertTCPSocket(livenessProbe, 1234);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertTCPSocket(readinessProbe, 1235);
      }

      @Test
      @DisplayName("and port using config, should enable probes with configured port")
      void andPortUsingConfig_shouldConfigureProbesWithPort() throws Exception {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1234\"},\"readiness\":{\"port\":\"1235\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        Probe livenessProbe = enricher.getLivenessProbe();
        assertTCPSocket(livenessProbe, 1234);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertTCPSocket(readinessProbe, 1235);
      }

      @Test
      @DisplayName("and port name using properties, should enable probes with configured port name")
      void andPortNameUsingProperties_shouldConfigureProbesWithPortName() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port-name", "health");
        properties.put("vertx.health.readiness.port-name", "ready");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket.port.strVal", "health");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket.port.strVal", "ready");
      }

      @Test
      @DisplayName("and port name using config, should enable probes with configured port name")
      void andPortNameUsingConfig_shouldConfigureProbesWithPortName() throws Exception {
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port-name\":\"health\"},\"readiness\":{\"port-name\":\"ready\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket.port.strVal", "health");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket.port.strVal", "ready");
      }

      @Test
      @DisplayName("and without liveness probe using properties, should disable liveness probe")
      void andWithoutLivenessProbeUsingProperties_shouldDisableLiveness() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.readiness.port", "1235");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNull();

        Probe readinessProbe = enricher.getReadinessProbe();
        assertTCPSocket(readinessProbe, 1235);
      }

      @Test
      @DisplayName("and without liveness probe using config, should disable liveness probe")
      void andWithoutLivenessProbeUsingConfig_shouldDisableLiveness() throws Exception {
        final String config = "{\"type\":\"tcp\",\"readiness\":{\"port\":\"1235\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNull();

        Probe readinessProbe = enricher.getReadinessProbe();
        assertTCPSocket(readinessProbe, 1235);
      }

      @Test
      @DisplayName("and negative readiness port using properties, should disable readiness probe")
      void andNegativeReadinessPortUsingProperties_shouldDisableReadiness() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1337");
        properties.put("vertx.health.readiness.port", "-1");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertTCPSocket(livenessProbe, 1337);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNull();
      }

      @Test
      @DisplayName("and negative readiness port using config, should disable readiness probe")
      void andNegativeReadinessPortUsingConfig_shouldDisableReadiness() throws Exception {
        final String config = "{\"type\":\"tcp\",\"liveness\":{\"port\":\"1235\"},\"readiness\":{\"port\":\"-1\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertTCPSocket(livenessProbe, 1235);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNull();
      }

      @Test
      @DisplayName("port and port name both using properties, should throw exception")
      void portAndPortNameUsingProperties_shouldThrowException() {
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1235");
        properties.put("vertx.health.port-name", "health");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getLivenessProbe)
            .withMessageContaining("Invalid health check configuration");

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getReadinessProbe)
            .withMessageStartingWith("Invalid health check configuration");
      }

      @Test
      @DisplayName("port and port name both using config, should throw exception")
      void portAndPortNameUsingConfig_shouldThrowException() throws Exception {
        final String config = "{\"type\":\"tcp\"," +
            "\"liveness\":{\"port\":\"1234\",\"port-name\":\"foo\"}," +
            "\"readiness\":{\"port\":\"1235\",\"port-name\":\"foo\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getLivenessProbe)
            .withMessageStartingWith("Invalid health check configuration");

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getReadinessProbe)
            .withMessageStartingWith("Invalid health check configuration");
      }

    }

    @Test
    @DisplayName("with empty configuration, should disable probes")
    void withEmptyConfig_shouldDisableProbes() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertNoLivenessReadinessProbes(enricher);
    }

    @Test
    @DisplayName("default configuration with path, should enable probes with configured path")
    void defaultConfigurationWithPath_shouldEnableProbes() {
        properties.put("vertx.health.path", "/ping");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHTTPGet(livenessProbe, "HTTP", 8080, "/ping");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHTTPGet(readinessProbe, "HTTP", 8080, "/ping");
    }

    @Test
    @DisplayName("with different paths using properties, should enable probes with different paths")
    void differentPathForLivenessAndReadinessProbes() {
        properties.put("vertx.health.path", "/ping");
        properties.put("vertx.health.readiness.path", "/ready");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHTTPGet(livenessProbe, "HTTP", 8080, "/ping");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHTTPGet(readinessProbe, "HTTP", 8080, "/ready");
    }

    @Test
    @DisplayName("with custom configuration coming from processor, should enable probes with config")
    void withCustomConfigurationComingFromProcessorConf() throws Exception {
        String configString = "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\"}";
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(configString));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHTTPGet(livenessProbe, "HTTPS", 1234, "/health");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHTTPGet(readinessProbe, "HTTPS", 1234, "/health");
    }

    @Test
    @DisplayName("with custom configuration for liveness and readiness probe coming from config, should enable probes with config")
    void withCustomConfigurationForLivenessAndReadinessComingFromConf() throws Exception {
       final String config =
                "{\"path\":\"health\",\"port\":\"1234\",\"scheme\":\"https\",\"readiness\":{\"path\":\"/ready\"}}";
       jKubePluginConfiguration.putAll(createFakeConfig(config));
       plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

       VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

       Probe livenessProbe = enricher.getLivenessProbe();
       assertHTTPGet(livenessProbe, "HTTPS", 1234, "/health");

       Probe readinessProbe = enricher.getReadinessProbe();
       assertHTTPGet(readinessProbe, "HTTPS", 1234, "/ready");
    }

    @Test
    @DisplayName("with custom configuration coming from properties, should enable probes with config")
    void withCustomConfigurationFromProperties() {
        properties.put("vertx.health.path", "/health");
        properties.put("vertx.health.port", " 8081 ");
        properties.put("vertx.health.scheme", " https");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHTTPGet(livenessProbe, "HTTPS", 8081, "/health");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHTTPGet(readinessProbe, "HTTPS", 8081, "/health");
    }

    @Test
    @DisplayName("with HTTP headers, should enable probes with headers")
    void withHttpHeaders() throws Exception {
        final String config = "{\"path\":\"health\",\"headers\":{\"X-Header\":\"X\",\"Y-Header\":\"Y\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHTTPGet(livenessProbe, "HTTP", 8080, "/health");
        assertThat(livenessProbe.getHttpGet().getHttpHeaders()).hasSize(2)
                .contains(new HTTPHeader("X-Header", "X"), new HTTPHeader("Y-Header", "Y"));

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHTTPGet(readinessProbe, "HTTP", 8080, "/health");
        assertThat(readinessProbe.getHttpGet().getHttpHeaders()).hasSize(2)
                .contains(new HTTPHeader("X-Header", "X"), new HTTPHeader("Y-Header", "Y"));
    }

    @Test
    @DisplayName("with negative port using property, should disable probes")
    void withNegativePort_shouldDisableProbes() {
        properties.put("vertx.health.port", " -1 ");
        properties.put("vertx.health.path", " /ping ");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertNoLivenessReadinessProbes(enricher);
    }

    @Test
    @DisplayName("with invalid port using property, should throw exception")
    void withInvalidPort_shouldThrowException() {
        properties.put("vertx.health.port", "not an integer");
        properties.put("vertx.health.path", " /ping ");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(enricher::getLivenessProbe)
            .withMessageContaining("not an integer");

        assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(enricher::getReadinessProbe)
            .withMessageContaining("not an integer");
    }

    @Test
    @DisplayName("with port name, should disable probes ")
    void withPortName_shouldDisableProbes() {
        properties.put("vertx.health.port-name", " health ");
        properties.put("vertx.health.path", " /ping ");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe.getHttpGet().getPort().getStrVal()).isEqualToIgnoringCase("health");
        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe.getHttpGet().getPort().getStrVal()).isEqualToIgnoringCase("health");
    }

    @DisplayName("using invalid properties")
    @ParameterizedTest(name = "{index}: with ''{0}'' should disable probes")
    @MethodSource("invalidProperties")
    void withInvalidProperties_shouldDisableProbes(String description, String property, String propVal) {
        // Given
        properties.put(property, propVal);
        // When
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        // Then
        assertNoLivenessReadinessProbes(enricher);
    }

    static Stream<Arguments> invalidProperties() {
      return Stream.of(
          arguments("empty health path", "vertx.health.path", ""),
          arguments("TCP health type", "vertx.health.type", "tcp"),
          arguments("exec health type", "vertx.health.type", "exec"));
    }

    @DisplayName("using invalid config")
    @ParameterizedTest(name = "{index}: with ''{0}'' should disable probes")
    @MethodSource("invalidConfigs")
    void withInvalidConfigs_shouldDisableProbes(String description, String config) throws Exception {
        // Given
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        // When
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        // Then
        assertNoLivenessReadinessProbes(enricher);
    }

    static Stream<Arguments> invalidConfigs() {
      return Stream.of(
          arguments("negative port", "{\"path\":\"/ping\",\"port\":\"-1\"}"),
          arguments("TCP health type", "{\"type\":\"tcp\"}"),
          arguments("exec health type", "{\"type\":\"exec\"}"));
    }

    @Test
    @DisplayName("with empty readiness and non-empty health path using properties, should disable readiness probe")
    void withEmptyReadinessAndNonEmptyHealthPathUsingProperties_shouldDisableReadiness() {
        properties.put("vertx.health.readiness.path", "");
        properties.put("vertx.health.path", "/ping");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet.path", "/ping");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNull();
    }

    @Test
    @DisplayName("with empty readiness and non-empty health path using config, should disable readiness probe")
    void withEmptyReadinessAndNonEmptyHealthPathUsingConfig_shouldDisableReadiness() throws Exception {
        final String config = "{\"readiness\":{\"path\":\"\"},\"path\":\"/ping\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet.path", "/ping");

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNull();
    }

    @Test
    @DisplayName("with empty health path and non-empty readiness path using properties, should disable liveness and enable readiness probe")
    void withEmptyHealthAndNonEmptyReadinessPathUsingProperties_shouldDisableLivenessAndEnableReadiness() {
        properties.put("vertx.health.readiness.path", "/ping");
        properties.put("vertx.health.path", "");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNull();

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet.path", "/ping");
    }

    @Test
    @DisplayName("with empty health path and non-empty readiness path using config, should disable liveness and enable readiness probe")
    void withEmptyHealthAndNonEmptyReadinessPathUsingConfig_shouldDisableLivenessAndEnableReadiness() throws Exception {
        final String config = "{\"readiness\":{\"path\":\"/ping\"},\"path\":\"\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNull();

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("httpGet.path", "/ping");
    }

    @Test
    @DisplayName("with exec type using config, should enable probes with configured exec")
    void withExecTypeUsingConfig_shouldConfigureProbesWithExec() throws Exception {
        final String config = "{\"type\":\"exec\"," +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}" +
                        "}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .extracting(Probe::getExec)
            .extracting(ExecAction::getCommand)
            .asList()
            .hasSize(3);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .extracting(Probe::getExec)
            .extracting(ExecAction::getCommand)
            .asList()
            .hasSize(3);
    }

    @Test
    @DisplayName("with exec type and readiness using config, should disable liveness probe")
    void withExecTypeReadinessUsingConfig_shouldDisableLiveness() throws Exception {
        final String config = "{\"type\":\"exec\"," +
                        "\"readiness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"liveness\":{}" +
                        "}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNull();

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .extracting(Probe::getExec)
            .extracting(ExecAction::getCommand).asList()
            .hasSize(3);
    }

    @Test
    @DisplayName("with exec type and liveness using config, should disable readiness probe")
    void withExecTypeLivenessUsingConfig_shouldDisableReadiness() throws Exception {
        final String config = "{\"type\":\"exec\"," +
                        "\"liveness\":{" +
                        "\"command\": {\"arg\":[\"/bin/sh\", \"-c\",\"touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy; sleep 600\"]}}," +
                        "\"readiness\":{}" +
                        "}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .extracting(Probe::getExec)
            .extracting(ExecAction::getCommand).asList()
            .hasSize(3);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNull();
    }

    @Test
    @DisplayName("with invalid type using properties, should throw exception")
    void withInvalidTypeUsingProperties_shouldThrowException() {
        properties.put("vertx.health.type", "not a valid type");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getLivenessProbe)
            .withMessageContaining("Invalid health check configuration");

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getReadinessProbe)
            .withMessageContaining("Invalid health check configuration");
    }

    @Test
    @DisplayName("with invalid type using config, should throw exception")
    void withInvalidTypeUsingConfig_shouldThrowException() throws Exception {
        final String config = "{\"type\":\"not a valid type\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getLivenessProbe)
            .withMessageContaining("Invalid health check configuration");

        assertThatIllegalArgumentException()
            .isThrownBy(enricher::getReadinessProbe)
            .withMessageContaining("Invalid health check configuration");
    }

    @Test
    @DisplayName("with no project, vertx enricher should not be applicable")
    void witNoPlugin_enricherShouldNotBeApplicable() {
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);
        context.getProject().setPlugins(Collections.emptyList());

        assertNoLivenessReadinessProbes(enricher);
    }

    @Test
    @DisplayName("with gradle plugin, vertx enricher should be applicable")
    void withGradlePlugin_enricherShouldBeApplicable() {
        properties.put("vertx.health.path", "/ping");
        context = context.toBuilder()
            .project(createNewJavaProjectWithVertxPlugin("io.vertx", "io.vertx.vertx-plugin"))
            .build();
        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull();
        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull();
    }

    @Test
    @DisplayName("with different types for liveness and readiness probes, should enable probes with configured types")
    void withDifferentTypesForLivenessAndReadiness_shouldConfigureProbesWithConfiguredTypes() throws Exception {
        final String config = "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"path\":\"/ping\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .extracting(Probe::getExec)
            .isNotNull();

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .extracting(Probe::getHttpGet)
            .isNotNull();
    }

    @Test
    @DisplayName("with specific config, should override generic user properties")
    void specificConfigOverrideGenericUserProperties() throws Exception {
        final String config = "{\"liveness\":{" +
                        "\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}" +
                        "},\"readiness\":{\"port\":\"1337\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");
        properties.put("vertx.health.path", "/path");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe.getHttpGet()).isNull();
        assertTCPSocket(readinessProbe, 1337);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getExec).isNotNull()
            .extracting(ExecAction::getCommand).asList()
            .singleElement()
            .isEqualTo("ls");
    }

    @Test
    @DisplayName("with generic config, should override generic user properties")
    void withGenericConfig_shouldOverrideGenericUserProperties() throws Exception {
        final String config = "{\"type\":\"exec\",\"command\":{\"arg\":\"ls\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.type", "tcp");
        properties.put("vertx.health.port", "1234");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getExec).isNotNull()
            .extracting(ExecAction::getCommand).asList()
            .singleElement()
            .isEqualTo("ls");

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getExec).isNotNull()
            .extracting(ExecAction::getCommand).asList()
            .singleElement()
            .isEqualTo("ls");
    }

    @Test
    @DisplayName("with specific config, should override specific user properties")
    void withSpecificConfig_shouldOverrideSpecificUserProperties() throws Exception {
        final String config = "{\"liveness\":{\"type\":\"http\",\"path\":\"/ping\"},\"readiness\":{\"port\":\"1337\"}}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.path", "/pong");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe.getHttpGet()).isNull();
        assertTCPSocket(readinessProbe, 1337);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getHttpGet)
            .isNotNull()
            .hasFieldOrPropertyWithValue("path", "/ping");
    }

    @Test
    @DisplayName("with specific user properties, should override generic config")
    void withSpecificUserProperties_shouldOverrideGenericConfig() throws Exception {
        final String config = "{\"path\":\"/ping\",\"type\":\"http\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe.getHttpGet()).isNull();
        assertTCPSocket(readinessProbe, 1234);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .hasFieldOrPropertyWithValue("tcpSocket", null)
            .extracting(Probe::getHttpGet)
            .isNotNull()
            .hasFieldOrPropertyWithValue("port.intVal", 1235);
    }

    @Test
    @DisplayName("with specific config, should override generic config")
    void withSpecificConfig_shouldOverrideGenericConfig() throws Exception {
        final String config = "{\"liveness\":{\"path\":\"/live\"}," +
                        "\"readiness\":{\"path\":\"/ping\",\"port-name\":\"ready\"}," +
                        "\"path\":\"/health\",\"port-name\":\"health\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe).isNotNull()
            .extracting(Probe::getHttpGet)
            .isNotNull()
            .hasFieldOrPropertyWithValue("port.strVal", "ready")
            .hasFieldOrPropertyWithValue("path", "/ping");

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe).isNotNull()
            .extracting(Probe::getHttpGet)
            .isNotNull()
            .hasFieldOrPropertyWithValue("port.strVal", "health")
            .hasFieldOrPropertyWithValue("path", "/live");
    }

    @Test
    @DisplayName("with specific user properties, should override generic user properties")
    void withSpecificUserProperties_shouldOverrideGenericUserProperties() throws Exception {
        final String config =  "{\"path\":\"/ping\",\"type\":\"http\"}";
        jKubePluginConfiguration.putAll(createFakeConfig(config));
        plexusMavenConfig.putAll(createFakeConfigLikeMaven(config));
        properties.put("vertx.health.readiness.type", "tcp");
        properties.put("vertx.health.readiness.port", "1234");
        properties.put("vertx.health.port", "1235");
        properties.put("vertx.health.liveness.type", "tcp");
        properties.put("vertx.health.liveness.port", "1236");

        VertxHealthCheckEnricher enricher = new VertxHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertThat(readinessProbe.getHttpGet()).isNull();
        assertTCPSocket(readinessProbe, 1234);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertThat(livenessProbe.getHttpGet()).isNull();
        assertTCPSocket(livenessProbe, 1236);
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

    private void assertNoLivenessReadinessProbes(VertxHealthCheckEnricher enricher) {
      assertThat(enricher)
          .returns(null, VertxHealthCheckEnricher::getLivenessProbe)
          .returns(null, VertxHealthCheckEnricher::getReadinessProbe);
    }

    private void assertHTTPGet(Probe probe, String scheme, int port, String path) {
      assertThat(probe).isNotNull()
          .extracting(Probe::getHttpGet)
          .hasFieldOrPropertyWithValue("host", null)
          .hasFieldOrPropertyWithValue("scheme", scheme)
          .hasFieldOrPropertyWithValue("port.intVal", port)
          .hasFieldOrPropertyWithValue("path", path);
    }

    private void assertTCPSocket(Probe probe, int port) {
      assertThat(probe).isNotNull()
          .extracting(Probe::getTcpSocket)
          .isNotNull()
          .hasFieldOrPropertyWithValue("port.intVal", port);
    }

}
