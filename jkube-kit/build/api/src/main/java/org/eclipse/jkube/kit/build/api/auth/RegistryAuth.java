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
package org.eclipse.jkube.kit.build.api.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Configuration object holding auth information for
 * pushing to Docker
 *
 * @author roland
 */

@Getter
@EqualsAndHashCode
public class RegistryAuth {

    public static final RegistryAuth EMPTY_REGISTRY_AUTH =
        RegistryAuth.builder().username("").password("").email("").auth("").build();

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String AUTH = "authToken";

    private final String username;
    private final String password;
    private final String email;
    private final String auth;

    private final String authEncoded;

    @Builder
    public RegistryAuth(String username, String password, String email, String auth) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.auth = auth;
        authEncoded = createAuthEncoded();
    }

    public String toHeaderValue() {
        return authEncoded;
    }

    public static RegistryAuth fromCredentialsEncoded(String credentialsEncoded, String email) {
        final String credentials = new String(org.apache.commons.codec.binary.Base64.decodeBase64(credentialsEncoded));
        final String[] parsedCredentials = credentials.split(":",2);
        return RegistryAuth.builder()
            .username(parsedCredentials[0])
            .password(parsedCredentials[1])
            .email(email)
            .build();
    }

    private String createAuthEncoded() {
        JsonObject ret = new JsonObject();
        putNonNull(ret, USERNAME, username);
        putNonNull(ret, PASSWORD, password);
        putNonNull(ret, EMAIL, email);
        putNonNull(ret, AUTH, auth);
        return encodeBase64ChunkedURLSafeString(ret.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encodes the given binaryData in a format that is compatible with the Docker Engine API.
     * That is, base64 encoded, padded, and URL safe.
     *
     * @param binaryData data to encode
     * @return encoded data
     */
    private String encodeBase64ChunkedURLSafeString(final byte[] binaryData) {
        return Base64.getEncoder().encodeToString(binaryData)
                     .replace('+', '-')
                     .replace('/', '_');
    }

    private void putNonNull(JsonObject ret, String key, String value) {
        if (value != null) {
            ret.addProperty(key,value);
        }
    }
}
