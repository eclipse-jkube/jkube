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
package org.eclipse.jkube.kit.resource.helm;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.InstanceOf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HelmServiceTest {

  private HelmConfig.HelmConfigBuilder helmConfig;
  private JKubeConfiguration jKubeConfiguration;
  private HelmService helmService;

  @Before
  public void setUp() throws Exception {
    helmConfig = HelmConfig.builder();
    jKubeConfiguration = JKubeConfiguration.builder()
        .registryConfig(RegistryConfig.builder().settings(new ArrayList<>()).build()).build();
    helmService = new HelmService(jKubeConfiguration, new KitLogger.SilentLogger());
  }

  @After
  public void tearDown() {
    helmService = null;
    helmConfig = null;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test(expected = IOException.class)
  public void prepareSourceDirValidWithNoYamls() throws Exception {
    File file = mock(File.class);
    // Given
    when(file.isDirectory()).thenReturn(true);
    // When
    HelmService.prepareSourceDir(helmConfig.build(), HelmConfig.HelmType.OPENSHIFT);
  }

  @Test
  public void createChartYaml() throws Exception {
    File outputDir = mock(File.class);
    ResourceUtil resourceUtil1 = mock(ResourceUtil.class);
    // Given
    helmConfig.chart("Chart Name").version("1337");
    // When
    HelmService.createChartYaml(helmConfig.build(), outputDir);
    // Then
    ArgumentCaptor<Chart> argumentCaptor = ArgumentCaptor.forClass(Chart.class);
    verify(resourceUtil1).save(notNull(), argumentCaptor.capture(), ResourceFileType.yaml);
    assertThat(argumentCaptor.capture())
            .hasFieldOrPropertyWithValue("apiVersion", "v1")
            .hasFieldOrPropertyWithValue("name", "Chart Name")
            .hasFieldOrPropertyWithValue("version", "1337");
  }

  @Test
  public void createChartYamlWithDependencies() throws Exception {
    File outputDir = mock(File.class);
    ResourceUtil resourceUtil = mock(ResourceUtil.class);
    // Given
    HelmDependency helmDependency = new HelmDependency()
        .toBuilder()
        .name("nginx")
        .version("1.2.3.")
        .repository("repository")
        .build();
    helmConfig.chart("Chart Name").version("1337")
        .dependencies(Collections.singletonList(helmDependency));
    // When
    HelmService.createChartYaml(helmConfig.build(), outputDir);
    // Then
    ArgumentCaptor<Chart> argumentCaptor = ArgumentCaptor.forClass(Chart.class);
    verify(resourceUtil).save(notNull(), argumentCaptor.capture(), ResourceFileType.yaml);
    assertThat(argumentCaptor.capture())
            .hasFieldOrPropertyWithValue("apiVersion", "v1")
            .hasFieldOrPropertyWithValue("name", "Chart Name")
            .hasFieldOrPropertyWithValue("version", "1337")
            .hasFieldOrPropertyWithValue("dependencies",
                    Collections.singletonList(helmDependency));
  }

  @Test
  public void uploadChart_withValidRepository_shouldUpload()
      throws IOException, BadUploadException {
    HelmUploader helmUploader = mock(HelmUploader.class);
    //Given
    final HelmRepository helmRepository = completeValidRepository().name("stable-repo").build();
    helmConfig
        .types(Collections.singletonList(HelmType.KUBERNETES))
        .chart("chartName")
        .version("1337")
        .chartExtension("tar.gz")
        .outputDir("target")
        .tarballOutputDir("target")
        .snapshotRepository(HelmRepository.builder().name("Snapshot-Repo").build())
        .stableRepository(helmRepository);
    // @formatter:off
    new Expectations() {{
      helmUploader.uploadSingle(withInstanceOf(File.class), helmRepository);
    }};
    // @formatter:on
    doNothing().when(helmUploader).uploadSingle(new File.class,helmRepository);
    // When
    helmService.uploadHelmChart(helmConfig.build());
    // Then
    new Verifications() {{
      String fileName = "chartName-1337-helm.tar.gz";
      File file;
      helmUploader.uploadSingle(file = withCapture(), helmRepository);
      assertThat(file)
          .hasName(fileName);
    }};
  }

  @Test
  public void uploadHelmChart_withInvalidRepositoryConfiguration_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(HelmRepository.builder().name("INVALID").build())
        .build();
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> helmService.uploadHelmChart(helm));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  public void uploadHelmChart_withMissingRepositoryConfiguration_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT").build();
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> helmService.uploadHelmChart(helm));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  public void uploadHelmChart_withServerConfigurationWithoutUsername_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(completeValidRepository().username(null).build()).build();
    jKubeConfiguration.getRegistryConfig().getSettings()
        .add(RegistryServerConfiguration.builder().id("SNAP-REPO").build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        helmService.uploadHelmChart(helm));
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO was found in server list but has no username/password.");
  }

  @Test
  public void uploadHelmChart_withServerConfigurationWithoutPassword_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(completeValidRepository().password(null).build()).build();
    jKubeConfiguration.getRegistryConfig().getSettings()
        .add(RegistryServerConfiguration.builder().id("SNAP-REPO").build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        helmService.uploadHelmChart(helm));
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO has a username but no password defined.");
  }


  @Test
  public void uploadHelmChart_withMissingServerConfiguration_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(completeValidRepository().username(null).build()).build();
    jKubeConfiguration.getRegistryConfig().getSettings()
        .add(RegistryServerConfiguration.builder().id("DIFFERENT").build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        helmService.uploadHelmChart(helm));
    // Then
    assertThat(result).hasMessage("No credentials found for SNAP-REPO in configuration or settings.xml server list.");
  }

  private static HelmRepository.HelmRepositoryBuilder completeValidRepository() {
    return HelmRepository.builder()
        .name("SNAP-REPO")
        .type(HelmRepository.HelmRepoType.ARTIFACTORY)
        .url("https://example.com/artifactory")
        .username("User")
        .password("S3cret");
  }
}
