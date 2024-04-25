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
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpServer;

/**
 * @author: Wayne Kirimi
 */
public class App {

  private static final Logger log = Logger.getLogger(App.class.getSimpleName());
  private static final int PORT = 8080;

  public static void main(String[] args) {

    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
      server.createContext("/hello", new RootHandler());
      server.setExecutor(null);
      server.start();
      log.info("Server started on port: " + PORT);
    } catch (IOException e) {
      log.severe("Error occured when starting server: " + e.getMessage());
    }
  }
}
