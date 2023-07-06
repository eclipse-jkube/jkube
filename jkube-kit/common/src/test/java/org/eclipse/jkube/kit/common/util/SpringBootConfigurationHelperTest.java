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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootConfigurationHelperTest {
  @Test
  void undefinedSpringBootVersion_shouldDefaultToSpringBoot1ConfigurationProperties() {
    // Given
    Optional<String> springBootVersion = Optional.of("undefined");
    SpringBootConfigurationHelper springBootConfigurationHelper = new SpringBootConfigurationHelper(springBootVersion);

    // When + Then
    assertSpringBoot1ConfigurationProperties(springBootConfigurationHelper);
  }

  private void assertSpringBoot1ConfigurationProperties(SpringBootConfigurationHelper springBootConfigurationHelper) {
    assertThat(springBootConfigurationHelper.getManagementPortPropertyKey()).isEqualTo("management.port");
    assertThat(springBootConfigurationHelper.getServerPortPropertyKey()).isEqualTo("server.port");
    assertThat(springBootConfigurationHelper.getServerKeystorePropertyKey()).isEqualTo("server.ssl.key-store");
    assertThat(springBootConfigurationHelper.getManagementKeystorePropertyKey()).isEqualTo("management.ssl.key-store");
    assertThat(springBootConfigurationHelper.getServletPathPropertyKey()).isEqualTo("server.servlet-path");
    assertThat(springBootConfigurationHelper.getServerContextPathPropertyKey()).isEqualTo("server.context-path");
    assertThat(springBootConfigurationHelper.getManagementContextPathPropertyKey()).isEqualTo("management.context-path");
    assertThat(springBootConfigurationHelper.getActuatorBasePathPropertyKey()).isEmpty();
    assertThat(springBootConfigurationHelper.getActuatorDefaultBasePath()).isEmpty();
  }
}
