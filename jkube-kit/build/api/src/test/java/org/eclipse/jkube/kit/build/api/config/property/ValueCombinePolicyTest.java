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
package org.eclipse.jkube.kit.build.api.config.property;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueCombinePolicyTest {

  @ParameterizedTest(name = "{index}: fromString({0}) = {1}")
  @MethodSource("fromStringEntries")
  void fromString(String valueCombinePolicy, ValueCombinePolicy expected) {
    // When
    final ValueCombinePolicy result = ValueCombinePolicy.fromString(valueCombinePolicy);
    // Then
    assertThat(result).isEqualTo(expected);
  }

  static Stream<Arguments> fromStringEntries() {
    return Stream.of(
      Arguments.of("REPLACE", ValueCombinePolicy.REPLACE),
      Arguments.of("replace", ValueCombinePolicy.REPLACE),
      Arguments.of("MERGE", ValueCombinePolicy.MERGE),
      Arguments.of("merge", ValueCombinePolicy.MERGE)
    );
  }

  @Test
  void fromString_unknown() {
    assertThatThrownBy(() -> ValueCombinePolicy.fromString("unknown"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No value combine policy unknown known. Valid values are: REPLACE, MERGE");
  }
}
