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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ChartTest {

  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
  }

  @After
  public void tearDown() {
    objectMapper = null;
  }

  @Test
  public void deserialize() throws Exception {
    // Given
    final String serializedMaintainer = "{" +
        "\"name\":\"chart\"," +
        "\"home\":\"e.t.\"," +
        "\"version\":\"1337\"," +
        "\"sources\":[\"source\"]," +
        "\"ignored\":\"don't fail\"}";
    // When
    final Chart result = objectMapper.readValue(serializedMaintainer, Chart.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "chart")
        .hasFieldOrPropertyWithValue("home", "e.t.")
        .hasFieldOrPropertyWithValue("version", "1337")
        .extracting("sources").asList().containsExactly("source");
  }
}
