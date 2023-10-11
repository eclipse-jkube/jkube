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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.http.HttpResponse;

import static org.eclipse.jkube.kit.common.util.okhttp.HttpHeaders.parseWwwAuthenticateChallengeHeaders;

public class Fabric8HttpUtil {
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  private Fabric8HttpUtil() { }

  /**
   * Parse WWW-Authenticate Header challenges as map
   *
   * @param response Http Response of a particular request
   * @return list of maps containing various authentication challenges which each authentication challenge as map
   */
  public static List<Map<String, String>> extractAuthenticationChallengeIntoMap(HttpResponse<?> response) throws IOException {
    return parseWwwAuthenticateChallengeHeaders(response.header(WWW_AUTHENTICATE));
  }

  /**
   * Create Form Data String from map
   *
   * @param formData map containing key value pairs for form data
   * @return URL encoded value of form data
   */
  public static String toFormData(Map<String, String> formData) throws UnsupportedEncodingException {
    StringBuilder result = new StringBuilder();
    for (Map.Entry<String, String> e : formData.entrySet()) {
      if (result.length() > 0) {
        result.append("&");
      }
      result.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.name()));
      result.append("=");
      result.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8.name()));
    }
    return result.toString();
  }
}
