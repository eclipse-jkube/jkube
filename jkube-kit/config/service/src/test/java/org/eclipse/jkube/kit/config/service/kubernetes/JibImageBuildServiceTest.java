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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.global.JibSystemProperties;
import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.NativeLibrary;
import com.marcnuri.helm.jni.RepoServerOptions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibServiceUtil;

import com.google.cloud.tools.jib.api.Credential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JibImageBuildServiceTest {

    private static HelmLib helmLib;
    @TempDir
    private Path temporaryFolder;

    private KitLogger logger;

    private JKubeServiceHub jKubeServiceHub;

    private ImageConfiguration imageConfiguration;

    @BeforeAll
    static void setUpAll() {
        helmLib = NativeLibrary.getInstance().load();
    }

    @BeforeEach
    void setUp() {
        logger = spy(new KitLogger.SilentLogger());
        jKubeServiceHub = JKubeServiceHub.builder()
          .log(logger)
          .platformMode(RuntimeMode.KUBERNETES)
          .buildServiceConfig(BuildServiceConfig.builder().build())
          .configuration(JKubeConfiguration.builder()
            .pullRegistryConfig(RegistryConfig.builder().build())
            .pushRegistryConfig(RegistryConfig.builder().build())
            .project(JavaProject.builder().baseDirectory(temporaryFolder.toFile()).build())
            .build())
          .build();
        imageConfiguration = ImageConfiguration.builder()
            .name("test/test-image:0.0.1")
            .build(BuildConfiguration.builder()
                .from("busybox")
                .build())
            .build();
    }

    @Test
    void isApplicable_withNoBuildStrategy_shouldReturnFalse() {
        // When
        final boolean result = new JibImageBuildService(jKubeServiceHub).isApplicable();
        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isApplicable_withJibBuildStrategy_shouldReturnTrue() {
        // Given
        jKubeServiceHub = jKubeServiceHub.toBuilder()
          .buildServiceConfig(BuildServiceConfig.builder()
              .jKubeBuildStrategy(JKubeBuildStrategy.jib)
              .build())
          .build();
        // When
        final boolean result = new JibImageBuildService(jKubeServiceHub).isApplicable();
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    void getRegistryCredentialsForPush() throws IOException {
        // Given
        final RegistryConfig registryConfig = RegistryConfig.builder()
          .settings(Collections.singletonList(
            RegistryServerConfiguration.builder()
              .id("test.example.org")
              .username("testuserpush")
              .password("testpass")
              .build()))
          .passwordDecryptionMethod(s -> s)
          .build();
        // When
        Credential credential = new JibImageBuildService(jKubeServiceHub)
          .getRegistryCredentials(registryConfig, true, "test.example.org");
        // Then
        assertThat(credential).isNotNull()
            .hasFieldOrPropertyWithValue("username", "testuserpush")
            .hasFieldOrPropertyWithValue("password", "testpass");
    }

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    void getRegistryCredentialsForPull() throws IOException {
        // Given
        final RegistryConfig registryConfig = RegistryConfig.builder()
          .settings(Collections.singletonList(
            RegistryServerConfiguration.builder()
              .id("test.example.org")
              .username("testuserpull")
              .password("testpass")
              .build()))
          .passwordDecryptionMethod(s -> s)
          .build();
        // When
        Credential credential = new JibImageBuildService(jKubeServiceHub)
          .getRegistryCredentials(registryConfig, false, "test.example.org");
        // Then
        assertThat(credential).isNotNull()
            .hasFieldOrPropertyWithValue("username", "testuserpull")
            .hasFieldOrPropertyWithValue("password", "testpass");
    }

    @Test
    void getBuildTarArchive() throws IOException {
        // Given
        File projectBaseDir = Files.createDirectory(temporaryFolder.resolve("test")).toFile();
        // When
        File tarArchive = JibImageBuildService.getBuildTarArchive(imageConfiguration, createJKubeConfiguration(projectBaseDir));
        // Then
        assertThat(tarArchive).isNotNull()
            .isEqualTo(projectBaseDir.toPath().resolve("target").resolve("test").resolve("test-image").resolve("0.0.1")
                .resolve("tmp").resolve("docker-build.tar").toFile());
    }


    @Test
    void getAssemblyTarArchive() throws IOException {
        // Given
        File projectBaseDir = Files.createDirectory(temporaryFolder.resolve("test")).toFile();
        // When
        File tarArchive = JibImageBuildService.getAssemblyTarArchive(imageConfiguration, createJKubeConfiguration(projectBaseDir), logger);
        // Then
        assertThat(tarArchive).isNotNull()
            .isEqualTo(projectBaseDir.toPath().resolve("target").resolve("test").resolve("test-image").resolve("0.0.1")
                .resolve("tmp").resolve("docker-build.tar").toFile());
    }

    @Nested
    @DisplayName("push")
    class Push {

        private boolean sendCredentialsOverHttp;
        private String remoteOciServer;

        @BeforeEach
        void setUp() throws Exception {
            sendCredentialsOverHttp = JibSystemProperties.sendCredentialsOverHttp();
            System.setProperty(JibSystemProperties.SEND_CREDENTIALS_OVER_HTTP, "true");

            // Setup OCI server
            remoteOciServer = helmLib.RepoOciServerStart(
              new RepoServerOptions(null, "oci-user", "oci-password")
            ).out;

            // Configure OCI server in image and JKube
            imageConfiguration = imageConfiguration.toBuilder()
              .registry(remoteOciServer)
              .build();
            jKubeServiceHub = jKubeServiceHub.toBuilder()
              .configuration(jKubeServiceHub.getConfiguration().toBuilder()
                .pushRegistryConfig(RegistryConfig.builder()
                  .registry(remoteOciServer)
                  .settings(Collections.singletonList(
                    RegistryServerConfiguration.builder()
                      .id(remoteOciServer)
                      .username("oci-user")
                      .password("oci-password")
                      .build()))
                  .passwordDecryptionMethod(s -> s)
                  .build())
                .build())
              .build();

            // Build fake container image
            final Path tarPath = temporaryFolder
              .resolve("localhost")
              .resolve(remoteOciServer.split(":")[1])
              .resolve("test")
              .resolve("test-image")
              .resolve("0.0.1")
              .resolve("tmp")
              .resolve("docker-build.tar");
            Jib.fromScratch()
              .setFormat(ImageFormat.Docker)
              .containerize(Containerizer.to(TarImage
                .at(tarPath)
                .named(imageConfiguration.getName()))
              );
        }

        @AfterEach
        void tearDown() {
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
              .hasMessageContaining("Error when push JIB image")
              .cause()
              .isInstanceOf(JKubeException.class)
              .hasMessageStartingWith("Unable to containerize image using Jib: Unauthorized for localhost:");
        }

        @Nested
        @DisplayName("withValidConfiguration")
        class ValidConfiguration {

            @BeforeEach
            void pushValidImage() throws Exception {
                new JibImageBuildService(jKubeServiceHub)
                  .push(Collections.singletonList(imageConfiguration), 1, false);
            }

            @Test
            void logsImagePush() {
                verify(logger, times(1))
                  .info("Pushing image: %s", remoteOciServer + "/test/test-image:0.0.1");
            }

            @Test
            void pushesImage() throws Exception {
                final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + remoteOciServer + "/v2/test/test-image/tags/list")
                  .openConnection();
                connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String("oci-user:oci-password".getBytes()));
                connection.connect();
                assertThat(connection.getResponseCode()).isEqualTo(200);
                assertThat(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8))
                  .contains("{\"name\":\"test/test-image\",\"tags\":[\"0.0.1\"]}");
            }
        }

    }

    @Nested
    @DisplayName("build")
    class Build {

        private MockedStatic<JibServiceUtil> jibServiceUtilMockedStatic;

        @BeforeEach
        void setUp() {
            jibServiceUtilMockedStatic = mockStatic(JibServiceUtil.class);
            jibServiceUtilMockedStatic.when(() -> JibServiceUtil.getBaseImage(argThat(ic -> ic.getBuild().getFrom().equals("busybox")), isNull()))
              .thenReturn("busybox");
            jibServiceUtilMockedStatic.when(() -> JibServiceUtil.containerFromImageConfiguration(any(), any(), any()))
              .thenReturn(mock(JibContainerBuilder.class, RETURNS_DEEP_STUBS));
        }

        @AfterEach
        void close() {
            jibServiceUtilMockedStatic.close();
        }

        @Test
        void build_withImageMissingBuildConfiguration_shouldNotBuildImage() throws JKubeServiceException {
            // Given
            imageConfiguration = ImageConfiguration.builder()
              .name("test/foo:latest")
              .build();
            // When
            new JibImageBuildService(jKubeServiceHub).build(imageConfiguration);
            // Then
            jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.buildContainer(any(), any(), any()), times(0));
        }

        @Test
        void build_withImageBuildConfigurationSkipTrue_shouldNotBuildImage() throws JKubeServiceException {
            // Given
            imageConfiguration = ImageConfiguration.builder()
              .name("test/foo:latest")
              .build(BuildConfiguration.builder()
                .from("test/base:latest")
                .skip(true)
                .build())
              .build();
            // When
            new JibImageBuildService(jKubeServiceHub).build(imageConfiguration);
            // Then
            jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.buildContainer(any(), any(), any()), times(0));
        }

        @Test
        void build_shouldCallPluginServiceAddFiles() throws JKubeServiceException {
            // Given
            imageConfiguration = ImageConfiguration.builder()
              .name("test/foo:latest")
              .build();
            // When
            new JibImageBuildService(jKubeServiceHub).build(imageConfiguration);
            // Then
            verify(logger, atLeastOnce()).debug(eq("Adding extra files for plugin %s"), anyString());
        }

        @Test
        void build_withRegistryConfig_shouldPrependRegistryToImageName() throws JKubeServiceException {
            // Given
            jKubeServiceHub = jKubeServiceHub.toBuilder()
              .configuration(jKubeServiceHub.getConfiguration().toBuilder()
                .pullRegistryConfig(RegistryConfig.builder().registry("quay.io").settings(Collections.emptyList()).build())
                .build())
              .build();
            // When
            new JibImageBuildService(jKubeServiceHub).build(imageConfiguration);
            // Then
            jibServiceUtilMockedStatic.verify(() -> JibServiceUtil
              .containerFromImageConfiguration(argThat(ic -> ic.getName().equals("quay.io/test/test-image:0.0.1")), any(), any()), times(1));
        }

    }

    private static JKubeConfiguration createJKubeConfiguration(File projectBaseDir) {
        return JKubeConfiguration.builder()
            .outputDirectory("target")
            .project(JavaProject.builder()
                .baseDirectory(projectBaseDir)
                .build())
            .build();
    }
}
