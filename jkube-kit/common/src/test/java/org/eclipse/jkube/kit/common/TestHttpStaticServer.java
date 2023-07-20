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
package org.eclipse.jkube.kit.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.jkube.kit.common.util.AsyncUtil.async;
import static org.eclipse.jkube.kit.common.util.IoUtil.getFreeRandomPort;

public class TestHttpStaticServer implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(TestHttpStaticServer.class);
  private final CompletableFuture<HttpServer> server;

  public TestHttpStaticServer(File staticDirectory) {
    server = async(startServer(staticDirectory));
  }

  @Override
  public void close() throws IOException {
    log.info("Stopping server");
    getServer().stop(0);
  }

  private static Callable<HttpServer> startServer(File staticDirectory) {
    return () -> {
      final int port = getFreeRandomPort();
      final HttpServer ret = HttpServer.create(new InetSocketAddress(port), 0);
      ret.createContext("/", exchange -> {
        final String path = exchange.getRequestURI().getPath();
        final File file = new File(staticDirectory, path).getCanonicalFile();
        if (path.equals("/health")) {
          reply(exchange, 200, "READY");
        } else if (file.isFile()) {
          try (
            OutputStream outputStream = exchange.getResponseBody();
            FileInputStream fis = new FileInputStream(file)
          ) {
            exchange.getResponseHeaders().set("Content-Type",
              Optional.ofNullable(URLConnection.guessContentTypeFromStream(fis)).orElse("application/octet-stream"));
            exchange.sendResponseHeaders(200, 0);
            IOUtils.copy(fis, outputStream);
          }
        } else {
          reply(exchange, 404, "404 (Not Found)\n");
        }
      });
      log.info("Starting server");
      ret.start();
      return ret;
    };
  }

  private static void reply(HttpExchange exchange, int code, String body) throws IOException {
    exchange.sendResponseHeaders(code , body.length());
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(body.getBytes());
    }
  }

  public int getPort() {
    return getServer().getAddress().getPort();
  }

  private HttpServer getServer() {
    try {
      return server.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (TimeoutException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
