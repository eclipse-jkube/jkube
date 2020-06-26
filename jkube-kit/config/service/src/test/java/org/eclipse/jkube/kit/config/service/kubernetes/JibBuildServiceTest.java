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
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class JibBuildServiceTest {
    @Mocked
    KitLogger logger;

    @Mocked
    JKubeServiceHub serviceHub;

    @Test
    @java.lang.SuppressWarnings("squid:S00112")
    public void testGetRegistryCredentials() throws IOException {
        // Given
        ImageConfiguration imageConfiguration = getImageConfiguration();
        new MockUp<AuthConfigFactory>() {
            @Mock
            AuthConfig createStandardAuthConfig(boolean isPush, Map authConfigMap, List<RegistryServerConfiguration> settings, String user, String registry, UnaryOperator<String> passwordDecryptionMethod, KitLogger log)
                    throws IOException {
                return AuthConfig.builder()
                        .username("testuser")
                        .password("testpass")
                        .build();
            }
        };
        RegistryConfig registryConfig = RegistryConfig.builder()
                .authConfig(Collections.emptyMap())
                .settings(Collections.emptyList())
                .build();

        // When
        Credential credential = JibBuildService.getRegistryCredentials(registryConfig, imageConfiguration, logger);

        // Then
        assertNotNull(credential);
        assertEquals("testuser", credential.getUsername());
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
        assertNotNull(tarArchive);
        assertEquals("/target/test/testimage/0.0.1/tmp/docker-build.tar", tarArchive.getAbsolutePath().substring(projectBaseDir.getAbsolutePath().length()));
    }


    @Test
    public void testGetAssemblyTarArchive() throws IOException {
        // Given
        File projectBaseDir = Files.createTempDirectory("test").toFile();
        ImageConfiguration imageConfiguration = getImageConfiguration();
        setupServiceHubExpectations(projectBaseDir);

        // When
        File tarArchive = JibBuildService.getAssemblyTarArchive(imageConfiguration, serviceHub, logger);

        // Then
        assertNotNull(tarArchive);
        assertEquals("/target/test/testimage/0.0.1/tmp/docker-build.tar", tarArchive.getAbsolutePath().substring(projectBaseDir.getAbsolutePath().length()));
    }

    @Test
    public void testAppendRegistry() {
        // Given
        ImageConfiguration imageConfiguration = getImageConfiguration();

        // When
        JibBuildService.appendRegistry(imageConfiguration, "quay.io");

        // Then
        assertNotNull(imageConfiguration);
        assertEquals("quay.io/test/testimage:0.0.1", imageConfiguration.getName());
    }

    private ImageConfiguration getImageConfiguration() {
        return ImageConfiguration.builder()
                .name("test/testimage:0.0.1")
                .build(BuildConfiguration.builder().build())
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
}
