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
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.assertj.core.api.InstanceOfAssertFactories;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.InitContainerConfig;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.kit.config.resource.PlatformMode.kubernetes;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.FILENAME_TO_KIND_MAPPER;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.KIND_TO_FILENAME_MAPPER;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.addNewConfigMapEntriesToExistingConfigMap;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.createConfigMapEntry;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.createNewInitContainersFromConfig;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.getNameWithSuffix;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.handleKubernetesClientException;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.initializeKindFilenameMapper;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.isContainerImage;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.mergeResources;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.spy;

class KubernetesResourceUtilTest {

  private static File fragmentsDir;
  private KitLogger log;
  private ImageConfiguration imageConfiguration;

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
    log = spy(new KitLogger.SilentLogger());
    imageConfiguration = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("base:latest")
            .build())
        .build();
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

  @Test
  void remove_whenInvoked_shouldRemoveKindFilenameMappings() {
    // Given
    KubernetesResourceUtil.updateKindFilenameMapper(Collections.singletonMap("CronTab", Collections.singletonList("foo")));

    // When
    KubernetesResourceUtil.remove("CronTab", "foo");

    // Then
    assertThat(KIND_TO_FILENAME_MAPPER).doesNotContainKey("CronTab");
    assertThat(FILENAME_TO_KIND_MAPPER).doesNotContainKey("foo");
  }

  @Test
  void updateKindFilenameMappings_whenProvidedMappingConfigs_thenShouldAddMappings() {
    // Given
    List<MappingConfig> mappingConfigs = Collections.singletonList(MappingConfig.builder()
            .kind("CronTab")
            .filenameTypes("crontab,cr")
        .build());

    // When
    KubernetesResourceUtil.updateKindFilenameMappings(mappingConfigs);

    // Then
    assertThat(KIND_TO_FILENAME_MAPPER).containsEntry("CronTab", "cr");
    assertThat(FILENAME_TO_KIND_MAPPER)
        .containsEntry("crontab", "CronTab")
        .containsEntry("cr", "CronTab");
  }

  @Test
  void updateKindFilenameMappings_whenProvidedInvalidMappingConfigs_thenThrowException() {
    // Given
    List<MappingConfig> mappingConfigs = Collections.singletonList(MappingConfig.builder()
        .kind("foo")
        .build());

    // When + Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KubernetesResourceUtil.updateKindFilenameMappings(mappingConfigs))
        .withMessageContaining("Invalid mapping for Kind foo and Filename Types");
  }

  @Test
  void removeItemFromKubernetesBuilder_whenInvoked_shouldRemoveItem() {
    // Given
    HasMetadata sa = new ServiceAccountBuilder().withNewMetadata().withName("sa1").endMetadata().build();
    HasMetadata pod = new PodBuilder().withNewMetadata().withName("p1").endMetadata().build();
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(sa);
    kubernetesListBuilder.addToItems(pod);

    // When
    KubernetesResourceUtil.removeItemFromKubernetesBuilder(kubernetesListBuilder, sa);

    // Then
    assertThat(kubernetesListBuilder.buildItems())
        .hasSize(1)
        .containsExactly(pod);
  }

  @Test
  void checkForKind_whenPresent_shouldReturnTrue() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new DeploymentBuilder().build());

    // When
    boolean result = KubernetesResourceUtil.checkForKind(klb, "Deployment");

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void checkForKind_whenNotPresent_shouldReturnFalse() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new DeploymentBuilder().build());

    // When
    boolean result = KubernetesResourceUtil.checkForKind(klb, "DeploymentConfig");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void handleKubernetesClientException_whenCauseNoHost_thenThrowExceptionWithMessage() {
    // Given
    KubernetesClientException exception = new KubernetesClientException("test", new UnknownHostException());

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> handleKubernetesClientException(exception, log))
        .withMessageContaining("Could not connect to kubernetes cluster. Are you sure if you're connected to a remote cluster via `kubectl`? Error: ");
  }

  @Test
  void handleKubernetesClientException_whenInvoked_thenThrowExceptionWithMessage() {
    // Given
    KubernetesClientException exception = new KubernetesClientException("kubernetes failure", new RuntimeException());

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> handleKubernetesClientException(exception, log))
        .withMessageContaining("kubernetes failure");
  }

  @Nested
  @DisplayName("mergeResources tests with different inputs")
  class MergeResourcesTest {
    @Test
    void mergeResources_whenDeploymentProvidedAndFirstDeploymentWithEmptySpec_thenShouldMergeBothObjects() {
      // Given
      Deployment d1 = new DeploymentBuilder()
          .withNewMetadata()
          .withName("test-deploy")
          .endMetadata()
          .build();
      Deployment d2 = new DeploymentBuilder()
          .withNewSpec()
          .withNewTemplate()
          .withNewSpec()
          .addNewContainer()
          .addToEnv(new EnvVarBuilder().withName("E1").withValue("V1").build())
          .withName("foo")
          .endContainer()
          .endSpec()
          .endTemplate()
          .endSpec()
          .build();

      // When
      HasMetadata mergedDeployment = mergeResources(d1, d2, log, true);

      // Then
      assertThat(mergedDeployment)
          .isInstanceOf(Deployment.class)
          .asInstanceOf(InstanceOfAssertFactories.type(Deployment.class))
          .hasFieldOrPropertyWithValue("metadata.name", "test-deploy")
          .extracting(Deployment::getSpec)
          .extracting(DeploymentSpec::getTemplate)
          .extracting(PodTemplateSpec::getSpec)
          .extracting(PodSpec::getContainers)
          .asList()
          .singleElement(InstanceOfAssertFactories.type(Container.class))
          .hasFieldOrPropertyWithValue("env", Collections.singletonList(new EnvVarBuilder().withName("E1").withValue("V1").build()))
          .hasFieldOrPropertyWithValue("name", "foo");
    }

    @Test
    void mergeResources_whenDeploymentProvidedAndSecondDeploymentWithEmptySpec_thenShouldMergeBothObjects() {
      // Given
      Deployment d1 = new DeploymentBuilder()
          .withNewSpec()
          .withNewTemplate()
          .withNewSpec()
          .addNewContainer()
          .withName("foo")
          .endContainer()
          .endSpec()
          .endTemplate()
          .endSpec()
          .build();
      Deployment d2 = new DeploymentBuilder()
          .withNewMetadata()
          .withName("test-deploy")
          .endMetadata()
          .build();

      // When
      HasMetadata mergedDeployment = mergeResources(d1, d2, log, true);

      // Then
      assertThat(mergedDeployment)
          .isInstanceOf(Deployment.class)
          .asInstanceOf(InstanceOfAssertFactories.type(Deployment.class))
          .hasFieldOrPropertyWithValue("metadata.name", "test-deploy")
          .extracting(Deployment::getSpec)
          .extracting(DeploymentSpec::getTemplate)
          .extracting(PodTemplateSpec::getSpec)
          .extracting(PodSpec::getContainers)
          .asList()
          .singleElement(InstanceOfAssertFactories.type(Container.class))
          .hasFieldOrPropertyWithValue("name", "foo");
    }

    @Test
    void mergeResources_whenBothDeploymentNonEmptySpec_thenShouldMergeBothObjects() {
      // Given
      Deployment d1 = new DeploymentBuilder()
          .withNewMetadata()
          .withName("test-deploy")
          .endMetadata()
          .withNewSpec()
          .withNewTemplate()
          .withNewMetadata()
          .addToLabels("app1", "test-deploy1")
          .endMetadata()
          .withNewSpec()
          .addNewContainer()
          .withName("c1")
          .endContainer()
          .endSpec()
          .endTemplate()
          .endSpec()
          .build();
      Deployment d2 = new DeploymentBuilder()
          .withNewSpec()
          .withNewTemplate()
          .withNewMetadata()
          .addToLabels("app2", "test-deploy2")
          .endMetadata()
          .withNewSpec()
          .addNewContainer()
          .withName("c2")
          .withImage("img2:latest")
          .endContainer()
          .endSpec()
          .endTemplate()
          .endSpec()
          .build();

      // When
      HasMetadata mergedDeployment = mergeResources(d1, d2, log, true);

      // Then
      assertThat(mergedDeployment)
          .isInstanceOf(Deployment.class)
          .asInstanceOf(InstanceOfAssertFactories.type(Deployment.class))
          .hasFieldOrPropertyWithValue("metadata.name", "test-deploy")
          .hasFieldOrPropertyWithValue("spec.template.metadata.labels.app1", "test-deploy1")
          .hasFieldOrPropertyWithValue("spec.template.metadata.labels.app2", "test-deploy2")
          .extracting(Deployment::getSpec)
          .extracting(DeploymentSpec::getTemplate)
          .extracting(PodTemplateSpec::getSpec)
          .extracting(PodSpec::getContainers)
          .asList()
          .singleElement(InstanceOfAssertFactories.type(Container.class))
          .hasFieldOrPropertyWithValue("name", "c1");
    }

    @Test
    void mergeResources_whenConfigMapsProvided_thenMergeBothConfigMaps() {
      // Given
      ConfigMap configMap1 = new ConfigMapBuilder()
          .withNewMetadata().withName("c1").endMetadata()
          .addToData("key1", "value1")
          .build();
      ConfigMap configMap2 = new ConfigMapBuilder()
          .withNewMetadata().withName("c2").endMetadata()
          .addToData("key2", "value2")
          .addToData("key3", "")
          .build();

      // When
      HasMetadata mergedConfigMap = mergeResources(configMap1, configMap2, log, true);
      // Then
      assertThat(mergedConfigMap)
          .isInstanceOf(ConfigMap.class)
          .asInstanceOf(InstanceOfAssertFactories.type(ConfigMap.class))
          .hasFieldOrPropertyWithValue("metadata.name", "c1")
          .hasFieldOrPropertyWithValue("data.key1", "value1")
          .hasFieldOrPropertyWithValue("data.key2", "value2")
          .extracting(ConfigMap::getData)
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .hasSize(2);
    }

    @Test
    void mergeResources_whenConfigMapsProvidedAndLocalCustomizationDisabled_thenMergeBothConfigMaps() {
      // Given
      ConfigMap configMap1 = new ConfigMapBuilder()
          .withNewMetadata().withName("c1").addToLabels("l1", "v1").endMetadata()
          .addToData("key1", "value1")
          .build();
      ConfigMap configMap2 = new ConfigMapBuilder()
          .withNewMetadata().withName("c2").addToLabels("l2", "v2").endMetadata()
          .addToData("key2", "value2")
          .build();

      // When
      HasMetadata mergedConfigMap = mergeResources(configMap1, configMap2, log, false);
      // Then
      assertThat(mergedConfigMap)
          .isInstanceOf(ConfigMap.class)
          .asInstanceOf(InstanceOfAssertFactories.type(ConfigMap.class))
          .hasFieldOrPropertyWithValue("metadata.name", "c1")
          .hasFieldOrPropertyWithValue("metadata.labels.l1", "v1")
          .hasFieldOrPropertyWithValue("metadata.labels.l2", "v2")
          .hasFieldOrPropertyWithValue("data.key1", "value1")
          .hasFieldOrPropertyWithValue("data.key2", "value2")
          .extracting(ConfigMap::getData)
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .hasSize(2);
    }

    @Test
    void mergeResources_whenPodsProvided_thenMergeBothPodMetadataOnly() {
      // Given
      Pod p1 = new PodBuilder()
          .withNewMetadata().withName("p1").addToLabels("l1", "v1").endMetadata()
          .withNewSpec()
          .addNewContainer()
          .withName("c1")
          .withImage("image1:latest")
          .endContainer()
          .endSpec()
          .build();
      Pod p2 = new PodBuilder()
          .withNewMetadata().withName("p2").addToLabels("l2", "v2").endMetadata()
          .withNewSpec()
          .addNewContainer()
          .withName("c2")
          .withImage("image2:latest")
          .endContainer()
          .endSpec()
          .build();

      // When
      HasMetadata mergedConfigMap = mergeResources(p1, p2, log, true);
      // Then
      assertThat(mergedConfigMap)
          .isInstanceOf(Pod.class)
          .asInstanceOf(InstanceOfAssertFactories.type(Pod.class))
          .hasFieldOrPropertyWithValue("metadata.name", "p1")
          .hasFieldOrPropertyWithValue("metadata.labels.l1", "v1")
          .hasFieldOrPropertyWithValue("metadata.labels.l2", "v2")
          .extracting(Pod::getSpec)
          .extracting(PodSpec::getContainers)
          .asList()
          .singleElement(InstanceOfAssertFactories.type(Container.class))
          .hasFieldOrPropertyWithValue("name", "c1")
          .hasFieldOrPropertyWithValue("image", "image1:latest");
    }
  }

  @Nested
  @DisplayName("tests related to initContainer manipulation")
  class InitContainerTests {

    @Test
    void simple() {
      PodTemplateSpecBuilder builder = getPodTemplateBuilder();
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isFalse();
      Container initContainer = createInitContainer("blub", "foo/blub");
      KubernetesResourceUtil.appendInitContainer(builder, initContainer, log);
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      verifyBuilder(builder, Collections.singletonList(initContainer));
    }

    @Test
    void append() {
      PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isFalse();
      Container initContainer = createInitContainer("blub", "foo/blub");
      KubernetesResourceUtil.appendInitContainer(builder, initContainer, log);
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      verifyBuilder(builder, Arrays.asList(createInitContainer("bla", "foo/bla"), initContainer));
    }

    @Test
    void appendAndEnsureSpec() {
      PodTemplateSpecBuilder builder =new PodTemplateSpecBuilder();
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isFalse();
      Container initContainer = createInitContainer("blub", "foo/blub");
      KubernetesResourceUtil.appendInitContainer(builder, initContainer, log);
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      verifyBuilder(builder, Collections.singletonList(initContainer));
    }

    @Test
    void removeAll() {
      PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "bla")).isTrue();
      KubernetesResourceUtil.removeInitContainer(builder, "bla");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "bla")).isFalse();
      verifyBuilder(builder, null);
    }

    @Test
    void removeOne() {
      PodTemplateSpecBuilder builder = getPodTemplateBuilder("bla", "foo/bla", "blub", "foo/blub");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "bla")).isTrue();
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      KubernetesResourceUtil.removeInitContainer(builder, "bla");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "bla")).isFalse();
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      verifyBuilder(builder, Collections.singletonList(createInitContainer("blub", "foo/blub")));
    }

    @Test
    void existingSame() {
      PodTemplateSpecBuilder builder = getPodTemplateBuilder("blub", "foo/blub");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      Container initContainer = createInitContainer("blub", "foo/blub");
      KubernetesResourceUtil.appendInitContainer(builder, initContainer, log);
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      verifyBuilder(builder, Collections.singletonList(initContainer));
    }

    @Test
    void existingDifferent() {
      PodTemplateSpecBuilder builder = getPodTemplateBuilder("blub", "foo/bla");
      assertThat(KubernetesResourceUtil.hasInitContainer(builder, "blub")).isTrue();
      Container initContainer = createInitContainer("blub", "foo/blub");
      assertThatIllegalArgumentException()
          .isThrownBy(() -> KubernetesResourceUtil.appendInitContainer(builder, initContainer, log))
          .withMessageContaining("blub");
    }

    @Test
    void createNewInitContainersFromConfig_whenConfigProvided_thenReturnsValidContainerList() {
      // Given
      List<InitContainerConfig> initContainerConfigs = new ArrayList<>();
      initContainerConfigs.add(InitContainerConfig.builder()
          .env(Collections.singletonMap("FOO_ENV", "BAR"))
          .imageName("foo/bar:latest")
          .name("init1")
          .imagePullPolicy("Always")
          .cmd(org.eclipse.jkube.kit.common.Arguments.builder()
              .exec(Arrays.asList("sleep", "10"))
              .build())
          .volumes(Collections.singletonList(VolumeConfig.builder()
              .name("workdir")
              .path("/work-dir")
              .build()))
          .build());

      // When
      List<Container> initContainers = createNewInitContainersFromConfig(initContainerConfigs);

      // Then
      assertThat(initContainers)
          .singleElement()
          .hasFieldOrPropertyWithValue("env", Collections.singletonList(new EnvVarBuilder().withName("FOO_ENV").withValue("BAR").build()))
          .hasFieldOrPropertyWithValue("image", "foo/bar:latest")
          .hasFieldOrPropertyWithValue("name", "init1")
          .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
          .hasFieldOrPropertyWithValue("command", Arrays.asList("sleep", "10"))
          .hasFieldOrPropertyWithValue("volumeMounts", Collections.singletonList(new VolumeMountBuilder().withName("workdir").withMountPath("/work-dir").build()));
    }

    @Test
    void isContainerImage_whenImagePresentInInitContainerConfig_thenReturnFalse() {
      // Given
      ControllerResourceConfig controllerResourceConfig = ControllerResourceConfig.builder()
          .initContainer(InitContainerConfig.builder().imageName("foo/bar:latest").build())
          .build();

      // When
      boolean result = isContainerImage(imageConfiguration, controllerResourceConfig);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    void isContainerImage_whenImageAbsentInInitContainerConfig_thenReturnTrue() {
      // Given
      ControllerResourceConfig controllerResourceConfig = ControllerResourceConfig.builder()
          .initContainers(Collections.emptyList())
          .build();

      // When
      boolean result = isContainerImage(imageConfiguration, controllerResourceConfig);

      // Then
      assertThat(result).isTrue();
    }
  }

  private void verifyBuilder(PodTemplateSpecBuilder builder, List<Container> initContainers) {
    PodTemplateSpec spec = builder.build();
    List<Container> initContainersInSpec = spec.getSpec().getInitContainers();
    if (initContainersInSpec.size() == 0) {
      assertThat(initContainers).isNull();;
    } else {
      assertThat(initContainers).hasSameSizeAs(initContainersInSpec);
      for (int i = 0; i < initContainers.size(); i++) {
        assertThat(initContainers.get(i)).isEqualTo(initContainersInSpec.get(i));
      }
    }
  }

  private PodTemplateSpecBuilder getPodTemplateBuilder(String ... definitions) {
    PodTemplateSpecBuilder ret = new PodTemplateSpecBuilder();
    ret.withNewMetadata().withName("test-pod-templateSpec").endMetadata().withNewSpec().withInitContainers(getInitContainerList(definitions)).endSpec();
    return ret;
  }

  private List<Container> getInitContainerList(String ... definitions) {
    List<Container> ret = new ArrayList<>();
    for (int i = 0; i < definitions.length; i += 2 ) {
      ret.add(createInitContainer(definitions[i], definitions[i+1]));
    }
    return ret;
  }

  private Container createInitContainer(String name, String image) {
    return new ContainerBuilder()
        .withName(name)
        .withImage(image)
        .build();
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

