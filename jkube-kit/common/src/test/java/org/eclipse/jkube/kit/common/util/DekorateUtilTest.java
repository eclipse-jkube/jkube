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
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class DekorateUtilTest {
  @Test
  public void useDekorate_withNoDekorateDependency_returnsFalse() {
    // Given
    JavaProject javaProject = JavaProject.builder().build();

    // When
    boolean result = DekorateUtil.useDekorate(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void useDekorate_withDekorateDependency_returnsTrue() {
    // Given
    JavaProject javaProject = JavaProject.builder()
      .dependency(Dependency.builder()
        .groupId("io.dekorate").artifactId("kubernetes-annotations").version("0.10.5")
        .build())
      .build();

    // When
    boolean result = DekorateUtil.useDekorate(javaProject);

    // Then
    assertThat(result).isTrue();
  }
}
