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

import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePullRegistryFrom;
import static org.eclipse.jkube.kit.build.api.helper.RegistryUtil.getApplicablePushRegistryFrom;

class RegistryUtilTest {
  @Test
  void getApplicablePushRegistryFrom_whenImageNameContainsRegistry_thenUseRegistryFromImageName() {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("word.word/word/word:tag")
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("word.word");
  }

  @Test
  void getApplicablePushRegistryFrom_whenImageNameHasNoRegistryAndImageConfigHasRegistry_thenUseImageConfigRegistry() {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("word/word:tag")
      .registry("word.word")
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("word.word");
  }

  @Test
  void getApplicablePushRegistryFrom_whenImageNameNotFullyQualified_thenGivePriorityToOtherSources() {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("word.word/word:tag")
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("registry-config.io");
  }

  @Test
  void getApplicablePushRegistryFrom_whenNoRegistryInImageConfig_thenUseImageConfigRegistry() {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("word/word:tag")
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("registry-config.io");
  }

  @Test
  void getApplicablePullRegistryFrom_whenRegistryInImageName_thenUseRegistryFromImageName() {
    // Given
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePullRegistryFrom("word.word/word/word:tag", registryConfig))
        .isEqualTo("word.word");
  }

  @Test
  void getApplicablePullRegistryFrom_whenRegistryInRegistryConfig_thenUseRegistryFromRegistryConfig() {
    // Given
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePullRegistryFrom("word:tag", registryConfig))
        .isEqualTo("registry-config.io");
  }

  @Test
  void getApplicablePullRegistryFrom_whenRegistryInRegistryConfigAndImageNameNotFullyQualified_thenUseRegistryFromRegistryConfig() {
    // Given
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("registry-config.io").build();
    // When + Then
    assertThat(RegistryUtil.getApplicablePullRegistryFrom("word.word/word:tag", registryConfig))
        .isEqualTo("registry-config.io");
  }

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
    final RegistryConfig registryConfig = RegistryConfig.builder().build();
    // When
    final String pullRegistry = getApplicablePullRegistryFrom(from, registryConfig);
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
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();
    // When
    final String pullRegistry = getApplicablePullRegistryFrom(from, registryConfig);
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
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();
    // When
    final String pullRegistry = getApplicablePullRegistryFrom(from, registryConfig);
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
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name(imageName)
      .build(BuildConfiguration.builder().from("test-image:latest").build())
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().build();
    // When
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    // Then
    assertThat(pushRegistry).isEqualTo(expectedPushRegistry);
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
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name(imageName)
      .build(BuildConfiguration.builder().from("test-image:latest").build())
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();
    // When
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    // Then
    assertThat(pushRegistry).isEqualTo(expectedPushRegistry);
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
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name(imageName)
      .build(BuildConfiguration.builder().from("test-image:latest").build())
      .registry("quay.io")
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().build();
    // When
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    // Then
    assertThat(pushRegistry).isEqualTo(expectedPushRegistry);
  }

  @ParameterizedTest(name = "push {0} when registry from image config, then registry used from image config {1}")
  @CsvSource({
    "word:word,quay.io",
    "word/word:tag,quay.io",
    "word.word/word:tag,quay.io"
  })
  void push_whenRegistryFromImageConfig_thenReturnRegistryFromImageConfig(String imageName, String expectedPushRegistry) {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name(imageName)
      .build(BuildConfiguration.builder().from("test-image:latest").build())
      .registry("quay.io")
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().build();
    // When
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    // Then
    assertThat(pushRegistry).isEqualTo(expectedPushRegistry);
  }

  @ParameterizedTest(name = "push {0} when registry from registry config, then registry used from registry config {1}")
  @CsvSource({
    "word:word,quay.io",
    "word/word:tag,quay.io",
    "word.word/word:tag,quay.io"
  })
  void push_whenRegistryFromRegistryConfig_thenReturnRegistryFromRegistryConfig(String imageName, String expectedPushRegistry) {
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name(imageName)
      .build(BuildConfiguration.builder().from("test-image:latest").build())
      .build();
    final RegistryConfig registryConfig = RegistryConfig.builder().registry("quay.io").build();
    // When
    final String pushRegistry = getApplicablePushRegistryFrom(imageConfiguration, registryConfig);
    // Then
    assertThat(pushRegistry).isEqualTo(expectedPushRegistry);
  }

}
