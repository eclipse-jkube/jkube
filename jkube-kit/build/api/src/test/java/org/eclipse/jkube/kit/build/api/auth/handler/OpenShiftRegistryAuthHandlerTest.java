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
package org.eclipse.jkube.kit.build.api.auth.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author roland
 */
class OpenShiftRegistryAuthHandlerTest {

  @TempDir
  Path temporaryFolder;
  private KitLogger log;
  private OpenShiftRegistryAuthHandler handler;

  @BeforeEach
  void setup() {
    log = new KitLogger.SilentLogger();
    RegistryAuthConfig registryAuthConfig = RegistryAuthConfig.builder()
        .skipExtendedAuthentication(false)
        .propertyPrefix("docker")
        .addHandlerConfig("openshift",
            OpenShiftRegistryAuthHandler.AUTH_USE_OPENSHIFT_AUTH,
            "true")
        .build();
    handler = new OpenShiftRegistryAuthHandler(registryAuthConfig, log);
  }

  @Test
  void openShiftConfigFromPluginConfig() throws IOException {

    executeWithTempHomeDir(homeDir -> {
      createOpenShiftConfig(homeDir, "openshift_simple_config.yaml");
      AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s);
      verifyAuthConfig(config, "admin", "token123", null);
    });
  }

  @Test
  void openShiftConfigFromSystemProps() throws IOException {

    try {
      System.setProperty("docker.useOpenShiftAuth", "true");
      executeWithTempHomeDir(homeDir -> {
        createOpenShiftConfig(homeDir, "openshift_simple_config.yaml");
        AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s);
        verifyAuthConfig(config, "admin", "token123", null);
      });
    } finally {
      System.getProperties().remove("docker.useOpenShiftAuth");
    }
  }

  @Test
  void openShiftConfigFromSystemPropsNegative() throws IOException {
    try {
      System.setProperty("docker.useOpenShiftAuth", "false");
      executeWithTempHomeDir(homeDir -> {
        createOpenShiftConfig(homeDir, "openshift_simple_config.yaml");
        AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s);
        assertThat(config).isNull();
      });
    } finally {
      System.getProperties().remove("docker.useOpenShiftAuth");
    }
  }

  @Test
  void openShiftConfigNotLoggedIn() throws IOException {
    executeWithTempHomeDir(homeDir -> {
      createOpenShiftConfig(homeDir, "openshift_nologin_config.yaml");
      assertThatIllegalArgumentException()
          .isThrownBy(() -> handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s))
          .withMessageContaining("~/.kube/config");
    });

  }

  private void executeWithTempHomeDir(Consumer<File> executor) throws IOException {
    String userHome = System.getProperty("user.home");
    try {
      File tempDir = Files.createDirectory(temporaryFolder.resolve("d-m-p")).toFile();
      System.setProperty("user.home", tempDir.getAbsolutePath());
      executor.accept(tempDir);
    } finally {
      System.setProperty("user.home", userHome);
    }

  }

  private void createOpenShiftConfig(File homeDir, String testConfig) {
    try {
      File kubeDir = new File(homeDir, ".kube");
      kubeDir.mkdirs();
      File config = new File(kubeDir, "config");
      IOUtils.copy(getClass().getResourceAsStream(testConfig), new FileOutputStream(config));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
