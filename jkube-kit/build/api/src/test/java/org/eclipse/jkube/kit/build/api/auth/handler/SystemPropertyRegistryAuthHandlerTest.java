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
package org.eclipse.jkube.kit.build.api.auth.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * @author roland
 */
public class SystemPropertyRegistryAuthHandlerTest {

  @Mocked
  KitLogger log;

  SystemPropertyRegistryAuthHandler handler;

  @Before
  public void setup() {
    RegistryAuthConfig registryAuthConfig = RegistryAuthConfig.builder()
        .skipExtendedAuthentication(false)
        .propertyPrefix("jkube.docker")
        .build();
    handler = new SystemPropertyRegistryAuthHandler(registryAuthConfig, log);
  }

  @Test
  public void testEmpty() throws Exception {
    String userHome = System.getProperty("user.home");
    try {
      File tempDir = Files.createTempDirectory("d-m-p").toFile();
      System.setProperty("user.home", tempDir.getAbsolutePath());
      assertThat(handler.create(RegistryAuthConfig.Kind.PUSH, null, "blubberbla:1611", s -> s)).isNull();
    } finally {
      System.setProperty("user.home", userHome);
    }
  }

  @Test
  public void testSystemProperty() throws Exception {
    System.setProperty("jkube.docker.push.username", "roland");
    System.setProperty("jkube.docker.push.password", "secret");
    System.setProperty("jkube.docker.push.email", "roland@jolokia.org");
    try {
      AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
      verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
    } finally {
      System.clearProperty("jkube.docker.push.username");
      System.clearProperty("jkube.docker.push.password");
      System.clearProperty("jkube.docker.push.email");
    }
  }

  @Test
  public void testSystemPropertyNoPassword() throws IOException {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> checkException("jkube.docker.username"));
    assertThat(exception).hasMessageContaining("No password provided for username secret");
  }

  private void checkException(String key) throws IOException {
    System.setProperty(key, "secret");
    try {
      handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
    } finally {
      System.clearProperty(key);
    }
  }

  private void verifyAuthConfig(AuthConfig config, String username, String password, String email) {
    JsonObject params = new Gson().fromJson(new String(Base64.getDecoder().decode(config.toHeaderValue(log).getBytes())),
        JsonObject.class);
    assertThat(params.get("username").getAsString()).isEqualTo(username);
    assertThat(params.get("password").getAsString()).isEqualTo(password);
    if (email != null) {
      assertThat(params.get("email").getAsString()).isEqualTo(email);
    }
  }

}
