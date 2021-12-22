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
package org.eclipse.jkube.kit.config.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.Comparator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.Named;

/**
 * Configuration for enrichers and generators
 *
 * @author roland
 */
@Getter
@Setter
@EqualsAndHashCode
public class ProcessorConfig {

    public static final ProcessorConfig EMPTY = new ProcessorConfig();
    /**
     * Modules to include, should hold <code>&lt;include&gt;</code> elements
     */
    private List<String> includes;

    /**
     * Modules to exclude, should hold <code>&lt;exclude&gt;</code> elements
     */
    private Set<String> excludes;

    /**
     * Configuration for enricher / generators
     */
    private Map<String, Map<String, Object>> config;

    public ProcessorConfig() {
        this(null, null, null);
    }

    public ProcessorConfig(List<String> includes, Set<String> excludes, Map<String, Map<String, Object>> config) {
        this.includes = includes != null ? includes : new ArrayList<>();
        this.excludes = excludes != null ? excludes : new HashSet<>();
        this.config = config != null ? config : new HashMap<>();
    }

    /**
     * Plexus deserialization specific setter.
     *
     * <p> See StackOverflow#38642613 why a "TreeMap" is used as parameter and not "Map&lt;String, String&gt;".
     *
     * <p> Plexus won't deserialize complex (Map with nested Map) structures e.g.
     *
     * <p> Will be parsed fully:
     * <pre>{@code
     * <config>
     *   <first-level>valid</first-level>
     * </config>
     * }</pre>
     *
     * <p> Some fields will be ignored:
     * <pre>{@code
     * <config>
     *   <first-level>valid</first-level>
     *   <second-complex-level-ignored>
     *     <ignored>ignored-value</ignored>
     *   </second-complex-level-ignored>
     * </config>
     * }</pre>
     * @see <a href="http://stackoverflow.com/questions/38628399/using-map-of-maps-as-maven-plugin-parameters/38642613">StackOverflow#38642613</a>
     */
    @SuppressWarnings({"squid:S3740", "rawtypes", "unchecked"})
    public void setConfig(Map<String, TreeMap> config) {
        this.config = new HashMap<>();
        config.forEach((key, value) -> this.config.put(key, value));
    }

    /**
     * Order elements according to the order provided by the include statements.
     * If no includes has been configured, return the given list unaltered.
     * Otherwise arrange the elements from the list in to the include order and return a new
     * list.
     *
     * If an include specifies an element which does not exist, an exception is thrown.
     *
     * @param namedList the list to order
     * @param type a description used in an error message (like 'generator' or 'enricher')
     * @param <T> the concrete type
     * @return the ordered list according to the algorithm described above
     */
    public <T extends Named> List<T> prepareProcessors(List<T> namedList, String type) {
        List<T> ret = new ArrayList<>();
        Map<String, T> lookup = new HashMap<>();
        for (T named : namedList) {
            lookup.put(named.getName(), named);
        }
        for (String inc : includes) {
            if (use(inc)) {
                T named = lookup.get(inc);
                if (named == null) {
                    List<String> keys = new ArrayList<>(lookup.keySet());
                    keys.sort(Comparator.naturalOrder());
                    throw new IllegalArgumentException(
                            "No " + type + " with name '" + inc +
                                    "' found to include. " +
                                    "Please check spelling in your profile / config and your project dependencies. Included " + type + "s: " +
                                    StringUtils.join(keys,", "));
                }
                ret.add(named);
            }
        }
        return ret;
    }

    public boolean use(String inc) {
        return !excludes.contains(inc) && includes.contains(inc);
    }

    /**
     * Clone a processorConfig, resulting in a copy of the given config
     *
     * @param processorConfig config to clone
     * @return the cloned config
     */
    public static ProcessorConfig cloneProcessorConfig(ProcessorConfig processorConfig) {
        return mergeProcessorConfigs(processorConfig);
    }

    /**
     * Returns a value from the provided {@link ProcessorConfig}, {@link Properties}, <code>defaultValue</code>
     * corresponding to the {@link Configs.Config#getKey()} with the following order of precedence:
     *  <ul>
     *    <li>Existing value in ProcessorConfig#config and corresponding configName.</li>
     *    <li><i>Or</i> existing value in provided Properties with pattern
     *      <code>$propertyPrefix.$configName.$Config#getKey</code>.</li>
     *    <li><i>Or</i> provided defaultValue if not null.</li>
     *    <li><i>Or</i> Configs.Config#getDefaultValue.</li>
     *  </ul>
     *
     * @param config from which to retrieve the value in first try.
     * @param configName Name of the config (Map) from which to retrieve te value (also used for property key).
     * @param propertyPrefix Prefix to prepend the property key with.
     * @param properties Properties from which to retrieve the value
     * @param key Config from which to get the key or default fallback value.
     * @param defaultValue Default value to return in case no value is found in config or properties
     * @return the resulting value of null if none is found.
     */
    public static String getConfigValue(
        ProcessorConfig config, String configName, String propertyPrefix, Properties properties, Configs.Config key,
        String defaultValue) {

        final Object configValue = config != null ? config.getConfig(configName, key) : null;
        if (configValue != null) {
            return configValue.toString();
        }
        final String fullKey = String.format("%s.%s.%s", propertyPrefix, configName, key.getKey());
        final String valueInProperties = Configs.getFromSystemPropertyWithPropertiesAsFallback(properties, fullKey);
        if (valueInProperties != null) {
            return valueInProperties;
        }
        return defaultValue != null ? defaultValue : key.getDefaultValue();
    }

    private Object getConfig(String configName, Configs.Config key) {
        return Optional.ofNullable(config).map(c-> c.get(configName)).map(entries -> entries.get(key.getKey())).orElse(null);
    }

    /**
     * Merge in another processor configuration, with a lower priority. I.e. the latter a config is
     * in the argument list, the less priority it has. This means:
     *
     * <ul>
     *     <li>A configuration earlier in the list overrides configuration later</li>
     *     <li>Includes and exclude earlier in the list take precedence of the includes/excludes later in the list</li>
     * </ul>
     *
     * @param processorConfigs configs to merge into the current config
     * @return a merged configuration for convenience of chaining and returning. This is a new object and can e.g.
     */
    public static ProcessorConfig mergeProcessorConfigs(ProcessorConfig ... processorConfigs) {
        // Merge the configuration
        Map<String, Map<String, Object>> configs = mergeConfig(processorConfigs);

        // Get all includes
        Set<String> excludes = mergeExcludes(processorConfigs);

        // Find the set of includes, which are the ones from the profile + the ones configured
        List<String> includes = mergeIncludes(processorConfigs);

        return new ProcessorConfig(includes, excludes, configs);
    }

    private static Set<String> mergeExcludes(ProcessorConfig ... configs) {
        return Stream.of(configs).filter(Objects::nonNull).map(ProcessorConfig::getExcludes).flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    private static List<String> mergeIncludes(ProcessorConfig ... configs) {
        return Stream.of(configs).filter(Objects::nonNull).map(ProcessorConfig::getIncludes).flatMap(List::stream)
            .distinct().collect(Collectors.toList());
    }

    private static Map<String, Map<String, Object>> mergeConfig(ProcessorConfig ... processorConfigs) {
        final Map<String, Map<String, Object>> ret= new HashMap<>();
        final Predicate<Map.Entry<?, ? >> nonNullEntry = e -> e.getKey() != null && e.getValue() !=null;
        IntStream.rangeClosed(1, processorConfigs.length).mapToObj(i -> processorConfigs[processorConfigs.length - i])
            .filter(Objects::nonNull)
            .map(ProcessorConfig::getConfig).map(Map::entrySet).flatMap(Set::stream)
            .filter(nonNullEntry)
            .forEach(configEntry -> {
                final Map<String, Object> existingConfigEntry = ret.computeIfAbsent(configEntry.getKey(), k -> new HashMap<>());
                configEntry.getValue().entrySet().stream().filter(nonNullEntry)
                    .forEach(e -> existingConfigEntry.put(e.getKey(), e.getValue()));
            });
        return ret;
    }

}

