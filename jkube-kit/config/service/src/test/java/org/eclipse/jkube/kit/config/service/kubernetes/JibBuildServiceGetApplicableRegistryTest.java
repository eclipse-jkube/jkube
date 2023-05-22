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
package org.eclipse.jkube.kit.config.service.kubernetes;

import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class JibBuildServiceGetApplicableRegistryTest {
  @ParameterizedTest(name = "pull {0}, when no registry from any source, then pull registry {1}")
  @CsvSource({
      "word:word,",
      "word/word:tag,",
      "word.word/word/word:tag,word.word",
      "word:5000/word:tag,word:5000",
      "word.word:5000/word:tag,word.word:5000",
      "word.word/word:tag,word.word",
      "word.word/word.word/word:tag,word.word",
      "word.word.word/word:tag,word.word.word",
      "word.word.word/word/word:tag,word.word.word"
  })
  void pull_whenRegistryNotPresentFromAnySource_thenReturnRegistryFromImageName(String from, String expectedPullRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration("test-image:latest", from, null);
    RegistryConfig registryConfig = RegistryConfig.builder().build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(false, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPullRegistry);
  }

  @ParameterizedTest(name = "pull {0} when registry present both in image name and registry config, then registry taken from image name {1}")
  @CsvSource({
      "word.word/word/word:tag,word.word",
      "word:5000/word:tag,word:5000",
      "word.word:5000/word:tag,word.word:5000",
      "word.word/word.word/word:tag,word.word",
      "word.word.word/word/word:tag,word.word.word"
  })
  void pull_whenRegistryPresentInBothImageNameAndRegistryConfig_thenReturnRegistryFromImageName(String from, String expectedPullRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration("test-image:latest", from, null);
    RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(false, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPullRegistry);
  }

  @ParameterizedTest(name = "pull {0} and registry from registry config, then registry from registry config {1}")
  @CsvSource({
      "word:word,quay.io",
      "word/word:tag,quay.io",
      "word.word/word:tag,quay.io"
  })
  void pull_whenRegistryFromRegistryConfig_thenReturnRegistryFromRegistryConfig(String from, String expectedPullRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration("test-image:latest", from, null);
    RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(false, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPullRegistry);
  }

  @ParameterizedTest(name = "push {0} when no registry from any source, then push registry from image name {1}")
  @CsvSource({
      "word:word,",
      "word/word:tag,",
      "word.word/word:tag,word.word",
      "word.word/word/word:tag,word.word",
      "word.word/word.word/word:tag,word.word",
      "word:5000/word:tag,word:5000",
      "word.word:5000/word:tag,word.word:5000",
      "word.word.word/word:tag,word.word.word",
      "word.word.word/word/word:tag,word.word.word"
  })
  void push_whenRegistryNotPresentFromAnySource_thenReturnRegistryFromImageName(String imageName, String expectedPushRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration(imageName, "test-image:latest", null);
    RegistryConfig registryConfig = RegistryConfig.builder().build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(true, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPushRegistry);
  }

  @ParameterizedTest(name = "push {0} when registry present both in image name and registry config, then registry taken from image name {1}")
  @CsvSource({
      "word.word/word/word:tag,word.word",
      "word.word/word.word/word:tag,word.word",
      "word:5000/word:tag,word:5000",
      "word.word:5000/word:tag,word.word:5000",
      "word.word.word/word/word:tag,word.word.word"
  })
  void push_whenRegistryInBothImageNameAndRegistryConfig_thenUseRegistryFromImageName(String imageName, String expectedPushRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration(imageName, "test-image:latest", null);
    RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(true, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPushRegistry);
  }

  @ParameterizedTest(name = "push {0} when registry present both in image name and image config, then registry taken from image name {1}")
  @CsvSource({
      "word.word/word/word:tag,word.word",
      "word.word/word.word/word:tag,word.word",
      "word:5000/word:tag,word:5000",
      "word.word:5000/word:tag,word.word:5000",
      "word.word.word/word/word:tag,word.word.word"
  })
  void push_whenRegistryPresentInBothImageNameAndImageConfig_thenUseRegistryFromImageName(String imageName, String expectedPushRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration(imageName, "test-image:latest", "quay.io");
    RegistryConfig registryConfig = RegistryConfig.builder().build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(true, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPushRegistry);
  }

  @ParameterizedTest(name = "push {0} when registry from image config, then registry used from image config {1}")
  @CsvSource({
      "word:word,quay.io",
      "word/word:tag,quay.io",
      "word.word/word:tag,quay.io"
  })
  void push_whenRegistryFromImageConfig_thenReturnRegistryFromImageConfig(String imageName, String expectedPushRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration(imageName, "test-image:latest", "quay.io");
    RegistryConfig registryConfig = RegistryConfig.builder().build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(true, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPushRegistry);
  }

  @ParameterizedTest(name = "push {0} when registry from registry config, then registry used from registry config {1}")
  @CsvSource({
      "word:word,quay.io",
      "word/word:tag,quay.io",
      "word.word/word:tag,quay.io"
  })
  void push_whenRegistryFromRegistryConfig_thenReturnRegistryFromRegistryConfig(String imageName, String expectedPushRegistry) {
    // Given
    ImageConfiguration imageConfiguration = createImageConfiguration(imageName, "test-image:latest", null);
    RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();

    // When
    String pullRegistry = JibBuildService.getApplicableRegistry(true, imageConfiguration, registryConfig);

    // Then
    assertThat(pullRegistry).isEqualTo(expectedPushRegistry);
  }

  private ImageConfiguration createImageConfiguration(String name, String fromImage, String registry) {
    return ImageConfiguration.builder()
        .name(name)
        .build(BuildConfiguration.builder().from(fromImage).build())
        .registry(registry)
        .build();
  }
}
