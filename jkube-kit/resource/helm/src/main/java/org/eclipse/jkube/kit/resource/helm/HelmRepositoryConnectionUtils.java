/*
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
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

public class HelmRepositoryConnectionUtils {

  private HelmRepositoryConnectionUtils() {}

  protected static HttpURLConnection getConnectionForUploadToChartMuseum(
      File file, HelmRepository repository) throws IOException {
    return createConnection(repository, repository.getUrl());
  }

  protected static HttpURLConnection getConnectionForUploadToArtifactory(
      File file,
      HelmRepository repository) throws IOException {
    final HttpURLConnection connection = createConnection(repository, formatRepositoryURL(file, repository));
    connection.setRequestMethod("PUT");
    return connection;
  }

  protected static HttpURLConnection getConnectionForUploadToNexus(File file, HelmRepository repository) throws IOException {
    String url = formatRepositoryURL(file, repository);
    if (url.endsWith(".tar.gz")) {
      url = url.replaceAll("tar.gz$", "tgz");
    }
    final HttpURLConnection connection = createConnection(repository, url);
    connection.setRequestMethod("PUT");
    return connection;
  }

  private static HttpURLConnection createConnection(HelmRepository repository, String url) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/gzip");
    verifyAndSetAuthentication(repository);
    return connection;
  }

  private static String formatRepositoryURL(File file, HelmRepository repository) {
    return String.format("%s%s", StringUtils.appendIfMissing(repository.getUrl(), "/"), file.getName());
  }

  private static void verifyAndSetAuthentication(HelmRepository helmRepository) {
    if (StringUtils.isNotBlank(helmRepository.getUsername()) && StringUtils.isNotBlank(helmRepository.getPassword())) {
      PasswordAuthentication authentication = new PasswordAuthentication(helmRepository.getUsername(),
          helmRepository.getPassword().toCharArray());

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return authentication;
        }
      });
    }
  }
}
