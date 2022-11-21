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
package org.eclipse.jkube.kit.common;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestHttpStaticServer implements Closeable {
  private final HttpServer server;
  private final File staticDirectory;
  private static final Logger log = LoggerFactory.getLogger(TestHttpStaticServer.class);

  public TestHttpStaticServer(int port, File staticDirectory) throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);
    this.staticDirectory = staticDirectory;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(this::startServer);
  }

  private void startServer() {
    server.createContext("/", exchange -> {
      File file = new File(staticDirectory, exchange.getRequestURI().getPath()).getCanonicalFile();

      if (file.isFile()) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream outputStream = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(file)) {
          final byte[] buffer = new byte[0x10000];
          int count;
          while ((count = fis.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, count);
          }
        }
      } else {
        String response = "404 (Not Found)\n";
        exchange.sendResponseHeaders(404 , response.length());
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(response.getBytes());
        }
      }
    });
    server.setExecutor(null);
    log.info("Starting server");
    server.start();
  }

  @Override
  public void close() throws IOException {
    log.info("Stopping server");
    server.stop(0);
  }
}
