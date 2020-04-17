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
package org.eclipse.jkube.kit.config.access;

import io.fabric8.kubernetes.client.Config;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterConfigurationTest {

  @Test
  public void should_load_configuration_from_properties() {
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
    assertThat(config.getUsername()).isEqualTo("user name");
    assertThat(config.getPassword()).isEqualTo("the pa$$w*rd");
    assertThat(config.getMasterUrl()).isEqualTo("https://example.com/");
  }

  @Test
  public void should_load_configuration_from_multiple_properties() {
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
    assertThat(config.getApiVersion()).isEqualTo("v1337");
    assertThat(config.getClientKeyPassphrase()).isEqualTo("notchanged");
    assertThat(config.getNamespace()).isEqualTo("not-the-default");
    assertThat(config.getUsername()).isEqualTo("user name");
    assertThat(config.getPassword()).isEqualTo("the pa$$w*rd");
  }

  @Test
  public void should_load_configuration_from_properties_with_initial() {
    // Given
    final ClusterConfiguration original = ClusterConfiguration.builder()
        .username("won't make it").password("the pa$$w*rd").build();
    final Properties properties = new Properties();
    properties.put("jkube.username", "user name");
    // When
    final Config config = ClusterConfiguration.from(original, properties).build().getConfig();
    // Then
    assertThat(config.getUsername()).isEqualTo("user name");
    assertThat(config.getPassword()).isEqualTo("the pa$$w*rd");
  }
  @Test
  public void should_load_configuration_from_empty_array() {
    // When
    final Config config = ClusterConfiguration.from().build().getConfig();
    // Then
    assertThat(config).isNotNull();
  }
}
