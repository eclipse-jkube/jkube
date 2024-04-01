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
package org.eclipse.jkube.maven.plugin.mojo.build;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.DefaultGeneratorManager;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ContainerResourcesConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildMojoTest {
    private MavenProject mavenProject;
    @BeforeEach
    void setUp() {
        mavenProject = mock(MavenProject.class,RETURNS_DEEP_STUBS);
        when(mavenProject.getBuild().getDirectory()).thenReturn("target");
    }

    @Test
    void buildServiceConfigBuilder_shouldReturnNonNullResourceConfigIfConfigured() {
        // Given
        BuildMojo buildMojo = new BuildMojo();
        buildMojo.project = mavenProject;
        buildMojo.resources = ResourceConfig.builder()
                .openshiftBuildConfig(ContainerResourcesConfig.builder()
                        .limit("cpu", "200m")
                        .request("memory", "1Gi")
                        .build())
                .build();
        buildMojo.resourceDir = new File("src/main/jkube");

        // When
        BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder = buildMojo.buildServiceConfigBuilder();
        // Then
        assertThat(buildServiceConfigBuilder.build()).isNotNull()
            .returns(new File("src/main/jkube"), BuildServiceConfig::getResourceDir)
            .extracting(BuildServiceConfig::getResourceConfig)
            .extracting(ResourceConfig::getOpenshiftBuildConfig)
            .returns("200m", c -> c.getLimits().get("cpu"))
            .returns("1Gi", c -> c.getRequests().get("memory"));
    }

    @Test
    @DisplayName("when Dockerfile present in project base directory, then generate opinionated ImageConfiguration for Dockerfile")
    void simpleDockerfileModeWorksAsExpected(@TempDir File temporaryFolder) throws IOException {
        // Given
        BuildMojo buildMojo = new BuildMojo();
        File dockerFileInProjectBaseDir = temporaryFolder.toPath().resolve("Dockerfile").toFile();
        Files.createFile(dockerFileInProjectBaseDir.toPath());
        buildMojo.resourceDir = new File("src/main/jkube");
        buildMojo.log = new KitLogger.SilentLogger();
        buildMojo.javaProject = JavaProject.builder()
            .baseDirectory(temporaryFolder)
            .outputDirectory(temporaryFolder.toPath().resolve("target").toFile())
            .buildDirectory(temporaryFolder.toPath().resolve("target").toFile())
            .plugin(Plugin.builder().groupId("org.springframework.boot").artifactId("spring-boot-maven-plugin").build())
            .properties(new Properties()).build();
        GeneratorContext generatorContext = buildMojo.generatorContextBuilder().build();
        List<ImageConfiguration> imageConfigurations = new ArrayList<>();
        DefaultGeneratorManager generatorManager = new DefaultGeneratorManager(generatorContext);

        // When
        List<ImageConfiguration> imageConfigurationList = generatorManager.generate(imageConfigurations);

        // Then
        assertThat(imageConfigurationList)
            .hasSize(1)
            .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
            .extracting(ImageConfiguration::getBuild)
            .extracting(BuildConfiguration::getDockerFileRaw)
            .isEqualTo(dockerFileInProjectBaseDir.getAbsolutePath());
    }
}
