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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaintainerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  public void tearDown() {
    objectMapper = null;
  }

  @Test
  void deserialize() throws Exception {
    // Given
    final String serializedMaintainer = "{\"name\":\"John\",\"email\":\"john@example.com\",\"ignored\":\"don't fail\"}";
    // When
    final Maintainer result = objectMapper.readValue(serializedMaintainer, Maintainer.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "John")
        .hasFieldOrPropertyWithValue("email", "john@example.com")
        .isEqualTo(new Maintainer("John", "john@example.com"));
  }
}
