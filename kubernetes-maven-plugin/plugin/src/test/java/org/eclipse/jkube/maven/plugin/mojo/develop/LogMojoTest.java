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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.PodLogService;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class LogMojoTest {

  private MockedConstruction<JKubeServiceHub> jKubeServiceHubMockedConstruction;
  private MockedConstruction<PodLogService> podLogServiceMockedConstruction;
  private File kubernetesManifestFile;
  private MavenProject mavenProject;

  private LogMojo logMojo;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    jKubeServiceHubMockedConstruction = mockConstruction(JKubeServiceHub.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
          final OpenShiftClient oc = mock(OpenShiftClient.class, RETURNS_DEEP_STUBS);
          doReturn(oc).when(oc).adapt(OpenShiftClient.class);
          when(mock.getClient()).thenReturn(oc);
        });
    podLogServiceMockedConstruction = mockConstruction(PodLogService.class);
    kubernetesManifestFile = Files.createTempFile(temporaryFolder, "kubernetes", ".yml").toFile();
    mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    // @formatter:off
    logMojo = new LogMojo() { {
      project = mavenProject;
      settings = mock(Settings.class);
      interpolateTemplateParameters = false;
      kubernetesManifest = kubernetesManifestFile;
    }};
    // @formatter:on
  }

  @AfterEach
  void tearDown() {
    jKubeServiceHubMockedConstruction.close();
    podLogServiceMockedConstruction.close();
    mavenProject = null;
    logMojo = null;
  }

  @Test
  void execute() throws Exception {
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

  @Test
  void execute_whenSkipTrue_shouldDoNothing() throws Exception {
    // Given
    KitLogger kitLogger = spy(new KitLogger.SilentLogger());
    Settings mockSettings = mock(Settings.class);
    MojoExecution mockExecution = new MojoExecution(new MojoDescriptor());
    mockExecution.getMojoDescriptor().setPluginDescriptor(new PluginDescriptor());
    mockExecution.getMojoDescriptor().setGoal("log");
    mockExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("k8s");
    LogMojo skipLogMojo = new LogMojo() {
      @Override
      protected KitLogger createLogger(String prefix) {
        return kitLogger;
      }
      {
      project = mavenProject;
      settings = mockSettings;
      interpolateTemplateParameters = false;
      kubernetesManifest = kubernetesManifestFile;
      skip = true;
      mojoExecution = mockExecution;
    }};
    // When
    skipLogMojo.execute();
    // Then
    verify(kitLogger).info("`%s` goal is skipped.", "k8s:log");
    assertThat(jKubeServiceHubMockedConstruction.constructed()).isEmpty();
    assertThat(podLogServiceMockedConstruction.constructed()).isEmpty();
  }
}
