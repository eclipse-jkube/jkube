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

import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelmRepositoryTest {

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
        "\"name\":\"repo-name\"," +
        "\"url\":\"https://example.com/url\"," +
        "\"username\":\"user\"," +
        "\"password\":\"pass\"," +
        "\"type\":\"ARTIFACTORY\"}";
    // When
    final HelmRepository result = objectMapper.readValue(serializedChart, HelmRepository.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "repo-name")
        .hasFieldOrPropertyWithValue("url", "https://example.com/url")
        .hasFieldOrPropertyWithValue("username", "user")
        .hasFieldOrPropertyWithValue("password", "pass")
        .hasFieldOrPropertyWithValue("type", HelmRepoType.ARTIFACTORY)
        .hasToString("[repo-name / https://example.com/url]");
  }

  @Test
  void builder() {
    // Given
    final HelmRepository.HelmRepositoryBuilder builder = HelmRepository.builder()
        .name("repo-name")
        .url("https://example.com/url")
        .username("user")
        .password("pass")
        .type(HelmRepoType.ARTIFACTORY);
    // When
    final HelmRepository result = builder.build();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "repo-name")
        .hasFieldOrPropertyWithValue("url", "https://example.com/url")
        .hasFieldOrPropertyWithValue("username", "user")
        .hasFieldOrPropertyWithValue("password", "pass")
        .hasFieldOrPropertyWithValue("type", HelmRepoType.ARTIFACTORY);
  }

  @Test
  void toString_returnsStringRepresentationWithNameAndUrl() {
    // Given
    final HelmRepository repository = HelmRepository.builder()
      .name("repo-name")
      .url("https://example.com/url")
      .username("user")
      .password("pass")
      .type(HelmRepoType.ARTIFACTORY)
      .build();
    // When
    final String result = repository.toString();
    // Then
    assertThat(result).isEqualTo("[repo-name / https://example.com/url]");

  }
}
