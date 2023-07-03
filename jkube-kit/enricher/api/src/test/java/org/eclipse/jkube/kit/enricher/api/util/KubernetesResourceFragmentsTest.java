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
package org.eclipse.jkube.kit.enricher.api.util;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceFragments.getNameWithSuffix;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceFragments.readResourceFragmentsFrom;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING;

public class KubernetesResourceFragmentsTest {

  private static File fragmentsDir;

  @BeforeAll
  static void initPath() {
    fragmentsDir = new File(Objects.requireNonNull(KubernetesResourceUtilTest.class.getResource(
      "/kubernetes-resource-fragments/simple-rc.yaml")).getFile()).getParentFile();
  }

  @Nested
  @DisplayName("updateKindFilenameMappings")
  class UpdateKindFilenameMappings {
    @Test
    void whenAddsExistentCronTabMapping_updatesKindToFileNameMapper() {
      // Given
      final MappingConfig mappingConfig = MappingConfig.builder()
        .kind("CronTab")
        .filenameTypes("overridden-crontab")
        .build();
      // When
      KubernetesResourceFragments.updateKindFilenameMappings(Collections.singletonList(mappingConfig));
      // Then
      assertThat(getNameWithSuffix("name", "CronTab")).isEqualTo("name-overridden-crontab");
    }

    @Test
    void withNonexistentMappingConfigs_thenShouldAddMappings(@TempDir Path tempDir) throws IOException {
      // Given
      List<MappingConfig> mappingConfigs = Collections.singletonList(MappingConfig.builder()
        .kind("FooTab")
        .filenameTypes("footab,ft")
        .build());
      final File fooTabYaml = tempDir.resolve("the-foo-tab-footab.yaml").toFile();
      FileUtils.write(fooTabYaml, "metadata:\n  name: the-foo-tab", StandardCharsets.UTF_8);
      // When
      KubernetesResourceFragments.updateKindFilenameMappings(mappingConfigs);
      // Then
      assertThat(getNameWithSuffix("the-foo-tab", "FooTab")).isEqualTo("the-foo-tab-ft");
      assertThat(readResourceFragmentsFrom(DEFAULT_RESOURCE_VERSIONING, "app", fooTabYaml).buildItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("kind", "FooTab");
    }

    @Test
    void withInvalidMappingConfigs_thenThrowException() {
      // Given
      List<MappingConfig> mappingConfigs = Collections.singletonList(MappingConfig.builder()
        .kind("foo")
        .build());
      // When + Then
      assertThatIllegalArgumentException()
        .isThrownBy(() -> KubernetesResourceFragments.updateKindFilenameMappings(mappingConfigs))
        .withMessageContaining("Invalid mapping for Kind foo and Filename Types");
    }
  }

  @Nested
  @DisplayName("getNameWithSuffix")
  class GetNameWithSuffix {

    @Test
    void withKnownMapping_shouldReturnKnownMapping() {
      assertThat(getNameWithSuffix("name", "Pod")).isEqualTo("name-pod");
    }

    @Test
    void withKnownMappingAndNameWithDots_shouldReplaceDotsWithUnderscore() {
      assertThat(getNameWithSuffix("name.with.dots", "Pod")).isEqualTo("name_with_dots-pod");
    }

    @Test
    void withUnknownMapping_shouldReturnCRAsFallback() {
      assertThat(getNameWithSuffix("name", "VeryCustomKind")).isEqualTo("name-cr");
    }
  }

  @Nested
  @DisplayName("readResourceFragmentsFrom")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class ReadResourceFragmentsFrom {

    @Test
    @DisplayName("with valid directory, should read all files in the directory")
    void withValidDirectory_shouldReadAllFragments() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "pong", new File(fragmentsDir, "complete-directory")
          .listFiles());
      // Then
      assertThat(result.build().getItems())
        .hasSize(3)
        .extracting("class", "apiVersion", "kind", "metadata.name")
        .containsExactlyInAnyOrder(
          tuple(Service.class, "v1", "Service", "pong"),
          tuple(ReplicationController.class, "v1", "ReplicationController", "pong"),
          tuple(GenericKubernetesResource.class, "jkube/v1", "CustomKind", "custom")
        );
    }

    @Test
    @DisplayName("with directory containing excluded files, should read only the applicable files")
    void withExcludedFile_shouldNotIncludeExcludedFile(@TempDir Path resourceDir) throws IOException {
      // Given
      final File[] resourceFiles = new File[] {
        Files.write(resourceDir.resolve("Chart.helm.yaml"), "field: value".getBytes()).toFile().getAbsoluteFile(),
        Files.write(resourceDir.resolve("Chart.helm.yml"), "field: value".getBytes()).toFile().getAbsoluteFile(),
        Files.write(resourceDir.resolve("Chart.hElm.yaml"), "field: value".getBytes()).toFile().getAbsoluteFile(),
        Files.write(resourceDir.resolve("other.hElm.yaml"), "field: value".getBytes()).toFile().getAbsoluteFile(),
        Files.write(resourceDir.resolve("configmap.yaml"), "field: value".getBytes()).toFile().getAbsoluteFile(),
        Files.write(resourceDir.resolve("named-cm.yaml"), "field: value".getBytes()).toFile().getAbsoluteFile()
      };
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "pong", resourceFiles);
      // Then
      assertThat(result.buildItems()).hasSize(2)
        .extracting("additionalProperties.field")
        .containsExactly("value", "value");
    }
    @Test
    @DisplayName("with version, kind and name from yaml file, should return valid resource")
    void withYamlFileAndVersionKindNameFromFile_shouldReturnValidResource() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "simple-rc.yaml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(ReplicationController.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "ReplicationController")
        .hasFieldOrPropertyWithValue("metadata.name", "simple");
    }

    @Test
    @DisplayName("with kind, name and version from json file, should return valid resource")
    void withJsonFileAndVersionKindNameFromFile_shouldReturnValidResource() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "simple-rc.json"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(ReplicationController.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "ReplicationController")
        .hasFieldOrPropertyWithValue("metadata.name", "simple");
    }

    @Test
    @DisplayName("with empty file and name and kind in file name, should return empty resource")
    void withEmptyFileAndNameAndKindInFileName_shouldReturnEmptyResource() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "empty-file-svc.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "Service")
        .hasFieldOrPropertyWithValue("metadata.name", "empty-file");
    }

    @Test
    @DisplayName("with invalid file name, should throw exception")
    void invalidFilename() {
      // Given
      final File resource = new File(fragmentsDir, "simple-rc.txt");
      // When & Then
      assertThatIllegalArgumentException()
        .isThrownBy(() -> KubernetesResourceFragments.readResourceFragmentsFrom(DEFAULT_RESOURCE_VERSIONING, "app", resource))
        .withMessage("Resource file name 'simple-rc.txt' does not match pattern <name>-<type>.(yaml|yml|json)");

    }

    @Test
    @DisplayName("with no kind inferrable from file name, should throw exception")
    void withNoKindInferrableFromFileName_shouldThrowException() {
      // Given
      final File resource = new File(fragmentsDir, "contains_no_kind.yml");
      // When & Then
      assertThatIllegalArgumentException()
        .isThrownBy(() -> KubernetesResourceFragments.readResourceFragmentsFrom(DEFAULT_RESOURCE_VERSIONING, "app", resource))
        .withMessageStartingWith("No type given as part of the file name (e.g. 'app-rc.yml') and no 'kind' defined in resource descriptor contains_no_kind.yml");
    }

    @Test
    @DisplayName("with invalid resource content")
    void withInvalidResourceContent() {
      // Given
      final File resource = new File(fragmentsDir, "invalid-metadata-pod.yaml");
      // When & Then
      assertThatIllegalArgumentException()
        .isThrownBy(() -> KubernetesResourceFragments.readResourceFragmentsFrom(DEFAULT_RESOURCE_VERSIONING, "app", resource))
        .withMessageStartingWith("Metadata is expected to be a Map, not a");
    }

    @Test
    @DisplayName("with non-existent file, should throw exception")
    void withNonExistentFile_shouldThrowException() {
      // Given
      final File resource = new File(fragmentsDir, "I-Dont-EXIST.yaml");
      // When & Then
      assertThatIOException()
        .isThrownBy(() -> KubernetesResourceFragments.readResourceFragmentsFrom(DEFAULT_RESOURCE_VERSIONING, "app", resource))
        .withMessageContaining("I-Dont-EXIST.yaml")
        .isInstanceOf(NoSuchFileException.class);
    }

    @Test
    @DisplayName("with name in field and file name, should be named from field")
    void withNameInFieldAndFilename_shouldBeNamedFromField() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "named-svc.yaml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "Service")
        .hasFieldOrPropertyWithValue("metadata.name", "pong");
    }

    @Test
    @DisplayName("with name in field, should be named from field")
    void withNameInField_shouldBeamedFromField() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "rc.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(ReplicationController.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "ReplicationController")
        .hasFieldOrPropertyWithValue("metadata.name", "flipper");
    }

    @Test
    @DisplayName("with no name both in value and file, should be named from default name")
    void withNoNameInFieldAndNoNameInFilename_shouldBeNamedFromDefaultName() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "default-app", new File(fragmentsDir, "svc.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "Service")
        .hasFieldOrPropertyWithValue("metadata.name", "default-app");
    }

    @Test
    @DisplayName("with kind inf field not in file, should get the kind from value")
    void withKindInFieldAndNotInFilename_shouldGetKindFromValue() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "contains_kind.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(ReplicationController.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "ReplicationController");
    }

    @Test
    @DisplayName("with kind in filename, should get the kind from filename")
    void withKindFromFilename_shouldGetKindFromFilename() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "job.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(Job.class)
        .hasFieldOrPropertyWithValue("apiVersion", "batch/v1")
        .hasFieldOrPropertyWithValue("kind", "Job")
        .hasFieldOrPropertyWithValue("metadata.name", "app");
    }

    @Test
    @DisplayName("with kind in field and in filename, should get the kind from field")
    void withKindInFieldAndInFilename_shouldGetKindFromField() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "override-kind-pod.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(GenericKubernetesResource.class)
        .hasFieldOrPropertyWithValue("apiVersion", "custom.jkube.eclipse.org/v1")
        .hasFieldOrPropertyWithValue("kind", "NotAPod");
    }

    @Test
    @DisplayName("with kind in field and in filename with dashes, should get the kind from value")
    void withValueInKindAndFilenameWithDashes_shouldGetKindFromValue() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "file-name-with-dashes-kind-in-field.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(Pod.class)
        .hasFieldOrPropertyWithValue("apiVersion", "v1")
        .hasFieldOrPropertyWithValue("kind", "Pod");
    }

    @Test
    @DisplayName("with network ingress, should load network v1 ingress")
    void withNetworkV1Ingress_shouldLoadNetworkV1Ingress() throws IOException {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "network-v1-ingress.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(Ingress.class)
        .hasFieldOrPropertyWithValue("apiVersion", "networking.k8s.io/v1")
        .hasFieldOrPropertyWithValue("kind", "Ingress")
        .hasFieldOrPropertyWithValue("metadata.name", "my-ingress");
    }

    @Test
    @DisplayName("with network policy v1, should load V1 network policy")
    void withNetworkPolicyV1_shouldLoadV1NetworkPolicy() throws Exception {
      // When
      final KubernetesListBuilder result = KubernetesResourceFragments.readResourceFragmentsFrom(
        DEFAULT_RESOURCE_VERSIONING, "app", new File(fragmentsDir, "networking-v1-np.yml"));
      // Then
      assertThat(result.buildItems())
        .singleElement()
        .isInstanceOf(NetworkPolicy.class)
        .hasFieldOrPropertyWithValue("kind", "NetworkPolicy")
        .hasFieldOrPropertyWithValue("spec.podSelector.matchLabels.role", "db");
    }
  }
}
