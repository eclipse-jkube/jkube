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
package org.eclipse.jkube.smallrye;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmallRyeUtilsTest {
  private JavaProject javaProject;

  @BeforeEach
  public void setUp() {
    javaProject = mock(JavaProject.class);
  }

  @Test
  void hasSmallRyeDependency_whenNoSmallRyeFound_thenReturnFalse() {
    // Given
    mockDependencies(Collections.emptyList());

    // When
    boolean result = SmallRyeUtils.hasSmallRyeDependency(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasSmallRyeDependency_whenSmallRyeFound_thenReturnTrue() {
    // Given
    mockDependencies(Collections.singletonList(Dependency.builder()
        .groupId("io.smallrye")
        .artifactId("smallrye-health")
        .build()));

    // When
    boolean result = SmallRyeUtils.hasSmallRyeDependency(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  private void mockDependencies(List<Dependency> dependencyList) {
    when(javaProject.getDependencies()).thenReturn(dependencyList);
  }
}
