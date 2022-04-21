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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("unused")
public class DebugMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;

  private DebugMojo debugMojo;

  @Before
  public void setUp() throws IOException {
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
          final OpenShiftClient oc = mock(OpenShiftClient.class, RETURNS_DEEP_STUBS);
          doReturn(oc).when(oc).adapt(OpenShiftClient.class);
          when(mock.getClient()).thenReturn(oc);
        });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class);
    kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
    mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    // @formatter:off
    debugMojo = new DebugMojo() { {
      project = mavenProject;
      settings = mock(Settings.class);
      kubernetesManifest = kubernetesManifestFile;
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
    clusterAccessMockedConstruction.close();
    jKubeServiceHubMockedConstruction.close();
    mavenProject = null;
    debugMojo = null;
  }

  @Test
  public void execute() throws Exception {
    // When
    debugMojo.execute();
    // Then
    assertThat(jKubeServiceHubMockedConstruction.constructed()).singleElement()
        .satisfies(jks -> verify(jks.getDebugService(), times(1)).debug(
            isNull(),
            eq("kubernetes.yml"),
            isNotNull(),
            isNull(),
            eq(false),
            any(AnsiLogger.class)));
  }
}
