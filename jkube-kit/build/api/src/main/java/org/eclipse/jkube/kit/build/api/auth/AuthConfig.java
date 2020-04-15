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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Configuration object holding auth information for
 * pushing to Docker
 *
 * @author roland
 * @since 22.07.14
 */
@Getter
@EqualsAndHashCode
public class AuthConfig {

  public static final AuthConfig EMPTY_AUTH_CONFIG = new AuthConfig("", "", "", "");

  private final String username;
  private final String password;
  private final String email;
  private final String auth;

  private String authEncoded;

  @Builder
  public AuthConfig(String username, String password, String email, String auth) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.auth = auth;
    authEncoded = createAuthEncoded();
  }

  public String toHeaderValue() {
    return authEncoded;
  }

  private String createAuthEncoded() {
    JsonObject ret = new JsonObject();
    putNonNull(ret, "username", username);
    putNonNull(ret, "password", password);
    putNonNull(ret, "email", email);
    putNonNull(ret, "auth", auth);
    return encodeBase64ChunkedURLSafeString(ret.toString().getBytes(StandardCharsets.UTF_8));
  }

  public static AuthConfig fromMap(Map<String, String> params) {
    return AuthConfig.builder()
        .username(params.get("username"))
        .password(params.get("password"))
        .email(params.get("email"))
        .auth(params.get("auth"))
        .build();
  }

  public static AuthConfig fromCredentialsEncoded(String credentialsEncoded, String email) {
    final String credentials = new String(Base64.decodeBase64(credentialsEncoded));
    final String[] parsedCredentials = credentials.split(":",2);
    return AuthConfig.builder()
        .username(parsedCredentials[0])
        .password(parsedCredentials[1])
        .email(email)
        .build();
  }

  public static AuthConfig fromRegistryAuthConfig(RegistryAuthConfig registryAuthConfig,
                                                  RegistryAuthConfig.Kind kind,
                                                  UnaryOperator<String> decryptor) {

    final String password = registryAuthConfig.getPassword(kind);
    return AuthConfig.builder()
        .username(registryAuthConfig.getUsername(kind))
        .email(registryAuthConfig.getEmail(kind))
        .auth(registryAuthConfig.getAuth(kind))
        .password(decryptPassword(password, decryptor))
        .build();
  }

  public static String decryptPassword(String password, UnaryOperator<String> decryptor) {
    return Optional.ofNullable(decryptor).map(d -> d.apply(password)).orElse(password);
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

}
