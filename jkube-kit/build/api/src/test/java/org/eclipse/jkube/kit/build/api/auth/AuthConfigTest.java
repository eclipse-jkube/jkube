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
package org.eclipse.jkube.kit.build.api.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class AuthConfigTest {

  @Test
  void simpleConstructor() {
    AuthConfig config = AuthConfig.builder()
      .username("roland")
      .password("#>secrets??")
      .email("roland@jolokia.org")
      .build();
    // Since Base64.decodeBase64 handles URL-safe encoding, must explicitly check
    // the correct characters are used
    assertThat(config.toHeaderValue(new KitLogger.SilentLogger()))
      .isEqualTo("eyJlbWFpbCI6InJvbGFuZEBqb2xva2lhLm9yZyIsInBhc3N3b3JkIjoiIz5zZWNyZXRzPz8iLCJ1c2VybmFtZSI6InJvbGFuZCJ9");

    String header = new String(Base64.getDecoder().decode(config.toHeaderValue(new KitLogger.SilentLogger())));

    final Map<String, Object> result = Serialization.unmarshal(header, new TypeReference<Map<String, Object>>() {
    });
    assertThat(result)
      .containsOnly(
        entry("username", "roland"),
        entry("password", "#>secrets??"),
        entry("email", "roland@jolokia.org")
      );
  }
}
