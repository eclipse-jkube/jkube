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
package org.eclipse.jkube.kit.common;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssemblyConfigurationTest {

  @Test
  public void testDefaultPermissions() {
    assertThat(new AssemblyConfiguration().getPermissions()).isEqualTo(AssemblyConfiguration.PermissionMode.keep);
  }

  @Test
  public void testDefaultMode() {
    assertThat(new AssemblyConfiguration().getMode()).isEqualTo(AssemblyMode.dir);
  }

  @Test
  @Deprecated
  public void getInline_withInlineAndLayers_shouldReturnInline() {
    // Given
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder()
        .layer(Assembly.builder().id("inline-1").build())
        .layer(Assembly.builder().id("inline-2").build())
        .inline(Assembly.builder().id("inline-deprecated").build())
        .layer(Assembly.builder().id("inline-3").build());
    // When
    final AssemblyConfiguration result = builder.build();
    // Then
    assertThat(result.getInline())
        .hasFieldOrPropertyWithValue("id", "inline-deprecated");
  }

  @Test
  public void getLayers_withNoInlineAndNoLayers_shouldReturnEmptyList() {
    // Given
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder()
        .user("test");
    // When
    final AssemblyConfiguration result = builder.build();
    // Then
    assertThat(result.getLayers()).isNotNull().isEmpty();
  }

  @Test
  public void getLayers_withNoInlineAndLayers_shouldReturnInlines() {
    // Given
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder()
        .user("test")
        .layers(Collections.singletonList(Assembly.builder().id("assembly-1").build()));
    // When
    final AssemblyConfiguration result = builder.build();
    // Then
    assertThat(result.getLayers())
        .extracting("id")
        .containsExactly("assembly-1");
  }

  @Test
  public void getLayers_withInlineAndNoLayers_shouldReturnInline() {
    // Given
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder()
        .user("test")
        .inline(Assembly.builder().id("assembly-1").build());
    // When
    final AssemblyConfiguration result = builder.build();
    // Then
    assertThat(result.getLayers())
        .extracting("id")
        .containsExactly("assembly-1");
  }

  @Test
  public void getLayers_withInlineAndLayers_shouldReturnInlinesAndInlineLast() {
    // Given
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder()
        .user("test")
        .layer(Assembly.builder().id("inline-1").build())
        .layer(Assembly.builder().id("inline-2").build())
        .inline(Assembly.builder().id("inline-deprecated").build())
        .layer(Assembly.builder().id("inline-3").build());
    // When
    final AssemblyConfiguration result = builder.build();
    // Then
    assertThat(result.getLayers())
        .extracting("id")
        .containsExactly("inline-1", "inline-2", "inline-3", "inline-deprecated");
  }

  /**
   * Verifies that deserialization works for raw deserialization (Maven-Plexus) disregarding annotations.
   *
   * Especially designed to catch problems if Enum names are changed.
   */
  @Test
  public void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.USE_ANNOTATIONS, false);
    // When
    final AssemblyConfiguration result = mapper.readValue(
        AssemblyConfigurationTest.class.getResourceAsStream("/assembly-configuration.json"),
        AssemblyConfiguration.class
    );
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "assembly")
        .hasFieldOrPropertyWithValue("targetDir", "target")
        .hasFieldOrPropertyWithValue("exportTargetDir", false)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .hasFieldOrPropertyWithValue("permissions", AssemblyConfiguration.PermissionMode.exec)
        .hasFieldOrPropertyWithValue("permissionsRaw", "exec")
        .hasFieldOrPropertyWithValue("mode", AssemblyMode.zip)
        .hasFieldOrPropertyWithValue("modeRaw", "zip")
        .hasFieldOrPropertyWithValue("user", "root")
        .hasFieldOrPropertyWithValue("tarLongFileMode", "posix")
        .extracting(AssemblyConfiguration::getLayers).asList().extracting("id")
        .containsExactly("multi-layer-support", "not-the-last-layer", "deprecated-single");
  }
}
