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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.execution.MavenSession;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.mockito.AdditionalAnswers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelmPushMojoTest {

  @TempDir
  private Path projectDir;
  private MavenSession mavenSession;
  private MojoExecution mojoExecution;

  private HelmPushMojo helmPushMojo;

  @BeforeEach
  void setUp() throws Exception {
    helmPushMojo = new HelmPushMojo();
    mavenSession = mock(MavenSession.class);
    mojoExecution = mock(MojoExecution.class, RETURNS_DEEP_STUBS);
    helmPushMojo.helm = new HelmConfig();
    helmPushMojo.project = new MavenProject();
    helmPushMojo.session = mavenSession;
    helmPushMojo.settings = new Settings();
    helmPushMojo.securityDispatcher = mock(SecDispatcher.class);
    helmPushMojo.mojoExecution = mojoExecution;
    helmPushMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmPushMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmPushMojo.project.setFile(projectDir.resolve("target").toFile());
    when(mojoExecution.getMojoDescriptor().getFullGoalName()).thenReturn("k8s:helm-push");
    when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:helm-push"));
    when(mojoExecution.getGoal()).thenReturn("k8s:helm-push");
    when(helmPushMojo.securityDispatcher.decrypt(anyString()))
      .thenReturn(String.valueOf(AdditionalAnswers.returnsFirstArg()));
  }

  @AfterEach
  void tearDown() {
    helmPushMojo = null;
  }

  @Test
  void execute_withValidXMLConfig_shouldUpload() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
      helmPushMojo.project.setVersion("1337-SNAPSHOT");
      // When
      helmPushMojo.execute();
      // Then
      assertThat(helmPushMojo.helm)
          .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml")
          .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepoType.ARTIFACTORY);
      assertHelmServiceUpload(helmServiceMockedConstruction);
    }
  }

  @Test
  void execute_withValidXMLConfigAndUploadError_shouldFail() {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class,
      (mock, ctx) -> doThrow(new BadUploadException("Error uploading helm chart")).when(mock).uploadHelmChart(any())
    )) {
      // Given
      when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:helm-push"));
      when(mojoExecution.getGoal()).thenReturn("k8s:helm-push");
      helmPushMojo.helm.setStableRepository(completeValidRepository());
      helmPushMojo.project.setVersion("1337");
      // When & Then
      assertThatExceptionOfType(MojoExecutionException.class)
          .isThrownBy(() -> helmPushMojo.execute())
          .withMessage("Error uploading helm chart");
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void execute_withValidPropertiesConfig_shouldUpload() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      helmPushMojo.project.getProperties().put("jkube.helm.snapshotRepository.name", "props repo");
      helmPushMojo.project.getProperties().put("jkube.helm.snapshotRepository.type", "nExus");
      helmPushMojo.project.getProperties().put("jkube.helm.snapshotRepository.url", "http://example.com/url");
      helmPushMojo.project.getProperties().put("jkube.helm.snapshotRepository.username", "propsUser");
      helmPushMojo.project.getProperties().put("jkube.helm.snapshotRepository.password", "propS3cret");
      helmPushMojo.project.setVersion("1337-SNAPSHOT");
      when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:helm-push"));
      when(mojoExecution.getGoal()).thenReturn("k8s:helm-push");
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
      assertHelmServiceUpload(helmServiceMockedConstruction);
    }
  }

  @Test
  void execute_withValidPropertiesAndXMLConfig_shouldGenerateWithPropertiesTakingPrecedence() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      helmPushMojo.helm.setSnapshotRepository(completeValidRepository());
      helmPushMojo.project.getProperties().put("jkube.helm.snapshotRepository.password", "propS3cret");
      helmPushMojo.project.setVersion("1337-SNAPSHOT");
      when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:helm-push"));
      when(mojoExecution.getGoal()).thenReturn("k8s:helm-push");
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
      assertHelmServiceUpload(helmServiceMockedConstruction);
    }
  }

  @Test
  void execute_withValidMavenSettings_shouldUpload() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      helmPushMojo.settings.addServer(completeValidServer());
      helmPushMojo.project.setVersion("1337-SNAPSHOT");
      when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:helm-push"));
      when(mojoExecution.getGoal()).thenReturn("k8s:helm-push");
      // When
      helmPushMojo.execute();
      // Then
      assertThat(helmPushMojo.jkubeServiceHub.getConfiguration().getRegistryConfig().getSettings()).singleElement()
          .isEqualTo(RegistryServerConfiguration.builder()
              .id("SNAP-REPO").username("mavenUser").password("mavenPassword").configuration(new HashMap<>()).build());
      assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
      verify(helmServiceMockedConstruction.constructed().get(0), times(1)).uploadHelmChart(helmPushMojo.helm);
    }
  }

  @Test
  void execute_withSkip_shouldSkipExecution() throws Exception {
    try (MockedConstruction<HelmService> helmServiceMockedConstruction = mockConstruction(HelmService.class)) {
      // Given
      when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:helm-push"));
      when(mojoExecution.getGoal()).thenReturn("k8s:helm-push");
      helmPushMojo.skip = true;
      // When
      helmPushMojo.execute();
      // Then
      assertThat(helmServiceMockedConstruction.constructed()).isEmpty();
    }
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

  private void assertHelmServiceUpload(MockedConstruction<HelmService> helmServiceMockedConstruction) throws Exception {
    assertThat(helmServiceMockedConstruction.constructed()).hasSize(1);
    verify(helmServiceMockedConstruction.constructed().get(0),times(1)).uploadHelmChart(helmPushMojo.helm);
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
