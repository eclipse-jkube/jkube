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

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.common.util.SemanticVersionUtil.isVersionAtLeast;

public class SemanticVersionUtilTest {
  @Test
  public void isVersionAtLeast_withLargerMajorVersion_shouldBeFalse() {
    assertThat(isVersionAtLeast(2, 1, "1.13.7.Final")).isFalse();
  }

  @Test
  public void isVersionAtLeast_withSameMajorAndLargerMinorVersion_shouldBeFalse() {
    assertThat(isVersionAtLeast(1, 14, "1.13.7.Final")).isFalse();
  }

  @Test
  public void isVersionAtLeast_withSameMajorAndMinorVersion_shouldBeTrue() {
    assertThat(isVersionAtLeast(1, 13, "1.13.7.Final")).isTrue();
  }

  @Test
  public void isVersionAtLeast_withSameMajorAndSmallerMinorVersion_shouldBeTrue() {
    assertThat(isVersionAtLeast(1, 12, "1.13.7.Final")).isTrue();
  }

  @Test
  public void isVersionAtLeast_withSmallerMajorMinorVersion_shouldBeTrue() {
    assertThat(isVersionAtLeast(0, 12, "1.13.7.Final")).isTrue();
  }

  @Test
  public void isVersionAtLeast_withSmallerMajorAndIncompleteVersion_shouldBeTrue() {
    assertThat(isVersionAtLeast(0, 12, "1.Final")).isTrue();
  }

}
