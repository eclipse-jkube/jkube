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
package org.eclipse.jkube.generator.api.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eclipse.jkube.generator.api.PortsExtractor;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractPortsExtractor implements PortsExtractor {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private static final String DOT = ".";
    private static final String JSON_EXTENSION = ".json";
    private static final String YAML_EXTENSION = ".yaml";
    private static final String YML_EXTENSION = ".yml";
    private static final String PROPERTIES_EXTENSION = ".properties";

    private static final String NUMBER_REGEX = "\\d+";

    private static final String PORT_REGEX = "([a-zA-Z0-9_]+)(([\\.-_]+p)|([P]))ort";
    private static final Pattern PORT_PATTERN = Pattern.compile(PORT_REGEX);

    protected final PrefixedLogger log;

    public AbstractPortsExtractor(PrefixedLogger log) {
        this.log = log;
    }

    /**
     * @return The name of the system property that points to the path of the configuration file.
     */
    public abstract String getConfigPathPropertyName();

    /**
     * Finds the name of the configuration file from the {@link JKubeProject}.
     * @param project   The {@link JKubeProject} to use.
     * @return          The path to the configuration file or null if none has been found
     */
    public abstract String getConfigPathFromProject(JKubeProject project);


    public File getConfigLocation(JKubeProject project) {
        String propertyName = getConfigPathPropertyName();
        if (StringUtils.isBlank(propertyName)) {
            return null;
        }
        // The system property / Maven property has priority over what is specified in the pom.
        String configPath = Configs.getSystemPropertyWithMavenPropertyAsFallback(project.getProperties(), getConfigPathPropertyName());
        if (configPath == null) {
            configPath = getConfigPathFromProject(project);
        }
        if (StringUtils.isBlank(configPath)) {
            return null;
        }
        return Paths.get(configPath).toFile();
    }

    @Override
    public Map<String, Integer> extract(JKubeProject project) {
        Map<String, Integer> answer = new HashMap<>();
        File configFile = getConfigLocation(project);
        if (configFile == null) {
            // No config file configured
            return answer;
        }
        if (!configFile.exists()) {
            log.warn("Could not find config: %s. Ignoring.", configFile.getAbsolutePath());
            return answer;
        }

        try {
            Map<String, String> configMap = readConfig(configFile);
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String key = entry.getKey();
                if (isValidPortPropertyKey(key)) {
                    addPortIfValid(answer, key, entry.getValue());
                }
            }
            return answer;
        } catch (IOException e) {
            log.warn("Error reading config: [%s], due to: [%s]. Ignoring.", configFile.getAbsolutePath(), e.getMessage());
            return answer;
        }
    }



    /**
     * Reads the configuration from the file.
     * @param f
     * @return
     * @throws IOException
     */
    private Map<String, String> readConfig(File f) throws IOException {
        Map<String, String> map;
        if (f.getName().endsWith(JSON_EXTENSION)) {
            map = flatten(JSON_MAPPER.readValue(f, Map.class));
        } else if (f.getName().endsWith(YAML_EXTENSION) || f.getName().endsWith(YML_EXTENSION)) {
            map = flatten(YAML_MAPPER.readValue(f, Map.class));
        } else if (f.getName().endsWith(PROPERTIES_EXTENSION)) {
            Properties properties = new Properties();
            properties.load(new FileInputStream(f));
            map = propertiesToMap(properties);
        } else {
            throw new IllegalArgumentException("Can't read configuration from: [" + f.getName() + "]. Unknown file extension.");
        }
        return map;
    }



    /**
     * Flattens a nested map into a Map<String, String>.
     * @param map   The target map.
     * @return      The flattened map.
     */
    private Map<String, String> flatten(Map map) {
        Map<String, String> flat = new HashMap<>();
        for (Object key : map.keySet()) {
            String stringKey = String.valueOf(key);
            Object value = map.get(key);
            if (value instanceof String) {
                flat.put(stringKey, (String) value);
            } else if (value instanceof Map) {
                for (Map.Entry<String, String> entry : flatten((Map) value).entrySet()) {
                    flat.put(
                            new StringBuilder(stringKey).append(DOT).append(entry.getKey()).toString(),
                            entry.getValue());
                }
            } else {
                flat.put(stringKey, String.valueOf(value));
            }
        }
        return flat;
    }


    /**
     * Converts a {@link Properties} object to a {@link Map}.
     * @param properties    The properties object.
     * @return              The map.
     */
    private Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> map = new HashMap<>();
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }

    /**
     * Checks if the given string matches the port property key convention.
     * The regex for the convention is ([a-zA-Z0-9_]+)(([\.-_]+p{1})|([P]{1}))ort.
     * @param candidate The string to check
     * @return
     */
    private boolean isValidPortPropertyKey(String candidate) {
        return PORT_PATTERN.matcher(candidate).matches();
    }

    /**
     * Adds a port to the list.
     * @param map   The list.
     * @param key   The key.
     * @param port  The candidate port.
     */
    private void addPortIfValid(Map<String, Integer> map, String key, String port) {
        if (StringUtils.isNotBlank(port)) {
            String t = port.trim();
            if (t.matches(NUMBER_REGEX)) {
                map.put(key, Integer.parseInt(t));
            }
        }
    }
}
