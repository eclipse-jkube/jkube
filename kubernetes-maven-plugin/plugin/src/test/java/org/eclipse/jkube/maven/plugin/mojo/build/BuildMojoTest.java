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

import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.eclipse.jkube.kit.config.resource.OpenshiftBuildConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import io.fabric8.kubernetes.client.KubernetesClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BuildMojoTest {
    @Mocked
    private MavenProject mavenProject;

    @Mocked
    private Settings mavenSettings;

    @Mocked
    private MavenSession mavenSession;

    @Mocked
    private Build mavenProjectBuild;

    @Mocked
    private DockerAccessFactory mockedDockerAccessFactory;

    @Mocked
    private ImageConfigResolver mockedImageConfigResolver;

    @Mocked
    private ClusterAccess mockedClusterAccess;

    @Mocked
    private KubernetesClient client;

    @Mocked
    private JKubeServiceHub mockedJKubeServiceHub;

    @Mocked
    private ServiceHubFactory mockedServiceHubFactory;

    @Mocked
    private DockerAccess mockedDockerAccess;

    @Test
    public void testBuildServiceConfigBuilderReturnsNonNullResourceConfigIfConfigured() {
        // Given
        BuildMojo buildMojo = new BuildMojo();
        buildMojo.project = mavenProject;
        buildMojo.session = mavenSession;
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
        assertThat(buildServiceConfigBuilder).isNotNull();
        BuildServiceConfig buildServiceConfig = buildServiceConfigBuilder.build();
        assertThat(buildServiceConfig).isNotNull();
        assertThat(buildServiceConfig.getResourceConfig()).isNotNull();
        assertThat(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig()).isNotNull();
        assertThat(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig().getLimits()).containsEntry("cpu", "200m");
        assertThat(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig().getRequests()).containsEntry("memory", "1Gi");
        assertThat(buildServiceConfig.getResourceDir().getPath()).isEqualTo("src/main/jkube");
    }

    @Test
    public void testExecuteInternal() throws MojoExecutionException, DependencyResolutionRequiredException, MojoFailureException, JKubeServiceException {
        // Given
        File artifactFile = new File("testDir/target/test.jar");
        ImageConfiguration imageConfiguration = getImageConfiguration();
        setupExpectations(artifactFile, Collections.singletonList(imageConfiguration), "jar");
        BuildMojo buildMojo = getBuildMojo(imageConfiguration);

        // When
        buildMojo.init();
        buildMojo.execute();

        // Then
        assertTrue(buildMojo.canExecute());
        assertNotNull(buildMojo.authConfigFactory);
        assertNotNull(buildMojo.getKitLogger());
        assertEquals(RuntimeMode.KUBERNETES, buildMojo.getRuntimeMode());
        new Verifications() {{
            mockedJKubeServiceHub.getBuildService().build((ImageConfiguration)any);
            times = 1;
            mockedJKubeServiceHub.getBuildService().postProcess((BuildServiceConfig)any);
            times = 1;
        }};
    }

    @Test
    public void testExecuteInternalWithPomPackaging() throws DependencyResolutionRequiredException, MojoFailureException, MojoExecutionException, JKubeServiceException {
        // Given
        File artifactFile = new File("testDir/target/test.jar");
        ImageConfiguration imageConfiguration = getImageConfiguration();
        setupExpectations(artifactFile, Collections.singletonList(imageConfiguration), "pom");
        BuildMojo buildMojo = getBuildMojo(imageConfiguration);
        buildMojo.skipBuildPom = true;

        // When
        buildMojo.init();
        buildMojo.execute();

        // Then
        assertTrue(buildMojo.canExecute());
        assertNotNull(buildMojo.authConfigFactory);
        assertNotNull(buildMojo.getKitLogger());
        assertEquals(RuntimeMode.KUBERNETES, buildMojo.getRuntimeMode());
    }

    @Test
    public void testExecuteInternalWithNoImageConfiguration() throws DependencyResolutionRequiredException, MojoFailureException, MojoExecutionException {
        // Given
        File artifactFile = new File("testDir/target/test.jar");
        setupExpectations(artifactFile, Collections.emptyList(), "jar");
        BuildMojo buildMojo = getBuildMojo(null);
        buildMojo.resolvedImages = Collections.emptyList();

        // When
        buildMojo.init();
        buildMojo.execute();

        // Then
        assertTrue(buildMojo.canExecute());
        assertNotNull(buildMojo.authConfigFactory);
        assertNotNull(buildMojo.getKitLogger());
        assertEquals(RuntimeMode.KUBERNETES, buildMojo.getRuntimeMode());
    }

    @Test
    public void testGetJKubeBuildStrategyDefault() {
        // Given
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());

        // When
        JKubeBuildStrategy jKubeBuildStrategy = buildMojo.getJKubeBuildStrategy();
        boolean isDockerAccessRequired = buildMojo.isDockerAccessRequired();

        // Then
        assertEquals(JKubeBuildStrategy.docker, jKubeBuildStrategy);
        assertTrue(isDockerAccessRequired);
    }

    @Test
    public void testGetJKubeBuildStrategyProvided() {
        // Given
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());
        buildMojo.buildStrategy = JKubeBuildStrategy.jib;

        // When
        JKubeBuildStrategy jKubeBuildStrategy = buildMojo.getJKubeBuildStrategy();
        boolean isDockerAccessRequired = buildMojo.isDockerAccessRequired();

        // Then
        assertEquals(JKubeBuildStrategy.jib, jKubeBuildStrategy);
        assertFalse(isDockerAccessRequired);
    }

    @Test
    public void testShouldSkipBecauseOfPomPackagingWithJar() {
        // Given
        new Expectations() {{
            mavenProject.getPackaging();
            result = "jar";
        }};
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());
        buildMojo.project = mavenProject;

        // When
        boolean result = buildMojo.shouldSkipBecauseOfPomPackaging();

        // Then
        assertFalse(result);
    }

    @Test
    public void testShouldSkipBecauseOfPomPackagingWithPom() {
        // Given
        new Expectations() {{
            mavenProject.getPackaging();
            result = "pom";
        }};
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());
        buildMojo.project = mavenProject;
        buildMojo.resolvedImages = Collections.singletonList(getImageConfiguration());

        // When
        boolean result = buildMojo.shouldSkipBecauseOfPomPackaging();

        // Then
        assertFalse(result);
    }

    @Test
    public void testShouldSkipBecauseOfPomPackagingWithPomWithSkipPomEnabled() {
        // Given
        new Expectations() {{
            mavenProject.getPackaging();
            result = "pom";
        }};
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());
        buildMojo.project = mavenProject;
        buildMojo.skipBuildPom = true;

        // When
        boolean result = buildMojo.shouldSkipBecauseOfPomPackaging();

        // Then
        assertTrue(result);
    }

    @Test
    public void testShouldSkipBecauseOfPomPackagingWithPomWithSkipPomEnabledAndNoImageConfiguration() {
        // Given
        new Expectations() {{
            mavenProject.getPackaging();
            result = "pom";
        }};
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());
        buildMojo.project = mavenProject;
        buildMojo.resolvedImages = Collections.emptyList();

        // When
        boolean result = buildMojo.shouldSkipBecauseOfPomPackaging();

        // Then
        assertTrue(result);
    }

    @Test
    public void testGetAndEnsureOutputDirectory() throws IOException {
        // Given
        File tempDir = Files.createTempDirectory("buildmojo-project").toFile();
        new Expectations() {{
            mavenProject.getBuild().getDirectory();
            result = tempDir.getPath();
        }};
        BuildMojo buildMojo = getBuildMojo(getImageConfiguration());

        // When
        File outputDir = buildMojo.getAndEnsureOutputDirectory();

        // Then
        assertNotNull(outputDir);
        assertEquals(new File(tempDir, "docker-extra").toPath(), outputDir.toPath());
    }

    private BuildMojo getBuildMojo(ImageConfiguration imageConfiguration) {
        BuildMojo buildMojo = new BuildMojo();
        buildMojo.settings = mavenSettings;
        buildMojo.project = mavenProject;
        buildMojo.dockerAccessFactory = mockedDockerAccessFactory;
        buildMojo.serviceHubFactory = mockedServiceHubFactory;
        buildMojo.jkubeServiceHub = mockedJKubeServiceHub;
        buildMojo.imageConfigResolver = mockedImageConfigResolver;
        buildMojo.session = mavenSession;
        Map<String, Date> pluginContext = new HashMap<>();
        buildMojo.setPluginContext(pluginContext);
        buildMojo.images = Collections.singletonList(imageConfiguration);
        return buildMojo;
    }

    private void setupExpectations(File artifactFile, List<ImageConfiguration> imageConfigurations, String projectPackaging) {
        new Expectations() {{
            mavenProject.getProperties();
            result = new Properties();

            mavenProject.getBuild();
            result = mavenProjectBuild;

            mavenProjectBuild.getOutputDirectory();
            result = "testDir/target/classes";

            mavenProjectBuild.getDirectory();
            result = "testDir/target";

            mavenProject.getBasedir();
            result = "testDir";

            mavenProject.getPackaging();
            result = projectPackaging;

            mavenProject.getArtifact().getFile();
            result = artifactFile;

            mockedImageConfigResolver.resolve((ImageConfiguration) any, (JavaProject)any);
            result = imageConfigurations;

            mavenSession.getUserProperties();
            result = new Properties();
        }};
    }

    private ImageConfiguration getImageConfiguration() {
        return ImageConfiguration.builder()
                .name("test-image")
                .build(BuildConfiguration.builder()
                        .from("test-from:latest")
                        .build())
                .build();
    }
}
