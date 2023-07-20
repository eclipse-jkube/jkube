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
package org.eclipse.jkube.kit.build.service.docker.access.hc;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.eclipse.jkube.kit.build.service.docker.access.chunked.EntityStreamReaderUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.stream.Stream;

class HcChunkedResponseHandlerWrapper implements ResponseHandler<Object> {

  private final EntityStreamReaderUtil.JsonEntityResponseHandler handler;

  HcChunkedResponseHandlerWrapper(EntityStreamReaderUtil.JsonEntityResponseHandler handler) {
    this.handler = handler;
  }

  @Override
  public Object handleResponse(HttpResponse response) throws IOException {
    if (!hasJsonContentType(response) && !hasTextPlainContentType(response) && !hasNoContentTypeAsForPodman(response)) {
      throw new IllegalStateException(
          "Docker daemon returned an unexpected content type while trying to build the Dockerfile.\n" +
            "Status: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
    }

    try (InputStream stream = response.getEntity().getContent()) {
      // Parse text as json
      EntityStreamReaderUtil.processJsonStream(handler, stream);
    }
    return null;
  }

  private static Function<HttpResponse, Boolean> isContentType(String contentType) {
    return response -> Stream.of(response.getAllHeaders())
      .filter(h -> h.getName().equalsIgnoreCase("Content-Type"))
      .anyMatch(h -> h.getValue().toLowerCase().startsWith(contentType));
  }

  private static boolean hasJsonContentType(HttpResponse response) {
    return isContentType("application/json").apply(response);
  }

  private static boolean hasTextPlainContentType(HttpResponse response) {
    return isContentType("text/plain").apply(response);
  }

  private static boolean hasNoContentTypeAsForPodman(HttpResponse response) {
    return Stream.of(response.getAllHeaders())
        .noneMatch(h -> h.getName().equalsIgnoreCase("Content-Type"));
  }
}
