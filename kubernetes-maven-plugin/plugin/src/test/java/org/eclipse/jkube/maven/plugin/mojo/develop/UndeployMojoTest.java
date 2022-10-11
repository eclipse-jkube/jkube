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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.config.service.UndeployService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UndeployMojoTest {
  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedStatic<MavenUtil> mavenUtilMockedStatic;
  private MockedStatic<OpenshiftHelper> openshiftHelperMockedStatic;
  private UndeployService mockedUndeployService;
  private MavenProject mavenProject;
  private Settings mavenSettings;
  private MojoExecution mockedMojoExecution;
  private File mockManifest;
  private File mockResourceDir;
  private UndeployMojo undeployMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    mockManifest = File.createTempFile("junit", "ext", temporaryFolder.toFile());
    mockResourceDir = Files.createDirectory(temporaryFolder.resolve("resources")).toFile();
    mockedUndeployService = mock(UndeployService.class);
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class, (mock, ctx) -> {
      when(mock.getUndeployService()).thenReturn(mockedUndeployService);
    });
    openshiftHelperMockedStatic = mockStatic(OpenshiftHelper.class);
    openshiftHelperMockedStatic.when(() -> OpenshiftHelper.isOpenShift(any())).thenReturn(false);
    mavenUtilMockedStatic = mockStatic(MavenUtil.class);
    mavenProject = mock(MavenProject.class);
    mavenSettings = mock(Settings.class);
    mockedMojoExecution = mock(MojoExecution.class, RETURNS_DEEP_STUBS);
    undeployMojo = new UndeployMojo() {{
      resourceDir = mockResourceDir;
      kubernetesManifest = mockManifest;
      project = mavenProject;
      settings = mavenSettings;
      log = new KitLogger.SilentLogger();
      mojoExecution = mockedMojoExecution;
    }};
    when(mavenProject.getProperties()).thenReturn(new Properties());
  }

  @AfterEach
  void tearDown() {
    mavenUtilMockedStatic.close();
    openshiftHelperMockedStatic.close();
    jKubeServiceHubMockedConstruction.close();
    undeployMojo = null;
  }

  @Test
  void execute() throws Exception {
    // When
    undeployMojo.execute();
    // Then
    assertUndeployServiceUndeployWasCalled();
  }

  @Test
  void execute_whenSkipUndeployEnabled_thenUndeployServiceNotCalled() throws Exception {
    // Given
    undeployMojo.skipUndeploy = true;
    when(mockedMojoExecution.getMojoDescriptor().getFullGoalName()).thenReturn("k8s:undeploy");
    // When
    undeployMojo.execute();

    // Then
    assertUndeployServiceUndeployWasNotCalled();
  }

  @Test
  void execute_withCustomProperties() throws Exception {
    // Given
    undeployMojo.namespace = "  custom-namespace  ";
    // When
    undeployMojo.execute();
    // Then
    assertUndeployServiceUndeployWasCalled();
    assertThat(undeployMojo.getResources())
        .hasFieldOrPropertyWithValue("namespace", "custom-namespace");
  }

  private void assertUndeployServiceUndeployWasCalled() throws Exception {
    verify(mockedUndeployService, times(1)).undeploy(eq(Collections.singletonList(mockResourceDir)), any(), eq(mockManifest));
  }

  private void assertUndeployServiceUndeployWasNotCalled() {
    verify(jKubeServiceHubMockedConstruction.constructed().get(0), times(0)).getUndeployService();
  }
}