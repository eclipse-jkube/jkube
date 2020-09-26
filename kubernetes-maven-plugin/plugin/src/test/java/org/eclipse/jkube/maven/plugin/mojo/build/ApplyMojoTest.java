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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jkube.kit.config.service.ApplyService;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplyMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File kubernetesManifestFile;

  @Mocked
  private MavenProject mavenProject;
  @Mocked
  private Settings mavenSettings;
  @Mocked
  private ApplyService applyService;

  private ApplyMojo applyMojo;

  @Before
  public void setUp() throws IOException {
    kubernetesManifestFile = temporaryFolder.newFile("kubernetes.yml");
    applyMojo = new ApplyMojo() {
      {
        project = mavenProject;
        settings = mavenSettings;
        kubernetesManifest = kubernetesManifestFile;
      }
    };
  }

  @After
  public void tearDown() {
    mavenProject = null;
    applyMojo = null;
    applyService = null;
  }

  @Test
  public void executeInternalWithRecreateProperty() throws Exception {
    // Given
    final Properties properties = new Properties();
    applyMojo.recreate = true;

    new Expectations() {
      {
        mavenProject.getProperties();
        result = properties;
        mavenProject.getBuild().getOutputDirectory();
        result = "target/classes";
        mavenProject.getBuild().getDirectory();
        result = "target";
        mavenProject.getArtifactId();
        result = "artifact-id";
        mavenProject.getVersion();
        result = "1337";
        mavenProject.getDescription();
        result = "A description from Maven";
        mavenProject.getParent();
        result = null;
        applyService.isRecreateMode();
        result = applyMojo.recreate;
      }
    };
    // When
    applyMojo.init();
    applyMojo.executeInternal();
    // Then
    assertThat(applyMojo.applyService.isRecreateMode(), is(true));
  }

  @Test
  public void executeInternalWithoutRecreateProperty() throws Exception {
    // Given
    final Properties properties = new Properties();

    new Expectations() {
      {
        mavenProject.getProperties();
        result = properties;
        mavenProject.getBuild().getOutputDirectory();
        result = "target/classes";
        mavenProject.getBuild().getDirectory();
        result = "target";
        mavenProject.getArtifactId();
        result = "artifact-id";
        mavenProject.getVersion();
        result = "1337";
        mavenProject.getDescription();
        result = "A description from Maven";
        mavenProject.getParent();
        result = null;
        applyService.isRecreateMode();
        result = applyMojo.recreate;
      }
    };
    // When
    applyMojo.init();
    applyMojo.executeInternal();
    // Then
    assertThat(applyMojo.applyService.isRecreateMode(), is(false));
  }

}
