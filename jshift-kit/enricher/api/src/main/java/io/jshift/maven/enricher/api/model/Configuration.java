package io.jshift.maven.enricher.api.model;

import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.config.resource.ProcessorConfig;
import io.jshift.kit.config.resource.ResourceConfig;

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
 * @since 12.10.18
 */
public class Configuration {

    // Project properties
    private Properties properties = new Properties();

    // List of image configuration used when building
    private List<ImageConfiguration> images;

    // Configuration influencing the resource generation
    private ResourceConfig resource;

    // Lookup plugin project configuration
    private BiFunction<String, String, Optional<Map<String,Object>>> pluginConfigLookup;

    // Lookup secret configuration
    private Function<String, Optional<Map<String,Object>>> secretConfigLookup;


    // Processor config which holds all the configuration for processors / enrichers
    private ProcessorConfig processorConfig;

    private Configuration() {
    }

    public Properties getProperties() {
        return properties;
    }

    public Optional<List<ImageConfiguration>> getImages() {
        return Optional.ofNullable(images);
    }

    public Optional<ResourceConfig> getResource() {
        return Optional.ofNullable(resource);
    }

    public Optional<ProcessorConfig> getProcessorConfig() {
        return Optional.ofNullable(processorConfig);
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

    public static class Builder {
        private Configuration cfg = new Configuration();

        public Builder properties(Properties props) {
            cfg.properties = props;
            return this;
        }

        public Builder images(List<ImageConfiguration> images) {
            cfg.images = images;
            return this;
        }

        public Builder resource(ResourceConfig resource) {
            cfg.resource = resource;
            return this;
        }

        public Builder processorConfig(ProcessorConfig config) {
            cfg.processorConfig = config;
            return this;
        }

        public Builder pluginConfigLookup(BiFunction<String, String, Optional<Map<String,Object>>> pluginConfigLookup) {
            cfg.pluginConfigLookup = pluginConfigLookup;
            return this;
        }

        public Builder secretConfigLookup(Function<String, Optional<Map<String,Object>>> secretConfigLookup) {
            cfg.secretConfigLookup = secretConfigLookup;
            return this;
        }

        public Configuration build() {
            return cfg;
        }
    }
}

