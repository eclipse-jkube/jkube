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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.common.util.SemanticVersionUtil.isVersionAtLeast;

class SemanticVersionUtilTest {

  @DisplayName("Major-Minor version tests")
  @ParameterizedTest(name = "{0}")
  @MethodSource("versionTestData")
  void versionTest(String testDesc, int majorVersion, int minorVersion, String version, boolean expected) {
    assertThat(isVersionAtLeast(majorVersion, minorVersion, version)).isEqualTo(expected);
  }

  public static Stream<Arguments> versionTestData() {
    return Stream.of(
            Arguments.arguments("With larger major version should return false", 2, 1, "1.13.7.Final", false),
            Arguments.arguments("With same major and larger minor version should return false", 1, 14, "1.13.7.Final", false),
            Arguments.arguments("With same major and minor version should return true", 1, 13, "1.13.7.Final", true),
            Arguments.arguments("With same major and smaller minor version should return true", 1, 12, "1.13.7.Final", true),
            Arguments.arguments("With smaller major and larger minor version should return true", 0, 12, "1.13.7.Final", true),
            Arguments.arguments("With smaller major and incomplete version should return true", 0, 12, "1.Final", true),
            Arguments.arguments("With invalid version should return false", 2, 1, "two.one.seven.Final", false)
    );
  }

  @ParameterizedTest(name = "given {0} should return {1} without metadata")
  @CsvSource({
      "0.31.0+git-3a994bd.build-5086,0.31.0",
      "24.0.7,24.0.7"
  })
  void removeBuildMetadata_whenSemanticVersionProvided_thenTrimMetadata(String semanticVersion, String expectedVersionWithoutMetadata) {
    assertThat(SemanticVersionUtil.removeBuildMetadata(semanticVersion)).isEqualTo(expectedVersionWithoutMetadata);
  }

}
