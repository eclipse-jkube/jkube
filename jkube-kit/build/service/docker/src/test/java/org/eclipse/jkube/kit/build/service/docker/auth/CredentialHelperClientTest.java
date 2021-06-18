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
package org.eclipse.jkube.kit.build.service.docker.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import mockit.Mocked;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CredentialHelperClientTest {
    private final Gson gson = new Gson();

    @Mocked
    private KitLogger logger;

    private CredentialHelperClient credentialHelperClient;

    private JsonObject jsonObject;

    private AuthConfig authConfig;

    @Before
    public void givenCredentialHelperClient() {
        this.credentialHelperClient = new CredentialHelperClient(logger, "desktop");
    }

    @Test
    public void testUsernamePasswordAuthConfig() {
        givenJson("{\"ServerURL\":\"registry.mycompany.com\",\"Username\":\"jane_doe\",\"Secret\":\"not-really\"}");

        whenJsonObjectConvertedToAuthConfig();
        
        assertEquals("username should match", "jane_doe", this.authConfig.getUsername());
        assertEquals("password should match", "not-really", this.authConfig.getPassword());
        assertNull("identityToken should not be set", this.authConfig.getIdentityToken());
    }

    @Test
    public void testTokenAuthConfig() {
        givenJson("{\"ServerURL\":\"registry.cloud-provider.com\",\"Username\":\"<token>\",\"Secret\":\"gigantic-mess-of-jwt\"}");

        whenJsonObjectConvertedToAuthConfig();

        assertNull("username should not be set", this.authConfig.getUsername());
        assertNull("password should not be set", this.authConfig.getPassword());
        assertEquals("identity token should match", "gigantic-mess-of-jwt", this.authConfig.getIdentityToken());
    }

    private void givenJson(String json) {
        this.jsonObject = this.gson.fromJson(json, JsonObject.class);
    }

    private void whenJsonObjectConvertedToAuthConfig() {
        this.authConfig = this.credentialHelperClient.toAuthConfig(this.jsonObject);
    }
}
