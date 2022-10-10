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
package org.eclipse.jkube.kit.resource.helm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ChartTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    objectMapper = null;
  }

  @Test
  void deserialize() throws Exception {
    // Given
    final String serializedChart = "{" +
        "\"name\":\"chart\"," +
        "\"home\":\"e.t.\"," +
        "\"version\":\"1337\"," +
        "\"sources\":[\"source\"]," +
        "\"ignored\":\"don't fail\"}";
    // When
    final Chart result = objectMapper.readValue(serializedChart, Chart.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "chart")
        .hasFieldOrPropertyWithValue("home", "e.t.")
        .hasFieldOrPropertyWithValue("version", "1337")
        .hasToString("Chart{name='chart', home='e.t.', version='1337'}")
        .extracting("sources").asList().containsExactly("source");
  }

  @Test
  void builder() {
    // Given
    final Chart.ChartBuilder builder = Chart.builder()
        .name("chart")
        .home("e.t.")
        .version("1337")
        .sources(Arrays.asList("source-1", "source-2"));
    // When
    final Chart result = builder.build();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "chart")
        .hasFieldOrPropertyWithValue("home", "e.t.")
        .hasFieldOrPropertyWithValue("version", "1337")
        .hasToString("Chart{name='chart', home='e.t.', version='1337'}")
        .extracting("sources").asList().containsExactly("source-1", "source-2");
  }

  @Test
  void equals() {
    // Given
    final Chart chart1 = Chart.builder()
        .name("chart")
        .home("e.t.")
        .version("1337")
        .sources(Arrays.asList("source-1", "source-2"))
        .build();
    // When
    final Chart result = Chart.builder()
        .name("chart")
        .home("e.t.")
        .version("1337")
        .sources(Arrays.asList("source-1", "source-2"))
        .build();
    // Then
    assertThat(result)
        .isNotSameAs(chart1)
        .isEqualTo(chart1);
  }
}
