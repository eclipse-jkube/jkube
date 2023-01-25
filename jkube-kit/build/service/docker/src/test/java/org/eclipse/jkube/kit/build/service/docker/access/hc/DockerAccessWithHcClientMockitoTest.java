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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.jkube.kit.build.service.docker.access.CreateImageOptions;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerAccessWithHcClientMockitoTest {

  private ApacheHttpClientDelegate mockDelegate;
  private DockerAccessWithHcClient client;

  @BeforeEach
  void setUp() throws Exception {
    mockDelegate = mock(ApacheHttpClientDelegate.class, RETURNS_DEEP_STUBS);
    when(mockDelegate.getHttpClient().execute(any(HttpGet.class))).thenReturn(mock(CloseableHttpResponse.class));
    client = new DockerAccessWithHcClient("tcp://1.2.3.4:2375", null, 1, new KitLogger.SilentLogger()) {
      @Override
      ApacheHttpClientDelegate createHttpClient(ClientBuilder builder) {
        return mockDelegate;
      }
    };
  }

  @Test
  void pullImage() throws Exception {
    // When
    client.pullImage("name", null, "registry", new CreateImageOptions());
    // Then
    verify(mockDelegate, times(1))
        .post(eq("tcp://1.2.3.4:2375/v1.18/images/create"), isNull(), any(Map.class), any(HcChunkedResponseHandlerWrapper.class), eq(200));
  }

  @Test
  void pullImageThrowsException() throws Exception {
    // Given
    when(mockDelegate.post(eq("tcp://1.2.3.4:2375/v1.18/images/create"), isNull(), any(Map.class), any(HcChunkedResponseHandlerWrapper.class), eq(200)))
        .thenThrow(new IOException("Problem with images/create"));
    // When
    final DockerAccessException result = assertThrows(DockerAccessException.class, () ->
        client.pullImage("name", null, "registry", new CreateImageOptions()));
    // Then
    assertThat(result)
        .hasMessageStartingWith("Unable to pull 'name' from registry 'registry'")
        .hasMessageEndingWith("Problem with images/create");
  }
}
