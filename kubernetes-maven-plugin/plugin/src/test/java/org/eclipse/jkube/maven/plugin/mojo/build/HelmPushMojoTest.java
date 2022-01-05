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

import java.util.HashMap;
import java.util.Properties;

import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;
import org.eclipse.jkube.kit.resource.helm.HelmService;

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
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
  MavenProject mavenProject;
  @Mocked
  Build mavenBuild;
  @Mocked
  HelmService helmService;
  @Mocked
  SecDispatcher secDispatcher;
  @Mocked
  MojoExecution mojoExecution;
  @Mocked
  MojoDescriptor mojoDescriptor;


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
    helmPushMojo.mojoExecution = mojoExecution;
    // @formatter:off
    new Expectations(helmPushMojo) {{
      mavenProject.getProperties(); result = mavenProperties; minTimes = 0;
      mavenProject.getBuild(); result = mavenBuild; minTimes = 0;
      mavenBuild.getOutputDirectory(); result = "target/classes"; minTimes = 0;
      mavenBuild.getDirectory(); result = "target"; minTimes = 0;
      mojoExecution.getMojoDescriptor(); result = mojoDescriptor; minTimes = 0;
      mojoDescriptor.getFullGoalName(); result = "k8s:helm-push"; minTimes = 0;
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
  public void execute_withValidXMLConfig_shouldUpload() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.execute();
    // Then
    assertThat(helmPushMojo.helm)
        .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml")
        .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.ARTIFACTORY);
    assertHelmServiceUpload();
  }

  @Test
  public void execute_withValidXMLConfigAndUploadError_shouldFail() throws Exception {
    // Given
    helmPushMojo.helm.setStableRepository(completeValidRepository());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337";
      helmService.uploadHelmChart(withNotNull());
      result = new BadUploadException("Error uploading helm chart");
    }};
    // When
    final MojoExecutionException result = assertThrows(MojoExecutionException.class,
        () -> helmPushMojo.execute());
    // Then
    assertThat(result).hasMessage("Error uploading helm chart");
  }

  @Test
  public void execute_withValidPropertiesConfig_shouldUpload() throws Exception {
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
    helmPushMojo.execute();
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
  public void execute_withValidPropertiesAndXMLConfig_shouldGenerateWithPropertiesTakingPrecedence() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    mavenProperties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.execute();
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
  public void execute_withValidMavenSettings_shouldUpload() throws Exception {
    // Given
    helmPushMojo.settings.addServer(completeValidServer());
    // @formatter:off
    new Expectations() {{
      mavenProject.getVersion(); result = "1337-SNAPSHOT";
    }};
    // @formatter:on
    // When
    helmPushMojo.execute();
    // Then
    assertThat(helmPushMojo.jkubeServiceHub.getConfiguration().getRegistryConfig().getSettings()).singleElement()
        .isEqualTo(RegistryServerConfiguration.builder()
            .id("SNAP-REPO").username("mavenUser").password("mavenPassword").configuration(new HashMap<>()).build());
    // @formatter:off
    new Verifications() {{
      helmService.uploadHelmChart(helmPushMojo.helm);
      times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void execute_withSkip_shouldSkipExecution() throws Exception {
    // Given
    helmPushMojo.skip = true;
    // When
    helmPushMojo.execute();
    // Then
    new Verifications() {{
      helmService.uploadHelmChart(helmPushMojo.helm);
      times = 0;
    }};
  }

  @Test
  public void canExecute_withSkip_shouldReturnFalse() {
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
      helmService.uploadHelmChart(helmPushMojo.helm);
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
