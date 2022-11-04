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

import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.eclipse.jkube.kit.build.service.docker.access.chunked.EntityStreamReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SuppressWarnings("unused")
class HcChunkedResponseHandlerWrapperTest {

  private Header[] headers;
  private TestJsonEntityResponseHandler handler;
  private HcChunkedResponseHandlerWrapper hcChunkedResponseHandlerWrapper;

  @BeforeEach
  void setUp() {
    handler = new TestJsonEntityResponseHandler();
    hcChunkedResponseHandlerWrapper = new HcChunkedResponseHandlerWrapper(handler);
  }

  @Test
  void handleResponseWithInvalidStatusAndJsonBody() throws IOException {
    final HttpResponse response = response(400, "WRONG!!",
      new StringEntity("{}"));
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    // TODO: Maybe we should propagate the status in some way
    assertThat(handler.processedObject).isNotNull();
  }

  @Test
  void handleResponseWithJsonContentTypeAndJsonBody() throws IOException {
    final HttpResponse response = response(200, "OK",
      new StringEntity("{\"field\":\"value\"}"),
      new BasicHeader("ConTenT-Type", "application/json; charset=UTF-8"));
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    assertThat(handler.processedObject)
      .returns("value", jo -> jo.get("field").getAsString());
  }
  @Test
  void handleResponseWithJsonContentTypeAndInvalidBody() throws IOException {
    final HttpResponse response = response(200, "OK",
      new StringEntity("This is not a JSON string"),
      new BasicHeader("ConTenT-Type", "application/json"));
    assertThatIllegalStateException()
      .isThrownBy(() -> hcChunkedResponseHandlerWrapper.handleResponse(response))
      .withMessageStartingWith("Not a JSON Object:");
  }

  @Test
  void handleResponseWithTextPlainAndJsonBody() throws IOException {
    final HttpResponse response = response(200, "OK",
      new StringEntity("{\"field\":\"value\"}"),
      new BasicHeader("ConTenT-Type", "text/plain"));
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    assertThat(handler.processedObject)
      .returns("value", jo -> jo.get("field").getAsString());
  }

  @Test
  void handleResponseWithNoContentTypeAndJsonBody() throws IOException {
    final HttpResponse response = response(200, "OK",
      new StringEntity("{\"field\":\"value\"}"));
    hcChunkedResponseHandlerWrapper.handleResponse(response);
    assertThat(handler.processedObject)
      .returns("value", jo -> jo.get("field").getAsString());
  }

  private static HttpResponse response(int code, String reason, HttpEntity entity, Header... headers) {
    final BasicHttpResponse response = new BasicHttpResponse(
      new ProtocolVersion("HTTP", 1, 1), code, reason);
    response.setEntity(entity);
    response.setHeaders(headers);
    return response;
  }

  private static final class TestJsonEntityResponseHandler implements EntityStreamReaderUtil.JsonEntityResponseHandler {

    private JsonObject processedObject;
    @Override
    public void process(JsonObject toProcess) {
      processedObject = toProcess;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
  }
}
