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
package org.eclipse.jkube.kit.build.service.docker.auth.ecr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.DockerRegistryAuthHandler;
import org.eclipse.jkube.kit.common.JsonFactory;
import org.eclipse.jkube.kit.common.KitLogger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author roland
 */
class DockerRegistryAuthHandlerTest {
  private KitLogger log;

  private DockerRegistryAuthHandler handler;

  @BeforeEach
  void setup() {
    log = mock(KitLogger.SilentLogger.class);
    handler = new DockerRegistryAuthHandler(log);
  }

  @Test
  void testDockerAuthLogin() throws Exception {
    executeWithTempHomeDir(homeDir -> {
      checkDockerAuthLogin(homeDir, "https://index.docker.io/v1/", null);
      checkDockerAuthLogin(homeDir, "localhost:5000", "localhost:5000");
      checkDockerAuthLogin(homeDir, "https://localhost:5000", "localhost:5000");
    });
  }

  @Test
  void testDockerLoginNoConfig() throws IOException {
    executeWithTempHomeDir(dir -> {
      AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s);
      assertThat(config).isNull();
    });
  }

  @Test
  void testDockerLoginFallsBackToAuthWhenCredentialHelperDoesNotMatchDomain() throws IOException {
    executeWithTempHomeDir(homeDir -> {
      writeDockerConfigJson(createDockerConfig(homeDir), null, singletonMap("registry1", "credHelper1-does-not-exist"));
      AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", "localhost:5000", s -> s);
      verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
    });
  }

  @Test
  void testDockerLoginNoAuthConfigFoundWhenCredentialHelperDoesNotMatchDomainOrAuth() throws IOException {
    executeWithTempHomeDir(homeDir -> {
      writeDockerConfigJson(createDockerConfig(homeDir), null, singletonMap("registry1", "credHelper1-does-not-exist"));
      AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", "does-not-exist-either:5000", s -> s);
      assertThat(config).isNull();
    });
  }

  @Test
  void testDockerLoginSelectCredentialHelper() throws IOException {
    executeWithTempHomeDir(homeDir -> {
      writeDockerConfigJson(createDockerConfig(homeDir), "credsStore-does-not-exist",
          singletonMap("registry1", "credHelper1-does-not-exist"));
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> handler.create(RegistryAuthConfig.Kind.PUSH, "roland", "registry1", s -> s));
      assertThat(exception).cause()
          .isInstanceOf(IOException.class)
          .hasMessageStartingWith("Failed to start 'docker-credential-credHelper1-does-not-exist version'");
    });
  }

  @Test
  void testDockerLoginSelectCredentialsStore() throws IOException {
    executeWithTempHomeDir(homeDir -> {
      writeDockerConfigJson(createDockerConfig(homeDir), "credsStore-does-not-exist",
          singletonMap("registry1", "credHelper1-does-not-exist"));
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s));
      assertThat(exception).cause()
          .isInstanceOf(IOException.class)
          .hasMessageStartingWith("Failed to start 'docker-credential-credsStore-does-not-exist version'");
    });
  }

  @Test
  void testDockerLoginDefaultToCredentialsStore() throws IOException {

    executeWithTempHomeDir(homeDir -> {
      writeDockerConfigJson(createDockerConfig(homeDir), "credsStore-does-not-exist",
          singletonMap("registry1", "credHelper1-does-not-exist"));
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> handler.create(RegistryAuthConfig.Kind.PUSH, "roland", "registry2", s -> s));
      assertThat(exception).cause()
          .hasCauseInstanceOf(IOException.class)
          .hasMessageStartingWith("Failed to start 'docker-credential-credsStore-does-not-exist version'");
    });
  }

  private void executeWithTempHomeDir(HomeDirExecutor executor) throws IOException {
    String userHome = System.getProperty("user.home");
    try {
      File tempDir = Files.createTempDirectory("d-m-p").toFile();
      System.setProperty("user.home", tempDir.getAbsolutePath());
      executor.exec(tempDir);
    } finally {
      System.setProperty("user.home", userHome);
    }

  }

  interface HomeDirExecutor {
    void exec(File dir) throws IOException;
  }

  private void checkDockerAuthLogin(File homeDir, String configRegistry, String lookupRegistry)
      throws IOException {

    writeDockerConfigJson(createDockerConfig(homeDir), "roland", "secret", "roland@jolokia.org", configRegistry);
    AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", lookupRegistry, s -> s);
    verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
  }

  private File createDockerConfig(File homeDir) {
    File dockerDir = new File(homeDir, ".docker");
    dockerDir.mkdirs();
    return dockerDir;
  }

  private void writeDockerConfigJson(File dockerDir, String user, String password,
      String email, String registry) throws IOException {
    File configFile = new File(dockerDir, "config.json");

    JsonObject config = new JsonObject();
    addAuths(config, user, password, email, registry);

    try (Writer writer = new FileWriter(configFile)) {
      new Gson().toJson(config, writer);
    }
  }

  private void writeDockerConfigJson(File dockerDir, String credsStore, Map<String, String> credHelpers) throws IOException {
    File configFile = new File(dockerDir, "config.json");

    JsonObject config = new JsonObject();
    if (!credHelpers.isEmpty()) {
      config.add("credHelpers", JsonFactory.newJsonObject(credHelpers));
    }

    if (credsStore != null) {
      config.addProperty("credsStore", credsStore);
    }

    addAuths(config, "roland", "secret", "roland@jolokia.org", "localhost:5000");

    try (Writer writer = new FileWriter(configFile)) {
      new Gson().toJson(config, writer);
    }
  }

  private void addAuths(JsonObject config, String user, String password, String email, String registry) {
    JsonObject auths = new JsonObject();
    JsonObject value = new JsonObject();
    value.addProperty("auth", new String(Base64.getEncoder().encode((user + ":" + password).getBytes())));
    value.addProperty("email", email);
    auths.add(registry, value);
    config.add("auths", auths);
  }

  private void verifyAuthConfig(AuthConfig config, String username, String password, String email) {
    JsonObject params = new Gson().fromJson(new String(Base64.getDecoder().decode(config.toHeaderValue(log).getBytes())),
        JsonObject.class);
    assertThat(params)
            .returns(username, j -> j.get("username").getAsString())
            .returns(password, j -> j.get("password").getAsString());
    if (email != null) {
      assertThat(params.get("email").getAsString()).isEqualTo(email);
    }
  }

}
