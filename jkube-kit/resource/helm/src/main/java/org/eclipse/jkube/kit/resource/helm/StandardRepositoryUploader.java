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

import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.Base64Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import static org.eclipse.jkube.kit.common.util.AsyncUtil.get;

public abstract class StandardRepositoryUploader implements HelmUploader {
  private final String method;
  private final HelmRepository.HelmRepoType type;
  private static final long HELM_UPLOAD_TIMEOUT_MINUTES = 30;

  protected StandardRepositoryUploader(String method, HelmRepository.HelmRepoType type) {
    this.method = method;
    this.type = type;
  }

  public abstract String url(File helmChart, HelmRepository repository);

  @Override
  public HelmRepository.HelmRepoType getType() {
    return type;
  }

  @Override
  public void uploadSingle(File file, HelmRepository repository) throws IOException, BadUploadException {
    String uploadUrl = url(file, repository);

    try (HttpClient httpClient = HttpClientUtils.getHttpClientFactory().newBuilder().tag(new RequestConfigBuilder().withRequestRetryBackoffLimit(0).build()).build()) {
      HttpRequest httpRequest = httpClient.newHttpRequestBuilder()
          .method(method, "application/gzip", Files.newInputStream(file.toPath()), file.length())
          // At this point username and password are always populated since this is requirement in HelmService
          .header("Authorization", String.format("Basic %s", Base64Util.encodeToString(repository.getUsername() + ":" + repository.getPassword())))
          .uri(uploadUrl)
          .build();
      HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(HELM_UPLOAD_TIMEOUT_MINUTES));
      handleHttpResponse(response);
    }
  }

  private void handleHttpResponse(HttpResponse<byte[]> response) throws BadUploadException {
    if (!response.isSuccessful()) {
      String responseStr;
      if (response.body() != null) {
        responseStr = new String(response.body());
      } else if (StringUtils.isNotBlank(response.message())) {
        responseStr = response.message();
      } else {
        responseStr = "No details provided";
      }
      throw new BadUploadException(responseStr);
    }
  }

  protected String formatRepositoryURL(File file, HelmRepository repository) {
    return String.format("%s%s", StringUtils.appendIfMissing(repository.getUrl(), "/"), file.getName());
  }
}
