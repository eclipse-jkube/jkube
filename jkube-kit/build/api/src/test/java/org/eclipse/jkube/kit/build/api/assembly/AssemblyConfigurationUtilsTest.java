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
package org.eclipse.jkube.kit.build.api.assembly;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Expectations;
import mockit.Injectable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFiles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("TestMethodWithIncorrectSignature")
public class AssemblyConfigurationUtilsTest {

  @Test
  public void getAssemblyConfigurationOrCreateDefaultNoConfigurationShouldReturnDefault(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    new Expectations() {{
      buildConfiguration.getAssemblies();
      result = Collections.emptyList();
    }};
    // When
    final List<AssemblyConfiguration> result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result).hasSize(1).first()
        .hasFieldOrPropertyWithValue("name", "maven")
        .hasFieldOrPropertyWithValue("targetDir", "/maven")
        .hasFieldOrPropertyWithValue("user", null);
  }

  @Test
  public void getAssemblyConfigurationOrCreateDefaultWithConfigurationShouldReturnConfiguration(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    final AssemblyConfiguration configuration = AssemblyConfiguration.builder().user("OtherUser").name("ImageName").build();
    new Expectations() {{
      buildConfiguration.getAssemblies();
      result = Collections.singletonList(configuration);
    }};
    // When
    final List<AssemblyConfiguration> result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result).hasSize(1).first()
        .hasFieldOrPropertyWithValue("name", "ImageName")
        .hasFieldOrPropertyWithValue("targetDir", "/ImageName")
        .hasFieldOrPropertyWithValue("user", "OtherUser");
  }

  @Test
  public void getAssemblyConfigurationOrCreateDefaultWithMultipleConfigurationShouldReturnConfigurationAndDefault(
      @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    final AssemblyConfiguration configuration = AssemblyConfiguration.builder().user("OtherUser").name("ImageName").build();
    new Expectations() {{
      buildConfiguration.getAssemblies();
      result = Arrays.asList(configuration, new AssemblyConfiguration());
    }};
    // When
    final List<AssemblyConfiguration> result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result).hasSize(2).extracting("name", "targetDir", "user")
        .containsExactly(
            tuple("ImageName", "/ImageName", "OtherUser"),
            tuple("maven", "/maven", null)
        );
  }


  @Test
  public void getJKubeAssemblyFileSetsNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(null);
    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getJKubeAssemblyFileSetsNotNullShouldReturnFileSets(
    @Injectable AssemblyConfiguration configuration, @Injectable Assembly assembly,
    @Injectable AssemblyFileSet fileSet) {

    // Given
    new Expectations() {{
      configuration.getInline();
      result = assembly;
      assembly.getFileSets();
      result = Collections.singletonList(fileSet);
      fileSet.getDirectory();
      result = "1337";
    }};
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(configuration);
    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("1337", result.iterator().next().getDirectory().getName());
  }



  @Test
  public void getJKubeAssemblyFilesNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(null);
    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getJKubeAssemblyFilesNotNullShouldReturnFiles(
    @Injectable AssemblyConfiguration configuration, @Injectable Assembly assembly,
    @Injectable AssemblyFile file) {

    // Given
    new Expectations() {{
      configuration.getInline();
      result = assembly;
      assembly.getFiles();
      result = Collections.singletonList(file);
      file.getSource();
      result = new File("1337");
    }};
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(configuration);
    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("1337", result.iterator().next().getSource().getName());
  }

}
