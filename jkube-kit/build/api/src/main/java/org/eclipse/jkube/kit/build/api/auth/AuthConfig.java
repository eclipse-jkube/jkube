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

import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Configuration object holding auth information for
 * pushing to Docker
 *
 * @author roland
 * @since 22.07.14
 */
public class AuthConfig {

    public final static AuthConfig EMPTY_AUTH_CONFIG = new AuthConfig("", "", "", "");

    private String username;
    private String password;
    private String email;
    private String auth;

    private String authEncoded;

    public AuthConfig(Map<String,String> params) {
        this(params.get("username"),
                params.get("password"),
                params.get("email"),
                params.get("auth"));
    }

    public AuthConfig(String username, String password, String email, String auth) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.auth = auth;
        authEncoded = createAuthEncoded();
    }

    /**
     * Constructor which takes an base64 encoded credentials in the form 'user:password'
     *
     * @param credentialsEncoded the docker encoded user and password
     * @param email the email to use for authentication
     */
    public AuthConfig(String credentialsEncoded, String email) {
        String credentials = new String(Base64.decodeBase64(credentialsEncoded));
        String[] parsedCreds = credentials.split(":",2);
        username = parsedCreds[0];
        password = parsedCreds[1];
        this.email = email;
        auth = null;
        authEncoded = createAuthEncoded();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAuth() {
        return auth;
    }

    public String toHeaderValue() {
        return authEncoded;
    }

    // ======================================================================================================

    private String createAuthEncoded() {
        JsonObject ret = new JsonObject();
        putNonNull(ret, "username", username);
        putNonNull(ret, "password", password);
        putNonNull(ret, "email", email);
        putNonNull(ret, "auth", auth);
        try {
            return encodeBase64ChunkedURLSafeString(ret.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return encodeBase64ChunkedURLSafeString(ret.toString().getBytes());
        }
    }

    public static AuthConfig fromRegistryAuthConfig(RegistryAuthConfig registryAuthConfig,
                                                      RegistryAuthConfig.Kind kind,
                                                      Function<String, String> decryptor) {
        return new AuthConfig.Builder()
                .username(registryAuthConfig.getUsername(kind))
                .email(registryAuthConfig.getEmail(kind))
                .auth(registryAuthConfig.getAuth(kind))
                .password(registryAuthConfig.getPassword(kind), decryptor)
                .build();
    }

    /**
     * Encodes the given binaryData in a format that is compatible with the Docker Engine API.
     * That is, base64 encoded, padded, and URL safe.
     *
     * @param binaryData data to encode
     * @return encoded data
     */
    private String encodeBase64ChunkedURLSafeString(final byte[] binaryData) {
        return Base64.encodeBase64String(binaryData)
                .replace('+', '-')
                .replace('/', '_');
    }

    private void putNonNull(JsonObject ret, String key, String value) {
        if (value != null) {
            ret.addProperty(key,value);
        }
    }

    public static class Builder {
        AuthConfig registryAuth;

        public Builder() {
            registryAuth = new AuthConfig(null, null, null, null);
        }

        public Builder username(String username) {
            registryAuth.username = username;
            return this;
        }

        public Builder password(String password) {
            return password(password, null);
        }

        public Builder password(String password, Function<String, String> decryptor) {
            registryAuth.password =
                    Optional.ofNullable(decryptor).map(d -> d.apply(password)).orElse(password);
            return this;
        }

        public Builder email(String email) {
            registryAuth.email = email;
            return this;
        }

        public Builder auth(String auth) {
            registryAuth.auth = auth;
            return this;
        }

        public Builder withCredentialsEncoded(String creds) {
            String credentials = new String(Base64.decodeBase64(creds));
            String[] parsedCreds = credentials.split(":", 2);
            registryAuth.username = parsedCreds[0];
            registryAuth.password = parsedCreds[1];
            return this;
        }


        public AuthConfig build() {
            registryAuth.authEncoded = registryAuth.createAuthEncoded();
            return registryAuth;
        }
    }
}
