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
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenshiftUndeployMojoTest {
  private JKubeServiceHub mockServiceHub;
  private File kubernetesManifestFile;
  private File openShiftManifestFile;
  private File openShiftISManifestFile;
  private OpenshiftUndeployMojo undeployMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
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
}

