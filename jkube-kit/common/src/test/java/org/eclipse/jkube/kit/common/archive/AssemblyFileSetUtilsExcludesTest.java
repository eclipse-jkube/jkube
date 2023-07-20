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
package org.eclipse.jkube.kit.common.archive;

import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AssemblyFileSetUtilsExcludesTest {

  private List<Path> paths;

  @BeforeEach
  public void setUp() throws Exception {
    paths = Arrays.asList(
        Paths.get("usr", "bin"),
        Paths.get("usr", ".git"),
        Paths.get("var", ".git", "refs"),
        Paths.get("var", ".git", "..", "normalized")
    );
  }

  @Test
  void isNotExcluded_withNoExcludes() {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    // When
    final List<Path> filtered = paths.stream()
        .filter(AssemblyFileSetUtils.isNotExcluded(Paths.get(""), afs))
        .collect(Collectors.toList());
    // Then
    assertThat(filtered).isNotEmpty().containsExactlyInAnyOrder(
        Paths.get("usr", "bin"),
        Paths.get("usr", ".git"),
        Paths.get("var", ".git", "refs"),
        Paths.get("var", ".git", "..", "normalized")
    );
  }

  @Test
  void isNotExcluded_withExcludes() {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .exclude("**/.git/**")
        .exclude("**/other/**")
        .build();
    // When
    final List<Path> filtered = paths.stream()
        .filter(AssemblyFileSetUtils.isNotExcluded(Paths.get(""), afs))
        .collect(Collectors.toList());
    // Then
    assertThat(filtered).isNotEmpty().containsExactlyInAnyOrder(
        Paths.get("usr", "bin"),
        Paths.get("usr", ".git"),
        Paths.get("var", ".git", "..", "normalized")
    );
  }

}
