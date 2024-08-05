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
package org.eclipse.jkube.kit.config.service.kubernetes;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.global.JibSystemProperties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.TestOciServer;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JibImageBuildServicePushTest {

  @TempDir
  private Path temporaryFolder;

  private boolean sendCredentialsOverHttp;
  private TestOciServer remoteOciServer;

  private KitLogger logger;
  private JKubeServiceHub jKubeServiceHub;
  private ImageConfiguration imageConfiguration;

  @BeforeEach
  void setUp() throws Exception {
    sendCredentialsOverHttp = JibSystemProperties.sendCredentialsOverHttp();
    System.setProperty(JibSystemProperties.SEND_CREDENTIALS_OVER_HTTP, "true");

    logger = spy(new KitLogger.SilentLogger());

    // Setup OCI server
    remoteOciServer = new TestOciServer();

    // Configure OCI server in image and JKube
    imageConfiguration = ImageConfiguration.builder()
      .name("test/test-image:0.0.1")
      .build(BuildConfiguration.builder()
        .from("scratch")
        .build())
      .registry(remoteOciServer.getUrl())
      .build();
    jKubeServiceHub = JKubeServiceHub.builder()
      .log(logger)
      .platformMode(RuntimeMode.KUBERNETES)
      .buildServiceConfig(BuildServiceConfig.builder().build())
      .configuration(JKubeConfiguration.builder()
        .project(JavaProject.builder().baseDirectory(temporaryFolder.toFile()).build())
        .pullRegistryConfig(RegistryConfig.builder().settings(Collections.emptyList()).build())
        .pushRegistryConfig(RegistryConfig.builder()
          .registry(remoteOciServer.getUrl())
          .settings(Collections.singletonList(
            RegistryServerConfiguration.builder()
              .id(remoteOciServer.getUrl())
              .username(remoteOciServer.getUser())
              .password(remoteOciServer.getPassword())
              .build()))
          .passwordDecryptionMethod(s -> s)
          .build())
        .build())
      .build();

    // Build fake container image
    final Path tarPath = temporaryFolder
      .resolve("localhost")
      .resolve(remoteOciServer.getUrl().split(":")[1])
      .resolve("test")
      .resolve("test-image")
      .resolve("0.0.1")
      .resolve("tmp")
      .resolve("jib-image.linux-amd64.tar");
    Jib.fromScratch()
      .setFormat(ImageFormat.Docker)
      .containerize(Containerizer.to(TarImage
        .at(tarPath)
        .named(imageConfiguration.getName()))
      );
  }

  @AfterEach
  void tearDown() throws Exception {
    remoteOciServer.close();
    if (!sendCredentialsOverHttp) {
      System.clearProperty(JibSystemProperties.SEND_CREDENTIALS_OVER_HTTP);
    }
  }

  @Test
  void pushWithNoConfigurations_doesNothing() throws Exception {
    // When
    new JibImageBuildService(jKubeServiceHub).push(Collections.emptyList(), 1, false);
    // Then
    verify(logger, never()).info(anyString(), anyString());
  }

  @Test
  void push_withImageBuildConfigurationSkipTrue_doesNothing() throws JKubeServiceException {
    // Given
    imageConfiguration = imageConfiguration.toBuilder()
      .build(imageConfiguration.getBuild().toBuilder()
        .skip(true)
        .build())
      .build();
    // When
    new JibImageBuildService(jKubeServiceHub).push(Collections.singletonList(imageConfiguration), 1, false);
    // Then
    verify(logger, never()).info(anyString(), anyString());
  }

  @Test
  void invalidCredentials_throwsException() {
    jKubeServiceHub = jKubeServiceHub.toBuilder()
      .configuration(jKubeServiceHub.getConfiguration().toBuilder()
        .pushRegistryConfig(RegistryConfig.builder()
          .settings(Collections.emptyList()).build())
        .build())
      .build();
    final JibImageBuildService jibImageBuildService = new JibImageBuildService(jKubeServiceHub);
    assertThatThrownBy(() -> jibImageBuildService.push(Collections.singletonList(imageConfiguration), 1, false))
      .isInstanceOf(JKubeServiceException.class)
      .hasMessageContaining("Error when pushing JIB image")
      .cause()
      .isInstanceOf(JKubeException.class)
      .hasMessageStartingWith("Unable to containerize image using Jib: Unauthorized for localhost:");
  }

  @Nested
  @DisplayName("withValidNoPlatformConfiguration")
  class ValidNoPlatformConfiguration {

    @BeforeEach
    void pushValidImage() throws Exception {
      new JibImageBuildService(jKubeServiceHub)
        .push(Collections.singletonList(imageConfiguration), 1, false);
    }

    @Test
    void logsImagePush() {
      verify(logger, times(1))
        .info("Pushing image: %s", remoteOciServer.getUrl() + "/test/test-image:0.0.1");
    }

    @Test
    void pushesImage() throws Exception {
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer.getUrl() + "/v2/test/test-image/tags/list")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
      connection.connect();
      assertThat(connection.getResponseCode()).isEqualTo(200);
      assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
        .contains("{\"name\":\"test/test-image\",\"tags\":[\"0.0.1\"]}");
    }
  }

  @Nested
  @DisplayName("withValidMultiplatformConfiguration")
  class ValidMultiplatformConfiguration {

    @BeforeEach
    void pushValidImage() throws Exception {
      imageConfiguration = imageConfiguration.toBuilder()
        .name("test/test-image:multiplatform")
        .build(imageConfiguration.getBuild().toBuilder()
          .platform("linux/amd64")
          .platform("linux/arm64")
          .platform("darwin/amd64")
          .build())
        .build();
      new JibImageBuildService(jKubeServiceHub)
        .push(Collections.singletonList(imageConfiguration), 1, false);
    }

    @Test
    void logsImagePush() {
      verify(logger, times(1))
        .info("Pushing image: %s", remoteOciServer.getUrl() + "/test/test-image:multiplatform");
    }

    @Test
    void pushesImage() throws Exception {
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer.getUrl() + "/v2/test/test-image/tags/list")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
      connection.connect();
      assertThat(connection.getResponseCode()).isEqualTo(200);
      assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
        .contains("{\"name\":\"test/test-image\",\"tags\":[\"multiplatform\"]}");
    }

    @Test
    void pushedImageManifestIsMultiplatform() throws Exception {
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer.getUrl() + "/v2/test/test-image/manifests/multiplatform")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
      connection.setRequestProperty("Accept", "application/vnd.docker.distribution.manifest.list.v2+json");
      connection.connect();
      assertThat(connection.getResponseCode()).isEqualTo(200);
      assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
        .contains(":{\"architecture\":\"amd64\",\"os\":\"linux\"}}")
        .contains(":{\"architecture\":\"arm64\",\"os\":\"linux\"}}")
        .contains(":{\"architecture\":\"amd64\",\"os\":\"darwin\"}}");
    }
  }

}
