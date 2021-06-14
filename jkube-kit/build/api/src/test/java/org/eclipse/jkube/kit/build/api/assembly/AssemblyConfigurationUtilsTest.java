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
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFiles;

public class AssemblyConfigurationUtilsTest {

  @Test
  public void getAssemblyConfigurationOrCreateDefaultNoConfigurationShouldReturnDefault(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    // @formatter:off
    new Expectations() {{
      buildConfiguration.getAssembly(); result = null;
    }};
    // @formatter:on
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "maven")
        .hasFieldOrPropertyWithValue("targetDir", "/maven")
        .hasFieldOrPropertyWithValue("user", null);
  }

  @Test
  public void getAssemblyConfigurationOrCreateDefaultWithConfigurationShouldReturnConfiguration(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    final AssemblyConfiguration configuration = AssemblyConfiguration.builder().user("OtherUser").name("ImageName").build();
    // @formatter:off
    new Expectations() {{
      buildConfiguration.getAssembly(); result = configuration;
    }};
    // @formatter:on
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "ImageName")
        .hasFieldOrPropertyWithValue("targetDir", "/ImageName")
        .hasFieldOrPropertyWithValue("user", "OtherUser");
  }

  @Test
  public void getJKubeAssemblyFileSetsNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFileSetsNullFileSetsShouldReturnEmptyList() {
    // Given
    final Assembly assembly = new Assembly();
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(assembly);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFileSetsNotNullShouldReturnFileSets(
      @Injectable Assembly assembly, @Injectable AssemblyFileSet fileSet) {

    // Given
    // @formatter:off
    new Expectations() {{
      assembly.getFileSets(); result = Collections.singletonList(fileSet);
      fileSet.getDirectory(); result = "1337";
    }};
    // @formatter:on
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(assembly);
    // Then
    assertThat(result)
        .isNotNull()
        .hasSize(1).first()
        .hasFieldOrPropertyWithValue("directory.name", "1337");
  }

  @Test
  public void getJKubeAssemblyFilesNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFilesNullFilesShouldReturnEmptyList() {
    // Given
    final Assembly assembly = new Assembly();
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(assembly);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFilesNotNullShouldReturnFiles(
      @Injectable Assembly assembly, @Injectable AssemblyFile file) {

    // Given
    // @formatter:off
    new Expectations() {{
      assembly.getFiles(); result = Collections.singletonList(file);
      file.getSource(); result = new File("1337");
    }};
    // @formatter:on
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(assembly);
    // Then
    assertThat(result)
        .isNotNull()
        .hasSize(1).first()
        .hasFieldOrPropertyWithValue("source.name", "1337");
  }

}
