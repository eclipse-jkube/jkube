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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Serialization {

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
    .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
    .configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true));
  private static final KubernetesSerialization KUBERNETES_SERIALIZATION = new KubernetesSerialization(JSON_MAPPER, true);
  static {
    for (ObjectMapper mapper : new ObjectMapper[]{JSON_MAPPER, YAML_MAPPER}) {
      mapper.enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
        .disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    }
    YAML_MAPPER.registerModules(new JavaTimeModule(), KUBERNETES_SERIALIZATION.getUnmatchedFieldTypeModule());
    KUBERNETES_SERIALIZATION.getUnmatchedFieldTypeModule().setRestrictToTemplates(false);
    KUBERNETES_SERIALIZATION.getUnmatchedFieldTypeModule().setLogWarnings(false);
  }

  private Serialization() {}

  public static <T> T unmarshal(String objectAsString) {
    return unmarshal(objectAsString, (Class<T>) KubernetesResource.class);
  }

  public static <T> T unmarshal(URL url) throws IOException {
    return unmarshal(url, (Class<T>) KubernetesResource.class);
  }

  public static <T> T unmarshal(File file) throws IOException {
    return unmarshal(file, (Class<T>) KubernetesResource.class);
  }

  public static <T> T unmarshal(File file, Class<T> clazz) throws IOException {
    return unmarshal(file.toPath(), clazz);
  }

  public static <T> T unmarshal(File file, TypeReference<T> type) throws IOException {
    return unmarshal(file.toPath(), type);
  }

  public static <T> T unmarshal(Path file, Class<T> clazz) throws IOException {
    try (InputStream fis = Files.newInputStream(file)) {
      return unmarshal(fis, clazz);
    }
  }

  public static <T> T unmarshal(Path file, TypeReference<T> type) throws IOException {
    try (InputStream fis = Files.newInputStream(file)) {
      return unmarshal(fis, type);
    }
  }

  public static <T> T unmarshal(URL url, Class<T> clazz) throws IOException {
    try (InputStream is = url.openStream()){
      return unmarshal(is, clazz);
    }
  }

  public static <T> T unmarshal(URL url, TypeReference<T> type) throws IOException {
    try (InputStream is = url.openStream()){
      return unmarshal(is, type);
    }
  }

  public static <T> T unmarshal(InputStream is, Class<T> clazz) {
    return KUBERNETES_SERIALIZATION.unmarshal(is, clazz);
  }

  public static <T> T unmarshal(InputStream is, TypeReference<T> type) {
    return KUBERNETES_SERIALIZATION.unmarshal(is, type);
  }

  public static <T> T unmarshal(String string, Class<T> clazz) {
    return KUBERNETES_SERIALIZATION.unmarshal(string, clazz);
  }

  public static <T> T unmarshal(String string, TypeReference<T> type) {
    return unmarshal(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)), type);
  }

  public static <T> T merge(T original, T overrides) throws IOException {
    final ObjectReader reader = JSON_MAPPER.readerForUpdating(original);
    return reader.readValue(asJson(overrides));
  }

  public static <T> T convertValue(Object object, Class<T> type) {
    return JSON_MAPPER.convertValue(object, type);
  }

  public static String asJson(Object object) {
    return KUBERNETES_SERIALIZATION.asJson(object);
  }

  public static ObjectWriter jsonWriter() {
    return JSON_MAPPER.writer();
  }

  public static String asYaml(Object object) {
    return KUBERNETES_SERIALIZATION.asYaml(object);
  }

  public static void saveJson(File resultFile, Object value) throws IOException {
    JSON_MAPPER.writeValue(resultFile, value);
  }

  public static void saveYaml(File resultFile, Object value) throws IOException {
    YAML_MAPPER.writeValue(resultFile, value);
  }
}
