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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;

import java.util.LinkedHashMap;
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

  public static final AuthConfig EMPTY_AUTH_CONFIG = new AuthConfig("", "", "", "", "");

  private final String username;
  private final String password;
  private final String email;
  private final String auth;
  private final String identityToken;

  private String authEncoded;

  @Builder
  public AuthConfig(String username, String password, String email, String auth, String identityToken) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.auth = auth;
    this.identityToken = identityToken;
  }

  public AuthConfig(String username, String password, String email, String auth) {
    this(username, password, email, auth, null);
  }

  public String toHeaderValue(KitLogger logger) {
    final Map<String, String> ret = new LinkedHashMap<>();
    if(StringUtils.isNotBlank(identityToken)) {
      putNonNull(ret, "identityToken", identityToken);
      if (StringUtils.isNotBlank(username)) {
        logger.warn("Using identityToken, found username not blank : " + username);
      }
    } else {
      putNonNull(ret, "username", username);
      putNonNull(ret, "password", password);
      putNonNull(ret, "email", email);
      putNonNull(ret, "auth", auth);
    }
    try {
      final byte[] bytes = Serialization.jsonWriter().without(SerializationFeature.INDENT_OUTPUT)
        .writeValueAsBytes(ret);
      return encodeBase64ChunkedURLSafeString(bytes);
    } catch(JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot encode auth config", e);
    }
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

  private void putNonNull(Map<String, String> ret, String key, String value) {
    if (value != null) {
      ret.put(key,value);
    }
  }

}
