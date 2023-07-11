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
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

import org.eclipse.jkube.kit.common.KitLogger;

import org.apache.commons.io.IOUtils;

public class HelmUploader {

  private final KitLogger logger;

  public HelmUploader(KitLogger logger) {
    this.logger = logger;
  }

  void uploadSingle(File file, HelmRepository repository)
      throws IOException, BadUploadException {
    HttpURLConnection connection;

    if (repository.getType() == null) {
      throw new IllegalArgumentException(
          "Repository type missing. Check your plugin configuration.");
    }

    connection = repository.getType().createConnection(file, repository);

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

  private void writeFileOnConnection(File file, HttpURLConnection connection) throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      IOUtils.copy(fileInputStream, connection.getOutputStream());
    }
  }

}
