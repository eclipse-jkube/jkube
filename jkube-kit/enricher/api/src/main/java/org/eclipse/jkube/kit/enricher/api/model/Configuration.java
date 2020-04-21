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
package org.eclipse.jkube.kit.enricher.api.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Configuration class which holds various configuration
 * related components
 *
 * @author roland
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class Configuration {

    /**
     * Project properties.
     */
    private Properties properties = new Properties();

    /**
     * List of image configuration used when building.
     */
    private List<ImageConfiguration> images;

    /**
     * Configuration influencing the resource generation.
     */
    private ResourceConfig resource;

    /**
     * Lookup plugin project configuration.
     */
    private BiFunction<String, String, Optional<Map<String,Object>>> pluginConfigLookup;

    /**
     * Lookup secret configuration.
     */
    private Function<String, Optional<Map<String,Object>>> secretConfigLookup;

    /**
     * Processor config which holds all the configuration for processors / enrichers.
     */
    private ProcessorConfig processorConfig;

    @Builder(toBuilder = true)
    public Configuration(
        Properties properties, @Singular List<ImageConfiguration> images, ResourceConfig resource,
        BiFunction<String, String, Optional<Map<String, Object>>> pluginConfigLookup,
        Function<String, Optional<Map<String, Object>>> secretConfigLookup, ProcessorConfig processorConfig) {

        this.properties = Optional.ofNullable(properties).orElse(new Properties());
        this.images = images;
        this.resource = resource;
        this.pluginConfigLookup = pluginConfigLookup;
        this.secretConfigLookup = secretConfigLookup;
        this.processorConfig = processorConfig;
    }

    /**
     * Gets plugin configuration values. Since there can be inner values,
     * it returns a Map of Objects where an Object can be a
     * simple type, List or another Map.
     *
     * @param system the underlying build platform (e.g. "maven")
     * @param id which plugin configuration to pick
     * @return configuration map specific to this id
     */
    public Optional<Map<String, Object>> getPluginConfiguration(String system, String id) {
        return pluginConfigLookup.apply(system, id);
    }

    /**
     * Gets configuration values. Since there can be inner values,
     * it returns a Map of Objects where an Object can be a
     * simple type, List or another Map.
     *
     * @param id id specific to the secret store
     * @return configuration map specific to this id
     */
    public Optional<Map<String, Object>> getSecretConfiguration(String id) {
        return secretConfigLookup.apply(id);
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public String getPropertyWithSystemOverride(String name) {
        String ret = System.getProperty(name);
        if (ret != null) {
            return ret;
        }
        return getProperty(name);
    }

}

