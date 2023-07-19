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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootConfigurationHelperTest {

  private Properties properties;

  @BeforeEach
  void setUp() {
    properties = new Properties();
    properties.put("management.port", "1");
    properties.put("management.server.port", "2");
    properties.put("server.port", "1");
  }

  @Nested
  @DisplayName("With '2.0' Spring Boot Version")
  class SpringBoot2 {
    private SpringBootConfigurationHelper springBootConfigurationHelper;

    @BeforeEach
    void setUp() {
      springBootConfigurationHelper = new SpringBootConfigurationHelper(Optional.of("2.0"));
    }

    @Test
    @DisplayName("getManagementPort defaults to 'management.server.port'")
    void getManagementPort() {
      assertThat(springBootConfigurationHelper.getManagementPort(properties)).isEqualTo(2);
    }

    @Test
    @DisplayName("getServerPort defaults to 'server.port'")
    void getServerPort() {
      assertThat(springBootConfigurationHelper.getServerPort(properties)).isEqualTo(1);
    }

    @Test
    @DisplayName("getServerKeystorePropertyKey defaults to 'server.ssl.key-store'")
    void getServerKeystorePropertyKey() {
      assertThat(springBootConfigurationHelper.getServerKeystorePropertyKey()).isEqualTo("server.ssl.key-store");
    }

    @Test
    @DisplayName("getManagementKeystorePropertyKey defaults to 'management.server.ssl.key-store'")
    void getManagementKeystorePropertyKey() {
      assertThat(springBootConfigurationHelper.getManagementKeystorePropertyKey())
        .isEqualTo("management.server.ssl.key-store");
    }

    @Test
    @DisplayName("getServletPathPropertyKey defaults to 'server.servlet.path'")
    void getServletPathPropertyKey() {
      assertThat(springBootConfigurationHelper.getServletPathPropertyKey()).isEqualTo("server.servlet.path");
    }

    @Test
    @DisplayName("getServerContextPathPropertyKey defaults to 'server.servlet.context-path'")
    void getServerContextPathPropertyKey() {
      assertThat(springBootConfigurationHelper.getServerContextPathPropertyKey())
        .isEqualTo("server.servlet.context-path");
    }

    @Test
    @DisplayName("getManagementContextPathPropertyKey defaults to 'management.server.servlet.context-path'")
    void getManagementContextPathPropertyKey() {
      assertThat(springBootConfigurationHelper.getManagementContextPathPropertyKey())
        .isEqualTo("management.server.servlet.context-path");
    }

    @Test
    @DisplayName("getActuatorBasePathPropertyKey defaults to 'management.endpoints.web.base-path'")
    void getActuatorBasePathPropertyKey() {
      assertThat(springBootConfigurationHelper.getActuatorBasePathPropertyKey())
        .isEqualTo("management.endpoints.web.base-path");
    }

    @Test
    @DisplayName("getActuatorDefaultBasePath defaults to '/actuator'")
    void getActuatorDefaultBasePath() {
      assertThat(springBootConfigurationHelper.getActuatorDefaultBasePath()).isEqualTo("/actuator");
    }
  }

  @Nested
  @DisplayName("With '1.0' (or undefined) Spring Boot Version")
  class SpringBoot1 {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getManagementPort defaults to 'management.port' (Spring Boot 1)")
    void getManagementPort(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getManagementPort(properties))
        .isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServerPort defaults to 'server.port' (Spring Boot 1)")
    void getServerPort(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getServerPort(properties))
        .isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServerKeystorePropertyKey defaults to 'server.ssl.key-store' (Spring Boot 1)")
    void getServerKeystorePropertyKey(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getServerKeystorePropertyKey())
        .isEqualTo("server.ssl.key-store");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getManagementKeystorePropertyKey defaults to 'management.ssl.key-store' (Spring Boot 1)")
    void getManagementKeystorePropertyKey(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getManagementKeystorePropertyKey())
        .isEqualTo("management.ssl.key-store");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServletPathPropertyKey defaults to 'server.servlet-path' (Spring Boot 1)")
    void getServletPathPropertyKey(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getServletPathPropertyKey())
        .isEqualTo("server.servlet-path");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServerContextPathPropertyKey defaults to 'server.context-path' (Spring Boot 1)")
    void getServerContextPathPropertyKey(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getServerContextPathPropertyKey())
        .isEqualTo("server.context-path");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getManagementContextPathPropertyKey defaults to 'management.context-path' (Spring Boot 1)")
    void getManagementContextPathPropertyKey(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getManagementContextPathPropertyKey())
        .isEqualTo("management.context-path");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getActuatorBasePathPropertyKey defaults to '' (Spring Boot 1)")
    void getActuatorBasePathPropertyKey(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getActuatorBasePathPropertyKey())
        .isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getActuatorDefaultBasePath defaults to '' (Spring Boot 1)")
    void getActuatorDefaultBasePath(String version) {
      assertThat(new SpringBootConfigurationHelper(Optional.of(version)).getActuatorDefaultBasePath()).
        isEmpty();
    }
  }
}
