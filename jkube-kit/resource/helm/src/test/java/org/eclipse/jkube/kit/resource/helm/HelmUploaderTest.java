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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.resource.helm.HelmRepository.HelmRepoType;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HelmUploaderTest {

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

  @Test(expected = IllegalArgumentException.class)
  public void uploadThrowsException(
      @Mocked HelmRepository helmRepository) throws IOException, BadUploadException {
    // Given
    File file = new File("test");
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = null;
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    fail();
  }

  @Test(expected = BadUploadException.class)
  public void uploadResponseCodeLessHigherThan300InputStreamNullThrowsException(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = File.createTempFile("test",".tmp");
    file.deleteOnExit();
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = HelmRepoType.ARTIFACTORY;
      helmRepository.getUrl(); result = "http://some-url";
      helmRepository.getUsername(); result = "username";
      helmRepository.getPassword(); result = "password";
      helmUploader.createConnection(anyString); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 500;
      httpURLConnection.getErrorStream(); result = new ByteArrayInputStream("error".getBytes());
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    fail();
  }

  @Test(expected = BadUploadException.class)
  public void uploadResponseCodeLessHigherThan300ErrorStreamNullThrowsException(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = File.createTempFile("test",".tmp");
    file.deleteOnExit();
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = HelmRepoType.ARTIFACTORY;
      helmRepository.getUrl(); result = "http://some-url";
      helmRepository.getUsername(); result = "username";
      helmRepository.getPassword(); result = "password";
      helmUploader.createConnection(anyString); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 500;
      httpURLConnection.getInputStream(); result = new ByteArrayInputStream("error".getBytes());
      httpURLConnection.getErrorStream(); result = null;
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    fail();
  }

  @Test(expected = BadUploadException.class)
  public void uploadResponseCodeLessHigherThan300ThrowsException(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = File.createTempFile("test",".tmp");
    file.deleteOnExit();
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = HelmRepoType.ARTIFACTORY;
      helmRepository.getUrl(); result = "http://some-url";
      helmRepository.getUsername(); result = "username";
      helmRepository.getPassword(); result = "password";
      helmUploader.createConnection(anyString); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 500;
      httpURLConnection.getErrorStream(); result = null;
      httpURLConnection.getInputStream(); result = null;
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    fail();
  }

  @Test
  public void uploadOnArtifactory(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = File.createTempFile("test",".tmp");
    file.deleteOnExit();
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = HelmRepoType.ARTIFACTORY;
      helmRepository.getUrl(); result = "http://some-url";
      helmRepository.getUsername(); result = "username";
      helmRepository.getPassword(); result = "password";
      helmUploader.createConnection(anyString); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 201;
      httpURLConnection.getInputStream(); result = null;
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    assertThat(httpURLConnection.getResponseCode()).isEqualTo(201);
  }

  @Test
  public void uploadOnNexus(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = File.createTempFile("test",".tmp");
    file.deleteOnExit();
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = HelmRepoType.NEXUS;
      helmRepository.getUrl(); result = "http://some-url";
      helmRepository.getUsername(); result = "username";
      helmRepository.getPassword(); result = "password";
      helmUploader.createConnection(anyString); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 201;
      httpURLConnection.getInputStream(); result = null;
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    assertThat(httpURLConnection.getResponseCode()).isEqualTo(201);
  }

  @Test
  public void uploadOnChartMuseum(
      @Mocked HelmRepository helmRepository,
      @Mocked HttpURLConnection httpURLConnection) throws IOException, BadUploadException {
    // Given
    File file = File.createTempFile("test",".tmp");
    file.deleteOnExit();
    new Expectations(helmUploader) {{
      helmRepository.getType(); result = HelmRepoType.CHARTMUSEUM;
      helmRepository.getUrl(); result = "http://some-url";
      helmRepository.getUsername(); result = "username";
      helmRepository.getPassword(); result = "password";
      helmUploader.createConnection(anyString); result = httpURLConnection;
      httpURLConnection.getResponseCode(); result = 201;
      httpURLConnection.getInputStream(); result = null;
    }};
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    assertThat(httpURLConnection.getResponseCode()).isEqualTo(201);
  }
}
