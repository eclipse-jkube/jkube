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
package org.eclipse.jkube.thorntail.v2.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ThorntailV2GeneratorTest {

  private Properties properties;
  private GeneratorContext generatorContext;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    properties = new Properties();
    final File targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    generatorContext = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .properties(properties)
        .baseDirectory(temporaryFolder.toFile())
        .buildDirectory(targetDir)
        .buildPackageDirectory(targetDir)
        .outputDirectory(targetDir)
        .version("0.0.1")
        .build())
      .build();
  }

  @Test
  @DisplayName("with jkube.java.version set, should still use jkube-java-11 base image")
  void withJavaVersionProperty_shouldStillUseJava11Image() {
    // Given
    properties.put("jkube.generator.thorntail-v2.mainClass", "org.example.Foo");
    properties.put("jkube.java.version", "21");
    // When
    final List<ImageConfiguration> result = new ThorntailV2Generator(generatorContext)
      .customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
      .extracting(ImageConfiguration::getBuildConfiguration)
      .extracting(BuildConfiguration::getFrom)
      .asString()
      .contains("jkube-java-11")
      .doesNotContain("jkube-java-21");
  }
}
