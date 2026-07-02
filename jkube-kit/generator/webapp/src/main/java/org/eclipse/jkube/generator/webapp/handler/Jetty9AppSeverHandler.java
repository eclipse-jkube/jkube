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
package org.eclipse.jkube.generator.webapp.handler;

import org.eclipse.jkube.generator.api.GeneratorContext;

/**
 * Legacy Jetty 9 handler, opt-in only via {@code jkube.generator.webapp.server=jetty9}.
 */
public class Jetty9AppSeverHandler extends AbstractAppServerHandler {

  public Jetty9AppSeverHandler(GeneratorContext context) {
    super("jetty9", context);
  }

  @Override
  public boolean isApplicable() {
    return false;
  }

  @Override
  public String getFrom() {
    return imageLookup.getImageName("jetty9.upstream.docker");
  }

  @Override
  public String getDeploymentDir() {
    return "/deployments";
  }

  @Override
  public String getCommand() {
    return "/usr/local/s2i/run";
  }

  @Override
  public boolean supportsS2iBuild() {
    return true;
  }
}
