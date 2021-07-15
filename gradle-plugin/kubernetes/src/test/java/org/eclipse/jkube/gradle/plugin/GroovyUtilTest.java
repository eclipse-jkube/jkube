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
package org.eclipse.jkube.gradle.plugin;

import java.util.List;
import java.util.Objects;

import groovy.lang.Closure;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.gradle.plugin.GroovyUtil.closureTo;
import static org.eclipse.jkube.gradle.plugin.GroovyUtil.namedListClosureTo;

public class GroovyUtilTest {

  @Test
  public void closureTo_withNestedClosure_shouldReturnStructuredClass() {
    // Given
    final Closure<?> closure = closure(this,
        "property", "value",
        "nested", closure(this, "nestedProperty", "nestedValue"));
    // When
    final StructuredClass result = closureTo(closure, StructuredClass.class);
    // Then
    assertThat(result).isNotNull()
        .hasFieldOrPropertyWithValue("property", "value")
        .hasFieldOrPropertyWithValue("nested.nestedProperty", "nestedValue")
        .extracting(sc -> sc.nested)
        .hasFieldOrPropertyWithValue("nestedProperty", "nestedValue");
  }

  @Test
  public void namedListClosureTo_withNamedListNestedClosure_shouldReturnOrderedList() {
    // Given
    final Closure<?> element1 = closure(this, "property", "value",
        "nested", closure(this, "nestedProperty", "nestedValue"));
    final Closure<?> element2 = closure(this, "property", "value2",
        "nested", closure(this, "nestedProperty", "nestedValue2"));
    final Closure<?> closure = closure(this, "element1", element1, "element2", element2);
    // When
    final List<StructuredClass> result = namedListClosureTo(closure, StructuredClass.class);
    // Then
    assertThat(result).hasSize(2)
        .extracting("property", "nested.nestedProperty")
        .containsExactly(
            tuple("value", "nestedValue"),
            tuple("value2", "nestedValue2")
        );
  }

  private static Closure<Object> closure(Object owner, Object... propPairs) {
    if (propPairs.length % 2 != 0) {
      throw new AssertionError("Invalid number of propPairs, should be even (key, value)");
    }
    return new Closure<Object>(owner) {

      public Object doCall(Object... args) {
        for (int it = 0; it < propPairs.length; it++) {
          setProperty(Objects.toString(propPairs[it]), propPairs[++it]);
        }
        return null;
      }

    };
  }
  static final class StructuredClass {
    public String property;
    public NestedStructuredClass nested;
  }
  static final class NestedStructuredClass {
    public String nestedProperty;
  }
}