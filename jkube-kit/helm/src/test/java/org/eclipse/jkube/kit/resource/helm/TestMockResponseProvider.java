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

import io.fabric8.mockwebserver.utils.ResponseProvider;
import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.Map;

public class TestMockResponseProvider implements ResponseProvider<Object> {
  private final int code;
  private final Map<String, String> headers;
  private final Object body;

  public TestMockResponseProvider(int code, Map<String, String> headers, Object body) {
    this.code = code;
    this.headers = headers;
    this.body = body;
  }

  @Override
  public int getStatusCode(RecordedRequest recordedRequest) {
    return code;
  }

  @Override
  public Headers getHeaders() {
    return Headers.of(headers);
  }

  @Override
  public void setHeaders(Headers headers) {

  }

  @Override
  public Object getBody(RecordedRequest recordedRequest) {
    return body;
  }
}
