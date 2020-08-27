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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;

public class YamlUtil {

  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);
  private static final String EMPTY_YAML = "---\n";

  private YamlUtil() {
  }

    protected static Properties getPropertiesFromYamlResource(URL resource) {
        return getPropertiesFromYamlResource(null, resource);
    }

  protected static Properties getPropertiesFromYamlResource(String activeProfile, URL resource) {
    if (resource != null) {
      try {
        Properties properties = new Properties();
        List<String> profiles = splitYamlResource(resource);
        if (!profiles.isEmpty()) {
          properties.putAll(getPropertiesFromYamlString(getYamlFromYamlList(activeProfile, profiles)));
        }
        return properties;
      } catch (IOException e) {
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

  public static Properties getPropertiesFromYamlString(String yamlString) throws IOException {
    final Properties properties = new Properties();
    final SortedMap<String, ?> source = YAML_MAPPER.readValue(
        Optional.ofNullable(yamlString).filter(StringUtils::isNotBlank).orElse(EMPTY_YAML),
        new TypeReference<SortedMap<String, ?>>() {
        });
    if (source != null) {
      properties.putAll(getFlattenedMap(source));
    }
    return properties;
  }

  static List<String> splitYamlResource(URL resource) throws IOException {
    final List<String> serializedYamlList = new ArrayList<>();
    final List<Map<String, ?>> parsedList = YAML_MAPPER.readValues(
        YAML_FACTORY.createParser(resource), new TypeReference<Map<String, ?>>() {
        }).readAll();
    for (Map<String, ?> listEntry : parsedList) {
      serializedYamlList.add(YAML_MAPPER.writeValueAsString(listEntry));
    }
    return serializedYamlList;
  }

    static String getYamlFromYamlList(String pattern, List<String> yamlAsStringList) {
        if (pattern != null) {
            for (String yamlStr : yamlAsStringList) {
              if (yamlStr.contains(pattern)) {
                return yamlStr;
              }
            }
        }
        return yamlAsStringList.iterator().next();
    }

}

