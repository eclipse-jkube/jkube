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
package org.eclipse.jkube.micronaut;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.micronaut.MicronautUtils.extractPort;
import static org.eclipse.jkube.micronaut.MicronautUtils.getMicronautConfiguration;
import static org.eclipse.jkube.micronaut.MicronautUtils.isHealthEnabled;

public class MicronautUtilsTest {

  @Test
  public void extractPortWithPort() {
    // Given
    final Properties properties = new Properties();
    properties.put("micronaut.server.port", "1337");
    // When
    final String result = extractPort(properties, "80");
    // Then
    assertThat(result).isEqualTo("1337");
  }

  @Test
  public void extractPortWithNoPort() {
    // When
    final String result = extractPort(new Properties(), "80");
    // Then
    assertThat(result).isEqualTo("80");
  }

  @Test
  public void isHealthEnabledWithDefaults() {
    // When
    final boolean result = isHealthEnabled(new Properties());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isHealthEnabledWithHealthEnabled() {
    // Given
    final Properties properties = new Properties();
    properties.put("endpoints.health.enabled", "tRuE");
    // When
    final boolean result = isHealthEnabled(properties);
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void getMicronautConfigurationPrecedence() {
    // Given
    final URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
        MicronautUtilsTest.class.getResource("/utils-test/port-config/json/"),
        MicronautUtilsTest.class.getResource("/utils-test/port-config/yaml/"),
        MicronautUtilsTest.class.getResource("/utils-test/port-config/properties/")
    });
    // When
    final Properties props = getMicronautConfiguration(ucl);
    // Then
    assertThat(props).containsExactly(
        entry("micronaut.application.name", "port-config-test-PROPERTIES"),
        entry("micronaut.server.port", "1337"));
  }

  @Test
  public void getMicronautConfigurationNoConfigFiles() {
    // Given
    final URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
        MicronautUtilsTest.class.getResource("/")
    });
    // When
    final Properties props = getMicronautConfiguration(ucl);
    // Then
    assertThat(props).isEmpty();
  }

}
