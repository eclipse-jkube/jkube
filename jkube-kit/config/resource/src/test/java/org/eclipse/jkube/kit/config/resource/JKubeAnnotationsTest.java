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
package org.eclipse.jkube.kit.config.resource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JKubeAnnotationsTest {
  @Test
  void value_withUseDeprecatedPrefixFalse_shouldReturnJKubeEclipseOrgPrefix() {
    // Given + When
    String result = JKubeAnnotations.GIT_COMMIT.value();
    // Then
    assertThat(result).isEqualTo("jkube.eclipse.org/git-commit");
  }

  @Test
  void value_withUseDeprecatedPrefixTrue_shouldReturnJKubeIoPrefix() {
    // Given + When
    String result = JKubeAnnotations.GIT_COMMIT.value(true);
    // Then
    assertThat(result).isEqualTo("jkube.io/git-commit");
  }

  @Test
  void toString_whenInvoked_shouldUseJkubeEclipseOrgPrefix() {
    // Given + When
    String result = JKubeAnnotations.GIT_BRANCH.toString();
    // Then
    assertThat(result).isEqualTo("jkube.eclipse.org/git-branch");
  }
}
