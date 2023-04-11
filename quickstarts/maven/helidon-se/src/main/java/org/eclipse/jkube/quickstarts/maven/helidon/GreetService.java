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
package org.eclipse.jkube.quickstarts.maven.helidon;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public class GreetService implements Service {

  private final AtomicReference<String> greeting = new AtomicReference<>();

  private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

  private static final Logger LOGGER = Logger.getLogger(GreetService.class.getName());

  GreetService(Config config) {
    greeting.set(config.get("app.greeting").asString().orElse("Ciao"));
  }

  @Override
  public void update(Routing.Rules rules) {
    rules
        .get("/", this::getDefaultMessageHandler)
        .get("/{name}", this::getMessageHandler)
        .put("/greeting", this::updateGreetingHandler);
  }

  private void getDefaultMessageHandler(ServerRequest request, ServerResponse response) {
    sendResponse(response, "World");
  }

  private void getMessageHandler(ServerRequest request, ServerResponse response) {
    String name = request.path().param("name");
    sendResponse(response, name);
  }

  private void sendResponse(ServerResponse response, String name) {
    String msg = String.format("%s %s!", greeting.get(), name);

    JsonObject returnObject = JSON.createObjectBuilder()
        .add("message", msg)
        .build();
    response.send(returnObject);
  }

  private static <T> T processErrors(Throwable ex, ServerRequest request, ServerResponse response) {

    if (ex.getCause() instanceof JsonException) {

      LOGGER.log(Level.FINE, "Invalid JSON", ex);
      JsonObject jsonErrorObject = JSON.createObjectBuilder()
          .add("error", "Invalid JSON")
          .build();
      response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
    } else {

      LOGGER.log(Level.FINE, "Internal error", ex);
      JsonObject jsonErrorObject = JSON.createObjectBuilder()
          .add("error", "Internal error")
          .build();
      response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonErrorObject);
    }

    return null;
  }

  private void updateGreetingFromJson(JsonObject jo, ServerResponse response) {
    if (!jo.containsKey("greeting")) {
      JsonObject jsonErrorObject = JSON.createObjectBuilder()
          .add("error", "No greeting provided")
          .build();
      response.status(Http.Status.BAD_REQUEST_400)
          .send(jsonErrorObject);
      return;
    }

    greeting.set(jo.getString("greeting"));
    response.status(Http.Status.NO_CONTENT_204).send();
  }

  private void updateGreetingHandler(ServerRequest request,
                                     ServerResponse response) {
    request.content().as(JsonObject.class)
        .thenAccept(jo -> updateGreetingFromJson(jo, response))
        .exceptionally(ex -> processErrors(ex, request, response));
  }
}
