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

  @DisplayName("Major-Minor-Patch version tests")
  @ParameterizedTest(name = "{0}")
  @MethodSource("patchVersionTestData")
  void patchVersionTest(String testDesc, int majorVersion, int minorVersion, int patchVersion, String version, boolean expected) {
    assertThat(isVersionAtLeast(majorVersion, minorVersion, patchVersion, version)).isEqualTo(expected);
  }

  public static Stream<Arguments> patchVersionTestData() {
    return Stream.of(
            // Major version comparisons
            Arguments.arguments("With larger major version should return false", 3, 0, 0, "2.3.2", false),
            Arguments.arguments("With smaller major version should return true", 1, 0, 0, "2.3.2", true),
            // Minor version comparisons (same major)
            Arguments.arguments("With same major and larger minor version should return false", 2, 4, 0, "2.3.2", false),
            Arguments.arguments("With same major and smaller minor version should return true", 2, 2, 0, "2.3.2", true),
            Arguments.arguments("With same major and smaller minor version ignores patch", 2, 2, 9, "2.3.2", true),
            // Patch version comparisons (same major and minor)
            Arguments.arguments("With same major.minor and larger patch should return false", 2, 3, 3, "2.3.2", false),
            Arguments.arguments("With same major.minor and same patch should return true", 2, 3, 2, "2.3.2", true),
            Arguments.arguments("With same major.minor and smaller patch should return true", 2, 3, 1, "2.3.2", true),
            Arguments.arguments("With same major.minor and zero patch should return true", 2, 3, 0, "2.3.2", true),
            // Edge cases for Spring Boot version check (2.3.2 cutoff)
            Arguments.arguments("Spring Boot 2.3.0 is not at least 2.3.2", 2, 3, 2, "2.3.0", false),
            Arguments.arguments("Spring Boot 2.3.1 is not at least 2.3.2", 2, 3, 2, "2.3.1", false),
            Arguments.arguments("Spring Boot 2.3.2 is at least 2.3.2", 2, 3, 2, "2.3.2", true),
            Arguments.arguments("Spring Boot 2.3.3 is at least 2.3.2", 2, 3, 2, "2.3.3", true),
            Arguments.arguments("Spring Boot 2.4.0 is at least 2.3.2", 2, 3, 2, "2.4.0", true),
            Arguments.arguments("Spring Boot 3.0.0 is at least 2.3.2", 2, 3, 2, "3.0.0", true),
            Arguments.arguments("Spring Boot 2.2.13 is not at least 2.3.2", 2, 3, 2, "2.2.13", false),
            Arguments.arguments("Spring Boot 1.5.0 is not at least 2.3.2", 2, 3, 2, "1.5.0", false),
            // Version without patch (e.g., "2.3") should treat patch as 0
            Arguments.arguments("Version 2.3 (no patch) is not at least 2.3.2", 2, 3, 2, "2.3", false),
            Arguments.arguments("Version 2.3 (no patch) is at least 2.3.0", 2, 3, 0, "2.3", true),
            Arguments.arguments("Version 2.4 (no patch) is at least 2.3.2", 2, 3, 2, "2.4", true),
            // Version with suffix (e.g., "1.13.7.Final")
            Arguments.arguments("Version with suffix and larger patch should return false", 1, 13, 8, "1.13.7.Final", false),
            Arguments.arguments("Version with suffix and same patch should return true", 1, 13, 7, "1.13.7.Final", true),
            Arguments.arguments("Version with suffix and smaller patch should return true", 1, 13, 6, "1.13.7.Final", true),
            // Invalid versions
            Arguments.arguments("With invalid version should return false", 2, 3, 2, "invalid.version", false),
            Arguments.arguments("With empty version should return false", 2, 3, 2, "", false),
            Arguments.arguments("With null-like version should return false", 2, 3, 2, "   ", false)
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
