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
package org.eclipse.jkube.kit.build.api.helper;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RegistryUtilTest {
  @Test
  void getApplicablePushRegistryFrom_whenImageNameContainsRegistry_thenUseRegistryFromImageName() {
    // Given
    ImageConfiguration imageConfiguration = createNewImageConfiguration("word.word/word/word:tag", null);
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("word.word");
  }

  @Test
  void getApplicablePushRegistryFrom_whenImageNameHasNoRegistryAndImageConfigHasRegistry_thenUseImageConfigRegistry() {
    // Given
    ImageConfiguration imageConfiguration = createNewImageConfiguration("word/word:tag", "word.word");
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("word.word");
  }

  @Test
  void getApplicablePushRegistryFrom_whenImageNameNotFullyQualified_thenGivePriorityToOtherSources() {
    // Given
    ImageConfiguration imageConfiguration = createNewImageConfiguration("word.word/word:tag", null);
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("registry-config.io");
  }

  @Test
  void getApplicablePushRegistryFrom_whenNoRegistryInImageConfig_thenUseImageConfigRegistry() {
    // Given
    ImageConfiguration imageConfiguration = createNewImageConfiguration("word/word:tag", null);
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePushRegistryFrom(imageConfiguration, registryConfig))
        .isEqualTo("registry-config.io");
  }

  @Test
  void getApplicablePullRegistryFrom_whenRegistryInImageName_thenUseRegistryFromImageName() {
    // Given
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePullRegistryFrom("word.word/word/word:tag", registryConfig))
        .isEqualTo("word.word");
  }

  @Test
  void getApplicablePullRegistryFrom_whenRegistryInRegistryConfig_thenUseRegistryFromRegistryConfig() {
    // Given
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePullRegistryFrom("word:tag", registryConfig))
        .isEqualTo("registry-config.io");
  }

  @Test
  void getApplicablePullRegistryFrom_whenRegistryInRegistryConfigAndImageNameNotFullyQualified_thenUseRegistryFromRegistryConfig() {
    // Given
    RegistryConfig registryConfig = createNewRegistryConfig();

    // When + Then
    assertThat(RegistryUtil.getApplicablePullRegistryFrom("word.word/word:tag", registryConfig))
        .isEqualTo("registry-config.io");
  }

  private ImageConfiguration createNewImageConfiguration(String name, String registry) {
    ImageConfiguration.ImageConfigurationBuilder imageConfigurationBuilder = ImageConfiguration.builder();
    imageConfigurationBuilder.name(name);
    if (StringUtils.isNotBlank(registry)) {
      imageConfigurationBuilder.registry(registry);
    }
    return imageConfigurationBuilder.build();
  }

  private RegistryConfig createNewRegistryConfig() {
    return RegistryConfig.builder()
        .registry("registry-config.io")
        .build();
  }
}