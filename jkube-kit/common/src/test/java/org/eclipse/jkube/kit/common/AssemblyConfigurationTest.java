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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class AssemblyConfigurationTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

  @Test
  public void getProcessedLayers_withFlat_shouldReturnOriginalLayers() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder().build()).build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .layer(Assembly.builder().file(AssemblyFile.builder().destName("test").build()).build())
        .build().getFlattenedClone(configuration);
    // Then
    final List<Assembly> result = ac.getProcessedLayers(configuration);
    // Then
    assertThat(result).hasSize(1).first()
        .hasFieldOrPropertyWithValue("id", null)
        .extracting(Assembly::getFiles).asList().hasSize(1).first()
        .hasFieldOrPropertyWithValue("destName", "test");

  }

  @Test
  public void getProcessedLayers_withSingleNoIdLayer_shouldAddIdToLayer() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder().build()).build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .layer(Assembly.builder().file(AssemblyFile.builder().destName("test").build()).build())
        .build();
    // Then
    final List<Assembly> result = ac.getProcessedLayers(configuration);
    // Then
    assertThat(result).hasSize(1).first()
        .hasFieldOrPropertyWithValue("id", "jkube-generated-layer-original")
        .extracting(Assembly::getFiles).asList().hasSize(1).first()
        .hasFieldOrPropertyWithValue("destName", "test");
  }

  @Test
  public void getProcessedLayers_withMultipleNoIdLayer_shouldReturnUnmodifiedLayers() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder().build()).build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .layer(Assembly.builder().file(AssemblyFile.builder().destName("test").build()).build())
        .layer(Assembly.builder().file(AssemblyFile.builder().destName("other").build()).build())
        .build();
    // Then
    final List<Assembly> result = ac.getProcessedLayers(configuration);
    // Then
    assertThat(result).hasSize(2)
        .allSatisfy(a -> assertThat(a).hasFieldOrPropertyWithValue("id", null))
        .flatExtracting(Assembly::getFiles).extracting("destName")
        .containsExactly("test", "other");
  }

  @Test
  public void getProcessedLayers_withSingleNoIdLayerAndArtifact_shouldReturnOriginalAndArtifactLayers() throws IOException {
    // Given
    final File buildDirectory = temporaryFolder.newFolder("target");
    FileUtils.touch(new File(buildDirectory, "final-artifact.jar"));
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder()
            .buildDirectory(buildDirectory)
            .buildFinalName("final-artifact")
            .packaging("jar")
            .build()).build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .layer(Assembly.builder().file(AssemblyFile.builder().destName("test").build()).build())
        .build();
    // Then
    final List<Assembly> result = ac.getProcessedLayers(configuration);
    // Then
    assertThat(result).hasSize(2)
        .anySatisfy(layer -> assertThat(layer)
            .hasFieldOrPropertyWithValue("id", "jkube-generated-layer-original")
            .extracting(Assembly::getFiles).asList().hasSize(1).first()
            .hasFieldOrPropertyWithValue("destName", "test")
        )
        .element(1)
        .hasFieldOrPropertyWithValue("id", "jkube-generated-layer-final-artifact")
        .extracting(Assembly::getFiles).asList().hasSize(1).first()
        .hasFieldOrPropertyWithValue("destName", "final-artifact.jar");
  }

  @Test
  public void getFlattenedClone_withAlreadyFlattened_shouldThrowException() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder().build()).build();
    final AssemblyConfiguration alreadyFlat = AssemblyConfiguration.builder().build().getFlattenedClone(configuration);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class,
        () -> alreadyFlat.getFlattenedClone(configuration));
    // Then
    assertThat(result)
        .hasMessage("This image has already been flattened, you can only flatten the image once");
  }

  @Test
  public void getFlattenedClone_withNoInlineAndNoLayers_shouldReturnEmpty() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder().build()).build();
    // When
    final AssemblyConfiguration result = AssemblyConfiguration.builder().build();
    // Then
    assertThat(result.getFlattenedClone(configuration))
        .hasFieldOrPropertyWithValue("flattened", true)
        .extracting(AssemblyConfiguration::getLayers).asList().hasSize(1).first()
        .hasFieldOrPropertyWithValue("id", null)
        .hasFieldOrPropertyWithValue("fileSets", Collections.emptyList())
        .hasFieldOrPropertyWithValue("files", Collections.emptyList());
  }

  @Test
  public void getFlattenedClone_withInlineAndLayers_shouldReturnInlinesAndInlineLast() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder().build()).build();
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder()
        .user("test")
        .layer(Assembly.builder().id("inline-1").file(AssemblyFile.builder().destName("file.1").build()).build())
        .layer(Assembly.builder().id("inline-2")
            .fileSet(AssemblyFileSet.builder().directory(new File("target")).build()).build())
        .inline(Assembly.builder().id("inline-deprecated")
            .file(AssemblyFile.builder().destName("file.old").build()).build())
        .layer(Assembly.builder().id("inline-3")
            .fileSet(AssemblyFileSet.builder().directory(new File("static")).build())
            .file(AssemblyFile.builder().destName("file.2").build())
            .file(AssemblyFile.builder().destName("file.3").build()).build());
    // When
    final AssemblyConfiguration result = builder.build();
    // Then
    assertThat(result.getFlattenedClone(configuration))
        .hasFieldOrPropertyWithValue("flattened", true)
        .extracting(AssemblyConfiguration::getLayers).asList().hasSize(1)
        .first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .hasFieldOrPropertyWithValue("id", null)
        .hasFieldOrPropertyWithValue("fileSets", Arrays.asList(
            AssemblyFileSet.builder().directory(new File("target")).build(),
            AssemblyFileSet.builder().directory(new File("static")).build()
        ))
        .extracting(Assembly::getFiles).asList()
        .extracting("destName")
        .containsExactly("file.1", "file.2", "file.3", "file.old");
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
        .hasFieldOrPropertyWithValue("flattened", false)
        .extracting(AssemblyConfiguration::getLayers).asList().extracting("id")
        .containsExactly("multi-layer-support", "not-the-last-layer", "deprecated-single");
  }
}
