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

import java.util.Properties;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class HelmPushMojoTest {

  @Mocked
  KitLogger logger;
  @Mocked
  MavenProject mavenProject;
  @Mocked
  HelmService helmService;
  @Mocked
  SecDispatcher secDispatcher;

  private Properties mavenProperties;

  private HelmPushMojo helmPushMojo;

  @Before
  public void setUp() throws Exception {
    mavenProperties = new Properties();
    helmPushMojo = new HelmPushMojo();
    helmPushMojo.helm = new HelmConfig();
    helmPushMojo.project = mavenProject;
    helmPushMojo.settings = new Settings();
    helmPushMojo.securityDispatcher = secDispatcher;
    // @formatter:off
    new Expectations(helmPushMojo) {{
      helmPushMojo.getKitLogger(); result = logger; minTimes = 0;
      mavenProject.getProperties(); result = mavenProperties; minTimes = 0;
      secDispatcher.decrypt(anyString);
      result = new Delegate<String>() {String delegate(String arg) {return arg;}}; minTimes = 0;
    }};
    // @formatter:on
  }

  @After
  public void tearDown() {
    mavenProject = null;
    helmPushMojo = null;
    helmService = null;
  }

  @Test
  public void executeInternalWithValidXMLConfigShouldGenerate() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.executeInternal();
    // Then
    assertThat(helmPushMojo.helm)
        .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml")
        .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.ARTIFACTORY);
    assertHelmServiceUpload();
  }

  @Test
  public void executeInternalWithInvalidXMLRepositoryConfigShouldFail() {
    // Given
    helmPushMojo.helm.setSnapshotRepository(new HelmRepository());
    helmPushMojo.helm.getSnapshotRepository().setName("SNAP-REPO");
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.executeInternal());
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  public void executeInternalWithMissingXMLRepositoryConfigShouldFail() {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337"; // Will require the stable repository
    }};
    // @formatter:on
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.executeInternal());
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  public void executeInternalWithValidXMLConfigAndUploadErrorShouldFail() throws Exception {
    // Given
    helmPushMojo.helm.setStableRepository(completeValidRepository());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337";
      helmService.uploadHelmChart(null, withNotNull(), withNotNull());
      result = new BadUploadException("Error uploading helm chart");
    }};
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.executeInternal());
    // Then
    assertThat(result).hasMessage("Error uploading helm chart");
  }

  @Test
  public void executeInternalWithValidPropertiesConfigShouldGenerate() throws Exception {
    // Given
    mavenProperties.put("jkube.helm.snapshotRepository.name", "props repo");
    mavenProperties.put("jkube.helm.snapshotRepository.type", "nExus");
    mavenProperties.put("jkube.helm.snapshotRepository.url", "http://example.com/url");
    mavenProperties.put("jkube.helm.snapshotRepository.username", "propsUser");
    mavenProperties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.executeInternal();
    // Then
    assertThat(helmPushMojo.helm)
        .hasFieldOrPropertyWithValue("snapshotRepository.name", "props repo")
        .hasFieldOrPropertyWithValue("snapshotRepository.url", "http://example.com/url")
        .hasFieldOrPropertyWithValue("snapshotRepository.username", "propsUser")
        .hasFieldOrPropertyWithValue("snapshotRepository.password", "propS3cret")
        .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.NEXUS)
        .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml");
    assertHelmServiceUpload();
  }

  @Test
  public void executeInternalWithValidPropertiesAndXMLConfigShouldGenerateWithPropertiesTakingPrecedence() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    mavenProperties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.executeInternal();
    // Then
    assertThat(helmPushMojo.helm)
        .hasFieldOrPropertyWithValue("snapshotRepository.name", "SNAP-REPO")
        .hasFieldOrPropertyWithValue("snapshotRepository.url", "https://example.com/artifactory")
        .hasFieldOrPropertyWithValue("snapshotRepository.username", "User")
        .hasFieldOrPropertyWithValue("snapshotRepository.password", "propS3cret")
        .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.ARTIFACTORY)
        .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml");
    assertHelmServiceUpload();
  }

  @Test
  public void executeInternalWithoutUsernameShouldFail() {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    helmPushMojo.helm.getSnapshotRepository().setUsername(null);
    helmPushMojo.helm.getSnapshotRepository().setPassword(null);
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.executeInternal());
    // Then
    assertThat(result).hasMessage("No credentials found for SNAP-REPO in configuration or settings.xml server list.");
  }

  @Test
  public void executeInternalWithUsernameWithoutPasswordShouldFail() {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    helmPushMojo.helm.getSnapshotRepository().setPassword(null);
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.executeInternal());
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO has a username but no password defined.");
  }

  @Test
  public void executeInternalWithValidMavenSettingsShouldGenerate() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    helmPushMojo.helm.getSnapshotRepository().setUsername(null);
    helmPushMojo.helm.getSnapshotRepository().setPassword(null);
    helmPushMojo.settings.addServer(completeValidServer());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.executeInternal();
    // Then
    assertThat(helmPushMojo.helm)
        .hasFieldOrPropertyWithValue("snapshotRepository.username", "mavenUser")
        .hasFieldOrPropertyWithValue("snapshotRepository.password", "mavenPassword")
        .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.ARTIFACTORY)
        .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml");
    assertHelmServiceUpload();
  }

  @Test
  public void executeInternalWithMavenSettingsWithoutUsernameShouldFail() {
    // Given
    helmPushMojo.helm = new HelmConfig();
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    helmPushMojo.helm.getSnapshotRepository().setUsername(null);
    helmPushMojo.helm.getSnapshotRepository().setPassword(null);
    helmPushMojo.settings.addServer(completeValidServer());
    helmPushMojo.settings.getServer("SNAP-REPO").setUsername(null);
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.executeInternal());
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO was found in server list but has no username/password.");
  }

  @Test
  public void executeInternalWithSkipShouldSkipExecution() throws Exception {
    // Given
    helmPushMojo.skip = true;
    // When
    helmPushMojo.executeInternal();
    // Then
    new Verifications() {{
      HelmService.uploadHelmChart(null, helmPushMojo.helm, withNotNull());
      times = 0;
    }};
  }

  @Test
  public void canExecuteWithSkipShouldReturnFalse() throws Exception {
    // Given
    helmPushMojo.skip = true;
    // When
    final boolean result = helmPushMojo.canExecute();
    // Then
    assertThat(result).isFalse();
  }

  private void assertHelmServiceUpload() throws Exception {
    // @formatter:off
    new Verifications() {{
      HelmService.uploadHelmChart(null, helmPushMojo.helm, withNotNull());
      times = 1;
    }};
    // @formatter:on
  }

  private static HelmRepository completeValidRepository() {
    final HelmRepository repository = new HelmRepository();
    repository.setName("SNAP-REPO");
    repository.setType("ArtIFACTORY");
    repository.setUrl("https://example.com/artifactory");
    repository.setUsername("User");
    repository.setPassword("S3cret");
    return repository;
  }

  private static Server completeValidServer() {
    final Server server = new Server();
    server.setId("SNAP-REPO");
    server.setUsername("mavenUser");
    server.setPassword("mavenPassword");
    return server;
  }
}
