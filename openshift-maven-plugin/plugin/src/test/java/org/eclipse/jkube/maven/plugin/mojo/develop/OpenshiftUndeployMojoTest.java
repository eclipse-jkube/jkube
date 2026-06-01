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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class OpenshiftUndeployMojoTest {
  private JKubeServiceHub mockServiceHub;
  private File kubernetesManifestFile;
  private File openShiftManifestFile;
  private File openShiftISManifestFile;
  private OpenshiftUndeployMojo undeployMojo;
  @Mock
  private Settings settings;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    mocks = openMocks(this);
    mockServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    kubernetesManifestFile = Files.createTempFile(temporaryFolder, "junit", "ext").toFile();
    openShiftManifestFile = Files.createTempFile(temporaryFolder, "junit", "ext").toFile();
    openShiftISManifestFile = Files.createTempFile(temporaryFolder, "junit", "ext").toFile();
    // @formatter:off
    undeployMojo = new OpenshiftUndeployMojo() {{
      kubernetesManifest = kubernetesManifestFile;
      openshiftManifest = openShiftManifestFile;
      openshiftImageStreamManifest = openShiftISManifestFile;
      jkubeServiceHub = mockServiceHub;
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void getManifestsToUndeploy() {
    // Given
    final OpenShiftClient client = mock(OpenShiftClient.class);
    when(mockServiceHub.getClient()).thenReturn(client);
    when(client.hasApiGroup("openshift.io", false)).thenReturn(true);
    // When
    final List<File> result = undeployMojo.getManifestsToUndeploy();
    // Then
    assertThat(result).contains(openShiftManifestFile, openShiftISManifestFile);
  }

  @Test
  void getRuntimeMode() {
    assertThat(undeployMojo.getRuntimeMode()).isEqualTo(RuntimeMode.OPENSHIFT);
  }

  @Test
  void getLogPrefix() {
    assertThat(undeployMojo.getLogPrefix()).isEqualTo("oc: ");
  }

  @Test
  void execute_whenSkipUndeployTrue_shouldDoNothing() throws MojoExecutionException, MojoFailureException {
    // Given
    KitLogger kitLogger = spy(new KitLogger.SilentLogger());
    MavenProject mockProject = mock(MavenProject.class);
    when(mockProject.getProperties()).thenReturn(new Properties());
    MojoExecution mockExecution = new MojoExecution(new MojoDescriptor());
    mockExecution.getMojoDescriptor().setPluginDescriptor(new PluginDescriptor());
    mockExecution.getMojoDescriptor().setGoal("undeploy");
    mockExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("oc");

    OpenshiftUndeployMojo skipUndeployMojo = new OpenshiftUndeployMojo() {
      @Override
      protected KitLogger createLogger(String prefix) {
        return kitLogger;
      }
      {
        project = mockProject;
        settings = OpenshiftUndeployMojoTest.this.settings;
        skip = true;
        mojoExecution = mockExecution;
      }
    };

    // When
    skipUndeployMojo.execute();
    // Then
    verify(kitLogger).info("`%s` goal is skipped.", "oc:undeploy");
  }
}

