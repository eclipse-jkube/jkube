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
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.eclipse.jkube.kit.common.KitLogger;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HelmUploaderTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  KitLogger kitLogger;

  HelmUploader helmUploader;

  @Before
  public void setUp() throws Exception {
    helmUploader = new HelmUploader(kitLogger);
  }

  @After
  public void tearDown() {
    kitLogger = null;
    helmUploader = null;
  }

  @Test
  public void uploadSingle_withMissingType_shouldThrowException(@Mocked HelmRepository helmRepository) {
    // Given
    File file = new File("test");
    // @formatter:off
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = null;
    }};
    // @formatter:on
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Repository type missing. Check your plugin configuration.");
  }

  @Test
  public void uploadSingle_withServerErrorAndErrorStream_shouldThrowException(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException {
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    // @formatter:off
    new Expectations(helmUploader) {{
      helmRepository.getType().createConnection((File) any, helmRepository); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 500;
      httpURLConnection.getErrorStream(); result = new ByteArrayInputStream("Server error in ES".getBytes());
      httpURLConnection.getInputStream(); result = new ByteArrayInputStream("Server error in IS".getBytes()); minTimes = 0;
    }};
    // @formatter:on
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Server error in ES");
  }

  @Test
  public void uploadSingle_withServerErrorAndInputStream_shouldThrowException(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException {
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    // @formatter:off
    new Expectations(helmUploader) {{
      helmRepository.getType().createConnection((File) any, helmRepository); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 500;
      httpURLConnection.getErrorStream(); result = null;
      httpURLConnection.getInputStream(); result = new ByteArrayInputStream("Server error in IS".getBytes());
    }};
    // @formatter:on
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Server error in IS");
  }

  @Test
  public void uploadSingle_withServerError_shouldThrowException(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException {
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    // @formatter:off
    new Expectations(helmUploader) {{
      helmRepository.getType().createConnection((File) any, helmRepository); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 500;
      httpURLConnection.getErrorStream(); result = null;
      httpURLConnection.getInputStream(); result = null;
    }};
    // @formatter:on
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("No details provided");
  }

  @Test
  public void uploadSingle_withCreatedStatus_shouldDisconnect(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    // @formatter:off
    new Expectations(helmUploader) {{
      helmRepository.getType().createConnection((File)any, helmRepository); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 201;
      httpURLConnection.getInputStream(); result = null;
    }};
    // @formatter:on
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    // @formatter:off
    new Verifications() {{
      httpURLConnection.disconnect(); times = 1;
    }};
    // @formatter:on
  }
}
