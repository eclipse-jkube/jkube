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
package org.eclipse.jkube.kit.config.service.openshift;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.jkube.kit.build.api.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildConfigSpecBuilder;
import io.fabric8.openshift.api.model.BuildOutput;
import io.fabric8.openshift.api.model.BuildStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.computeS2IBuildName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildArchive;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildOutput;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildStrategy;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.getBuildConfigSpec;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenShiftBuildServiceUtilsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private JKubeServiceHub jKubeServiceHub;
  private ImageConfiguration imageConfiguration;

  @Before
  public void setUp() throws Exception {
    jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getBuildServiceConfig().getBuildDirectory())
        .thenReturn(temporaryFolder.getRoot().getAbsolutePath());
    imageConfiguration = ImageConfiguration.builder()
        .name("myapp")
        .build(BuildConfiguration.builder()
            .env(Collections.singletonMap("FOO", "BAR"))
            .from("ubi8/s2i-base")
            .build()
        ).build();
  }

  @After
  public void tearDown() throws Exception {
    jKubeServiceHub = null;
  }

  @Test
  public void createBuildArchive_withIOExceptionOnCreateDockerBuildArchive_shouldThrowException() throws Exception {
    // Given
    when(jKubeServiceHub.getDockerServiceHub().getArchiveService().createDockerBuildArchive(
        any(ImageConfiguration.class), any(JKubeConfiguration.class), any(ArchiverCustomizer.class)))
        .thenThrow(new IOException("Mocked Exception"));
    // When
    final JKubeServiceException result = assertThrows(JKubeServiceException.class, () ->
        createBuildArchive(jKubeServiceHub, imageConfiguration));
    // Then
    assertThat(result).hasMessage("Unable to create the build archive").getCause().hasMessage("Mocked Exception");
  }

  @Test
  public void computeS2IBuildName_withImageNameAndEmptyBuildServiceConfig_shouldReturnName() {
    // Given
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(new BuildServiceConfig(), imageName);
    // Then
    assertThat(result).isEqualTo("name");
  }

  @Test
  public void computeS2IBuildName_withImageNameAndBuildServiceWithS2I_shouldReturnNameWithDefaultSuffix() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build();
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(buildServiceConfig, imageName);
    // Then
    assertThat(result).isEqualTo("name-s2i");
  }

  @Test
  public void computeS2IBuildName_withImageNameAndBuildServiceWithCustomSuffix_shouldReturnNameWithCustomSuffix() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .s2iBuildNameSuffix("-custom")
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build();
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(buildServiceConfig, imageName);
    // Then
    assertThat(result).isEqualTo("name-custom");
  }

  @Test
  public void createBuildStrategy_withJibBuildStrategy_shouldThrowException() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        createBuildStrategy(jKubeServiceHub, imageConfiguration, null));
    // Then
    assertThat(result).hasMessageContaining("Unsupported BuildStrategy jib");
  }

  @Test
  public void createBuildStrategy_withS2iBuildStrategyAndNoPullSecret_shouldReturnValidBuildStrategy() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.s2i);
    // When
    final BuildStrategy result = createBuildStrategy(jKubeServiceHub, imageConfiguration, null);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("type", "Source")
        .extracting(BuildStrategy::getSourceStrategy)
        .hasFieldOrPropertyWithValue("from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("from.name", "ubi8/s2i-base");
  }

  @Test
  public void createBuildStrategy_withS2iBuildStrategyAndPullSecret_shouldReturnValidBuildStrategy() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.s2i);
    // When
    final BuildStrategy result = createBuildStrategy(jKubeServiceHub, imageConfiguration, "my-secret-for-pull");
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("type", "Source")
        .extracting(BuildStrategy::getSourceStrategy)
        .hasFieldOrPropertyWithValue("from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("from.name", "ubi8/s2i-base")
        .hasFieldOrPropertyWithValue("pullSecret.name", "my-secret-for-pull");
  }

  @Test
  public void createBuildStrategy_withDockerBuildStrategyAndNoPullSecret_shouldReturnValidBuildStrategy() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy())
        .thenReturn(JKubeBuildStrategy.docker);
    // When
    final BuildStrategy result = createBuildStrategy(jKubeServiceHub, imageConfiguration, null);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("type", "Docker")
        .extracting(BuildStrategy::getDockerStrategy)
        .hasFieldOrPropertyWithValue("from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("from.name", "ubi8/s2i-base");
  }

  @Test
  public void createBuildStrategy_withDockerBuildStrategyAndPullSecret_shouldReturnValidBuildStrategy() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy())
        .thenReturn(JKubeBuildStrategy.docker);
    // When
    final BuildStrategy result = createBuildStrategy(jKubeServiceHub, imageConfiguration, "my-secret-for-pull");
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("type", "Docker")
        .extracting(BuildStrategy::getDockerStrategy)
        .hasFieldOrPropertyWithValue("from.kind", "DockerImage")
        .hasFieldOrPropertyWithValue("from.name", "ubi8/s2i-base")
        .hasFieldOrPropertyWithValue("noCache", false)
        .hasFieldOrPropertyWithValue("pullSecret.name", "my-secret-for-pull");
  }

  @Test
  public void createBuildOutput_withDefaults_shouldReturnImageStreamTag() {
    // When
    final BuildOutput result = createBuildOutput(new BuildServiceConfig(), new ImageName("my-app-image"));
    // Then
    assertThat(result)
        .extracting(BuildOutput::getTo)
        .hasFieldOrPropertyWithValue("kind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("name", "my-app-image:latest");
  }

  @Test
  public void createBuildOutput_withOutputKindDockerAndPushSecret_shouldReturnDocker() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .buildOutputKind("DockerImage")
        .openshiftPushSecret("my-push-secret")
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build();
    // When
    final BuildOutput result = createBuildOutput(buildServiceConfig, new ImageName("my-app-image"));
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("pushSecret.name", "my-push-secret")
        .extracting(BuildOutput::getTo)
        .hasFieldOrPropertyWithValue("kind", "DockerImage")
        .hasFieldOrPropertyWithValue("name", "my-app-image:latest");
  }

  @Test
  public void checkTarPackage() throws Exception {
    final JKubeBuildTarArchiver tarArchiver = mock(JKubeBuildTarArchiver.class);
    createBuildArchive(jKubeServiceHub, imageConfiguration);

    final ArgumentCaptor<ArchiverCustomizer> customizer = ArgumentCaptor.forClass(ArchiverCustomizer.class);
    verify(jKubeServiceHub.getDockerServiceHub().getArchiveService(), times(1))
        .createDockerBuildArchive(any(ImageConfiguration.class), any(JKubeConfiguration.class), customizer.capture());

    customizer.getValue().customize(tarArchiver);
    final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<File> file = ArgumentCaptor.forClass(File.class);
    verify(tarArchiver, times(1)).includeFile(file.capture(), path.capture());
    assertThat(path.getAllValues()).singleElement().isEqualTo(".s2i/environment");
    assertThat(file.getAllValues()).singleElement().satisfies(f -> assertThat(f).hasContent("FOO=BAR"));
  }

  @Test
  public void getBuildConfigSpec_withNull_shouldReturnNew() {
    // Given
    final BuildConfig buildConfig = new BuildConfig();
    // When
    final BuildConfigSpec result = getBuildConfigSpec(buildConfig);
    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo(new BuildConfigSpec())
        .isSameAs(buildConfig.getSpec());
  }

  @Test
  public void getBuildConfigSpec_withExistingSpec_shouldReturnExistingSpec() {
    // Given
    final BuildConfigSpec originalSpec = new BuildConfigSpecBuilder().withRunPolicy("Serial").build();
    final BuildConfig buildConfig = new BuildConfigBuilder().withSpec(originalSpec).build();
    // When
    final BuildConfigSpec result = getBuildConfigSpec(buildConfig);
    // Then
    assertThat(result)
        .isEqualTo(originalSpec)
        .isNotSameAs(originalSpec)
        .isSameAs(buildConfig.getSpec())
        .hasFieldOrPropertyWithValue("runPolicy", "Serial");
  }
}
