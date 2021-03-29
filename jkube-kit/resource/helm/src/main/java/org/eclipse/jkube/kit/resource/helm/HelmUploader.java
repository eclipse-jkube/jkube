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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

import org.eclipse.jkube.kit.common.KitLogger;

import org.apache.commons.io.IOUtils;

public class HelmUploader {

  private KitLogger logger;

  public HelmUploader(KitLogger logger) {
    this.logger = logger;
  }

  protected void uploadSingle(File file, HelmRepository repository)
      throws IOException, BadUploadException {
    HttpURLConnection connection;

    if (repository.getType() == null) {
      throw new IllegalArgumentException(
          "Repository type missing. Check your plugin configuration.");
    }

    switch (repository.getType()) {
      case ARTIFACTORY:
        connection = getConnectionForUploadToArtifactory(file, repository);
        break;
      case CHARTMUSEUM:
        connection = getConnectionForUploadToChartmuseum(repository);
        break;
      case NEXUS:
        connection = getConnectionForUploadToNexus(file, repository);
        break;
      default:
        throw new IllegalArgumentException("Unsupported repository type for upload.");
    }

    writeFileOnConnection(file, connection);

    if (connection.getResponseCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
      String response;
      if (connection.getErrorStream() != null) {
        response = IOUtils.toString(connection.getErrorStream(), Charset.defaultCharset());
      } else if (connection.getInputStream() != null) {
        response = IOUtils.toString(connection.getInputStream(), Charset.defaultCharset());
      } else {
        response = "No details provided";
      }
      throw new BadUploadException(response);
    } else {
      String message = Integer.toString(connection.getResponseCode());
      if (connection.getInputStream() != null) {
        message += " - " + IOUtils.toString(connection.getInputStream(), Charset.defaultCharset());
      }
      logger.info(message);
    }
    connection.disconnect();
  }

  protected void writeFileOnConnection(File file, HttpURLConnection connection) throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      IOUtils.copy(fileInputStream, connection.getOutputStream());
    }
  }

  protected HttpURLConnection getConnectionForUploadToChartmuseum(HelmRepository repository) throws IOException {
    final HttpURLConnection connection = createConnection(repository.getUrl());
    configureConnection(connection);
    connection.setRequestProperty("Content-Type", "application/gzip");

    verifyAndSetAuthentication(repository);

    return connection;
  }

  protected HttpURLConnection getConnectionForUploadToArtifactory(
      File file,
      HelmRepository repository
  ) throws IOException {
    String uploadUrl = formatRepositoryURL(file, repository);

    final HttpURLConnection connection = createConnection(uploadUrl);
    configureConnection(connection);
    connection.setRequestProperty("Content-Type", "application/gzip");

    verifyAndSetAuthentication(repository);

    return connection;
  }

  protected HttpURLConnection getConnectionForUploadToNexus(File file, HelmRepository repository) throws IOException {
    String uploadUrl = formatRepositoryURL(file, repository);

    final HttpURLConnection connection = createConnection(uploadUrl);
    configureConnection(connection);

    verifyAndSetAuthentication(repository);

    return connection;
  }

  private void verifyAndSetAuthentication(HelmRepository helmRepository) {

    PasswordAuthentication authentication =
        new PasswordAuthentication(helmRepository.getUsername(),
            helmRepository.getPassword().toCharArray());

    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return authentication;
      }
    });
  }

  protected HttpURLConnection createConnection(String uploadUrl) throws IOException {
    return (HttpURLConnection) new URL(uploadUrl).openConnection();
  }

  private HttpURLConnection configureConnection(HttpURLConnection connection)
      throws ProtocolException {
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");

    return connection;
  }

  private String formatRepositoryURL(File file, HelmRepository repository) {
    String uploadUrl = repository.getUrl();
    // Append slash if not already in place
    if (!uploadUrl.endsWith("/")) {
      uploadUrl += "/";
    }
    uploadUrl = uploadUrl + file.getName();
    return uploadUrl;
  }

}
