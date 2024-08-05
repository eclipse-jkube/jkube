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
package org.eclipse.jkube.kit.service.jib;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.RegistryUnauthorizedException;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.global.JibSystemProperties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.TestOciServer;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JibServiceTest {

  @TempDir
  private Path tempDir;
  private TestOciServer remoteOciServer;
  private JibLogger jibLogger;
  private TestAuthConfigFactory testAuthConfigFactory;
  private JKubeConfiguration configuration;
  private ImageConfiguration imageConfiguration;

  @BeforeEach
  void setUp() {
    remoteOciServer = new TestOciServer();
    jibLogger = new JibLogger(new KitLogger.SilentLogger());
    testAuthConfigFactory = new TestAuthConfigFactory();
    configuration = JKubeConfiguration.builder()
      .pullRegistryConfig(RegistryConfig.builder().build())
      .pushRegistryConfig(RegistryConfig.builder()
        .registry(remoteOciServer.getUrl())
        .settings(Collections.singletonList(RegistryServerConfiguration.builder()
          .id(remoteOciServer.getUrl())
          .username(remoteOciServer.getUser())
          .password(remoteOciServer.getPassword())
          .build()))
        .build())
      .project(JavaProject.builder()
        .baseDirectory(tempDir.toFile())
        .build())
      .build();
    imageConfiguration = ImageConfiguration.builder()
      .name(remoteOciServer.getUrl() + "/" + "the-image-name")
      .build(BuildConfiguration.builder()
        .from("scratch")
        .build())
      .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    remoteOciServer.close();
  }

  @Test
  void prependsRegistryWhenNotConfiguredInName() throws Exception {
    configuration = configuration.toBuilder()
      .pushRegistryConfig(RegistryConfig.builder()
        .registry("prepend.example.com")
        .build())
      .build();
    imageConfiguration = ImageConfiguration.builder().name("the-image-name").build();
    try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
      assertThat(jibService.getImageName().getFullName()).isEqualTo("prepend.example.com/the-image-name:latest");
    }
  }

  @Nested
  @DisplayName("build")
  class Build {

    @Test
    void build() throws Exception {
      try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
        final List<File> containerImageTarFiles = jibService.build();
        assertThat(containerImageTarFiles)
          .singleElement()
          .returns("jib-image.linux-amd64.tar", File::getName)
          .satisfies(jibContainerImageTar -> ArchiveAssertions.assertThat(jibContainerImageTar)
            .fileTree()
            .contains("manifest.json", "config.json"));
      }
    }

    @Test
    void buildMultiplePlatforms() throws Exception {
      imageConfiguration = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder()
          .platform("linux/amd64")
          .platform("linux/arm64")
          .platform("linux/arm")
          .build())
        .build();
      try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
        final List<File> containerImageTarFiles = jibService.build();
        assertThat(containerImageTarFiles)
          .hasSize(3)
          .allSatisfy(jibContainerImageTar -> ArchiveAssertions.assertThat(jibContainerImageTar)
            .fileTree()
            .contains("manifest.json", "config.json"))
          .extracting(File::getName)
          .contains("jib-image.linux-amd64.tar", "jib-image.linux-arm64.tar", "jib-image.linux-arm.tar");
      }

    }

  }

  @Nested
  @DisplayName("push")
  class Push {

    private boolean sendCredentialsOverHttp;

    @BeforeEach
    void setUp() throws Exception {
      sendCredentialsOverHttp = JibSystemProperties.sendCredentialsOverHttp();
      System.setProperty(JibSystemProperties.SEND_CREDENTIALS_OVER_HTTP, "true");

      final BuildDirs buildDirs = new BuildDirs(imageConfiguration.getName(), configuration);
      Jib.fromScratch()
        .setFormat(ImageFormat.Docker)
        .containerize(Containerizer.to(TarImage
          .at(buildDirs.getTemporaryRootDirectory().toPath().resolve("jib-image.linux-amd64.tar"))
          .named(imageConfiguration.getName()))
        );
    }

    @AfterEach
    void tearDown() {
      if (!sendCredentialsOverHttp) {
        System.clearProperty(JibSystemProperties.SEND_CREDENTIALS_OVER_HTTP);
      }
    }

    @SuppressWarnings("resource")
    @Test
    void emptyImageNameThrowsException() {
      final ImageConfiguration emptyImageConfiguration = ImageConfiguration.builder().build();
      assertThatThrownBy(() -> new JibService(jibLogger, testAuthConfigFactory, configuration, emptyImageConfiguration))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Image name must not be null");
    }

    @Test
    void pushInvalidCredentials() throws Exception {
      configuration = configuration.toBuilder()
        .pushRegistryConfig(RegistryConfig.builder().build())
        .build();
      try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
        assertThatThrownBy(jibService::push)
          .isInstanceOf(JKubeException.class)
          .hasMessageContaining("Unable to containerize image using Jib: Unauthorized for")
          .cause()
          .isInstanceOf(RegistryUnauthorizedException.class);
      }
    }

    @Test
    void push() throws Exception {
      try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
        jibService.push();
      }
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer.getUrl() + "/v2/the-image-name/tags/list")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
      connection.connect();
      assertThat(connection.getResponseCode()).isEqualTo(200);
      assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
        .contains("{\"name\":\"the-image-name\",\"tags\":[\"latest\"]}");
    }

    @Test
    void pushAdditionalTags() throws Exception {
      imageConfiguration = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder()
          .tag("1.0")
          .tag("1.0.0")
          .build())
        .build();
      try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
        jibService.push();
      }
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer.getUrl() + "/v2/the-image-name/tags/list")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
      connection.connect();
      assertThat(connection.getResponseCode()).isEqualTo(200);
      assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
        .contains("{\"name\":\"the-image-name\",\"tags\":[\"1.0\",\"1.0.0\",\"latest\"]}");
    }

    @Test
    void pushMultiplatform() throws Exception {
      imageConfiguration = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder()
          .platform("linux/amd64")
          .platform("linux/arm64")
          .platform("linux/arm")
          .build())
        .build();
      try (JibService jibService = new JibService(jibLogger, testAuthConfigFactory, configuration, imageConfiguration)) {
        jibService.push();
      }
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer.getUrl() + "/v2/the-image-name/manifests/latest")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
      connection.setRequestProperty("Accept", "application/vnd.docker.distribution.manifest.list.v2+json");
      connection.connect();
      assertThat(connection.getResponseCode()).isEqualTo(200);
      assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
        .contains(":{\"architecture\":\"amd64\",\"os\":\"linux\"}}")
        .contains(":{\"architecture\":\"arm64\",\"os\":\"linux\"}}")
        .contains(":{\"architecture\":\"arm\",\"os\":\"linux\"}}");
    }
  }

}
