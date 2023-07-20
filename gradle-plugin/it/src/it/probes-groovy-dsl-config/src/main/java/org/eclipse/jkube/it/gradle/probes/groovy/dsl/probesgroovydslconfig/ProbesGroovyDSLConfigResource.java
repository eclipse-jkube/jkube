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
package org.eclipse.jkube.it.gradle.probes.groovy.dsl.probesgroovydslconfig;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbesGroovyDSLConfigResource {
  @GetMapping(value = "/liveness/startup")
  public String getStartup() {
    return "OK";
  }

  @GetMapping(value = "/health/ready")
  public String getReady() {
    return "OK";
  }

  @GetMapping(value = "/health/live")
  public String getLiveness() {
    return "OK";
  }
}
