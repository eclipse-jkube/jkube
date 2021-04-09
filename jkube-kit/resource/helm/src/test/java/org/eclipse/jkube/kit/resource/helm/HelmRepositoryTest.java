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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HelmRepositoryTest {

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
    final String serializedChart = "{" +
        "\"name\":\"repo-name\"," +
        "\"url\":\"http://example.com/url\"," +
        "\"username\":\"user\"," +
        "\"password\":\"pass\"," +
        "\"type\":\"ARTIFACTORY\"}";
    // When
    final HelmRepository result = objectMapper.readValue(serializedChart, HelmRepository.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "repo-name")
        .hasFieldOrPropertyWithValue("url", "http://example.com/url")
        .hasFieldOrPropertyWithValue("username", "user")
        .hasFieldOrPropertyWithValue("password", "pass")
        .hasFieldOrPropertyWithValue("type", HelmRepoType.ARTIFACTORY)
        .hasToString("[repo-name / http://example.com/url]");
  }

  @Test
  public void builder() {
    // Given
    final HelmRepository.HelmRepositoryBuilder builder = HelmRepository.builder()
        .name("repo-name")
        .url("http://example.com/url")
        .username("user")
        .password("pass")
        .type(HelmRepoType.ARTIFACTORY);
    // When
    final HelmRepository result = builder.build();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "repo-name")
        .hasFieldOrPropertyWithValue("url", "http://example.com/url")
        .hasFieldOrPropertyWithValue("username", "user")
        .hasFieldOrPropertyWithValue("password", "pass")
        .hasFieldOrPropertyWithValue("type", HelmRepoType.ARTIFACTORY)
        .hasToString("[repo-name / http://example.com/url]");
  }
}
