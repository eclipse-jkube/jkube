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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fabric8.openshift.api.model.ImageStreamTag;
import io.fabric8.openshift.api.model.ImageStreamTagBuilder;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.computeS2IBuildName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createAdditionalTagsIfPresent;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildArchive;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildOutput;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.createBuildStrategy;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.getAdditionalTagsToCreate;
import static org.eclipse.jkube.kit.config.service.openshift.OpenShiftBuildServiceUtils.getBuildConfigSpec;
import static org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService.DOCKER_IMAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftBuildServiceUtilsTest {

  @TempDir
  File temporaryFolder;

  private JKubeServiceHub jKubeServiceHub;
  private ImageConfiguration imageConfiguration;

  @BeforeEach
  void setUp() {
    jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getBuildServiceConfig().getBuildDirectory())
        .thenReturn(temporaryFolder.getAbsolutePath());
    imageConfiguration = ImageConfiguration.builder()
        .name("myapp")
        .build(BuildConfiguration.builder()
            .env(Collections.singletonMap("FOO", "BAR"))
            .from("ubi8/s2i-base")
            .build()
        ).build();
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
    final String result = computeS2IBuildName(new BuildServiceConfig(), imageName);
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
    final String result = computeS2IBuildName(buildServiceConfig, imageName);
    // Then
    assertThat(result).isEqualTo("name-s2i");
  }

  @Test
  void computeS2IBuildName_withImageNameAndBuildServiceWithCustomSuffix_shouldReturnNameWithCustomSuffix() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.s2i)
        .s2iBuildNameSuffix("-custom")
        .buildDirectory(temporaryFolder.getAbsolutePath())
        .build();
    final ImageName imageName = new ImageName("registry/name:tag");
    // When
    final String result = computeS2IBuildName(buildServiceConfig, imageName);
    // Then
    assertThat(result).isEqualTo("name-custom");
  }

  @Test
  void createBuildStrategy_withJibBuildStrategy_shouldThrowException() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
    // When + Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> createBuildStrategy(jKubeServiceHub, imageConfiguration, null))
        .withMessageContaining("Unsupported BuildStrategy jib");
  }

  @Test
  void createBuildStrategy_withS2iBuildStrategyAndNoPullSecret_shouldReturnValidBuildStrategy() {
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
  void createBuildStrategy_withS2iBuildStrategyAndPullSecret_shouldReturnValidBuildStrategy() {
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
  void createBuildStrategy_withDockerBuildStrategyAndNoPullSecret_shouldReturnValidBuildStrategy() {
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
  void createBuildStrategy_withDockerBuildStrategyAndPullSecret_shouldReturnValidBuildStrategy() {
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
  void createBuildOutput_withDefaults_shouldReturnImageStreamTag() {
    // When
    final BuildOutput result = createBuildOutput(new BuildServiceConfig(), new ImageName("my-app-image"));
    // Then
    assertThat(result)
        .extracting(BuildOutput::getTo)
        .hasFieldOrPropertyWithValue("kind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("name", "my-app-image:latest");
  }

  @Test
  void createBuildOutput_withOutputKindDockerAndPushSecret_shouldReturnDocker() {
    // Given
    final BuildServiceConfig buildServiceConfig = BuildServiceConfig.builder()
        .buildOutputKind("DockerImage")
        .openshiftPushSecret("my-push-secret")
        .buildDirectory(temporaryFolder.getAbsolutePath())
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

  @Test
  void createAdditionalTagsIfPresent_withNoAdditionalTag_shouldReturnEmptyList() {
    // Given + When
    List<ImageStreamTag> imageStreamTagList = createAdditionalTagsIfPresent(imageConfiguration, "ns1", null);

    // Then
    assertThat(imageStreamTagList).isEmpty();
  }

  @Test
  void createAdditionalTagsIfPresent_withAdditionalTags_shouldReturnNonEmptyImageStreamTagList() {
    // Given
    ImageConfiguration imageConfigurationWithAdditionalTags = createNewImageConfigurationWithAdditionalTags();

    // When
    List<ImageStreamTag> imageStreamTagList = createAdditionalTagsIfPresent(imageConfigurationWithAdditionalTags, "ns1", new ImageStreamTagBuilder()
        .withNewMetadata().withName("test:t1").endMetadata()
        .withNewImage().withDockerImageReference("foo-registry.openshift.svc:5000/test/test@sha256:1234").endImage()
        .build());

    // Then
    assertThat(imageStreamTagList)
        .hasSize(2)
        .containsExactlyInAnyOrder(
            new ImageStreamTagBuilder()
                .withNewMetadata()
                .withName("test:t2")
                .withNamespace("ns1")
                .endMetadata()
                .withNewTag()
                .withNewFrom()
                .withKind(DOCKER_IMAGE)
                .withName("foo-registry.openshift.svc:5000/test/test@sha256:1234")
                .endFrom()
                .endTag()
                .withGeneration(0L)
                .build(),
            new ImageStreamTagBuilder()
                .withNewMetadata()
                .withName("test:t3")
                .withNamespace("ns1")
                .endMetadata()
                .withNewTag()
                .withNewFrom()
                .withKind(DOCKER_IMAGE)
                .withName("foo-registry.openshift.svc:5000/test/test@sha256:1234")
                .endFrom()
                .endTag()
                .withGeneration(0L)
                .build()
        );
  }

  @Test
  void getAdditionalTagsToCreate_withNoAdditionalTag_shouldReturnEmptyList() {
    // Given + When
    List<String> additionalTags = getAdditionalTagsToCreate(imageConfiguration);

    // Then
    assertThat(additionalTags).isEmpty();
  }

  @Test
  void getAdditionalTagsToCreate_withAdditionalTags_shouldReturnExtraTagsList() {
    // Given + When
    List<String> additionalTags = getAdditionalTagsToCreate(createNewImageConfigurationWithAdditionalTags());

    // Then
    assertThat(additionalTags).containsExactlyInAnyOrder("t2", "t3");
  }

  private ImageConfiguration createNewImageConfigurationWithAdditionalTags() {
    return ImageConfiguration.builder()
        .name("test:t1")
        .build(BuildConfiguration.builder().tags(Arrays.asList("t2", "t3")).build())
        .build();
  }
}
