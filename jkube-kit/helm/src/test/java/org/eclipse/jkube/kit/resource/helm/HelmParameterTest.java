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
package org.eclipse.jkube.kit.resource.helm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HelmParameterTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "{{ .Values.foo }}",
    "{{ .Values.foo }} {{ .Values.bar }}",
    "    {{ .Values.spaces }}   ",
    "    {{ .Values.spaces.and.line }} \n \n  ",
    "{{ .Values.foo }} \n {{ .Values.bar }} \n"
  })
  void isGolangExpressionWithGolangExpressionReturnsTrue(String expression) {
    assertThat(HelmParameter.builder().value(expression).build().isGolangExpression()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "1",
      "annotation \n {{ .Values.unsupported.Expression }}"
    })
  void isGolangExpressionWithStringValueReturnsFalse(String value) {
    assertThat(HelmParameter.builder().value(value).build().isGolangExpression()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, Integer.MAX_VALUE, Integer.MIN_VALUE})
  void isGolangExpressionWithIntValueReturnsFalse(int value) {
    assertThat(HelmParameter.builder().value(value).build().isGolangExpression()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isGolangExpressionWithBooleanValueReturnsFalse(boolean value) {
    assertThat(HelmParameter.builder().value(value).build().isGolangExpression()).isFalse();
  }
}
