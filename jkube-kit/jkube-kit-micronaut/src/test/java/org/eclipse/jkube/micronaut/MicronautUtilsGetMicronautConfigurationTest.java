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
package org.eclipse.jkube.micronaut;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.micronaut.MicronautUtils.getMicronautConfiguration;

class MicronautUtilsGetMicronautConfigurationTest {
  @TempDir
  private Path temporaryFolder;

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of("properties", "PROPERTIES"),
        Arguments.of("yaml", "YAML"),
        Arguments.of("json", "JSON")
    );
  }

  @ParameterizedTest(name = "Micronaut configuration can be read from application.{0} files")
  @MethodSource("data")
  void getMicronautConfigurationFromProperties(String directory, String nameSuffix) throws IOException {
    // Given
    JavaProject javaProject = JavaProject.builder()
      .compileClassPathElement(MicronautUtilsGetMicronautConfigurationTest.class.getResource(String.format("/utils-test/port-config/%s/", directory)).getPath())
      .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
      .build();
    // When
    final Properties props = getMicronautConfiguration(javaProject);
    // Then
    assertThat(props).containsExactly(
        entry("micronaut.application.name", "port-config-test-" + nameSuffix),
        entry("micronaut.server.port", "1337"));
  }

}
