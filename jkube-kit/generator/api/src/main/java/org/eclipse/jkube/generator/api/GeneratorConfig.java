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
package org.eclipse.jkube.generator.api;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import javax.annotation.Nonnull;
import java.util.Properties;

public class GeneratorConfig {

    private static final String GENERATOR_PROP_PREFIX = "jkube.generator";

    private final String name;
    private final ProcessorConfig config;
    private final Properties properties;

    public GeneratorConfig(Properties properties, String name, ProcessorConfig config) {
        this.config =  config;
        this.name = name;
        this.properties = properties;
    }

    /**
     * Get a configuration value
     *
     * @param key key to lookup. If it implements also DefaultValueProvider then use this for a default value
     * @return the defa
     */
    public String get(Configs.Config key) {
        return get(key, null);
    }

    /**
     * Get a config value with a default
     * @param key key part to lookup.
     * @param defaultVal the default value to use when the no config is set
     * @return the value looked up or the default value.
     */
    public String get(@Nonnull Configs.Config key, String defaultVal) {
        return ProcessorConfig.getConfigValue(config, name, GENERATOR_PROP_PREFIX, properties, key, defaultVal);
    }

    public String getWithFallback(Configs.Config key, String fallbackPropertyKey, String defaultVal) {
        final String value = get(key, Configs.getFromSystemPropertyWithPropertiesAsFallback(properties, fallbackPropertyKey));
        if (value != null) {
            return value;
        }
        return defaultVal;
    }

}
