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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.kit.config.resource.PlatformMode.kubernetes;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.FILENAME_TO_KIND_MAPPER;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.KIND_TO_FILENAME_MAPPER;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.addNewConfigMapEntriesToExistingConfigMap;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.createConfigMapEntry;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.getNameWithSuffix;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.initializeKindFilenameMapper;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class KubernetesResourceUtilTest {

  private static File fragmentsDir;

  @BeforeAll
  static void initPath() {
    fragmentsDir = new File(Objects.requireNonNull(KubernetesResourceUtilTest.class.getResource(
        "/kubernetes-resource-util/simple-rc.yaml")).getFile()).getParentFile();
  }

  @BeforeEach
  void setUp() {
    FILENAME_TO_KIND_MAPPER.clear();
    KIND_TO_FILENAME_MAPPER.clear();
    initializeKindFilenameMapper();
  }

  @Nested
  @DisplayName("get resource")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class GetResource {
    @Test
    @DisplayName("with version, kind and name from yaml file, should return valid resource")
    void withYamlFileAndVersionKindNameFromFile_shouldReturnValidResource() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "simple-rc.yaml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(ReplicationController.class)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "ReplicationController")
          .hasFieldOrPropertyWithValue("metadata.name", "simple");
    }

    @Test
    @DisplayName("with kind, name and version from json file, should return valid resource")
    void withJsonFileAndVersionKindNameFromFile_shouldReturnValidResource() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "simple-rc.json"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(ReplicationController.class)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "ReplicationController")
          .hasFieldOrPropertyWithValue("metadata.name", "simple");
    }

    @Test
    @DisplayName("with value in name and file, should be named from value")
    void withValueInNameAndFile_shouldBeNamedNamedFromValue() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "named-svc.yaml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(Service.class)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "Service")
          .hasFieldOrPropertyWithValue("metadata.name", "pong");
    }

    @Test
    @DisplayName("with value in name, should be named from value")
    void withValueInName_shouldBeNamedNamedFromValue() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "rc.yml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(ReplicationController.class)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "ReplicationController")
          .hasFieldOrPropertyWithValue("metadata.name", "flipper");
    }

    @Test
    @DisplayName("with no name both in value and file, should be named from app name")
    void withNoNameInValueAndNoNameInFileName_shouldBeNamedFromAppName() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "svc.yml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(Service.class)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "Service")
          .hasFieldOrPropertyWithValue("metadata.name", "app");
    }

    @DisplayName("with invalid resource")
    @ParameterizedTest(name = "{index}: ''{0}'', should throw exception")
    @MethodSource("invalidResources")
    void withInvalidResource(String resourceType, String resourceFile, String message) {
      // Given
      final File resource = new File(fragmentsDir, resourceFile);
      // When & Then
      assertThatIllegalArgumentException()
          .isThrownBy(() -> KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING, resource, "app"))
          .withMessageContaining(message);
    }

    Stream<Arguments> invalidResources() {
      return Stream.of(
          arguments("invalid extension file", "simple-rc.txt",
              "Resource file name 'simple-rc.txt' does not match pattern <name>-<type>.(yaml|yml|json)"),
          arguments("invalid type", "simple-bla.yaml", "Unknown type 'bla' for file simple-bla.yaml"),
          arguments("no type and no kind", "contains_no_kind.yml",
              "No type given as part of the file name (e.g. 'app-rc.yml') and no 'Kind' defined in resource descriptor contains_no_kind.yml"));
    }

    @Test
    @DisplayName("with non-existent file, should throw exception")
    void withNonExistentFile_shouldThrowException() {
      // Given
      final File resource = new File(fragmentsDir, "I-Dont-EXIST.yaml");
      // When & Then
      assertThatIOException()
          .isThrownBy(() -> KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING, resource, "app"))
          .withMessageContaining("I-Dont-EXIST.yaml")
          .withMessageContaining("No such file or directory")
          .withCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("with value in kind not in file, should get the kind from value")
    void withValueInKindAndNotInFilename_shouldGetKindFromValue() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "contains_kind.yml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(ReplicationController.class)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("kind", "ReplicationController");
    }

    @Test
    @DisplayName("with kind from file, should get the kind from file")
    void withKindFromFilename_shouldGetKindFromFilename() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "job.yml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(Job.class)
          .hasFieldOrPropertyWithValue("apiVersion", "batch/v1")
          .hasFieldOrPropertyWithValue("kind", "Job")
          .hasFieldOrPropertyWithValue("metadata.name", "app");
    }

    @Test
    @DisplayName("with network ingress, should load network v1 ingress")
    void withNetworkV1Ingress_shouldLoadNetworkV1Ingress() throws IOException {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "network-v1-ingress.yml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(Ingress.class)
          .hasFieldOrPropertyWithValue("apiVersion", "networking.k8s.io/v1")
          .hasFieldOrPropertyWithValue("kind", "Ingress")
          .hasFieldOrPropertyWithValue("metadata.name", "my-ingress");
    }

    @Test
    @DisplayName("with network policy v1, should load V1 network policy")
    void withNetworkPolicyV1_shouldLoadV1NetworkPolicy() throws Exception {
      // When
      final HasMetadata result = KubernetesResourceUtil.getResource(kubernetes, DEFAULT_RESOURCE_VERSIONING,
          new File(fragmentsDir, "networking-v1-np.yml"), "app");
      // Then
      assertThat(result)
          .isInstanceOf(NetworkPolicy.class)
          .hasFieldOrPropertyWithValue("kind", "NetworkPolicy")
          .hasFieldOrPropertyWithValue("spec.podSelector.matchLabels.role", "db");
    }
  }

  @Test
  void extractContainerName() {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("dummy-image")
        .registry("example.com/someregistry")
        .name("test")
        .build();
    // When
    final String result = KubernetesResourceUtil.extractContainerName(
        new GroupArtifactVersion("io.fabric8-test-", "fabric8-maven-plugin-dummy", "0"),
        imageConfiguration);
    // Then
    assertThat(result).isEqualTo("iofabric8-test--fabric8-maven-plugin-dummy");
  }

  @Test
  void extractContainerName_withPeriodsInImageUser_shouldRemovePeriodsFromContainerName() {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("org.eclipse.jkube.testing/test-image")
        .build();
    final GroupArtifactVersion gav = new GroupArtifactVersion("org.eclipse.jkube.testing",
        "test-image", "1.0.0");
    // When
    final String result = KubernetesResourceUtil.extractContainerName(gav, imageConfiguration);
    // Then
    assertThat(result).isEqualTo("orgeclipsejkubetesting-test-image");
  }

  @Test
  void readResourceFragmentsFrom_withValidDirectory_shouldReadAllFragments() throws IOException {
    // When
    final KubernetesListBuilder result = KubernetesResourceUtil.readResourceFragmentsFrom(
        kubernetes, DEFAULT_RESOURCE_VERSIONING, "pong", new File(fragmentsDir, "complete-directory")
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
  void mergePodSpec_withFragmentWithContainerNameAndSidecarDisabled_shouldPreserveContainerNameFromFragment() {
    // Given
    final PodSpecBuilder fragment = new PodSpecBuilder()
        .addNewContainer()
        .withArgs("/usr/local/s2i/run")
        .withName("demo")
        .addNewEnv()
        .withName("JAVA_APP_DIR")
        .withValue("/deployments/ROOT.war ")
        .endEnv()
        .endContainer();
    final PodSpec defaultPodSpec = defaultPodSpec();
    // When
    final String result = KubernetesResourceUtil.mergePodSpec(
        fragment, defaultPodSpec, "default-name", false);
    // Then
    assertThat(result).isEqualTo("demo");
    assertThat(fragment.build().getContainers())
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "demo")
        .hasFieldOrPropertyWithValue("image", "spring-boot-test:latest")
        .hasFieldOrPropertyWithValue("args", Collections.singletonList("/usr/local/s2i/run"))
        .extracting("ports").asList().extracting("containerPort")
        .containsExactly(8080, 9779, 8778);
  }

  @Test
  void mergePodSpec_withFragmentWithNoContainerNameAndSidecarDisabled_shouldGetContainerNameFromDefault() {
    // Given
    final PodSpecBuilder fragment = new PodSpecBuilder()
        .addNewContainer()
        .withNewResources()
        .addToRequests("cpu", new Quantity("0.2"))
        .addToRequests("memory", new Quantity("256Mi"))
        .addToLimits("cpu", new Quantity("1.0"))
        .addToLimits("memory", new Quantity("512Mi"))
        .endResources()
        .addNewEnv()
        .withName("SPRING_APPLICATION_JSON")
        .withValue("{\"server\":{\"undertow\":{\"io-threads\":1, \"worker-threads\":2 }}}")
        .endEnv()
        .endContainer();
    final PodSpec defaultPodSpec = defaultPodSpec();
    // When
    final String result = KubernetesResourceUtil.mergePodSpec(
        fragment, defaultPodSpec, "default-name", false);
    // Then
    assertThat(result).isEqualTo("spring-boot");
    assertThat(fragment.build().getContainers())
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "spring-boot")
        .hasFieldOrPropertyWithValue("image", "spring-boot-test:latest")
        .hasFieldOrPropertyWithValue("resources.requests.cpu.amount", "0.2")
        .hasFieldOrPropertyWithValue("resources.requests.memory.amount", "256")
        .hasFieldOrPropertyWithValue("resources.limits.cpu.amount", "1.0")
        .hasFieldOrPropertyWithValue("resources.limits.memory.amount", "512")
        .extracting("ports").asList().extracting("containerPort")
        .containsExactly(8080, 9779, 8778);
  }

  @Test
  void mergePodSpec_withFragmentWithContainerNameAndSidecarEnabled_shouldGetContainerNameFromDefault() {
    // Given
    final PodSpecBuilder fragment = new PodSpecBuilder()
        .addNewContainer()
        .withName("sidecar1")
        .withImage("busybox")
        .endContainer()
        .addNewContainer()
        .withName("sidecar2")
        .withImage("busybox")
        .endContainer();
    final PodSpec defaultPodSpec = defaultPodSpec();
    // When
    final String result = KubernetesResourceUtil.mergePodSpec(
        fragment, defaultPodSpec, "default-name", true);
    // Then
    assertThat(result).isEqualTo("spring-boot");
    assertThat(fragment.build().getContainers())
        .hasSize(3)
        .extracting("name", "image")
        .containsExactlyInAnyOrder(
            tuple("spring-boot", "spring-boot-test:latest"),
            tuple("sidecar1", "busybox"),
            tuple("sidecar2", "busybox")
        );
  }

  @Test
  void getNameWithSuffix_withKnownMapping_shouldReturnKnownMapping() {
    assertThat(getNameWithSuffix("name", "Pod")).isEqualTo("name-pod");
  }

  @Test
  void getNameWithSuffix_withUnknownMapping_shouldReturnCR() {
    assertThat(getNameWithSuffix("name", "VeryCustomKind")).isEqualTo("name-cr");
  }

  @Test
  void updateKindFilenameMappings_whenAddsCronTabMapping_updatesKindToFileNameMapper() {
    // Given
    final Map<String, List<String>> mappings = Collections.singletonMap("CronTab", Collections.singletonList("foo"));
    // When
    KubernetesResourceUtil.updateKindFilenameMapper(mappings);
    // Then
    assertThat(getNameWithSuffix("name", "CronTab")).isEqualTo("name-foo");
    assertThat(KIND_TO_FILENAME_MAPPER).containsKey("CronTab");
    assertThat(FILENAME_TO_KIND_MAPPER).containsKey("foo");
  }

  @Test
  void createConfigMapEntry_whenKeyAndPathProvided_thenShouldCreateEntryWithFileContents() throws IOException {
    // Given
    URL fileUrl = getClass().getResource("/kubernetes-resource-util/configmap-directory/test.properties");
    assertThat(fileUrl).isNotNull();

    // When
    Map.Entry<String, String> entry = createConfigMapEntry("custom-key", Paths.get(fileUrl.getFile()));

    // Then
    assertThat(entry)
        .satisfies(e -> assertThat(e.getKey()).isEqualTo("custom-key"))
        .satisfies(e -> assertThat(e.getValue()).isEqualTo("db.url=jdbc:mysql://localhost:3306/sample_db"));
  }

  @Test
  void createConfigMapEntry_whenBinaryFileProvided_thenShouldCreateEntryWithFileContents() throws IOException {
    // Given
    URL fileUrl = getClass().getResource("/kubernetes-resource-util/test.bin");
    assertThat(fileUrl).isNotNull();

    // When
    Map.Entry<String, String> entry = createConfigMapEntry("custom-key", Paths.get(fileUrl.getFile()));

    // Then
    assertThat(entry)
        .satisfies(e -> assertThat(e.getKey()).isEqualTo("custom-key"))
        .satisfies(e -> assertThat(e.getValue()).isEqualTo("wA=="));
  }

  @Test
  void addNewConfigMapEntriesToExistingConfigMap_whenFileProvided_thenShouldCreateConfigMapWithFile() throws IOException {
    // Given
    URL fileUrl = getClass().getResource("/kubernetes-resource-util/configmap-directory/test.properties");
    assertThat(fileUrl).isNotNull();
    ConfigMapBuilder configMapBuilder = new ConfigMapBuilder();

    // When
    addNewConfigMapEntriesToExistingConfigMap(configMapBuilder, "custom-key", Paths.get(fileUrl.getFile()));

    // Then
    assertThat(configMapBuilder.build())
        .asInstanceOf(InstanceOfAssertFactories.type(ConfigMap.class))
        .extracting(ConfigMap::getData)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("custom-key", "db.url=jdbc:mysql://localhost:3306/sample_db");
  }

  @Test
  void addNewConfigMapEntriesToExistingConfigMap_whenBinaryFileProvided_thenShouldCreateConfigMapWithBinaryContent() throws IOException {
    // Given
    URL fileUrl = getClass().getResource("/kubernetes-resource-util/test.bin");
    assertThat(fileUrl).isNotNull();
    ConfigMapBuilder configMapBuilder = new ConfigMapBuilder();

    // When
    addNewConfigMapEntriesToExistingConfigMap(configMapBuilder, "custom-key", Paths.get(fileUrl.getFile()));

    // Then
    assertThat(configMapBuilder.build())
        .asInstanceOf(InstanceOfAssertFactories.type(ConfigMap.class))
        .extracting(ConfigMap::getBinaryData)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("custom-key", "wA==");
  }

    @Test
  void addNewConfigMapEntriesToExistingConfigMap_whenDirectoryProvided_thenShouldCreateConfigMapWithFilesInDir() throws IOException {
    // Given
    URL fileUrl = getClass().getResource("/kubernetes-resource-util/configmap-directory");
    assertThat(fileUrl).isNotNull();
    ConfigMapBuilder configMapBuilder = new ConfigMapBuilder();

    // When
    addNewConfigMapEntriesToExistingConfigMap(configMapBuilder, "custom-key", Paths.get(fileUrl.getFile()));

    // Then
    assertThat(configMapBuilder.build())
        .asInstanceOf(InstanceOfAssertFactories.type(ConfigMap.class))
        .extracting(ConfigMap::getData)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("test.properties", "db.url=jdbc:mysql://localhost:3306/sample_db")
        .containsEntry("prod.properties", "db.url=jdbc:mysql://prod.example.com:3306/sample_db");
  }

  private static PodSpec defaultPodSpec() {
    return new PodSpecBuilder()
        .addNewContainer()
        .withName("spring-boot")
        .withImage("spring-boot-test:latest")
        .addNewEnv()
        .withName("KUBERNETES_NAMESPACE")
        .withNewValueFrom()
        .withNewFieldRef()
        .withFieldPath("metadata.namespace")
        .endFieldRef()
        .endValueFrom()
        .endEnv()
        .withImagePullPolicy("IfNotPresent")
        .addNewPort().withContainerPort(8080).withProtocol("TCP").endPort()
        .addNewPort().withContainerPort(9779).withProtocol("TCP").endPort()
        .addNewPort().withContainerPort(8778).withProtocol("TCP").endPort()
        .endContainer()
        .build();
  }
}

