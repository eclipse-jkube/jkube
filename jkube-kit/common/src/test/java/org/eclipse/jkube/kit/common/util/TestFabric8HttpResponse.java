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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestFabric8HttpResponse implements HttpResponse<byte[]> {
  private final int code;
  private final Map<String, List<String>> headers;
  private final String body;
  private final String message;

  public TestFabric8HttpResponse(int code, Map<String, List<String>> headers, String body, String message) {
    this.code = code;
    this.headers = headers;
    this.body = body;
    this.message = message;
  }

  @Override
  public int code() { return code; }

  @Override
  public byte[] body() {
    if (StringUtils.isNotBlank(body)) {
      return body.getBytes();
    }
    return null;
  }

  @Override
  public HttpRequest request() { return null; }

  @Override
  public Optional<HttpResponse<?>> previousResponse() { return Optional.empty(); }

  @Override
  public List<String> headers(String s) { return headers.get(s); }

  @Override
  public Map<String, List<String>> headers() { return headers; }

  @Override
  public String message() { return message; }
}
