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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
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
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.EnricherManager;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenshiftBuildMojoTest {
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
    public void testExecuteInternal() throws MojoExecutionException, DependencyResolutionRequiredException, MojoFailureException, JKubeServiceException {
        // Given
        File artifactFile = new File("testDir/target/test.jar");
        ImageConfiguration imageConfiguration = getImageConfiguration();
        setupExpectations(artifactFile, imageConfiguration);
        OpenshiftBuildMojo openshiftBuildMojo = getOpenshiftBuildMojo(imageConfiguration);

        // When
        openshiftBuildMojo.init();
        openshiftBuildMojo.execute();

        // Then
        assertTrue(openshiftBuildMojo.canExecute());
        assertEquals(RuntimeMode.OPENSHIFT, openshiftBuildMojo.getRuntimeMode());
        assertEquals(JKubeBuildStrategy.s2i, openshiftBuildMojo.getJKubeBuildStrategy());
        assertNotNull(openshiftBuildMojo.getKitLogger());
        assertFalse(openshiftBuildMojo.isDockerAccessRequired());
        assertEquals(OpenShift.DEFAULT_LOG_PREFIX, openshiftBuildMojo.getLogPrefix());
        new Verifications() {{
            mockedJKubeServiceHub.getBuildService().build((ImageConfiguration)any);
            times = 1;
            mockedJKubeServiceHub.getBuildService().postProcess((BuildServiceConfig)any);
            times = 1;
        }};
    }

    @Test
    public void testGetJKubeBuildStrategyDefault() {
        // Given
        OpenshiftBuildMojo openshiftBuildMojo = getOpenshiftBuildMojo(getImageConfiguration());

        // When
        JKubeBuildStrategy jKubeBuildStrategy = openshiftBuildMojo.getJKubeBuildStrategy();
        boolean isDockerAccessRequired = openshiftBuildMojo.isDockerAccessRequired();

        // Then
        assertEquals(JKubeBuildStrategy.s2i, jKubeBuildStrategy);
        assertFalse(isDockerAccessRequired);
    }

    @Test
    public void testGetJKubeBuildStrategyProvided() {
        // Given
        OpenshiftBuildMojo openshiftBuildMojo = getOpenshiftBuildMojo(getImageConfiguration());
        openshiftBuildMojo.buildStrategy = JKubeBuildStrategy.jib;

        // When
        JKubeBuildStrategy jKubeBuildStrategy = openshiftBuildMojo.getJKubeBuildStrategy();
        boolean isDockerAccessRequired = openshiftBuildMojo.isDockerAccessRequired();

        // Then
        assertEquals(JKubeBuildStrategy.jib, jKubeBuildStrategy);
        assertFalse(isDockerAccessRequired);
    }

    @Test
    public void testEnricherTask(@Mocked EnricherManager enricherManager) {
        // Given
        OpenshiftBuildMojo openshiftBuildMojo = getOpenshiftBuildMojo(getImageConfiguration());
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();

        // When
        openshiftBuildMojo.enricherTask(kubernetesListBuilder, enricherManager);

        // Then
        new Verifications() {{
            enricherManager.enrich(PlatformMode.openshift, kubernetesListBuilder);
            times = 1;
        }};
    }

    private OpenshiftBuildMojo getOpenshiftBuildMojo(ImageConfiguration imageConfiguration) {
        OpenshiftBuildMojo openshiftBuildMojo = new OpenshiftBuildMojo();
        openshiftBuildMojo.settings = mavenSettings;
        openshiftBuildMojo.project = mavenProject;
        openshiftBuildMojo.session = mavenSession;
        openshiftBuildMojo.serviceHubFactory = mockedServiceHubFactory;
        openshiftBuildMojo.jkubeServiceHub = mockedJKubeServiceHub;
        openshiftBuildMojo.imageConfigResolver = mockedImageConfigResolver;
        Map<String, Date> pluginContext = new HashMap<>();
        openshiftBuildMojo.setPluginContext(pluginContext);
        openshiftBuildMojo.images = Collections.singletonList(imageConfiguration);
        return openshiftBuildMojo;
    }

    private ImageConfiguration getImageConfiguration() {
        return ImageConfiguration.builder()
                .name("test-image")
                .build(BuildConfiguration.builder()
                        .from("test-from:latest")
                        .build())
                .build();
    }

    private void setupExpectations(File artifactFile, ImageConfiguration imageConfiguration) {
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
            result = "jar";

            mavenProject.getArtifact().getFile();
            result = artifactFile;

            mavenSession.getUserProperties();
            result = new Properties();

            mockedImageConfigResolver.resolve((ImageConfiguration) any, (JavaProject)any);
            result = Collections.singletonList(imageConfiguration);
        }};
    }
}
