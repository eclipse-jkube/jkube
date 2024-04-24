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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ConfigHelperTest {
  private JavaProject javaProject;

  @BeforeEach
  void setUp() {
    javaProject = JavaProject.builder()
      .groupId("org.eclipse.jkube")
      .artifactId("test-java-project")
      .version("0.0.1-SNAPSHOT")
      .properties(new Properties())
      .baseDirectory(new File("dummy-dir"))
      .build();
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
