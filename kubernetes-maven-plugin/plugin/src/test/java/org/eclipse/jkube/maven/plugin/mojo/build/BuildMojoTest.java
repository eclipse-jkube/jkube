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
package org.eclipse.jkube.maven.plugin.mojo.build;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.OpenshiftBuildConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildMojoTest {
    private DockerAccessFactory mockedDockerAccessFactory;
    private MavenProject mockedMavenProject;
    private Settings mockedMavenSettings;
    private BuildMojo buildMojo;
    private BuildService mockedBuildService;
    private List<ImageConfiguration> imageConfigurationList;
    private MockedStatic<MavenUtil> mavenUtilMockedStatic;
    private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
    @TempDir
    private File temporaryFolder;

    @BeforeEach
    void setUp() {
        imageConfigurationList = Collections.singletonList(ImageConfiguration.builder()
            .name("test/foo:latest")
            .build(BuildConfiguration.builder()
                .from("test/base:latest")
                .build())
            .build());
        mockedDockerAccessFactory = mock(DockerAccessFactory.class);
        ImageConfigResolver mockedImageConfigResolver = mock(ImageConfigResolver.class);
        MojoExecution mockedMojoExecution = mock(MojoExecution.class);
        MavenSession mockedMavenSession = mock(MavenSession.class);
        JavaProject mockedJavaProject = mock(JavaProject.class);
        JKubeConfiguration mockedJKubeConfiguration = mock(JKubeConfiguration.class);
        SummaryService mockedSummaryService = mock(SummaryService.class);
        mockedMavenProject = mock(MavenProject.class, RETURNS_DEEP_STUBS);
        mockedMavenSettings = mock(Settings.class, RETURNS_DEEP_STUBS);
        mockedBuildService = mock(BuildService.class);
        mavenUtilMockedStatic = mockStatic(MavenUtil.class);
        jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class, (mock, ctx) -> {
            when(mock.getBuildService()).thenReturn(mockedBuildService);
            when(mock.getConfiguration()).thenReturn(mockedJKubeConfiguration);
            when(mock.getSummaryService()).thenReturn(mockedSummaryService);
        });
        mavenUtilMockedStatic.when(() -> MavenUtil.convertMavenProjectToJKubeProject(any(), any()))
            .thenReturn(mockedJavaProject);
        when(mockedDockerAccessFactory.createDockerAccess(any())).thenReturn(mock(DockerAccess.class));
        when(mockedMavenProject.getPackaging()).thenReturn("jar");
        when(mockedMavenProject.getBuild().getDirectory()).thenReturn(temporaryFolder.getAbsolutePath());
        when(mockedJKubeConfiguration.getProject()).thenReturn(mockedJavaProject);
        when(mockedJavaProject.getProperties()).thenReturn(new Properties());
        buildMojo = new BuildMojo() {{
            dockerAccessFactory = mockedDockerAccessFactory;
            imageConfigResolver = mockedImageConfigResolver;
            project = mockedMavenProject;
            settings = mockedMavenSettings;
            session = mockedMavenSession;
            mojoExecution = mockedMojoExecution;
        }};
    }

    @AfterEach
    void tearDown() {
        mavenUtilMockedStatic.close();
        jKubeServiceHubMockedConstruction.close();
    }

    @Test
    void execute_withImageConfiguration_shouldDelegateToBuildService() throws Exception {
        // Given
        buildMojo.resolvedImages = imageConfigurationList;

        // When
        buildMojo.execute();

        // Then
        assertThat(jKubeServiceHubMockedConstruction.constructed()).isNotEmpty();
        verify(mockedBuildService).build(any());
        verify(mockedBuildService).postProcess();
    }

    @Test
    void execute_withPomPackagingAndSkipBuildPom_shouldDoNothing() throws Exception {
        // Given
        buildMojo.resolvedImages = imageConfigurationList;
        buildMojo.skipBuildPom = true;
        when(mockedMavenProject.getPackaging()).thenReturn("pom");

        // When
        buildMojo.execute();

        // Then
        verify(mockedBuildService, times(0)).build(any());
    }

    @Test
    void execute_whenBuildServiceFails_thenThrowException() throws Exception {
        // Given
        doThrow(new JKubeServiceException("failure in build"))
            .when(mockedBuildService).build(any());

        // When + Then
        assertThatExceptionOfType(MojoExecutionException.class)
            .isThrownBy(() -> buildMojo.execute())
            .withMessage("Failed to execute the build");
    }

    @Test
    void execute_whenFailureInInitializingProject_thenThrowException() {
        // Given
        DependencyResolutionRequiredException dependencyResolutionRequiredException = mock(DependencyResolutionRequiredException.class);
        when(dependencyResolutionRequiredException.getMessage()).thenReturn("Attempted to access the artifact foo; which has not yet been resolved");
        mavenUtilMockedStatic.when(() -> MavenUtil.convertMavenProjectToJKubeProject(any(), any()))
                .thenThrow(dependencyResolutionRequiredException);

        // When + Then
        assertThatExceptionOfType(MojoExecutionException.class)
            .isThrownBy(() -> buildMojo.execute())
            .withMessage("Attempted to access the artifact foo; which has not yet been resolved");
    }

    @Test
    void buildServiceConfigBuilder_shouldReturnNonNullResourceConfigIfConfigured() {
        // Given
        buildMojo.resources = ResourceConfig.builder()
                .openshiftBuildConfig(OpenshiftBuildConfig.builder()
                        .limit("cpu", "200m")
                        .request("memory", "1Gi")
                        .build())
                .build();
        buildMojo.resourceDir = new File("src/main/jkube");

        // When
        BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder = buildMojo.buildServiceConfigBuilder();
        // Then
        assertThat(buildServiceConfigBuilder.build()).isNotNull()
            .returns("src/main/jkube", c -> c.getResourceDir().getPath())
            .extracting(BuildServiceConfig::getResourceConfig)
            .extracting(ResourceConfig::getOpenshiftBuildConfig)
            .returns("200m", c -> c.getLimits().get("cpu"))
            .returns("1Gi", c -> c.getRequests().get("memory"));
    }
}
