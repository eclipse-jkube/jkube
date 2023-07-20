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
package org.eclipse.jkube.gradle.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.gradle.plugin.GroovyUtil.closureTo;
import static org.eclipse.jkube.gradle.plugin.GroovyUtil.invokeOrParseClosureList;

@SuppressWarnings("unused")
class GroovyUtilTest {

  /**
   * <pre>
   * {@code
   * {
   *   property = 'value'
   *   nested = {
   *     nestedProperty = 'nestedValue'
   *   }
   * }
   * }
   * </pre>
   */
  @Test
  void closureTo_withNestedClosure_shouldReturnStructuredClass() {
    // Given
    final Closure<?> closure = closure(this,
        "property", new GStringImpl(new Object[] { "lue" }, new String[] { "va" }),
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

  /**
   * <pre>
   * {array=['one','two',{nested='closure'}]}
   * </pre>
   */
  @SuppressWarnings("unchecked")
  @Test
  void closureTo_withNestedClosureCollection_shouldReturnMap() {
    // Given
    final Closure<?> closure = closure(this,
        "array", Arrays.asList("one", "two", closure(this, "nested", "closure")));
    // When
    final Map<String, Object> result = closureTo(closure, Map.class);
    // Then
    assertThat(result).hasSize(1)
        .extracting("array").asList().containsExactly("one", "two", Collections.singletonMap("nested", "closure"));
  }

  /**
   * <pre>
   * {@code
   * {
   *   element1 = {
   *     property = 'value'
   *     nestedProperty = 'nestedValue'
   *   }
   *   element2 = {
   *     property = 'value2'
   *     nestedProperty = 'nestedValue2'
   *   }
   * }
   * }
   * </pre>
   */
  @Test
  void invokeOrParseClosureList_namedClosureListTo_withNamedListNestedClosure_shouldReturnOrderedList() {
    // Given
    final Closure<?> element1 = closure(this, "property", "value",
        "nested", closure(this, "nestedProperty", "nestedValue"));
    final Closure<?> element2 = closure(this, "property", "value2",
        "nested", closure(this, "nestedProperty", "nestedValue2"));
    final Closure<?> closure = closure(this, "element1", element1, "element2", element2);
    // When
    final Optional<List<StructuredClass>> result = invokeOrParseClosureList(closure, StructuredClass.class);
    // Then
    assertThat(result).isPresent().get().asList()
        .hasSize(2)
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