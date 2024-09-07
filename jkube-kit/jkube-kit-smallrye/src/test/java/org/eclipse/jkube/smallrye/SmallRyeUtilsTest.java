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
package org.eclipse.jkube.smallrye;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SmallRyeUtilsTest {
  private JavaProject javaProject;

  @BeforeEach
  public void setUp() {
    javaProject = JavaProject.builder().build();
  }

  @Test
  void hasSmallRyeDependency_whenNoSmallRyeFound_thenReturnFalse() {
    // Given
    javaProject.setDependencies(Collections.emptyList());

    // When
    boolean result = SmallRyeUtils.hasSmallRyeDependency(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasSmallRyeDependency_whenSmallRyeFound_thenReturnTrue() {
    // Given
    javaProject.setDependencies(Collections.singletonList(Dependency.builder()
        .groupId("io.smallrye")
        .artifactId("smallrye-health")
        .build()));

    // When
    boolean result = SmallRyeUtils.hasSmallRyeDependency(javaProject);

    // Then
    assertThat(result).isTrue();
  }
}
