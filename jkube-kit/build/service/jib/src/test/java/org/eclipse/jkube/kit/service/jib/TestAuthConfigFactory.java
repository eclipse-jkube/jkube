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
package org.eclipse.jkube.kit.service.jib;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class TestAuthConfigFactory implements AuthConfigFactory {

  @Override
  public AuthConfig createAuthConfig(boolean isPush, boolean skipExtendedAuth, Map authConfig, List<RegistryServerConfiguration> settings, String user, String registry, UnaryOperator<String> passwordDecryptionMethod) {
    if (settings == null) {
      return null;
    }
    for (RegistryServerConfiguration setting : settings) {
      if (setting.getId().equals(registry)) {
        return AuthConfig.builder()
          .username(setting.getUsername())
          .password(setting.getPassword())
          .build();
      }
    }
    return null;
  }
}
