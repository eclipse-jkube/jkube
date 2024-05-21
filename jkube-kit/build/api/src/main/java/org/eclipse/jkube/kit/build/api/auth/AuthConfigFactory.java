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

import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public interface AuthConfigFactory {
  AuthConfig createAuthConfig(
    boolean isPush,
    boolean skipExtendedAuth,
    Map authConfig,
    List<RegistryServerConfiguration> settings,
    String user,
    String registry,
    UnaryOperator<String> passwordDecryptionMethod
  ) throws IOException;
}
