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

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DekorateUtilTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  void useDekorate(String testDesc, List<Dependency> dependencies, boolean expected) {
    // Given
    JavaProject javaProject = JavaProject.builder()
            .dependencies(dependencies)
            .build();
    // When
    boolean result = DekorateUtil.useDekorate(javaProject);
    //Then
    assertThat(result).isEqualTo(expected);
  }

  public static Stream<Arguments> data() {
    return Stream.of(
            Arguments.arguments("without dekorate dependency should be false", Collections.emptyList(), false),
            Arguments.arguments("with dekorate dependency should be true", Collections.singletonList(
                    Dependency.builder().groupId("io.dekorate").artifactId("kubernetes-annotations").version("0.10.5").build()), true)
    );
  }
}
