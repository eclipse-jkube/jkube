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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.function.BiConsumer;

import org.eclipse.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApacheHttpClientDelegateTest {

  private CloseableHttpClient httpClient;
  private ApacheHttpClientDelegate apacheHttpClientDelegate;

  @BeforeEach
  void setUp() throws Exception {
    httpClient = mock(CloseableHttpClient.class);
    final ClientBuilder clientBuilder = mock(ClientBuilder.class);
    when(clientBuilder.buildBasicClient()).thenReturn(httpClient);
    apacheHttpClientDelegate = new ApacheHttpClientDelegate(clientBuilder, false);
  }

  @Test
  void createBasicClient() {
    final CloseableHttpClient result = apacheHttpClientDelegate.createBasicClient();
    assertThat(result).isNotNull();
  }

  @Test
  void delete() throws IOException {
    // Given
    when(httpClient.execute(any(), any(ResponseHandler.class))).thenReturn(1337);
    // When
    final int result = apacheHttpClientDelegate.delete("https://example.com");
    // Then
    assertThat(result).isEqualTo(1337);
    verifyHttpClientExecute((request, responseHandler) ->
      assertThat(request.getAllHeaders())
          .hasSize(1)
          .extracting("name", "value")
          .containsOnly(new Tuple("Accept", "*/*"))
    );
  }

  @Test
  void get() throws IOException {
    // Given
    when(httpClient.execute(any(), any(ResponseHandler.class))).thenReturn("Response");
    // When
    final String response = apacheHttpClientDelegate.get("https://example.com");
    // Then
    assertThat(response).isEqualTo("Response");
    verifyHttpClientExecute((request, responseHandler) ->{
      assertThat(request.getAllHeaders())
          .hasSize(1)
          .extracting("name", "value")
          .containsOnly(new Tuple("Accept", "*/*"));
      assertThat(responseHandler)
          .extracting("delegate")
          .isInstanceOf(ApacheHttpClientDelegate.BodyResponseHandler.class);
    });
  }

  @Test
  void postWithStringBody() throws IOException {
    // Given
    when(httpClient.execute(any(), any(ResponseHandler.class))).thenReturn("Response");
    // When
    final String response = apacheHttpClientDelegate.post(
        "https://example.com", "{body}", Collections.singletonMap("EXTRA", "HEADER"), null);
    // Then
    assertThat(response).isEqualTo("Response");
    verifyHttpClientExecute((request, responseHandler) ->
      assertThat(request.getAllHeaders())
          .hasSize(3)
          .extracting("name", "value")
          .containsOnly(
              new Tuple("Accept", "*/*"),
              new Tuple("Content-Type", "application/json"),
              new Tuple("EXTRA", "HEADER"))
    );
  }

  @Test
  void postWithFileBody() throws IOException {
    // Given
    when(httpClient.execute(any(), any(ResponseHandler.class))).thenReturn("Response");
    // When
    final String response = apacheHttpClientDelegate.post(
        "https://example.com", new File("fake-file.tar"), null);
    // Then
    assertThat(response).isEqualTo("Response");
    verifyHttpClientExecute((request, responseHandler) ->
      assertThat(request.getAllHeaders())
          .hasSize(2)
          .extracting("name", "value")
          .containsOnly(
              new Tuple("Accept", "*/*"),
              new Tuple("Content-Type", "application/x-tar"))
    );
  }

  private void verifyHttpClientExecute(BiConsumer<HttpUriRequest, ResponseHandler<?>> consumer) throws IOException {
    ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
    ArgumentCaptor<ResponseHandler<Object>> hArgumentCaptor = ArgumentCaptor.forClass(ResponseHandler.class);
    verify(httpClient).execute(httpUriRequestArgumentCaptor.capture(), hArgumentCaptor.capture());
    consumer.accept(httpUriRequestArgumentCaptor.getValue(),hArgumentCaptor.getValue());
  }

}
