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
package org.eclipse.jkube.sample.helloworld;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * @author: Wayne Kirimi
 */
public class RootHandler implements HttpHandler {

  private static final Logger log = Logger.getLogger(App.class.getSimpleName());

  @Override
  public void handle(HttpExchange exchange) {
    try (OutputStream outputStream = exchange.getResponseBody()) {
      log.info("GET /hello");
      String response = "Hello World";
      exchange.sendResponseHeaders(200, response.length());
      exchange.getResponseHeaders().set("Content-Type", "text/plain");
      outputStream.write(response.getBytes());
      log.info("Response written successfully: " + response);
    } catch (IOException e) {
      log.severe("Server failed to respond: " + e.getMessage());
    }
  }
}
