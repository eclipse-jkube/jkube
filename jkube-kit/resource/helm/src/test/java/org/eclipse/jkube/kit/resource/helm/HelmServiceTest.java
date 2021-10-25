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

import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.kit.common.KitLogger;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class HelmServiceTest {

  @Mocked
  KitLogger kitLogger;

  private HelmConfig.HelmConfigBuilder helmConfig;

  @Before
  public void setUp() throws Exception {
    helmConfig = HelmConfig.builder();
  }

  @After
  public void tearDown() {
    kitLogger = null;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test(expected = IOException.class)
  public void prepareSourceDirValidWithNoYamls(@Mocked File file) throws Exception {
    // Given
    // @formatter:off
    new Expectations() {{
      file.isDirectory();result = true;
    }};
    // @formatter:on
    // When
    HelmService.prepareSourceDir(helmConfig.build(), HelmConfig.HelmType.OPENSHIFT);
  }

  @Test
  public void createChartYaml(@Mocked File outputDir, @Mocked ResourceUtil resourceUtil) throws Exception {
    // Given
    helmConfig.chart("Chart Name").version("1337");
    // When
    HelmService.createChartYaml(helmConfig.build(), outputDir);
    // Then
    new Verifications() {{
      Chart chart;
      ResourceUtil.save(withNotNull(), chart = withCapture(), ResourceFileType.yaml);
      assertThat(chart)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("name", "Chart Name")
          .hasFieldOrPropertyWithValue("version", "1337");
    }};
  }

  @Test
  public void createChartYamlWithDependencies(@Mocked File outputDir, @Mocked ResourceUtil resourceUtil) throws Exception {
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
    new Verifications() {{
      Chart chart;
      ResourceUtil.save(withNotNull(), chart = withCapture(), ResourceFileType.yaml);
      assertThat(chart)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("name", "Chart Name")
          .hasFieldOrPropertyWithValue("version", "1337")
          .hasFieldOrPropertyWithValue("dependencies",
              Collections.singletonList(helmDependency));
    }};
  }

  @Test
  public void uploadChart(@Mocked HelmUploader helmUploader)
      throws IOException, BadUploadException {
    //Given
    helmConfig
        .types(Collections.singletonList(HelmType.KUBERNETES))
        .chart("chartName")
        .version("1337")
        .chartExtension("tar.gz")
        .outputDir("target")
        .tarballOutputDir("target");
    final HelmRepository helmRepository = HelmRepository.builder().name("Artifactory").build();
    // @formatter:off
    new Expectations() {{
      helmUploader.uploadSingle(withInstanceOf(File.class), helmRepository);
    }};
    // @formatter:on
    // When
    HelmService.uploadHelmChart(kitLogger, helmConfig.build(), helmRepository);
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
    final List<RegistryServerConfiguration> serverConfigurationList = Collections.emptyList();
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> HelmService.uploadHelmChart(helm, serverConfigurationList, s -> s, kitLogger));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  public void uploadHelmChart_withMissingRepositoryConfiguration_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT").build();
    final List<RegistryServerConfiguration> serverConfigurationList = Collections.emptyList();
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
      () -> HelmService.uploadHelmChart(helm, serverConfigurationList, s -> s, kitLogger));
    // Then
    assertThat(result).hasMessage("No repository or invalid repository configured for upload");
  }

  @Test
  public void uploadHelmChart_withServerConfigurationWithoutUsername_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(completeValidRepository().username(null).build()).build();
    final List<RegistryServerConfiguration> serverConfigurations = Collections.singletonList(
        RegistryServerConfiguration.builder()
            .id("SNAP-REPO")
            .build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        HelmService.uploadHelmChart(helm, serverConfigurations, s -> s, kitLogger));
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO was found in server list but has no username/password.");
  }

  @Test
  public void uploadHelmChart_withServerConfigurationWithoutPassword_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(completeValidRepository().password(null).build()).build();
    final List<RegistryServerConfiguration> serverConfigurations = Collections.singletonList(
        RegistryServerConfiguration.builder()
            .id("SNAP-REPO")
            .build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        HelmService.uploadHelmChart(helm, serverConfigurations, s -> s, kitLogger));
    // Then
    assertThat(result).hasMessage("Repo SNAP-REPO has a username but no password defined.");
  }


  @Test
  public void uploadHelmChart_withMissingServerConfiguration_shouldFail() {
    // Given
    final HelmConfig helm = helmConfig.chart("chart").version("1337-SNAPSHOT")
        .snapshotRepository(completeValidRepository().username(null).build()).build();
    final List<RegistryServerConfiguration> serverConfigurations = Collections.singletonList(
        RegistryServerConfiguration.builder()
            .id("DIFFERENT")
            .build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        HelmService.uploadHelmChart(helm, serverConfigurations, s -> s, kitLogger));
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
