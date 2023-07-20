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

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getValueFromProperties;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;

class PropertiesUtilTest {

  @Test
  void testPropertiesParsing() {
    // When
    Properties result = getPropertiesFromResource(PropertiesUtilTest.class.getResource("/util/test-application.properties"));
    // Then
    assertThat(result).containsOnly(
        entry("management.port", "8081"),
        entry("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306"),
        entry("example.nested.items[0].name", "item0"),
        entry("example.nested.items[0].value", "value0"),
        entry("example.nested.items[1].name", "item1"),
        entry("example.nested.items[1].value", "value1")
    );
  }

  @Test
  void testNonExistentPropertiesParsing() {
    // When
    Properties result = getPropertiesFromResource(PropertiesUtilTest.class.getResource("/this-file-does-not-exist"));
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testGetValueFromProperties() {
    // Given
    Properties properties = new Properties();
    String[] keys = new String[] {"property1", "property2"};

    // When
    String imageName = getValueFromProperties(properties, keys);

    // Then
    assertThat(imageName).isNull();
  }

  @Test
  void testGetValueFromPropertiesReturnsValidValue() {
    // Given
    Properties properties = new Properties();
    properties.put("property1", "value1");
    String[] keys = new String[] {"property1", "property2"};

    // When
    String imageName = getValueFromProperties(properties, keys);

    // Then
    assertThat(imageName).isNotNull().isEqualTo("value1");
  }

  @Test
  void toMap_null_shouldReturnEmpty() {
    // When
    final Map<String, String> result = toMap(null);
    // Then
    assertThat(result)
        .isNotNull()
        .isEmpty();
  }

  @Test
  void toMap_empty_shouldReturnEmpty() {
    // When
    final Map<String, String> result = toMap(new Properties());
    // Then
    assertThat(result)
        .isNotNull()
        .isEmpty();
  }

  @Test
  void toMap_validProperties_shouldReturnValidMap() {
    // Given
    final Properties properties = new Properties();
    properties.put("Number", 1L);
    properties.put("String", "string");
    properties.put("Char", 'c');
    // When
    final Map<String, String> result = toMap(properties);
    // Then
    assertThat(result)
        .containsOnly(
            entry("Number", "1"),
            entry("String", "string"),
            entry("Char", "c")
        );
  }
}
