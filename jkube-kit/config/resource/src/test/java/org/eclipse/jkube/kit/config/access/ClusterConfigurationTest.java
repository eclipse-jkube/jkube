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
package org.eclipse.jkube.kit.config.access;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterConfigurationTest {

  @Test
  void should_load_configuration_from_properties() {
    // Given
    final Properties properties = new Properties();
    properties.put("jkube.username", "user name");
    properties.put("jkube.password", "the pa$$w*rd");
    properties.put("jkube.masterUrl", "https://example.com");
    properties.put("jkube.corner-case", "corner");
    properties.put("manufactur8.jkube.corner-case", "cased");
    // When
    final Config config = ClusterConfiguration.from(properties).build().getConfig();
    // Then
    assertThat(config).isNotNull()
            .hasFieldOrPropertyWithValue("username", "user name")
            .hasFieldOrPropertyWithValue("password", "the pa$$w*rd")
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
      .hasFieldOrPropertyWithValue("trustStoreFile", "trustStoreFile")
      .hasFieldOrPropertyWithValue("trustStorePassphrase", "trustStorePassphrase")
      .hasFieldOrPropertyWithValue("keyStoreFile", "keyStoreFile")
      .hasFieldOrPropertyWithValue("keyStorePassphrase", "keyStorePassphrase")
      .hasFieldOrPropertyWithValue("trustCerts", true);
  }
}
