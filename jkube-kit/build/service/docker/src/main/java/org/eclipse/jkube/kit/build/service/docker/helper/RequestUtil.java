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
package org.eclipse.jkube.kit.build.service.docker.helper;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * @author roland
 * @since 30/11/14
 */
public class RequestUtil {

    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ACCEPT_ALL = "*/*";

    private RequestUtil() { }

    // -----------------------
    // Request related methods
    public static HttpUriRequest newGet(String url) {
        return addDefaultHeaders(new HttpGet(url));
    }

    public static HttpUriRequest newPost(String url, String body) {
        HttpPost post = new HttpPost(url);
        if (body != null) {
            post.setEntity(new StringEntity(body, Charset.defaultCharset()));
        }
        return addDefaultHeaders(post);
    }

    public static HttpUriRequest newDelete(String url) {
        return addDefaultHeaders(new HttpDelete(url));
    }

    public static HttpUriRequest addDefaultHeaders(HttpUriRequest req) {
        req.addHeader(HEADER_ACCEPT, HEADER_ACCEPT_ALL);
        req.addHeader("Content-Type", "application/json");
        return req;
    }

    @SuppressWarnings("deprecation")
    public static String encode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // wont happen
            return URLEncoder.encode(param);
        }
    }

}
