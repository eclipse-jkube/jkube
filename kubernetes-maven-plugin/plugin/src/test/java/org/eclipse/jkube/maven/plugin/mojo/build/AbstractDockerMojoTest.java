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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.IOException;
import java.util.Collections;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractDockerMojoTest {

  private TestMojo mojo;
  private SettingsDecrypter settingsDecrypter;

  @BeforeEach
  void setUp() {
    settingsDecrypter = mock(SettingsDecrypter.class);
    mojo = new TestMojo();
    mojo.settings = new Settings();
    mojo.settingsDecrypter = settingsDecrypter;
    mojo.log = spy(new KitLogger.SilentLogger());
  }

  @Test
  void getRegistryConfig_shouldDecryptPasswordUsingSettingsDecrypter() {
    // Given
    Server decrypted = new Server();
    decrypted.setPassword("decrypted");
    SettingsDecryptionResult result = mock(SettingsDecryptionResult.class);
    when(result.getServer()).thenReturn(decrypted);
    when(result.getProblems()).thenReturn(Collections.emptyList());
    when(settingsDecrypter.decrypt(any())).thenReturn(result);
    // When
    RegistryConfig config = mojo.getRegistryConfig(null);
    // Then
    assertThat(config.getPasswordDecryptionMethod().apply("encrypted")).isEqualTo("decrypted");
  }

  @Test
  void getRegistryConfig_whenDecryptionFails_shouldReturnOriginalPasswordAndLogProblem() {
    // Given
    Server original = new Server();
    original.setPassword("encrypted");
    SettingsProblem problem = mock(SettingsProblem.class);
    when(problem.toString()).thenReturn("Failed to decrypt password: test error");
    SettingsDecryptionResult result = mock(SettingsDecryptionResult.class);
    when(result.getServer()).thenReturn(original);
    when(result.getProblems()).thenReturn(Collections.singletonList(problem));
    when(settingsDecrypter.decrypt(any())).thenReturn(result);
    // When
    RegistryConfig config = mojo.getRegistryConfig(null);
    // Then
    assertThat(config.getPasswordDecryptionMethod().apply("encrypted")).isEqualTo("encrypted");
    verify(mojo.log).error("Failed to decrypt password: %s", problem);
  }

  @Test
  void getRegistryConfig_whenDecryptionReturnsNullServer_shouldReturnOriginalPassword() {
    // Given
    SettingsDecryptionResult result = mock(SettingsDecryptionResult.class);
    when(result.getServer()).thenReturn(null);
    when(result.getProblems()).thenReturn(Collections.emptyList());
    when(settingsDecrypter.decrypt(any())).thenReturn(result);
    // When
    RegistryConfig config = mojo.getRegistryConfig(null);
    // Then
    assertThat(config.getPasswordDecryptionMethod().apply("original")).isEqualTo("original");
  }

  @Test
  void getRegistryConfig_withSpecificRegistry_shouldSetRegistry() {
    // When
    RegistryConfig config = mojo.getRegistryConfig("my-registry.io");
    // Then
    assertThat(config.getRegistry()).isEqualTo("my-registry.io");
  }

  private static class TestMojo extends AbstractDockerMojo {
    @Override
    protected void executeInternal() throws IOException {
    }
  }
}
