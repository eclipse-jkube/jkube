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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class HelmParameter {

  private static final String GOLANG_EXPRESSION_REGEX = "\\{\\{.+}}";

  private boolean required;
  private String name;
  private Object value;

  boolean isString() {
    return value instanceof String;
  }

  boolean isGolangExpression() {
    return isString() && Stream.of(value.toString().split("\r?\n"))
        .filter(StringUtils::isNotBlank)
        .allMatch(s -> s.trim().matches(GOLANG_EXPRESSION_REGEX));
  }

  String toExpression() {
    // Golang expressions are placed directly on the template, they won't be added to the values.yaml file
    if (isGolangExpression()) {
      return StringUtils.trimToEmpty(getValue().toString());
    }
    final String requiredExpression;
    if (isRequired()) {
      requiredExpression = "required \"A valid .Values." + getName() + " entry required!\" ";
    } else {
      requiredExpression = "";
    }
    return "{{ " + requiredExpression + ".Values." + getName() + " }}";
  }
}
