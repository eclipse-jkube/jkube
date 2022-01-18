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
import java.net.URI;
import java.util.Properties;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.PodLogService;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("unused")
public class LogMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedConstruction<PodLogService> podLogServiceMockedConstruction;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;

  private LogMojo logMojo;

  @Before
  public void setUp() throws IOException {
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class);
    podLogServiceMockedConstruction = mockConstruction(PodLogService.class);
    kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
    mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    // @formatter:off
    logMojo = new LogMojo() { {
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
    logMojo = null;
  }

  @Test
  public void execute() throws Exception {
    // When
    logMojo.execute();
    // Then
    assertThat(podLogServiceMockedConstruction.constructed()).singleElement()
        .satisfies(podLogService -> verify(podLogService, times(1)).tailAppPodsLogs(
            any(KubernetesClient.class),
            isNull(),
            isNotNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(true)));
  }
}
