/*
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
package org.eclipse.jkube.kit.common.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.fabric8.kubernetes.api.model.NamedContextBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;

class ClusterConfigurationTest {

  private String originalKubeConfig;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    originalKubeConfig = System.getProperty(Config.KUBERNETES_KUBECONFIG_FILE);
    final Path kubeConfig = temporaryFolder.resolve("config");
    Files.write(kubeConfig, String.join("\n",
      "apiVersion: v1",
      "kind: Config",
      "current-context: dev-context",
      "clusters:",
      "- name: dev-cluster",
      "  cluster:",
      "    server: https://dev.example.com",
      "- name: prod-cluster",
      "  cluster:",
      "    server: https://prod.example.com",
      "users:",
      "- name: dev-user",
      "  user:",
      "    username: dev-username",
      "    password: dev-password",
      "- name: prod-user",
      "  user:",
      "    username: prod-username",
      "    password: prod-password",
      "contexts:",
      "- name: dev-context",
      "  context:",
      "    cluster: dev-cluster",
      "    user: dev-user",
      "    namespace: dev-namespace",
      "- name: prod-context",
      "  context:",
      "    cluster: prod-cluster",
      "    user: prod-user",
      "    namespace: prod-namespace"
    ).getBytes(StandardCharsets.UTF_8));
    System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, kubeConfig.toString());
  }

  @AfterEach
  void tearDown() {
    if (originalKubeConfig == null) {
      System.clearProperty(Config.KUBERNETES_KUBECONFIG_FILE);
    } else {
      System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, originalKubeConfig);
    }
  }

  @Test
  void should_load_configuration_from_properties() {
    // Given
    final Properties properties = new Properties();
    properties.put("jkube.username", "user name");
    properties.put("jkube.password", "the pa$$w*rd");
    properties.put("jkube.namespace", "myns1");
    properties.put("jkube.masterUrl", "https://example.com");
    properties.put("jkube.corner-case", "corner");
    properties.put("jkube.currentContext", "prod-context");
    properties.put("manufactur8.jkube.corner-case", "cased");
    // When
    final Config config = ClusterConfiguration.from(properties).build().getConfig();
    // Then
    assertThat(config).isNotNull()
            .hasFieldOrPropertyWithValue("username", "user name")
            .hasFieldOrPropertyWithValue("password", "the pa$$w*rd")
            .hasFieldOrPropertyWithValue("namespace", "myns1")
            .hasFieldOrPropertyWithValue("currentContext.name", "prod-context")
            .hasFieldOrPropertyWithValue("currentContext.context.namespace", "myns1")
            .hasFieldOrPropertyWithValue("masterUrl", "https://example.com/");
  }

  @Test
  void should_load_configuration_from_multiple_properties() {
    // Given
    final Properties props1 = new Properties();
    props1.put("jkube.namespace", "not-the-default");
    props1.put("jkube.username", "won't make it");
    props1.put("jkube.password", "won't make it either");
    final Properties props2 = new Properties();
    props2.put("jkube.clientKeyPassphrase", "notchanged");
    props2.put("jkube.username", "user name");
    props2.put("jkube.password", "I don't think you can make it either");
    final Properties props3 = new Properties();
    props3.put("jkube.apiVersion", "v1337");
    props3.put("jkube.password", "the pa$$w*rd");
    // When
    final Config config = ClusterConfiguration.from(props1, props2, props3).build().getConfig();
    // Then
    assertThat(config).isNotNull()
            .hasFieldOrPropertyWithValue("apiVersion", "v1337")
            .hasFieldOrPropertyWithValue("clientKeyPassphrase", "notchanged")
            .hasFieldOrPropertyWithValue("namespace", "not-the-default")
            .hasFieldOrPropertyWithValue("username", "user name")
            .hasFieldOrPropertyWithValue("password", "the pa$$w*rd");
  }

  @Test
  void should_load_configuration_from_properties_with_initial() {
    // Given
    final ClusterConfiguration original = ClusterConfiguration.builder()
        .username("won't make it").password("the pa$$w*rd").build();
    final Properties properties = new Properties();
    properties.put("jkube.username", "user name");
    // When
    final Config config = ClusterConfiguration.from(original, properties).build().getConfig();
    // Then
    assertThat(config).isNotNull()
            .hasFieldOrPropertyWithValue("username","user name")
            .hasFieldOrPropertyWithValue("password","the pa$$w*rd");
  }

  @Test
  void should_load_configuration_from_empty_array() {
    // When
    final Config config = ClusterConfiguration.from().build().getConfig();
    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void loadsConfigurationFromKubernetesConfig() {
    // Given
    final Config config = new ConfigBuilder()
      .withUsername("username")
      .withPassword("password")
      .withMasterUrl("https://example.com")
      .withNamespace("namespace")
      .withApiVersion("v1337")
      .withCaCertFile("caCertFile")
      .withCaCertData("caCertData")
      .withClientKeyFile("clientKeyFile")
      .withClientKeyData("clientKeyData")
      .withClientKeyAlgo("clientKeyAlgo")
      .withClientKeyPassphrase("clientKeyPassphrase")
      .withCurrentContext(new NamedContextBuilder().withName("prod-context").build())
      .withTrustStoreFile("trustStoreFile")
      .withTrustStorePassphrase("trustStorePassphrase")
      .withKeyStoreFile("keyStoreFile")
      .withKeyStorePassphrase("keyStorePassphrase")
      .withTrustCerts(true)
      .build();
    // When
    final ClusterConfiguration result = ClusterConfiguration.from(config).build();
    // Then
    // All fields match those of the original config
    assertThat(result.getConfig())
      .hasFieldOrPropertyWithValue("username", "username")
      .hasFieldOrPropertyWithValue("password", "password")
      .hasFieldOrPropertyWithValue("masterUrl", "https://example.com/")
      .hasFieldOrPropertyWithValue("namespace", "namespace")
      .hasFieldOrPropertyWithValue("apiVersion", "v1337")
      .hasFieldOrPropertyWithValue("caCertFile", "caCertFile")
      .hasFieldOrPropertyWithValue("caCertData", "caCertData")
      .hasFieldOrPropertyWithValue("clientKeyFile", "clientKeyFile")
      .hasFieldOrPropertyWithValue("clientKeyData", "clientKeyData")
      .hasFieldOrPropertyWithValue("clientKeyAlgo", "clientKeyAlgo")
      .hasFieldOrPropertyWithValue("clientKeyPassphrase", "clientKeyPassphrase")
      .hasFieldOrPropertyWithValue("currentContext", new NamedContextBuilder().withName("prod-context").withNewContext()
        .withCluster("prod-cluster").withUser("prod-user").withNamespace("namespace").endContext().build())
      .hasFieldOrPropertyWithValue("trustStoreFile", "trustStoreFile")
      .hasFieldOrPropertyWithValue("trustStorePassphrase", "trustStorePassphrase")
      .hasFieldOrPropertyWithValue("keyStoreFile", "keyStoreFile")
      .hasFieldOrPropertyWithValue("keyStorePassphrase", "keyStorePassphrase")
      .hasFieldOrPropertyWithValue("trustCerts", true);
  }

  @Nested
  @DisplayName("with kubeconfig containing multiple contexts")
  class MultipleContexts {

    @Test
    @DisplayName("without currentContext, should use kubeconfig current-context")
    void withoutCurrentContext() {
      // When
      final Config config = ClusterConfiguration.builder().build().getConfig();
      // Then
      assertThat(config)
        .hasFieldOrPropertyWithValue("masterUrl", "https://dev.example.com/")
        .hasFieldOrPropertyWithValue("username", "dev-username")
        .hasFieldOrPropertyWithValue("password", "dev-password")
        .hasFieldOrPropertyWithValue("namespace", "dev-namespace")
        .hasFieldOrPropertyWithValue("currentContext.name", "dev-context");
    }

    @Test
    @DisplayName("with currentContext, should use cluster, credentials and namespace of selected context")
    void withCurrentContext() {
      // When
      final Config config = ClusterConfiguration.builder().currentContext("prod-context").build().getConfig();
      // Then
      assertThat(config)
        .hasFieldOrPropertyWithValue("masterUrl", "https://prod.example.com/")
        .hasFieldOrPropertyWithValue("username", "prod-username")
        .hasFieldOrPropertyWithValue("password", "prod-password")
        .hasFieldOrPropertyWithValue("namespace", "prod-namespace")
        .hasFieldOrPropertyWithValue("currentContext.name", "prod-context")
        .hasFieldOrPropertyWithValue("currentContext.context.cluster", "prod-cluster")
        .hasFieldOrPropertyWithValue("currentContext.context.user", "prod-user")
        .hasFieldOrPropertyWithValue("currentContext.context.namespace", "prod-namespace");
    }

    @Test
    @DisplayName("with currentContext from properties, should use selected context")
    void withCurrentContextFromProperties() {
      // Given
      final Properties properties = new Properties();
      properties.put("jkube.currentContext", "prod-context");
      // When
      final Config config = ClusterConfiguration.from(properties).build().getConfig();
      // Then
      assertThat(config)
        .hasFieldOrPropertyWithValue("masterUrl", "https://prod.example.com/")
        .hasFieldOrPropertyWithValue("username", "prod-username");
    }

    @Test
    @DisplayName("with currentContext and explicit values, explicit values should take precedence")
    void withCurrentContextAndExplicitValues() {
      // When
      final Config config = ClusterConfiguration.builder()
        .currentContext("prod-context")
        .masterUrl("https://override.example.com")
        .namespace("override-namespace")
        .build().getConfig();
      // Then
      assertThat(config)
        .hasFieldOrPropertyWithValue("masterUrl", "https://override.example.com/")
        .hasFieldOrPropertyWithValue("username", "prod-username")
        .hasFieldOrPropertyWithValue("namespace", "override-namespace")
        .hasFieldOrPropertyWithValue("currentContext.context.cluster", "prod-cluster")
        .hasFieldOrPropertyWithValue("currentContext.context.namespace", "override-namespace");
    }

    @Test
    @DisplayName("getNamespace with currentContext, should return namespace of selected context")
    void getNamespaceWithCurrentContext() {
      // When
      final String namespace = ClusterConfiguration.builder().currentContext("prod-context").build().getNamespace();
      // Then
      assertThat(namespace).isEqualTo("prod-namespace");
    }

    @Test
    @DisplayName("with unknown currentContext, should throw exception instead of using kubeconfig current-context")
    void withUnknownCurrentContext() {
      // Given
      final ClusterConfiguration clusterConfiguration = ClusterConfiguration.builder().currentContext("unknown-context").build();
      // When + Then
      assertThatIllegalArgumentException()
        .isThrownBy(clusterConfiguration::getConfig)
        .withMessage("Context 'unknown-context' not found in kubeconfig. Available contexts: [dev-context, prod-context]");
    }

    @Test
    @DisplayName("with unknown currentContext and explicit masterUrl, should not use kubeconfig current-context")
    void withUnknownCurrentContextAndExplicitMasterUrl() {
      // When
      final Config config = ClusterConfiguration.builder()
        .currentContext("unknown-context")
        .masterUrl("https://explicit.example.com")
        .build().getConfig();
      // Then
      assertThat(config)
        .hasFieldOrPropertyWithValue("masterUrl", "https://explicit.example.com/")
        .hasFieldOrPropertyWithValue("username", null)
        .hasFieldOrPropertyWithValue("password", null)
        .hasFieldOrPropertyWithValue("currentContext.name", "unknown-context");
    }

    @Test
    @DisplayName("getNamespace with unknown currentContext, should throw exception")
    void getNamespaceWithUnknownCurrentContext() {
      // Given
      final ClusterConfiguration clusterConfiguration = ClusterConfiguration.builder().currentContext("unknown-context").build();
      // When + Then
      assertThatIllegalArgumentException()
        .isThrownBy(clusterConfiguration::getNamespace)
        .withMessageStartingWith("Context 'unknown-context' not found in kubeconfig");
    }
  }
}
