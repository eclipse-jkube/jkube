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
package org.eclipse.jkube.quickstarts.gradle.springbootcamelcomplete;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class CamelRouteBuilder extends RouteBuilder {

  private static final Logger logger = LoggerFactory.getLogger(CamelRouteBuilder.class);

  private final String camelGreeting;

  @Autowired
  public CamelRouteBuilder(@Value("${camel.greeting}") String camelGreeting) {
    this.camelGreeting = camelGreeting;
  }

  @Override
  public void configure() {
    // Simple Route to generate a random order.xml every 5 seconds
    from("timer:order?period=5000")
        .bean(OrderGenerator.class, "generateOrder")
        .setHeader(Exchange.FILE_NAME)
        .method(OrderGenerator.class, "generateFileName")
        .log("Generating order ${file:name}")
        .to("file:/tmp/work/orders/input");
    // Route to consume these orders and move them to a processed directory
    from("file:/tmp/work/orders/input")
        .log("Processing order ${file:name}")
        .to("file:/tmp/work/orders/processed");
    // Route to log a Hello world message every second
    from("timer:foo?period=1s")
        .setBody().constant(camelGreeting)
        .log(LoggingLevel.INFO, logger, "${body}")
        .log(LoggingLevel.INFO, logger, "My id is ${id}");
    // Route for REST
    restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
    rest().get("/hello-camel").produces(MediaType.APPLICATION_JSON_VALUE).to("direct:hello-camel");
    from("direct:hello-camel").transform().simple(camelGreeting);
  }
}
