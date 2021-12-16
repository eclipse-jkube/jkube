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
package org.eclipse.jkube.micronaut.generator;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MicronautGeneratorTest {
  @Mocked
  private GeneratorContext ctx;

  @Mocked
  private JavaProject project;

  private MicronautGenerator micronautGenerator;

  @Before
  public void setUp() {
    // @formatter:off
    new Expectations() {{
      ctx.getProject(); result = project;
    }};
    // @formatter:on
    micronautGenerator = new MicronautGenerator(ctx);
  }

  @Test
  public void isApplicableWithNoPlugin() {
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableWithMavenPlugin() {
    // Given
    // @formatter:off
    new Expectations() {{
      project.getPlugins(); result = Collections.singletonList(Plugin.builder()
          .groupId("io.micronaut.build")
          .artifactId("micronaut-maven-plugin")
          .build()
      );
    }};
    // @formatter:on
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableWithGradlePlugin() {
    // Given
    // @formatter:off
    new Expectations() {{
      project.getPlugins(); result = Collections.singletonList(Plugin.builder()
          .groupId("io.micronaut.application")
          .artifactId("io.micronaut.application.gradle.plugin")
          .build()
      );
    }};
    // @formatter:on
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void extractPortsWebIsFirst() {
    // Given
    // @formatter:off
    new Expectations() {{
      project.getCompileClassPathElements(); result = Collections.emptyList();
      project.getOutputDirectory(); result = new File("MOCK");
    }};
    // @formatter:on
    // When
    final List<String> result = micronautGenerator.extractPorts();
    // Then
    assertThat(result).containsExactly(
        "8080",
        "8778",
        "9779"
    );
  }
}
