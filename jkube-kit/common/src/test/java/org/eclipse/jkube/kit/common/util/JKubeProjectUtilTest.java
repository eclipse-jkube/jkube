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


import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class JKubeProjectUtilTest {

  @Test
  public void hasDependencyWithGroupIdWithNulls() {
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(null, null);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void hasDependencyWithGroupIdWithDependency(@Mocked JavaProject project) {
    // Given
    // @formatter:off
    new Expectations() {{
      project.getDependencies(); result = Arrays.asList(
          Dependency.builder().groupId("io.dep").build(),
          Dependency.builder().groupId("io.dep").artifactId("artifact").version("1.3.37").build(),
          Dependency.builder().groupId("io.other").artifactId("artifact").version("1.3.37").build()
        );
    }};
    // @formatter:on
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(project, "io.dep");
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void hasDependencyWithGroupIdWithNoDependency(@Mocked JavaProject project) {
    // Given
    // @formatter:off
    new Expectations() {{
      project.getDependencies(); result = Arrays.asList(
          Dependency.builder().groupId("io.dep").build(),
          Dependency.builder().groupId("io.dep").artifactId("artifact").version("1.3.37").build(),
          Dependency.builder().groupId("io.other").artifactId("artifact").version("1.3.37").build()
      );
    }};
    // @formatter:on
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(project, "io.nothere");
    // Then
    assertThat(result).isFalse();
  }
}