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

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ApplyMojoFailureScenariosTest {

  private JKubeServiceHub mockedJKubeServiceHub;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;
  private File kubernetesManifestFile;
  private ApplyService mockedApplyService;

  private ApplyMojo applyMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    mockedApplyService = mock(ApplyService.class);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    kubernetesHelperMockedStatic.when(KubernetesHelper::getDefaultNamespace).thenReturn("default");
    kubernetesManifestFile = Files.createFile(temporaryFolder.resolve("kubernetes.yml")).toFile();
    NamespacedOpenShiftClient defaultKubernetesClient = mock(NamespacedOpenShiftClient.class);
    when(defaultKubernetesClient.adapt(any())).thenReturn(defaultKubernetesClient);
    when(defaultKubernetesClient.getMasterUrl()).thenReturn(URI.create("https://www.example.com").toURL());
    mockedJKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    when(mockedJKubeServiceHub.getApplyService()).thenReturn(mockedApplyService);
    when(mockedJKubeServiceHub.getClient()).thenReturn(defaultKubernetesClient);
    // @formatter:off
    applyMojo = new ApplyMojo() {{
      jkubeServiceHub = mockedJKubeServiceHub;
      settings = mock(Settings.class);
      kubernetesManifest = kubernetesManifestFile;
      log = new KitLogger.SilentLogger();
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    kubernetesHelperMockedStatic.close();
    applyMojo = null;
  }

  @Test
  void executeInternal_whenApplyServiceFailsToApplyManifests_thenThrowException() {
    // Given
    doThrow(new KubernetesClientException("kubernetes failure"))
        .when(mockedApplyService).applyEntities(anyString(), anyCollection(), any(), anyLong());

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> applyMojo.executeInternal())
        .withMessage("kubernetes failure");
  }

  @Test
  void executeInternal_whenManifestLoadFailure_thenThrowException() {
      // Given
      kubernetesHelperMockedStatic.when(() -> KubernetesHelper.loadResources(any()))
          .thenThrow(new IOException("failure in loading manifests"));

      // When + Then
      assertThatExceptionOfType(MojoExecutionException.class)
          .isThrownBy(() -> applyMojo.executeInternal())
          .withMessage("failure in loading manifests");
  }

}
