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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HelmRepoTypeTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private HelmRepository.HelmRepositoryBuilder helmRepositoryBuilder;

  @Before
  public void setUp() throws Exception {
    helmRepositoryBuilder = HelmRepository.builder()
      .url("https://example.com/base/");
  }

  @Test
  public void createConnection_withChartMuseumAndNoAuth_shouldReturnConnection() throws IOException {
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.CHARTMUSEUM
        .createConnection(temporaryFolder.newFile(), helmRepositoryBuilder.build());
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("url", new URL("https://example.com/base/"))
        .hasFieldOrPropertyWithValue("requestMethod", "POST")
        .hasFieldOrPropertyWithValue("doOutput", true)
        .extracting(HttpURLConnection::getRequestProperties)
        .hasFieldOrPropertyWithValue("Content-Type", Collections.singletonList("application/gzip"));
    assertThat(Authenticator.requestPasswordAuthentication(InetAddress.getLocalHost(), 443, "https", "test", "basic"))
        .isNull();
  }

  @Test
  public void createConnection_withChartMuseumAndAuth_shouldReturnConnection() throws IOException {
    helmRepositoryBuilder.username("user").password("s3cret");
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.CHARTMUSEUM
        .createConnection(temporaryFolder.newFile(), helmRepositoryBuilder.build());
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("url", new URL("https://example.com/base/"))
        .hasFieldOrPropertyWithValue("requestMethod", "POST")
        .hasFieldOrPropertyWithValue("doOutput", true)
        .extracting(HttpURLConnection::getRequestProperties)
        .hasFieldOrPropertyWithValue("Content-Type", Collections.singletonList("application/gzip"));
    assertThat(Authenticator.requestPasswordAuthentication(InetAddress.getLocalHost(), 443, "https", "test", "basic"))
        .isNotNull()
        .hasFieldOrPropertyWithValue("userName", "user")
        .hasFieldOrPropertyWithValue("password", new char[]{'s', '3', 'c', 'r', 'e', 't'});
  }

  @Test
  public void createConnection_withArtifactoryAndNoAuth_shouldReturnConnection() throws IOException {
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.ARTIFACTORY
        .createConnection(temporaryFolder.newFile("chart.tar"), helmRepositoryBuilder.build());
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("url", new URL("https://example.com/base/chart.tar"))
        .hasFieldOrPropertyWithValue("requestMethod", "PUT")
        .hasFieldOrPropertyWithValue("doOutput", true)
        .extracting(HttpURLConnection::getRequestProperties)
        .hasFieldOrPropertyWithValue("Content-Type", Collections.singletonList("application/gzip"));
  }

  @Test
  public void createConnection_withNexusAndNoAuthAndTarGzExtension_shouldReturnConnectionToTgzUrl() throws IOException {
    // Given
    helmRepositoryBuilder.url("https://example.com");
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.NEXUS
        .createConnection(temporaryFolder.newFile("chart.tar.gz"), helmRepositoryBuilder.build());
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("url", new URL("https://example.com/chart.tgz"))
        .hasFieldOrPropertyWithValue("requestMethod", "PUT")
        .hasFieldOrPropertyWithValue("doOutput", true)
        .extracting(HttpURLConnection::getRequestProperties)
        .hasFieldOrPropertyWithValue("Content-Type", Collections.singletonList("application/gzip"));
  }

  @Test
  public void createConnection_withNexusAndNoAuthAndTgzExtension_shouldReturnConnection() throws IOException {
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.NEXUS
        .createConnection(temporaryFolder.newFile("chart.tgz"), helmRepositoryBuilder.build());
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("url", new URL("https://example.com/base/chart.tgz"))
        .hasFieldOrPropertyWithValue("requestMethod", "PUT")
        .hasFieldOrPropertyWithValue("doOutput", true)
        .extracting(HttpURLConnection::getRequestProperties)
        .hasFieldOrPropertyWithValue("Content-Type", Collections.singletonList("application/gzip"));
  }
}
