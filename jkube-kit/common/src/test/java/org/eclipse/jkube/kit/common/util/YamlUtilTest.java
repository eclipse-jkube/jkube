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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlString;
import static org.eclipse.jkube.kit.common.util.YamlUtil.splitYamlResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlUtilTest {

  @Test
  void getPropertiesFromYamlStringEmptyStringTest() throws Exception {
    // Given
    final String yamlString = "";
    // When
    final Properties result = getPropertiesFromYamlString(yamlString);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getPropertiesFromYamlStringNullStringTest() throws Exception {
    // When
    final Properties result = getPropertiesFromYamlString(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getPropertiesFromYamlStringInvalidStringTest() {
    assertThrows(JsonMappingException.class, () -> getPropertiesFromYamlString("not\na\nvalid\nyaml"));
  }

  @Test
  void getPropertiesFromYamlStringValidStringTest() throws Exception {
    // Given
    final String yamlString = "---\ntest: 1\nlist:\n  - name: item 1\n    value: value 1\nstill-test: 1";
    // When
    final Properties result = getPropertiesFromYamlString(yamlString);
    // Then
    assertThat(result).isNotNull().hasSize(4)
            .containsOnly(
                    entry("test", "1"),
                    entry("list[0].name", "item 1"),
                    entry("list[0].value", "value 1"),
                    entry("still-test", "1")
            );
  }

  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=561261
  @Test
  void getPropertiesFromYamlCWE502Test() throws Exception {
    // Given
    final String yamlString = "maps: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\\\"http://localhost:9000/\\\"]]]]";
    // When
    final Properties result = getPropertiesFromYamlString(yamlString);
    // Then
    assertThat(result).isNotNull()
            .hasSize(1)
            .containsOnly(entry("maps[0][0][0][0]", "\\\"http://localhost:9000/\\\""));
  }

  @Test
  void splitYamlResourceTest() throws Exception {
    // Given
    final URL resource = YamlUtilTest.class.getResource("/util/yaml-list.yml");
    // When
    final List<String> result = splitYamlResource(resource);
    // Then
    assertThat(result).isNotNull().hasSize(4);
    assertThat(result.get(1)).contains("name: \"YAML --- 2\"");
    assertThat(result.get(3)).startsWith("---\nname: \"Edge case --- 1");
  }

  @ParameterizedTest
  @ValueSource(strings = {"file.yaml", "file.yml", "file.YAML", "file.YML", "file.YaML", "file.YmL", ".yml"})
  void isYamlWithYamlExtensionReturnsTrue(String fileName) {
    assertThat(YamlUtil.isYaml(new File(fileName))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"file.yeaml", "file.txt", "file.JML",})
  void isYamlWithNonYamlExtensionReturnsFalse(String fileName) {
    assertThat(YamlUtil.isYaml(new File(fileName))).isFalse();
  }

  @Test
  void listYamls(@TempDir Path directory) throws IOException {
    // Given
    final Path file1 = Files.createFile(directory.resolve("file1.yaml"));
    final Path file2 = Files.createFile(directory.resolve("file2.json"));
    final Path file3 = Files.createFile(directory.resolve("file3.YML"));
    // When
    final List<File> result = YamlUtil.listYamls(directory.toFile());
    // Then
    assertThat(result).containsExactlyInAnyOrder(
      file1.toFile(),
      file3.toFile()
    );
  }

  @Test
  void listYamlsWithNullDirectory() {
    assertThat(YamlUtil.listYamls(null)).isEmpty();
  }

  @Test
  void mergeYaml_withNonOverlappingProperties() throws IOException {
    // Given
    String existingYaml = "metadata:\n" +
        "  annotations:\n" +
        "    key1: value1\n" +
        "spec:\n" +
        "  field1: value1\n";
    String newYaml = "metadata:\n" +
        "  namespace: demo\n" +
        "spec:\n" +
        "  field2: value2\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then
    assertThat(result)
        .contains("key1: \"value1\"")
        .contains("namespace: \"demo\"")
        .contains("field1: \"value1\"")
        .contains("field2: \"value2\"");
  }

  @Test
  void mergeYaml_withOverlappingProperties() throws IOException {
    // Given
    String existingYaml = "spec:\n" +
        "  tls:\n" +
        "    termination: edge\n" +
        "    insecureEdgeTerminationPolicy: Redirect\n";
    String newYaml = "spec:\n" +
        "  tls:\n" +
        "    termination: dev-overridden\n" +
        "    insecureEdgeTerminationPolicy: Allow\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then - new values should overwrite existing ones
    assertThat(result)
        .contains("termination: \"dev-overridden\"")
        .contains("insecureEdgeTerminationPolicy: \"Allow\"")
        .doesNotContain("edge")
        .doesNotContain("Redirect");
  }

  @Test
  void mergeYaml_withPartialOverlap() throws IOException {
    // Given
    String existingYaml = "metadata:\n" +
        "  annotations:\n" +
        "    cert-manager.io/issuer-kind: ClusterIssuer\n" +
        "    cert-manager.io/issuer-name: letsencrypt-prod\n" +
        "    haproxy.router.openshift.io/timeout: 120s\n" +
        "spec:\n" +
        "  tls:\n" +
        "    termination: edge\n" +
        "    insecureEdgeTerminationPolicy: Redirect\n";
    String newYaml = "metadata:\n" +
        "  namespace: demo-namespace\n" +
        "spec:\n" +
        "  host: demoapp.apps.example.com\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then - should merge without duplicates
    assertThat(result)
        .contains("cert-manager.io/issuer-kind: \"ClusterIssuer\"")
        .contains("cert-manager.io/issuer-name: \"letsencrypt-prod\"")
        .contains("haproxy.router.openshift.io/timeout: \"120s\"")
        .contains("namespace: \"demo-namespace\"")
        .contains("termination: \"edge\"")
        .contains("insecureEdgeTerminationPolicy: \"Redirect\"")
        .contains("host: \"demoapp.apps.example.com\"");
    // Verify no duplicate keys by checking the structure
    assertThat(getPropertiesFromYamlString(result))
        .containsEntry("metadata.annotations.cert-manager.io/issuer-kind", "ClusterIssuer")
        .containsEntry("metadata.annotations.cert-manager.io/issuer-name", "letsencrypt-prod")
        .containsEntry("metadata.annotations.haproxy.router.openshift.io/timeout", "120s")
        .containsEntry("metadata.namespace", "demo-namespace")
        .containsEntry("spec.tls.termination", "edge")
        .containsEntry("spec.tls.insecureEdgeTerminationPolicy", "Redirect")
        .containsEntry("spec.host", "demoapp.apps.example.com");
  }

  @Test
  void mergeYaml_withDifferentListItems_shouldMerge() throws IOException {
    // Given
    String existingYaml = "spec:\n" +
        "  template:\n" +
        "    spec:\n" +
        "      containers:\n" +
        "        - name: container1\n" +
        "          image: image1\n";
    String newYaml = "spec:\n" +
        "  template:\n" +
        "    spec:\n" +
        "      containers:\n" +
        "        - name: container2\n" +
        "          image: image2\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);

    // Then - should have both containers
    assertThat(result)
        .contains("container1")
        .contains("image1")
        .contains("container2")
        .contains("image2");

    // Verify both containers exist
    Properties props = getPropertiesFromYamlString(result);
    assertThat(props)
        .containsEntry("spec.template.spec.containers[0].name", "container1")
        .containsEntry("spec.template.spec.containers[0].image", "image1")
        .containsEntry("spec.template.spec.containers[1].name", "container2")
        .containsEntry("spec.template.spec.containers[1].image", "image2");
  }

  @Test
  void mergeYaml_withEmptyExistingYaml_shouldReturnNewYaml() throws IOException {
    // Given
    String existingYaml = "---\n";
    String newYaml = "metadata:\n" +
        "  name: test\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then
    assertThat(result).isEqualTo(newYaml);
  }

  @Test
  void mergeYaml_withCommentsOnlyExistingYaml_shouldReturnNewYaml() throws IOException {
    // Given - simulating fragments-custom-mapping/first-fl.yaml which has only comments
    String existingYaml = "#\n" +
        "# Copyright (c) 2019 Red Hat, Inc.\n" +
        "# This program and the accompanying materials are made\n" +
        "# available under the terms of the Eclipse Public License 2.0\n" +
        "#\n" +
        "\n";
    String newYaml = "apiVersion: v1\n" +
        "kind: Service\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then
    assertThat(result).isEqualTo(newYaml);
  }

  @Test
  void mergeYaml_withNullExistingYaml_shouldReturnNewYaml() throws IOException {
    // Given
    String newYaml = "metadata:\n" +
        "  name: test\n";
    // When
    String result = YamlUtil.mergeYaml(null, newYaml);
    // Then
    assertThat(result).isEqualTo(newYaml);
  }

  @Test
  void mergeYaml_withEmptyNewYaml_shouldReturnExistingYaml() throws IOException {
    // Given
    String existingYaml = "metadata:\n" +
        "  name: test\n";
    String newYaml = "---\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then
    assertThat(result).isEqualTo(existingYaml);
  }

  @Test
  void mergeYaml_withBothEmpty_shouldReturnEmpty() throws IOException {
    // Given
    String existingYaml = "---\n";
    String newYaml = "---\n";
    // When
    String result = YamlUtil.mergeYaml(existingYaml, newYaml);
    // Then
    assertThat(result).isEqualTo(newYaml);
  }

  @Test
  void mergeYaml_withInvalidYaml_shouldThrowIOException() {
    // Given
    String existingYaml = "valid:\n  yaml: true\n";
    String invalidYaml = "invalid: yaml: content: [[[";

    // When & Then
    assertThatThrownBy(() -> YamlUtil.mergeYaml(existingYaml, invalidYaml))
      .isInstanceOf(IOException.class)
      .hasMessageContaining("Failed to parse and merge YAML content");
  }

  @Test
  void mergeYaml_withEmptyString_shouldReturnOther() throws IOException {
    // Given
    String emptyYaml = "";
    String validYaml = "metadata:\n  name: test\n";

    // When
    String result1 = YamlUtil.mergeYaml(emptyYaml, validYaml);
    String result2 = YamlUtil.mergeYaml(validYaml, emptyYaml);

    // Then
    assertThat(result1).isEqualTo(validYaml);
    assertThat(result2).isEqualTo(validYaml);
  }

  @Test
  void mergeYaml_withWhitespaceOnly_shouldReturnOther() throws IOException {
    // Given
    String whitespaceYaml = "   \n  \n";
    String validYaml = "metadata:\n  name: test\n";

    // When
    String result1 = YamlUtil.mergeYaml(whitespaceYaml, validYaml);
    String result2 = YamlUtil.mergeYaml(validYaml, whitespaceYaml);

    // Then
    assertThat(result1).isEqualTo(validYaml);
    assertThat(result2).isEqualTo(validYaml);
  }

  @Test
  void mergeYaml_withEmptyObject_shouldReturnOther() throws IOException {
    // Given
    String emptyObjectYaml = "{}";
    String validYaml = "metadata:\n  name: test\n";

    // When
    String result1 = YamlUtil.mergeYaml(emptyObjectYaml, validYaml);

    // Then - result should contain the valid yaml properties
    assertThat(getPropertiesFromYamlString(result1))
      .containsEntry("metadata.name", "test");
  }

  @Test
  void getPropertiesFromYamlResourceWithActiveProfile(@TempDir Path tempDir) throws IOException {
    // Given
    final URL resource = YamlUtilTest.class.getResource("/util/yaml-list.yml");

    // When - get properties from resource with a specific profile pattern
    final Properties result = YamlUtil.getPropertiesFromYamlResource("YAML --- 2", resource);

    // Then - should get the matching YAML document
    assertThat(result).isNotNull();
    assertThat(result.getProperty("name")).isEqualTo("YAML --- 2");
  }

  @Test
  void getPropertiesFromYamlResourceWithNoMatchingProfile() throws IOException {
    // Given
    final URL resource = YamlUtilTest.class.getResource("/util/yaml-list.yml");

    // When - get properties with a pattern that doesn't match any document
    final Properties result = YamlUtil.getPropertiesFromYamlResource("non-existent", resource);

    // Then - should return the first document as fallback
    assertThat(result).isNotNull();
    // First document in yaml-list.yml has name "YAML 1"
    assertThat(result.getProperty("name")).isEqualTo("YAML 1");
  }

  @Test
  void getPropertiesFromYamlResourceWithNullResource() {
    // When
    final Properties result = YamlUtil.getPropertiesFromYamlResource(null, null);

    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void getPropertiesFromYamlResourceWithNullPattern() throws IOException {
    // Given
    final URL resource = YamlUtilTest.class.getResource("/util/yaml-list.yml");

    // When - get properties with null pattern
    final Properties result = YamlUtil.getPropertiesFromYamlResource(null, resource);

    // Then - should return the first document
    assertThat(result).isNotNull();
    assertThat(result.getProperty("name")).isEqualTo("YAML 1");
  }
}
