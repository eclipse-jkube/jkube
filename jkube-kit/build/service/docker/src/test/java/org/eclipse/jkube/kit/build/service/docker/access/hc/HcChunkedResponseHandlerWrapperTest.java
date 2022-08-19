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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
class HcChunkedResponseHandlerWrapperTest {

  @Mocked
  private EntityStreamReaderUtil.JsonEntityResponseHandler handler;
  @Mocked
  private HttpResponse response;
  @Mocked
  private EntityStreamReaderUtil entityStreamReaderUtil;

  private Header[] headers;
  private HcChunkedResponseHandlerWrapper hcChunkedResponseHandlerWrapper;

  @BeforeEach
  void setUp() {
    hcChunkedResponseHandlerWrapper = new HcChunkedResponseHandlerWrapper(handler);
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
    // @formatter:off
    new Expectations() {{
      response.getAllHeaders(); result = headers;
    }};
    // @formatter:on
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private void verifyProcessJsonStream(int timesCalled) throws IOException {
    // @formatter:off
    new Verifications() {{
      entityStreamReaderUtil.processJsonStream(handler, response.getEntity().getContent()); times = timesCalled;
    }};
    // @formatter:on
  }
}