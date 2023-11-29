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

import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author roland
 */
class RegistryAuthTest {

    @Test
    void simpleConstructor() {
        RegistryAuth config = RegistryAuth.builder()
            .username("roland")
            .password("#>secrets??")
            .email("roland@jolokia.org")
            .build();
        check(config);
    }

    @Test
    void dockerLoginConstructor() {
        RegistryAuth config = RegistryAuth.fromCredentialsEncoded(
            Base64.getEncoder().encodeToString("roland:#>secrets??".getBytes()),
            "roland@jolokia.org");
        check(config);
    }

    private void check(RegistryAuth config) {
        // Since Base64.decodeBase64 handles URL-safe encoding, must explicitly check
        // the correct characters are used
        assertThat(config.toHeaderValue())
            .isEqualTo("eyJ1c2VybmFtZSI6InJvbGFuZCIsInBhc3N3b3JkIjoiIz5zZWNyZXRzPz8iLCJlbWFpbCI6InJvbGFuZEBqb2xva2lhLm9yZyJ9");

        String header = new String(Base64.getDecoder().decode(config.toHeaderValue()));

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
