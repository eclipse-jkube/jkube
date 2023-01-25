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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
class OpenshiftHelperStatusTest {

  @DisplayName("OpenshiftHelper isFinished tests")
  @ParameterizedTest(name = "OpenshiftHelper with status ''{0}'' should return ''{4}''")
  @MethodSource("data")
  void testIsFinished(String input, boolean isFinished, boolean isCancelled, boolean isFailed, boolean isCompleted) {
    boolean result = OpenshiftHelper.isFinished(input);
    assertThat(result).isEqualTo(isFinished);
  }

  @DisplayName("OpenshiftHelper isCancelled tests")
  @ParameterizedTest(name = "OpenshiftHelper with status ''{0}'' should return ''{4}''")
  @MethodSource("data")
  void testIsCancelled(String input, boolean isFinished, boolean isCancelled, boolean isFailed, boolean isCompleted) {
    boolean result = OpenshiftHelper.isCancelled(input);
    assertThat(result).isEqualTo(isCancelled);
  }

  @DisplayName("OpenshiftHelper isFailed tests")
  @ParameterizedTest(name = "OpenshiftHelper with status ''{0}'' should return ''{4}''")
  @MethodSource("data")
  void testIsFailed(String input, boolean isFinished, boolean isCancelled, boolean isFailed, boolean isCompleted) {
    boolean result = OpenshiftHelper.isFailed(input);
    assertThat(result).isEqualTo(isFailed);
  }

  @DisplayName("OpenshiftHelper isCompleted tests")
  @ParameterizedTest(name = "OpenshiftHelper with status ''{0}'' should return ''{4}''")
  @MethodSource("data")
  void testIsCompleted(String input, boolean isFinished, boolean isCancelled, boolean isFailed, boolean isCompleted) {
    boolean result = OpenshiftHelper.isCompleted(input);
    assertThat(result).isEqualTo(isCompleted);
  }

  public static Stream<Arguments> data() {
    return Stream.of(
            // input, isFinished, isCancelled, isFailed, isCompleted
            Arguments.arguments("Complete", true, false, false, true),
            Arguments.arguments("Error", true, false, true, false),
            Arguments.arguments("Cancelled", true, true, false, false),
            Arguments.arguments("not Complete", false, false, false, false),
            Arguments.arguments(null, false, false, false, false)
    );
  }
}
