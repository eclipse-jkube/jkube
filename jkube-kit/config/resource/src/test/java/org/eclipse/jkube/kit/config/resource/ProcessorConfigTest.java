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
package org.eclipse.jkube.kit.config.resource;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.Configs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Test;

@SuppressWarnings("unused")
public class ProcessorConfigTest {

  @AllArgsConstructor
  public enum TestConfig implements Configs.Config {
    ONE("one", "configDefaultForOne"),
    TWO("two", "configDefaultForTwo"),
    NO_DEFAULT("three", null);

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void setProcessorConfigShouldReplaceExistingProcessorConfig() {
    // Given
    final Map<String, TreeMap> newConfig = Collections.singletonMap("configRef", new TreeMap<>(Collections.singletonMap("key", "value")));
    final ProcessorConfig processorConfig = new ProcessorConfig();
    processorConfig.getConfig().put("configRef", Collections.singletonMap("existingKey", "existingRef"));
    // When
    processorConfig.setConfig(newConfig);
    // Then
    assertThat(processorConfig.getConfig()).containsOnlyKeys("configRef");
    assertThat(processorConfig.getConfig().get("configRef")).containsEntry("key", "value");
    assertThat(processorConfig.getConfig().get("configRef")).doesNotContainEntry("existingKey", "existingRef");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void cloneProcessorConfig() {
    // Given
    final ProcessorConfig initial = new ProcessorConfig();
    initial.getIncludes().add("one-include");
    initial.getExcludes().add("one-exclude");
    initial.getConfig().put("config-one", mapOf(entry("field-one", "one")));
    initial.getConfig().put("config-two", mapOf(entry("field-one", "value-one"), entry("field-two", "value-two")));
    // When
    final ProcessorConfig result = ProcessorConfig.cloneProcessorConfig(initial);
    // Then
    assertThat(result).isNotSameAs(initial).isEqualTo(initial).isEqualToComparingFieldByField(initial);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void cloneProcessorConfigWithNestedMaps() {
    // Given
    final ProcessorConfig initial = new ProcessorConfig();
    initial.getIncludes().add("one-include");
    initial.getExcludes().add("one-exclude");
    initial.getConfig().put("config-one", mapOf(entry("field-one", "one")));
    initial.getConfig().put("config-two", mapOf(
        entry("field-one", "value-one"), entry("field-two", "value-two"),
        entry("field-with-nested", mapOf( entry("one", "one")))
    ));
    // When
    final ProcessorConfig result = ProcessorConfig.cloneProcessorConfig(initial);
    // Then
    assertThat(result).isNotSameAs(initial).isEqualTo(initial).isEqualToComparingFieldByField(initial);
  }

  @Test
  public void getValueWithValueInAllShouldReturnValueInProcessorConfig() {
    // Given
    final ProcessorConfig config = new ProcessorConfig();
    config.getConfig().put("name", Collections.singletonMap("one", "oneInConfig"));
    final Properties properties = new Properties();
    properties.put("prefix.name.one", "oneInProp");
    properties.put("other-prefix.name.one", "otherOneInProp");
    // When
    final String result = ProcessorConfig.getConfigValue(
        config, "name", "prefix", properties, TestConfig.ONE, "defaultValueOne");
    // Then
    assertThat(result).isEqualTo("oneInConfig");
  }

  @Test
  public void getValueWithValueInPropertiesAndDefaultValAndDefaultConfigShouldReturnValueInProperties() {
    // Given
    final ProcessorConfig config = new ProcessorConfig();
    config.getConfig().put("name", Collections.singletonMap("two", "twoInConfig"));
    final Properties properties = new Properties();
    properties.put("prefix.name.one", "oneInProp");
    // When
    final String result = ProcessorConfig.getConfigValue(
        config, "name", "prefix", properties, TestConfig.ONE, "defaultValueOne");
    // Then
    assertThat(result).isEqualTo("oneInProp");
  }

  @Test
  public void getValueWithValueInDefaultValAndDefaultConfigShouldReturnValueInDefaultVal() {
    // Given
    final ProcessorConfig config = new ProcessorConfig();
    config.getConfig().put("name", Collections.singletonMap("two", "twoInConfig"));
    final Properties properties = new Properties();
    properties.put("prefix.other-name.one", "oneInProp");
    // When
    final String result = ProcessorConfig.getConfigValue(
        config, "name", "prefix", properties, TestConfig.ONE, "defaultValueOne");
    // Then
    assertThat(result).isEqualTo("defaultValueOne");
  }

  @Test
  public void getValueWithNoValueAndConfigDefaultShouldReturnValueInConfigDefault() {
    // Given
    final ProcessorConfig config = new ProcessorConfig();
    config.getConfig().put("name", Collections.singletonMap("two", "twoInConfig"));
    final Properties properties = new Properties();
    properties.put("prefix.other-name.one", "oneInProp");
    // When
    final String result = ProcessorConfig.getConfigValue(
        config, "name", "prefix", properties, TestConfig.ONE, null);
    // Then
    assertThat(result).isEqualTo("configDefaultForOne");
  }

  @Test
  public void getValueWithNoValueShouldReturnNull() {
    // Given
    final ProcessorConfig config = new ProcessorConfig();
    config.getConfig().put("name", Collections.singletonMap("two", "twoInConfig"));
    final Properties properties = new Properties();
    properties.put("prefix.other-name.one", "oneInProp");
    // When
    final String result = ProcessorConfig.getConfigValue(
        config, "name", "prefix", properties, TestConfig.NO_DEFAULT, null);
    // Then
    assertThat(result).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void mergeProcessorConfigsShouldMergeAccordingToPrioritiesDescribedInJavaDoc() {
    // Given
    // n.b. Include order must be preserved
    final ProcessorConfig highPriority = new ProcessorConfig(
        Arrays.asList("module9", "module2", "module3", "module4"),
        new HashSet<>(Arrays.asList("module10", "module11", "module12", "module13")),
        new HashMap<>(mapOf(
            entry("module1", mapOf(
                entry("config1", "config1ValueToPrevail")
            )),
            entry("module2", mapOf(
                entry("config1", "mod2Value")
            ))
        ))
    );
    highPriority.getConfig().put(null, null);
    highPriority.getConfig().get("module1").put(null, "corner-case");
    final ProcessorConfig mediumPriority = new ProcessorConfig(
        Arrays.asList("module5", "module2", "module3", "module4"),
        new HashSet<>(Arrays.asList("module10", "module11", "module12", "module15")),
        new HashMap<>(mapOf(
            entry("module1", mapOf(
                entry("config1", "config1ValueToBeOverridden"),
                entry("config2", "config2ValueToPrevail")
            )),
            entry("module3", mapOf(
                entry("config1", "mod3Value")
            ))
        ))
    );
    final ProcessorConfig lowPriority = new ProcessorConfig(
        Arrays.asList("module6", "module1", "module5"),
        new HashSet<>(Arrays.asList("module10", "module11", "module12", "module15", "module16")),
        new HashMap<>(mapOf(
            entry("module1", mapOf(
                entry("config1", "config1ValueToBeOverridden"),
                entry("config2", "config2ValueToBeOverridden")
            )),
            entry("module3", mapOf(
                entry("config2", "mod32Value")
            )),
            entry("module4", mapOf(
                entry("config1", "mod4Value")
            ))
        ))
    );
    // When
    final ProcessorConfig result = ProcessorConfig.mergeProcessorConfigs(highPriority, mediumPriority, lowPriority);
    // Then
    assertThat(result.getIncludes()).containsExactly("module9", "module2", "module3", "module4", "module5", "module6", "module1");
    assertThat(result.getExcludes()).containsExactlyInAnyOrder("module10", "module11", "module12", "module13", "module15", "module16");
    assertThat(result.getConfig()).hasSize(4);
    assertThat(result.getConfig().get("module1")).containsOnly(
        entry("config1", "config1ValueToPrevail"), entry("config2", "config2ValueToPrevail"));
    assertThat(result.getConfig().get("module2")).containsOnly(entry("config1", "mod2Value"));
    assertThat(result.getConfig().get("module3")).containsOnly(
        entry("config1", "mod3Value"), entry("config2", "mod32Value"));
    assertThat(result.getConfig().get("module4")).containsOnly(entry("config1", "mod4Value"));
  }

  private static <K, V> Map<K, V> mapOf(AbstractMap.Entry<K, V>... entries) {
    return Stream.of(entries).collect(Collectors.toMap(AbstractMap.Entry::getKey, AbstractMap.Entry::getValue));
  }

  private static <K, V> AbstractMap.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry<>(key, value);
  }
}