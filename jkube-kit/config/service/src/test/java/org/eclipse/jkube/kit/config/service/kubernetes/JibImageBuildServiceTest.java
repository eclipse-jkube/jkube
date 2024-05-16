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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import com.google.cloud.tools.jib.api.JibContainerBuilder;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibServiceUtil;

import com.google.cloud.tools.jib.api.Credential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JibImageBuildServiceTest {

    @TempDir
    Path temporaryFolder;

    private KitLogger logger;

    private JKubeServiceHub mockedServiceHub;

    private ImageConfiguration imageConfiguration;

    private RegistryConfig registryConfig;

    private MockedStatic<JibServiceUtil> jibServiceUtilMockedStatic;

    @BeforeEach
    void setUp() {
        logger = spy(new KitLogger.SilentLogger());
        mockedServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
        when(mockedServiceHub.getLog()).thenReturn(logger);
        jibServiceUtilMockedStatic = mockStatic(JibServiceUtil.class);
        imageConfiguration = ImageConfiguration.builder()
            .name("test/testimage:0.0.1")
            .build(BuildConfiguration.builder()
                .from("busybox")
                .build())
            .build();
        registryConfig = RegistryConfig.builder()
            .authConfig(Collections.emptyMap())
            .settings(Collections.emptyList())
            .build();
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
    void isApplicable_withNoBuildStrategy_shouldReturnFalse() {
        // When
        final boolean result = new JibImageBuildService(mockedServiceHub).isApplicable();
        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isApplicable_withJibBuildStrategy_shouldReturnTrue() {
        // Given
        when(mockedServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
        // When
        final boolean result = new JibImageBuildService(mockedServiceHub).isApplicable();
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    void getRegistryCredentialsForPush() throws IOException {
        // Given
        registryConfig = RegistryConfig.builder()
            .settings(Collections.singletonList(createNewRegistryServerConfiguration("test.example.org", "testuserpush", "testpass")))
            .passwordDecryptionMethod(s -> s)
            .build();
        // When
        Credential credential = new JibImageBuildService(mockedServiceHub)
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
        registryConfig = RegistryConfig.builder()
            .settings(Collections.singletonList(createNewRegistryServerConfiguration("test.example.org", "testuserpull", "testpass")))
            .passwordDecryptionMethod(s -> s)
            .build();
        // When
        Credential credential = new JibImageBuildService(mockedServiceHub)
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
            .isEqualTo(projectBaseDir.toPath().resolve("target").resolve("test").resolve("testimage").resolve("0.0.1")
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
            .isEqualTo(projectBaseDir.toPath().resolve("target").resolve("test").resolve("testimage").resolve("0.0.1")
                .resolve("tmp").resolve("docker-build.tar").toFile());
    }

    @Test
    void pushWithNoConfigurations() throws Exception {
        // When
        new JibImageBuildService(mockedServiceHub).push(Collections.emptyList(), 1, null, false);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.jibPush(any(), any(), any(), any()), times(0));
    }

    @Test
    void pushWithConfiguration() throws Exception {
        // Given
        registryConfig = RegistryConfig.builder()
            .settings(Collections.singletonList(createNewRegistryServerConfiguration("docker.io", "testuserpush", "testpass")))
            .passwordDecryptionMethod(s -> s)
            .build();
        // When
        new JibImageBuildService(mockedServiceHub).push(Collections.singletonList(imageConfiguration), 1, registryConfig, false);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.jibPush(eq(imageConfiguration), eq(Credential.from("testuserpush", "testpass")), any(), any()), times(1));
    }

    @Test
    void push_withImageBuildConfigurationSkipTrue_shouldNotPushImage() throws JKubeServiceException {
        // Given
        imageConfiguration = ImageConfiguration.builder()
            .name("test/foo:latest")
            .build(BuildConfiguration.builder()
                .from("test/base:latest")
                .skip(true)
                .build())
            .build();
        // When
        new JibImageBuildService(mockedServiceHub).push(Collections.singletonList(imageConfiguration), 1, registryConfig, false);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.jibPush(any(), any(), any(), any()), times(0));
    }

    @Test
    void build_withImageMissingBuildConfiguration_shouldNotBuildImage() throws JKubeServiceException {
        // Given
        imageConfiguration = ImageConfiguration.builder()
            .name("test/foo:latest")
            .build();
        // When
        new JibImageBuildService(mockedServiceHub).build(imageConfiguration);
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
        new JibImageBuildService(mockedServiceHub).build(imageConfiguration);
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
        new JibImageBuildService(mockedServiceHub).build(imageConfiguration);
        // Then
        verify(mockedServiceHub.getPluginManager().resolvePluginService(), times(1)).addExtraFiles();
    }

    @Test
    void build_withRegistryConfig_shouldPrependRegistryToImageName() throws JKubeServiceException {
        // Given
        when(mockedServiceHub.getConfiguration().getRegistryConfig())
          .thenReturn(RegistryConfig.builder().registry("quay.io").settings(Collections.emptyList()).build());
        when(mockedServiceHub.getConfiguration().getProject())
          .thenReturn(JavaProject.builder().baseDirectory(temporaryFolder.toFile()).build());
        // When
        new JibImageBuildService(mockedServiceHub).build(imageConfiguration);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil
          .containerFromImageConfiguration(argThat(ic -> ic.getName().equals("quay.io/test/testimage:0.0.1")), any(), any()), times(1));
    }

    private static JKubeConfiguration createJKubeConfiguration(File projectBaseDir) {
        return JKubeConfiguration.builder()
            .outputDirectory("target")
            .project(JavaProject.builder()
                .baseDirectory(projectBaseDir)
                .build())
            .build();
    }

    private RegistryServerConfiguration createNewRegistryServerConfiguration(String id, String username, String password) {
        return RegistryServerConfiguration.builder()
            .id(id)
            .username(username)
            .password(password)
            .build();
    }
}
