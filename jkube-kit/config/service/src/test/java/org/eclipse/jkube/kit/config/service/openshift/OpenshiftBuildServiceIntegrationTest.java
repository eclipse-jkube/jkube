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
package org.eclipse.jkube.kit.config.service.openshift;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.build.service.docker.ArchiveService;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.BuildRecreateMode;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ContainerResourcesConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient
class OpenshiftBuildServiceIntegrationTest {

  KubernetesMockServer mockServer;
  OpenShiftClient client;

  private JKubeServiceHub jKubeServiceHub;

  private MockedConstruction<JKubeBuildTarArchiver> jKubeBuildTarArchiver;

  private File baseDirectory;

  private String baseDir;

  private File target;

  private String projectName;

  private ImageConfiguration image;

  private BuildServiceConfig.BuildServiceConfigBuilder defaultConfig;

  private ResourceConfig resourceConfig;

  @BeforeEach
  void init(@TempDir Path temporaryFolder) throws Exception {
    final KitLogger logger = new KitLogger.StdoutLogger();
    baseDirectory = Files.createDirectory(temporaryFolder.resolve("openshift-build-service")).toFile();
    target = Files.createDirectory(baseDirectory.toPath().resolve("target")).toFile();
    final File emptyDockerBuildTar = new File(target, "docker-build.tar");
    FileUtils.touch(emptyDockerBuildTar);
    baseDir = baseDirectory.getAbsolutePath();
    projectName = "myapp";
    FileUtils.deleteDirectory(new File(baseDir, projectName));
    final File dockerFile = new File(baseDir, "Docker.tar");
    FileUtils.touch(dockerFile);

    jKubeBuildTarArchiver = mockConstruction(JKubeBuildTarArchiver.class, (mock, ctx) ->
        when(mock.createArchive(any(File.class), any(BuildDirs.class), eq(ArchiveCompression.none)))
        .thenReturn(emptyDockerBuildTar));

    resourceConfig = ResourceConfig.builder().namespace("ns1").build();

    jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getClient()).thenReturn(client);
    when(jKubeServiceHub.getLog()).thenReturn(logger);
    when(jKubeServiceHub.getDockerServiceHub().getArchiveService())
        .thenReturn(new ArchiveService(AssemblyManager.getInstance(), logger));
    when(jKubeServiceHub.getBuildServiceConfig().getBuildDirectory()).thenReturn(target.getAbsolutePath());
    when(jKubeServiceHub.getConfiguration()).thenReturn(JKubeConfiguration.builder()
        .outputDirectory(target.getName())
        .project(JavaProject.builder()
            .baseDirectory(baseDirectory)
            .buildDirectory(target)
            .build())
        .pullRegistryConfig(RegistryConfig.builder().build())
        .clusterConfiguration(ClusterConfiguration.from(client.getConfiguration()).build())
        .build());

    image = ImageConfiguration.builder()
        .name(projectName)
        .build(BuildConfiguration.builder()
            .from(projectName)
            .openshiftS2iBuildNameSuffix("-s2i-suffix-configured-in-image")
            .openshiftBuildRecreateMode(BuildRecreateMode.none)
            .build()
        ).build();

    defaultConfig = BuildServiceConfig.builder()
        .buildDirectory(baseDir)
        .resourceConfig(resourceConfig)
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i);
  }

  @AfterEach
  void tearDown() {
    jKubeBuildTarArchiver.close();
  }

  @Test
  @DisplayName("S2I build with custom suffix should create BuildConfig with configured suffix")
  void successfulBuild() throws Exception {
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    collector.assertEventsNotRecorded("patch-build-config");
    assertThat(containsRequest("imagestreams")).isTrue();
    // Verify BuildConfig structure
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i-suffix-configured-in-image")
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("output.to.kind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("output.to.name", "myapp:latest")
        .hasFieldOrPropertyWithValue("source.type", "Binary")
        .hasFieldOrPropertyWithValue("strategy.type", "Source")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.forcePull", false)
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.name", "myapp");
  }

  @Test
  @DisplayName("build should call PluginService.addExtraFiles()")
  void build_shouldCallPluginServiceAddFiles() throws JKubeServiceException {
    // Given
    withBuildServiceConfig(defaultConfig.build());
    MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();
    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);
    // Then
    verify(jKubeServiceHub.getPluginManager().resolvePluginService(), times(1)).addExtraFiles();
  }

  @Test
  @DisplayName("build with assembly should succeed")
  void build_withAssembly_shouldSucceed() throws Exception {
    // Given
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();
    image.setBuild(image.getBuild().toBuilder()
        .from(projectName)
        .assembly(AssemblyConfiguration.builder()
            .layer(Assembly.builder().id("one").build())
            .build())
        .build());
    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);
    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
  }

  @Test
  @DisplayName("build with Dockerfile mode and already-flattened assembly should throw exception")
  void build_withDockerfileModeAndFlattenedAssembly_shouldThrowException() {
    //Given
    image.setBuild(BuildConfiguration.builder()
        .dockerFile(new File(target, "Dockerfile").getAbsolutePath())
        .assembly(AssemblyConfiguration.builder()
            .layer(Assembly.builder().id("one").build())
            .build().getFlattenedClone(jKubeServiceHub.getConfiguration()))
        .build());
    image.getBuildConfiguration().initAndValidate();
    final OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);
    // When + Then
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> openshiftBuildService.build(image))
        .havingCause()
        .withMessage("This image has already been flattened, you can only flatten the image once");
  }

  @Test
  @DisplayName("build with Dockerfile mode and assembly should succeed")
  void build_withDockerfileModeAndAssembly_shouldSucceed() throws Exception {
    //Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    FileUtils.write(dockerFile, "FROM busybox\n", StandardCharsets.UTF_8);
    image.setBuild(BuildConfiguration.builder()
        .from(projectName)
        .dockerFile(dockerFile.getAbsolutePath())
        .assembly(AssemblyConfiguration.builder()
            .layer(Assembly.builder().id("one").baseDirectory(baseDirectory).build())
            .build())
        .build());
    image.getBuildConfiguration().initAndValidate();
    withBuildServiceConfig(defaultConfig.build());
    // Default S2I suffix is "-s2i" since no suffix is specified in BuildConfiguration
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();
    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);
    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
  }

  @Test
  @DisplayName("S2I build without custom suffix should use default -s2i suffix")
  void successfulBuildNoS2iSuffix() throws Exception {
    image = image.toBuilder()
        .build(image.getBuild().toBuilder().openshiftS2iBuildNameSuffix(null).build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    // Default S2I suffix is "-s2i" when openshiftS2iBuildNameSuffix is null
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    collector.assertEventsNotRecorded("patch-build-config");
    // Verify default -s2i suffix is used
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i")
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("strategy.type", "Source")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.kind", "DockerImage");
  }

  @Test
  @DisplayName("Docker build with custom suffix should create DockerStrategy BuildConfig")
  void dockerBuild() throws Exception {
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftS2iBuildNameSuffix("-docker")
            .build())
        .build();
    withBuildServiceConfig(BuildServiceConfig.builder()
        .buildDirectory(baseDir)
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .resourceConfig(resourceConfig).build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-docker")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    collector.assertEventsNotRecorded("patch-build-config");
    // Verify Docker strategy BuildConfig
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-docker")
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("output.to.kind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("output.to.name", "myapp:latest")
        .hasFieldOrPropertyWithValue("strategy.type", "Docker")
        .hasFieldOrPropertyWithValue("strategy.dockerStrategy.noCache", false)
        .hasFieldOrPropertyWithValue("strategy.dockerStrategy.from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("strategy.dockerStrategy.from.name", "myapp");
  }

  @Test
  @DisplayName("Docker build with multi-component image name and custom suffix should flatten name correctly")
  void dockerBuildWithMultiComponentImageName() throws Exception {
    withBuildServiceConfig(BuildServiceConfig.builder()
        .buildDirectory(baseDir)
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .resourceConfig(resourceConfig).build());
    image = image.toBuilder()
        .name("docker.io/registry/component1/component2/name:tag")
        .build(image.getBuild().toBuilder().openshiftS2iBuildNameSuffix("-docker").build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName("component1-component2-name")
        .buildConfigSuffix("-docker")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(collector.getBodies().get(1))
            .isEqualTo("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"component1-component2-name-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"component1-component2-name:tag\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}");
    collector.assertEventsNotRecorded("patch-build-config");
  }

  @Test
  @DisplayName("Docker build without custom suffix should have no suffix in BuildConfig name")
  void dockerBuildNoS2iSuffix() throws Exception {
    withBuildServiceConfig(BuildServiceConfig.builder()
        .buildDirectory(baseDir)
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .resourceConfig(resourceConfig)
        .build());
    image = image.toBuilder()
        .build(image.getBuild().toBuilder().openshiftS2iBuildNameSuffix(null).build())
        .build();
    // Docker strategy with null suffix = no suffix (empty string)
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(collector.getBodies().get(1))
            .isEqualTo("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"},\"noCache\":false},\"type\":\"Docker\"}}}");
    collector.assertEventsNotRecorded("patch-build-config");
  }

  @Test
  @DisplayName("Docker build with custom suffix and fromExt should use ImageStreamTag as base with namespace")
  void dockerBuildFromExt() throws Exception {
    withBuildServiceConfig(BuildServiceConfig.builder()
        .buildDirectory(baseDir)
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .resourceConfig(resourceConfig)
        .build());
    Map<String,String> fromExt = ImmutableMap.of("name", "app:1.2-1",
        "kind", "ImageStreamTag",
        "namespace", "my-project");
    image = ImageConfiguration.builder()
        .name(projectName)
        .build(BuildConfiguration.builder()
            .fromExt(fromExt)
            .nocache(Boolean.TRUE)
            .openshiftS2iBuildNameSuffix("-docker")
            .build()
        ).build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-docker")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    OpenshiftBuildService service = new OpenshiftBuildService(jKubeServiceHub);

    service.build(image);

    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(collector.getBodies().get(1))
            .isEqualTo("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-docker\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"dockerStrategy\":{\"from\":{\"kind\":\"ImageStreamTag\",\"name\":\"app:1.2-1\",\"namespace\":\"my-project\"},\"noCache\":true},\"type\":\"Docker\"}}}");
    collector.assertEventsNotRecorded("patch-build-config");
  }

  @Test
  @DisplayName("S2I build with pull secret should configure secret in BuildConfig")
  void successfulBuildSecret() throws Exception {
    withBuildServiceConfig(defaultConfig.build());
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftPullSecret("pullsecret-fabric8")
            .build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // we should find a better way to assert that a certain call has been made
    assertThat(mockServer.getRequestCount()).isGreaterThan(8);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    collector.assertEventsNotRecorded("patch-build-config");
  }

  @Test
  @DisplayName("S2I build with openshiftForcePull=true should set forcePull in SourceStrategy")
  void s2iBuild_withForcePullTrue_shouldSetForcePullInSourceStrategy() throws Exception {
    // Given
    withBuildServiceConfig(defaultConfig.build());
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftForcePull(true)
            .build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("strategy.type", "Source")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.forcePull", true);
  }

  @Test
  @DisplayName("S2I build with openshiftForcePull=false (default) should set forcePull to false")
  void s2iBuild_withForcePullFalse_shouldSetForcePullFalseInSourceStrategy() throws Exception {
    // Given - openshiftForcePull is false by default
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("strategy.type", "Source")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.forcePull", false);
  }

  @Test
  @DisplayName("build without mock server setup should fail with descriptive error")
  void failedBuild() {
    withBuildServiceConfig(defaultConfig.build());
    final OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> openshiftBuildService.build(image))
        .withMessageContaining("Unable to build the image using the OpenShift build service");
  }

  @Test
  @DisplayName("second build with existing BuildConfig should patch instead of create")
  void successfulSecondBuild() throws Exception {
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(true)
        .imageStreamExists(true)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    collector.assertEventsRecordedInOrder("build-config-check", "patch-build-config", "pushed");
    collector.assertEventsNotRecorded("new-build-config");
  }

  @Test
  @DisplayName("build with recreateMode=buildConfig should delete and recreate BuildConfig")
  void build_withRecreateModeBuildConfig_shouldDeleteAndRecreateBuildConfig() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftBuildRecreateMode(BuildRecreateMode.buildConfig)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(true)
        .imageStreamExists(true)
        .recreateMode(BuildRecreateMode.buildConfig)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "build-config-delete", "new-build-config", "pushed");
    collector.assertEventsNotRecorded("imagestream-delete");
  }

  @Test
  @DisplayName("build with recreateMode=imageStream should delete and recreate ImageStream")
  void build_withRecreateModeImageStream_shouldDeleteAndRecreateImageStream() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftBuildRecreateMode(BuildRecreateMode.imageStream)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(true)
        .imageStreamExists(true)
        .recreateMode(BuildRecreateMode.imageStream)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("imagestream-check", "imagestream-delete", "imagestream-create", "pushed");
    collector.assertEventsNotRecorded("build-config-delete");
  }

  @Test
  @DisplayName("build with recreateMode=all should delete and recreate both BuildConfig and ImageStream")
  void build_withRecreateModeAll_shouldDeleteAndRecreateBoth() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftBuildRecreateMode(BuildRecreateMode.all)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(true)
        .imageStreamExists(true)
        .recreateMode(BuildRecreateMode.all)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - BuildConfig operations happen first, then ImageStream delete, then create, then build
    collector.assertEventsRecorded("build-config-delete", "imagestream-delete", "new-build-config", "imagestream-create", "pushed");
  }

  @Test
  @DisplayName("build with recreateMode=none should patch existing resources without deletion")
  void build_withRecreateModeNone_shouldPatchExistingResourcesWithoutDeletion() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftBuildRecreateMode(BuildRecreateMode.none)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(true)
        .imageStreamExists(true)
        .recreateMode(BuildRecreateMode.none)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - should patch without any deletes
    collector.assertEventsRecordedInOrder("build-config-check", "patch-build-config", "pushed");
    collector.assertEventsNotRecorded("build-config-delete", "imagestream-delete", "new-build-config", "imagestream-create");
  }

  @Test
  @DisplayName("build with resource limits should include limits in BuildConfig")
  void successfulBuildWithResourceConfig() throws Exception {
    final Map<String, String> limitsMap = new HashMap<>();
    limitsMap.put("cpu", "100m");
    limitsMap.put("memory", "256Mi");
    resourceConfig = resourceConfig.toBuilder()
      .openshiftBuildConfig(ContainerResourcesConfig.builder().limits(limitsMap).build())
      .build();
    withBuildServiceConfig(defaultConfig.resourceConfig(resourceConfig).build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    collector.assertEventsNotRecorded("patch-build-config");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i-suffix-configured-in-image")
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("output.to.kind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("output.to.name", "myapp:latest")
        .hasFieldOrPropertyWithValue("output.to.namespace", null)
        .hasFieldOrPropertyWithValue("resources.limits.memory", Quantity.parse("256Mi"))
        .hasFieldOrPropertyWithValue("resources.limits.cpu", Quantity.parse("100m"))
        .hasFieldOrPropertyWithValue("source.type", "Binary")
        .hasFieldOrPropertyWithValue("strategy.type", "Source")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("strategy.sourceStrategy.from.name", "myapp");
    assertThat(containsRequest("imagestreams")).isTrue();
  }

  @Test
  @DisplayName("build with buildconfig.yml fragment should merge fragment spec into BuildConfig")
  void build_withBuildConfigFragment_shouldMergeFragmentSpec() throws Exception {
    // Given - create a buildconfig.yml fragment with custom triggers
    final File resourceDir = Files.createDirectories(baseDirectory.toPath().resolve("src/main/jkube")).toFile();
    final String fragmentContent = "apiVersion: build.openshift.io/v1\n" +
        "kind: BuildConfig\n" +
        "metadata:\n" +
        "  name: fragment-buildconfig\n" +
        "spec:\n" +
        "  triggers:\n" +
        "    - type: ConfigChange\n" +
        "    - type: ImageChange\n" +
        "  runPolicy: SerialLatestOnly\n";
    Files.write(resourceDir.toPath().resolve("buildconfig.yml"), fragmentContent.getBytes(StandardCharsets.UTF_8));
    withBuildServiceConfig(defaultConfig.resourceDir(resourceDir).build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - verify fragment was merged
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i-suffix-configured-in-image")
        .extracting(BuildConfig::getSpec)
        .satisfies(spec -> {
          // Fragment-specified fields
          assertThat(spec.getRunPolicy()).isEqualTo("SerialLatestOnly");
          assertThat(spec.getTriggers()).hasSize(2);
          assertThat(spec.getTriggers()).extracting("type")
              .containsExactlyInAnyOrder("ConfigChange", "ImageChange");
          // Generated fields (not overwritten by fragment)
          assertThat(spec.getOutput().getTo().getKind()).isEqualTo("ImageStreamTag");
          assertThat(spec.getOutput().getTo().getName()).isEqualTo("myapp:latest");
          assertThat(spec.getStrategy().getType()).isEqualTo("Source");
        });
  }

  @Test
  @DisplayName("build with DockerImage output kind should not create ImageStream")
  void successfulDockerImageOutputBuild() throws Exception {
    withBuildServiceConfig(defaultConfig.build());
    image = image.toBuilder()
        .build(image.getBuild()
            .toBuilder()
            .openshiftBuildOutputKind("DockerImage")
            .build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildOutputKind("DockerImage")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // we should add a better way to assert that a certain call has been made
    assertThat(mockServer.getRequestCount()).isGreaterThan(7);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(collector.getBodies().get(1))
            .isEqualTo("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix-configured-in-image\"},\"spec\":{\"output\":{\"to\":{\"kind\":\"DockerImage\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}");
    collector.assertEventsNotRecorded("patch-build-config");
    assertThat(containsRequest("imagestreams")).isFalse();
  }

  @Test
  @DisplayName("build with DockerImage output and push secret should configure push secret")
  void successfulDockerImageOutputBuildSecret() throws Exception {
    withBuildServiceConfig(defaultConfig.build());
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftPullSecret("pullsecret-fabric8")
            .openshiftPushSecret("pushsecret-fabric8")
            .openshiftBuildOutputKind("DockerImage")
            .build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildOutputKind("DockerImage")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // we should find a better way to assert that a certain call has been made
    assertThat(mockServer.getRequestCount()).isGreaterThan(7);
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(collector.getBodies().get(1))
            .isEqualTo("{\"apiVersion\":\"build.openshift.io/v1\",\"kind\":\"BuildConfig\",\"metadata\":{\"name\":\"myapp-s2i-suffix-configured-in-image\"},\"spec\":{\"output\":{\"pushSecret\":{\"name\":\"pushsecret-fabric8\"},\"to\":{\"kind\":\"DockerImage\",\"name\":\"myapp:latest\"}},\"source\":{\"type\":\"Binary\"},\"strategy\":{\"sourceStrategy\":{\"forcePull\":false,\"from\":{\"kind\":\"DockerImage\",\"name\":\"myapp\"}},\"type\":\"Source\"}}}");
    collector.assertEventsNotRecorded("patch-build-config");
    assertThat(containsRequest("imagestreams")).isFalse();
  }

  @Test
  @DisplayName("build with additional tags should create ImageStreamTags for each tag")
  void build_withAdditionalTags_shouldCreateNewImageStreamTags() throws Exception {
    // Given
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .additionalTagsCreated(true)
        .configure();
    image.setBuild(image.getBuild().toBuilder()
            .tag("t1")
        .build());
    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);
    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed", "imagestreamtag-get", "imagestreamtag-create");
  }

  @Test
  @DisplayName("build with skip enabled should not make any API calls")
  void build_withSkipEnabled_shouldNotMakeAnyAPICalls() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder().skip(true).build())
        .build();
    withBuildServiceConfig(defaultConfig.build());

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - no API calls should be made when build is skipped
    assertThat(mockServer.getRequestCount()).isZero();
  }

  @Test
  @DisplayName("build should use resourceConfig.namespace when specified")
  void build_withResourceConfigNamespace_shouldUseResourceConfigNamespace() throws Exception {
    // Given - resourceConfig has namespace "ns1" (set in @BeforeEach)
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .namespace("ns1")
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - API calls should be made to ns1 namespace (verified by MockServerSetup expectations)
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i-suffix-configured-in-image");
  }

  @Test
  @DisplayName("build should use clusterConfiguration.namespace when resourceConfig.namespace is null")
  void build_withoutResourceConfigNamespace_shouldUseClusterConfigurationNamespace() throws Exception {
    // Given - resourceConfig has no namespace, clusterConfiguration has "cluster-ns"
    final ResourceConfig noNamespaceResourceConfig = ResourceConfig.builder().build();
    withBuildServiceConfig(defaultConfig.resourceConfig(noNamespaceResourceConfig).build());
    // Override the jKubeConfiguration to use a specific cluster namespace
    when(jKubeServiceHub.getConfiguration()).thenReturn(JKubeConfiguration.builder()
        .outputDirectory(target.getName())
        .project(JavaProject.builder()
            .baseDirectory(baseDirectory)
            .buildDirectory(target)
            .build())
        .pullRegistryConfig(RegistryConfig.builder().build())
        .clusterConfiguration(ClusterConfiguration.builder().namespace("cluster-ns").build())
        .build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .namespace("cluster-ns")
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - API calls should be made to cluster-ns namespace
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-s2i-suffix-configured-in-image");
  }

  @Test
  @DisplayName("S2I build with openshiftS2iImageStreamLookupPolicyLocal=true should set lookupPolicy.local=true")
  void s2iBuild_withLookupPolicyLocalTrue_shouldCreateImageStreamWithLookupPolicyLocalTrue() throws Exception {
    // Given - explicitly set to true (Maven/Gradle plugins default to true)
    withBuildServiceConfig(defaultConfig.build());
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftS2iImageStreamLookupPolicyLocal(true)
            .build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - ImageStream should have lookupPolicy.local=true
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "imagestream-create", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(2), ImageStream.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp")
        .extracting(ImageStream::getSpec)
        .hasFieldOrPropertyWithValue("lookupPolicy.local", true);
  }

  @Test
  @DisplayName("S2I build with openshiftS2iImageStreamLookupPolicyLocal=false should set lookupPolicy.local=false")
  void s2iBuild_withLookupPolicyLocalFalse_shouldCreateImageStreamWithLookupPolicyLocalFalse() throws Exception {
    // Given - explicitly set to false
    withBuildServiceConfig(defaultConfig.build());
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .openshiftS2iImageStreamLookupPolicyLocal(false)
            .build())
        .build();
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - ImageStream should have lookupPolicy.local=false
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "imagestream-create", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(2), ImageStream.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp")
        .extracting(ImageStream::getSpec)
        .hasFieldOrPropertyWithValue("lookupPolicy.local", false);
  }

  @Test
  @DisplayName("S2I build with environment variables should include .s2i/environment in archive")
  void s2iBuild_withEnvVars_shouldIncludeS2iEnvironmentInArchive() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .env(ImmutableMap.of("MY_VAR", "my-value", "ANOTHER_VAR", "another-value"))
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - build completes and .s2i/environment file is included in archive
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(containsRequest("imagestreams")).isTrue();
    // Verify .s2i/environment file was added to the archive with env vars
    ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(jKubeBuildTarArchiver.constructed().get(0), atLeastOnce())
        .includeFile(fileCaptor.capture(), pathCaptor.capture());
    int s2iEnvIndex = pathCaptor.getAllValues().indexOf(".s2i/environment");
    assertThat(s2iEnvIndex).as(".s2i/environment should be included in archive").isGreaterThanOrEqualTo(0);
    assertThat(fileCaptor.getAllValues().get(s2iEnvIndex))
        .content()
        .contains("MY_VAR=my-value")
        .contains("ANOTHER_VAR=another-value");
  }

  @Test
  @DisplayName("build with DockerImage output and registry should include registry in output name")
  void build_withDockerImageOutputAndRegistry_shouldIncludeRegistryInOutputName() throws Exception {
    // Given
    image = ImageConfiguration.builder()
        .name("myapp:latest")
        .registry("my-registry.io")
        .build(BuildConfiguration.builder()
            .from("baseimage")
            .openshiftBuildOutputKind("DockerImage")
            .openshiftS2iBuildNameSuffix("-s2i")
            .openshiftBuildRecreateMode(BuildRecreateMode.none)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName("myapp")
        .buildConfigSuffix("-s2i")
        .buildOutputKind("DockerImage")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .extracting(bc -> bc.getSpec().getOutput().getTo().getName())
        .isEqualTo("my-registry.io/myapp:latest");
  }

  @Test
  @DisplayName("S2I build with multi-layer assembly should flatten layers")
  void s2iBuild_withMultiLayerAssembly_shouldFlattenLayers() throws Exception {
    // Given
    image = image.toBuilder()
        .build(image.getBuild().toBuilder()
            .assembly(AssemblyConfiguration.builder()
                .layer(Assembly.builder().id("layer1").build())
                .layer(Assembly.builder().id("layer2").build())
                .build())
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then - build should complete successfully (layers are flattened internally for S2I)
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
  }

  @Test
  @DisplayName("Docker build with build args in image configuration should include args in BuildConfig")
  void dockerBuild_withBuildArgsInImageConfig_shouldIncludeArgsInBuildConfig() throws Exception {
    // Given
    image = ImageConfiguration.builder()
        .name(projectName)
        .build(BuildConfiguration.builder()
            .from(projectName)
            .openshiftS2iBuildNameSuffix("-docker")
            .openshiftBuildRecreateMode(BuildRecreateMode.none)
            .args(ImmutableMap.of("BUILD_ARG_KEY", "build-arg-value"))
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.jKubeBuildStrategy(JKubeBuildStrategy.docker).build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-docker")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-docker")
        .satisfies(bc -> assertThat(bc.getSpec().getStrategy().getDockerStrategy().getBuildArgs())
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("name", "BUILD_ARG_KEY")
            .hasFieldOrPropertyWithValue("value", "build-arg-value"));
  }

  @Test
  @DisplayName("Docker build with build args in project properties should include args in BuildConfig")
  void dockerBuild_withBuildArgsInProjectProperties_shouldIncludeArgsInBuildConfig() throws Exception {
    // Given
    Properties properties = new Properties();
    properties.put("docker.buildArg.PROP_BUILD_ARG", "prop-build-arg-value");
    when(jKubeServiceHub.getConfiguration()).thenReturn(JKubeConfiguration.builder()
        .outputDirectory(target.getName())
        .project(JavaProject.builder()
            .baseDirectory(baseDirectory)
            .buildDirectory(target)
            .properties(properties)
            .build())
        .pullRegistryConfig(RegistryConfig.builder().build())
        .clusterConfiguration(ClusterConfiguration.from(client.getConfiguration()).build())
        .build());
    image = ImageConfiguration.builder()
        .name(projectName)
        .build(BuildConfiguration.builder()
            .from(projectName)
            .openshiftS2iBuildNameSuffix("-docker")
            .openshiftBuildRecreateMode(BuildRecreateMode.none)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.jKubeBuildStrategy(JKubeBuildStrategy.docker).build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-docker")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-docker")
        .satisfies(bc -> assertThat(bc.getSpec().getStrategy().getDockerStrategy().getBuildArgs())
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("name", "PROP_BUILD_ARG")
            .hasFieldOrPropertyWithValue("value", "prop-build-arg-value"));
  }

  @Test
  @DisplayName("build with failed status should throw exception with failure details")
  void build_withFailedBuildStatus_shouldThrowExceptionWithDetails() {
    // Given
    withBuildServiceConfig(defaultConfig.build());
    MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-s2i-suffix-configured-in-image")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .buildSucceeds(false) // Configure build to fail
        .configure();

    // When/Then
    final OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> openshiftBuildService.build(image))
        .withMessageContaining("Unable to build the image using the OpenShift build service")
        .havingCause()
        .isInstanceOf(IOException.class)
        .withMessageContaining("failed")
        .withMessageContaining("ExitCodeError");
  }

  @Test
  @DisplayName("Docker build with docker.nocache system property should override BuildConfiguration.nocache")
  void dockerBuild_withNoCacheSystemProperty_shouldSetNoCacheTrue() throws Exception {
    // Given - set docker.nocache system property
    System.setProperty("docker.nocache", "true");
    try {
      image = image.toBuilder()
          .build(BuildConfiguration.builder()
              .from(projectName)
              .nocache(Boolean.FALSE) // config says false, but system property should override
              .openshiftS2iBuildNameSuffix("-docker")
              .openshiftBuildRecreateMode(BuildRecreateMode.none)
              .build())
          .build();
      withBuildServiceConfig(defaultConfig.jKubeBuildStrategy(JKubeBuildStrategy.docker).build());
      final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
          .resourceName(projectName)
          .buildConfigSuffix("-docker")
          .buildConfigExists(false)
          .imageStreamExists(false)
          .configure();

      // When
      new OpenshiftBuildService(jKubeServiceHub).build(image);

      // Then - noCache should be true due to system property override
      collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "imagestream-create", "pushed");
      assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
          .hasFieldOrPropertyWithValue("metadata.name", "myapp-docker")
          .extracting(BuildConfig::getSpec)
          .hasFieldOrPropertyWithValue("strategy.type", "Docker")
          .hasFieldOrPropertyWithValue("strategy.dockerStrategy.noCache", true);
    } finally {
      System.clearProperty("docker.nocache");
    }
  }

  @Test
  @DisplayName("Docker build with BuildConfiguration.nocache=true should set noCache in DockerStrategy")
  void dockerBuild_withNoCacheConfigTrue_shouldSetNoCacheTrue() throws Exception {
    // Given
    image = image.toBuilder()
        .build(BuildConfiguration.builder()
            .from(projectName)
            .nocache(Boolean.TRUE)
            .openshiftS2iBuildNameSuffix("-docker")
            .openshiftBuildRecreateMode(BuildRecreateMode.none)
            .build())
        .build();
    withBuildServiceConfig(defaultConfig.jKubeBuildStrategy(JKubeBuildStrategy.docker).build());
    final WebServerEventCollector collector = MockServerSetup.forServer(mockServer)
        .resourceName(projectName)
        .buildConfigSuffix("-docker")
        .buildConfigExists(false)
        .imageStreamExists(false)
        .configure();

    // When
    new OpenshiftBuildService(jKubeServiceHub).build(image);

    // Then
    collector.assertEventsRecordedInOrder("build-config-check", "new-build-config", "imagestream-create", "pushed");
    assertThat(Serialization.unmarshal(collector.getBodies().get(1), BuildConfig.class))
        .hasFieldOrPropertyWithValue("metadata.name", "myapp-docker")
        .extracting(BuildConfig::getSpec)
        .hasFieldOrPropertyWithValue("strategy.type", "Docker")
        .hasFieldOrPropertyWithValue("strategy.dockerStrategy.noCache", true);
  }

  private BuildServiceConfig withBuildServiceConfig(BuildServiceConfig buildServiceConfig) {
    when(jKubeServiceHub.getBuildServiceConfig()).thenReturn(buildServiceConfig);
    return buildServiceConfig;
  }

  private Boolean containsRequest(String path)   {
    try {
      int count = mockServer.getRequestCount();
      RecordedRequest request;
      while ( count-- > 0 ) {
        request = mockServer.takeRequest();
        if (request.getPath().contains(path)) {
          return true;
        }
      }
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
    return false;
  }
}
