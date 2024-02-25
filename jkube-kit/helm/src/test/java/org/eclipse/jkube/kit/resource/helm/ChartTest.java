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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.Maintainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
      .apiVersion("v2")
      .name("chart")
      .version("1337")
      .kubeVersion(">= 1.13.0 < 1.15.0")
      .description("chart-description")
      .type("application")
      .keywords(Arrays.asList("keyword-1", "keyword-2"))
      .home("e.t.")
      .sources(Arrays.asList("source-1", "source-2"))
      .dependencies(Collections.singletonList(HelmDependency.builder().name("dep-1").build()))
      .maintainers(Collections.singletonList(Maintainer.builder().name("maintainer-1").build()))
      .icon("https://example.com/icon.png")
      .appVersion("1.33.7")
      .deprecated(false)
      .annotations(Collections.singletonMap("com.example/annotation", "value-1"));
    // When
    final Chart result = builder.build();
    // Then
    assertThat(result)
      .hasFieldOrPropertyWithValue("apiVersion", "v2")
      .hasFieldOrPropertyWithValue("name", "chart")
      .hasFieldOrPropertyWithValue("version", "1337")
      .hasFieldOrPropertyWithValue("kubeVersion", ">= 1.13.0 < 1.15.0")
      .hasFieldOrPropertyWithValue("description", "chart-description")
      .hasFieldOrPropertyWithValue("type", "application")
      .hasFieldOrPropertyWithValue("keywords", Arrays.asList("keyword-1", "keyword-2"))
      .hasFieldOrPropertyWithValue("home", "e.t.")
      .hasFieldOrPropertyWithValue("sources", Arrays.asList("source-1", "source-2"))
      .hasFieldOrPropertyWithValue("dependencies", Collections.singletonList(HelmDependency.builder().name("dep-1").build()))
      .hasFieldOrPropertyWithValue("maintainers", Collections.singletonList(Maintainer.builder().name("maintainer-1").build()))
      .hasFieldOrPropertyWithValue("icon", "https://example.com/icon.png")
      .hasFieldOrPropertyWithValue("appVersion", "1.33.7")
      .hasFieldOrPropertyWithValue("deprecated", false)
      .hasToString("Chart{name='chart', home='e.t.', version='1337'}")
      .isEqualTo(result.toBuilder().build())
      .extracting("annotations").asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .containsOnly(entry("com.example/annotation", "value-1"));
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
