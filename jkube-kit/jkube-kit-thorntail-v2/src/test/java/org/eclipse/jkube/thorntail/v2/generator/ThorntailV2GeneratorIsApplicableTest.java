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


import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ThorntailV2GeneratorIsApplicableTest {

  private GeneratorContext context;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    context = GeneratorContext.builder()
      .project(JavaProject.builder()
        .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
        .build())
      .logger(new KitLogger.SilentLogger())
      .build();
  }

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of("ThorntailV2 generator SHOULD NOT be applicable if there is no plugin nor dependency",
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false),
        Arguments.of("ThorntailV2 generator SHOULD be applicable if there is thorntail maven plugin",
            Collections.singletonList(Plugin.builder().groupId("io.thorntail").artifactId("thorntail-maven-plugin").build()),
            Collections.emptyList(), Collections.emptyList(), true),
        Arguments.of("ThorntailV2 generator SHOULD be applicable if there is thorntail gradle plugin", Collections.emptyList(),
            Collections.emptyList(), Collections.singletonList("org.wildfly.swarm.plugin.gradle.ThorntailArquillianPlugin"),
            true),
        Arguments.of("ThorntailV2 generator SHOULD NOT be applicable if there is thorntail kernel dependency",
            Collections.singletonList(Plugin.builder().groupId("io.thorntail").artifactId("thorntail-maven-plugin").build()),
            Collections.singletonList(Dependency.builder().groupId("io.thorntail").artifactId("thorntail-kernel").build()),
            Collections.emptyList(), false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  void isApplicable(String testDescription, List<Plugin> pluginList, List<Dependency> dependencyList,
                    List<String> gradlePluginList, boolean expectedValue) {
    // Given
    context = context.toBuilder().project(context.getProject().toBuilder()
      .plugins(pluginList)
      .dependencies(dependencyList)
      .gradlePlugins(gradlePluginList)
      .build()).build();
    // When
    final boolean result = new ThorntailV2Generator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isEqualTo(expectedValue);
  }
}
