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

import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class AbstractDockerMojoTest {

  private TestMojo mojo;
  private SecDispatcher secDispatcher;

  @BeforeEach
  void setUp() {
    secDispatcher = mock(SecDispatcher.class);
    mojo = new TestMojo();
    mojo.settings = new Settings();
    mojo.securityDispatcher = secDispatcher;
    mojo.log = spy(new KitLogger.SilentLogger());
  }

  @Test
  void getRegistryConfig_shouldDecryptPasswordUsingSecDispatcher() throws SecDispatcherException {
    // Given
    when(secDispatcher.decrypt("encrypted")).thenReturn("decrypted");
    // When
    RegistryConfig config = mojo.getRegistryConfig(null);
    // Then
    assertThat(config.getPasswordDecryptionMethod().apply("encrypted")).isEqualTo("decrypted");
  }

  @Test
  void getRegistryConfig_whenDecryptionFails_shouldReturnOriginalPassword() throws SecDispatcherException {
    // Given
    when(secDispatcher.decrypt("encrypted")).thenThrow(new SecDispatcherException("test"));
    // When
    RegistryConfig config = mojo.getRegistryConfig(null);
    // Then
    assertThat(config.getPasswordDecryptionMethod().apply("encrypted")).isEqualTo("encrypted");
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
