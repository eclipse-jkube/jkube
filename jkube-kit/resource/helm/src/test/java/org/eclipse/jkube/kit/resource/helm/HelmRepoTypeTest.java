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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class HelmRepoTypeTest {

  @TempDir
  File temporaryFolder;

  private HelmRepository.HelmRepositoryBuilder helmRepositoryBuilder;

  @BeforeEach
  void setUp() {
    helmRepositoryBuilder = HelmRepository.builder()
      .url("https://example.com/base/");
  }

  @Test
  void createConnection_withChartMuseumAndNoAuth_shouldReturnConnection() throws IOException {
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.CHARTMUSEUM
        .createConnection(File.createTempFile("junit", "ext", temporaryFolder), helmRepositoryBuilder.build());
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
  void createConnection_withChartMuseumAndAuth_shouldReturnConnection() throws IOException {
    helmRepositoryBuilder.username("user").password("s3cret");
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.CHARTMUSEUM
        .createConnection(File.createTempFile("junit", "ext", temporaryFolder), helmRepositoryBuilder.build());
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
  void createConnection_withArtifactoryAndNoAuth_shouldReturnConnection() throws IOException {
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.ARTIFACTORY
        .createConnection(Files.createFile(temporaryFolder.toPath().resolve("chart.tar")).toFile(),
                helmRepositoryBuilder.build());
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
  void createConnection_withNexusAndNoAuthAndTarGzExtension_shouldReturnConnectionToTgzUrl() throws IOException {
    // Given
    helmRepositoryBuilder.url("https://example.com");
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.NEXUS
        .createConnection(Files.createFile(temporaryFolder.toPath().resolve("chart.tar.gz")).toFile(),
                helmRepositoryBuilder.build());
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
  void createConnection_withNexusAndNoAuthAndTgzExtension_shouldReturnConnection() throws IOException {
    // When
    final HttpURLConnection result = HelmRepository.HelmRepoType.NEXUS
        .createConnection(Files.createFile(temporaryFolder.toPath().resolve("chart.tgz")).toFile(),
                helmRepositoryBuilder.build());
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
