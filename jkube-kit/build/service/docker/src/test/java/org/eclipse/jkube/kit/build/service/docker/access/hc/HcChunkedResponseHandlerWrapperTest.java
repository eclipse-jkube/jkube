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

import org.eclipse.jkube.kit.build.service.docker.access.chunked.EntityStreamReaderUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HcChunkedResponseHandlerWrapperTest {
  private EntityStreamReaderUtil.JsonEntityResponseHandler handler;

  private HttpResponse response;

  private EntityStreamReaderUtil entityStreamReaderUtil;

  private HcChunkedResponseHandlerWrapper hcChunkedResponseHandlerWrapper;

  @BeforeEach
  void setUp() {
    hcChunkedResponseHandlerWrapper = new HcChunkedResponseHandlerWrapper(handler);
    handler = mock(EntityStreamReaderUtil.JsonEntityResponseHandler.class);
    response = mock(HttpResponse.class);
    entityStreamReaderUtil = mock(EntityStreamReaderUtil.class);

  }

  @Test
  void handleResponseWithJsonResponse() throws IOException {
    givenResponseHeaders(new BasicHeader("ConTenT-Type", "application/json; charset=UTF-8"));
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    verifyProcessJsonStream(1);
  }

  @Test
  void handleResponseWithTextPlainResponse() throws IOException {
    givenResponseHeaders(new BasicHeader("Content-Type", "text/plain"));
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    verifyProcessJsonStream(0);
  }

  @Test
  void handleResponseWithNoContentType() throws IOException {
    givenResponseHeaders();
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    verifyProcessJsonStream(0);
  }

  private void givenResponseHeaders(Header... headers) {
    when(response.getAllHeaders()).thenReturn(headers);
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private void verifyProcessJsonStream(int timesCalled) throws IOException {
    verify(entityStreamReaderUtil,times(timesCalled)).processJsonStream(handler, response.getEntity().getContent());
  }
}