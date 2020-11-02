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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.function.BiConsumer;

import org.eclipse.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"rawtypes", "unused"})
public class ApacheHttpClientDelegateTest {

  @Mocked
  private ClientBuilder clientBuilder;
  @Mocked
  private CloseableHttpClient httpClient;

  private ApacheHttpClientDelegate apacheHttpClientDelegate;

  @Before
  public void setUp() throws Exception {
    // @formatter:off
    new Expectations() {{
      clientBuilder.buildBasicClient(); result = httpClient;
    }};
    // @formatter:on
    apacheHttpClientDelegate = new ApacheHttpClientDelegate(clientBuilder, false);
  }

  @Test
  public void createBasicClient() {
    final CloseableHttpClient result = apacheHttpClientDelegate.createBasicClient();
    assertThat(result).isNotNull();
  }

  @Test
  public void delete() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      httpClient.execute((HttpUriRequest) any, (ResponseHandler) any); result = 1337;
    }};
    // @formatter:on
    // When
    final int result = apacheHttpClientDelegate.delete("http://example.com");
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
  public void get() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      httpClient.execute((HttpUriRequest) any, (ResponseHandler) any); result = "Response";
    }};
    // @formatter:on
    // When
    final String response = apacheHttpClientDelegate.get("http://example.com");
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
  public void postWithStringBody() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      httpClient.execute((HttpUriRequest) any, (ResponseHandler) any); result = "Response";
    }};
    // @formatter:on
    // When
    final String response = apacheHttpClientDelegate.post(
        "http://example.com", "{body}", Collections.singletonMap("EXTRA", "HEADER"), null);
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
  public void postWithFileBody() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      httpClient.execute((HttpUriRequest) any, (ResponseHandler) any); result = "Response";
    }};
    // @formatter:on
    // When
    final String response = apacheHttpClientDelegate.post(
        "http://example.com", new File("fake-file.tar"), null);
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

  private <H extends ResponseHandler> void verifyHttpClientExecute(BiConsumer<HttpUriRequest, H> consumer) throws IOException {
    // @formatter:off
    new Verifications() {{
      HttpUriRequest request;
      H responseHandler;
      httpClient.execute(request = withCapture(), responseHandler = withCapture());
      consumer.accept(request, responseHandler);
    }};
    // @formatter:on
  }

}