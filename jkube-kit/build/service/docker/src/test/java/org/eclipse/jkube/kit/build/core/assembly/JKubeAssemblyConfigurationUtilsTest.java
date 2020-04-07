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
package org.eclipse.jkube.kit.build.core.assembly;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.kit.build.core.config.JKubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeAssemblyFile;
import org.eclipse.jkube.kit.common.JKubeAssemblyFileSet;
import org.eclipse.jkube.kit.common.JKubeProjectAssembly;

import mockit.Expectations;
import mockit.Injectable;
import org.junit.Test;

import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getJKubeAssemblyFileSetsExcludes;
import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getJKubeAssemblyFiles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JKubeAssemblyConfigurationUtilsTest {


  @Test
  public void getAssemblyConfigurationOrCreateDefaultNoConfigurationShouldReturnDefault(
    @Injectable final JKubeBuildConfiguration buildConfiguration) {

    // Given
    new Expectations() {{
      buildConfiguration.getAssemblyConfiguration();
      result = null;
    }};
    // When
    final JKubeAssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertEquals("/maven", result.getTargetDir());
    assertEquals("root", result.getUser());
  }

  @Test
  public void getAssemblyConfigurationOrCreateDefaultWithConfigurationShouldReturnConfiguration(
    @Injectable final JKubeBuildConfiguration buildConfiguration, @Injectable JKubeAssemblyConfiguration configuration) {

    // Given
    new Expectations() {{
      buildConfiguration.getAssemblyConfiguration();
      result = configuration;
      configuration.getUser();
      result = "OtherUser";
      configuration.getName();
      result = "ImageName";
    }};
    // When
    final JKubeAssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertNull(result.getTargetDir());
    assertEquals("OtherUser", result.getUser());
    assertEquals("ImageName", result.getName());
  }


  @Test
  public void getJKubeAssemblyFileSetsNullShouldReturnEmptyList() {
    // When
    final List<JKubeAssemblyFileSet> result = getJKubeAssemblyFileSets(null);
    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getJKubeAssemblyFileSetsNotNullShouldReturnFileSets(
    @Injectable JKubeAssemblyConfiguration configuration, @Injectable JKubeProjectAssembly assembly,
    @Injectable JKubeAssemblyFileSet fileSet) {

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
    final List<JKubeAssemblyFileSet> result = getJKubeAssemblyFileSets(configuration);
    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("1337", result.iterator().next().getDirectory().getName());
  }


  @Test
  public void getJKubeAssemblyFileSetsExcludesShouldReturnExcludes(
    @Injectable JKubeAssemblyConfiguration configuration, @Injectable JKubeProjectAssembly assembly,
    @Injectable JKubeAssemblyFileSet fileSet) {

    // Given
    new Expectations() {{
      configuration.getInline();
      result = assembly;
      assembly.getFileSets();
      result = Arrays.asList(fileSet, null, fileSet, fileSet);
      fileSet.getExcludes();
      result = Collections.singletonList("1337");
      result = Arrays.asList("1","3", null, "3", "7");
      result = null;
    }};
    // When
    final List<String> result = getJKubeAssemblyFileSetsExcludes(configuration);
    // Then
    assertNotNull(result);
    assertEquals(5, result.size());
    assertEquals("1337", result.get(0));
    assertEquals("1", result.get(1));
    assertEquals("3", result.get(2));
    assertEquals("3", result.get(3));
    assertEquals("7", result.get(4));
  }

  @Test
  public void getJKubeAssemblyFilesNullShouldReturnEmptyList() {
    // When
    final List<JKubeAssemblyFile> result = getJKubeAssemblyFiles(null);
    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getJKubeAssemblyFilesNotNullShouldReturnFiles(
    @Injectable JKubeAssemblyConfiguration configuration, @Injectable JKubeProjectAssembly assembly,
    @Injectable JKubeAssemblyFile file) {

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
    final List<JKubeAssemblyFile> result = getJKubeAssemblyFiles(configuration);
    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("1337", result.iterator().next().getSource().getName());
  }

}
