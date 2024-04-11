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
package org.eclipse.jkube.generator.dockerfile.simple;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SimpleDockerfileUtilTest {
  @Nested
  @DisplayName("isSimpleDockerfileMode")
  class IsSimpleDockerFileMode {
    @Test
    @DisplayName("Dockerfile present in base directory, return true")
    void whenDockerfilePresentInBaseDirectory_thenReturnTrue() {
      // Given
      File projectDir = new File(Objects.requireNonNull(getClass().getResource("/dummy-javaproject")).getFile());
      // When
      boolean result = SimpleDockerfileUtil.isSimpleDockerFileMode(projectDir);
      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Dockerfile absent in base directory, return false")
    void whenNoDockerfilePresentInBaseDirectory_thenReturnFalse(@TempDir File temporaryFolder) {
      assertThat(SimpleDockerfileUtil.isSimpleDockerFileMode(temporaryFolder)).isFalse();
    }

    @Test
    @DisplayName("When project directory is null, return false")
    void whenNullProjectBaseDirectory_thenReturnFalse() {
      assertThat(SimpleDockerfileUtil.isSimpleDockerFileMode(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("createSimpleDockerfileConfig")
  class CreateSimpleDockerfileConfig {
    @Test
    @DisplayName("simple Dockerfile, should generate ImageConfiguration")
    void simple() throws IOException {
      // Given
      File dockerFile = Files.createTempFile("Dockerfile", "-test").toFile();
      // When
      ImageConfiguration imageConfiguration1 = SimpleDockerfileUtil.createSimpleDockerfileConfig(dockerFile, null);
      ImageConfiguration imageConfiguration2 = SimpleDockerfileUtil.createSimpleDockerfileConfig(dockerFile, "someImage:0.0.1");
      // Then
      assertThat(imageConfiguration1).isNotNull()
          .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
          .extracting(ImageConfiguration::getBuild)
          .extracting(BuildConfiguration::getDockerFileRaw)
          .isEqualTo(dockerFile.getPath());
      assertThat(imageConfiguration2).isNotNull()
          .hasFieldOrPropertyWithValue("name", "someImage:0.0.1")
          .extracting(ImageConfiguration::getBuild)
          .extracting(BuildConfiguration::getDockerFileRaw)
          .isEqualTo(dockerFile.getPath());
    }

    @Test
    @DisplayName("Dockerfile with EXPOSE statement, then generated ImageConfiguration contains ports")
    void exposeStatementsInDockerfileAddedAsPortsInImageConfiguration() {
      // Given
      File dockerFile = new File(Objects.requireNonNull(getClass().getResource("/Dockerfile_expose_ports")).getFile());
      // When
      ImageConfiguration imageConfiguration1 = SimpleDockerfileUtil.createSimpleDockerfileConfig(dockerFile, null);
      // Then
      assertThat(imageConfiguration1.getBuild().getDockerFileRaw()).isEqualTo(dockerFile.getPath());
      assertThat(imageConfiguration1).isNotNull()
          .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
          .extracting(ImageConfiguration::getBuild)
          .extracting(BuildConfiguration::getPorts).isNotNull()
          .asList()
          .hasSize(5)
          .containsExactly("80/tcp", "8080/udp", "80", "8080", "99/udp");
    }
  }

  @Test
  void addSimpleDockerfileConfig() throws IOException {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("test-image")
        .build();
    File dockerFile = Files.createTempFile("Dockerfile", "-test").toFile();

    // When
    ImageConfiguration result = SimpleDockerfileUtil.addSimpleDockerfileConfig(imageConfiguration, dockerFile);

    // Then
    assertThat(result.getBuild()).isNotNull()
        .extracting(BuildConfiguration::getDockerFileRaw)
        .isEqualTo(dockerFile.getPath());
  }

  @Nested
  @DisplayName("extractPorts")
  class ExtractPorts {
    @Test
    @DisplayName("should throw exception on Invalid Dockerfile provided")
    void fromInvalidDockerFile_shouldThrowException() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> SimpleDockerfileUtil.extractPorts(new File("iDoNotExist")))
          .withMessage("Error in reading Dockerfile");
    }

    @Test
    @DisplayName("should extract port numbers from EXPOSE statements in Dockerfile")
    void fromDockerFileLines() {
      // Given
      List<String[]> input1 = Arrays.asList(new String[]{"EXPOSE", "8080", "9090", "9999"} , new String[]{"EXPOSE", "9010"});
      List<String[]> input2 = Arrays.asList(new String[]{"EXPOSE", "9001"}, new String[]{"EXPOSE", null});
      List<String[]> input3 = Arrays.asList(new String[]{"EXPOSE", ""}, new String[]{"EXPOSE", "8001"});

      // When
      List<String> result1 = SimpleDockerfileUtil.extractPorts(input1);
      List<String> result2 = SimpleDockerfileUtil.extractPorts(input2);
      List<String> result3 = SimpleDockerfileUtil.extractPorts(input3);

      // Then
      assertThat(result1).containsExactly("9090", "8080", "9999", "9010");
      assertThat(result2).containsExactly("9001");
      assertThat(result3).containsExactly("8001");
    }
  }
}
