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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UndeployMojoTest {

  @TempDir
  private Path temporaryFolder;
  private MockedStatic<MavenUtil> mavenUtilMockedStatic;
  private UndeployService mockedUndeployService;
  private MockedStatic<OpenshiftHelper> openshiftHelperMockedStatic;
  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private UndeployMojo undeployMojo;

  @BeforeEach
  void setUp() throws IOException {
    File buildDirectory = new File("target");
    KitLogger kitLogger = new KitLogger.SilentLogger();
    openshiftHelperMockedStatic = mockStatic(OpenshiftHelper.class);
    openshiftHelperMockedStatic.when(() -> OpenshiftHelper.isOpenShift(any())).thenReturn(false);
    mavenUtilMockedStatic = mockStatic(MavenUtil.class);
    mockedUndeployService = mock(UndeployService.class);
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class, (mock, ctx) -> {
      when(mock.getConfiguration()).thenReturn(JKubeConfiguration.builder()
          .project(JavaProject.builder()
              .buildDirectory(buildDirectory)
              .build())
          .build());
      when(mock.getLog()).thenReturn(kitLogger);
      when(mock.getPlatformMode()).thenReturn(RuntimeMode.KUBERNETES);
      when(mock.getUndeployService()).thenReturn(mockedUndeployService);
      when(mock.getSummaryService()).thenReturn(new SummaryService(buildDirectory, kitLogger, false));
    });
    undeployMojo = new UndeployMojo() {{
      this.resourceDir = Files.createDirectory(temporaryFolder.resolve("resources")).toFile();
      this.kubernetesManifest = Files.createFile(temporaryFolder.resolve("kubernetes.yml")).toFile();
      project = new MavenProject();
      settings = new Settings();
      mojoExecution = new MojoExecution(new MojoDescriptor());
      mojoExecution.getMojoDescriptor().setPluginDescriptor(new PluginDescriptor());
      mojoExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("k8s");
      mojoExecution.getMojoDescriptor().setGoal("undeploy");
    }};
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

  @Test
  void execute_whenUndeployServiceFails_thenExceptionThrown() throws IOException {
    // Given
    doThrow(new IOException("failure"))
        .when(mockedUndeployService).undeploy(any(), any(), any());

    // When
    assertThatExceptionOfType(MojoExecutionException.class)
        .isThrownBy(() -> undeployMojo.execute())
        .withMessage("failure");
  }

  private void assertUndeployServiceUndeployWasCalled() throws Exception {
    verify(mockedUndeployService, times(1))
      .undeploy(eq(Collections.singletonList(temporaryFolder.resolve("resources").toFile())), any(),
        eq(temporaryFolder.resolve("kubernetes.yml").toFile()));
  }

  private void assertUndeployServiceUndeployWasNotCalled() throws IOException {
    verify(mockedUndeployService, times(0))
        .undeploy(any(), any(), any());
  }
}
