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

import com.google.cloud.tools.jib.api.Credential;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RegistryConfig;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibServiceUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class JibBuildServiceTest {
    @Mocked
    private KitLogger logger;

    @Mocked
    private JKubeServiceHub serviceHub;

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    public void testGetRegistryCredentialsForPush() throws IOException {
        // Given
        ImageConfiguration imageConfiguration = getImageConfiguration();
        mockAuthConfig();
        RegistryConfig registryConfig = RegistryConfig.builder()
                .authConfig(Collections.emptyMap())
                .settings(Collections.emptyList())
                .build();
        // When
        Credential credential = JibBuildService.getRegistryCredentials(
            registryConfig, true, imageConfiguration, logger);
        // Then
        assertNotNull(credential);
        assertEquals("testuserpush", credential.getUsername());
        assertEquals("testpass", credential.getPassword());
    }

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    public void testGetRegistryCredentialsForPull() throws IOException {
        // Given
        ImageConfiguration imageConfiguration = getImageConfiguration();
        mockAuthConfig();
        RegistryConfig registryConfig = RegistryConfig.builder()
            .authConfig(Collections.emptyMap())
            .settings(Collections.emptyList())
            .build();
        // When
        Credential credential = JibBuildService.getRegistryCredentials(
            registryConfig, false, imageConfiguration, logger);
        // Then
        assertNotNull(credential);
        assertEquals("testuserpull", credential.getUsername());
        assertEquals("testpass", credential.getPassword());
    }

    @Test
    public void testGetBuildTarArchive() throws IOException {
        // Given
        File projectBaseDir = Files.createTempDirectory("test").toFile();
        ImageConfiguration imageConfiguration = getImageConfiguration();
        setupServiceHubExpectations(projectBaseDir);
        // When
        File tarArchive = JibBuildService.getBuildTarArchive(imageConfiguration, serviceHub);
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
        ImageConfiguration imageConfiguration = getImageConfiguration();
        setupServiceHubExpectations(projectBaseDir);
        // When
        File tarArchive = JibBuildService.getAssemblyTarArchive(imageConfiguration, serviceHub.getConfiguration(), logger);
        // Then
        assertThat(tarArchive)
          .isNotNull()
          .isEqualTo(projectBaseDir.toPath().resolve(
            FileSystems.getDefault().getPath("target", "test", "testimage", "0.0.1", "tmp", "docker-build.tar")
          ).toFile());
    }

    @Test
    public void testPrependRegistry() {
        // Given
        ImageConfiguration imageConfiguration = getImageConfiguration();
        // When
        JibBuildService.prependRegistry(imageConfiguration, "quay.io");
        // Then
        assertNotNull(imageConfiguration);
        assertEquals("quay.io/test/testimage:0.0.1", imageConfiguration.getName());
    }

    @Test
    public void testPushWithNoConfigurations(@Mocked JibServiceUtil jibServiceUtil) throws Exception {
        // When
        new JibBuildService(serviceHub, logger).push(Collections.emptyList(), 1, null, false);
        // Then
        // @formatter:off
        new Verifications() {{
            JibServiceUtil.jibPush((ImageConfiguration)any, (Credential)any, (File)any, logger); times = 0;
        }};
        // @formatter:on
    }

    @Test
    public void testPushWithConfiguration(@Mocked JibServiceUtil jibServiceUtil) throws Exception {
        // Given
        mockAuthConfig();
        final ImageConfiguration imageConfiguration = getImageConfiguration();
        final RegistryConfig registryConfig = RegistryConfig.builder()
            .build();
        // When
        new JibBuildService(serviceHub, logger).push(Collections.singletonList(imageConfiguration), 1, registryConfig, false);
        // Then
        // @formatter:off
        new Verifications() {{
            JibServiceUtil.jibPush(
                imageConfiguration,
                Credential.from("testuserpush", "testpass"),
                (File)any,
                logger);
            times = 1;
        }};
        // @formatter:on
    }

    private ImageConfiguration getImageConfiguration() {
        return ImageConfiguration.builder()
                .name("test/testimage:0.0.1")
                .build(BuildConfiguration.builder().from("busybox").build())
                .build();
    }

    @java.lang.SuppressWarnings("squid:S00112")
    private void setupServiceHubExpectations(File projectBaseDir) {
        JKubeConfiguration jKubeConfiguration = JKubeConfiguration.builder()
                .outputDirectory("target")
                .project(JavaProject.builder()
                        .baseDirectory(projectBaseDir)
                        .build())
                .build();
        new Expectations() {{
            serviceHub.getConfiguration();
            result = jKubeConfiguration;
        }};
    }


    private static void mockAuthConfig() {
        new MockUp<AuthConfigFactory>() {
            @Mock
            AuthConfig createAuthConfig(boolean isPush, boolean skipExtendedAuth, Map authConfig, List<RegistryServerConfiguration> settings, String user, String registry, UnaryOperator<String> passwordDecryptionMethod)
                throws IOException {
                return AuthConfig.builder()
                    .username("testuser" + (isPush ? "push" : "pull"))
                    .password("testpass")
                    .build();
            }
        };
    }
}
