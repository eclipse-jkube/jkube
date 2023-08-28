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
package org.eclipse.jkube.kit.enricher.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.KindFilenameMapperUtil;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.MappingConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.FILENAME_PATTERN;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING;

public class KubernetesResourceFragments {

  private static final Map<String, MappingConfig> FILENAME_TO_MAPPING_CONFIG_MAPPER = new HashMap<>();
  private static final Map<String, String> KIND_TO_FILENAME_MAPPER = new HashMap<>();
  private static final Consumer<MappingConfig> KIND_FILENAME_MAPPER_UPDATER = mappingConfig -> {
    if (!mappingConfig.isValid()) {
      throw new IllegalArgumentException(String.format("Invalid mapping for Kind %s and Filename Types %s",
        mappingConfig.getKind(), mappingConfig.getFilenameTypes()));
    }
    final String[] fileNameTypes = mappingConfig.getFilenamesAsArray();
    for (String filenameType : fileNameTypes) {
      FILENAME_TO_MAPPING_CONFIG_MAPPER.put(filenameType.toLowerCase(), mappingConfig);
    }
    // In previous version, last one wins, so we do the same.
    KIND_TO_FILENAME_MAPPER.put(mappingConfig.getKind(), fileNameTypes[fileNameTypes.length- 1]);
  };
  static final Set<String> EXCLUDED_RESOURCE_FILENAME_SUFFIXES = new HashSet<>();
  static {
    EXCLUDED_RESOURCE_FILENAME_SUFFIXES.add(".helm.yaml");
    EXCLUDED_RESOURCE_FILENAME_SUFFIXES.add(".helm.yml");
    KindFilenameMapperUtil.loadMappings()
      .entrySet().stream()
      .map(mc -> MappingConfig.builder()
        .kind(mc.getKey())
        .filenameTypes(String.join(",", mc.getValue()))
        .build())
      .forEach(KIND_FILENAME_MAPPER_UPDATER);
  }

  private KubernetesResourceFragments() {
  }

  /**
   * Upsert the static list of kind-filename mappings.
   * @param mappings the mappings to add.
   */
  public static void updateKindFilenameMappings(List<MappingConfig> mappings) {
    if (mappings != null) {
      mappings.forEach(KIND_FILENAME_MAPPER_UPDATER);
    }
  }

  public static String getNameWithSuffix(String name, String kind) {
    String suffix =  KIND_TO_FILENAME_MAPPER.get(kind);
    return String.format("%s-%s", name.replace(".", "_"), suffix != null ? suffix : "cr");
  }

  /**
   * Read all Kubernetes resource fragments from a directory and create a {@link KubernetesListBuilder} which
   * can be adapted later.
   *
   * @param resourceFiles files to add.
   * @return the list builder
   * @throws IOException in case file is not found
   */
  public static KubernetesListBuilder readResourceFragmentsFrom(File... resourceFiles) throws IOException {
    final KubernetesListBuilder builder = new KubernetesListBuilder();
    if (resourceFiles != null) {
      for (File file : resourceFiles) {
        if (EXCLUDED_RESOURCE_FILENAME_SUFFIXES.stream()
          .noneMatch(s -> file.getName().toLowerCase(Locale.ROOT).endsWith(s))) {
          builder.addToItems(getResource(file));
        }
      }
    }
    return builder;
  }

  /**
   * Read a Kubernetes resource fragment and add meta information extracted from the filename
   * to the resource descriptor. I.e. the following elements are added if not provided in the fragment:
   *
   * <ul>
   *     <li>name - Name of the resource added to metadata</li>
   *     <li>kind - Resource's kind</li>
   *     <li>apiVersion - API version (given as parameter to this method)</li>
   * </ul>
   *
   * @param file file to read.
   * @return HasMetadata object for resource
   * @throws IOException in case file loading is failed
   */
  private static HasMetadata getResource(File file) throws IOException {
    final Map<String, Object> fragment = Optional.ofNullable(Serialization.unmarshal(file, new TypeReference<Map<String, Object>>() {}))
      .orElse(new HashMap<>());
    if (StringUtils.isBlank((String) fragment.get("apiVersion")) ||
      StringUtils.isBlank((String) fragment.get("kind")) ||
      StringUtils.isBlank((String) getOrInitMetadata(fragment).get("name"))) {
      // Fragment is incomplete let's enrich the missing parts
      enrichFragment(fragment, file);
    }
    try {
      return Serialization.convertValue(fragment, HasMetadata.class);
    } catch (ClassCastException exp) {
      throw new IllegalArgumentException(String.format("Resource fragment %s has an invalid syntax (%s)", file.getPath(), exp.getMessage()));
    }
  }

  // Read fragment and add default values
  private static void enrichFragment(Map<String, Object> fragment, File file) {
    final Map<String, Object> metadata = getOrInitMetadata(fragment);

    // Infer values
    final Matcher matcher = FILENAME_PATTERN.matcher(file.getName());
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        String.format("Resource file name '%s' does not match pattern <name>-<type>.(yaml|yml|json)", file.getName()));
    }
    final String nameFromFile = matcher.group("name");
    final String typeFromFile = matcher.group("type"); // nullable

    // Kind
    boolean nameFromFileIsKind = false;
    final MappingConfig mcFromType = typeFromFile == null ? null : FILENAME_TO_MAPPING_CONFIG_MAPPER.get(typeFromFile.toLowerCase());
    final MappingConfig mcFromName = FILENAME_TO_MAPPING_CONFIG_MAPPER.get(nameFromFile.toLowerCase());
    if (fragment.get("kind") == null) {
      if (mcFromType == null && mcFromName == null) {
        throw new IllegalArgumentException(
          "No type given as part of the file name (e.g. 'app-rc.yml') " +
          "and no 'kind' defined in resource descriptor " + file.getName() +
          ".\nMust be one of: " + StringUtils.join(FILENAME_TO_MAPPING_CONFIG_MAPPER.keySet().iterator(), ", "));
      }
      nameFromFileIsKind = mcFromType == null;
      fragment.put("kind", nameFromFileIsKind ? mcFromName.getKind() : mcFromType.getKind());
    }

    // Name
    if (StringUtils.isNotBlank(nameFromFile) && !nameFromFileIsKind) {
      metadata.putIfAbsent("name", nameFromFile);
    }

    // ApiVersion
    final String apiVersion;
    if (mcFromName != null && mcFromName.getApiVersion() != null) {
      apiVersion = mcFromName.getApiVersion();
    } else if (mcFromType != null && mcFromType.getApiVersion() != null) {
      apiVersion = mcFromType.getApiVersion();
    } else {
      apiVersion = DEFAULT_RESOURCE_VERSIONING.getForKind((String)fragment.get("kind"));
    }
    fragment.putIfAbsent("apiVersion", apiVersion);
  }

  private static Map<String, Object> getOrInitMetadata(Map<String, Object> fragment) {
    Object mo = fragment.get("metadata");
    Map<String, Object> meta;
    if (mo == null) {
      meta = new HashMap<>();
      fragment.put("metadata", meta);
      return meta;
    } else if (mo instanceof Map) {
      return (Map<String, Object>) mo;
    } else {
      throw new IllegalArgumentException("Metadata is expected to be a Map, not a " + mo.getClass());
    }
  }
}
