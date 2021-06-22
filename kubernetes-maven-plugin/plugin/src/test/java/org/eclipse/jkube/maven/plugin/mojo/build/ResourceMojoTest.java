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

public class ResourceMojoTest {
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
        ResourceMojo resourceMojo = getResourceMojo();

        // When
        resourceMojo.init();
        resourceMojo.execute();

        // Then
        assertNotNull(resourceMojo.log);
        assertEquals(RuntimeMode.KUBERNETES, resourceMojo.getRuntimeMode());
        assertEquals(PlatformMode.kubernetes, resourceMojo.getPlatformMode());
        assertEquals(ResourceClassifier.KUBERNETES, resourceMojo.getResourceClassifier());
        new Verifications() {{
            mockedJKubeServiceHub.getResourceService().generateResources(PlatformMode.kubernetes, (DefaultEnricherManager)any, (KitLogger)any);
            times = 1;
            mockedJKubeServiceHub.getResourceService().writeResources((KubernetesList)any, ResourceClassifier.KUBERNETES, (KitLogger)any);
            times = 1;
        }};
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteInternalOnFailure() throws MojoExecutionException, DependencyResolutionRequiredException, IOException, MojoFailureException {
        // Given
        new Expectations() {{
            mockedJKubeServiceHub.getResourceService().generateResources(PlatformMode.kubernetes, (DefaultEnricherManager)any, (KitLogger)any);
            result = new IOException("Not able to generate resources");
        }};
        ResourceMojo resourceMojo = getResourceMojo();

        // When
        resourceMojo.init();
        resourceMojo.execute();
    }

    private ResourceMojo getResourceMojo() {
        ResourceMojo resourceMojo = new ResourceMojo();
        resourceMojo.settings = mavenSettings;
        resourceMojo.project = mavenProject;
        resourceMojo.interpolateTemplateParameters = false;
        resourceMojo.failOnValidationError = false;
        resourceMojo.skipResourceValidation = true;
        resourceMojo.projectHelper = new TestMavenProjectHelper();
        resourceMojo.jkubeServiceHub = mockedJKubeServiceHub;
        resourceMojo.namespace = "test-jkube-namespace";
        resourceMojo.imageConfigResolver = mockedImageConfigResolver;
        Map<String, Date> pluginContext = new HashMap<>();
        resourceMojo.setPluginContext(pluginContext);
        File artifactFile = new File("testDir/target/test.jar");
        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test-image")
                .build(BuildConfiguration.builder()
                        .from("test-from:latest")
                        .build())
                .build();
        resourceMojo.images = Collections.singletonList(imageConfiguration);
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
        return resourceMojo;
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
