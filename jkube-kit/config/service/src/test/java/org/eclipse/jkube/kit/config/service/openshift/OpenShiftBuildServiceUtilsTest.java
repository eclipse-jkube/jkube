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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.computeS2IBuildName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildArchive;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildOutput;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildStrategy;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.getBuildConfigSpec;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("ConstantConditions")
public class OpenShiftBuildServiceUtilsTest {

  @Mocked
  private JKubeServiceHub jKubeServiceHub;

  @Mocked
  private JKubeBuildTarArchiver tarArchiver;

  private ImageConfiguration imageConfiguration;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    final String projectName = "myapp";
    imageConfiguration = ImageConfiguration.builder()
        .name(projectName)
        .build(BuildConfiguration.builder()
            .env(Collections.singletonMap("FOO", "BAR"))
            .from("ubi8/s2i-base")
            .build()
        ).build();
  }

  @Test
  public void createBuildArchive_withIOExceptionOnCreateDockerBuildArchive_shouldThrowException() throws Exception {
    // Given
    //  @formatter:off
    withBuildServiceConfig(BuildServiceConfig.builder()
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
    new Expectations() {{
      jKubeServiceHub.getDockerServiceHub().getArchiveService().createDockerBuildArchive(
         withInstanceOf(ImageConfiguration.class), withInstanceOf(JKubeConfiguration.class), withInstanceOf(ArchiverCustomizer.class));
      result = new IOException("Mocked Exception");
    }};
    // @formatter:on
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
    withBuildServiceConfig(BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.jib)
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        createBuildStrategy(jKubeServiceHub, imageConfiguration, null));
    // Then
    assertThat(result).hasMessageContaining("Unsupported BuildStrategy jib");
  }

  @Test
  public void createBuildStrategy_withS2iBuildStrategyAndNoPullSecret_shouldReturnValidBuildStrategy() {
    // Given
    withBuildServiceConfig(BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
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
    withBuildServiceConfig(BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
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
    withBuildServiceConfig(BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
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
    withBuildServiceConfig(BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
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
    withBuildServiceConfig(BuildServiceConfig.builder()
        .buildDirectory(temporaryFolder.getRoot().getAbsolutePath())
        .build());
    createBuildArchive(jKubeServiceHub, imageConfiguration);

    final List<ArchiverCustomizer> customizer = new ArrayList<>();
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getDockerServiceHub().getArchiveService().createDockerBuildArchive(
          withInstanceOf(ImageConfiguration.class), withInstanceOf(JKubeConfiguration.class), withCapture(customizer));
    }};
    // @formatter:on
    assertThat(customizer).hasSize(1);
    customizer.iterator().next().customize(tarArchiver);
    final List<String> path = new LinkedList<>();
    final List<File> file = new LinkedList<>();
    // @formatter:off
    new Verifications() {{
      tarArchiver.includeFile(withCapture(file), withCapture(path));
    }};
    // @formatter:on
    assertThat(path).singleElement().isEqualTo(".s2i/environment");
    assertThat(file).singleElement().satisfies(f -> assertThat(f).hasContent("FOO=BAR"));
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

  private void withBuildServiceConfig(BuildServiceConfig buildServiceConfig) {
    //  @formatter:off
    new Expectations() {{
      jKubeServiceHub.getBuildServiceConfig(); result = buildServiceConfig;
    }};
    // @formatter:on
  }
}