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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.MapUtil.getFlattenedMap;

public class YamlUtil {

  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);
  private static final String EMPTY_YAML = "---\n";

  private YamlUtil() {
  }

  public static Properties getPropertiesFromYamlResource(URL resource) {
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

  public static boolean isYaml(File file) {
    return file.getName().toLowerCase().matches(".*?\\.ya?ml$");
  }

  public static List<File> listYamls(File directory) {
    return Stream.of(Optional.ofNullable(directory).map(File::listFiles).orElse(new File[0]))
      .filter(File::isFile)
      .filter(YamlUtil::isYaml)
      .collect(Collectors.toList());
  }

  /**
   * Merges two YAML strings by performing deep property-level merge.
   *
   * @param existingYaml the existing YAML content
   * @param newYaml the new YAML content to merge
   * @return merged YAML as string
   * @throws IOException if YAML parsing fails
   */
  public static String mergeYaml(String existingYaml, String newYaml) throws IOException {
    try {
      // Use plain YAML mapper to get raw Map/List structures, not Kubernetes-typed objects
      Object existing = YAML_MAPPER.readValue(existingYaml, Object.class);
      Object newObj = YAML_MAPPER.readValue(newYaml, Object.class);
      Object merged = deepMerge(existing, newObj);
      return YAML_MAPPER.writeValueAsString(merged);
    } catch (Exception e) {
      throw new IOException("Failed to parse and merge YAML content: " + e.getMessage(), e);
    }
  }

  /**
   * Deep merges two objects following these rules:
   * - Maps: recursively merge at property level
   * - Lists: concatenate
   * - Primitives or type mismatch: new value overwrites existing
   *
   * @param existing the existing object
   * @param newObj the new object to merge
   * @return merged object
   */
  @SuppressWarnings("unchecked")
  private static Object deepMerge(Object existing, Object newObj) {
    if (existing instanceof Map && newObj instanceof Map) {
      // Deep merge maps at property level
      Map<String, Object> existingMap = new LinkedHashMap<>((Map<String, Object>) existing);
      Map<String, Object> newMap = (Map<String, Object>) newObj;

      for (Map.Entry<String, Object> entry : newMap.entrySet()) {
        String key = entry.getKey();
        Object newValue = entry.getValue();

        if (existingMap.containsKey(key)) {
          // Key exists in both - recursively merge
          existingMap.put(key, deepMerge(existingMap.get(key), newValue));
        } else {
          // New key - add it
          existingMap.put(key, newValue);
        }
      }
      return existingMap;
    } else if (existing instanceof List && newObj instanceof List) {
      // Merge lists by concatenation
      List<Object> merged = new ArrayList<>((List<Object>) existing);
      merged.addAll((List<Object>) newObj);
      return merged;
    } else {
      // For primitives or type mismatch, new value overwrites existing
      return newObj;
    }
  }
}

