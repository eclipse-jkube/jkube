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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class RegistryConfig implements Serializable {

    private String registry;

    private List<RegistryServerConfiguration> settings;

    private AuthConfigFactory authConfigFactory;

    private boolean skipExtendedAuth;

    private Map authConfig;

    private transient UnaryOperator<String> passwordDecryptionMethod;

    public String getRegistry() {
        return registry;
    }

    public List<RegistryServerConfiguration> getSettings() {
        return settings;
    }

    public AuthConfigFactory getAuthConfigFactory() {
        return authConfigFactory;
    }

    public boolean isSkipExtendedAuth() {
        return skipExtendedAuth;
    }

    public Map getAuthConfig() {
        return authConfig;
    }

    public UnaryOperator<String> getPasswordDecryptionMethod() {
        return passwordDecryptionMethod;
    }

    public static class Builder {

        private RegistryConfig context = new RegistryConfig();

        public Builder() {
            this.context = new RegistryConfig();
        }

        public Builder(RegistryConfig context) {
            this.context = context;
        }

        public Builder registry(String registry) {
            context.registry = registry;
            return this;
        }

        public Builder settings(List<RegistryServerConfiguration> registryServerConfigurations) {
            context.settings = registryServerConfigurations;
            return this;
        }

        public Builder authConfigFactory(AuthConfigFactory authConfigFactory) {
            context.authConfigFactory = authConfigFactory;
            return this;
        }

        public Builder skipExtendedAuth(boolean skipExtendedAuth) {
            context.skipExtendedAuth = skipExtendedAuth;
            return this;
        }

        public Builder authConfig(Map authConfig) {
            context.authConfig = authConfig;
            return this;
        }

        public Builder passwordDecryptionMethod(UnaryOperator<String> passwordDecrypt) {
            context.passwordDecryptionMethod =passwordDecrypt;
            return this;
        }

        public RegistryConfig build() {
            return context;
        }
    }
}
