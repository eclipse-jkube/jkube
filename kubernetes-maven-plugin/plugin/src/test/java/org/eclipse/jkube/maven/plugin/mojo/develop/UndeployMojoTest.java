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
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class UndeployMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private MavenUtil mavenUtil;
  @Mocked
  private MavenProject mavenProject;
  @Mocked
  private Settings mavenSettings;
  @Mocked
  private KitLogger mockedKitLogger;
  @Mocked
  private MojoExecution mockedMojoExecution;
  private File mockManifest;
  private File mockResourceDir;
  private UndeployMojo undeployMojo;

  @Before
  public void setUp() throws IOException {
    mockManifest = temporaryFolder.newFile();
    mockResourceDir = temporaryFolder.newFolder();
    // @formatter:off
    undeployMojo = new UndeployMojo() {{
      resourceDir = mockResourceDir;
      kubernetesManifest = mockManifest;
      project = mavenProject;
      settings = mavenSettings;
      log = mockedKitLogger;
      mojoExecution = mockedMojoExecution;
    }};
    new Expectations() {{
      mavenProject.getProperties(); result = new Properties();
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
    undeployMojo = null;
  }

  @Test
  public void execute() throws Exception {
    // When
    undeployMojo.execute();
    // Then
    assertUndeployServiceUndeployWasCalled();
  }

  @Test
  public void execute_whenSkipUndeployEnabled_thenUndeployServiceNotCalled() throws Exception {
    // Given
    undeployMojo.skipUndeploy = true;
    new Expectations() {{
      mockedMojoExecution.getMojoDescriptor().getFullGoalName(); result = "k8s:undeploy";
    }};

    // When
    undeployMojo.execute();

    // Then
    assertUndeployServiceUndeployWasNotCalled();
  }

  @Test
  public void executeWithCustomProperties() throws Exception {
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
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getUndeployService().undeploy(Collections.singletonList(mockResourceDir), withNotNull(), mockManifest);
      times = 1;
    }};
    // @formatter:on
  }

  private void assertUndeployServiceUndeployWasNotCalled() {
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getUndeployService();
      times = 0;
    }};
    // @formatter:on
  }
}