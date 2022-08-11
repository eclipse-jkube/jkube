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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.mockito.AdditionalAnswers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelmPushMojoTest {

  MavenProject mavenProject;
  Build mavenBuild;
  HelmService helmService;
  SecDispatcher secDispatcher;
  MojoExecution mojoExecution;
  MojoDescriptor mojoDescriptor;

  private Properties mavenProperties;

  private HelmPushMojo helmPushMojo;

  @BeforeEach
  void setUp() throws Exception {
    mavenProject = mock(MavenProject.class);
    mavenBuild = mock(Build.class);
    helmService = mock(HelmService.class);
    secDispatcher = mock(SecDispatcher.class);
    mojoExecution = mock(MojoExecution.class);
    mojoDescriptor = mock(MojoDescriptor.class);
    mavenProperties = new Properties();
    helmPushMojo = new HelmPushMojo();
    helmPushMojo.helm = new HelmConfig();
    helmPushMojo.project = mavenProject;
    helmPushMojo.settings = new Settings();
    helmPushMojo.securityDispatcher = secDispatcher;
    helmPushMojo.mojoExecution = mojoExecution;
    when(mavenProject.getProperties()).thenReturn(mavenProperties);
    when(mavenProject.getBuild()).thenReturn(mavenBuild);
    when(mavenBuild.getOutputDirectory()).thenReturn("target/classes");
    when(mavenBuild.getDirectory()).thenReturn("target");
    when(mojoExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);
    when(mojoDescriptor.getFullGoalName()).thenReturn("k8s:helm-push");
    when(secDispatcher.decrypt(anyString())).thenReturn(String.valueOf(AdditionalAnswers.returnsFirstArg()));
  }

  @AfterEach
  void tearDown() {
    mavenProject = null;
    helmPushMojo = null;
    helmService = null;
  }

  @Test
  void execute_withValidXMLConfig_shouldUpload() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    when(mavenProject.getVersion()).thenReturn("1337-SNAPSHOT");
    // When
    helmPushMojo.execute();
    // Then
    assertThat(helmPushMojo.helm)
        .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml")
        .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.ARTIFACTORY);
    assertHelmServiceUpload();
  }

  @Test
  void execute_withValidXMLConfigAndUploadError_shouldFail() throws Exception {
    // Given
    helmPushMojo.helm.setStableRepository(completeValidRepository());
    when(mavenProject.getVersion()).thenReturn("1337");
    doThrow(new BadUploadException("Error uploading helm chart")).when(helmService).uploadHelmChart(any());
    // When & Then
    assertThatExceptionOfType(MojoExecutionException.class)
            .isThrownBy(() -> helmPushMojo.execute())
            .withMessage("Error uploading helm chart");
  }

  @Test
  void execute_withValidPropertiesConfig_shouldUpload() throws Exception {
    // Given
    mavenProperties.put("jkube.helm.snapshotRepository.name", "props repo");
    mavenProperties.put("jkube.helm.snapshotRepository.type", "nExus");
    mavenProperties.put("jkube.helm.snapshotRepository.url", "http://example.com/url");
    mavenProperties.put("jkube.helm.snapshotRepository.username", "propsUser");
    mavenProperties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    when(mavenProject.getVersion()).thenReturn("1337-SNAPSHOT");
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
  void execute_withValidPropertiesAndXMLConfig_shouldGenerateWithPropertiesTakingPrecedence() throws Exception {
    // Given
    helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
    mavenProperties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    when(mavenProject.getVersion()).thenReturn("1337-SNAPSHOT");
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
  void execute_withValidMavenSettings_shouldUpload() throws Exception {
    // Given
    helmPushMojo.settings.addServer(completeValidServer());
    when(mavenProject.getVersion()).thenReturn("1337-SNAPSHOT");
    // When
    helmPushMojo.execute();
    // Then
    assertThat(helmPushMojo.jkubeServiceHub.getConfiguration().getRegistryConfig().getSettings()).singleElement()
        .isEqualTo(RegistryServerConfiguration.builder()
            .id("SNAP-REPO").username("mavenUser").password("mavenPassword").configuration(new HashMap<>()).build());
    verify(helmService,times(1)).uploadHelmChart(helmPushMojo.helm);
  }

  @Test
  void execute_withSkip_shouldSkipExecution() throws Exception {
    // Given
    helmPushMojo.skip = true;
    // When
    helmPushMojo.execute();
    // Then
    verify(helmService,times(0)).uploadHelmChart(helmPushMojo.helm);
  }

  @Test
  void shouldSkip_withSkip_shouldReturnTrue() {
    // Given
    helmPushMojo.skip = true;
    // When
    final boolean result = helmPushMojo.shouldSkip();
    // Then
    assertThat(result).isTrue();
  }

  private void assertHelmServiceUpload() throws Exception {
    verify(helmService,times(1)).uploadHelmChart(helmPushMojo.helm);
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
