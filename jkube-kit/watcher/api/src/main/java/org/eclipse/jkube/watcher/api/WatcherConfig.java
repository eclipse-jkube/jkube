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
package org.eclipse.jkube.watcher.api;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import java.util.Properties;


/**
 * @author nicola
 * @since 09/02/17
 */
public class WatcherConfig {

    private static final String WATCHER_PROP_PREFIX = "jkube.watcher";

    private final String name;
    private final ProcessorConfig config;
    private final Properties projectProperties;

    public WatcherConfig(Properties projectProperties, String name, ProcessorConfig config) {
        this.config = config;
        this.name = name;
        this.projectProperties = projectProperties;
    }

    /**
     * Get a configuration value
     *
     * @param key key to lookup
     * @return the value
     */
    public String get(Configs.Key key) {
        return get(key, key.def());
    }

    /**
     * Get a config value with a default. If no value is given, as a last resort, project properties are looked up.
     *
     * @param key key part to lookup. The whole key is build up from <code>prefix + "." + key</code>. If key is null,
     *            then only the prefix is used for the lookup (this is suitable for enrichers having only one config option)
     * @param defaultVal the default value to use when the no config is set
     * @return the value looked up or the default value.
     */
    public String get(Configs.Key key, String defaultVal) {
        String val = config != null ? config.getConfig(name, key.name()) : null;

        if (val == null) {
            String fullKey = WATCHER_PROP_PREFIX + "." + name + "." + key;
            val = Configs.getSystemPropertyWithMavenPropertyAsFallback(projectProperties, fullKey);
        }
        return val != null ? val : defaultVal;
    }

}
