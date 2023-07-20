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
package org.eclipse.jkube.kit.enricher.api;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

/**
 * @author roland
 */
public class EnricherConfig {

    private static final String ENRICHER_PROP_PREFIX = "jkube.enricher";

    private final String name;
    private final EnricherContext context;

    public EnricherConfig(String name, EnricherContext context) {
        this.name = name;
        this.context = context;
    }

    /**
     * Get a configuration value
     *
     * @param key key to lookup.
     * @return the configuration value
     */
    public String get(Configs.Config key) {
        return get(key, null);
    }

    /**
     * Get a config value with a default. If no value is given, as a last resort, project properties are looked up.
     *
     * @param key key part to lookup. The whole key is build up from <code>prefix + "." + key</code>. If key is null,
     *            then only the prefix is used for the lookup (this is suitable for enrichers having only one config option)
     * @param defaultVal the default value to use when the no config is set
     * @return the value looked up or the default value.
     */
    public String get(Configs.Config key, String defaultVal) {
        return ProcessorConfig.getConfigValue(
            context.getConfiguration().getProcessorConfig(), name, ENRICHER_PROP_PREFIX, context.getProperties(), key, defaultVal);
    }

}
