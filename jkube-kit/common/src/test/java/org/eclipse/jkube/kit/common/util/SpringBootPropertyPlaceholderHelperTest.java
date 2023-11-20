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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jkube.kit.common.util.SpringBootUtilTest.createClassLoader;

class SpringBootPropertyPlaceholderHelperTest {

  private Properties result;

  @BeforeEach
  void setUp(@TempDir File tempDir) throws IOException {
    final URLClassLoader classLoader = createClassLoader(tempDir, "/util/springboot/spring-boot-complex-application.properties");
    result =  SpringBootUtil.getSpringBootApplicationProperties(classLoader);
  }

  @Test
  void resolvesStandard() {
    assertThat(result).containsEntry("custom.property.name", "my-application");
  }

  @Test
  void resolvesSimpleDefaultValue() {
    assertThat(result).containsEntry("server.port", "8082");
  }

  @Test
  void resolvesStackedDefaultValue() {
    assertThat(result).containsEntry("custom.property.db-host", "example.com");
  }

  @Test
  void resolvesInterleavedWithStackedDefaultValue() {
    assertThat(result).containsEntry("app.description", "An application with the name my-application");
  }

  @Test
  void resolvesMultipleInterleavedWithStackedDefaultValue() {
    assertThat(result).containsEntry("custom.property.url", "https://example.com");
  }

  @Test
  void resolvesMultipleInterleavedWithDeepStackedDefaultValue() {
    assertThat(result).containsEntry("spring.datasource.url", "jdbc:postgresql://example.com/production");
  }

  @Test
  void resolvesNestedWithStackedDefaultValue() {
    assertThat(result).containsEntry("custom.property.nested", "dev.example.com");
  }
  @Test
  void resolvesDefaultNestedValue() {
    assertThat(result).containsEntry("custom.property.nested.default", "dev");
  }

  @Test
  void failsWithCircular(@TempDir Path tempDir) throws IOException {
    final Path applicationProperties = tempDir.resolve("application.properties");
    Files.write(applicationProperties, Collections.singletonList("custom.property.circular=${custom.property.circular}"));
    try (URLClassLoader classLoader = ClassUtil.createClassLoader(Arrays.asList(
      tempDir.toFile().getAbsolutePath(),
      applicationProperties.toFile().getAbsolutePath()
    ))) {
      assertThatThrownBy(() -> SpringBootUtil.getSpringBootApplicationProperties(classLoader))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Circular placeholder reference 'custom.property.circular' in property definitions");
    }
  }

  @Test
  void ignoresUnresolvedValues() {
    assertThat(result).containsEntry("custom.property.unresolved", "${unresolved}");
  }
}
