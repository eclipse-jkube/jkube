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
package org.eclipse.jkube.it.gradle.smallrye.health;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "CDIBasedHealthCheck", urlPatterns = "/health")
public class CDIBasedHealthCheck extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    SmallRyeHealthReporter reporter = new SmallRyeHealthReporter();
    SmallRyeHealth health = reporter.getHealth();
    if (health.isDown()) {
      res.setStatus(503);
    }
    try {
      reporter.reportHealth(res.getOutputStream(), health);
    } catch (IOException ioe) {
      res.setStatus(500);
    }
  }
}
