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
package org.eclipse.jkube.kit.build.service.docker.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialHelperClientTest {
    private final Gson gson = new Gson();

    private CredentialHelperClient credentialHelperClient;

    private JsonObject jsonObject;

    private AuthConfig authConfig;

    @BeforeEach
    void givenCredentialHelperClient() {
        this.credentialHelperClient = new CredentialHelperClient(new KitLogger.SilentLogger(), "desktop");
    }

    @Test
    void usernamePasswordAuthConfig() {
        givenJson("{\"ServerURL\":\"registry.mycompany.com\",\"Username\":\"jane_doe\",\"Secret\":\"not-really\"}");

        whenJsonObjectConvertedToAuthConfig();
        assertThat(authConfig)
                .hasFieldOrPropertyWithValue("username", "jane_doe")
                .hasFieldOrPropertyWithValue("password", "not-really")
                .hasFieldOrPropertyWithValue("identityToken", null);
    }

    @Test
    void tokenAuthConfig() {
        givenJson("{\"ServerURL\":\"registry.cloud-provider.com\",\"Username\":\"<token>\",\"Secret\":\"gigantic-mess-of-jwt\"}");

        whenJsonObjectConvertedToAuthConfig();
        assertThat(authConfig)
                .hasFieldOrPropertyWithValue("username", null)
                .hasFieldOrPropertyWithValue("password", null)
                .hasFieldOrPropertyWithValue("identityToken", "gigantic-mess-of-jwt");
    }

    private void givenJson(String json) {
        this.jsonObject = this.gson.fromJson(json, JsonObject.class);
    }

    private void whenJsonObjectConvertedToAuthConfig() {
        this.authConfig = this.credentialHelperClient.toAuthConfig(this.jsonObject);
    }
}
