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
package org.eclipse.jkube.kit.build.api.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.api.helper.KubernetesConfigAuthUtil.readKubeConfigAuth;

class KubernetesConfigAuthUtilTest {

  private File home;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    home = Files.createDirectory(temporaryFolder.resolve("home")).toFile();
  }

  @Test
  void readKubeConfigAuth_withNoKubeConfig() {
    executeWithTempSystemUserHome(() -> {
      final AuthConfig result = readKubeConfigAuth();
      assertThat(result).isNull();
    });
  }

  @Test
  void readKubeConfigAuth_withEmptyFile() throws IOException {
    withKubeConfig("kube-config-empty.yaml");
    executeWithTempSystemUserHome(() -> {
      final AuthConfig result = readKubeConfigAuth();
      assertThat(result).isNull();
    });
  }

  @Test
  void readKubeConfigAuth_withMissingCurrentContext() throws IOException {
    withKubeConfig("kube-config-missing-current-context.yaml");
    executeWithTempSystemUserHome(() -> {
      final AuthConfig result = readKubeConfigAuth();
      assertThat(result).isNull();
    });
  }

  @Test
  void readKubeConfigAuth_withValidKubeConfig() throws IOException {
    withKubeConfig("kube-config.yaml");
    executeWithTempSystemUserHome(() -> {
      final AuthConfig result = readKubeConfigAuth();
      assertThat(result)
          .hasFieldOrPropertyWithValue("username", "user")
          .hasFieldOrPropertyWithValue("password", "the-token");
    });
  }

  private void withKubeConfig(String kubeConfigFile) throws IOException {
    final File kube = new File(home, ".kube");
    FileUtils.forceMkdir(kube);
    final File kubeConfig = new File(kube, "config");
    IOUtils.copy(
        KubernetesConfigAuthUtilTest.class.getResourceAsStream(
            "/org/eclipse/jkube/kit/build/api/helper/kubernetes-config-auth/" + kubeConfigFile),
            Files.newOutputStream(kubeConfig.toPath())
    );
  }
  private void executeWithTempSystemUserHome(Runnable executor) {
    String userHome = System.getProperty("user.home");
    try {
      System.setProperty("user.home", home.getAbsolutePath());

      executor.run();
    } finally {
      System.setProperty("user.home", userHome);
    }

  }
}
