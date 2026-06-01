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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftRemoteDevMojoTest {

  private OpenShiftRemoteDevMojo openShiftRemoteDevMojo;
  private MockedConstruction<RemoteDevelopmentService> remoteDevelopmentService;

  @BeforeEach
  void setUp() {
    final MavenProject mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    openShiftRemoteDevMojo = new OpenShiftRemoteDevMojo() {{
      project = mavenProject;
      settings = mock(Settings.class, RETURNS_DEEP_STUBS);
      interpolateTemplateParameters = false;
    }};
    remoteDevelopmentService = mockConstruction(RemoteDevelopmentService.class);
  }

  @AfterEach
  void tearDown() {
    remoteDevelopmentService.close();
  }

  @Test
  void getLogPrefix_shouldReturnOpenShiftPrefix() {
    assertThat(openShiftRemoteDevMojo.getLogPrefix()).isEqualTo("oc: ");
  }

  @Test
  void execute_whenSkipTrue_shouldDoNothing() throws Exception {
    // Given
    KitLogger kitLogger = spy(new KitLogger.SilentLogger());
    final MavenProject mavenProject = mock(MavenProject.class);
    when(mavenProject.getProperties()).thenReturn(new Properties());
    MojoExecution mockExecution = new MojoExecution(new MojoDescriptor());
    mockExecution.getMojoDescriptor().setPluginDescriptor(new PluginDescriptor());
    mockExecution.getMojoDescriptor().setGoal("remote-dev");
    mockExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("oc");

    OpenShiftRemoteDevMojo skipOpenShiftRemoteDevMojo = new OpenShiftRemoteDevMojo() {
      @Override
      protected KitLogger createLogger(String prefix) {
        return kitLogger;
      }
      {
        project = mavenProject;
        settings = mock(Settings.class, RETURNS_DEEP_STUBS);
        interpolateTemplateParameters = false;
        skip = true;
        mojoExecution = mockExecution;
      }
    };

    // When
    skipOpenShiftRemoteDevMojo.execute();

    // Then
    assertThat(remoteDevelopmentService.constructed()).isEmpty();
    verify(kitLogger).info("`%s` goal is skipped.", "oc:remote-dev");
  }
}
