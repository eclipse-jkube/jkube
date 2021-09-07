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
package org.eclipse.jkube.kit.build.api.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;

/**
 * @author roland
 */
@EqualsAndHashCode
public class RegistryAuthConfig {


    private final Map<String, Map<String, String>> handlerConfig;
    private final Map<Kind, Map<String, String>> kindConfig;
    private final Map<String, String> defaultConfig;

    private boolean skipExtendedAuthentication = false;

    private String propertyPrefix;

    @Builder
    public RegistryAuthConfig(
        Map<String, Map<String, String>> handlerConfig,
        Map<Kind, Map<String, String>> kindConfig,
        @Singular("putDefaultConfig") Map<String, String> defaultConfig,
        boolean skipExtendedAuthentication,
        String propertyPrefix
    ) {
        this.handlerConfig = Optional.ofNullable(handlerConfig).orElse(new HashMap<>());
        this.kindConfig = Optional.ofNullable(kindConfig).orElse(new HashMap<>());
        this.defaultConfig = Optional.ofNullable(defaultConfig).orElse(new HashMap<>());
        this.skipExtendedAuthentication = skipExtendedAuthentication;
        this.propertyPrefix = propertyPrefix;
    }

    public String getConfigForHandler(String handlerName, String key) {
        return Optional.ofNullable(handlerConfig.get(handlerName)).map(m -> m.get(key)).orElse(null);
    }

    public String getUsername(Kind kind) {
        return getValueWithFallback(kind, RegistryAuth.USERNAME);
    }

    public String getPassword(Kind kind) {
        return getValueWithFallback(kind, RegistryAuth.PASSWORD);
    }

    public String getEmail(Kind kind) {
        return getValueWithFallback(kind, RegistryAuth.EMAIL);
    }

    public String getAuth(Kind kind) {
        return getValueWithFallback(kind, RegistryAuth.AUTH);
    }

    private String getValueWithFallback(Kind kind, String key) {
        return Optional.ofNullable(kindConfig.get(kind)).map(m -> m.get(key)).orElse(defaultConfig.get(key));
    }

    public boolean skipExtendedAuthentication() {
        return skipExtendedAuthentication;
    }

    public String extractFromProperties(Properties properties, Kind kind, String key) {
        String value = properties.getProperty(propertyPrefix + "." + kind.name().toLowerCase() + "." + key);
        if (value != null) {
            return value;
        }
        // Default is without kind
        return properties.getProperty(propertyPrefix + "." + key);
    }

    public enum Kind {
        PUSH,
        PULL;
    }

    public static class RegistryAuthConfigBuilder {
        public RegistryAuthConfigBuilder addKindConfig(Kind kind, String key, String value) {
            if (kindConfig == null) {
                kindConfig = new HashMap<>();
            }
            kindConfig.computeIfAbsent(kind, k -> new HashMap<>()).put(key, value);
            return this;
        }
        public RegistryAuthConfigBuilder addHandlerConfig(String id, String key, String value) {
            if (handlerConfig == null) {
                handlerConfig = new HashMap<>();
            }
            if (value != null) {
                handlerConfig.computeIfAbsent(id, i -> new HashMap<>()).put(key, value);
            }
            return this;
        }
    }
}
