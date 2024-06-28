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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.config.service.DebugContext;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class DebugMojoTest {

  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;

  private DebugMojo debugMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
          final OpenShiftClient oc = mock(OpenShiftClient.class, RETURNS_DEEP_STUBS);
          doReturn(oc).when(oc).adapt(OpenShiftClient.class);
          when(mock.getClient()).thenReturn(oc);
        });
    kubernetesManifestFile = Files.createFile(temporaryFolder.resolve("kubernetes.yml")).toFile();
    mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    // @formatter:off
    debugMojo = new DebugMojo() { {
      project = mavenProject;
      settings = mock(Settings.class);
      kubernetesManifest = kubernetesManifestFile;
      interpolateTemplateParameters = false;
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    jKubeServiceHubMockedConstruction.close();
    mavenProject = null;
    debugMojo = null;
  }

  @Test
  void execute() throws Exception {
    // Given
    ArgumentCaptor<DebugContext> debugContextArgumentCaptor = ArgumentCaptor.forClass(DebugContext.class);
    // When
    debugMojo.execute();
    // Then
    assertThat(jKubeServiceHubMockedConstruction.constructed()).singleElement()
        .satisfies(jks -> verify(jks.getDebugService(), times(1)).debug(debugContextArgumentCaptor.capture(), any()));
    assertThat(debugContextArgumentCaptor.getValue())
        .hasFieldOrPropertyWithValue("fileName", "kubernetes.yml")
        .hasFieldOrPropertyWithValue("debugSuspend", false)
        .satisfies(d -> assertThat(d.getPodWaitLog()).isInstanceOf(AnsiLogger.class));
  }
}
