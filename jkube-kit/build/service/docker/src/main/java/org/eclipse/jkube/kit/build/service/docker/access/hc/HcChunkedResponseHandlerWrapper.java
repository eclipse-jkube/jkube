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
package org.eclipse.jkube.kit.build.service.docker.access.hc;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.build.service.docker.access.chunked.EntityStreamReaderUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

class HcChunkedResponseHandlerWrapper implements ResponseHandler<Object> {

  private final EntityStreamReaderUtil.JsonEntityResponseHandler handler;

  HcChunkedResponseHandlerWrapper(EntityStreamReaderUtil.JsonEntityResponseHandler handler) {
    this.handler = handler;
  }

  @Override
  public Object handleResponse(HttpResponse response) throws IOException {
    try (InputStream stream = response.getEntity().getContent()) {
      // Parse text as json
      if (isJson(response)) {
        EntityStreamReaderUtil.processJsonStream(handler, stream);
      }
    }
    return null;
  }

  private static boolean isJson(HttpResponse response) {
    return Stream.of(response.getAllHeaders())
        .filter(h -> h.getName().equalsIgnoreCase("Content-Type"))
        .anyMatch(h -> h.getValue().toLowerCase().startsWith("application/json"));
  }
}
