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

import java.util.Properties;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigsTest {

  public enum ConfigWithDefaults implements Configs.Config {
    ONE, TWO;
  }

  @AllArgsConstructor
  public enum ConfigWithImplementations implements Configs.Config {
    ONE("one", "this is the default value one"),
    TWO("two", "default for two");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  @Test
  void asIntValueWithValidStringShouldReturnParsed() {
    // When
    final int result = Configs.asInt("2");
    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test
  void asIntValueWithInvalidStringShouldThrowException() {
    assertThrows(NumberFormatException.class, () -> Configs.asInt("2.15"));
  }

  @Test
  void asIntValueWithNullShouldReturnZero() {
    // When
    final int result = Configs.asInt(null);
    // Then
    assertThat(result).isZero();
  }

  @Test
  void asIntegerValueWithValidStringShouldReturnParsed() {
    // When
    final Integer result = Configs.asInteger("2");
    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test
  void asIntegerValueWithInvalidStringShouldThrowException() {
    assertThrows(NumberFormatException.class, () -> Configs.asInteger("2.15"));
  }

  @Test
  void asIntegerValueWithNullShouldReturnNull() {
    // When
    final Integer result = Configs.asInteger(null);
    // Then
    assertThat(result).isNull();
  }

  @Test
  void getStringValueTest() {
    String test = RandomStringUtils.randomAlphabetic(10);
    assertThat(Configs.asString(test)).isEqualTo(test);
  }

  @DisplayName("System Properties tests")
  @ParameterizedTest(name = "{0}")
  @MethodSource("propertyTestData")
  void propertyTest(String testDesc, String systemKey, String systemValue, String fallbackKey, String fallbackValue, String expected) {
    // Given
    try {
      System.setProperty(systemKey, systemValue);
      // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed
      final Properties fallback = new Properties();
      fallback.put(fallbackKey, fallbackValue);
      // When
      final String result = Configs.getFromSystemPropertyWithPropertiesAsFallback(fallback, "key");
      // Then
      assertThat(result).isEqualTo(expected);
    } finally {
      System.clearProperty(systemKey);
    }
  }

  public static Stream<Arguments> propertyTestData() {
    return Stream.of(
            Arguments.arguments("System Property with Properties as Fallback has Key in System should return system value", "key", "systemValue", "key", "fallbackValue", "systemValue"),
            Arguments.arguments("System Property with Properties as Fallback has not key in System should return fallback value", "not-the-key", "systemValue", "key", "fallbackValue", "fallbackValue"),
            Arguments.arguments("System Property with Properties as Fallback has not key should return null", "not-the-key", "systemValue", "not-the-key-either", "fallbackValue", null)
    );
  }

  @DisplayName("Config interface tests without implementation")
  @ParameterizedTest(name = "{0}")
  @MethodSource("configInterfaceWithDefaultsTestData")
  void configInterfaceWithDefaultsTest(String testDesc, ConfigWithDefaults config, String key, String defaultValue) {
    assertThat(config.getKey()).isEqualTo(key);
    assertThat(config.getDefaultValue()).isEqualTo(defaultValue);
  }

  public static Stream<Arguments> configInterfaceWithDefaultsTestData() {
    return Stream.of(
            Arguments.arguments("ONE's key must be ONE and default value must be null", ConfigWithDefaults.ONE, "ONE", null),
            Arguments.arguments("TWO's key must be TWO and default value must be null", ConfigWithDefaults.TWO, "TWO", null)
    );
  }

  @DisplayName("Config interface tests with implementation")
  @ParameterizedTest(name = "{0}")
  @MethodSource("configInterfaceWithImplementationsTestData")
  void configInterfaceWithImplementationsTest(String testDesc, ConfigWithImplementations config, String key, String defaultValue) {
    assertThat(config.getKey()).isEqualTo(key);
    assertThat(config.getDefaultValue()).isEqualTo(defaultValue);
  }

  public static Stream<Arguments> configInterfaceWithImplementationsTestData() {
    return Stream.of(
            Arguments.arguments("ONE's key must be one and default value must be \"this is the default value one\"", ConfigWithImplementations.ONE, "one", "this is the default value one"),
            Arguments.arguments("TWO's key must be two and default value must be \"default for two\"", ConfigWithImplementations.TWO, "two", "default for two")
    );
  }
}
