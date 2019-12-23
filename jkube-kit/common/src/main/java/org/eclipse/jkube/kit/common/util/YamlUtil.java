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
package org.eclipse.jkube.kit.common.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import org.yaml.snakeyaml.Yaml;

public class YamlUtil {
    protected static Properties getPropertiesFromYamlResource(URL resource) {
        return getPropertiesFromYamlResource(null, resource);
    }

    protected static Properties getPropertiesFromYamlResource(String activeProfile, URL resource) {
        if (resource != null) {
            try {
                Properties properties = new Properties();
                // Splitting file for the possibility of different profiles, by default
                // only first profile would be considered.
                List<String> profiles = getYamlListFromFile(resource);
                if (profiles.size() > 0) {
                    try {
                        properties.putAll(getPropertiesFromYamlString(getYamlFromYamlList(activeProfile, profiles)));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format("Spring Boot configuration file %s is not formatted correctly. %s",
                                resource.toString(), e.getMessage()));
                    }
                }
                return properties;
            } catch (IOException | URISyntaxException e) {
                throw new IllegalStateException("Error while reading Yaml resource from URL " + resource, e);
            }
        }
        return new Properties();
    }

    /**
     * Build a flattened representation of the Yaml tree. The conversion is compliant with the thorntail spring-boot rules.
     */
    private static Map<String, Object> getFlattenedMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private static void buildFlattenedMap(Map<String, Object> result, Map<?, ?> source, String path) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object keyObject = entry.getKey();

            String key;
            if (keyObject instanceof String) {
                key = (String) keyObject;
            } else if (keyObject instanceof Number) {
                key = String.valueOf(keyObject);
            } else {
                // If user creates a wrong application.yml then we get a runtime classcastexception
                throw new IllegalArgumentException(String.format("Expected to find a key of type String but %s with content %s found.",
                        keyObject.getClass(), keyObject.toString()));
            }

            if (path !=null && path.trim().length()>0) {
                if (key.startsWith("[")) {
                    key = path + key;
                }
                else {
                    key = path + "." + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof Map) {

                Map<?, ?> map = (Map<?, ?>) value;
                buildFlattenedMap(result, map, key);
            }
            else if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result,
                            Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            }
            else {
                result.put(key, (value != null ? value.toString() : ""));
            }
        }
    }

    public static Properties getPropertiesFromYamlString(String yamlString) throws IllegalArgumentException {
        Yaml yaml = new Yaml();
        Properties properties = new Properties();

        @SuppressWarnings("unchecked")
        SortedMap<String, Object> source = yaml.loadAs(yamlString, SortedMap.class);
        if (source != null) {
            properties.putAll(getFlattenedMap(source));
        }
        return properties;
    }

    public static List<String> getYamlListFromFile(URL resource) throws URISyntaxException, IOException {
        String fileAsString = new String(Files.readAllBytes(Paths.get(resource.toURI())));
        String[] profiles = fileAsString.split("---");
        return Arrays.asList(profiles);
    }

    public static String getYamlFromYamlList(String pattern, List<String> yamlAsStringList) {
        if (pattern != null) {
            for (String yamlStr : yamlAsStringList) {
                if (yamlStr.contains(pattern))
                    return yamlStr;
            }
        }
        return yamlAsStringList.get(0);
    }

}

