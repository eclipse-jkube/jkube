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
package org.eclipse.jkube.kit.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BuildRecreateModeTest {
  static Stream<Arguments> fromParameterSource() {
    return Stream.of(
      arguments("buildConfig", BuildRecreateMode.buildConfig),
      arguments("bc", BuildRecreateMode.bc),
      arguments("imageStream", BuildRecreateMode.imageStream),
      arguments("is", BuildRecreateMode.is),
      arguments("all", BuildRecreateMode.all),
      arguments("none", BuildRecreateMode.none)
    );
  }

  @ParameterizedTest(name = "fromParameter({0}) should return {1}")
  @MethodSource("fromParameterSource")
  void fromParameter_shouldParseEnumCorrectly(String param, BuildRecreateMode expectedValue) {
    assertThat(BuildRecreateMode.fromParameter(param)).isEqualTo(expectedValue);
  }
}
