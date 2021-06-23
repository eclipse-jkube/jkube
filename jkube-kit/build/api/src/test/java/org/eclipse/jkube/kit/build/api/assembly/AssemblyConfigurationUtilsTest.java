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
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Expectations;
import mockit.Injectable;
import org.junit.Test;

import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFiles;
import static org.assertj.core.api.Assertions.assertThat;

public class AssemblyConfigurationUtilsTest {

  @Test
  public void getAssemblyConfigurationOrCreateDefaultNoConfigurationShouldReturnDefault(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    new Expectations() {{
      buildConfiguration.getAssembly();
      result = null;
    }};
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result.getName()).isEqualTo("maven");
    assertThat(result.getTargetDir()).isEqualTo("/maven");
    assertThat(result.getUser()).isNull();
  }

  @Test
  public void getAssemblyConfigurationOrCreateDefaultWithConfigurationShouldReturnConfiguration(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    final AssemblyConfiguration configuration = AssemblyConfiguration.builder().user("OtherUser").name("ImageName").build();
    new Expectations() {{
      buildConfiguration.getAssembly();
      result = configuration;
    }};
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("ImageName");
    assertThat(result.getTargetDir()).isEqualTo("/ImageName");
    assertThat(result.getUser()).isEqualTo("OtherUser");
  }


  @Test
  public void getJKubeAssemblyFileSetsNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(null);
    // Then
    assertThat(result).isNotNull();
    assertThat(result.isEmpty());
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
    assertThat(result).isNotNull();
    assertThat(result.isEmpty()).isFalse();
    assertThat(result.iterator().next().getDirectory().getName()).isEqualTo("1337");
  }



  @Test
  public void getJKubeAssemblyFilesNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(null);
    // Then
    assertThat(result).isNotNull();
    assertThat(result.isEmpty());
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
    assertThat(result).isNotNull();
    assertThat(result.isEmpty()).isFalse();
    assertThat(result.iterator().next().getSource().getName()).isEqualTo("1337");
  }

}
