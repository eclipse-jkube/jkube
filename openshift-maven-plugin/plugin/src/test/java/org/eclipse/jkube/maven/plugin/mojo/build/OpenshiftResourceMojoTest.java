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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.maven.plugin.enricher.DefaultEnricherManager;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenshiftResourceMojoTest {
    @Mocked
    private MavenProject mavenProject;

    @Mocked
    private Settings mavenSettings;

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
    public void testExecuteInternal() throws MojoExecutionException, DependencyResolutionRequiredException, IOException, MojoFailureException {
        // Given
        ImageConfiguration imageConfiguration = getImageConfiguration();
        File artifactFile = new File("testDir/target/test.jar");
        setupExpectations(artifactFile, imageConfiguration);
        OpenshiftResourceMojo openshiftResourceMojo = getOpenshiftResourceMojo(imageConfiguration);

        // When
        openshiftResourceMojo.init();
        openshiftResourceMojo.execute();

        // Then
        assertTrue(openshiftResourceMojo.canExecute());
        assertNotNull(openshiftResourceMojo.getKitLogger());
        assertEquals(RuntimeMode.OPENSHIFT, openshiftResourceMojo.getRuntimeMode());
        assertEquals(PlatformMode.openshift, openshiftResourceMojo.getPlatformMode());
        assertEquals(ResourceClassifier.OPENSHIFT, openshiftResourceMojo.getResourceClassifier());
        assertEquals("test-jkube-namespace", openshiftResourceMojo.project.getProperties().get("docker.image.user"));
        assertEquals("OPENSHIFT", openshiftResourceMojo.project.getProperties().get("jkube.internal.effective.platform.mode"));
        new Verifications() {{
            mockedJKubeServiceHub.getResourceService().generateResources(PlatformMode.openshift, (DefaultEnricherManager)any, (KitLogger)any);
            times = 1;
            mockedJKubeServiceHub.getResourceService().writeResources((KubernetesList)any, ResourceClassifier.OPENSHIFT, (KitLogger)any);
            times = 1;
        }};
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteInternalOnFailure() throws MojoExecutionException, DependencyResolutionRequiredException, IOException, MojoFailureException {
        // Given
        File artifactFile = new File("testDir/target/test.jar");
        setupExpectations(artifactFile, getImageConfiguration());
        new Expectations() {{
            mockedJKubeServiceHub.getResourceService().generateResources(PlatformMode.openshift, (DefaultEnricherManager)any, (KitLogger)any);
            result = new IOException("Not able to generate resources");
        }};
        OpenshiftResourceMojo openshiftResourceMojo = getOpenshiftResourceMojo(getImageConfiguration());

        // When
        openshiftResourceMojo.init();
        openshiftResourceMojo.execute();
    }

    private OpenshiftResourceMojo getOpenshiftResourceMojo(ImageConfiguration imageConfiguration) {
        OpenshiftResourceMojo openshiftResourceMojo = new OpenshiftResourceMojo();
        openshiftResourceMojo.settings = mavenSettings;
        openshiftResourceMojo.project = mavenProject;
        openshiftResourceMojo.interpolateTemplateParameters = false;
        openshiftResourceMojo.failOnValidationError = false;
        openshiftResourceMojo.skipResourceValidation = true;
        openshiftResourceMojo.projectHelper = new TestMavenProjectHelper();
        openshiftResourceMojo.jkubeServiceHub = mockedJKubeServiceHub;
        openshiftResourceMojo.namespace = "test-jkube-namespace";
        openshiftResourceMojo.imageConfigResolver = mockedImageConfigResolver;
        Map<String, Date> pluginContext = new HashMap<>();
        openshiftResourceMojo.setPluginContext(pluginContext);
        openshiftResourceMojo.images = Collections.singletonList(imageConfiguration);
        return openshiftResourceMojo;
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

            mockedImageConfigResolver.resolve((ImageConfiguration) any, (JavaProject)any);
            result = Collections.singletonList(imageConfiguration);
        }};
    }

    private static class TestMavenProjectHelper implements MavenProjectHelper {

        @Override
        public void attachArtifact(MavenProject mavenProject, File file, String s) { }

        @Override
        public void attachArtifact(MavenProject mavenProject, String s, File file) { }

        @Override
        public void attachArtifact(MavenProject mavenProject, String s, String s1, File file) { }

        @Override
        public void addResource(MavenProject mavenProject, String s, List<String> list, List<String> list1) { }

        @Override
        public void addTestResource(MavenProject mavenProject, String s, List<String> list, List<String> list1) { }
    }
}
