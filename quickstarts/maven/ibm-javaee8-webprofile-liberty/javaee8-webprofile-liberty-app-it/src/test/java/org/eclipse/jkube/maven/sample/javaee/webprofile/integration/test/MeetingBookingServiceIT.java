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
package org.eclipse.jkube.maven.sample.javaee.webprofile.integration.test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Before;
import org.junit.Test;

public class MeetingBookingServiceIT {

  private static final String APPSERVER_TEST_HOST_PROPERTY = "appserver.test.host";

  private static final String APPSERVER_TEST_PORT_PROPERTY = "appserver.test.port";

  private static final String APPLICATION_CONTEXT_ROOT_PROPERTY = "application.context.root";

  private static final String HOST = "host";

  private static final String PORT = "port";

  private static final String CONTEXTROOT = "context-root";

  private static final String PROXY_URL_FMT = "http://{host}:{port}/{context-root}";

  private WebTarget target;

  @Before
  public void prepareWebTarget() {
    target = ClientBuilder.newClient().target(PROXY_URL_FMT)
        .resolveTemplate(HOST, System.getProperty(APPSERVER_TEST_HOST_PROPERTY))
        .resolveTemplate(PORT, System.getProperty(APPSERVER_TEST_PORT_PROPERTY))
        .resolveTemplate(CONTEXTROOT, System.getProperty(APPLICATION_CONTEXT_ROOT_PROPERTY));
  }

  @Test
  public void testBaseline() {
    String banner = target.path("/")
        .request()
        .accept("text/html")
        .get(String.class);
    assertEquals("Microservice Meeting Room Booking API Application", banner);
  }
}
