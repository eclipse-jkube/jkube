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

import static org.assertj.core.api.Assertions.assertThat;

class JavaProjectTest {

  @ParameterizedTest(name = "{index}: with version = ''{0}'' should return ''{1}''")
  @MethodSource("isSnapshotData")
  void isSnapshot(String version, boolean expected) {
    final JavaProject result = JavaProject.builder()
      .version(version)
      .build();
    assertThat(result).extracting(JavaProject::isSnapshot).isEqualTo(expected);
  }

  static Stream<Arguments> isSnapshotData() {
    return Stream.of(
      Arguments.of(null, false),
      Arguments.of("1.0.0", false),
      Arguments.of("1.0.0-SNAPSHOT", true),
      Arguments.of("1.0.0-snapshot", true),
      Arguments.of("1.0.0-20190214.090012-1-SNAPSHOT", true),
      Arguments.of("1.0.0-20190214.090012-1-snapshot", true),
      Arguments.of("1.0.0-20190214.090012-1-foo", false),
      Arguments.of("1.0.0-20190214.090012-1-FOO", false),
      Arguments.of("1.0.0-20190214.090012-1-SNAP", false),
      Arguments.of("1.0.0-20190214.090012-1-snap", false),
      Arguments.of("1.0.0-20190214.090012-1-SNAPSHOT-FOO", false),
      Arguments.of("1.0.0-20190214.090012-1-snapshot-foo", false)
    );
  }
}
