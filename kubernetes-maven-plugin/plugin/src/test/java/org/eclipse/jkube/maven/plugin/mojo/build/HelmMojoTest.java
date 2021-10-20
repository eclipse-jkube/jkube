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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.fail;

public class HelmMojoTest {
  @Mocked
  JavaProject javaProject;
  @Mocked
  HelmService helmService;
  @Mocked
  KitLogger logger;
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private HelmMojo helmMojo;

  @Before
  public void setUp() {
    helmMojo = new HelmMojo();
    helmMojo.javaProject = javaProject;
    helmMojo.log = logger;
    new Expectations() {{
      javaProject.getProperties();
      result = new Properties();
    }};
  }

  @After
  public void tearDown() {
    javaProject = null;
    helmMojo = null;
    helmService = null;
  }

  @Test
  public void executeInternalCallsHelmService() throws MojoExecutionException, IOException {
    // Given
    File kubernetesManifest = new File(temporaryFolder.getRoot(), "target/classes/META-INF/jkube/kubernetes.yml");
    File tempateDir = new File(temporaryFolder.getRoot(),"target/classes/jkube");
    boolean templateDirCreated = tempateDir.mkdirs();
    helmMojo.kubernetesManifest = kubernetesManifest;
    helmMojo.kubernetesTemplate = tempateDir;

    // When
    helmMojo.executeInternal();

    // Then
    assertThat(templateDirCreated).isTrue();
    new Verifications() {{
      helmService.generateHelmCharts(logger, withNotNull());
      times = 1;
    }};
  }

  @Test(expected = MojoExecutionException.class)
  public void executeInternalWithNoConfigGenerateThrowsExceptionShouldRethrowWithMojoExecutionException()
    throws Exception {

    // Given
    new Expectations() {{
      HelmService.generateHelmCharts(logger, withNotNull());
      result = new IOException("Exception is thrown");
    }};
    // When
    helmMojo.executeInternal();
    // Then
    fail();
  }
}
