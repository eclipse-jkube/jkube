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

import java.util.Properties;


/**
 */
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
    public String get(Configs.Key key) {
        return get(key, key.def());
    }

    /**
     * Get a config value with a default
     * @param key key part to lookup. The whole key is build up from <code>prefix + "." + key</code>. If key is null,
     *            then only the prefix is used for the lookup (this is suitable for enrichers having only one config option)
     * @param defaultVal the default value to use when the no config is set
     * @return the value looked up or the default value.
     */
    public String get(Configs.Key key, String defaultVal) {
        String keyVal = key != null ? key.name() : "";
        String val = config != null ? config.getConfig(name, keyVal) : null;
        if (val == null) {
            String fullKey = GENERATOR_PROP_PREFIX + "." + name + "." + key;
            val = Configs.getSystemPropertyWithMavenPropertyAsFallback(properties, fullKey);
        }
        return val != null ? val : defaultVal;
    }

}