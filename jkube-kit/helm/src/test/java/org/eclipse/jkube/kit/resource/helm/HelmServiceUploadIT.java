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
package org.eclipse.jkube.kit.resource.helm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.DefaultMockServer;
import io.fabric8.mockwebserver.MockServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.dsl.HttpMethod;
import io.fabric8.mockwebserver.internal.SimpleRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.util.Base64Util;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("HelmService.uploadHelmChart")
class HelmServiceUploadIT {

  private Map<ServerRequest, Queue<ServerResponse>> responses;
  private MockServer mockServer;
  private KitLogger logger;
  private HelmConfig helmConfig;
  private RegistryServerConfiguration registryServerConfiguration;
  private HelmService helmService;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    responses = new HashMap<>();
    mockServer = new DefaultMockServer(new Context(), new MockWebServer(), responses, true);
    logger = spy(new KitLogger.SilentLogger());
    final Path helmOutput = Files.createDirectory(temporaryFolder.resolve("helm-output"));
    Files.write(Files.createDirectory(helmOutput.resolve("kubernetes"))
      .resolve("Helm-Chart-1337-SNAPSHOT.tar"), "archive content".getBytes(StandardCharsets.UTF_8));
    helmConfig = HelmConfig.builder()
      .chart("Helm-Chart")
      .version("1337-SNAPSHOT")
      .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
      .snapshotRepository(HelmRepository.builder()
        .type(HelmRepository.HelmRepoType.ARTIFACTORY)
        .name("SNAP-REPO")
        .url(mockServer.url("/"))
        .build())
      .outputDir(helmOutput.toFile().getAbsolutePath())
      .tarballOutputDir(helmOutput.toFile().getAbsolutePath())
      .chartExtension("tar")
      .build();
    registryServerConfiguration = RegistryServerConfiguration.builder()
      .id("SNAP-REPO")
      .username("user")
      .password("pa33word")
      .build();
    helmService = new HelmService(
      JKubeConfiguration.builder()
        .project(JavaProject.builder().properties(new Properties()).build())
        .registryConfig(RegistryConfig.builder()
          .settings(Collections.singletonList(registryServerConfiguration)).build())
        .build(),
      new ResourceServiceConfig(),
      logger);
  }

  @AfterEach
  void tearDown() {
    helmService = null;
  }
  @Test
  @DisplayName("With no repository configuration throws Exception")
  void withNoRepositoryConfiguration_shouldFail() {
    // Given
    helmConfig.setVersion("1337-SNAPSHOT");
    helmConfig.setSnapshotRepository(null);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  @DisplayName("With repository configuration missing URL throws Exception")
  void withRepositoryConfigurationMissingUrl_shouldFail() {
    // Given
    helmConfig.setVersion("1337-SNAPSHOT");
    helmConfig.setSnapshotRepository(HelmRepository.builder()
      .type(HelmRepository.HelmRepoType.ARTIFACTORY)
      .name("name")
      .build());
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  @DisplayName("With repository configuration missing type throws Exception")
  void withRepositoryConfigurationMissingType_shouldFail() {
    // Given
    helmConfig.setVersion("1337-SNAPSHOT");
    helmConfig.setSnapshotRepository(HelmRepository.builder()
      .name("name")
      .url("https://example.com")
      .build());
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  @DisplayName("With repository configuration missing name throws Exception")
  void withRepositoryConfigurationMissingName_shouldFail() {
    // Given
    helmConfig.setVersion("1337-SNAPSHOT");
    helmConfig.setSnapshotRepository(HelmRepository.builder()
      .type(HelmRepository.HelmRepoType.ARTIFACTORY)
      .url("https://example.com")
      .build());
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  @DisplayName("With no server configuration throws Exception")
  void withNoServerConfiguration_shouldFail() {
    // Given
    helmConfig.getSnapshotRepository().setName("NOT-SNAP-REPO");
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
      helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("No credentials found for NOT-SNAP-REPO in configuration or settings.xml server list.");
  }

  @Test
  @DisplayName("With no server configuration and repository with username throws Exception")
  void withNoServerConfigurationAndRepositoryWithUsername_shouldFail() {
    // Given
    helmConfig.getSnapshotRepository().setName("NOT-SNAP-REPO");
    helmConfig.getSnapshotRepository().setUsername("user");
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
      helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("Repo NOT-SNAP-REPO has a username but no password defined.");
  }

  @Test
  @DisplayName("With server configuration missing username throws Exception")
  void withServerConfigurationWithoutUsername_shouldFail() {
    // Given
    registryServerConfiguration.setUsername(null);
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
      helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO was found in server list but has no username/password.");
  }

  @Test
  @DisplayName("With server configuration missing password throws Exception")
  void withServerConfigurationWithoutPassword_shouldFail() {
    // Given
    registryServerConfiguration.setPassword(null);
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
      helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO was found in server list but has no username/password.");
  }

  @Test
  void withServerErrorAndErrorStream_shouldThrowException() {
    // Given
     mockServer.expect()
       .put().withPath("/Helm-Chart-1337-SNAPSHOT.tar")
       .andReturn(500, "Server error in ES")
       .always();
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result)
      .isNotNull()
      .hasMessage("Server error in ES");
  }

  @Test
  void withServerErrorAndInputStream_shouldThrowException() {
    // Given
     mockServer.expect()
       .put().withPath("/Helm-Chart-1337-SNAPSHOT.tar")
       .andReturn(302, "Server error in IS")
       .always();
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result)
      .isNotNull()
      .hasMessage("Server error in IS");
  }

  @Test
  void withServerErrorAndNoDetails_shouldThrowException() {
    // Given
    mockServer.expect()
      .put().withPath("/Helm-Chart-1337-SNAPSHOT.tar")
      .andReturn(404, "")
      .always();
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
      () -> helmService.uploadHelmChart(helmConfig));
    // Then
    assertThat(result).isNotNull();
  }

  @DisplayName("Authorizes with registry server credentials (PUT)")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"ARTIFACTORY", "NEXUS"})
  void withPutUpload_shouldUseRegistryServerCredentials(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    expect(HttpMethod.PUT, "/Helm-Chart-1337-SNAPSHOT.tar", new NeedsAuthorizationResponse());
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    assertThat(mockServer.getLastRequest())
      .extracting(r -> r.getHeader("Authorization"))
      .isEqualTo("Basic " + Base64Util.encodeToString("user:pa33word"));
  }

  @DisplayName("Authorizes with registry server credentials (POST)")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"CHARTMUSEUM"})
  void withPostUpload_shouldUseRegistryServerCredentials(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    expect(HttpMethod.POST, "/", new NeedsAuthorizationResponse());
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    assertThat(mockServer.getLastRequest())
      .extracting(r -> r.getHeader("Authorization"))
      .isEqualTo("Basic " + Base64Util.encodeToString("user:pa33word"));
  }

  @DisplayName("Authorizes with repository credentials -take precedence- (PUT)")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"ARTIFACTORY", "NEXUS"})
  void withPutUploadAndRepositoryCredentials_shouldUseRepositoryCredentials(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    helmConfig.getSnapshotRepository().setUsername("these");
    helmConfig.getSnapshotRepository().setPassword("take-precedence");
    expect(HttpMethod.PUT, "/Helm-Chart-1337-SNAPSHOT.tar", new NeedsAuthorizationResponse());
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    assertThat(mockServer.getLastRequest())
      .extracting(r -> r.getHeader("Authorization"))
      .isEqualTo("Basic " + Base64Util.encodeToString("these:take-precedence"));
  }

  @DisplayName("Authorizes with repository credentials -take precedence- (POST)")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"CHARTMUSEUM"})
  void withPostUploadAndRepositoryCredentials_shouldUseRepositoryCredentials(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    helmConfig.getSnapshotRepository().setUsername("these");
    helmConfig.getSnapshotRepository().setPassword("take-precedence");
    expect(HttpMethod.POST, "/", new NeedsAuthorizationResponse());
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    assertThat(mockServer.getLastRequest())
      .extracting(r -> r.getHeader("Authorization"))
      .isEqualTo("Basic " + Base64Util.encodeToString("these:take-precedence"));
  }

  @DisplayName("Logs success after successful PUT request")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"ARTIFACTORY", "NEXUS"})
  void withSuccessfulPutUpload_shouldLogSuccess(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    mockServer.expect()
      .put().withPath("/Helm-Chart-1337-SNAPSHOT.tar")
      .andReturn(201, "Upload successful")
      .always();
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    verify(logger).info("Upload Successful");
  }

  @DisplayName("Logs success after successful POST request")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"CHARTMUSEUM"})
  void withSuccessfulPostUpload_shouldLogSuccess(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    mockServer.expect()
      .post().withPath("/")
      .andReturn(201, "Upload successful")
      .always();
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    verify(logger).info("Upload Successful");
  }

  @DisplayName("Sends file in PUT request")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"ARTIFACTORY", "NEXUS"})
  void withSuccessfulPutUpload_shouldPutFile(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    mockServer.expect()
      .put().withPath("/Helm-Chart-1337-SNAPSHOT.tar")
      .andReturn(201, "Upload successful")
      .always();
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    assertThat(mockServer.getLastRequest().getBody().readUtf8())
      .isEqualTo("archive content");
  }

  @DisplayName("Sends file in POST request")
  @ParameterizedTest(name = "{index}: with repository type {0}")
  @ValueSource(strings = {"CHARTMUSEUM"})
  void withSuccessfulPostUpload_shouldPostFile(String repositoryType) throws Exception {
    // Given
    helmConfig.getSnapshotRepository().setType(repositoryType);
    mockServer.expect()
      .post().withPath("/")
      .andReturn(201, "Upload successful")
      .always();
    // When
    helmService.uploadHelmChart(helmConfig);
    // Then
    assertThat(mockServer.getLastRequest().getBody().readUtf8())
      .isEqualTo("archive content");
  }

  @DisplayName("Nexus repository specifics")
  @Nested
  class Nexus {

    @BeforeEach
    void setUp() {
      helmConfig.getSnapshotRepository().setType("NEXUS");
    }

    @Test
    @DisplayName(".tar.gz extension is contracted to .tgz")
    void tgzExtensionHandling() throws Exception {
      // Given
      helmConfig.setChartExtension("tar.gz");
      Files.write(
        Paths.get(helmConfig.getOutputDir()).resolve("kubernetes")
          .resolve("Helm-Chart-1337-SNAPSHOT.tar.gz"),
        "I'm a tar.gz, not a .tgz".getBytes(StandardCharsets.UTF_8));
      mockServer.expect()
        .put().withPath("/Helm-Chart-1337-SNAPSHOT.tgz")
        .andReturn(201, "Upload successful")
        .always();
      // When
      helmService.uploadHelmChart(helmConfig);
      // Then
      assertThat(mockServer.getLastRequest().getBody().readUtf8())
        .isEqualTo("I'm a tar.gz, not a .tgz");
    }
  }

  @DisplayName("OCI repository specifics")
  @Nested
  class OCI {

    @BeforeEach
    void setUp() {
      helmConfig.getSnapshotRepository().setType("OCI");
    }

    @Test
    @DisplayName("Sends chart metadata file, tarball and manifest file in separate requests")
    @DisabledOnOs(OS.WINDOWS)
    void withSuccessfulUpload_shouldUploadBlobsAndUpdateManifest() throws Exception {
      // Given
      helmConfig.setChartExtension("tar.gz");
      Files.write(
          Paths.get(helmConfig.getOutputDir()).resolve("kubernetes")
              .resolve("Helm-Chart-1337-SNAPSHOT.tar.gz"),
          "I'm a tar.gz, not a .tgz".getBytes(StandardCharsets.UTF_8));
      Files.write(Paths.get(helmConfig.getOutputDir()).resolve("kubernetes").resolve("Chart.yaml"),
          "---\napiVersion: v1\nname: test-chart\nversion: 0.0.1".getBytes(StandardCharsets.UTF_8));
      mockServer.expect().post()
          .withPath("/v2/test-chart/blobs/uploads/")
          .andReply(new TestMockResponseProvider(202, singletonMap("Location", "/v2/test-chart/blobs/upload/first-upload-endpoint"), null))
          .once();
      mockServer.expect().post()
          .withPath("/v2/test-chart/blobs/uploads/")
          .andReply(new TestMockResponseProvider(202, singletonMap("Location", "/v2/test-chart/blobs/upload/second-upload-endpoint"), null))
          .once();
      mockServer.expect().put()
          .withPath("/v2/test-chart/blobs/upload/first-upload-endpoint?digest=sha256%3Abe0152670c8a31981ab17af598592ce91417c8be8700e708e4613739ec563031")
          .andReply(new TestMockResponseProvider(200, singletonMap("Docker-Content-Digest", "sha256:be0152670c8a31981ab17af598592ce91417c8be8700e708e4613739ec563031"), null))
          .once();
      mockServer.expect().put()
          .withPath("/v2/test-chart/blobs/upload/second-upload-endpoint?digest=sha256%3Ac7051faa2fb28d147b34070a6bce25eaf1ee6bb4ca3b47af5ee6148d50079154")
          .andReply(new TestMockResponseProvider(200, singletonMap("Docker-Content-Digest", "sha256:c7051faa2fb28d147b34070a6bce25eaf1ee6bb4ca3b47af5ee6148d50079154"), null))
          .once();
      mockServer.expect().put()
          .withPath("/v2/test-chart/manifests/0.0.1")
          .andReply(new TestMockResponseProvider(200, singletonMap("Docker-Content-Digest", "manifestdigest"), null))
          .once();
      // When
      helmService.uploadHelmChart(helmConfig);
      // Then
      assertThat(mockServer.getLastRequest().getBody().readUtf8())
          .isEqualTo("{\n" +
            "  \"schemaVersion\" : 2,\n" +
            "  \"config\" : {\n" +
            "    \"mediaType\" : \"application/vnd.cncf.helm.config.v1+json\",\n" +
            "    \"digest\" : \"sha256:be0152670c8a31981ab17af598592ce91417c8be8700e708e4613739ec563031\",\n" +
            "    \"size\" : 73\n" +
            "  },\n" +
            "  \"layers\" : [ {\n" +
            "    \"mediaType\" : \"application/vnd.cncf.helm.chart.content.v1.tar+gzip\",\n" +
            "    \"digest\" : \"sha256:c7051faa2fb28d147b34070a6bce25eaf1ee6bb4ca3b47af5ee6148d50079154\",\n" +
            "    \"size\" : 24\n" +
            "  } ]\n" +
            "}");
    }
    @Test
    @DisplayName("On Windows, Sends chart metadata file, tarball and manifest file in separate requests")
    @EnabledOnOs(OS.WINDOWS)
    void withSuccessfulUploadOnWindows_shouldUploadBlobsAndUpdateManifest() throws Exception {
      helmConfig.setChartExtension("tar.gz");
      Files.write(
          Paths.get(helmConfig.getOutputDir()).resolve("kubernetes")
              .resolve("Helm-Chart-1337-SNAPSHOT.tar.gz"),
          "I'm a tar.gz, not a .tgz".getBytes(StandardCharsets.UTF_8));
      Files.write(Paths.get(helmConfig.getOutputDir()).resolve("kubernetes").resolve("Chart.yaml"),
          "---\r\napiVersion: v1\r\nname: test-chart\r\nversion: 0.0.1".getBytes(StandardCharsets.UTF_8));

      mockServer.expect().post()
          .withPath("/v2/test-chart/blobs/uploads/")
          .andReply(new TestMockResponseProvider(202, singletonMap("Location", "/v2/test-chart/blobs/upload/first-upload-endpoint"), null))
          .once();
      mockServer.expect().post()
          .withPath("/v2/test-chart/blobs/uploads/")
          .andReply(new TestMockResponseProvider(202, singletonMap("Location", "/v2/test-chart/blobs/upload/second-upload-endpoint"), null))
          .once();
      mockServer.expect().put()
          .withPath("/v2/test-chart/blobs/upload/first-upload-endpoint?digest=sha256%3A46a607aed71245e2489a79ad8b401b269565de36f3aa7eadf6f7e368a8938a43")
          .andReply(new TestMockResponseProvider(200, singletonMap("Docker-Content-Digest", "sha256:46a607aed71245e2489a79ad8b401b269565de36f3aa7eadf6f7e368a8938a43"), null))
          .once();
      mockServer.expect().put()
          .withPath("/v2/test-chart/blobs/upload/second-upload-endpoint?digest=sha256%3Ac7051faa2fb28d147b34070a6bce25eaf1ee6bb4ca3b47af5ee6148d50079154")
          .andReply(new TestMockResponseProvider(200, singletonMap("Docker-Content-Digest", "sha256:c7051faa2fb28d147b34070a6bce25eaf1ee6bb4ca3b47af5ee6148d50079154"), null))
          .once();
      mockServer.expect().put()
          .withPath("/v2/test-chart/manifests/0.0.1")
          .andReply(new TestMockResponseProvider(200, singletonMap("Docker-Content-Digest", "manifestdigest"), null))
          .once();
      helmService.uploadHelmChart(helmConfig);
      assertThat(mockServer.getLastRequest().getBody().readUtf8())
          .isEqualTo("{\r\n" +
            "  \"schemaVersion\" : 2,\r\n" +
            "  \"config\" : {\r\n" +
            "    \"mediaType\" : \"application/vnd.cncf.helm.config.v1+json\",\r\n" +
            "    \"digest\" : \"sha256:46a607aed71245e2489a79ad8b401b269565de36f3aa7eadf6f7e368a8938a43\",\r\n" +
            "    \"size\" : 77\r\n" +
            "  },\r\n" +
            "  \"layers\" : [ {\r\n" +
            "    \"mediaType\" : \"application/vnd.cncf.helm.chart.content.v1.tar+gzip\",\r\n" +
            "    \"digest\" : \"sha256:c7051faa2fb28d147b34070a6bce25eaf1ee6bb4ca3b47af5ee6148d50079154\",\r\n" +
            "    \"size\" : 24\r\n" +
            "  } ]\r\n" +
            "}");
    }
  }

  private void expect(HttpMethod method, String path, ServerResponse response) {
    responses.computeIfAbsent(
        new SimpleRequest(method, path),
        k -> new ArrayDeque<>())
      .add(response);
  }

  private static final class NeedsAuthorizationResponse implements ServerResponse {
    @Override
    public boolean isRepeatable() {
      // always()
      return true;
    }

    @Override
    public MockResponse toMockResponse(RecordedRequest recordedRequest) {
      if (recordedRequest.getHeader("Authorization") != null) {
        return new MockResponse()
          .setResponseCode(201).setBody("Upload successful");
      } else {
        return new MockResponse()
          .setResponseCode(401)
          .setHeader("WWW-Authenticate", "Basic")
          .setBody("Unauthorized");
      }
    }
  }
}
