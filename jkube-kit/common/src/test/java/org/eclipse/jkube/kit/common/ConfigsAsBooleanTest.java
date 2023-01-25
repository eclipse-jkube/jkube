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
package org.eclipse.jkube.kit.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigsAsBooleanTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  void asBooleanTest(String testDesc, String value, boolean expected) {
    assertThat(Configs.asBoolean(value)).isEqualTo(expected);
  }

  public static Stream<Arguments> data() {
    return Stream.of(
            Arguments.arguments("With Unsupported String should return false", " 1 2 1337", false),
            Arguments.arguments("With One should return false", "1", false),
            Arguments.arguments("With Zero should return false", "0", false),
            Arguments.arguments("With True should return true", "true", true),
            Arguments.arguments("With True uppercase should return true", "TRUE", true),
            Arguments.arguments("With True mixed case should return true", "TrUE", true),
            Arguments.arguments("With False mixed case should return false", "fALsE", false),
            Arguments.arguments("With False should return false", "false", false)
    );
  }

}
