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

import java.io.IOException;
import java.util.Base64;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuth;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author roland
 * @since 23.10.18
 */
public class FromConfigRegistryAuthHandlerTest {

  private KitLogger log;

  @Test
  public void testFromPluginConfiguration() throws IOException {
    log = mock(KitLogger.class);
    FromConfigRegistryAuthHandler handler = new FromConfigRegistryAuthHandler(setupAuthConfigFactoryWithConfigData(), log);

    AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
    verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
  }

  protected RegistryAuthConfig setupAuthConfigFactoryWithConfigData() {
    return RegistryAuthConfig.builder()
        .skipExtendedAuthentication(false)
        .putDefaultConfig(RegistryAuth.USERNAME, "roland")
        .putDefaultConfig(RegistryAuth.PASSWORD, "secret")
        .putDefaultConfig(RegistryAuth.EMAIL, "roland@jolokia.org")
        .build();
  }

  private RegistryAuthConfig setupAuthConfigFactoryWithConfigDataForKind(RegistryAuthConfig.Kind kind) {
    return RegistryAuthConfig.builder()
        .skipExtendedAuthentication(false)
        .addKindConfig(kind, RegistryAuth.USERNAME, "roland")
        .addKindConfig(kind, RegistryAuth.PASSWORD, "secret")
        .addKindConfig(kind, RegistryAuth.EMAIL, "roland@jolokia.org")
        .build();
  }

  @Test
  public void testFromPluginConfigurationPull() {
    log = mock(KitLogger.class);
    FromConfigRegistryAuthHandler handler = new FromConfigRegistryAuthHandler(
        setupAuthConfigFactoryWithConfigDataForKind(RegistryAuthConfig.Kind.PULL), log);

    AuthConfig config = handler.create(RegistryAuthConfig.Kind.PULL, null, null, s -> s);
    verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
  }

  @Test
  public void testFromPluginConfigurationFailed() {
    log = mock(KitLogger.class);
    FromConfigRegistryAuthHandler handler = new FromConfigRegistryAuthHandler(
        RegistryAuthConfig.builder().putDefaultConfig(RegistryAuth.USERNAME, "admin").build(), log);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s));
    assertThat(exception).hasMessageContaining("password");
  }

  private void verifyAuthConfig(AuthConfig config, String username, String password, String email) {
    log = mock(KitLogger.class);
    JsonObject params = new Gson().fromJson(new String(Base64.getDecoder().decode(config.toHeaderValue(log).getBytes())),
        JsonObject.class);
    assertEquals(username, params.get("username").getAsString());
    assertEquals(password, params.get("password").getAsString());
    if (email != null) {
      assertEquals(email, params.get("email").getAsString());
    }
  }

}
