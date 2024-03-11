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

/**
 * @Author: Wayne Kirimi
 */
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class App {
    private static final Logger log = Logger.getLogger(App.class.getSimpleName());
    private static int PORT = 8081;
    public static void main(String[] args) throws IOException {

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

    static class RootHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            try {
                log.info("GET /hello");
                String response = "Hello World";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                log.info("Response received successfully: " + response);
                outputStream.close();
            }catch (IOException e){
                log.severe("Server failed to respond: " + e.getMessage());
            }
        }
    }
}
