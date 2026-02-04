/*
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

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildConfigSpecBuilder;
import org.eclipse.jkube.kit.build.api.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.build.api.assembly.JKubeBuildTarArchiver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.computeS2IBuildName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildArchive;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildStrategy;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.getBuildConfigSpec;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftBuildServiceUtilsTest {

  @TempDir
  File temporaryFolder;

  private JKubeServiceHub jKubeServiceHub;
  private ImageConfiguration imageConfiguration;
  private KitLogger log;

  @BeforeEach
  void setUp() {
    jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getBuildServiceConfig().getBuildDirectory())
        .thenReturn(temporaryFolder.getAbsolutePath());
    when(jKubeServiceHub.getConfiguration()).thenReturn(JKubeConfiguration.builder()
            .project(JavaProject.builder().build())
            .buildArgs(Collections.emptyMap())
        .build());
    imageConfiguration = ImageConfiguration.builder()
        .name("myapp")
        .build(BuildConfiguration.builder()
            .env(Collections.singletonMap("FOO", "BAR"))
            .from("ubi8/s2i-base")
            .build()
        ).build();
    log = spy(new KitLogger.SilentLogger());
  }

  @AfterEach
  void tearDown() {
    jKubeServiceHub = null;
  }

  @Test
  void createBuildArchive_withIOExceptionOnCreateDockerBuildArchive_shouldThrowException() throws Exception {
    // Given
    when(jKubeServiceHub.getDockerServiceHub().getArchiveService().createDockerBuildArchive(
        any(ImageConfiguration.class), any(JKubeConfiguration.class), any(ArchiverCustomizer.class)))
        .thenThrow(new IOException("Mocked Exception"));
    // When + Then
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> createBuildArchive(jKubeServiceHub, imageConfiguration))
        .withMessage("Unable to create the build archive")
        .havingCause()
        .withMessage("Mocked Exception");
  }

  @Test
  void computeS2IBuildName_withImageNameAndEmptyBuildServiceConfig_shouldReturnName() {
    // Given
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(imageConfiguration, new BuildServiceConfig(), imageName);
    // Then
    assertThat(result).isEqualTo("name");
  }

  @Test
  void computeS2IBuildName_withImageNameAndBuildServiceWithS2I_shouldReturnNameWithDefaultSuffix() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .buildDirectory(temporaryFolder.getAbsolutePath())
        .build();
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(imageConfiguration, buildServiceConfig, imageName);
    // Then
    assertThat(result).isEqualTo("name-s2i");
  }

  @Test
  void computeS2IBuildName_withImageNameAndBuildServiceWithCustomSuffix_shouldReturnNameWithCustomSuffix() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .buildDirectory(temporaryFolder.getAbsolutePath())
        .build();
    imageConfiguration = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder().openshiftS2iBuildNameSuffix("-custom").build())
        .build();
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(imageConfiguration, buildServiceConfig, imageName);
    // Then
    assertThat(result).isEqualTo("name-custom");
  }

  @Test
  void createBuildStrategy_withJibBuildStrategy_shouldThrowException() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
    // When + Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> createBuildStrategy(jKubeServiceHub, imageConfiguration, null, log))
        .withMessageContaining("Unsupported BuildStrategy jib");
  }

  @Test
  void checkTarPackage() throws Exception {
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
  void getBuildConfigSpec_withNull_shouldReturnNew() {
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
  void getBuildConfigSpec_withExistingSpec_shouldReturnExistingSpec() {
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
