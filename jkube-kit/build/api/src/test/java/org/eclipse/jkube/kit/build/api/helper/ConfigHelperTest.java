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
package org.eclipse.jkube.kit.build.api.helper;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

class ConfigHelperTest {
  private KitLogger logger;
  private ImageConfigResolver imageConfigResolver;
  private JavaProject javaProject;
  private JKubeConfiguration jKubeConfiguration;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    imageConfigResolver = mock(ImageConfigResolver.class, RETURNS_DEEP_STUBS);
    javaProject = JavaProject.builder()
      .groupId("org.eclipse.jkube")
      .artifactId("test-java-project")
      .version("0.0.1-SNAPSHOT")
      .properties(new Properties())
      .baseDirectory(new File("dummy-dir"))
      .build();
    jKubeConfiguration = JKubeConfiguration.builder()
      .project(javaProject)
      .build();
  }

  @Test
  void initImageConfiguration_withSimpleImageConfiguration_shouldReturnImageConfiguration() {
    // Given
    ImageConfiguration dummyImageConfiguration = createNewDummyImageConfiguration();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(dummyImageConfiguration);
    when(imageConfigResolver.resolve(dummyImageConfiguration, javaProject)).thenReturn(images);

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration(new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration);

    // Then
    assertThat(resolvedImages).isNotNull()
        .singleElement()
        .isEqualTo(dummyImageConfiguration);
  }

  @Test
  void initImageConfiguration_withSimpleDockerFileInProjectBaseDir_shouldCreateImageConfiguration() {
    List<ImageConfiguration> images = new ArrayList<>();
    File dockerFile = new File(Objects.requireNonNull(getClass().getResource("/dummy-javaproject/Dockerfile")).getFile());
    jKubeConfiguration = jKubeConfiguration.toBuilder()
      .project(javaProject.toBuilder()
        .baseDirectory(dockerFile.getParentFile())
        .build())
      .build();

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration(new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration);

    // Then
    assertThat(resolvedImages).isNotNull()
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "jkube/test-java-project:latest")
        .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
        .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
  }

  @Test
  void initImageConfiguration_withSimpleDockerFileModeEnabledAndImageConfigurationWithNoBuild_shouldModifyExistingImageConfiguration() {
    ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
        .name("imageconfiguration-no-build:latest")
        .build();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(dummyImageConfiguration);
    File dockerFile = new File(getClass().getResource("/dummy-javaproject/Dockerfile").getFile());
    jKubeConfiguration = jKubeConfiguration.toBuilder()
      .project(javaProject.toBuilder()
        .baseDirectory(dockerFile.getParentFile())
        .build())
      .build();
    when(imageConfigResolver.resolve(dummyImageConfiguration, jKubeConfiguration.getProject())).thenReturn(images);

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration(new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration);

    // Then
    assertThat(resolvedImages).isNotNull()
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "imageconfiguration-no-build:latest")
        .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
        .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
    verify(logger).info(eq("Using Dockerfile: %s"), anyString());
    verify(logger).info(eq("Using Docker Context Directory: %s"), any(File.class));
  }

  @Test
  void initImageConfiguration_whenImageConfigurationNameBlank_thenThrowException() {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder().build();
    List<ImageConfiguration> images = Collections.singletonList(imageConfiguration);
    when(imageConfigResolver.resolve(imageConfiguration, javaProject)).thenReturn(images);

    // When + Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ConfigHelper.initImageConfiguration(new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration))
        .withMessage("Configuration error: <image> must have a non-null <name>");
  }

  @Test
  void initImageConfiguration_whenNoMatchForImageFilter_thenLogWarning() {
    // Given
    ImageConfiguration dummyImageConfiguration = createNewDummyImageConfiguration();
    List<ImageConfiguration> images = Collections.singletonList(createNewDummyImageConfiguration());
    when(imageConfigResolver.resolve(dummyImageConfiguration, javaProject)).thenReturn(images);

    // When
    ConfigHelper.initImageConfiguration(new Date(), images, imageConfigResolver, logger, "i-dont-exist", configs -> configs, jKubeConfiguration);

    // Then
    verify(logger).warn("None of the resolved images [%s] match the configured filter '%s'", "foo/bar:latest", "i-dont-exist");
  }

  @Test
  void validateExternalPropertyActivation_withMultipleImagesWithoutExplicitExternalConfig_shouldThrowException() {
    // Given
    javaProject.getProperties().put("docker.imagePropertyConfiguration", "Override");
    ImageConfiguration i1 = createNewDummyImageConfiguration();
    ImageConfiguration i2 = createNewDummyImageConfiguration().toBuilder()
        .name("imageconfig2")
        .external(Collections.singletonMap("type", "compose"))
        .build();
    ImageConfiguration i3 = createNewDummyImageConfiguration()
        .toBuilder()
        .name("external")
        .external(Collections.singletonMap("type", "properties"))
        .build();
    List<ImageConfiguration> images = Arrays.asList(i1, i2, i3);

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> ConfigHelper.validateExternalPropertyActivation(javaProject, images))
        .withMessage("Configuration error: Cannot use property docker.imagePropertyConfiguration on projects with multiple images without explicit image external configuration.");
  }

  private ImageConfiguration createNewDummyImageConfiguration() {
    return ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("foobase:latest")
            .build())
        .build();
  }
}
