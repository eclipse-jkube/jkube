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

import java.util.function.UnaryOperator;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthHandler;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 21.10.18
 */
public class FromConfigRegistryAuthHandler implements RegistryAuthHandler {
    private final RegistryAuthConfig registryAuthConfig;
    private final KitLogger log;

    public FromConfigRegistryAuthHandler(RegistryAuthConfig registryAuthConfig, KitLogger log) {
        this.registryAuthConfig = registryAuthConfig;
        this.log = log;
    }

    @Override
    public String getId() {
        return "config";
    }

    @Override
    public AuthConfig create(RegistryAuthConfig.Kind kind, String user, String registry, UnaryOperator<String> decryptor) {
        // Get configuration from global plugin config

        if (registryAuthConfig.getUsername(kind) != null) {
            if (registryAuthConfig.getPassword(kind) == null) {
                throw new IllegalArgumentException("No 'password' given while using <authConfig> in configuration for mode " + kind);
            }
            log.debug("AuthConfig: credentials from plugin config");
            return AuthConfig.fromRegistryAuthConfig(registryAuthConfig, kind, decryptor);
        }
        return null;
    }

}
