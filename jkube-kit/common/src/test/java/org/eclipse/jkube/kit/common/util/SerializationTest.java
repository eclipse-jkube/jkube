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
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Template;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("DataFlowIssue")
class SerializationTest {

  @Test
  void unmarshal_withString_shouldReturnConfigMap() {
    // When
    final HasMetadata result = Serialization.unmarshal(
      "{\"apiVersion\":\"v1\",\"kind\":\"ConfigMap\",\"metadata\":{\"name\":\"my-config-map\"}}");
    // Then
    assertThat(result)
      .isInstanceOf(ConfigMap.class)
      .hasFieldOrPropertyWithValue("metadata.name", "my-config-map");
  }

  @Test
  void unmarshal_withCustomResourceUrl_shouldLoadGenericKubernetesResource() throws Exception {
    // When
    final HasMetadata result = Serialization.unmarshal(
      ResourceUtilTest.class.getResource( "/util/resource-util/custom-resource-cr.yml"));
    // Then
    assertThat(result)
      .isInstanceOf(GenericKubernetesResource.class)
      .hasFieldOrPropertyWithValue("kind", "SomeCustomResource")
      .hasFieldOrPropertyWithValue("metadata.name", "my-custom-resource");
  }

  @Test
  void unmarshal_withCustomResourceFile_shouldLoadGenericKubernetesResource() throws Exception {
    // When
    final HasMetadata result = Serialization.unmarshal(
      new File(ResourceUtilTest.class.getResource( "/util/resource-util/custom-resource-cr.yml").getFile()));
    // Then
    assertThat(result)
      .isInstanceOf(GenericKubernetesResource.class)
      .hasFieldOrPropertyWithValue("kind", "SomeCustomResource")
      .hasFieldOrPropertyWithValue("metadata.name", "my-custom-resource");
  }

  @Test
  void unmarshal_withTemplateUrl_shouldLoadTemplate() throws Exception {
    // When
    final HasMetadata result = Serialization.unmarshal(
      ResourceUtilTest.class.getResource( "/util/resource-util/template.yml"));
    // Then
    assertThat(result)
      .isInstanceOf(Template.class)
      .hasFieldOrPropertyWithValue("metadata.name", "template-example")
      .extracting("objects").asInstanceOf(InstanceOfAssertFactories.list(Pod.class)).singleElement()
      .hasFieldOrPropertyWithValue("metadata.name", "pod-from-template")
      .extracting("spec.containers").asInstanceOf(InstanceOfAssertFactories.list(Container.class)).singleElement()
      .hasFieldOrPropertyWithValue("image", "busybox")
      .hasFieldOrPropertyWithValue("securityContext.additionalProperties.privileged", "${POD_SECURITY_CONTEXT}")
      .extracting("env").asInstanceOf(InstanceOfAssertFactories.list(EnvVar.class)).singleElement()
      .hasFieldOrPropertyWithValue("value", "${ENV_VAR_KEY}");
  }

  @Test
  void unmarshal_withTemplateFile_shouldLoadTemplate() throws Exception {
    // When
    final HasMetadata result = Serialization.unmarshal(
      new File(ResourceUtilTest.class.getResource( "/util/resource-util/template.yml").getFile()));
    // Then
    assertThat(result)
      .isInstanceOf(Template.class)
      .hasFieldOrPropertyWithValue("metadata.name", "template-example")
      .extracting("objects").asInstanceOf(InstanceOfAssertFactories.list(Pod.class)).singleElement()
      .hasFieldOrPropertyWithValue("metadata.name", "pod-from-template")
      .extracting("spec.containers").asInstanceOf(InstanceOfAssertFactories.list(Container.class)).singleElement()
      .hasFieldOrPropertyWithValue("image", "busybox")
      .hasFieldOrPropertyWithValue("securityContext.additionalProperties.privileged", "${POD_SECURITY_CONTEXT}")
      .extracting("env").asInstanceOf(InstanceOfAssertFactories.list(EnvVar.class)).singleElement()
      .hasFieldOrPropertyWithValue("value", "${ENV_VAR_KEY}");
  }

  @Test
  void unmarshal_withTemplateUrlAndGenericTypeReference_shouldGenericResource() throws Exception {
    // When
    final HasMetadata result = Serialization.unmarshal(
      ResourceUtilTest.class.getResource("/util/resource-util/template.yml"),
      new TypeReference<GenericKubernetesResource>() {});
    // Then
    assertThat(result)
      .isInstanceOf(GenericKubernetesResource.class)
      .hasFieldOrPropertyWithValue("metadata.name", "template-example");
  }

  @Test
  void unmarshal_withTemplateFileAndGenericTypeReference_shouldGenericResource() throws Exception {
    // When
    final HasMetadata result = Serialization.unmarshal(
      new File(ResourceUtilTest.class.getResource( "/util/resource-util/template.yml").getFile()),
      new TypeReference<GenericKubernetesResource>() {});
    // Then
    assertThat(result)
      .isInstanceOf(GenericKubernetesResource.class)
      .hasFieldOrPropertyWithValue("metadata.name", "template-example");
  }

  @Test
  void convertValue_withMapToConfigMap_returnsConfigMap() {
    // Given
    final Map<String, Object> source = new HashMap<>();
    source.put("metadata", Collections.singletonMap("name", "test"));
    source.put("data", Collections.singletonMap("key", "value"));
    // When
    final ConfigMap result = Serialization.convertValue(source, ConfigMap.class);
    // Then
    assertThat(result)
      .hasFieldOrPropertyWithValue("metadata.name", "test")
      .hasFieldOrPropertyWithValue("data.key", "value");
  }

  @Test
  void asJson_withConfigMap_returnsJsonString() {
    // Given
    final ConfigMap source = new ConfigMapBuilder()
      .withNewMetadata().withName("test").endMetadata()
      .addToData("key", "value")
      .build();
    // When
    final String result = Serialization.asJson(source);
    // Then
    assertThat(result)
      .isEqualTo(String.format("{%n" +
        "  \"apiVersion\" : \"v1\",%n" +
        "  \"kind\" : \"ConfigMap\",%n" +
        "  \"metadata\" : {%n" +
        "    \"name\" : \"test\"%n" +
        "  },%n" +
        "  \"data\" : {%n" +
        "    \"key\" : \"value\"%n" +
        "  }%n" +
        "}"));
  }

  @Test
  void saveJson_withConfigMap_savesFile(@TempDir Path targetDir) throws IOException {
    // Given
    final File targetFile = targetDir.resolve("cm.json").toFile();
    final ConfigMap source = new ConfigMapBuilder()
      .withNewMetadata().withName("test").endMetadata()
      .addToData("key", "value")
      .build();
    // When
    Serialization.saveJson(targetFile, source);
    // Then
    assertThat(targetFile)
      .content()
      .isEqualTo(String.format("{%n" +
        "  \"apiVersion\" : \"v1\",%n" +
        "  \"kind\" : \"ConfigMap\",%n" +
        "  \"metadata\" : {%n" +
        "    \"name\" : \"test\"%n" +
        "  },%n" +
        "  \"data\" : {%n" +
        "    \"key\" : \"value\"%n" +
        "  }%n" +
        "}"));
  }

  @Test
  void saveYaml_withConfigMap_savesFile(@TempDir Path targetDir) throws IOException {
    // Given
    final File targetFile = targetDir.resolve("cm.yaml").toFile();
    final ConfigMap source = new ConfigMapBuilder()
      .withNewMetadata().withName("test").endMetadata()
      .addToData("key", "value")
      .build();
    // When
    Serialization.saveYaml(targetFile, source);
    // Then
    assertThat(targetFile)
      .content()
      .isEqualTo(String.format("---%n" +
        "apiVersion: v1%n" +
        "kind: ConfigMap%n" +
        "metadata:%n" +
        "  name: test%n" +
        "data:%n" +
        "  key: value%n"));
  }

  @Nested
  @DisplayName("Multi-line strings are serialized to YAML using scalar blocks")
  class MultiLineStringsSerializedToScalarYamlBlocks {
    @Test
    @DisplayName("string ends with newline, then add | to use block style in serialized object")
    void unmarshal_whenStringEndingWithNewline_thenAddBlockIndicatorInSerializedObject(@TempDir Path targetDir) throws IOException {
      // Given
      final File targetFile = targetDir.resolve("cm.yaml").toFile();
      final ConfigMap source = new ConfigMapBuilder()
              .withNewMetadata()
              .addToAnnotations("proxy.istio.io/config", "proxyMetadata:\n    ISTIO_META_DNS_CAPTURE: \"false\"\nholdApplicationUntilProxyStarts: true\n")
              .endMetadata()
              .build();
      // When
      Serialization.saveYaml(targetFile, source);
      // Then
      assertThat(targetFile)
          .content()
          .isEqualTo(String.format("---%n" +
              "apiVersion: v1%n" +
              "kind: ConfigMap%n" +
              "metadata:%n" +
              "  annotations:%n" +
              "    proxy.istio.io/config: |%n" +
              "      proxyMetadata:%n" +
              "          ISTIO_META_DNS_CAPTURE: \"false\"%n"+
              "      holdApplicationUntilProxyStarts: true%n"));
    }

    @Test
    @DisplayName("when string contains windows line breaks, then convert then to unix line breaks during deserialization")
    void unmarshal_withWindowsLineEndings_shouldDeserializeMultilineStringWithLineFeeds() {
      // Given
      String input = "apiVersion: v1\r\n" +
          "kind: ConfigMap\r\n" +
          "metadata:\r\n" +
          "  annotations:\r\n" +
          "    proxy.istio.io/config: |\r\n" +
          "      proxyMetadata:\r\n" +
          "        ISTIO_META_DNS_CAPTURE: \"false\"\r\n"+
          "      holdApplicationUntilProxyStarts: true\r\n";

      // When
      ConfigMap configMap = Serialization.unmarshal(input, ConfigMap.class);

      // Then
      assertThat(configMap.getMetadata().getAnnotations())
              .containsEntry("proxy.istio.io/config", "proxyMetadata:\n" +
                      "  ISTIO_META_DNS_CAPTURE: \"false\"\n" +
              "holdApplicationUntilProxyStarts: true\n");
    }

    @Test
    @DisplayName("when string contains unix line breaks, then line breaks remain unchanged during deserialization")
    void unmarshal_withUnixLineEndings_shouldDeserializeMultilineStringWithLineFeeds() {
      // Given
      String input = "apiVersion: v1\n" +
          "kind: ConfigMap\n" +
          "metadata:\n" +
          "  annotations:\n" +
          "    proxy.istio.io/config: |\n" +
          "      proxyMetadata:\n" +
          "        ISTIO_META_DNS_CAPTURE: \"false\"\n"+
          "      holdApplicationUntilProxyStarts: true\n";
      // When
      ConfigMap configMap = Serialization.unmarshal(input, ConfigMap.class);

      // Then
      assertThat(configMap.getMetadata().getAnnotations())
              .containsEntry("proxy.istio.io/config", "proxyMetadata:\n" +
                      "  ISTIO_META_DNS_CAPTURE: \"false\"\n" +
                      "holdApplicationUntilProxyStarts: true\n");
    }
  }
}
