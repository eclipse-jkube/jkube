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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibServiceUtil;

import com.google.cloud.tools.jib.api.Credential;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JibBuildServiceTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private KitLogger mockedLogger;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JKubeServiceHub mockedServiceHub;

    private ImageConfiguration imageConfiguration;

    private RegistryConfig registryConfig;

    private MockedStatic<JibServiceUtil> jibServiceUtilMockedStatic;

    @Before
    public void setUp() {
        when(mockedServiceHub.getLog()).thenReturn(mockedLogger);
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
        jibServiceUtilMockedStatic.when(() -> JibServiceUtil.getBaseImage(imageConfiguration))
            .thenReturn("busybox");
    }

    @After
    public void close() {
        jibServiceUtilMockedStatic.close();
    }

    @Test
    public void isApplicable_withNoBuildStrategy_shouldReturnFalse() {
        // When
        final boolean result = new JibBuildService(mockedServiceHub).isApplicable();
        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void isApplicable_withJibBuildStrategy_shouldReturnTrue() {
        // Given
        when(mockedServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
        // When
        final boolean result = new JibBuildService(mockedServiceHub).isApplicable();
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    public void testGetRegistryCredentialsForPush() throws IOException {
        try (MockedConstruction<AuthConfigFactory> authConfigFactoryMockedConstruction = mockAuthConfig(true)) {
            // When
            Credential credential = JibBuildService.getRegistryCredentials(
                registryConfig, true, imageConfiguration, mockedLogger);
            // Then
            assertNotNull(credential);
            assertEquals("testuserpush", credential.getUsername());
            assertEquals("testpass", credential.getPassword());
        }
    }

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    public void testGetRegistryCredentialsForPull() throws IOException {
        try (MockedConstruction<AuthConfigFactory> authConfigFactoryMockedConstruction = mockAuthConfig(false)) {
            // When
            Credential credential = JibBuildService.getRegistryCredentials(
                registryConfig, false, imageConfiguration, mockedLogger);
            // Then
            assertNotNull(credential);
            assertEquals("testuserpull", credential.getUsername());
            assertEquals("testpass", credential.getPassword());
        }
    }

    @Test
    public void testGetBuildTarArchive() throws IOException {
        // Given
        File projectBaseDir = Files.createTempDirectory("test").toFile();
        // When
        File tarArchive = JibBuildService.getBuildTarArchive(imageConfiguration, createJKubeConfiguration(projectBaseDir));
        // Then
        assertThat(tarArchive)
            .isNotNull()
            .isEqualTo(projectBaseDir.toPath().resolve(
                FileSystems.getDefault().getPath("target", "test", "testimage", "0.0.1", "tmp", "docker-build.tar")
            ).toFile());
    }


    @Test
    public void testGetAssemblyTarArchive() throws IOException {
        // Given
        File projectBaseDir = Files.createTempDirectory("test").toFile();
        // When
        File tarArchive = JibBuildService.getAssemblyTarArchive(imageConfiguration, createJKubeConfiguration(projectBaseDir), mockedLogger);
        // Then
        assertThat(tarArchive)
            .isNotNull()
            .isEqualTo(projectBaseDir.toPath().resolve(
                FileSystems.getDefault().getPath("target", "test", "testimage", "0.0.1", "tmp", "docker-build.tar")
            ).toFile());
    }

    @Test
    public void testPrependRegistry() {
        // When
        JibBuildService.prependRegistry(imageConfiguration, "quay.io");
        // Then
        assertNotNull(imageConfiguration);
        assertEquals("quay.io/test/testimage:0.0.1", imageConfiguration.getName());
    }

    @Test
    public void testPushWithNoConfigurations() throws Exception {
        // When
        new JibBuildService(mockedServiceHub).push(Collections.emptyList(), 1, null, false);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.jibPush(any(), any(), any(), eq(mockedLogger)), times(0));
    }

    @Test
    public void testPushWithConfiguration() throws Exception {
        try (MockedConstruction<AuthConfigFactory> authConfigFactoryMockedConstruction = mockAuthConfig(true)) {
            // When
            new JibBuildService(mockedServiceHub).push(Collections.singletonList(imageConfiguration), 1, registryConfig, false);
            // Then
            jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.jibPush(eq(imageConfiguration), eq(Credential.from("testuserpush", "testpass")), any(), eq(mockedLogger)), times(1));
        }
    }

    @Test
    public void push_withImageBuildConfigurationSkipTrue_shouldNotPushImage() throws JKubeServiceException {
        // Given
        imageConfiguration = ImageConfiguration.builder()
            .name("test/foo:latest")
            .build(BuildConfiguration.builder()
                .from("test/base:latest")
                .skip(true)
                .build())
            .build();
        // When
        new JibBuildService(mockedServiceHub).push(Collections.singletonList(imageConfiguration), 1, registryConfig, false);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.jibPush(any(), any(), any(), any()), times(0));
    }

    @Test
    public void build_withImageMissingBuildConfiguration_shouldNotBuildImage() throws JKubeServiceException {
        // Given
        imageConfiguration = ImageConfiguration.builder()
            .name("test/foo:latest")
            .build();
        // When
        new JibBuildService(mockedServiceHub).build(imageConfiguration);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.buildContainer(any(), any(), any()), times(0));
    }

    @Test
    public void build_withImageBuildConfigurationSkipTrue_shouldNotBuildImage() throws JKubeServiceException {
        // Given
        imageConfiguration = ImageConfiguration.builder()
            .name("test/foo:latest")
            .build(BuildConfiguration.builder()
                .from("test/base:latest")
                .skip(true)
                .build())
            .build();
        // When
        new JibBuildService(mockedServiceHub).build(imageConfiguration);
        // Then
        jibServiceUtilMockedStatic.verify(() -> JibServiceUtil.buildContainer(any(), any(), any()), times(0));
    }

    @Test
    public void build_shouldCallPluginServiceAddFiles() throws JKubeServiceException {
        // Given
        imageConfiguration = ImageConfiguration.builder()
          .name("test/foo:latest")
          .build();
        // When
        new JibBuildService(mockedServiceHub).build(imageConfiguration);
        // Then
        verify(mockedServiceHub.getPluginManager().resolvePluginService(), times(1)).addExtraFiles();
    }

    private static JKubeConfiguration createJKubeConfiguration(File projectBaseDir) {
        return JKubeConfiguration.builder()
            .outputDirectory("target")
            .project(JavaProject.builder()
                .baseDirectory(projectBaseDir)
                .build())
            .build();
    }

    private MockedConstruction<AuthConfigFactory> mockAuthConfig(boolean isPush) {
        return mockConstruction(AuthConfigFactory.class, (mock, ctx) -> {
            when(mock.createAuthConfig(anyBoolean(), anyBoolean(), any(), anyList(), isNull(), anyString(), any()))
                .thenReturn(AuthConfig.builder()
                    .username("testuser" + (isPush ? "push" : "pull"))
                    .password("testpass")
                    .build());
        });
    }
}
