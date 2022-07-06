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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OpenshiftUndeployMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private JKubeServiceHub mockServiceHub;
  private File kubernetesManifestFile;
  private File openShiftManifestFile;
  private File openShiftISManifestFile;
  private OpenshiftUndeployMojo undeployMojo;

  @Before
  public void setUp() throws IOException {
    kubernetesManifestFile = temporaryFolder.newFile();
    openShiftManifestFile = temporaryFolder.newFile();
    openShiftISManifestFile = temporaryFolder.newFile();
    undeployMojo = new OpenshiftUndeployMojo() {{
      kubernetesManifest = kubernetesManifestFile;
      openshiftManifest = openShiftManifestFile;
      openshiftImageStreamManifest = openShiftISManifestFile;
      jkubeServiceHub = mockServiceHub;
    }};
  }

  @Test
  public void getManifestsToUndeploy() {
    KubernetesClient client= mock(KubernetesClient.class);
    mockServiceHub=mock(JKubeServiceHub.class);
    undeployMojo = mock(OpenshiftUndeployMojo.class);
    // Given
    when(mockServiceHub.getClient()).thenReturn(client);
    when(client.isAdaptable(OpenShiftClient.class)).thenReturn(true);
    // When
    final List<File> result = undeployMojo.getManifestsToUndeploy();
    // Then
    assertThat(result).contains(openShiftManifestFile, openShiftISManifestFile);
  }

  @Test
  public void getRuntimeMode() {
    assertThat(undeployMojo.getRuntimeMode()).isEqualTo(RuntimeMode.OPENSHIFT);
  }

  @Test
  public void getLogPrefix() {
    assertThat(undeployMojo.getLogPrefix()).isEqualTo("oc: ");
  }
}