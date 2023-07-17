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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MappingConfigTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final MappingConfig result = mapper.readValue(
        getClass().getResourceAsStream("/mapping-config.json"),
        MappingConfig.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("kind", "CronTab")
        .hasFieldOrPropertyWithValue("apiVersion", "custom-cron-tab.example.com/v1")
        .hasFieldOrPropertyWithValue("filenameTypes", "crontab,cr");
  }

  @Test
  void getFilenamesAsArray_whenNoTypesPresent_thenReturnsEmptyArray() {
    // Given
    final MappingConfig config = MappingConfig.builder().kind("Foo").build();
    // When + Then
    assertThat(config.getFilenamesAsArray())
        .isEmpty();
  }

  @Test
  void getFilenamesAsArray_whenTypesPresent_thenReturnsArray() {
    // Given
    final MappingConfig config = MappingConfig.builder().kind("Foo").filenameTypes("foo,foos").build();
    // When + Then
    assertThat(config.getFilenamesAsArray())
        .contains("foo", "foos");
  }

  @Test
  void isValid_withMissingKind_shouldReturnFalse() {
    // Given
    MappingConfig config = MappingConfig.builder()
      .apiVersion("custom-cron-tab.example.com/v1")
      .filenameTypes("crontab,cr")
      .build();
    // When
    boolean result = config.isValid();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isValid_withMissingFileNameTypes_shouldReturnFalse() {
    // Given
    MappingConfig config = MappingConfig.builder()
      .apiVersion("custom-cron-tab.example.com/v1")
      .kind("Foo")
      .build();
    // When
    boolean result = config.isValid();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isValid_withKindAndFileName_shouldReturnTrue() {
    // Given
    MappingConfig config = MappingConfig.builder()
        .kind("Foo")
        .filenameTypes("foos")
        .build();
    // When
    boolean result = config.isValid();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isValid_withKindAndApiVersionAndFileName_shouldReturnTrue() {
    // Given
    MappingConfig config = MappingConfig.builder()
        .kind("Foo")
        .apiVersion("custom-cron-tab.example.com/v1")
        .filenameTypes("foos")
        .build();
    // When
    boolean result = config.isValid();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    MappingConfig  mc1 = MappingConfig.builder().kind("Foo").filenameTypes("foos").build();
    MappingConfig mc2 = MappingConfig.builder().kind("Foo").filenameTypes("foos").build();
    // When + Then
    assertThat(mc1)
        .isEqualTo(mc2)
        .hasSameHashCodeAs(mc2);
  }
}
