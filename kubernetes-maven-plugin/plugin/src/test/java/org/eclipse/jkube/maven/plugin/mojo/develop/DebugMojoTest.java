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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class DebugMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File kubernetesManifestFile;

    @Mocked
    private MavenProject mavenProject;

    @Mocked
    private Settings mavenSettings;

    @Mocked
    private ClusterAccess mockedClusterAccess;

    @Mocked
    private KubernetesClient kubernetesClient;

    @Mocked
    private KitLogger logger;

    @Mocked
    private JKubeServiceHub mockedJKubeServiceHub;

    @Test
    public void testExecuteInternal() throws MojoExecutionException, IOException, MojoFailureException {
        // Given
        DebugMojo debugMojo = getDebugMojo();

        // When
        debugMojo.execute();

        // Then
        new Verifications() {{
            mockedJKubeServiceHub.getDebugService().debug(kubernetesClient, anyString, anyString, (Set<HasMetadata>)any, anyString, anyBoolean, (KitLogger)any);
            times = 1;
        }};
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteInternalOnFailure() throws IOException, MojoFailureException, MojoExecutionException {
        // Given
        new Expectations() {{
            mockedJKubeServiceHub.getDebugService().debug(kubernetesClient, anyString, anyString, (Set<HasMetadata>)any, anyString, anyBoolean, (KitLogger)any);
            result = new IllegalStateException("Could not find a running pod with environment variables ");
        }};

        DebugMojo debugMojo = getDebugMojo();

        // When
        debugMojo.execute();
    }

    private DebugMojo getDebugMojo() throws IOException {
        kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
        new Expectations() {{
            mavenProject.getParent();
            result = null;

            mavenProject.getProperties();
            result = new Properties();

            mavenProject.getBuild().getOutputDirectory();
            result = "target/classes";

            mavenProject.getBuild().getDirectory();
            result = "target";

            mockedClusterAccess.createDefaultClient();
            result = kubernetesClient;
        }};

        DebugMojo debugMojo = new DebugMojo() {{
            project = mavenProject;
            settings = mavenSettings;
            clusterAccess = mockedClusterAccess;
            log = logger;
            kubernetesManifest = kubernetesManifestFile;
            jkubeServiceHub = mockedJKubeServiceHub;
            verbose = "true";
        }};
        return debugMojo;
    }
}
