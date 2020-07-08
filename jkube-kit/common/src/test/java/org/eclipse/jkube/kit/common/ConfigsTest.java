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
package org.eclipse.jkube.kit.common;

import java.util.Properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConfigsTest {

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
  public void configInterfaceWithDefaults() {
    assertThat(ConfigWithDefaults.ONE.getKey()).isEqualTo("ONE");
    assertThat(ConfigWithDefaults.ONE.getDefaultValue()).isNull();
    assertThat(ConfigWithDefaults.TWO.getKey()).isEqualTo("TWO");
    assertThat(ConfigWithDefaults.TWO.getDefaultValue()).isNull();
  }

  @Test
  public void configInterfaceWithImplementations() {
    assertThat(ConfigWithImplementations.ONE.getKey()).isEqualTo("one");
    assertThat(ConfigWithImplementations.ONE.getDefaultValue()).isEqualTo("this is the default value one");
    assertThat(ConfigWithImplementations.TWO.getKey()).isEqualTo("two");
    assertThat(ConfigWithImplementations.TWO.getDefaultValue()).isEqualTo("default for two");
  }

  @Test
  public void asIntValueWithValidStringShouldReturnParsed() {
    // When
    final int result = Configs.asInt("2");
    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test(expected = NumberFormatException.class)
  public void asIntValueWithInvalidStringShouldThrowException() {
    // When
    Configs.asInt("2.15");
    // Then
    fail();
  }

  @Test
  public void asIntValueWithNullShouldReturnZero() {
    // When
    final int result = Configs.asInt(null);
    // Then
    assertThat(result).isZero();
  }

  @Test
  public void asIntegerValueWithValidStringShouldReturnParsed() {
    // When
    final Integer result = Configs.asInteger("2");
    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test(expected = NumberFormatException.class)
  public void asIntegerValueWithInvalidStringShouldThrowException() {
    // When
    Configs.asInteger("2.15");
    // Then
    fail();
  }

  @Test
  public void asIntegerValueWithNullShouldReturnNull() {
    // When
    final Integer result = Configs.asInteger(null);
    // Then
    assertThat(result).isNull();
  }

  @Test
  public void asBooleanValueWithUnsupportedStringShouldReturnFalse() {
    // When
    final boolean result = Configs.asBoolean(" 1 2 1337");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void asBooleanValueWithOneShouldReturnFalse() {
    // When
    final boolean result = Configs.asBoolean("1");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void asBooleanValueWithZeroShouldReturnFalse() {
    // When
    final boolean result = Configs.asBoolean("0");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void asBooleanValueWithTrueShouldReturnTrue() {
    // When
    final boolean result = Configs.asBoolean("true");
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void asBooleanValueWithTrueUpperCaseShouldReturnTrue() {
    // When
    final boolean result = Configs.asBoolean("TRUE");
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void asBooleanValueWithTrueMixedCaseShouldReturnTrue() {
    // When
    final boolean result = Configs.asBoolean("TrUE");
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void asBooleanValueWithFalseMixedCaseShouldReturnFalse() {
    // When
    final boolean result = Configs.asBoolean("fALsE");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void asBooleanValueWithFalseShouldReturnFalse() {
    // When
    final boolean result = Configs.asBoolean("false");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void getStringValueTest() {
    String test = RandomStringUtils.randomAlphabetic(10);
    assertEquals(test, Configs.asString(test));
  }

  @Test
  public void getFromSystemPropertyWithPropertiesAsFallbackHasKeyInSystemShouldReturnSystemValue() {
    // Given
    new SystemMock().put("key", "systemValue");
    final Properties fallback = new Properties();
    fallback.put("key", "fallbackValue");
    // When
    final String result = Configs.getFromSystemPropertyWithPropertiesAsFallback(fallback, "key");
    // Then
    assertThat(result).isEqualTo("systemValue");
  }

  @Test
  public void getFromSystemPropertyWithPropertiesAsFallbackHasNotKeyInSystemShouldReturnSystemValue() {
    // Given
    new SystemMock().put("not-the-key", "systemValue");
    final Properties fallback = new Properties();
    fallback.put("key", "fallbackValue");
    // When
    final String result = Configs.getFromSystemPropertyWithPropertiesAsFallback(fallback, "key");
    // Then
    assertThat(result).isEqualTo("fallbackValue");
  }

  @Test
  public void getFromSystemPropertyWithPropertiesAsFallbackHasNotKeyShouldReturnNull() {
    // Given
    new SystemMock().put("not-the-key", "systemValue");
    final Properties fallback = new Properties();
    fallback.put("not-the-key-either", "fallbackValue");
    // When
    final String result = Configs.getFromSystemPropertyWithPropertiesAsFallback(fallback, "key");
    // Then
    assertThat(result).isNull();
  }

}
