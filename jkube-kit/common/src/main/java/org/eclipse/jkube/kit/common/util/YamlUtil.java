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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
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
   * @param newYaml      the new YAML content to merge
   * @return merged YAML as string
   * @throws IOException if YAML parsing fails
   */
  public static String mergeYaml(String existingYaml, String newYaml) throws IOException {
    try {
      // If existing YAML is effectively empty (null, blank, or only comments), return new YAML
      if (isEffectivelyEmpty(existingYaml)) {
        return newYaml;
      }
      // If new YAML is effectively empty, return existing YAML
      if (isEffectivelyEmpty(newYaml)) {
        return existingYaml;
      }

      Object existing = YAML_MAPPER.readValue(existingYaml, Object.class);
      Object newObj = YAML_MAPPER.readValue(newYaml, Object.class);
      Object merged = deepMerge(existing, newObj);
      return YAML_MAPPER.writeValueAsString(merged);
    } catch (Exception e) {
      throw new IOException("Failed to parse and merge YAML content: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if a YAML string is effectively empty (null, blank, or contains only comments).
   *
   * @param yaml the YAML string to check
   * @return true if the YAML is effectively empty
   */
  private static boolean isEffectivelyEmpty(String yaml) {
    if (yaml == null || yaml.trim().isEmpty()) {
      return true;
    }
    // Remove comments and whitespace to check if there's any actual content
    String[] lines = yaml.split("\n");
    for (String line : lines) {
      String trimmed = line.trim();
      // Skip empty lines, comments, and YAML document separator
      if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.equals("---")) {
        return false;
      }
    }
    return true;
  }

  /**
   * Deep merges two objects following these rules:
   * - Maps: recursively merge at property level
   * - Lists: merge without duplicates (deduplicate based on equality)
   * - Primitives or type mismatch: new value overwrites existing
   *
   * @param existing the existing object
   * @param newObj the new object to merge
   * @return merged object
   */
  @SuppressWarnings("unchecked")
  private static Object deepMerge(Object existing, Object newObj) {
    if (existing instanceof Map && newObj instanceof Map) {
      return mergeMaps((Map<String, Object>) existing, (Map<String, Object>) newObj);
    } else if (existing instanceof List && newObj instanceof List) {
      return mergeLists((List<Object>) existing, (List<Object>) newObj);
    } else {
      // For primitives or type mismatch, new value overwrites existing
      return newObj;
    }
  }

  private static Map<String, Object> mergeMaps(Map<String, Object> existing, Map<String, Object> newMap) {
    // Deep merge maps at property level
    Map<String, Object> existingMap = new LinkedHashMap<>(existing);

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
  }

  private static List<Object> mergeLists(List<Object> existingList, List<Object> newList) {
    // Use LinkedHashSet to maintain order and avoid duplicates
    Set<Object> merged = new LinkedHashSet<>(existingList);

    // Add new items only if they don't already exist in the merged set
    for (Object newItem : newList) {
      if (!containsEquivalent(new ArrayList<>(merged), newItem)) {
        merged.add(newItem);
      }
    }

    return new ArrayList<>(merged);
  }

  /**
   * Checks if a list contains an item that is equivalent to the given item.
   * Uses deep equality comparison for Maps and Lists.
   *
   * @param list the list to search
   * @param item the item to find
   * @return true if the list contains an equivalent item
   */
  private static boolean containsEquivalent(List<Object> list, Object item) {
    for (Object existing : list) {
      if (areEquivalent(existing, item)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if two objects are equivalent using deep comparison.
   *
   * @param obj1 first object
   * @param obj2 second object
   * @return true if objects are equivalent
   */
  @SuppressWarnings("unchecked")
  private static boolean areEquivalent(Object obj1, Object obj2) {
    if (obj1 == obj2) {
      return true;
    }
    if (obj1 == null || obj2 == null) {
      return false;
    }
    if (obj1.getClass() != obj2.getClass()) {
      return false;
    }

    if (obj1 instanceof Map && obj2 instanceof Map) {
      return areMapsEquivalent((Map<String, Object>) obj1, (Map<String, Object>) obj2);
    } else if (obj1 instanceof List && obj2 instanceof List) {
      return areListsEquivalent((List<Object>) obj1, (List<Object>) obj2);
    } else {
      return obj1.equals(obj2);
    }
  }

  private static boolean areMapsEquivalent(Map<String, Object> map1, Map<String, Object> map2) {
    if (map1.size() != map2.size()) {
      return false;
    }

    for (Map.Entry<String, Object> entry : map1.entrySet()) {
      String key = entry.getKey();
      if (!map2.containsKey(key)) {
        return false;
      }
      if (!areEquivalent(entry.getValue(), map2.get(key))) {
        return false;
      }
    }
    return true;
  }

  private static boolean areListsEquivalent(List<Object> list1, List<Object> list2) {
    if (list1.size() != list2.size()) {
      return false;
    }

    for (int i = 0; i < list1.size(); i++) {
      if (!areEquivalent(list1.get(i), list2.get(i))) {
        return false;
      }
    }
    return true;
  }
}

