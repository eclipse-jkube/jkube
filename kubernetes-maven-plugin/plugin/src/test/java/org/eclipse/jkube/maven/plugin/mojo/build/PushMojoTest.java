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
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class PushMojoTest {

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
    public void testExecuteInternal() throws MojoExecutionException, DependencyResolutionRequiredException, IOException, MojoFailureException, JKubeServiceException {
        // Given
        PushMojo pushMojo = getPushMojo();

        // When
        pushMojo.init();
        pushMojo.execute();

        // Then
        assertNotNull(pushMojo.authConfigFactory);
        assertNotNull(pushMojo.log);
        Assert.assertEquals(RuntimeMode.KUBERNETES, pushMojo.getRuntimeMode());
        new Verifications() {{
            mockedJKubeServiceHub.getBuildService().push((List<ImageConfiguration>)any, anyInt, (RegistryConfig)any, anyBoolean);
            times = 1;
        }};
    }

    private PushMojo getPushMojo() {
        PushMojo pushMojo = new PushMojo();
        pushMojo.settings = mavenSettings;
        pushMojo.project = mavenProject;
        pushMojo.dockerAccessFactory = mockedDockerAccessFactory;
        pushMojo.serviceHubFactory = mockedServiceHubFactory;
        pushMojo.imageConfigResolver = mockedImageConfigResolver;
        pushMojo.session = mavenSession;
        Map<String, Date> pluginContext = new HashMap<>();
        pushMojo.setPluginContext(pluginContext);
        File artifactFile = new File("testDir/target/test.jar");
        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test-image")
                .build(BuildConfiguration.builder()
                        .from("test-from:latest")
                        .build())
                .build();
        pushMojo.images = Collections.singletonList(imageConfiguration);
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

            mavenSession.getUserProperties();
            result = new Properties();
        }};
        return pushMojo;
    }
}
