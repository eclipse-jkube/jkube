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

import io.fabric8.openshift.client.OpenShiftClient;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"unused", "java:S3599", "java:S1171"})
public class OpenshiftUndeployMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mocked
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
  public void getManifestsToUndeploy() {
    // Given
    // @formatter:off
    new Expectations() {{
      mockServiceHub.getClient().isAdaptable(OpenShiftClient.class); result = true;
    }};
    // @formatter:on
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